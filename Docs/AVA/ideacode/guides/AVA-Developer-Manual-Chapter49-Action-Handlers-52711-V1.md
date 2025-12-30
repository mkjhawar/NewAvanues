# Developer Manual - Chapter 49: Action Handlers

**Version:** 1.0
**Last Updated:** 2025-11-27
**Module:** Features/Actions
**Coverage:** 27/28 intents (96%)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Implementation Guide](#implementation-guide)
4. [Handler Registry](#handler-registry)
5. [Entity Extraction](#entity-extraction)
6. [Implemented Handlers](#implemented-handlers)
7. [Testing](#testing)
8. [Best Practices](#best-practices)

---

## Overview

The Action Handlers system provides the execution layer for AVA's intent classification system. When an intent is classified from user utterances, the corresponding action handler executes the appropriate Android system action.

### Key Features

- **27 Production Handlers**: 96% coverage of AON 3.0 intents
- **Registry Pattern**: Centralized handler registration and lookup
- **Entity Extraction**: Pattern-based extraction from natural language
- **Graceful Fallbacks**: Smart app detection with fallback mechanisms
- **Production Ready**: Comprehensive error handling and logging

### Coverage by Category

| Category | Coverage | Status |
|----------|----------|--------|
| Communication | 3/3 (100%) | ✅ Complete |
| Media | 6/6 (100%) | ✅ Complete |
| Productivity | 6/6 (100%) | ✅ Complete |
| Navigation | 5/5 (100%) | ✅ Complete |
| Device Control | 6/8 (75%) | Partial |

**Total**: 27/28 intents (96%) - Production Ready

---

## Architecture

### Component Structure

```
Universal/AVA/Features/Actions/
├── src/main/kotlin/
│   ├── IntentActionHandler.kt         # Base interface
│   ├── IntentActionHandlerRegistry.kt # Handler registry
│   ├── ActionsInitializer.kt         # Registration point
│   ├── ActionResult.kt                # Result sealed class
│   ├── entities/
│   │   └── EntityExtractor.kt         # Entity extraction
│   └── handlers/
│       ├── ProductivityActionHandlers.kt
│       ├── CommunicationActionHandlers.kt
│       ├── NavigationAndMediaHandlers.kt
│       ├── MediaControlActionHandlers.kt
│       ├── AlarmActionHandler.kt
│       └── ... (120+ VoiceOS handlers)
└── src/androidTest/kotlin/
    └── FeatureGapAnalysisTest.kt     # Coverage tests
```

### Flow Diagram

```
User Utterance
      ↓
Intent Classification (NLU)
      ↓
IntentActionHandlerRegistry.getHandler(intent)
      ↓
ActionHandler.execute(context, utterance)
      ↓
Entity Extraction (if needed)
      ↓
Android Intent Creation
      ↓
Intent Execution (startActivity/sendBroadcast)
      ↓
ActionResult (Success/Failure)
```

---

## Implementation Guide

### Creating a New Handler

#### Step 1: Implement IntentActionHandler

```kotlin
class MyCustomActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "MyCustomHandler"
    }

    override val intent = "my_custom_intent"

    override suspend fun execute(
        context: Context,
        utterance: String
    ): ActionResult {
        return try {
            Log.d(TAG, "Executing for: '$utterance'")

            // 1. Extract entities (if needed)
            val data = extractData(utterance)

            // 2. Build Android Intent
            val androidIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("myapp://action?data=$data")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 3. Execute intent
            context.startActivity(androidIntent)

            // 4. Return success
            Log.i(TAG, "Successfully executed")
            ActionResult.Success(message = "Action completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute", e)
            ActionResult.Failure(
                message = "Failed: ${e.message}",
                exception = e
            )
        }
    }

    private fun extractData(utterance: String): String {
        // Pattern matching logic
        val pattern = Regex("do (.+)", RegexOption.IGNORE_CASE)
        return pattern.find(utterance)?.groupValues?.get(1) ?: ""
    }
}
```

#### Step 2: Register Handler

Add to `ActionsInitializer.kt`:

```kotlin
IntentActionHandlerRegistry.registerAll(
    // ... existing handlers
    MyCustomActionHandler(),
)
```

#### Step 3: Create Test

Add to `FeatureGapAnalysisTest.kt`:

```kotlin
@Test
fun `my_custom_intent - PASS - handler exists`() = runTest {
    val intent = "my_custom_intent"
    val handler = IntentActionHandlerRegistry.getHandler(intent)

    assertThat(handler).isNotNull()
    // ✅ IMPLEMENTED
    // MyCustomActionHandler does X
}
```

---

## Handler Registry

### IntentActionHandlerRegistry

Centralized registry for all action handlers.

#### Key Methods

```kotlin
object IntentActionHandlerRegistry {
    // Register single handler
    fun register(handler: IntentActionHandler)

    // Register multiple handlers
    fun registerAll(vararg handlers: IntentActionHandler)

    // Get handler by intent ID
    fun getHandler(intent: String): IntentActionHandler?

    // Get all registered intent IDs
    fun getRegisteredIntents(): Set<String>

    // Clear all handlers (testing only)
    fun clear()
}
```

#### Usage Example

```kotlin
// Registration (in ActionsInitializer)
IntentActionHandlerRegistry.registerAll(
    SendTextActionHandler(),
    MakeCallActionHandler()
)

// Lookup (in ChatViewModel or IntentProcessor)
val intent = "send_text"
val handler = IntentActionHandlerRegistry.getHandler(intent)
val result = handler?.execute(context, utterance)
```

---

## Entity Extraction

### EntityExtractor Interface

Generic interface for extracting structured data from utterances.

```kotlin
interface EntityExtractor<T> {
    fun extract(utterance: String): T?
}
```

### Built-in Extractors

#### 1. QueryEntityExtractor

Extracts search queries from web search utterances.

```kotlin
object QueryEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("search for (.+)", RegexOption.IGNORE_CASE),
        Regex("google (.+)", RegexOption.IGNORE_CASE),
        Regex("what is (.+)", RegexOption.IGNORE_CASE),
        // ... 9 more patterns
    )

    override fun extract(utterance: String): String? {
        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let {
                return it.trim()
            }
        }
        return null
    }
}
```

**Usage:**
```kotlin
val query = QueryEntityExtractor.extract("search for cats")
// Result: "cats"
```

#### 2. URLEntityExtractor

Extracts URLs and auto-adds HTTPS protocol.

```kotlin
object URLEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("(?:go to|open|navigate to) (.+\\.\\w+)", RegexOption.IGNORE_CASE),
        // ... more patterns
    )

    override fun extract(utterance: String): String? {
        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { url ->
                return if (url.startsWith("http")) url else "https://$url"
            }
        }
        return null
    }
}
```

**Usage:**
```kotlin
val url = URLEntityExtractor.extract("go to youtube.com")
// Result: "https://youtube.com"
```

#### 3. PhoneNumberEntityExtractor

Extracts phone numbers in various formats.

```kotlin
object PhoneNumberEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("(?:call|dial) ([0-9-+ ]+)", RegexOption.IGNORE_CASE),
        Regex("([0-9]{3}-[0-9]{3}-[0-9]{4})"),  // 555-123-4567
        Regex("([0-9]{10})"),                    // 5551234567
        // ... more patterns
    )
}
```

#### 4. RecipientEntityExtractor

Extracts recipient information (name, phone, email).

```kotlin
data class Recipient(
    val name: String?,
    val phoneNumber: String?,
    val email: String?
)

object RecipientEntityExtractor : EntityExtractor<Recipient> {
    override fun extract(utterance: String): Recipient? {
        // Extract name, phone, or email from patterns
    }
}
```

**Usage:**
```kotlin
val recipient = RecipientEntityExtractor.extract("text mom")
// Result: Recipient(name="mom", phoneNumber=null, email=null)

val recipient2 = RecipientEntityExtractor.extract("email alice@example.com")
// Result: Recipient(name=null, phoneNumber=null, email="alice@example.com")
```

#### 5. MessageEntityExtractor

Extracts message content from "saying X" or "that Y" patterns.

```kotlin
object MessageEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("saying (.+)", RegexOption.IGNORE_CASE),
        Regex("that (.+)", RegexOption.IGNORE_CASE),
        // ... more patterns
    )
}
```

**Usage:**
```kotlin
val message = MessageEntityExtractor.extract("text mom saying I'll be late")
// Result: "I'll be late"
```

---

## Implemented Handlers

### Communication (3/3 - 100%)

#### SendTextActionHandler

**Intent:** `send_text`
**Examples:** "text mom", "send message to John saying hello"

```kotlin
override suspend fun execute(context: Context, utterance: String): ActionResult {
    val recipient = RecipientEntityExtractor.extract(utterance)
    val message = MessageEntityExtractor.extract(utterance)

    val smsUri = Uri.parse("smsto:${recipient.phoneNumber ?: recipient.name}")
    val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
        message?.let { putExtra("sms_body", it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
    return ActionResult.Success("Opening text to ${recipient.name}")
}
```

#### MakeCallActionHandler

**Intent:** `make_call`
**Examples:** "call mom", "dial 555-1234"

Uses `Intent.ACTION_DIAL` (no permission required).

#### SendEmailActionHandler

**Intent:** `send_email`
**Examples:** "email alice@example.com", "send email to bob about meeting"

Extracts recipient, subject ("about X"), and message ("saying Y").

---

### Productivity (6/6 - 100%)

#### SearchWebActionHandler

**Intent:** `search_web`
**Examples:** "search for cats", "google kotlin tutorials"

```kotlin
val query = QueryEntityExtractor.extract(utterance)
val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
    putExtra(SearchManager.QUERY, query)
}
```

#### NavigateURLActionHandler

**Intent:** `navigate_url` (custom)
**Examples:** "go to youtube.com", "open github.com"

#### CreateReminderActionHandler

**Intent:** `create_reminder`
**Examples:** "remind me to buy milk"

Opens Google Tasks or Google Keep with fallback.

#### CreateCalendarEventActionHandler

**Intent:** `create_calendar_event`
**Examples:** "schedule meeting with John"

Uses `CalendarContract.Events.CONTENT_URI`.

#### AddTodoActionHandler

**Intent:** `add_todo`
**Examples:** "add to do buy groceries", "I need to call dentist"

#### CreateNoteActionHandler

**Intent:** `create_note`
**Examples:** "take a note meeting summary"

#### CheckCalendarActionHandler

**Intent:** `check_calendar`
**Examples:** "check calendar", "what's on my calendar"

Opens calendar app via content URI or CATEGORY_APP_CALENDAR.

---

### Navigation (5/5 - 100%)

#### GetDirectionsActionHandler

**Intent:** `get_directions`
**Examples:** "directions to work", "navigate to downtown"

```kotlin
val destination = extractDestination(utterance)
val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
val intent = Intent(Intent.ACTION_VIEW, uri).apply {
    setPackage("com.google.android.apps.maps")
}
```

#### FindNearbyActionHandler

**Intent:** `find_nearby`
**Examples:** "find coffee near me", "nearby restaurants"

Uses `geo:0,0?q=place_type` URI.

#### ShowTrafficActionHandler

**Intent:** `show_traffic`
**Examples:** "show traffic", "how is traffic"

Opens Maps with `?traffic=1` parameter.

#### ShareLocationActionHandler

**Intent:** `share_location`
**Examples:** "share my location", "send my location"

Opens Google Maps for location sharing.

#### SaveLocationActionHandler

**Intent:** `save_location`
**Examples:** "save location", "bookmark this place"

Opens Maps to save/bookmark current location.

---

### Media (6/6 - 100%)

#### PlayMusicActionHandler / ResumeMusicActionHandler

**Intents:** `play_music`, `resume_media`
**Examples:** "play music", "resume", "continue playing"

Uses `AudioManager.dispatchMediaKeyEvent()` with `KEYCODE_MEDIA_PLAY`.

#### PauseMusicActionHandler

**Intent:** `pause_music`
**Examples:** "pause", "pause music"

Uses `KEYCODE_MEDIA_PAUSE`.

#### NextTrackActionHandler / PreviousTrackActionHandler

**Intents:** `skip_track`, `previous_track`
**Examples:** "next", "previous", "skip"

Uses `KEYCODE_MEDIA_NEXT` / `KEYCODE_MEDIA_PREVIOUS`.

#### PlayVideoActionHandler

**Intent:** `play_video`
**Examples:** "play video cats", "watch funny videos on youtube"

Opens YouTube app or web fallback.

---

### Device Control (6/8 - 75%)

#### SetTimerActionHandler

**Intent:** `set_timer`
**Examples:** "set timer for 10 minutes", "timer for 30 seconds"

```kotlin
val durationSeconds = extractDuration(utterance) // Handles min/sec/hours
val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
    putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
}
```

#### AlarmActionHandler

**Intent:** `set_alarm`
**Examples:** "set alarm", "wake me up at 7am"

Uses `AlarmClock.ACTION_SET_ALARM`.

**Note**: Also includes 100+ VoiceOS system handlers (volume, brightness, wifi, bluetooth, flashlight, etc.)

---

## Testing

### FeatureGapAnalysisTest

Comprehensive test suite documenting all 28 AON 3.0 intents.

```kotlin
@RunWith(AndroidJUnit4::class)
class FeatureGapAnalysisTest {

    @Before
    fun setup() {
        // Initialize handlers (happens in Application.onCreate)
        ActionsInitializer.reset()
        ActionsInitializer.initialize(
            ApplicationProvider.getApplicationContext()
        )
    }

    @Test
    fun `send_text - PASS - handler exists`() = runTest {
        val intent = "send_text"
        val handler = IntentActionHandlerRegistry.getHandler(intent)

        assertThat(handler).isNotNull()
        // ✅ IMPLEMENTED
        // SendTextActionHandler opens SMS with recipient and message
    }

    @Test
    fun `generate_coverage_report - documents all 28 intents`() = runTest {
        val allIntents = listOf(
            // Communication (3)
            "send_email", "send_text", "make_call",
            // Device Control (8)
            "control_lights", "set_volume", "set_brightness",
            "toggle_wifi", "toggle_bluetooth", "toggle_flashlight",
            "set_alarm", "set_timer",
            // Media (6)
            "play_music", "pause_media", "resume_media",
            "skip_track", "previous_track", "play_video",
            // Navigation (5)
            "get_directions", "find_nearby", "show_traffic",
            "share_location", "save_location",
            // Productivity (6)
            "create_reminder", "create_calendar_event",
            "check_calendar", "create_note", "add_todo", "search_web"
        )

        val registeredIntents = IntentActionHandlerRegistry
            .getRegisteredIntents()

        val coverage = allIntents.count { registeredIntents.contains(it) }
        val coveragePercent = (coverage * 100.0 / allIntents.size).toInt()

        println("\n=== INTENT COVERAGE REPORT ===")
        println("Total Intents: ${allIntents.size}")
        println("Implemented: $coverage")
        println("Coverage: $coveragePercent%")
        println("\nStatus by Intent:")

        allIntents.forEach { intent ->
            val status = if (registeredIntents.contains(intent)) "✅" else "❌"
            println("$status $intent")
        }
    }
}
```

### Running Tests

```bash
# Run all action handler tests
./gradlew :Universal:AVA:Features:Actions:testReleaseUnitTest

# Run instrumented tests
./gradlew :Universal:AVA:Features:Actions:connectedAndroidTest

# Generate coverage report
./gradlew :Universal:AVA:Features:Actions:testReleaseUnitTestCoverage
```

---

## Best Practices

### 1. Pattern Matching

Use multiple regex patterns for robustness:

```kotlin
val patterns = listOf(
    Regex("primary pattern (.+)", RegexOption.IGNORE_CASE),
    Regex("alternative pattern (.+)", RegexOption.IGNORE_CASE),
    Regex("fallback (.+)", RegexOption.IGNORE_CASE)
)

patterns.forEach { pattern ->
    pattern.find(utterance)?.groupValues?.getOrNull(1)?.let {
        return it.trim()
    }
}
```

### 2. Graceful Fallbacks

Always provide fallback mechanisms:

```kotlin
try {
    // Try primary app (e.g., Google Tasks)
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = Uri.parse("content://com.google.android.apps.tasks/tasks")
    }
    context.startActivity(intent)
} catch (e: Exception) {
    // Fallback to generic app
    val fallback = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(fallback, "Choose app..."))
}
```

### 3. Comprehensive Logging

Log at appropriate levels:

```kotlin
Log.d(TAG, "Processing utterance: '$utterance'")  // Debug
Log.i(TAG, "Successfully opened app")              // Info
Log.w(TAG, "Could not extract entity")             // Warning
Log.e(TAG, "Failed to execute", exception)         // Error
```

### 4. User-Friendly Messages

Provide clear, actionable error messages:

```kotlin
// Good
"I couldn't find who to text. Try: 'text mom' or 'text 555-1234'"

// Bad
"RecipientEntityExtractor returned null"
```

### 5. Intent Flags

Always include `FLAG_ACTIVITY_NEW_TASK` for intents launched from services/background:

```kotlin
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
```

### 6. KDoc Documentation

Document all handlers comprehensively:

```kotlin
/**
 * Action handler for XYZ.
 *
 * Longer description of what this handler does.
 *
 * Intent: xyz_action (category.aot)
 * Utterances: "do X", "perform Y", "execute Z"
 * Entities: required_entity (required), optional_entity (optional)
 *
 * Examples:
 * - "example 1" → Expected behavior
 * - "example 2 with param" → Expected behavior with param
 *
 * Priority: P0/P1/P2/P3
 * Effort: X hours
 */
```

### 7. Error Handling

Use sealed class for type-safe results:

```kotlin
sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : ActionResult()
}
```

---

## Common Patterns

### Android Intent Types

```kotlin
// Web Search
Intent(Intent.ACTION_WEB_SEARCH).apply {
    putExtra(SearchManager.QUERY, query)
}

// Browser
Intent(Intent.ACTION_VIEW, Uri.parse(url))

// SMS
Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipient"))

// Phone Call (no permission)
Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))

// Email
Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
    putExtra(Intent.EXTRA_SUBJECT, subject)
    putExtra(Intent.EXTRA_TEXT, message)
}

// Calendar
Intent(Intent.ACTION_INSERT).apply {
    data = CalendarContract.Events.CONTENT_URI
    putExtra(CalendarContract.Events.TITLE, title)
}

// Google Maps Navigation
Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destination"))
    .setPackage("com.google.android.apps.maps")

// Media Control
fun sendMediaKeyEvent(context: Context, keyCode: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val eventTime = SystemClock.uptimeMillis()

    val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
    audioManager.dispatchMediaKeyEvent(downEvent)

    val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
    audioManager.dispatchMediaKeyEvent(upEvent)
}
```

---

## Troubleshooting

### Handler Not Found

```kotlin
val handler = IntentActionHandlerRegistry.getHandler("my_intent")
if (handler == null) {
    // Check: Is handler registered in ActionsInitializer?
    // Check: Is ActionsInitializer.initialize() called in Application.onCreate?
    // Check: Does intent ID match exactly?
}
```

### Entity Extraction Failing

```kotlin
val entity = MyExtractor.extract(utterance)
if (entity == null) {
    // Add more regex patterns
    // Test patterns with regex101.com
    // Log the utterance to see what's being passed
}
```

### Intent Not Launching

```kotlin
try {
    context.startActivity(intent)
} catch (e: ActivityNotFoundException) {
    // App not installed - provide fallback
    // Or show user-friendly error message
}
```

---

## Future Enhancements

### Planned Improvements

1. **Advanced Entity Extraction**
   - Date/time parsing with natural language
   - Location coordinates extraction
   - Multi-entity extraction

2. **Permission Handling**
   - Runtime permission requests
   - Permission status checking
   - Graceful permission denial handling

3. **Deep Linking**
   - Direct app state navigation
   - Pre-filled forms
   - Action parameters

4. **Smart Home Integration**
   - Google Home API
   - Alexa integration
   - control_lights implementation

---

## Related Documentation

- [Chapter 34: Intent Management](Developer-Manual-Chapter34-Intent-Management.md)
- [Chapter 48: AON 3.0 Semantic Ontology](Developer-Manual-Chapter48-AON-3.0-Semantic-Ontology.md)
- [ADR-003: ONNX NLU Integration](architecture/android/ADR-003-ONNX-NLU-Integration.md)
- [CHANGELOG.md](../CHANGELOG.md) - Release history (v1.2.0, v1.3.0, v1.4.0)

---

**Version:** 1.0
**Last Updated:** 2025-11-27
**Coverage:** 27/28 intents (96%)
**Status:** ✅ Production Ready
