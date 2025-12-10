# CommandManager → VoiceOSService Integration TODO

**Created:** 2025-10-10 14:23:00 PDT
**Source:** CommandManager Architecture Analysis (251010-1423)
**Priority:** HIGH - Required for voice command execution
**Status:** Not Started

---

## Overview

Integrate CommandManager with VoiceOSService to enable voice-triggered accessibility actions. This TODO tracks the 4-phase implementation plan from analysis to production.

**Related Documents:**
- [Architecture Analysis](/coding/STATUS/CommandManager-Architecture-Analysis-251010-1423.md)
- [Architecture Documentation](/docs/voiceos-master/architecture/Voice-Command-System-Complete-Architecture-251010-1326.md)

---

## Phase 1: Core Integration (Week 1)

### ✅ Prerequisites
- [ ] Review architecture analysis document
- [ ] Approve integration approach (Option A+D recommended)
- [ ] Answer discussion questions (Section 9 of analysis)
- [ ] Define MVP action set (which actions are critical?)

### 🔧 Implementation Tasks

#### 1.1 Create AccessibilityServiceProvider
**File:** `CommandManager/src/main/java/com/augmentalis/commandmanager/service/AccessibilityServiceProvider.kt`

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Create ServiceProvider object
- [ ] Implement bind/unbind methods
- [ ] Add get() and requireService() methods
- [ ] Add logging for bind/unbind events
- [ ] Write unit tests for ServiceProvider

**Code Template:**
```kotlin
object AccessibilityServiceProvider {
    private var service: AccessibilityService? = null

    fun bind(service: AccessibilityService) { /* ... */ }
    fun unbind() { /* ... */ }
    fun get(): AccessibilityService? = service
    fun requireService(): AccessibilityService = /* ... */
}
```

---

#### 1.2 Update CommandManager with Service Binding
**File:** `CommandManager/src/main/java/com/augmentalis/commandmanager/CommandManager.kt`

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Add `accessibilityService` property
- [ ] Implement `bindAccessibilityService(service)` method
- [ ] Implement `unbindAccessibilityService()` method
- [ ] Update `executeCommandInternal()` to use service
- [ ] Add NO_ACCESSIBILITY_SERVICE error handling
- [ ] Update existing action mappings to use service
- [ ] Write unit tests with mock service

**Changes Required:**
```kotlin
class CommandManager(private val context: Context) {
    private var accessibilityService: AccessibilityService? = null

    // NEW
    fun bindAccessibilityService(service: AccessibilityService) { /* ... */ }
    fun unbindAccessibilityService() { /* ... */ }

    // MODIFY
    private suspend fun executeCommandInternal(command: Command): CommandResult {
        if (accessibilityService == null) {
            return CommandResult(/* NO_ACCESSIBILITY_SERVICE error */)
        }
        // ... execute with service
    }
}
```

---

#### 1.3 Update BaseAction for Service Access
**File:** `CommandManager/src/main/java/com/augmentalis/commandmanager/actions/BaseAction.kt`

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Add `getAccessibilityService(command)` helper method
- [ ] Update `performGlobalAction()` to use ServiceProvider as fallback
- [ ] Update `getContext()` method for better context extraction
- [ ] Add null-safety checks for service access
- [ ] Document service access patterns
- [ ] Update all action subclasses if needed

**Changes Required:**
```kotlin
abstract class BaseAction {
    protected fun getAccessibilityService(command: Command): AccessibilityService? {
        return /* try command context */ ?: AccessibilityServiceProvider.get()
    }

    protected fun performGlobalAction(
        service: AccessibilityService?,
        action: Int
    ): Boolean {
        val actualService = service ?: AccessibilityServiceProvider.get()
        return actualService?.performGlobalAction(action) ?: false
    }
}
```

---

