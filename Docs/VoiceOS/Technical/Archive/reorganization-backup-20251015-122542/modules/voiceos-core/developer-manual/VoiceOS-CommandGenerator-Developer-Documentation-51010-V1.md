# CommandGenerator - Developer Documentation

**File:** `src/main/java/com/augmentalis/voiceaccessibility/scraping/CommandGenerator.kt`
**Package:** `com.augmentalis.voiceaccessibility.scraping`
**Purpose:** NLP-based voice command generator with synonym support
**Dependencies:** Room (GeneratedCommandEntity, ScrapedElementEntity)
**Last Analyzed:** 2025-10-10 10:34:00 PDT

## Overview

`CommandGenerator` automatically creates natural language voice commands from scraped UI elements. It uses NLP rules to generate primary commands and synonyms, calculates confidence scores based on text quality, and determines appropriate action types for each element.

### Key Features

- **Multi-Action Support**: Generates commands for click, long_click, type, scroll, and focus actions
- **Synonym Generation**: Creates alternative phrases for the same action (e.g., "tap", "press", "click")
- **Confidence Scoring**: Rates command quality based on text source, length, and element type
- **Contextual Commands**: Adapts command structure based on element capabilities

### Command Generation Strategy

```
UI Element (Button with text "Submit")
         ↓
Extract Text: "Submit" (from element.text)
         ↓
Determine Action: "click" (element.isClickable = true)
         ↓
Generate Primary: "click submit"
         ↓
Generate Synonyms: ["tap submit", "press submit", "send", "submit button"]
         ↓
Calculate Confidence: 0.95 (high - clear button with text)
         ↓
Store as GeneratedCommandEntity
```

## Public API

### `generateCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity>`

**Purpose:** Generate all applicable voice commands for a single element
**Parameters:**
- `element: ScrapedElementEntity` - UI element to generate commands for

**Returns:** `List<GeneratedCommandEntity>` - Generated commands (may be empty)
**Threading:** Suspending function
**Filtering:** Automatically filters out low-confidence commands (< 0.2)

**Algorithm:**
1. Extract meaningful text from element (text → contentDescription → viewId)
2. If element is clickable → generate click commands
3. If element is long clickable → generate long click commands
4. If element is editable → generate input commands
5. If element is scrollable → generate scroll commands
6. If element is focusable (and not clickable) → generate focus commands
7. Filter commands below MIN_CONFIDENCE threshold

**Usage Example:**
```kotlin
val commands = commandGenerator.generateCommands(buttonElement)
// Returns: [GeneratedCommandEntity(commandText="click submit", confidence=0.95, ...)]
```

---

### `generateCommandsForElements(elements: List<ScrapedElementEntity>): List<GeneratedCommandEntity>`

**Purpose:** Batch generate commands for multiple elements
**Parameters:**
- `elements: List<ScrapedElementEntity>` - List of elements to process

**Returns:** `List<GeneratedCommandEntity>` - All generated commands (flattened)
**Threading:** Suspending function

**Usage Example:**
```kotlin
val allElements = database.scrapedElementDao().getElementsByAppId(appId)
val allCommands = commandGenerator.generateCommandsForElements(allElements)
database.generatedCommandDao().insertBatch(allCommands)
```

## Private Implementation

### Text Extraction

#### `extractElementText(element: ScrapedElementEntity): String?`

**Priority Order:**
1. **element.text** - Highest priority (visible label)
2. **element.contentDescription** - Second priority (accessibility label)
3. **element.viewIdResourceName** - Lowest priority (converted from "submit_button" → "submit button")

**Returns:** `null` if no meaningful text found

---

### Command Generation by Action Type

#### `generateClickCommands(element, text): List<GeneratedCommandEntity>`

**Primary Command:** `"click {text}"`
**Synonyms Generated:**
- Verb variations: "tap {text}", "press {text}", "select {text}", "activate {text}"
- Semantic synonyms (if text matches common buttons): "submit" → "send", "post", "confirm"
- Simplified version: Just the text without verb

**Confidence Calculation:** Based on text quality and element type

---

#### `generateLongClickCommands(element, text): List<GeneratedCommandEntity>`

**Primary Command:** `"long press {text}"`
**Synonyms:** "hold {text}", "long click {text}"
**Confidence:** 90% of base confidence (slightly less reliable than click)

---

#### `generateInputCommands(element, text): List<GeneratedCommandEntity>`

**Primary Command:** `"type in {text}"`
**Synonyms:** "enter {text}", "input {text}", "write {text}"
**Action Type:** `"type"`

