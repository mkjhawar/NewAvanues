# VoiceOSCoreNG Command Flow Analysis

**Date:** 2026-01-08
**Version:** 1.0
**Status:** Complete
**Analysis Type:** Code (.swarm .auto)
**Method:** Chain of Thought (CoT)

---

## Executive Summary

This analysis traces the complete flow of voice commands in VoiceOSCoreNG from registration through propagation to execution. The system implements a **dual-track architecture** with static (system-wide) and dynamic (screen-specific) commands, using **priority-based handler dispatch** and **platform-specific execution** via Android AccessibilityService.

### Key Findings

| Metric | Value |
|--------|-------|
| Total Architecture Layers | 3 (Registration, Propagation, Execution) |
| Static Commands | 30+ predefined system commands |
| Handler Categories | 11 priority-ordered categories |
| Matching Algorithm | Modified Jaccard Index |
| Default Confidence Threshold | 0.7 (70%) |
| Execution Timeout | 5000ms |
| Result Types | 9 ActionResult variants |

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         VOICE INPUT LAYER                                   │
│  SpeechEngine → CommandWordDetector → Recognized Text                       │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      COMMAND REGISTRATION LAYER                             │
│  ┌─────────────────┐  ┌──────────────────────┐  ┌─────────────────────┐    │
│  │ CommandGenerator │  │ StaticCommandRegistry│  │   HandlerRegistry   │    │
│  │ (UI → Commands)  │  │ (30+ System Commands)│  │ (Action Handlers)   │    │
│  └────────┬────────┘  └───────────┬──────────┘  └─────────┬───────────┘    │
│           │                       │                        │                │
│           ▼                       ▼                        ▼                │
│      CommandRegistry       Pre-loaded             Priority-ordered          │
│      (Dynamic, Screen)     (Always Available)     (11 Categories)           │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     COMMAND PROPAGATION LAYER                               │
│  ┌───────────────────┐    ┌─────────────────┐    ┌───────────────────┐      │
│  │  CommandMatcher   │───▶│ CommandDispatcher│───▶│ ActionCoordinator │      │
│  │  (Jaccard Index)  │    │ (Mode Routing)   │    │ (Handler Lookup)  │      │
│  └───────────────────┘    └─────────────────┘    └───────────────────┘      │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      COMMAND EXECUTION LAYER                                │
│  ┌───────────────────┐    ┌───────────────────────┐    ┌────────────────┐  │
│  │    IHandler       │───▶│   IActionExecutor     │───▶│  ActionResult  │  │
│  │ (Platform-Agnostic)│    │ (Platform-Specific)  │    │ (9 Variants)   │  │
│  └───────────────────┘    └───────────────────────┘    └────────────────┘  │
│                                    │                                        │
│                                    ▼                                        │
│                      AndroidActionExecutor                                  │
│                      (AccessibilityService)                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Command Registration Layer

### 2.1 Static Command Registration

**Trigger:** Application startup (singleton initialization)

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/StaticCommandRegistry.kt`

```kotlin
object StaticCommandRegistry {
    val navigationCommands: List<StaticCommand>   // back, home, recents
    val mediaCommands: List<StaticCommand>        // play, pause, volume
    val systemCommands: List<StaticCommand>       // settings, notifications
    val voiceOSCommands: List<StaticCommand>      // voice control, dictation
    val appCommands: List<StaticCommand>          // launch browser, camera
    val accessibilityCommands: List<StaticCommand>// scroll actions
}
```

**Command Categories:**

| Category | Example Commands | Count |
|----------|------------------|-------|
| Navigation | "go back", "home", "recents" | 4 |
| Media | "play", "pause", "volume up/down" | 7 |
| System | "settings", "notifications", "screenshot" | 6 |
| VoiceOS | "voice mute", "show commands" | 5 |
| App Launch | "open browser", "open camera" | 8+ |
| Accessibility | "scroll up/down/left/right" | 4 |

### 2.2 Dynamic Command Registration

**Trigger:** Screen change (accessibility tree modification)

**Files:**
- `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandGenerator.kt`
- `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandRegistry.kt`

**Flow:**

```
Accessibility Tree Change
         │
         ▼
