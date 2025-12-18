# AVA Developer Manual - Chapter: Actions Module & VoiceOS IPC

**Version**: 1.0
**Last Updated**: 2025-11-17
**Authors**: AVA AI Team

---

## Chapter Overview

This chapter covers the Actions module, which handles intent execution and VoiceOS IPC integration. It explains how commands are routed between local execution (AVA) and accessibility service execution (VoiceOS).

---

## 1. Actions Module Architecture

### 1.1 Module Location

```
Universal/AVA/Features/Actions/
├── src/main/kotlin/com/augmentalis/ava/features/actions/
│   ├── ActionResult.kt              # Result types
│   ├── ActionsInitializer.kt        # Handler registration
│   ├── ActionsManager.kt            # Unified execution interface
│   ├── IntentActionHandler.kt       # Handler interface
│   ├── IntentActionHandlerRegistry.kt # Handler registry
│   ├── IntentRouter.kt              # Routing decision logic
│   ├── VoiceOSConnection.kt         # IPC client
│   └── handlers/                    # Handler implementations
│       ├── TimeActionHandler.kt
│       ├── AlarmActionHandler.kt
│       ├── WeatherActionHandler.kt
│       ├── OpenSettingsActionHandler.kt
│       ├── OpenAppActionHandler.kt
│       ├── SystemControlActionHandler.kt
│       ├── ExtendedSystemControlHandlers.kt
│       ├── MediaControlActionHandlers.kt
│       ├── NavigationActionHandlers.kt
│       └── SettingsActionHandlers.kt
```

### 1.2 Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| `IntentActionHandler` | Interface for all handlers |
| `IntentActionHandlerRegistry` | Stores and retrieves handlers |
| `ActionsInitializer` | Registers all handlers at startup |
| `ActionsManager` | Routes and executes actions |
| `IntentRouter` | Decides AVA vs VoiceOS routing |
| `VoiceOSConnection` | IPC client for VoiceOS |

---

## 2. Intent Routing System

### 2.1 Routing Categories

**AVA-Capable Categories** (Execute Locally):
```kotlin
val AVA_CAPABLE_CATEGORIES = setOf(
    "connectivity",     // WiFi, Bluetooth on/off
    "volume",          // Volume up/down/mute
    "media",           // Play, pause, skip tracks
    "system",          // App launching, settings
    "navigation",      // App navigation
    "productivity",    // Notes, reminders
    "smart_home",      // Smart device control
    "information"      // Time, weather, etc.
)
```

**VoiceOS-Only Categories** (Require Accessibility Service):
```kotlin
val VOICEOS_ONLY_CATEGORIES = setOf(
    "gesture",         // Swipe, drag gestures
    "cursor",          // Cursor movement
    "scroll",          // Scroll up/down
    "swipe",           // Swipe gestures
    "drag",            // Drag operations
    "keyboard",        // Keyboard control
    "editing",         // Text selection, copy/paste
    "gaze",            // Gaze tracking
    "overlays",        // Overlay UI management
    "dialog",          // Dialog interaction
    "menu",            // Menu navigation
    "dictation",       // Dictation mode
    "notifications",   // Notification handling
    "settings"         // Accessibility settings
)
```

### 2.2 Routing Decision Flow

```
Intent Classified by NLU
         ↓
┌─────────────────────────────────────┐
│  Does handler exist in Registry?    │
└─────────────────────────────────────┘
    YES ↓                    NO ↓
[Execute via Handler]  ┌──────────────────────┐
                       │ Get intent category  │
                       └──────────────────────┘
                               ↓
                ┌──────────────────────────────┐
                │ Is AVA_CAPABLE category?     │
                └──────────────────────────────┘
                    YES ↓           NO ↓
              [FallbackToLLM]  ┌─────────────────┐
                               │ Is VoiceOS      │
                               │ available?      │
                               └─────────────────┘
                                YES ↓      NO ↓
                          [Route to    [Return
                           VoiceOS]     Error]
```

---

## 3. VoiceOS IPC Integration

### 3.1 ContentProvider Contract

**Authority**: `com.avanues.voiceos.provider`

**Endpoints**:

| Method | URI | Purpose |
|--------|-----|---------|
| INSERT | `/execute_command` | Queue command for execution |
| QUERY | `/execution_result/{id}` | Poll for result |

