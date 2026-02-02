# VoiceOSCore: Garbage Text Filtering & Icon Commands

**Version:** 1.0.0
**Created:** 2026-02-02
**Module:** VoiceOSCore
**File:** `CommandGenerator.kt`
**Status:** Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Garbage Text Filtering](#garbage-text-filtering)
3. [Icon Command Support](#icon-command-support)
4. [Localization](#localization)
5. [API Reference](#api-reference)
6. [Usage Examples](#usage-examples)
7. [Configuration](#configuration)

---

## Overview

This document describes the garbage text filtering and icon command support features added to the VoiceOSCore `CommandGenerator`. These features improve voice command quality by:

1. **Filtering garbage text** - Removes invalid voice command candidates like "comma comma com", CSS classes, UUIDs, and programming artifacts
2. **Supporting icon commands** - Enables single-word voice commands for navigation icons (Menu, More, Search, etc.)
3. **External localization** - Filter data loaded from AVU files, allowing language updates without recompiling

### Architecture

Filter data is loaded from external AVU files via `FilterFileLoader`:

```
Modules/VoiceOS/managers/CommandManager/src/main/assets/filters/
├── en-US/
│   ├── garbage-words.avu
│   └── navigation-icons.avu
├── de-DE/
│   ├── garbage-words.avu
│   └── navigation-icons.avu
├── es-ES/
│   └── ...
└── fr-FR/
    └── ...
```

This allows language updates via external storage without app recompilation.

### Problem Statement

Users reported erroneous overlay items appearing in apps like Gmail Android, showing text like:
- "comma comma com" (repetitive garbage text)
- CSS class names (e.g., "btn-primary-flex")
- Base64/hash strings
- Programming artifacts ([object Object], undefined, null)

Additionally, icon buttons with `contentDescription` metadata (like "Meet", "More") were not generating voice commands because they only had single words.

---

## Garbage Text Filtering

### Detection Patterns

The system uses multiple detection strategies:

#### 1. Repetitive Words (Loaded from AVU Files)

Detects repeated technical words that don't form valid commands. Loaded from external AVU files:

**AVU File Format (garbage-words.avu):**
```
# Avanues Universal Format v2.0
# Type: FILTER
---
schema: avu-2.0
version: 1.0.0
locale: en-US
---
GWD:comma:Repetitive word
GWD:dot:Repetitive word
GWD:dash:Repetitive word
GWD:space:Repetitive word
GWD:null:Repetitive word
GWD:undefined:Repetitive word
---
GEX:undefined:Exact garbage
GEX:null:Exact garbage
GEX:[object object]:Exact garbage
```

**Detection Logic:**
- Split text into words
- Count occurrences of repetitive words (from GWD entries)
- Flag as garbage if >50% of words are repetitive

#### 2. Regex Patterns (Language-Agnostic)

```kotlin
private val GARBAGE_PATTERNS = listOf(
    // CSS class-like: "btn-primary", "flex-row-reverse"
    Regex("^[a-z]+(-[a-z]+){2,}$", RegexOption.IGNORE_CASE),

    // Base64/hash strings (20+ alphanumeric chars)
    Regex("^[A-Za-z0-9+/=]{20,}$"),

    // Hex strings (8+ hex chars)
    Regex("^(0x)?[a-f0-9]{8,}$", RegexOption.IGNORE_CASE),

    // Just punctuation/whitespace
    Regex("^[\\s\\p{Punct}]+$"),

    // UUIDs
    Regex("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"),

    // Package names (com.example.app)
    Regex("^[a-z]+\\.[a-z]+\\.[a-z]+", RegexOption.IGNORE_CASE),

    // Object toString: [object Object], Object@hash
    Regex("^\\[?object\\s*\\w*\\]?$|^\\w+@[a-f0-9]+$", RegexOption.IGNORE_CASE)
)
```

#### 3. Exact Match Garbage

```kotlin
private val GARBAGE_EXACT = setOf(
    "undefined", "null", "nan", "NaN", "NULL",
    "[object object]", "[Object object]",
    "function", "error", "exception",
    "...", "---", "___", "true", "false", ""
)
```

### API

```kotlin
/**
 * Check if text is garbage and should be filtered.
 *
 * @param text The text to check
 * @param locale The locale for localized filtering (default: "en")
 * @return true if text should be filtered out
 */
fun isGarbageText(text: String, locale: String = "en"): Boolean

/**
 * Clean a label by removing garbage content.
 * Returns null if the entire text is garbage.
 *
 * @param text The text to clean
 * @param locale The locale for cleaning
 * @return Cleaned text or null if all garbage
 */
fun cleanLabel(text: String, locale: String = "en"): String?
```

---

## Icon Command Support

### Problem

Icons with `contentDescription` metadata like "Meet", "More", "Search" were not generating voice commands because the standard command generator requires 2+ words for matching.

### Solution

Icon elements are detected and allowed to have single-word commands:

```kotlin
/**
 * Check if an element is likely an icon button.
 * Icon buttons should allow single-word commands.
 *
 * @param element The element to check
 * @param locale The locale for navigation icon matching
 * @return true if element is an icon
 */
fun isIconElement(element: ElementInfo, locale: String = "en"): Boolean
```

### Detection Criteria

An element is considered an icon if:

1. **Clickable** - Must be actionable
2. **Has contentDescription** - Icons use accessibility labels
3. **No visible text** (or text matches contentDescription)
4. **Matches known navigation icons** (localized)
5. **Small size** - Typically < 150x150 pixels
6. **Icon class name** - Contains "imagebutton", "iconbutton", "actionbutton", "fab"

### Localized Navigation Icons

```kotlin
private val LOCALIZED_NAVIGATION_ICONS = mapOf(
    "en" to setOf(
        // Navigation
        "menu", "more", "options", "settings", "back", "forward",
        "home", "close", "refresh", "reload", "search", "filter", "sort",
        // Actions
        "add", "new", "create", "edit", "delete", "remove", "save", "cancel",
        "share", "send", "download", "upload", "attach", "copy", "paste",
        // Communication
        "call", "meet", "video", "camera", "mic", "mute", "unmute",
        "compose", "reply", "archive", "trash", "spam",
        // Media
        "play", "pause", "stop", "skip", "previous", "next", "volume",
        // Status
        "star", "favorite", "bookmark", "pin", "flag", "label",
        // Help
        "help", "info", "about", "feedback"
    ),
    "de" to setOf(
        "menü", "mehr", "optionen", "einstellungen", "zurück", "vorwärts",
        "startseite", "schließen", "aktualisieren", "suchen", "filtern", "sortieren",
        // ... (full list in code)
    ),
    "es" to setOf(
        "menú", "más", "opciones", "ajustes", "atrás", "adelante",
        "inicio", "cerrar", "actualizar", "buscar", "filtrar", "ordenar",
        // ... (full list in code)
    ),
    "fr" to setOf(
        "menu", "plus", "options", "paramètres", "retour", "avancer",
        "accueil", "fermer", "actualiser", "rechercher", "filtrer", "trier",
        // ... (full list in code)
    )
)
```

### Icon Command Generation

```kotlin
/**
 * Get the label for an icon element.
 * For icons, we use contentDescription directly without
 * minimum word requirements.
 *
 * @param element The icon element
 * @return The icon label or null if not suitable
 */
fun getIconLabel(element: ElementInfo): String?

/**
 * Generate commands for navigation/icon elements.
 * These get single-word commands like "Meet", "More", "Search".
 * Also generates numbered commands for icons without labels.
 *
 * @param elements List of detected icon elements
 * @param locale The locale for command generation
 * @return List of QuantizedCommand objects
 */
fun generateIconCommands(
    elements: List<ElementInfo>,
    locale: String = "en"
): List<QuantizedCommand>
```

---

## Localization

### Supported Locales

| Code | Language | Coverage |
|------|----------|----------|
| `en` | English | Full (default) |
| `de` | German | Full |
| `es` | Spanish | Full |
| `fr` | French | Full |
| `zh` | Chinese | Partial (garbage words only) |
| `ja` | Japanese | Partial (garbage words only) |

### Locale Fallback

The system automatically falls back to English if:
- Locale is not supported
- Locale-specific data is missing

```kotlin
private fun getRepetitiveWords(locale: String = "en"): Set<String> {
    val langCode = locale.take(2).lowercase()
    return LOCALIZED_REPETITIVE_WORDS[langCode]
        ?: LOCALIZED_REPETITIVE_WORDS["en"]!!
}

private fun getNavigationIcons(locale: String = "en"): Set<String> {
    val langCode = locale.take(2).lowercase()
    val localized = LOCALIZED_NAVIGATION_ICONS[langCode] ?: emptySet()
    val english = LOCALIZED_NAVIGATION_ICONS["en"]!!
    return localized + english  // Combine for better coverage
}
```

### Adding New Locales

To add support for a new locale:

1. Add garbage words to `LOCALIZED_REPETITIVE_WORDS`:
```kotlin
"it" to setOf("virgola", "punto", "trattino", "spazio", ...)
```

2. Add navigation icons to `LOCALIZED_NAVIGATION_ICONS`:
```kotlin
"it" to setOf("menu", "altro", "opzioni", "impostazioni", ...)
```

---

## API Reference

### CommandGenerator Object

```kotlin
object CommandGenerator {

    /**
     * Check if text is garbage and should be filtered.
     */
    fun isGarbageText(text: String, locale: String = "en"): Boolean

    /**
     * Clean a label by removing garbage content.
     */
    fun cleanLabel(text: String, locale: String = "en"): String?

    /**
     * Check if an element is an icon button.
     */
    fun isIconElement(element: ElementInfo, locale: String = "en"): Boolean

    /**
     * Get the label for an icon element.
     */
    fun getIconLabel(element: ElementInfo): String?

    /**
     * Generate commands for icon elements.
     */
    fun generateIconCommands(
        elements: List<ElementInfo>,
        locale: String = "en"
    ): List<QuantizedCommand>

    /**
     * Generate quantized commands from elements.
     * Includes garbage filtering and icon support.
     */
    fun generateCommand(
        element: ElementInfo,
        screenContextId: String?,
        existingCommand: QuantizedCommand? = null,
        locale: String = "en"
    ): QuantizedCommand?
}
```

---

## Usage Examples

### Basic Garbage Filtering

```kotlin
// In accessibility service
val text = accessibilityNode.text?.toString() ?: ""

if (CommandGenerator.isGarbageText(text)) {
    // Skip this element - it's garbage
    return
}

// Clean the text before generating command
val cleanedText = CommandGenerator.cleanLabel(text) ?: return
```

### Icon Command Generation

```kotlin
// Detect and handle icon elements
val elements = scrapeScreen()

elements.forEach { element ->
    if (CommandGenerator.isIconElement(element)) {
        // This is an icon - use single-word command
        val label = CommandGenerator.getIconLabel(element)
        if (label != null) {
            // Generate overlay with single word: "Meet", "More", etc.
            createOverlay(element, label)
        }
    } else {
        // Standard element - needs 2+ words
        val command = CommandGenerator.generateCommand(element, screenContextId)
        if (command != null) {
            createOverlay(element, command.commandText)
        }
    }
}
```

### Localized Filtering

```kotlin
// Get device locale
val locale = Locale.getDefault().language  // "de", "es", etc.

// Filter with locale awareness
val isGarbage = CommandGenerator.isGarbageText(text, locale)
val isIcon = CommandGenerator.isIconElement(element, locale)
```

---

## Configuration

### Constants

```kotlin
// Maximum icon size in pixels (roughly 100dp)
private const val MAX_ICON_SIZE = 150

// Minimum words for standard commands
private const val MIN_WORDS_FOR_COMMAND = 2

// Minimum words for icon commands
private const val MIN_WORDS_FOR_ICON = 1
```

### Performance

- Garbage detection: O(n) where n = word count
- Icon detection: O(1) for size check, O(n) for navigation icon lookup
- Localization: O(1) map lookup with fallback

---

## Testing

### Test Cases

1. **Garbage Text Detection**
   - Input: "comma comma com" → Filtered: true
   - Input: "btn-primary-flex" → Filtered: true
   - Input: "Send Email" → Filtered: false

2. **Icon Detection**
   - Element with contentDescription="Meet", size=48x48 → Icon: true
   - Element with text="Submit", size=200x50 → Icon: false

3. **Localization**
   - German: "Komma Komma" → Filtered: true
   - Spanish: "menú" as icon → Recognized: true

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-02 | Initial release with garbage filtering and icon commands |

---

**Author:** Augmentalis Engineering
**Module Owner:** VoiceOSCore Team
**License:** Proprietary - Augmentalis Inc.
