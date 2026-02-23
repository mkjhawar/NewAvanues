# Chapter 110: Unified Command Architecture — VoiceOSCore + IntentActions

## Overview

The Avanues command system uses **two complementary dispatch pipelines** with clean semantic separation:

| System | Scope | Interface | Context | Examples |
|--------|-------|-----------|---------|----------|
| **VoiceOSCore** | Control what's ON screen | `IHandler` | AccessibilityService | Gestures, system controls, module commands, media, text |
| **IntentActions** | Interact WITH the world | `IIntentAction` | Activity | Launch apps, email, call, navigate, search, alarms |

Both systems are accessible from any input source (voice, AI chat, macros). Macros can compose steps from both systems.

---

## Architecture Diagram

```
┌───────────────────────────────────────────────────┐
│         INPUT SOURCES                              │
│  Voice Speech → ISpeechEngine                      │
│  AI Chat → NLU IntentClassifier                    │
│  Macro Step → MacroExecutor                        │
│  Scheduled Task → WorkManager                      │
└──────────────────────┬────────────────────────────┘
                       │
                       ▼
            ┌─────────────────────┐
            │   NLU Classifier    │
            │  classifyAndRoute() │
            └─────────┬───────────┘
                      │
           ┌──────────┴──────────┐
           │                     │
           ▼                     ▼
   ┌───────────────┐    ┌────────────────┐
   │  VoiceOSCore  │    │  IntentActions  │
   │  IHandler     │    │  IIntentAction  │
   │               │    │                 │
   │ ACCESSIBILITY │    │ COMMUNICATION   │
   │  Gestures     │    │  SendEmail      │
   │  Scroll       │    │  SendText       │
   │  Cursor       │    │  MakeCall       │
   │               │    │                 │
   │ SYSTEM        │    │ NAVIGATION      │
   │  Media keys   │    │  GetDirections  │
   │  Brightness   │    │  FindNearby     │
   │  WiFi/BT      │    │  ShowTraffic    │
   │  Flashlight   │    │                 │
   │               │    │ PRODUCTIVITY    │
   │ MODULE        │    │  SetAlarm       │
   │  Note cmds    │    │  SetTimer       │
   │  Cockpit cmds │    │  CreateReminder │
   │  Camera cmds  │    │  CalendarEvent  │
   │  PDF cmds     │    │                 │
   │  Video cmds   │    │ SEARCH          │
   │  Cast cmds    │    │  WebSearch      │
   │  Draw cmds    │    │  Calculate      │
   │               │    │  NavigateURL    │
   │ TEXT          │    │                 │
   │  Copy/Paste   │    │ MEDIA_LAUNCH    │
   │  Select All   │    │  OpenApp        │
   │  Dictation    │    │  PlayVideo      │
   │               │    │  OpenBrowser    │
   └───────┬───────┘    └───────┬────────┘
           │                    │
           └──── MACROS ────────┘
                (compose both)
```

---

## VoiceOSCore: IHandler System

### Interface Contract

```kotlin
// Modules/VoiceOSCore/src/commonMain/.../handler/IHandler.kt
interface IHandler {
    val category: CommandCategory
    val supportedActions: Set<ActionType>

    suspend fun canHandle(command: QuantizedCommand): Boolean
    suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult
}
```

### Handler Registry

`HandlerRegistry` uses **priority-based category scanning**. When a command arrives, it iterates handlers in priority order:

1. **Domain-aware static match** — checks active domain (web, notes, cockpit)
2. **Dynamic command lookup** — CommandRegistry populated from screen scraping
3. **Static command match** — StaticCommandRegistry from VOS database
4. **Handler category match** — HandlerRegistry.findHandler by category
5. **Voice interpreter fallback** — keyword mapping

### Registered Handlers (17 total)