### 3.2 Execute Command Request

```kotlin
val contentValues = ContentValues().apply {
    put("command_id", "scroll_down")           // Intent ID
    put("parameters", "{\"amount\": 100}")     // JSON parameters
    put("requested_by", "com.augmentalis.ava") // Calling package
}

val resultUri = contentResolver.insert(
    Uri.parse("content://com.avanues.voiceos.provider/execute_command"),
    contentValues
)
// Returns: content://com.avanues.voiceos.provider/12345 (execution_id)
```

### 3.3 Poll for Result

```kotlin
val cursor = contentResolver.query(
    Uri.parse("content://com.avanues.voiceos.provider/execution_result/$executionId"),
    arrayOf("status", "message", "executed_steps", "execution_time_ms", "failed_at_step"),
    null, null, null
)

// Status values: pending, executing, success, error
```

### 3.4 VoiceOSConnection Implementation

```kotlin
class VoiceOSConnection(private val context: Context) {

    companion object {
        private const val AUTHORITY = "com.avanues.voiceos.provider"
        private const val POLL_INTERVAL_MS = 100L
        private const val EXECUTION_TIMEOUT_MS = 30_000L
    }

    suspend fun executeCommand(
        intent: String,
        category: String,
        parameters: Map<String, String> = emptyMap()
    ): CommandResult {
        // 1. Check VoiceOS availability
        if (!isVoiceOSInstalled()) {
            return CommandResult.Failure("VoiceOS not installed")
        }
        if (!isAccessibilityServiceRunning()) {
            return CommandResult.Failure("Accessibility service not running")
        }

        // 2. Insert command request
        val resultUri = context.contentResolver.insert(
            EXECUTE_COMMAND_URI, contentValues
        )

        // 3. Poll for result
        return pollForResult(executionId)
    }

    fun isAccessibilityServiceRunning(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("com.avanues.voiceos") == true
    }
}
```

---

## 4. Handler Implementation

### 4.1 Handler Interface

```kotlin
interface IntentActionHandler {
    val intent: String  // e.g., "show_time", "wifi_on"

    suspend fun execute(
        context: Context,
        utterance: String
    ): ActionResult
}
```

### 4.2 ActionResult Types

```kotlin
sealed class ActionResult {
    data class Success(
        val message: String,
        val data: Map<String, Any>? = null
    ) : ActionResult()

    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : ActionResult()
}
```

### 4.3 Example: Local Handler

```kotlin
class TimeActionHandler : IntentActionHandler {
    override val intent = "show_time"

    override suspend fun execute(
        context: Context,
        utterance: String
    ): ActionResult {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault())
            .format(Date())
        return ActionResult.Success("It's $time")
    }
}
```

### 4.4 Example: VoiceOS-Routing Handler

```kotlin
class GoBackActionHandler : IntentActionHandler {
    override val intent = "go_back"

    override suspend fun execute(
        context: Context,
        utterance: String
    ): ActionResult {
        val voiceOS = VoiceOSConnection(context)
        val result = voiceOS.executeCommand(intent, "navigation")

        return when (result) {
            is VoiceOSConnection.CommandResult.Success ->
                ActionResult.Success(result.message)
            is VoiceOSConnection.CommandResult.Failure ->
                ActionResult.Failure(result.error)
        }
    }
}
```

---

## 5. Registered Handlers (52 Total)

### 5.1 Core Handlers (3)

| Intent | Handler | Description |
|--------|---------|-------------|
| `show_time` | TimeActionHandler | Returns current time |
| `set_alarm` | AlarmActionHandler | Opens alarm app |
| `check_weather` | WeatherActionHandler | Shows weather info |

### 5.2 Settings Handlers (8)

| Intent | Handler | Description |
|--------|---------|-------------|
| `open_settings` | OpenSettingsActionHandler | Opens Settings app |
| `open_app` | OpenAppActionHandler | Opens app by name |
| `open_security` | OpenSecurityActionHandler | Opens security settings |
| `open_connection` | OpenConnectionActionHandler | Opens network settings |
| `open_sound` | OpenSoundActionHandler | Opens sound settings |
| `open_display` | OpenDisplayActionHandler | Opens display settings |
| `open_about` | OpenAboutActionHandler | Opens about device |
| `quick_settings` | QuickSettingsActionHandler | Opens quick settings |