ElementParser.parse(accessibilityTree)
         │
         ▼
List<ElementInfo>
  ├── text: String
  ├── contentDescription: String
  ├── resourceId: String
  ├── className: String
  ├── bounds: Rect
  ├── isClickable: Boolean
  └── isScrollable: Boolean
         │
         ▼
CommandGenerator.fromElement(element, packageName)
         │
  ┌──────┴──────────────────────────────────────────────────────────┐
  │ 1. Filter: isActionable? hasVoiceContent? → Skip if false       │
  │ 2. Derive label: text > contentDesc > resourceId > ""           │
  │ 3. Skip if label blank or equals className                      │
  │ 4. Derive actionType: EditText→TYPE, Button→CLICK               │
  │ 5. Generate VUID: packageName + typeCode + SHA256(element)      │
  │ 6. Calculate confidence:                                        │
  │    base 0.5 + resourceId(0.2) + contentDesc(0.15) + label(0.1)  │
  └─────────────────────────────────────────────────────────────────┘
         │
         ▼
List<QuantizedCommand>
         │
         ▼
CommandRegistry.update(newCommands)
  └── REPLACE semantics: Clear all → Index by VUID (HashMap)
```

**QuantizedCommand Structure:**

```kotlin
data class QuantizedCommand(
    val uuid: String,           // Unique ID (same as VUID)
    val phrase: String,         // "tap Submit", "scroll down"
    val actionType: CommandActionType,  // CLICK, TYPE, SCROLL_*
    val targetVuid: String?,    // Target element identifier
    val confidence: Float,      // 0.0 - 1.0
    val metadata: Map<String, String>  // packageName, screenId
)
```

### 2.3 Handler Registration

**Trigger:** VoiceOSCoreNG service initialization

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/HandlerRegistry.kt`

**Priority Order (ActionCategory):**

| Priority | Category | Example Actions |
|----------|----------|-----------------|
| 1 | SYSTEM | settings, power, lock |
| 2 | NAVIGATION | back, home, scroll |
| 3 | APP | open, launch, close |
| 4 | GAZE | eye tracking actions |
| 5 | GESTURE | pinch, zoom, drag |
| 6 | UI | click, tap, focus |
| 7 | DEVICE | volume, brightness |
| 8 | INPUT | type, delete, paste |
| 9 | MEDIA | play, pause, skip |
| 10 | ACCESSIBILITY | speak, describe |
| 11 | CUSTOM | user-defined |

**Handler Interface:**

```kotlin
interface IHandler {
    val category: ActionCategory
    val supportedActions: List<String>

    suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult
    fun canHandle(action: String): Boolean
    suspend fun initialize()
    suspend fun dispose()
}
```

---

## 3. Command Propagation Layer

### 3.1 Voice Input → Matching

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandMatcher.kt`

**Algorithm: Modified Jaccard Index**

```
Input: "open settings"
Registered: "open system settings"

Step 1: Split into word sets
  inputWords = {"open", "settings"}
  phraseWords = {"open", "system", "settings"}

Step 2: Calculate intersection
  exactIntersection = {"open", "settings"} = 2

Step 3: Calculate partial matches
  partialMatches = 0 (no substrings)

Step 4: Calculate union
  union = {"open", "settings", "system"} = 3

Step 5: Calculate confidence
  confidence = (2 + 0) / 3 = 0.667

Result: Below threshold (0.7) → May not match
```

**Match Result Types:**

| Type | Condition | Action |
|------|-----------|--------|
| Exact | confidence = 1.0 | Execute immediately |
| Fuzzy | confidence >= threshold | Execute with confidence |
| Ambiguous | top 2 scores within 0.1 | User selection overlay |
| NoMatch | confidence < threshold | No execution |

### 3.2 Command Dispatch Modes

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/CommandDispatcher.kt`