| Handler | Category | Command Count | What It Does |
|---------|----------|---------------|-------------|
| `MediaHandler` | MEDIA | 13 | Play, pause, next, previous, volume, mute via AudioManager/MediaSession |
| `ScreenHandler` | SYSTEM | 20 | Brightness, WiFi, Bluetooth, flashlight, screenshot, rotation, settings |
| `TextHandler` | TEXT | 8 | Copy, paste, cut, select all, delete, undo |
| `InputHandler` | INPUT | 6 | Show/hide keyboard, tab, enter |
| `AppControlHandler` | APP | 4 | Close app, exit, quit, force stop |
| `ReadingHandler` | ACCESSIBILITY | 7 | Read screen, read aloud, stop reading, describe |
| `VoiceControlHandler` | VOICE | 16 | Start/stop listening, dictation, speech mode, help |
| `SystemHandler` | NAVIGATION | varies | Home, back, recent apps, power menu, lock screen |
| `AndroidGestureHandler` | GESTURE | varies | Swipe, scroll, tap, double-tap, long-press, pinch, drag |
| `AndroidCursorHandler` | CURSOR | varies | Show/hide/center cursor, hand/normal mode |
| `WebCommandHandler` | WEB | 45 | All browser + web gesture commands |
| `NoteCommandHandler` | NOTE | 30+ | Bold, italic, heading, bullet, dictate, save |
| `CockpitCommandHandler` | COCKPIT | 20+ | Add/close frame, layout modes, navigate frames |
| `ImageCommandHandler` | IMAGE | 18 | Gallery, filters, rotate, zoom, share |
| `VideoCommandHandler` | VIDEO | 12 | Play/pause, seek, speed, fullscreen, loop |
| `CastCommandHandler` | CAST | 5 | Start/stop casting, connect, quality |
| `AICommandHandler` | AI | 5 | Summarize, AI chat, teach, clear context |

### ModuleCommandCallbacks

Content modules register volatile executor lambdas when their UI is active:

```kotlin
// In a content module's Composable (e.g., NoteEditor)
DisposableEffect(Unit) {
    ModuleCommandCallbacks.noteExecutor = { actionType, metadata ->
        when (actionType) {
            ActionType.FORMAT_BOLD -> { controller.bold(); HandlerResult.success("Bold applied") }
            ActionType.SAVE_NOTE -> { controller.save(); HandlerResult.success("Note saved") }
            // ...
        }
    }
    onDispose { ModuleCommandCallbacks.noteExecutor = null }
}
```

The corresponding `NoteCommandHandler` reads the executor:

```kotlin
class NoteCommandHandler : IHandler {
    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        val executor = ModuleCommandCallbacks.noteExecutor
            ?: return HandlerResult.failure("NoteAvanue not active")
        return executor(command.actionType, params)
    }
}
```

---

## IntentActions: IIntentAction System

### PlatformContext (KMP Expect/Actual)

`PlatformContext` is a KMP expect/actual wrapper providing platform-specific context to intent actions:

```kotlin
// commonMain — type declaration only
expect class PlatformContext

// androidMain — wraps Android Context (Kotlin 2.1: actual typealias no longer permitted)
actual class PlatformContext(val android: Context)

// iosMain / desktopMain — empty stubs for future platform support
actual class PlatformContext
```

**Usage in Android actions:** Functions receive `platformCtx: PlatformContext` and rebind via `val context = platformCtx.android` to access Android `Context` APIs (`startActivity`, `packageManager`, etc.).

**Constructing on Android:** Callers pass `PlatformContext(activity)` or `PlatformContext(applicationContext)`.

### Interface Contract

```kotlin
// Modules/IntentActions/src/commonMain/.../IIntentAction.kt
interface IIntentAction {
    val intentId: String
    val category: IntentCategory
    val requiredEntities: List<EntityType>

    suspend fun execute(
        context: PlatformContext,
        entities: ExtractedEntities
    ): IntentResult
}
```

### IntentCategory Enum

