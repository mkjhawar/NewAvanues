# Developer Manual - Chapter 65: NLU Intent System

## Overview

The AVA NLU (Natural Language Understanding) system provides intent classification for voice commands. This chapter covers the complete intent architecture, including NLU-only intents, LLM-assisted intents, and the hybrid approach for complex operations.

---

## Architecture

### Intent Processing Pipeline

```
User Input → Tokenization → Embedding → Intent Classification
                                              ↓
                          ┌─────────────────────────────────────┐
                          │          IntentRouter               │
                          ├─────────────────────────────────────┤
                          │  NLU Intent    → ActionHandler      │
                          │  LLM Intent    → LlmActionService   │
                          │  Hybrid Intent → NLU + LLM Extract  │
                          └─────────────────────────────────────┘
```

### Intent Types

| Type | Prefix | Description | Processing |
|------|--------|-------------|------------|
| NLU-Only | `NLU` | Direct action, deterministic | ActionHandler executes immediately |
| LLM-Required | `LLM` | Needs reasoning/generation | Routed to LlmActionService |
| Hybrid | `HYB` | Classification + extraction | NLU classifies, LLM extracts params |

### Processing Flow

1. **NLU-Only**: User says "turn on lights" → NLU classifies → `control_lights` → LightActionHandler → Execute
2. **LLM-Required**: User says "create a workflow for morning routine" → NLU classifies → `create_workflow` → LlmActionService → Parse → Execute
3. **Hybrid**: User says "set temperature to 72" → NLU classifies → `control_thermostat` → LLM extracts "72" → ThermostatActionHandler(72) → Execute

---

## Intent Files (.ava)

### AVU-1.0 Format

```yaml
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: smart-home.ava
  category: smart_home
  name: Smart Home Controls
  description: Voice commands for smart home devices
  intentCount: 12
---
NLU:control_lights:turn on the lights
NLU:control_lights:lights off
NLU:control_lights:dim the lights
HYB:control_thermostat:set temperature to {value}
HYB:control_thermostat:make it {value} degrees
LLM:create_automation:create a routine for {description}
```

### Current Intent Categories (180+ Intents)

| Category | File | Intent Count | Type Distribution |
|----------|------|--------------|-------------------|
| System | system.ava | 3 | show_history, new_conversation, teach_ava |
| Information | information.ava | 2 | check_weather, show_time |
| Productivity | productivity.ava | 2 | set_alarm, set_reminder |
| Smart Home | smart-home.ava | 2 | control_lights, control_temperature |
| Calculation | calculation.ava | 1 | perform_calculation |
| Communication | communication.ava | 10 | calls, texts, emails, voicemail |
| Navigation | maps-navigation.ava | 10 | directions, nearby, traffic, parking |
| Lists/Notes | lists-notes.ava | 8 | shopping list, notes, todos |
| Calendar | calendar.ava | 8 | events, meetings, scheduling |
| Timers | timers.ava | 6 | timer management |
| Translation | translation-conversion.ava | 6 | translate, convert units |
| Search/Info | search-info.ava | 10 | web search, definitions, knowledge |
| Sports | sports.ava | 6 | scores, schedules, standings |
| Entertainment | entertainment.ava | 12 | jokes, trivia, games, stories |
| Health/Fitness | health-fitness.ava | 8 | workout, steps, meditation |
| Contacts | contacts.ava | 6 | contact management |
| Camera/Photos | camera-photos.ava | 8 | photos, video, gallery |
| Device Settings | device-settings.ava | 20 | DND, battery, connectivity |
| Finance | finance.ava | 6 | banking, stocks, crypto |
| Travel | travel.ava | 8 | rides, flights, hotels |
| Routines | routines.ava | 8 | morning/night routines |
| Accessibility | accessibility.ava | 6 | font size, screen reader |
| App Control | app-control.ava | 10 | app management |
| Workflows | workflows.ava | 6 | automation |
| Social Media | social-media.ava | 6 | posting, interactions |
| Documents | documents.ava | 6 | file management |
| MagicUI | magicui.ava | 12 | UI creation (dev) |
| MagicCode | magiccode.ava | 15 | code generation (dev) |
| Avanues Plugin | avanues-plugin.ava | 12 | plugin dev |

**Total Current: ~180 intents across 29 .ava files**

---

## Gap Analysis: Jarvis-Like Experience

### Research Summary

Based on analysis of Google Assistant BIIs, Amazon Alexa, Apple Siri, and open-source Jarvis implementations, AVA is missing critical system housekeeping and power-user intents needed for a truly "Jarvis-like" AI experience.