```kotlin
enum class SpeechMode {
    STATIC_ONLY,      // System commands only
    DYNAMIC_ONLY,     // Screen-specific only
    COMBINED_COMMAND  // Try static first, then dynamic (default)
}
```

**Dispatch Flow:**

```
CommandDispatcher.dispatch(voiceInput, confidence)
         │
         ├── [If COMBINED_COMMAND]:
         │   │
         │   ├── Step 1: StaticCommandRegistry.findByPhrase()
         │   │   └── Match? → Execute + return
         │   │
         │   └── Step 2: CommandMatcher.match(dynamicRegistry)
         │       └── Match? → Execute + return
         │
         └── Return DispatchResult (Success/Failed/Ambiguous/NoMatch)
```

### 3.3 Handler Selection

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/ActionCoordinator.kt`

```
ActionCoordinator.processCommand(command)
         │
         ▼
HandlerRegistry.findHandler(command)
         │
  ┌──────┴──────────────────────────────────────────┐
  │ For each category in PRIORITY_ORDER:           │
  │   For each handler in category:                │
  │     if handler.canHandle(command.phrase):      │
  │       return handler                           │
  └────────────────────────────────────────────────┘
         │
         ▼
handler.execute(command) with 5000ms timeout
         │
         ▼
Record metrics + Emit CommandResult
```

---

## 4. Command Execution Layer

### 4.1 Executor Interface

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/IActionExecutor.kt`

**24 Suspending Methods:**

| Category | Methods |
|----------|---------|
| Element | tap, longPress, focus, enterText |
| Scroll | scroll(direction, amount, vuid?) |
| Navigation | back, home, recentApps, appDrawer |
| System | openSettings, showNotifications, screenshot, flashlight |
| Media | mediaPlayPause, mediaNext, mediaPrevious, volume |
| App | openApp, openAppByPackage, closeApp |
| Generic | executeCommand, executeAction |
| Lookup | elementExists, getElementBounds |

### 4.2 Android Implementation

**File:** `src/androidMain/kotlin/com/augmentalis/voiceoscoreng/handlers/AndroidActionExecutor.kt`

**Two-Tier Execution Strategy:**

```
AndroidActionExecutor.tap(vuid)
         │
         ▼
withContext(Dispatchers.Main) {  // Main thread required
         │
         ├── Step 1: Find element
         │   ├── Check cache: elementCache[vuid]?
         │   └── DFS traversal: searchForVuid(root, vuid)
         │       └── Match by: resourceId / text / contentDescription
         │
         ├── Step 2: Primary execution
         │   └── node.performAction(ACTION_CLICK)
         │       ├── Success → ActionResult.Success
         │       └── Failure → Try fallback
         │
         └── Step 3: Gesture fallback
             └── dispatchGesture(GestureDescription)
                 └── Callback → ActionResult
}
```

**VUID Resolution:**

```kotlin
private fun findNodeByVuid(vuid: String): AccessibilityNodeInfo? {
    // 1. Check cache (O(1))
    elementCache[vuid]?.let { return it }

    // 2. DFS traversal
    val root = service.rootInActiveWindow ?: return null
    return searchForVuid(root, vuid)
}
```