### 5.3 System Control Handlers (18)

| Intent | Handler | Behavior |
|--------|---------|----------|
| `wifi_on/off` | Wifi*ActionHandler | Opens WiFi settings |
| `bluetooth_on/off` | Bluetooth*ActionHandler | Opens Bluetooth settings |
| `volume_up/down/mute` | Volume*ActionHandler | Adjusts system volume |
| `brightness_up/down` | Brightness*ActionHandler | Adjusts brightness |
| `flashlight_on/off` | Flashlight*ActionHandler | Toggles torch |
| `airplane_mode_on/off` | AirplaneMode*ActionHandler | Opens airplane settings |
| `battery_status` | BatteryStatusActionHandler | Returns battery info |
| `lock_screen` | LockScreenActionHandler | Locks device (or routes to VoiceOS) |
| `screenshot` | ScreenshotActionHandler | Routes to VoiceOS |

### 5.4 Media Control Handlers (6)

| Intent | Handler | Description |
|--------|---------|-------------|
| `play_music` | PlayMusicActionHandler | Sends play media key |
| `pause_music` | PauseMusicActionHandler | Sends pause media key |
| `next_track` | NextTrackActionHandler | Sends next media key |
| `previous_track` | PreviousTrackActionHandler | Sends previous media key |
| `shuffle_on` | ShuffleOnActionHandler | App-specific |
| `repeat_mode` | RepeatModeActionHandler | App-specific |

### 5.5 Navigation Handlers (12)

| Intent | Handler | Behavior |
|--------|---------|----------|
| `go_home` | GoHomeActionHandler | Goes to home screen |
| `navigate_home` | NavigateHomeActionHandler | Alias for go_home |
| `go_back` | GoBackActionHandler | Routes to VoiceOS |
| `back` | BackActionHandler | Alias for go_back |
| `recent_apps` | RecentAppsActionHandler | Routes to VoiceOS |
| `open_recent_apps` | OpenRecentAppsActionHandler | Alias |
| `notifications` | NotificationsActionHandler | Expands notifications |
| `show_notifications` | ShowNotificationsActionHandler | Alias |
| `hide_notifications` | HideNotificationsActionHandler | Collapses panel |
| `open_browser` | OpenBrowserActionHandler | Opens browser |
| `menu` | MenuActionHandler | Routes to VoiceOS |
| `return_to_dashboard` | ReturnToDashboardActionHandler | Alias for go_home |

---

## 6. VoiceOS-Only Intents (69)

These intents don't have local handlers and route directly to VoiceOS:

### 6.1 Cursor Controls
`center_cursor`, `show_cursor`, `hide_cursor`, `hand_cursor`, `normal_cursor`, `change_cursor`

### 6.2 Gestures
`swipe_up`, `swipe_down`, `swipe_left`, `swipe_right`, `pinch_open`, `pinch_close`

### 6.3 Scroll
`scroll_up`, `scroll_down`

### 6.4 Clicks
`single_click`, `double_click`, `long_press`, `select`

### 6.5 Keyboard
`backspace`, `enter`, `clear_text`, `open_keyboard`, `close_keyboard`, `hide_keyboard`, `dismiss_keyboard`, `change_keyboard`, `switch_keyboard`, `keyboard_mode`, `keyboard_mode_change`, `keyboard_change`

### 6.6 Drag Operations
`drag_start`, `drag_stop`, `drag_up_down`

### 6.7 Dictation
`dictation`, `end_dictation`

### 6.8 UI Controls
`show_number`, `hide_number`, `show_help`, `hide_help`, `show_command`, `hide_command`, `scan_commands`

### 6.9 Gaze
`gaze_on`, `gaze_off`

### 6.10 System
`shut_down`, `reboot`, `turn_off_display`

### 6.11 Volume (Specific Levels)
`set_volume_1` through `set_volume_15`, `set_volume_max`, `increase_volume`, `decrease_volume`, `mute_volume`

### 6.12 Confirmation
`confirm`, `cancel`, `close`, `submit`

---

## 7. Adding New Handlers

### 7.1 Create Handler Class