---

#### `generateScrollCommands(element, text): List<GeneratedCommandEntity>`

**Primary Command:** `"scroll {text}"`
**Synonyms:** "swipe {text}", "move {text}"
**Confidence:** 80% of base confidence

---

#### `generateFocusCommands(element, text): List<GeneratedCommandEntity>`

**Primary Command:** `"focus {text}"`
**Synonyms:** "highlight {text}", "go to {text}"
**Confidence:** 70% of base confidence
**Only Generated:** When element is focusable but NOT clickable

---

### Confidence Scoring

#### `calculateConfidence(text: String, element: ScrapedElementEntity): Float`

**Base Confidence:** 0.5

**Text Source Bonus:**
- Direct text label: +0.3
- Content description: +0.2
- View ID fallback: +0.1

**Text Length Bonus:**
- Ideal (5-20 chars): +0.2
- Short (3-4 chars): +0.1
- Too long (>20 chars): -0.1
- Too short (<3 chars): -0.2

**Element Type Bonus:**
- Button: +0.2
- ImageButton: +0.15
- EditText: +0.15
- Clickable TextView: +0.1

**Text Quality Penalty:**
- Special characters: -0.05 per character
- Numbers: -0.02 per digit

**Final Score:** Clamped to [0.0, 1.0]

**Examples:**
```kotlin
// Button with text "Submit" (6 chars, button type, no special chars)
// 0.5 + 0.3 (text) + 0.2 (ideal length) + 0.2 (button) = 1.0 → clamped to 1.0

// TextView with viewId "btn_123_submit" (after conversion: "btn 123 submit")
// 0.5 + 0.1 (viewId) + 0.1 (short-ish) - 0.06 (3 numbers) = 0.64

// EditText with contentDescription "Email address"
// 0.5 + 0.2 (desc) + 0.2 (ideal length) + 0.15 (EditText) = 1.05 → clamped to 1.0
```

---

### Synonym Generation

#### `generateClickSynonyms(text: String): List<String>`

**Strategy:**
1. Add all click verb variations (tap, press, click, select, activate)
2. Check if text contains known button words (submit, cancel, next, etc.)
3. If match found, add semantic synonyms (e.g., "submit" → "send", "post", "confirm")
4. Add simplified version (text without verb)
5. Remove duplicates

**Synonym Dictionary (Built-in):**
- submit → send, post, confirm, ok
- cancel → close, dismiss, exit, back
- next → continue, forward, proceed, advance
- previous → back, backward, return, prior
- save → store, keep, preserve
- delete → remove, erase, clear
- edit → modify, change, update
- search → find, look for, locate
- login → sign in, log in, enter
- logout → sign out, log out, exit
- share → send, forward, distribute
- refresh → reload, update, renew
- settings → options, preferences, configuration
- help → assistance, support, info

**Example:**
```kotlin
generateClickSynonyms("submit form")
// Returns: [
//   "click submit form", "tap submit form", "press submit form",
//   "select submit form", "activate submit form",
//   "send", "post", "confirm", "ok",
//   "click send", "click post", "click confirm",
//   "submit form"
// ]
```

## Data Structures

### Constants

```kotlin
private const val MIN_CONFIDENCE = 0.2f  // Minimum threshold for commands

private val CLICK_VERBS = listOf("click", "tap", "press", "select", "activate")
private val INPUT_VERBS = listOf("type", "enter", "input", "write")
private val SCROLL_VERBS = listOf("scroll", "swipe", "move")
private val LONG_CLICK_VERBS = listOf("long press", "hold", "long click")
private val FOCUS_VERBS = listOf("focus", "highlight", "go to")

private val BUTTON_SYNONYMS = mapOf<String, List<String>>(
    // 14 common button types with semantic synonyms
)
```

## Threading Model

- **All methods are suspending functions**
- **Thread:** Runs on caller's coroutine context (typically Dispatchers.IO)
- **No explicit threading:** Room and coroutines handle concurrency

## Integration Points

### Used By

1. **AccessibilityScrapingIntegration**
   - Calls `generateCommandsForElements()` after scraping
   - Passes all scraped elements for batch processing

2. **ScrapingCoordinator** (deprecated)
   - Legacy integration point
   - Same usage pattern as AccessibilityScrapingIntegration

### Dependencies

- **ScrapedElementEntity**: Input data source
- **GeneratedCommandEntity**: Output data structure
- **JSONArray**: For storing synonyms as JSON string

## Examples