#### 1.4 Bind CommandManager in VoiceOSService
**File:** `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/VoiceOSService.kt`

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Add CommandManager property
- [ ] Add CommandLocalizer property
- [ ] Bind CommandManager in `onServiceConnected()`
- [ ] Initialize CommandLocalizer with locale loading
- [ ] Unbind CommandManager in `onDestroy()`
- [ ] Implement `handleVoiceCommand(recognizedText)` method
- [ ] Integrate with speech recognition pipeline
- [ ] Add error handling for command resolution failures
- [ ] Add user feedback for command execution results
- [ ] Write integration tests

**Implementation:**
```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var commandManager: CommandManager
    private lateinit var commandLocalizer: CommandLocalizer

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Bind CommandManager
        commandManager = CommandManager.getInstance(this)
        commandManager.bindAccessibilityService(this)

        // Initialize Localizer
        commandLocalizer = CommandLocalizer.create(this)
        lifecycleScope.launch {
            commandLocalizer.initialize()
        }
    }

    override fun onDestroy() {
        commandManager.unbindAccessibilityService()
        super.onDestroy()
    }

    suspend fun handleVoiceCommand(recognizedText: String) {
        // 1. Resolve command
        val voiceCommand = commandLocalizer.resolveCommand(recognizedText)
        if (voiceCommand == null) {
            // Show "command not found" feedback
            return
        }

        // 2. Create Command
        val command = Command(
            id = voiceCommand.id,
            text = recognizedText,
            source = CommandSource.VOICE,
            context = CommandContext(
                packageName = rootInActiveWindow?.packageName?.toString()
            )
        )

        // 3. Execute
        val result = commandManager.executeCommand(command)

        // 4. Handle result
        if (result.success) {
            // Show success feedback
        } else {
            // Show error feedback
        }
    }
}
```

---

#### 1.5 Testing Phase 1
**Status:** ⏸️ Not Started

**Test Checklist:**
- [ ] Unit test: ServiceProvider bind/unbind
- [ ] Unit test: CommandManager with mock service
- [ ] Unit test: BaseAction service access
- [ ] Integration test: VoiceOSService binding
- [ ] Integration test: Command resolution → execution flow
- [ ] E2E test: "go back" voice command
- [ ] E2E test: "volume up" voice command
- [ ] E2E test: Service disconnect handling

**Test Files to Create:**
- `CommandManager/src/test/.../ServiceProviderTest.kt`
- `CommandManager/src/test/.../CommandManagerIntegrationTest.kt`
- `VoiceAccessibility/src/androidTest/.../VoiceOSServiceTest.kt`

---

## Phase 2: Missing Actions Implementation (Week 2)

### 2.1 DictationActions (PRIORITY: HIGH)
**File:** `CommandManager/actions/DictationActions.kt`

**Status:** ⏸️ Not Started

**Actions to Implement:**
- [ ] `StartDictationAction` - Begin speech-to-text
- [ ] `StopDictationAction` - End dictation
- [ ] `InsertDictatedTextAction` - Insert recognized text at cursor
- [ ] `DictationModeToggleAction` - Toggle continuous dictation
- [ ] `PunctuationModeAction` - Add punctuation ("comma", "period")

**Dependencies:**
- [ ] Speech recognizer integration (Vivoka or Android STT)
- [ ] Text insertion to focused AccessibilityNodeInfo
- [ ] Microphone permission handling
- [ ] Dictation state management

**Questions to Answer:**
- Use Vivoka engine or Android SpeechRecognizer?
- Continuous dictation or discrete mode?
- How to handle punctuation commands?

---

### 2.2 CursorActions (PRIORITY: HIGH)
**File:** `CommandManager/actions/CursorActions.kt`

**Status:** ⏸️ Not Started

**Actions to Implement:**
- [ ] `ShowCursorAction` - Display voice cursor overlay
- [ ] `HideCursorAction` - Hide cursor overlay
- [ ] `MoveCursorAction` - Move to x,y coordinates
- [ ] `SnapCursorAction` - Snap to nearest element
- [ ] `CursorClickAction` - Click at cursor position
- [ ] `CursorLongClickAction` - Long press at cursor
- [ ] `CenterCursorAction` - Move cursor to screen center