```kotlin
enum class IntentCategory {
    COMMUNICATION,    // email, SMS, phone call
    NAVIGATION,       // maps, directions, nearby places
    PRODUCTIVITY,     // alarms, timers, reminders, calendar, todos
    SEARCH,           // web search, URL navigation, math calculation
    MEDIA_LAUNCH,     // open app, play video, open browser
    SYSTEM_SETTINGS   // open specific settings subsection
}
```

### IntentResult

```kotlin
sealed class IntentResult {
    data class Success(val message: String, val data: Any? = null) : IntentResult()
    data class NeedsMoreInfo(val missingEntity: EntityType, val prompt: String) : IntentResult()
    data class Failed(val reason: String) : IntentResult()
}
```

### Entity Extraction

IntentActions differ from VoiceOSCore actions in that they require **entity extraction** from natural language. The NLU layer extracts structured data before dispatch:

```kotlin
data class ExtractedEntities(
    val query: String? = null,        // "weather in San Francisco"
    val url: String? = null,          // "google.com"
    val phoneNumber: String? = null,  // "555-1234"
    val recipient: Recipient? = null, // name + contact info
    val message: String? = null,      // "I'll be late"
    val location: String? = null,     // "Costco near me"
    val time: String? = null,         // "7am"
    val duration: String? = null      // "30 minutes"
)
```

Entity extractors are **pure Kotlin regex** in commonMain — no platform dependencies:

| Extractor | Pattern | Example Input → Output |
|-----------|---------|----------------------|
| `QueryEntityExtractor` | Strips "search for", "look up", etc. | "search for weather" → "weather" |
| `URLEntityExtractor` | Detects domain patterns | "go to google.com" → "google.com" |
| `PhoneNumberEntityExtractor` | Digit patterns with separators | "call 555-123-4567" → "5551234567" |
| `RecipientEntityExtractor` | Name after "to", "tell", "text" | "text mom saying hello" → Recipient("mom") |
| `MessageEntityExtractor` | Content after "saying", "that", message verbs | "text mom saying hello" → "hello" |

### IntentActionRegistry

```kotlin
object IntentActionRegistry {
    private val actions = mutableMapOf<String, IIntentAction>()

    fun register(action: IIntentAction) { actions[action.intentId] = action }
    fun findByIntent(intentId: String): IIntentAction? = actions[intentId]
    fun getByCategory(category: IntentCategory): List<IIntentAction> =
        actions.values.filter { it.category == category }
    fun getAll(): List<IIntentAction> = actions.values.toList()

    suspend fun execute(
        intentId: String,
        context: PlatformContext,
        entities: ExtractedEntities
    ): IntentResult {
        val action = actions[intentId] ?: return IntentResult.Failed("Unknown intent: $intentId")
        return action.execute(context, entities)
    }
}
```

### Registered IntentActions (26)