### Example 1: Single Element Command Generation

```kotlin
val buttonElement = ScrapedElementEntity(
    elementHash = "abc123",
    appId = "app-uuid",
    className = "android.widget.Button",
    text = "Submit",
    isClickable = true,
    // ... other properties
)

val commands = commandGenerator.generateCommands(buttonElement)

commands.forEach { cmd ->
    println("Command: ${cmd.commandText}")
    println("Action: ${cmd.actionType}")
    println("Confidence: ${cmd.confidence}")
    println("Synonyms: ${cmd.synonyms}")
    println()
}

// Output:
// Command: click submit
// Action: click
// Confidence: 0.95
// Synonyms: ["tap submit", "press submit", "send", "submit button", ...]
```

### Example 2: EditText Command Generation

```kotlin
val editTextElement = ScrapedElementEntity(
    elementHash = "def456",
    appId = "app-uuid",
    className = "android.widget.EditText",
    contentDescription = "Email address",
    isEditable = true,
    isFocusable = true,
    // ... other properties
)

val commands = commandGenerator.generateCommands(editTextElement)

// Generates both type and focus commands:
// 1. GeneratedCommandEntity(commandText="type in email address", actionType="type", ...)
// Note: Focus command NOT generated because element is editable (implicitly focusable)
```

### Example 3: Multi-Action Element

```kotlin
val complexElement = ScrapedElementEntity(
    elementHash = "ghi789",
    appId = "app-uuid",
    className = "android.widget.ImageButton",
    contentDescription = "More options",
    isClickable = true,
    isLongClickable = true,
    // ... other properties
)

val commands = commandGenerator.generateCommands(complexElement)

// Generates 2 commands:
// 1. Click command: "click more options" (confidence ~0.8)
// 2. Long click command: "long press more options" (confidence ~0.72)
```

### Example 4: Low-Confidence Filtering

```kotlin
val poorElement = ScrapedElementEntity(
    elementHash = "jkl012",
    appId = "app-uuid",
    className = "android.view.View",
    viewIdResourceName = "com.example:id/x",  // Becomes just "x" after conversion
    isClickable = true,
    // No text, no contentDescription
)

val commands = commandGenerator.generateCommands(poorElement)

// May return empty list if confidence < 0.2:
// Text "x" is very short → -0.2 penalty
// Base 0.5 + viewId 0.1 - 0.2 (short) = 0.4 (would pass)
// But if viewId was empty, confidence would be 0.3, still passes
// Anything below 0.2 gets filtered out
```

### Example 5: Batch Processing

```kotlin
suspend fun generateCommandsForApp(appId: String) {
    val database = AppScrapingDatabase.getInstance(context)
    val commandGenerator = CommandGenerator(context)

    // Get all elements for app
    val elements = database.scrapedElementDao().getElementsByAppId(appId)

    // Generate commands in batch
    val commands = commandGenerator.generateCommandsForElements(elements)

    // Filter and log statistics
    val highConfidence = commands.count { it.confidence >= 0.8 }
    val mediumConfidence = commands.count { it.confidence in 0.5..0.79 }
    val lowConfidence = commands.count { it.confidence < 0.5 }

    Log.i(TAG, "Generated ${commands.size} commands:")
    Log.i(TAG, "  High confidence (≥0.8): $highConfidence")
    Log.i(TAG, "  Medium confidence (0.5-0.79): $mediumConfidence")
    Log.i(TAG, "  Low confidence (<0.5): $lowConfidence")

    // Insert to database
    database.generatedCommandDao().insertBatch(commands)
}
```

## Known Limitations

1. **English Only**: Synonym dictionary and verb lists are English-only
2. **Static Synonyms**: No machine learning, uses hardcoded synonym mappings
3. **No Context Awareness**: Doesn't use element hierarchy for command disambiguation
4. **Limited Action Types**: Missing swipe, drag, pinch, rotate gestures
5. **No User Learning**: Can't adapt to user's preferred phrasing

## Future Enhancements

1. **Multilingual Support**: i18n synonym dictionaries
2. **ML-Based Synonym Generation**: Use NLP models to discover synonyms
3. **Context-Aware Commands**: "click the button in the dialog"
4. **User Preference Learning**: Adapt to user's command style over time
5. **Gesture Support**: Add swipe, drag, pinch command generation
6. **Confidence Tuning**: Use success rate to adjust confidence algorithm

---

**Documentation Generated:** 2025-10-10 10:34:00 PDT
**VOS4 Version:** 4.0.0