**Dependencies:**
- [ ] VoiceCursor module API integration
- [ ] Overlay window state management
- [ ] Coordinate translation (voice → screen)

**Integration Decision:**
- [ ] Delegate to VoiceCursor API (RECOMMENDED)
- [ ] OR: Duplicate cursor functionality in CommandManager?

---

### 2.3 OverlayActions (PRIORITY: MEDIUM)
**File:** `CommandManager/actions/OverlayActions.kt`

**Status:** ⏸️ Not Started

**Actions to Implement:**
- [ ] `ShowOverlayAction` - Display overlay window
- [ ] `HideOverlayAction` - Remove overlay
- [ ] `UpdateOverlayPositionAction` - Move overlay to coordinates
- [ ] `SetOverlayTransparencyAction` - Adjust opacity (0-100%)

**Dependencies:**
- [ ] WindowManager integration
- [ ] SYSTEM_ALERT_WINDOW permission check
- [ ] Overlay layout management

**Permission Handling:**
- [ ] Check if SYSTEM_ALERT_WINDOW granted
- [ ] Graceful fallback when permission denied
- [ ] Guide user to grant permission

---

### 2.4 GestureActions (PRIORITY: MEDIUM)
**File:** `CommandManager/actions/GestureActions.kt`

**Status:** ⏸️ Not Started

**Actions to Implement:**
- [ ] `PinchZoomInAction` - Two-finger pinch to zoom in
- [ ] `PinchZoomOutAction` - Two-finger pinch to zoom out
- [ ] `RotateGestureAction` - Two-finger rotate
- [ ] `TwoFingerTapAction` - Dual tap gesture
- [ ] `ThreeFingerSwipeAction` - Triple finger swipe

**Dependencies:**
- [ ] Multi-touch gesture support via GestureDescription
- [ ] Coordinate calculation for multi-point gestures
- [ ] Gesture duration tuning

**Complexity:** HIGH - Requires advanced gesture synthesis

---

### 2.5 DragActions (PRIORITY: LOW)
**File:** `CommandManager/actions/DragActions.kt`

**Status:** ⏸️ Not Started

**Actions to Implement:**
- [ ] `DragToPositionAction` - Drag element to coordinates
- [ ] `DragAndDropAction` - Drag from element A to element B
- [ ] `LongPressDragAction` - Long press then drag

**Dependencies:**
- [ ] Element tracking during drag operation
- [ ] Touch event synthesis with GestureDescription
- [ ] Duration calculation for natural drag feel

---

### 2.6 Enhanced AppActions (PRIORITY: MEDIUM)
**File:** `CommandManager/actions/AppActions.kt` (UPDATE)

**Status:** ⏸️ Not Started

**Actions to Add:**
- [ ] `CloseAppAction` - Force stop current app (requires permission)
- [ ] `ClearAppDataAction` - Clear app cache/data
- [ ] `AppInfoAction` - Open app info settings screen
- [ ] `SwitchToAppAction` - Switch to specific app by name

**Dependencies:**
- [ ] ActivityManager integration
- [ ] PackageManager operations
- [ ] Permission handling for force-stop

---

## Phase 3: Command Integration (Week 3)

### 3.1 Update JSON Command Files
**Files to Update:**
- `/assets/localization/commands/en-US.json`
- `/assets/localization/commands/es-ES.json`
- `/assets/localization/commands/fr-FR.json`
- `/assets/localization/commands/de-DE.json`

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Add dictation commands (start, stop, insert)
- [ ] Add cursor commands (show, hide, move, snap)
- [ ] Add overlay commands (show, hide, transparency)
- [ ] Add gesture commands (pinch, rotate, tap)
- [ ] Add drag commands (drag, drop)
- [ ] Add enhanced app commands (close, clear data, info)
- [ ] Translate all commands to supported locales
- [ ] Update JSON version to "1.1" (force reload)