```kotlin
// handlers/NewActionHandler.kt
class NewActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "NewActionHandler"
    }

    override val intent = "new_intent"

    override suspend fun execute(
        context: Context,
        utterance: String
    ): ActionResult {
        return try {
            // Implementation
            ActionResult.Success("Done")
        } catch (e: Exception) {
            Log.e(TAG, "Failed", e)
            ActionResult.Failure("Failed: ${e.message}", e)
        }
    }
}
```

### 7.2 Register Handler

In `ActionsInitializer.kt`:

```kotlin
IntentActionHandlerRegistry.registerAll(
    // ... existing handlers
    NewActionHandler()
)
```

### 7.3 Add Intent to .ava File

In `apps/ava-standalone/src/main/assets/ava-examples/en-US/`:

```json
{
  "id": "new_intent",
  "c": "do new thing",
  "s": ["alternative phrase 1", "alternative phrase 2"],
  "cat": "system",
  "p": 1,
  "t": ["tag1", "tag2"]
}
```

---

## 8. Error Handling

### 8.1 VoiceOS Not Available

```kotlin
// VoiceOSConnection returns clear error messages
when {
    !isVoiceOSInstalled() ->
        "VoiceOS is not installed. Please install VoiceOS for accessibility commands."
    !isAccessibilityServiceRunning() ->
        "VoiceOS accessibility service is not running. Please enable it in Settings > Accessibility."
}
```

### 8.2 Handler Not Found

```kotlin
// ChatViewModel handles missing handlers
if (!IntentActionHandlerRegistry.hasHandler(intent)) {
    // Check if it's a VoiceOS-only intent
    val category = IntentRouter.getCategoryForIntent(intent)
    if (category in VOICEOS_ONLY_CATEGORIES) {
        // Route to VoiceOS
    } else {
        // Fall back to LLM response
    }
}
```

### 8.3 Execution Timeout

```kotlin
// VoiceOSConnection has 30-second timeout
return result ?: CommandResult.Failure(
    "Command execution timed out after 30 seconds"
)
```

---

## 9. Testing

### 9.1 Unit Tests

```kotlin
@Test
fun `TimeActionHandler returns formatted time`() = runTest {
    val handler = TimeActionHandler()
    val result = handler.execute(context, "what time is it")

    assertThat(result).isInstanceOf(ActionResult.Success::class.java)
    assertThat((result as ActionResult.Success).message)
        .contains(":")  // Contains time format
}
```

### 9.2 VoiceOS Mock

```kotlin
@Test
fun `GoBackActionHandler routes to VoiceOS`() = runTest {
    // Mock VoiceOSConnection
    val mockConnection = mockk<VoiceOSConnection>()
    coEvery {
        mockConnection.executeCommand("go_back", "navigation")
    } returns CommandResult.Success("Went back")

    // Test handler
    val handler = GoBackActionHandler(mockConnection)
    val result = handler.execute(context, "go back")

    assertThat(result).isInstanceOf(ActionResult.Success::class.java)
}
```

---

## 10. Performance Considerations

### 10.1 Handler Initialization

- All handlers registered at app startup
- Registration takes ~10ms for 52 handlers
- Handlers are stateless and reusable

### 10.2 VoiceOS IPC

- ContentProvider insert: ~5-10ms
- Polling interval: 100ms
- Typical execution: 50-500ms
- Timeout: 30 seconds

### 10.3 Handler Execution Targets

| Type | Target Time |
|------|-------------|
| Information (time, battery) | < 10ms |
| Settings open | < 50ms |
| App launch | < 100ms |
| Media controls | < 50ms |
| VoiceOS routing | < 500ms |

---

## 11. Future Enhancements

### 11.1 Planned Features

1. **Dynamic Handler Loading**: Load handlers from plugins
2. **Handler Analytics**: Track usage and success rates
3. **Custom User Handlers**: Let users create handlers via Teach-AVA
4. **Batch Execution**: Execute multiple commands in sequence

### 11.2 VoiceOS Integration Roadmap

1. **Phase 1** (Complete): Basic IPC and routing
2. **Phase 2**: Bidirectional communication
3. **Phase 3**: Context sharing
4. **Phase 4**: Seamless handoff

---

**Related Documentation**:
- Chapter 7: Features:NLU Module
- Chapter 8: Features:Chat Module
- ADR-006: VoiceOS Integration Strategy

---

*Last Updated: 2025-11-17*