### 4.3 Result Types

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/ActionResult.kt`

```kotlin
sealed class ActionResult {
    data class Success(val message: String, val data: Map<String, Any>? = null)
    data class ElementNotFound(val vuid: String)
    data class ElementNotActionable(val vuid: String, val reason: String)
    data class NotSupported(val actionType: CommandActionType)
    data class PermissionRequired(val permission: String)
    data class Timeout(val timeoutMs: Long)
    data class Error(val error: String, val exception: Throwable? = null)
    data class ServiceUnavailable(val serviceName: String)
    data class ConfirmationRequired(val prompt: String, val action: String)
    data class Ambiguous(val candidates: List<String>)
}
```

---

## 5. Handler Implementations

### 5.1 Handler Summary

| Handler | Category | Supported Actions | File |
|---------|----------|-------------------|------|
| NavigationHandler | NAVIGATION | scroll up/down/left/right, swipe, page up/down | `NavigationHandler.kt` |
| UIHandler | UI | click, tap, long press, double tap, expand, collapse, toggle | `UIHandler.kt` |
| SystemHandler | SYSTEM | back, home, recents, notifications, power menu, lock | `SystemHandler.kt` |
| AppHandler | APP | open, launch, start (+ app name) | `AppHandler.kt` |
| InputHandler | INPUT | type, delete, clear, copy, cut, paste, undo, redo | `InputHandler.kt` |
| DeviceHandler | DEVICE | volume up/down, brightness, flashlight, screen on/off | `DeviceHandler.kt` |
| GestureHandler | GESTURE | tap, double tap, long press, swipe, pinch, rotate | `GestureHandler.kt` |
| SelectHandler | UI | select, select all, copy, cut, paste, clear selection | `SelectHandler.kt` |

### 5.2 Security Features

**InputHandler Security:**

```kotlin
// Input validation
private fun validateInput(text: String): Boolean {
    if (text.length > 10000) return false  // Max 10KB

    // XSS protection
    if (text.contains("<script") ||
        text.contains("javascript:") ||
        text.contains("onclick=")) return false

    // SQL injection protection
    if (text.contains("'; DROP TABLE") ||
        text.contains("UNION SELECT")) return false

    return true
}
```

---

## 6. Complete Flow Example

**User says:** "tap Submit"

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. SPEECH ENGINE                                                            │
│    Input: Audio waveform                                                    │
│    Output: voiceText="tap Submit", confidence=0.95                          │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. COMMAND DISPATCHER                                                       │
│    Mode: COMBINED_COMMAND                                                   │
│    Step 1: StaticCommandRegistry.findByPhrase("tap Submit") → null          │
│    Step 2: CommandMatcher.match(dynamicRegistry) → Exact match!             │
│    Output: QuantizedCommand(phrase="tap Submit", actionType=CLICK,          │
│                             vuid="abc123", confidence=0.85)                 │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. ACTION COORDINATOR                                                       │
│    HandlerRegistry.findHandler(command)                                     │
│    Priority scan: SYSTEM✗ → NAV✗ → APP✗ → GAZE✗ → GESTURE✗ → UI✓            │
│    UIHandler.canHandle("tap Submit") → true                                 │
│    Output: UIHandler selected                                               │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. UI HANDLER                                                               │
│    Parse: actionType=TAP, targetVuid="abc123"                               │
│    Call: executor.tap("abc123")                                             │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 5. ANDROID ACTION EXECUTOR                                                  │
│    withContext(Dispatchers.Main) {                                          │
│      val node = findNodeByVuid("abc123")  // Found via DFS                  │
│      node.performAction(ACTION_CLICK)      // Primary: SUCCESS              │
│    }                                                                        │
│    Output: ActionResult.Success("Tapped element")                           │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 6. RESULT PROPAGATION                                                       │
│    ActionResult.Success → HandlerResult.Success → DispatchResult.Success    │
│                                                                             │
│    Side effects:                                                            │
│    ├── Metrics: duration=45ms, success_rate=98.5%                           │
│    ├── Overlay: Flash "✓ Tapped"                                            │
│    └── Voice: "Done"                                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Critical Issues Identified

### 7.1 Database Not Being Populated

**Root Cause:** No connection between `VoiceOSAccessibilityService` (exploration) and database persistence layer.

**Missing Integration:**
```kotlin
// Should exist in VoiceOSAccessibilityService
val databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)
val commandPersistence = AndroidCommandPersistence(databaseManager.generatedCommands)
```

**Impact:** Learned commands are not persisted across sessions.

### 7.2 Voice Engine Not Initialized

**Root Cause:** Two classes with same name `VoiceOSCoreNG` causing confusion.

**Issue:**
- Handler object `VoiceOSCoreNG` (in handlers package) is being used
- Facade class `VoiceOSCoreNG` with `createForAndroid()` is never instantiated

**Impact:** Speech recognition engine not started.

### 7.3 Screen Change Trigger Missing

**Root Cause:** `VoiceOSAccessibilityService.onAccessibilityEvent()` doesn't trigger command regeneration.

**Missing:**
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
        TYPE_WINDOW_STATE_CHANGED,
        TYPE_WINDOW_CONTENT_CHANGED -> {
            // Should trigger: CommandGenerator → CommandRegistry.update()
        }
    }
}
```