**Example Additions (en-US.json):**
```json
{
  "version": "1.1",
  "commands": [
    ["dictation_start", "start dictation", ["begin dictation", "dictate"], "Start speech-to-text"],
    ["dictation_stop", "stop dictation", ["end dictation", "finish dictating"], "Stop dictation"],
    ["cursor_show", "show cursor", ["enable cursor", "cursor on"], "Display voice cursor"],
    ["cursor_hide", "hide cursor", ["disable cursor", "cursor off"], "Hide voice cursor"],
    ["overlay_show", "show overlay", ["display overlay"], "Show overlay window"],
    ["gesture_pinch_zoom", "zoom in", ["pinch zoom", "enlarge"], "Pinch to zoom gesture"],
    ["drag_element", "drag", ["move element"], "Drag element to position"],
    ["app_close", "close app", ["force close", "quit app"], "Force stop application"]
  ]
}
```

---

### 3.2 Update CommandManager Action Mappings
**File:** `CommandManager.kt`

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Add `dictationActions` map
- [ ] Add `cursorActions` map
- [ ] Add `overlayActions` map
- [ ] Add `gestureActions` map
- [ ] Add `dragActions` map
- [ ] Update `executeCommandInternal()` with new mappings
- [ ] Add logging for action type resolution

**Code Changes:**
```kotlin
class CommandManager(private val context: Context) {
    // NEW action maps
    private val dictationActions = mapOf(
        "dictation_start" to DictationActions.StartDictationAction(),
        "dictation_stop" to DictationActions.StopDictationAction()
    )

    private val cursorActions = mapOf(
        "cursor_show" to CursorActions.ShowCursorAction(),
        "cursor_hide" to CursorActions.HideCursorAction()
    )

    // ... other maps

    private suspend fun executeCommandInternal(command: Command): CommandResult {
        val action = when {
            command.id.startsWith("dictation_") -> dictationActions[command.id]
            command.id.startsWith("cursor_") -> cursorActions[command.id]
            command.id.startsWith("overlay_") -> overlayActions[command.id]
            command.id.startsWith("gesture_") -> gestureActions[command.id]
            command.id.startsWith("drag_") -> dragActions[command.id]
            // ... existing mappings
            else -> null
        }
        // ...
    }
}
```

---

### 3.3 Force Reload Commands
**Location:** Settings screen or debug menu

**Status:** ⏸️ Not Started

**Checklist:**
- [ ] Add "Reload Commands" button to settings
- [ ] Implement reload logic using `CommandLoader.forceReload()`
- [ ] Show progress indicator during reload
- [ ] Display success/error toast
- [ ] Update command count display
- [ ] Log reload event for debugging