**Sources:**
- [Android Developers - Built-in Intents](https://developer.android.com/develop/devices/assistant/intents)
- [Google Developers - BII Index](https://developers.google.com/assistant/app/reference/built-in-intents/bii-index)
- [700+ Google Assistant Commands - Technastic](https://technastic.com/google-home-assistant-commands/)
- [GitHub - isair/jarvis](https://github.com/isair/jarvis)

### Gap Summary

| Status | Categories | Intents |
|--------|------------|---------|
| **Currently Have** | 29 files | ~180 intents |
| **Missing (High Priority)** | 6 categories | ~68 intents |
| **Missing (Medium Priority)** | 6 categories | ~48 intents |
| **Missing (Low Priority)** | 5 categories | ~25 intents |
| **Recommended Total** | 46 files | ~320+ intents |

### Top 10 Missing Intents for Jarvis Experience

| Rank | Intent | Category | Use Case |
|------|--------|----------|----------|
| 1 | `take_screenshot` | Screen Capture | Essential daily use |
| 2 | `find_device` | Security | "Find my phone" |
| 3 | `storage_status` | System Diagnostics | "How much storage left" |
| 4 | `read_notifications` | Notifications | Context awareness |
| 5 | `find_file` | File Management | "Find my resume" |
| 6 | `screen_record_start` | Screen Capture | Power user feature |
| 7 | `undo_last` | Quick Actions | Error recovery |
| 8 | `wifi_networks` | Connectivity | "Show available networks" |
| 9 | `lock_device` | Security | "Lock my phone" |
| 10 | `battery_drain_apps` | Diagnostics | "Which app drains battery" |

---

## Missing Intent Specifications (HIGH PRIORITY)

### 1. Security & Privacy (security-privacy.ava) - 12 intents

| Intent ID | Example Phrases | Type | Handler |
|-----------|-----------------|------|---------|
| `find_device` | "find my phone", "where's my phone", "ring my device" | NLU | FindDeviceHandler |
| `lock_device` | "lock my phone", "secure device", "lock screen now" | NLU | DeviceLockHandler |
| `erase_device` | "wipe my phone", "factory reset" | NLU | EraseDeviceHandler |
| `emergency_sos` | "emergency", "call 911", "I need help" | NLU | EmergencyHandler |
| `trusted_places` | "add trusted location", "remove trusted place" | HYB | TrustedPlacesHandler |
| `app_lock` | "lock Instagram", "secure banking app" | HYB | AppLockHandler |
| `check_security` | "security status", "is my phone secure" | NLU | SecurityCheckHandler |
| `view_security_events` | "who tried to unlock", "show failed logins" | NLU | SecurityEventsHandler |
| `biometric_toggle` | "enable fingerprint", "disable face unlock" | NLU | BiometricHandler |
| `guest_mode` | "enable guest mode", "switch to guest" | NLU | GuestModeHandler |
| `secure_folder` | "open secure folder", "hide private apps" | NLU | SecureFolderHandler |
| `privacy_dashboard` | "show privacy report", "which apps accessed camera" | NLU | PrivacyDashboardHandler |

### 2. Screen Capture & Clipboard (screen-clipboard.ava) - 10 intents

| Intent ID | Example Phrases | Type | Handler |
|-----------|-----------------|------|---------|
| `take_screenshot` | "take screenshot", "capture screen", "screenshot this" | NLU | ScreenshotHandler |
| `screen_record_start` | "start recording screen", "record my screen" | NLU | ScreenRecordHandler |
| `screen_record_stop` | "stop recording", "end screen capture" | NLU | ScreenRecordHandler |
| `screenshot_scroll` | "scrolling screenshot", "long screenshot" | NLU | ScrollScreenshotHandler |
| `copy_screen_text` | "copy text from screen", "OCR this" | NLU | OcrCopyHandler |
| `show_clipboard` | "what's in clipboard", "show clipboard" | NLU | ClipboardHandler |
| `clear_clipboard` | "clear clipboard", "empty clipboard" | NLU | ClipboardHandler |
| `paste_last` | "paste last item", "paste previous" | NLU | ClipboardHandler |
| `clipboard_search` | "search clipboard for", "find in clipboard" | HYB | ClipboardHandler |
| `share_screenshot` | "share last screenshot", "send screenshot to" | HYB | ShareScreenshotHandler |

### 3. File System (file-system.ava) - 14 intents

| Intent ID | Example Phrases | Type | Handler |
|-----------|-----------------|------|---------|
| `find_file` | "find my resume", "search for document" | HYB | FileSearchHandler |
| `open_file` | "open Downloads folder", "show my PDFs" | HYB | FileOpenHandler |
| `move_file` | "move this to Documents", "transfer to SD card" | HYB | FileMoveHandler |
| `copy_file` | "copy to backup folder", "duplicate this file" | HYB | FileCopyHandler |
| `delete_file` | "delete old downloads", "remove temp files" | HYB | FileDeleteHandler |
| `rename_file` | "rename to invoice_2024", "change file name" | HYB | FileRenameHandler |
| `compress_files` | "zip these files", "compress folder" | HYB | CompressHandler |
| `extract_archive` | "unzip this", "extract archive" | HYB | ExtractHandler |
| `recent_files` | "show recent files", "latest downloads" | NLU | RecentFilesHandler |
| `file_info` | "file size", "when was this modified" | HYB | FileInfoHandler |
| `share_file` | "share this file via", "send document" | HYB | FileShareHandler |
| `create_folder` | "new folder called Work", "create directory" | HYB | FolderCreateHandler |
| `storage_cleanup` | "clean up storage", "free up space" | NLU | StorageCleanupHandler |
| `download_status` | "check downloads", "download progress" | NLU | DownloadStatusHandler |

### 4. System Diagnostics (system-diagnostics.ava) - 12 intents

| Intent ID | Example Phrases | Type | Handler |
|-----------|-----------------|------|---------|
| `storage_status` | "storage space", "how much storage left" | NLU | StorageStatusHandler |
| `memory_usage` | "RAM usage", "available memory" | NLU | MemoryStatusHandler |
| `battery_health` | "battery health", "battery cycles" | NLU | BatteryHealthHandler |
| `data_usage` | "data consumption", "how much data used" | NLU | DataUsageHandler |
| `battery_drain_apps` | "which app drains battery", "battery usage by app" | NLU | BatteryDrainHandler |
| `device_temperature` | "phone temperature", "is device overheating" | NLU | TemperatureHandler |
| `system_info` | "system information", "device specs" | NLU | SystemInfoHandler |
| `running_apps` | "what's running", "background apps" | NLU | RunningAppsHandler |
| `performance_mode` | "optimize performance", "speed up phone" | NLU | PerformanceModeHandler |
| `clear_temp_data` | "clear temp files", "delete cache" | NLU | ClearCacheHandler |
| `safe_mode` | "boot safe mode", "troubleshoot mode" | NLU | SafeModeHandler |
| `network_diagnostics` | "test connection", "network speed" | NLU | NetworkDiagHandler |

### 5. Notification Management (notifications.ava) - 8 intents

| Intent ID | Example Phrases | Type | Handler |
|-----------|-----------------|------|---------|
| `read_notifications` | "read notifications", "what notifications" | NLU | ReadNotificationHandler |
| `clear_all_notifications` | "clear all notifications", "dismiss all" | NLU | ClearNotificationHandler |
| `clear_app_notifications` | "clear WhatsApp notifications" | HYB | ClearNotificationHandler |
| `snooze_notification` | "snooze this for 1 hour", "remind me later" | HYB | SnoozeNotificationHandler |
| `notification_history` | "notification history", "missed notifications" | NLU | NotificationHistoryHandler |
| `notification_channels` | "manage notification channels" | NLU | NotificationChannelHandler |
| `priority_only` | "priority notifications only", "important only" | NLU | PriorityModeHandler |
| `mute_app_notifications` | "mute Slack notifications" | HYB | MuteAppNotificationHandler |

### 6. Advanced Connectivity (connectivity.ava) - 10 intents

| Intent ID | Example Phrases | Type | Handler |
|-----------|-----------------|------|---------|
| `wifi_networks` | "available wifi", "show networks" | NLU | WifiNetworksHandler |
| `connect_specific_wifi` | "connect to Home_WiFi", "join this network" | HYB | WifiConnectHandler |
| `forget_wifi` | "forget this network", "remove saved wifi" | HYB | WifiForgetHandler |
| `wifi_password` | "show wifi password", "what's the password" | HYB | WifiPasswordHandler |
| `pair_bluetooth` | "pair new device", "connect headphones" | HYB | BluetoothPairHandler |
| `disconnect_bluetooth` | "disconnect speaker", "unpair device" | HYB | BluetoothDisconnectHandler |
| `cast_screen` | "cast to TV", "mirror screen" | HYB | ScreenCastHandler |
| `stop_casting` | "stop casting", "disconnect from TV" | NLU | ScreenCastHandler |
| `nearby_share` | "receive files", "accept nearby share" | NLU | NearbyShareHandler |
| `wifi_direct` | "connect directly", "wifi direct transfer" | HYB | WifiDirectHandler |

---

## Missing Intent Specifications (MEDIUM PRIORITY)

### 7. Quick Actions (quick-actions.ava) - 8 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `undo_last` | "undo", "undo that", "take that back" | NLU |
| `redo_action` | "redo", "do it again" | NLU |
| `repeat_command` | "repeat", "say that again", "do that again" | NLU |
| `cancel_action` | "cancel", "stop that", "never mind" | NLU |
| `go_back` | "go back", "previous screen" | NLU |
| `go_home` | "go home", "home screen" | NLU |
| `recent_apps` | "recent apps", "show app switcher" | NLU |
| `close_all_apps` | "close all apps", "clear recent apps" | NLU |

### 8. Context Awareness (context-awareness.ava) - 6 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `what_on_screen` | "what's on my screen", "read this" | LLM |
| `summarize_screen` | "summarize this page", "what does this say" | LLM |
| `extract_info` | "extract phone numbers", "find addresses here" | LLM |
| `describe_image` | "what's in this image", "describe this photo" | LLM |
| `read_qr_code` | "scan QR code", "what's this code" | NLU |
| `copy_visible_text` | "copy all text", "select all" | NLU |

### 9. Email Power (email-power.ava) - 8 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `archive_email` | "archive this email", "archive" | NLU |
| `mark_unread` | "mark as unread", "mark unread" | NLU |
| `star_email` | "star this email", "mark important" | NLU |
| `move_to_folder` | "move to Work folder", "file this" | HYB |
| `unsubscribe` | "unsubscribe from this", "stop emails" | NLU |
| `email_search` | "find emails from John", "search inbox" | HYB |
| `schedule_send` | "send this tomorrow at 9", "send later" | HYB |
| `draft_save` | "save as draft", "save this email" | NLU |

### 10. Focus Modes (focus-modes.ava) - 6 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `focus_work` | "focus mode work", "work mode" | NLU |
| `focus_driving` | "driving mode", "driving focus" | NLU |
| `focus_sleep` | "sleep mode", "bedtime focus" | NLU |
| `focus_gaming` | "gaming mode", "game focus" | NLU |
| `focus_custom` | "enable my custom focus" | HYB |
| `focus_off` | "turn off focus mode", "disable focus" | NLU |

### 11. Parental Controls (parental-controls.ava) - 6 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `screen_time_check` | "how much screen time today", "usage stats" | NLU |
| `app_time_limit` | "limit Instagram to 1 hour", "set app limit" | HYB |
| `bedtime_lock` | "enable bedtime lock", "lock at 10pm" | HYB |
| `content_filter` | "enable content filter", "safe search" | NLU |
| `pause_apps` | "pause all apps", "downtime now" | NLU |
| `view_usage_report` | "show weekly usage", "screen time report" | NLU |

### 12. Device Info (device-info.ava) - 6 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `device_model` | "what phone is this", "device model" | NLU |
| `android_version` | "Android version", "OS version" | NLU |
| `available_updates` | "any system updates", "check updates" | NLU |
| `imei_number` | "show IMEI", "device IMEI" | NLU |
| `serial_number` | "serial number", "device serial" | NLU |
| `warranty_status` | "warranty status", "is warranty active" | NLU |

---

## Missing Intent Specifications (LOW PRIORITY)

### 13. AI/Learning (ai-learning.ava) - 6 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `remember_preference` | "remember I prefer dark mode", "remember this" | LLM |
| `forget_preference` | "forget that preference", "don't remember" | LLM |
| `custom_wake_word` | "change wake word to", "new wake word" | HYB |
| `voice_profile` | "learn my voice", "voice training" | NLU |
| `feedback_positive` | "that was helpful", "good job" | NLU |
| `feedback_negative` | "that's wrong", "not what I wanted" | NLU |

### 14. Multi-Device (multi-device.ava) - 5 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `continue_on_tablet` | "continue this on tablet", "send to tablet" | HYB |
| `send_to_desktop` | "send to my computer", "open on desktop" | HYB |
| `find_other_device` | "find my tablet", "ring my watch" | HYB |
| `sync_devices` | "sync all devices", "sync now" | NLU |
| `nearby_devices` | "show nearby devices", "what's nearby" | NLU |

### 15. Proactive Suggestions (proactive.ava) - 4 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `suggest_action` | "what should I do", "any suggestions" | LLM |
| `smart_reminder` | "remind me based on context" | LLM |
| `daily_briefing` | "my daily briefing", "what's my day" | LLM |
| `routine_suggestion` | "suggest a routine", "automate this" | LLM |

### 16. Integration/Automation (integration.ava) - 5 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `ifttt_trigger` | "trigger IFTTT", "run applet" | HYB |
| `tasker_run` | "run Tasker task", "execute Tasker" | HYB |
| `macro_execute` | "run my macro", "execute macro" | HYB |
| `webhook_trigger` | "trigger webhook", "call my API" | HYB |
| `shortcut_run` | "run shortcut", "execute shortcut" | HYB |

### 17. Developer/Debug (developer.ava) - 5 intents

| Intent ID | Example Phrases | Type |
|-----------|-----------------|------|
| `show_logs` | "show system logs", "view logs" | NLU |
| `clear_logs` | "clear logs", "delete log files" | NLU |
| `enable_debug` | "enable debug mode", "developer mode" | NLU |
| `system_report` | "generate system report", "bug report" | NLU |
| `adb_wireless` | "enable wireless ADB", "ADB over wifi" | NLU |

---

## Implementation Roadmap

### Phase 1: Security & System (Week 1-2)
- security-privacy.ava (12 intents)
- system-diagnostics.ava (12 intents)
- **Total: 24 intents**

### Phase 2: Screen & Files (Week 3-4)
- screen-clipboard.ava (10 intents)
- file-system.ava (14 intents)
- **Total: 24 intents**

### Phase 3: Connectivity & Notifications (Week 5-6)
- connectivity.ava (10 intents)
- notifications.ava (8 intents)
- quick-actions.ava (8 intents)
- **Total: 26 intents**

### Phase 4: Power Features (Week 7-8)
- context-awareness.ava (6 intents)
- email-power.ava (8 intents)
- focus-modes.ava (6 intents)
- **Total: 20 intents**

### Phase 5: Advanced (Week 9-10)
- Remaining categories
- **Total: ~47 intents**

**Grand Total: ~141 new intents → 320+ total intents**

---

## Platform Compatibility Matrix

### iOS vs Android Intent Support

Not all intents are available on both platforms due to OS restrictions:

| Category | Android | iOS | Notes |
|----------|---------|-----|-------|
| **Security & Privacy** | 12/12 | 4/12 | iOS restricts find_device, lock, erase (MDM only) |
| **Screen Capture** | 10/10 | 3/10 | iOS restricts programmatic screenshot/recording |
| **File System** | 14/14 | 6/14 | iOS sandboxed; limited to app-accessible files |
| **System Diagnostics** | 12/12 | 5/12 | iOS hides battery health, RAM, temp APIs |
| **Notifications** | 8/8 | 6/8 | iOS can't clear other app notifications |
| **Connectivity** | 10/10 | 4/10 | iOS restricts WiFi programmatic control |
| **Quick Actions** | 8/8 | 8/8 | Full support via accessibility |
| **Context Awareness** | 6/6 | 6/6 | Full support (LLM-based) |
| **Email Power** | 8/8 | 8/8 | Full support via Mail app intents |
| **Focus Modes** | 6/6 | 6/6 | Full support (native iOS Focus) |
| **Parental Controls** | 6/6 | 4/6 | iOS requires Screen Time API |
| **Device Info** | 6/6 | 3/6 | iOS hides IMEI, serial number |
| **AI/Learning** | 6/6 | 6/6 | Full support |
| **Multi-Device** | 5/5 | 5/5 | Full support via Handoff/iCloud |
| **Proactive** | 4/4 | 4/4 | Full support (LLM-based) |
| **Integration** | 5/5 | 3/5 | iOS: Shortcuts only (no Tasker/IFTTT deep) |
| **Developer** | 5/5 | 1/5 | iOS restricts debug/logs access |

### Summary by Platform

| Platform | Total Intents | Supported | Percentage |
|----------|---------------|-----------|------------|
| **Android** | 141 | 141 | 100% |
| **iOS** | 141 | 82 | 58% |
| **Cross-Platform** | 141 | 82 | 58% |

### iOS-Restricted Intent Categories

| Intent | iOS Restriction | Workaround |
|--------|-----------------|------------|
| `find_device` | Requires Find My API (limited) | Use Find My app shortcut |
| `lock_device` | MDM/supervised devices only | Prompt user action |
| `take_screenshot` | No programmatic API | Guide user to button combo |
| `screen_record_start` | No programmatic API | Launch Control Center |
| `wifi_networks` | No scan API since iOS 12 | Open Settings app |
| `pair_bluetooth` | Settings only | Open Bluetooth settings |
| `storage_status` | Limited API access | Estimate from available space |
| `battery_drain_apps` | No API | Not supported |
| `show_logs` | No access | Not supported |

---

## Intent Execution Architecture

### How AVA Executes Intents

```
┌──────────────────────────────────────────────────────────────────┐
│                    USER VOICE COMMAND                            │
│                   "take a screenshot"                            │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                 1. INTENT CLASSIFICATION                         │
│  IntentClassifier.classify(input)                                │
│  → Returns: ClassificationResult(                                │
│       intentId = "take_screenshot",                              │
│       confidence = 0.97,                                         │
│       processingType = NLU_ONLY                                  │
│     )                                                            │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                 2. INTENT ROUTING                                │
│  IntentRouter.route(classification)                              │
│  → Looks up handler in ActionHandlerRegistry                     │
│  → Handler: ScreenshotHandler                                    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                 3. ACTION HANDLER EXECUTION                      │
│  ScreenshotHandler.handle(input, params)                         │
│                                                                  │
│  // Android Implementation:                                      │
│  fun handle(): ActionResult {                                    │
│    val mediaProjection = getMediaProjection()                    │
│    val bitmap = captureScreen(mediaProjection)                   │
│    saveToGallery(bitmap)                                         │
│    return ActionResult.Success("Screenshot saved")               │
│  }                                                               │
│                                                                  │
│  // iOS Implementation:                                          │
│  func handle() -> ActionResult {                                 │
│    // iOS doesn't allow programmatic screenshots                 │
│    return ActionResult.Unsupported(                              │
│      "Press Side + Volume Up to take screenshot"                 │
│    )                                                             │
│  }                                                               │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                 4. RESPONSE TO USER                              │
│  TTSEngine.speak("Screenshot saved to gallery")                  │
│  OR                                                              │
│  TTSEngine.speak("Press Side and Volume Up to screenshot")       │
└──────────────────────────────────────────────────────────────────┘
```

### Action Handler Registry

```kotlin
// ActionsInitializer.kt - Android
object ActionHandlerRegistry {
    private val handlers = mutableMapOf<String, ActionHandler>()

    fun init(context: Context) {
        // Security & Privacy
        register("find_device", FindDeviceHandler(context))
        register("lock_device", DeviceLockHandler(context))
        register("emergency_sos", EmergencyHandler(context))

        // Screen Capture
        register("take_screenshot", ScreenshotHandler(context))
        register("screen_record_start", ScreenRecordHandler(context))
        register("screen_record_stop", ScreenRecordHandler(context))

        // File System
        register("find_file", FileSearchHandler(context))
        register("storage_cleanup", StorageCleanupHandler(context))

        // System Diagnostics
        register("storage_status", StorageStatusHandler(context))
        register("battery_drain_apps", BatteryDrainHandler(context))

        // ... 130+ more handlers
    }

    fun get(intentId: String): ActionHandler? = handlers[intentId]
}
```

### Action Handler Interface

```kotlin
// ActionHandler.kt
interface ActionHandler {
    val intentId: String
    val requiredPermissions: List<String>
    val supportedPlatforms: Set<Platform>

    suspend fun handle(
        input: String,
        params: Map<String, Any>? = null,
        context: ActionContext
    ): ActionResult

    fun isSupported(): Boolean = Platform.current in supportedPlatforms
}

// ActionResult.kt
sealed class ActionResult {
    data class Success(val message: String, val data: Any? = null) : ActionResult()
    data class Error(val message: String, val code: Int) : ActionResult()
    data class Unsupported(val guidance: String) : ActionResult()
    data class NeedsPermission(val permissions: List<String>) : ActionResult()
    data class NeedsConfirmation(val prompt: String) : ActionResult()
}
```

### Platform-Specific Handler Example

```kotlin
// ScreenshotHandler.kt
class ScreenshotHandler(private val context: Context) : ActionHandler {
    override val intentId = "take_screenshot"
    override val requiredPermissions = listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    override val supportedPlatforms = setOf(Platform.ANDROID)

    override suspend fun handle(
        input: String,
        params: Map<String, Any>?,
        ctx: ActionContext
    ): ActionResult {
        return when (Platform.current) {
            Platform.ANDROID -> takeScreenshotAndroid()
            Platform.IOS -> ActionResult.Unsupported(
                "Press Side button and Volume Up together to take a screenshot"
            )
        }
    }

    private suspend fun takeScreenshotAndroid(): ActionResult {
        // Request MediaProjection if not granted
        val mediaProjection = MediaProjectionManager.get(context)
        val bitmap = mediaProjection.capture()
        val uri = MediaStore.saveImage(context, bitmap, "AVA_Screenshot")
        return ActionResult.Success("Screenshot saved", uri)
    }
}
```

### Hybrid Intent Execution (NLU + LLM)

```kotlin
// Example: "move my resume to Documents folder"
// Intent: move_file (HYB type)

class FileMoveHandler : ActionHandler {
    override suspend fun handle(input: String, params: Map<String, Any>?, ctx: ActionContext): ActionResult {
        // Step 1: LLM extracts parameters
        val extraction = ctx.llmService.extractParameters(
            input = input,
            schema = """
                {
                    "source_file": "string (filename or description)",
                    "destination": "string (folder name or path)"
                }
            """
        )
        // extraction = { "source_file": "resume", "destination": "Documents" }

        // Step 2: Find matching file
        val sourceFile = FileSearchService.find(extraction["source_file"])
            ?: return ActionResult.Error("File not found: ${extraction["source_file"]}")

        // Step 3: Resolve destination
        val destFolder = FolderResolver.resolve(extraction["destination"])

        // Step 4: Execute move
        FileManager.move(sourceFile, destFolder)

        return ActionResult.Success("Moved ${sourceFile.name} to ${destFolder.name}")
    }
}
```

### Permission Flow

```
┌─────────────────────────────────────────────────────────┐
│  User: "Take a screenshot"                              │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  ScreenshotHandler.handle()                             │
│  → Check: hasPermission(MEDIA_PROJECTION)?              │
└─────────────────────────────────────────────────────────┘
                         │
            ┌────────────┴────────────┐
            │                         │
            ▼                         ▼
    ┌───────────────┐       ┌─────────────────────────┐
    │ YES: Execute  │       │ NO: Request Permission  │
    │ screenshot    │       │                         │
    └───────────────┘       │ return ActionResult     │
                            │   .NeedsPermission([    │
                            │     MEDIA_PROJECTION    │
                            │   ])                    │
                            └─────────────────────────┘
                                       │
                                       ▼
                            ┌─────────────────────────┐
                            │ UI shows permission     │
                            │ dialog to user         │
                            └─────────────────────────┘
                                       │
                                       ▼
                            ┌─────────────────────────┐
                            │ User grants → Retry    │
                            │ User denies → Error    │
                            └─────────────────────────┘
```

**Grand Total: ~141 new intents → 320+ total intents**

---

## Smart Home Integration Architecture

### How Smart Home Intents Work

AVA doesn't directly control smart home devices. Instead, it integrates with existing smart home platforms:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    USER: "Turn on the living room lights"               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  NLU Classification → control_lights                                    │
│  Entity Extraction → { device: "lights", room: "living room", state: on }│
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   SmartHomeActionHandler                                │
│  1. Check configured smart home platform                                │
│  2. Route to appropriate adapter                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────┬───────────┼───────────┬───────────────┐
        │               │           │           │               │
        ▼               ▼           ▼           ▼               ▼
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐
│ Google Home   │ │ Alexa     │ │ Home      │ │ Matter/   │ │ SmartThings│
│ Adapter       │ │ Adapter   │ │ Assistant │ │ Thread    │ │ Adapter   │
│               │ │           │ │ Adapter   │ │ Adapter   │ │           │
│ Uses Google   │ │ Uses      │ │ Direct    │ │ Direct    │ │ SmartThings│
│ Home APIs     │ │ Alexa     │ │ REST API  │ │ protocol  │ │ REST API  │
│               │ │ Smart     │ │           │ │           │ │           │
│               │ │ Home APIs │ │           │ │           │ │           │
└───────────────┘ └───────────┘ └───────────┘ └───────────┘ └───────────┘
        │               │           │           │               │
        └───────────────┴───────────┼───────────┴───────────────┘
                                    │
                                    ▼
                        ┌───────────────────────┐
                        │   Physical Device     │
                        │   (Light turns on)    │
                        └───────────────────────┘
```

### Supported Smart Home Platforms

| Platform | Integration Method | Status | Capabilities |
|----------|-------------------|--------|--------------|
| **Google Home** | Google Home API / Cast | Planned | Lights, thermostats, cameras, locks |
| **Amazon Alexa** | Alexa Smart Home API | Planned | Full smart home control |
| **Home Assistant** | REST API / WebSocket | Planned | All devices (most flexible) |
| **Matter/Thread** | Direct protocol | Planned | Native smart home standard |
| **SmartThings** | SmartThings API | Planned | Samsung ecosystem |
| **Apple HomeKit** | HomeKit API (iOS only) | Planned | Apple ecosystem |
| **Local WiFi** | Direct IP control | Planned | Tuya, Tapo, Wyze devices |

### Smart Home Handler Implementation

```kotlin
// SmartHomeActionHandler.kt
class SmartHomeActionHandler(
    private val platformManager: SmartHomePlatformManager,
    private val deviceResolver: DeviceResolver,
    private val entityExtractor: EntityExtractor
) : ActionHandler {

    override val intentId = "control_lights"  // or control_temperature, etc.

    override suspend fun handle(
        input: String,
        params: Map<String, Any>?,
        ctx: ActionContext
    ): ActionResult {
        // Step 1: Extract entities from command
        val entities = entityExtractor.extract(input)
        // { device_type: "light", room: "living room", state: "on", brightness: 80 }

        // Step 2: Resolve device in user's smart home setup
        val device = deviceResolver.findDevice(
            deviceType = entities["device_type"],
            room = entities["room"],
            name = entities["device_name"]
        ) ?: return ActionResult.Error("Device not found in ${entities["room"]}")

        // Step 3: Get the appropriate platform adapter
        val adapter = platformManager.getAdapter(device.platform)
            ?: return ActionResult.Error("Smart home platform not configured")

        // Step 4: Execute the command
        return adapter.executeCommand(
            device = device,
            command = SmartHomeCommand(
                action = entities["state"],  // "on", "off", "dim"
                brightness = entities["brightness"],
                color = entities["color"],
                temperature = entities["temperature"]
            )
        )
    }
}
```

### Platform Adapter Interface

```kotlin
// SmartHomePlatformAdapter.kt
interface SmartHomePlatformAdapter {
    val platformId: String
    val platformName: String

    suspend fun authenticate(credentials: PlatformCredentials): AuthResult
    suspend fun discoverDevices(): List<SmartHomeDevice>
    suspend fun executeCommand(device: SmartHomeDevice, command: SmartHomeCommand): ActionResult
    suspend fun getDeviceState(device: SmartHomeDevice): DeviceState
}

// Google Home Implementation
class GoogleHomeAdapter : SmartHomePlatformAdapter {
    override val platformId = "google_home"
    override val platformName = "Google Home"

    override suspend fun executeCommand(
        device: SmartHomeDevice,
        command: SmartHomeCommand
    ): ActionResult {
        // Option 1: Use Google Home app intent
        val intent = Intent("com.google.android.apps.chromecast.app.CONTROL_DEVICE")
        intent.putExtra("device_id", device.googleHomeId)
        intent.putExtra("action", command.action)
        context.startActivity(intent)

        // Option 2: Use Google Smart Home API (requires OAuth)
        val response = googleHomeApi.executeCommand(
            deviceId = device.googleHomeId,
            command = command.toGoogleFormat()
        )

        return if (response.success) {
            ActionResult.Success("${device.name} turned ${command.action}")
        } else {
            ActionResult.Error(response.errorMessage)
        }
    }
}

// Home Assistant Implementation (Most Flexible)
class HomeAssistantAdapter(
    private val serverUrl: String,
    private val accessToken: String
) : SmartHomePlatformAdapter {
    override val platformId = "home_assistant"
    override val platformName = "Home Assistant"

    override suspend fun executeCommand(
        device: SmartHomeDevice,
        command: SmartHomeCommand
    ): ActionResult {
        val service = when (device.type) {
            DeviceType.LIGHT -> "light.turn_${command.action}"
            DeviceType.SWITCH -> "switch.turn_${command.action}"
            DeviceType.CLIMATE -> "climate.set_temperature"
            DeviceType.LOCK -> "lock.${command.action}"
            else -> return ActionResult.Error("Unsupported device type")
        }

        val response = httpClient.post("$serverUrl/api/services/${service}") {
            header("Authorization", "Bearer $accessToken")
            setBody(mapOf(
                "entity_id" to device.entityId,
                "brightness_pct" to command.brightness,
                "temperature" to command.temperature
            ))
        }

        return if (response.status.isSuccess()) {
            ActionResult.Success("${device.name} ${command.action}")
        } else {
            ActionResult.Error("Failed to control ${device.name}")
        }
    }
}
```

### Device Discovery & Setup Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  User: "Set up smart home"                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  AVA: "Which smart home platform do you use?"                   │
│                                                                 │
│  1. Google Home                                                 │
│  2. Amazon Alexa                                                │
│  3. Home Assistant                                              │
│  4. Apple HomeKit (iOS only)                                    │
│  5. SmartThings                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ (User selects Home Assistant)
┌─────────────────────────────────────────────────────────────────┐
│  AVA: "Please enter your Home Assistant URL and access token"  │
│       [Opens setup screen with input fields]                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  HomeAssistantAdapter.authenticate()                            │
│  HomeAssistantAdapter.discoverDevices()                         │
│                                                                 │
│  Found 15 devices:                                              │
│  - Living Room Lights (light.living_room)                       │
│  - Bedroom Lamp (light.bedroom_lamp)                            │
│  - Thermostat (climate.nest)                                    │
│  - Front Door Lock (lock.front_door)                            │
│  - ...                                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  AVA: "I found 15 devices. You can now say things like:"        │
│                                                                 │
│  - "Turn on the living room lights"                             │
│  - "Set thermostat to 72 degrees"                               │
│  - "Lock the front door"                                        │
│  - "Dim bedroom lamp to 50%"                                    │
└─────────────────────────────────────────────────────────────────┘
```

### Entity Resolution for Smart Home

```kotlin
// DeviceResolver.kt
class DeviceResolver(
    private val deviceRepository: SmartHomeDeviceRepository
) {
    // Fuzzy matching for natural language device references
    suspend fun findDevice(
        deviceType: String?,
        room: String?,
        name: String?
    ): SmartHomeDevice? {
        val allDevices = deviceRepository.getAllDevices()

        // Score each device based on match quality
        val scored = allDevices.map { device ->
            var score = 0

            // Match device type
            if (deviceType != null && device.type.matches(deviceType)) {
                score += 30
            }

            // Match room (fuzzy)
            if (room != null) {
                val roomScore = fuzzyMatch(room, device.room)
                score += (roomScore * 40).toInt()
            }

            // Match name (fuzzy)
            if (name != null) {
                val nameScore = fuzzyMatch(name, device.name)
                score += (nameScore * 30).toInt()
            }

            device to score
        }

        // Return highest scoring device above threshold
        return scored
            .filter { it.second >= 50 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun fuzzyMatch(query: String, target: String): Double {
        // Levenshtein distance, synonym matching, etc.
        return FuzzyMatcher.similarity(
            query.lowercase(),
            target.lowercase()
        )
    }
}
```

### Smart Home Data Model

```kotlin
// SmartHomeDevice.kt
data class SmartHomeDevice(
    val id: String,
    val name: String,                    // "Living Room Lights"
    val type: DeviceType,                // LIGHT, SWITCH, THERMOSTAT, LOCK, etc.
    val room: String,                    // "Living Room"
    val platform: String,                // "home_assistant", "google_home"
    val platformDeviceId: String,        // Platform-specific ID
    val capabilities: Set<Capability>,   // ON_OFF, BRIGHTNESS, COLOR, TEMPERATURE
    val state: DeviceState?              // Current state (cached)
)

enum class DeviceType {
    LIGHT, SWITCH, THERMOSTAT, LOCK, CAMERA, SENSOR, BLINDS, FAN, SPEAKER, TV
}

enum class Capability {
    ON_OFF, BRIGHTNESS, COLOR_TEMP, RGB_COLOR, TEMPERATURE_SET,
    LOCK_UNLOCK, OPEN_CLOSE, VOLUME, CHANNEL
}

data class SmartHomeCommand(
    val action: String,           // "on", "off", "lock", "unlock"
    val brightness: Int? = null,  // 0-100
    val colorTemp: Int? = null,   // Kelvin
    val rgbColor: String? = null, // "#FF0000"
    val temperature: Double? = null,
    val volume: Int? = null
)
```

### Without Smart Home Platform (Fallback)

If user hasn't configured a smart home platform:

```kotlin
class SmartHomeActionHandler : ActionHandler {
    override suspend fun handle(...): ActionResult {
        val adapter = platformManager.getConfiguredAdapter()

        if (adapter == null) {
            // No platform configured - guide user
            return ActionResult.NeedsSetup(
                message = "Smart home not configured. Would you like to set it up?",
                setupIntent = "configure_smart_home"
            )
        }

        // ... normal execution
    }
}
```

AVA will respond: *"I don't have access to your smart home devices yet. Would you like to connect Google Home, Alexa, or Home Assistant?"*

---

## LLM Prompt Templates (.avp)

### AVP-1.0 Format

```yaml
---
schema: avp-1.0
version: 1.0.0
locale: en-US
metadata:
  intent: create_workflow
  name: Workflow Creator
  processing_type: LLM_REQUIRED
  model_requirements:
    min_context: 512
    recommended_context: 1024
---
system: |
  You are AVA's workflow designer. Parse natural language into workflows.

  AVAILABLE TRIGGERS: time, location_arrive, location_leave, battery_low
  AVAILABLE ACTIONS: toggle_wifi, send_text, speak, notification

output_schema: |
  WORKFLOW:id:{uuid}
  WORKFLOW:name:{name}
  TRIGGER:type:{type}
  ACTION:1:type:{type}

examples:
  - input: "When I leave work, text my wife"
    output: |
      WORKFLOW:id:wf-001
      TRIGGER:type:location_leave
      TRIGGER:param:place:work
      ACTION:1:type:send_text
```

### Available Prompt Templates

| Template | Intent | Use Case |
|----------|--------|----------|
| create_workflow.avp | create_workflow | Automation workflows |
| create_ui_component.avp | create_ui_component | MagicUI components |
| create_ui_screen.avp | create_ui_screen | Full screen layouts |
| generate_code.avp | generate_code | Kotlin/Swift code |
| create_plugin.avp | create_plugin | Avanues plugins |
| answer_question.avp | answer_question | General knowledge |
| compose_message.avp | compose_message | Emails/texts |
| summarize_content.avp | summarize_content | Document summaries |

---

## IntentRouter Implementation

### Key Classes

```kotlin
// IntentRouter.kt
class IntentRouter(
    private val intentClassifier: IntentClassifier,
    private val actionHandlers: Map<String, ActionHandler>,
    private val llmActionService: LlmActionService
) {
    suspend fun route(userInput: String): ActionResult {
        val classification = intentClassifier.classify(userInput)

        return when (classification.processingType) {
            ProcessingType.NLU_ONLY -> {
                actionHandlers[classification.intentId]?.handle(userInput)
                    ?: ActionResult.Error("No handler for ${classification.intentId}")
            }
            ProcessingType.LLM_REQUIRED -> {
                llmActionService.process(classification.intentId, userInput)
            }
            ProcessingType.HYBRID -> {
                val params = llmActionService.extractParameters(userInput)
                actionHandlers[classification.intentId]?.handle(userInput, params)
                    ?: ActionResult.Error("No handler for ${classification.intentId}")
            }
        }
    }
}
```

### Model-Agnostic LLM Service

```kotlin
// LlmActionService.kt
class LlmActionService(
    private val deviceModelSelector: DeviceModelSelector,
    private val inferenceStrategy: MultiProviderInferenceStrategy,
    private val promptTemplateLoader: PromptTemplateLoader
) {
    suspend fun process(intentId: String, userInput: String): ActionResult {
        // Load prompt template for intent
        val template = promptTemplateLoader.load(intentId)

        // Select best model for device
        val model = deviceModelSelector.selectModel(template.minContext)

        // Run inference
        val response = inferenceStrategy.generate(
            model = model,
            systemPrompt = template.systemPrompt,
            userPrompt = userInput
        )

        // Parse AVU-1.0 response
        return parseAvuResponse(response)
    }
}
```

---

## Embedding Generation

### Pre-computation Pipeline

```bash
# Generate embeddings for all intents
python tools/embedding-generator/generate_embeddings.py \
    --model android/ava/src/main/assets/models/AVA-384-Base-INT8.AON \
    --vocab android/ava/src/main/assets/models/vocab.txt \
    --ava-core-dir .ava/core/en-US \
    --output-sql common/core/Data/src/main/sqldelight/.../PrecomputedEmbeddings.sq \
    --output-aot android/ava/src/main/assets/embeddings/bundled_embeddings.aot
```

### Output Files

| File | Purpose | Format |
|------|---------|--------|
| PrecomputedEmbeddings.sq | SQLDelight migration | SQL INSERT statements |
| bundled_embeddings.aot | Backup file | Binary (AOT format) |

---

## Adding New Intents

### Step 1: Create/Edit .ava File

```yaml
# .ava/core/en-US/my-feature.ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: my-feature.ava
  category: custom
---
NLU:my_action:do something
NLU:my_action:perform my action
```

### Step 2: Create Action Handler (NLU-Only)

```kotlin
class MyActionHandler : ActionHandler {
    override val intentId = "my_action"

    override suspend fun handle(input: String, params: Map<String, Any>?): ActionResult {
        // Implementation
        return ActionResult.Success("Action completed")
    }
}
```

### Step 3: Create Prompt Template (LLM-Required)

```yaml
# .ava/prompts/en-US/my_action.avp
---
schema: avp-1.0
metadata:
  intent: my_action
  processing_type: LLM_REQUIRED
---
system: |
  Instructions for LLM...
output_schema: |
  Expected output format...
examples:
  - input: "example"
    output: "example output"
```

### Step 4: Register Handler

```kotlin
// ActionsInitializer.kt
val actionHandlers = mapOf(
    "my_action" to MyActionHandler(),
    // ... other handlers
)
```

### Step 5: Regenerate Embeddings

```bash
cd tools/embedding-generator
python generate_embeddings.py --ava-core-dir ../../.ava/core/en-US ...
```

---

## Best Practices

### Intent Design

| Practice | Description |
|----------|-------------|
| Use NLU when possible | Faster, no LLM latency |
| Use HYB for parameters | "set X to Y" patterns |
| Use LLM for generation | Creative/complex output |
| 5-10 examples per intent | Better embedding accuracy |
| Diverse phrasing | Cover natural variations |

### Performance

| Metric | Target |
|--------|--------|
| NLU classification | <100ms |
| LLM response (on-device) | <2s |
| LLM response (cloud fallback) | <5s |
| Intent accuracy | >95% |

---

---

## NLUCoordinator (SOLID Architecture)

As part of the SOLID refactoring (Chapter 72), NLU functionality is now encapsulated in `NLUCoordinator`:

```kotlin
@Singleton
class NLUCoordinator @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val modelManager: ModelManager,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences
) {
    // State
    val isNLUReady: StateFlow<Boolean>
    val isNLULoaded: StateFlow<Boolean>
    val candidateIntents: StateFlow<List<String>>

    // Operations
    suspend fun initialize(): Result<Unit>
    suspend fun classify(utterance: String): IntentClassification?
    fun getCachedClassification(utterance: String): IntentClassification?
    suspend fun loadCandidateIntents()
    fun clearClassificationCache()
}
```

**Key Features:**
- LRU cache for classification results (configurable size via ChatPreferences)
- Intent cache with TTL for candidate intents
- Thread-safe via synchronized collections
- Single responsibility: NLU state and classification only

**See:** [Chapter 72: SOLID Architecture](Developer-Manual-Chapter72-SOLID-Architecture.md)

---

## Author

Manoj Jhawar