| Intent ID | Category | Required Entities | Android Implementation |
|-----------|----------|-------------------|----------------------|
| `send_email` | COMMUNICATION | recipient, message | `ACTION_SENDTO` with mailto: URI |
| `send_text` | COMMUNICATION | recipient, message | `ACTION_SENDTO` with smsto: URI |
| `make_call` | COMMUNICATION | phoneNumber | `ACTION_DIAL` |
| `get_directions` | NAVIGATION | location | Google Maps `google.navigation:q=` |
| `find_nearby` | NAVIGATION | query | Google Maps `geo:0,0?q=` |
| `show_traffic` | NAVIGATION | — | Google Maps traffic layer |
| `share_location` | NAVIGATION | — | Maps share intent |
| `save_location` | NAVIGATION | — | Maps bookmark |
| `set_alarm` | PRODUCTIVITY | time | `AlarmClock.ACTION_SET_ALARM` |
| `set_timer` | PRODUCTIVITY | duration | `AlarmClock.ACTION_SET_TIMER` |
| `create_reminder` | PRODUCTIVITY | message, time | Calendar/reminder intent |
| `create_calendar_event` | PRODUCTIVITY | message, time | `CalendarContract.Events.CONTENT_URI` |
| `add_todo` | PRODUCTIVITY | message | Notes/todo intent |
| `create_note` | PRODUCTIVITY | message | Notes intent |
| `check_calendar` | PRODUCTIVITY | — | Calendar view intent |
| `get_time` | PRODUCTIVITY | — | `AlarmClock.ACTION_SHOW_ALARMS` |
| `search_web` | SEARCH | query | Browser with search URL |
| `navigate_url` | SEARCH | url | `ACTION_VIEW` with URI |
| `calculate` | SEARCH | query | MathCalculator (local, no intent) |
| `get_weather` | SEARCH | query | Weather app/browser fallback |
| `play_video` | MEDIA_LAUNCH | query | YouTube app/browser |
| `resume_music` | MEDIA_LAUNCH | — | Music app launch |
| `open_browser` | MEDIA_LAUNCH | — | `ACTION_VIEW` |
| `open_app` | MEDIA_LAUNCH | query | `getLaunchIntentForPackage()` |
| `open_settings` | SYSTEM_SETTINGS | — | `Settings.ACTION_SETTINGS` |
| `open_settings_subsection` | SYSTEM_SETTINGS | query | Specific settings intent |

---

## Unified Dispatch Flow

### NLU classifyAndRoute()

The NLU classifier determines whether input should go to VoiceOSCore or IntentActions:

```kotlin
// Modules/IntentActions/src/commonMain/.../IntentClassifier.kt
sealed class CommandRoute {
    data class VoiceCommand(val phrase: String, val confidence: Float) : CommandRoute()
    data class IntentAction(val intentId: String, val entities: ExtractedEntities) : CommandRoute()
    data class Ambiguous(val voiceOption: VoiceCommand, val intentOption: IntentAction) : CommandRoute()
}

suspend fun classifyAndRoute(utterance: String): CommandRoute {
    // 1. Check if it matches a known VoiceOSCore command (static + dynamic)
    val voiceMatch = classifyCommand(utterance, allKnownCommands)

    // 2. Check if it matches an IntentAction pattern
    val intentMatch = classifyIntent(utterance, intentPatterns)

    // 3. Route based on confidence
    return when {
        voiceMatch.confidence > 0.8 -> CommandRoute.VoiceCommand(voiceMatch.phrase, voiceMatch.confidence)
        intentMatch.confidence > 0.8 -> CommandRoute.IntentAction(intentMatch.intentId, extractEntities(utterance))
        voiceMatch.confidence > intentMatch.confidence -> CommandRoute.VoiceCommand(voiceMatch.phrase, voiceMatch.confidence)
        else -> CommandRoute.IntentAction(intentMatch.intentId, extractEntities(utterance))
    }
}
```

### End-to-End Examples

**Example 1: "Scroll down"**
```
Input: "scroll down"
→ NLU: classifyAndRoute → VoiceCommand("scroll down", 0.99)
→ VoiceOSCore: ActionCoordinator.processVoiceCommand("scroll down")
→ HandlerRegistry: AndroidGestureHandler.execute(SCROLL_DOWN)
→ AccessibilityService: dispatchGesture(swipe path)
```

**Example 2: "Send email to John about the meeting"**
```
Input: "send email to John about the meeting"
→ NLU: classifyAndRoute → IntentAction("send_email", {recipient: "John", message: "the meeting"})
→ IntentActionRegistry: SendEmailAction.execute(context, entities)
→ Android: startActivity(Intent(ACTION_SENDTO, "mailto:john@...").putExtra(SUBJECT, "the meeting"))
```

**Example 3: "Bold" (in NoteAvanue)**
```
Input: "bold"
→ NLU: classifyAndRoute → VoiceCommand("bold", 0.95)
→ VoiceOSCore: ActionCoordinator → domain=NOTE → NoteCommandHandler
→ ModuleCommandCallbacks.noteExecutor(FORMAT_BOLD, {})
→ NoteAvanue: controller.bold()
```