**Implementation:**
```kotlin
// In settings screen
binding.btnReloadCommands.setOnClickListener {
    lifecycleScope.launch {
        binding.progressReload.visibility = View.VISIBLE

        val loader = CommandLoader.create(context)
        val result = loader.forceReload()

        binding.progressReload.visibility = View.GONE

        when (result) {
            is CommandLoader.LoadResult.Success -> {
                Toast.makeText(
                    context,
                    "Commands reloaded: ${result.commandCount}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is CommandLoader.LoadResult.Error -> {
                Toast.makeText(
                    context,
                    "Reload failed: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

---

## Phase 4: Testing & Validation (Week 4)

### 4.1 Unit Tests
**Status:** ⏸️ Not Started

**Test Files to Create:**
- [ ] `CommandManagerIntegrationTest.kt`
  - Test service binding/unbinding
  - Test command execution with mock service
  - Test error handling for missing service
  - Test action resolution logic

- [ ] `ServiceProviderTest.kt`
  - Test bind/unbind lifecycle
  - Test service retrieval
  - Test requireService() exception

- [ ] `DictationActionsTest.kt`
  - Test dictation start/stop
  - Test text insertion
  - Test mode toggle

- [ ] `CursorActionsTest.kt`
  - Test cursor show/hide
  - Test cursor movement
  - Test snap to element

- [ ] `CommandLocalizerTest.kt`
  - Test locale switching
  - Test fallback resolution
  - Test multi-locale matching

**Coverage Target:** 80%+

---

### 4.2 Integration Tests
**Status:** ⏸️ Not Started

**Test Scenarios:**
- [ ] **Voice → Database → Action Flow**
  - User says "go back" → Resolved to "navigate_backward" → Back action executed

- [ ] **Locale Switching**
  - Switch to Spanish → Say "adelante" → Forward action executed
  - Fallback to English if Spanish command not found

- [ ] **Service Lifecycle**
  - VoiceOSService connects → CommandManager binds
  - Service disconnects → Commands fail gracefully
  - Service reconnects → Commands work again

- [ ] **Fuzzy Matching**
  - Say "goo bacck" (typo) → Resolved to "go back" (70% match)
  - Say "volum up" → Resolved to "volume up"

- [ ] **Confidence Filtering**
  - Low confidence (< 0.6) → Rejected
  - Medium confidence (0.6-0.8) → Confirmation requested
  - High confidence (> 0.8) → Executed immediately

---

### 4.3 End-to-End Tests
**Status:** ⏸️ Not Started

**User Flow Tests:**
1. **Basic Navigation**
   - [ ] User says "go back" → Back button pressed
   - [ ] User says "go home" → Home screen shown
   - [ ] User says "recent apps" → App switcher opens

2. **Multi-Locale**
   - [ ] User switches language to Spanish
   - [ ] User says "empezar dictado" → Dictation starts
   - [ ] User says "mostrar cursor" → Cursor appears

3. **Cursor Control**
   - [ ] User says "show cursor" → Cursor overlay appears
   - [ ] User says "move cursor" → Cursor moves to coordinates
   - [ ] User says "click" → Element at cursor clicked
   - [ ] User says "hide cursor" → Cursor disappears

4. **App Control**
   - [ ] User says "close app" → Current app force closes
   - [ ] User says "open settings" → Settings app opens
   - [ ] User says "switch to chrome" → Chrome app activates

5. **Dictation**
   - [ ] User says "start dictation" → Dictation mode begins
   - [ ] User says "hello world" → Text inserted
   - [ ] User says "stop dictation" → Dictation mode ends

**Test Environment:**
- Real device (not emulator - accessibility features required)
- Multiple apps for testing (Chrome, Gmail, Calculator)
- Multiple locales configured

---

## Phase 5: Documentation & Deployment

### 5.1 Update Documentation
**Status:** ⏸️ Not Started

**Documents to Update:**
- [ ] **CommandManager Developer Guide**
  - Add service binding section
  - Document new actions
  - Add troubleshooting guide

- [ ] **VoiceOSService Integration Guide**
  - Document CommandManager integration
  - Add flow diagrams
  - Add code examples

- [ ] **User Manual**
  - List all available commands
  - Explain multi-locale support
  - Add dictation guide

- [ ] **API Reference**
  - Document all action classes
  - Document CommandLocalizer API
  - Document ServiceProvider API

---

### 5.2 Performance Validation
**Status:** ⏸️ Not Started

**Metrics to Measure:**
- [ ] Command resolution latency (target: < 100ms)
- [ ] Database lookup time (target: < 10ms)
- [ ] Action execution time (varies by action)
- [ ] Memory usage (CommandManager + database)
- [ ] App startup time impact

**Profiling:**
- [ ] Use Android Profiler for memory analysis
- [ ] Use Systrace for performance tracing
- [ ] Add custom trace markers for command pipeline

---

### 5.3 Deployment Checklist
**Status:** ⏸️ Not Started

**Pre-Release:**
- [ ] All Phase 1-4 tasks completed
- [ ] All tests passing (unit + integration + E2E)
- [ ] Documentation updated
- [ ] Performance validated
- [ ] Security review completed
- [ ] Accessibility audit passed

**Release:**
- [ ] Merge to main branch
- [ ] Tag release version
- [ ] Update changelog
- [ ] Build release APK
- [ ] Upload to Play Store (beta)

**Post-Release:**
- [ ] Monitor crash reports
- [ ] Monitor command success rates
- [ ] Collect user feedback
- [ ] Plan iteration based on analytics

---

## Open Questions (From Analysis Section 9)

### Architecture
- [ ] **Q1:** Service disconnect handling strategy?
  - Queue commands when service unavailable?
  - Retry logic for failed commands?
  - Timeout threshold?

- [ ] **Q2:** MVP action priority?
  - Which actions are CRITICAL for v1?
  - Can we defer gestures/overlays to v2?
  - Dictation vs external speech engine?

- [ ] **Q3:** Performance tuning?
  - In-memory cache needed?
  - Preload common locales?
  - Command execution timeout?

### Implementation
- [ ] **Q4:** VoiceCursor integration approach?
  - Delegate to VoiceCursor API?
  - Duplicate functionality?
  - State management ownership?

- [ ] **Q5:** Dictation engine choice?
  - Use Vivoka engine?
  - Use Android SpeechRecognizer?
  - Continuous vs discrete mode?

- [ ] **Q6:** Overlay permissions UX?
  - How to request SYSTEM_ALERT_WINDOW?
  - Fallback when denied?
  - Context restrictions?

### Features
- [ ] **Q7:** Missing features priority?
  - Macro support (command sequences)?
  - Context-aware commands?
  - User-defined custom commands?
  - Analytics and learning?

- [ ] **Q8:** Additional locales?
  - Which languages to prioritize?
  - Regional dialect support?
  - User-contributed translations?

---

## Dependencies & Risks

### Dependencies
- **VoiceOSService** - Must be connected for actions to work
- **VoiceCursor Module** - For cursor-related actions
- **Speech Recognition** - For dictation actions
- **Room Database** - For command storage
- **Accessibility Service API** - For all actions

### Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Service disconnection during command | HIGH | Queue commands, retry logic |
| Permission denial (overlay, mic) | MEDIUM | Graceful fallback, user guidance |
| Locale file not found | LOW | English fallback always available |
| Action not implemented | MEDIUM | Clear error message, suggest alternatives |
| Performance degradation | MEDIUM | Profiling, optimization, lazy loading |

---

## Success Criteria

### Phase 1 Success (Core Integration)
- [ ] VoiceOSService successfully binds CommandManager
- [ ] Basic commands work (nav, volume, system)
- [ ] Service lifecycle handled correctly
- [ ] All Phase 1 tests pass

### Phase 2 Success (Missing Actions)
- [ ] All priority actions implemented (dictation, cursor)
- [ ] Medium priority actions completed (overlay, app)
- [ ] Action tests pass with > 80% coverage

### Phase 3 Success (Command Integration)
- [ ] JSON files updated with new commands
- [ ] All locales include new commands
- [ ] Force reload works correctly
- [ ] Command count increases as expected

### Phase 4 Success (Testing)
- [ ] 100% of unit tests pass
- [ ] 100% of integration tests pass
- [ ] All E2E user flows work
- [ ] Performance metrics meet targets

### Overall Success (MVP)
- [ ] Voice commands execute actions successfully
- [ ] Multi-locale support works with fallback
- [ ] No crashes or ANRs
- [ ] User feedback positive
- [ ] Ready for beta release

---

**TODO Status:** Created - Awaiting Phase 1 Implementation
**Last Updated:** 2025-10-10 14:23:00 PDT
**Next Review:** After Q&A session with team
**Assigned To:** VOS4 Development Team