**Impact:** Dynamic commands not updated on screen changes.

---

## 8. Key Files Reference

| Layer | File | Purpose |
|-------|------|---------|
| **Registration** | `common/CommandGenerator.kt` | UI element → QuantizedCommand |
| | `common/CommandRegistry.kt` | Dynamic command storage (screen-specific) |
| | `common/StaticCommandRegistry.kt` | System command storage (30+ predefined) |
| | `handlers/HandlerRegistry.kt` | Handler registration and lookup |
| **Propagation** | `common/CommandMatcher.kt` | Jaccard similarity matching |
| | `handlers/CommandDispatcher.kt` | Mode-based routing (static/dynamic) |
| | `handlers/ActionCoordinator.kt` | Handler orchestration with timeout |
| **Execution** | `handlers/IHandler.kt` | Handler contract (11 implementations) |
| | `handlers/IActionExecutor.kt` | Execution contract (24 methods) |
| | `handlers/AndroidActionExecutor.kt` | Android AccessibilityService impl |
| | `handlers/ActionResult.kt` | 9 result type variants |
| **Data Models** | `common/QuantizedCommand.kt` | Voice command representation |
| | `common/CommandActionType.kt` | 40+ action type enum |
| | `handlers/ActionCategory.kt` | 11 priority-ordered categories |

---

## 9. Quality Assessment

### Architecture Score: 8/10

| Criteria | Score | Notes |
|----------|-------|-------|
| Modularity | 9/10 | Clear layer separation |
| Extensibility | 9/10 | Handler pattern allows easy additions |
| Thread Safety | 8/10 | Mutex + Main dispatcher |
| Error Handling | 8/10 | 9 typed result variants |
| Platform Abstraction | 7/10 | Good KMP structure, stubs for iOS/Desktop |
| Integration | 6/10 | Critical gaps in service wiring |

### Issues: 3 Critical, 0 High, 0 Medium

| Issue | Severity | Status |
|-------|----------|--------|
| Database not populated | Critical | Needs `AndroidCommandPersistence` wiring |
| Voice engine not initialized | Critical | Wrong class reference |
| Screen change trigger missing | Critical | Missing event handler |

---

## 10. Recommendations

### Immediate (P0)

1. **Wire Database Persistence**
   - Create `SqlDriverFactory` in `VoiceOSCoreNGApplication`
   - Instantiate `AndroidCommandPersistence`
   - Connect to `VoiceOSAccessibilityService`

2. **Fix Voice Engine Initialization**
   - Use facade `VoiceOSCoreNG.createForAndroid()`
   - Remove confusion with handler object

3. **Implement Screen Change Trigger**
   - Add event handling in `onAccessibilityEvent()`
   - Trigger `CommandRegistry.update()` on screen changes

### Short-term (P1)

4. **Add Persistence Layer Tests**
   - Verify command storage/retrieval
   - Test across screen changes

5. **Implement NLU Integration**
   - Connect ActionCoordinator to NLU module
   - Enable semantic command understanding

---

**Analysis Complete**

*Generated by VoiceOSCoreNG Code Analysis Swarm*