---

## Macro Composition

Macros can compose steps from both VoiceOSCore and IntentActions, enabling powerful automation:

### MacroStep Model

```kotlin
// Modules/VoiceOSCore/src/commonMain/.../macro/MacroStep.kt
sealed class MacroStep {
    data class VoiceAction(val command: String) : MacroStep()
    data class IntentStep(val intentId: String, val entities: Map<String, String>) : MacroStep()
    data class Delay(val ms: Long) : MacroStep()
    data class Conditional(val check: String, val thenSteps: List<MacroStep>, val elseSteps: List<MacroStep> = emptyList()) : MacroStep()
}
```

### Example Macro: "Start Field Inspection"

```kotlin
val fieldInspection = listOf(
    MacroStep.IntentStep("open_app", mapOf("query" to "com.augmentalis.avanues")),
    MacroStep.Delay(1000),
    MacroStep.VoiceAction("open cockpit"),
    MacroStep.VoiceAction("add camera frame"),
    MacroStep.VoiceAction("add note frame"),
    MacroStep.VoiceAction("layout split left"),
    MacroStep.IntentStep("get_directions", mapOf("location" to "job site")),
)
```

### MacroExecutor

```kotlin
class MacroExecutor(
    private val actionCoordinator: ActionCoordinator,
    private val intentRegistry: IntentActionRegistry,
    private val platformContext: PlatformContext
) {
    suspend fun execute(steps: List<MacroStep>): List<StepResult> {
        return steps.map { step ->
            when (step) {
                is MacroStep.VoiceAction -> {
                    val result = actionCoordinator.processVoiceCommand(step.command, 1.0f)
                    StepResult(step, result.success)
                }
                is MacroStep.IntentStep -> {
                    val entities = ExtractedEntities.fromMap(step.entities)
                    val result = intentRegistry.execute(step.intentId, platformContext, entities)
                    StepResult(step, result is IntentResult.Success)
                }
                is MacroStep.Delay -> {
                    delay(step.ms)
                    StepResult(step, true)
                }
                is MacroStep.Conditional -> {
                    val conditionMet = evaluateCondition(step.check)
                    val branch = if (conditionMet) step.thenSteps else step.elseSteps
                    val branchResults = execute(branch)
                    StepResult(step, branchResults.all { it.success }, "Ran ${branchResults.size} steps")
                }
            }
        }
    }
}
```

---

## Migration from Actions Module

### What Was Removed

The legacy `Modules/Actions/` module contained ~147 handlers. Of these:

| Category | Count | Disposition |
|----------|-------|------------|
| VoiceOS IPC routing stubs | 76 | **Deleted** — VoiceOSCore handles directly, no IPC needed |
| Duplicate system/media/navigation | 18 | **Deleted** — VoiceOSCore already has equivalent handlers |
| Unique intent-based handlers | 24 | **Migrated** → `IIntentAction` in AI/NLU module |
| Entity extractors | 6 | **Migrated** → AI/NLU commonMain (pure Kotlin) |
| MathCalculator | 1 | **Migrated** → AI/NLU commonMain (pure Kotlin) |
| DuckDuckGoSearchService | 1 | **Migrated** → AI/NLU androidMain |
| Framework (Registry, Router, Manager) | ~5 | **Replaced** by IntentActionRegistry |

### Why the Split Works

1. **VoiceOSCore runs in AccessibilityService context** — it has direct access to screen nodes, gesture dispatch, global actions (home/back/recents), audio manager. It does NOT need Activity context.

2. **IntentActions run in Activity context** — they launch external apps via `startActivity()`, use NLU entity extraction, and return informational results. They do NOT need accessibility node access.

3. **No IPC overhead** — Both systems run in the same app process. VoiceOSCore calling IntentActions for macros is a direct function call, not AIDL IPC.

4. **AI gets full access** — The NLU classifier routes to both systems, so the AI can do everything: gestures + app launches + module commands.

---

## Adding New Commands

### Adding a VoiceOSCore Handler

1. Create `IHandler` implementation in `Modules/VoiceOSCore/src/androidMain/.../handlers/`
2. Register in `AndroidHandlerFactory.createHandlers()`
3. Add `ActionType` entries for supported commands
4. Add voice phrases to VOS seed data

### Adding an IntentAction

1. Create `IIntentAction` implementation in `Modules/IntentActions/src/androidMain/.../actions/`
2. Register in `IntentActionsInitializer`
3. Add entity extraction patterns if needed
4. **Use `UriSanitizer`** for any URI construction from user input
5. **Use `entities.toSafeString()`** in all Log statements (never log raw entities)

### Adding a Module Command (Content Module)

1. Add `ActionType` entries in VoiceOSCore commonMain
2. Create handler in `Modules/VoiceOSCore/src/androidMain/.../handlers/`
3. Add `ModuleCommandCallbacks.xxxExecutor` slot
4. Register executor in module's Composable via `DisposableEffect`
5. Add voice phrases to VOS seed data

---

## Key Files Reference

| File | Location | Purpose |
|------|----------|---------|
| `IHandler.kt` | `Modules/VoiceOSCore/src/commonMain/.../handler/` | Handler interface |
| `HandlerRegistry.kt` | `Modules/VoiceOSCore/src/commonMain/.../handler/` | Priority-based handler lookup |
| `ActionCoordinator.kt` | `Modules/VoiceOSCore/src/commonMain/.../actions/` | Main dispatch orchestrator |
| `ModuleCommandCallbacks.kt` | `Modules/VoiceOSCore/src/androidMain/.../handlers/` | Volatile executor slots |
| `IIntentAction.kt` | `Modules/IntentActions/src/commonMain/.../` | IntentAction interface |
| `IntentActionRegistry.kt` | `Modules/IntentActions/src/commonMain/.../` | IntentAction lookup + execution |
| `IntentActionsInitializer.kt` | `Modules/IntentActions/src/androidMain/.../` | Registers all 26 actions |
| `UriSanitizer.kt` | `Modules/IntentActions/src/commonMain/.../` | URI injection prevention |
| `ExtractedEntities.kt` | `Modules/IntentActions/src/commonMain/.../` | Entity container + toSafeString() |
| `EntityExtractor.kt` | `Modules/IntentActions/src/commonMain/.../extractors/` | Entity extraction (pure Kotlin) |
| `MacroStep.kt` | `Modules/VoiceOSCore/src/commonMain/.../macro/` | Macro composition model |

---

## Security Guidelines for IntentActions

### URI Sanitization (Mandatory)

All IntentActions that construct URIs from user input MUST use `UriSanitizer`:

| URI Type | Sanitizer Method | What It Blocks |
|----------|-----------------|----------------|
| Web URLs | `sanitizeWebUrl()` | `javascript:`, `intent:`, `data:`, `file:`, `content:` schemes; upgrades http→https |
| Email | `isValidEmail()` | `?`, `&`, `#` in address (BCC/body injection) |
| Phone | `sanitizePhoneNumber()` | USSD codes (`*`, `#`), non-phone characters |
| SMS | `sanitizeSmsAddress()` | Same as phone |

### PII Logging (Mandatory)

Never log raw `ExtractedEntities`. Always use `entities.toSafeString()` which masks:
- `phoneNumber` → `phone=***`
- `recipientName` → `recipient=***`
- `recipientEmail` → `email=***`
- `message` → `message=***`

Safe fields shown as presence flags: `query=present`, `url=present`, `app=<name>`.

### Thread Safety

`IntentActionsInitializer.initialized` is `@Volatile` for correct double-checked locking.

---

*Chapter 110 | Unified Command Architecture | 2026-02-23 (updated with security guidelines)*
