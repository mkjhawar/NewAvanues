# VoiceCursor Module - Current Architecture Analysis

**Created:** 2025-10-10 17:54:00 PDT
**Author:** VOS4 Development Team
**Purpose:** Comprehensive architectural analysis for refactoring preparation
**Status:** BLOCKING - All implementation depends on this analysis

---

## Executive Summary

**Complexity Rating:** MODERATE
**Blockers Identified:** 2 (Dependencies on DeviceManager, Command routing infrastructure)
**Recommendation:** PROCEED with refactoring in phases
**Estimated Effort:** 3-5 days for complete separation

### Key Findings

1. **Current State:** VoiceCursor has MIXED concerns - cursor mechanics are reasonably separated, but command handling logic is tightly integrated
2. **Good News:** VoiceCursorAPI already exists and provides a clean public interface
3. **Challenge:** Command handling is split across 3 files with overlapping responsibilities
4. **Opportunity:** Clear separation points exist - can extract command logic cleanly

---

## File Structure Analysis

### Source Files (24 Kotlin/Java files)

#### **1. Core Entry Points**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/VoiceCursor.kt` (275 lines)
  - **Role:** Legacy main entry point (DEPRECATED)
  - **Concerns:** Initialization, IMU integration, voice integration coordination
  - **Status:** Marked deprecated in favor of VoiceCursorAPI

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/VoiceCursorAPI.kt` (237 lines)
  - **Role:** Modern public API (CURRENT)
  - **Concerns:** Pure cursor mechanics interface
  - **Quality:** EXCELLENT - Clean separation, well-documented
  - **Methods:** initialize(), showCursor(), hideCursor(), centerCursor(), executeAction(), updateConfiguration()

#### **2. Command Handling (TANGLED - NEEDS REFACTORING)**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/commands/CursorCommandHandler.kt` (739 lines)
  - **Role:** Unified command handler and voice accessibility integration
  - **Concerns:** Command registration, routing, parsing, execution, TTS feedback
  - **Issues:** Mixed command logic with cursor mechanics calls
  - **Dependencies:** VoiceCursor, VoiceCursorAPI, voice integration

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/integration/VoiceAccessibilityIntegration.kt` (202 lines)
  - **Role:** Voice command integration stub
  - **Concerns:** Command pattern registration, voice processing
  - **Status:** Placeholder implementation (simulated)

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/helper/CursorHelper.kt`
  - **Role:** Helper utilities (not examined in detail)

#### **3. Cursor Mechanics (CLEAN - KEEP IN VOICECURSOR)**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/manager/CursorOverlayManager.kt` (392 lines)
  - **Role:** Manages cursor overlay without being a service
  - **Concerns:** WindowManager integration, overlay lifecycle, configuration
  - **Quality:** EXCELLENT - Extracted from service architecture
  - **Dependencies:** CursorView, CursorGestureHandler, IMU integration

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/CursorView.kt` (995 lines)
  - **Role:** Main cursor rendering and interaction view
  - **Concerns:** Rendering, position tracking, gaze click, gestures, animations
  - **Quality:** GOOD - Well-structured with callbacks
  - **Components:** CursorRenderer, CursorPositionManager, GazeClickManager, CursorFilter, CursorAnimator, GestureManager

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/manager/CursorGestureHandler.kt` (285 lines)
  - **Role:** Dispatches accessibility gestures
  - **Concerns:** Click, long-press, drag, scroll gesture execution
  - **Quality:** EXCELLENT - Pure gesture mechanics
  - **Dependencies:** AccessibilityService (injected)

#### **4. Core Components**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/CursorPositionManager.kt` (394 lines)
  - **Role:** Thread-safe position calculations and movement
  - **Quality:** EXCELLENT - Edge detection, smoothing, filtering

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/CursorRenderer.kt`
  - **Role:** Cursor visual rendering with ARVision theme

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/GazeClickManager.kt`
  - **Role:** Gaze-dwell click detection

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/CursorTypes.kt` (131 lines)
  - **Role:** Core data types (CursorType, CursorOffset, CursorConfig, CursorState)

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/GestureManager.kt`
  - **Role:** Touch gesture recognition

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/CursorAnimator.kt`
  - **Role:** Cursor animation system

#### **5. Helper/Integration**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/helper/VoiceCursorIMUIntegration.kt` (300 lines)
  - **Role:** DeviceManager IMU integration adapter
  - **Quality:** GOOD - Provides modern and legacy APIs
  - **Dependencies:** DeviceManager.IMUManager, DeviceManager.CursorAdapter

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/filter/CursorFilter.kt`
  - **Role:** Jitter filtering for smooth movement

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/calibration/ClickAccuracyManager.kt`
  - **Role:** Click calibration system

#### **6. UI/Settings**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/ui/VoiceCursorSettingsActivity.kt`
  - **Role:** Settings screen

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/ui/VoiceCursorViewModel.kt`
  - **Role:** Settings ViewModel

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/CursorMenuView.kt`
  - **Role:** Compose-based cursor menu
  - **Contains:** CursorAction enum definition

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/GazeClickView.kt`
  - **Role:** Gaze click animation

- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/help/VoiceCursorHelpMenu.kt`
  - **Role:** Help overlay

---

## Current Architecture

### Component Hierarchy

```
VoiceCursorAPI (Public Interface)
    └── CursorOverlayManager (Overlay Lifecycle)
            ├── CursorView (Rendering & Interaction)
            │   ├── CursorRenderer (Visual)
            │   ├── CursorPositionManager (Movement)
            │   ├── GazeClickManager (Gaze Detection)
            │   ├── CursorAnimator (Animations)
            │   ├── GestureManager (Touch Gestures)
            │   └── CursorFilter (Smoothing)
            ├── CursorGestureHandler (Accessibility Gestures)
            └── VoiceCursorIMUIntegration (Sensor Input)

CursorCommandHandler (Command Logic - PARALLEL TO API)
    ├── VoiceCursor (Legacy wrapper)
    └── VoiceAccessibilityIntegration (Voice commands)
```

### Data Flow

#### **1. Cursor Movement Flow (CLEAN)**
```
IMU Sensors
    → DeviceManager.IMUManager
        → VoiceCursorIMUIntegration
            → CursorAdapter (physics-based tracking)
                → CursorFilter (jitter elimination)
                    → CursorView.updateCursorPosition()
                        → CursorRenderer.draw()
```

#### **2. Cursor Action Flow (CLEAN)**
```
User Interaction (gaze/touch/voice)
    → CursorView callbacks (onGazeAutoClick, onMenuRequest)
        → CursorOverlayManager.executeAction()
            → CursorGestureHandler.executeAction()
                → AccessibilityService.dispatchGesture()
```

#### **3. Voice Command Flow (TANGLED)**
```
Voice Command (external)
    → CursorCommandHandler.handleVoiceCommand()
        ├── Command Parsing (moveCursor, performAction, etc.)
        ├── VoiceCursor API calls (showCursor, hideCursor)
        └── VoiceCursorAPI calls (executeAction, updateConfig)
```

### Permissions & Features

**AndroidManifest.xml:**
- `SYSTEM_ALERT_WINDOW` - Overlay permission
- `HIGH_SAMPLING_RATE_SENSORS` - Enhanced IMU tracking
- `VIBRATE` - Haptic feedback
- Hardware: `accelerometer`, `gyroscope` (optional)
- Exported Activity: `VoiceCursorSettingsActivity`

### Dependencies (build.gradle.kts)

**VOS4 Module Dependencies:**
- `DeviceManager` (IMU system) - **CRITICAL DEPENDENCY**
- `VoiceUIElements` (UI components)
- `SpeechRecognition` (voice input)
- `LicenseManager` (licensing)

**External Dependencies:**
- Jetpack Compose (UI)
- Coroutines (async)
- Material Design

---

## Command Logic Identified (EXTRACT TO CommandManager)

### Commands Defined in CursorCommandHandler

#### **Movement Commands**
```kotlin
// Lines 218-265
"cursor up [distance]"      → moveCursor(UP, distance)
"cursor down [distance]"    → moveCursor(DOWN, distance)
"cursor left [distance]"    → moveCursor(LEFT, distance)
"cursor right [distance]"   → moveCursor(RIGHT, distance)
```

**Implementation:** Lines 317-344
- Gets current position from VoiceCursor
- Calculates new position based on direction
- Updates cursor position via VoiceCursor.updatePosition()

**Refactoring:** Extract to `CursorActions.moveCursor(direction, distance)`

#### **Click Actions**
```kotlin
// Lines 226-236
"cursor click"              → performCursorAction(SINGLE_CLICK)
"cursor double click"       → performCursorAction(DOUBLE_CLICK)
"cursor long press"         → performCursorAction(LONG_PRESS)
"click" / "click here"      → SINGLE_CLICK (standalone)
"double click"              → DOUBLE_CLICK (standalone)
"long press"                → LONG_PRESS (standalone)
```

**Implementation:** Lines 349-363
- Gets current cursor position
- TODO: Needs to call VoiceCursorAPI.executeAction() (currently incomplete)

**Refactoring:** Extract to `CursorActions.click()`, `CursorActions.doubleClick()`, etc.

#### **System Commands**
```kotlin
// Lines 239-256
"cursor center"             → centerCursor()
"cursor show"               → showCursor()
"cursor hide"               → hideCursor()
"cursor show coordinates"   → showCoordinates()
"cursor hide coordinates"   → hideCoordinates()
"cursor coordinates"        → toggleCoordinates()
"cursor menu"               → showCursorMenu()
"cursor settings"           → openCursorSettings()
```

**Implementation:**
- Lines 368-378: centerCursor() → VoiceCursor.centerCursor()
- Lines 384-422: show/hideCursor() → VoiceCursorAPI.showCursor/hideCursor()
- Lines 427-443: showCursorMenu() → VoiceCursorAPI.showCursor() (menu integrated)
- Lines 517-537: openCursorSettings() → Intent to SettingsActivity
- Lines 558-622: Coordinates commands → VoiceCursorAPI.updateConfiguration()

**Refactoring:** Extract to `CursorActions.centerCursor()`, `CursorActions.showCursor()`, etc.

#### **Type Commands**
```kotlin
// Lines 258-261
"cursor hand"               → setCursorType(CursorType.Hand)
"cursor normal"             → setCursorType(CursorType.Normal)
"cursor custom"             → setCursorType(CursorType.Custom)
```

**Implementation:** Lines 449-463
- Updates VoiceCursor config with new type

**Refactoring:** Extract to `CursorActions.setCursorType(type)`

#### **Voice Cursor System Commands**
```kotlin
// Lines 270-283
"voice cursor enable"       → enableVoiceCursor()
"voice cursor disable"      → disableVoiceCursor()
"voice cursor calibrate"    → calibrateCursor()
"voice cursor settings"     → openCursorSettings()
"voice cursor help"         → showCursorHelp()
```

**Implementation:**
- Lines 468-482: enable → VoiceCursor.initialize() + startCursor()
- Lines 487-498: disable → VoiceCursor.stopCursor()
- Lines 504-513: calibrate → VoiceCursor.calibrate()

**Refactoring:** Extract to `CursorActions` or keep in CommandManager as system-level commands

### Command Registration & Routing

**Lines 108-135:** `registerCommands()`
- Registers handler with SimpleCommandRouter
- Uses MODULE_ID = "voicecursor"
- Handler: `handleVoiceCommand(command)`

**Lines 157-166:** `canHandleCommand()`
- Checks command prefixes: "cursor", "voice cursor"
- Checks standalone commands

**Lines 173-206:** `handleVoiceCommand()`
- Main entry point for command processing
- Routes to: processCursorCommand(), processVoiceCursorCommand(), processStandaloneCommand()

**Refactoring:** Command routing stays in CommandManager, execution logic moves to CursorActions

---

## Cursor Mechanics Identified (KEEP IN VOICECURSOR)

### Pure Cursor Operations (VoiceCursorAPI)

#### **Overlay Management**
- `initialize(context, accessibilityService)` - Initialize cursor system
- `showCursor(config)` - Display cursor overlay
- `hideCursor()` - Hide cursor overlay
- `toggleCursor()` - Toggle visibility

#### **Position & Movement**
- `centerCursor()` - Move cursor to screen center
- `getCurrentPosition()` - Get cursor coordinates
- Internal: Position tracking via IMU, filtering, edge detection

#### **Actions**
- `executeAction(action, position?)` - Execute gesture at position
- `click()` - Single click convenience
- `doubleClick()` - Double click convenience
- `longPress()` - Long press convenience
- `scrollUp()` / `scrollDown()` - Scroll gestures
- `startDrag()` / `endDrag()` - Drag gestures

#### **Configuration**
- `updateConfiguration(config)` - Update cursor settings
- Config includes: type, size, color, speed, gaze delay, coordinates display, filter settings

#### **State**
- `isVisible()` - Check visibility
- `isInitialized()` - Check initialization

### Internal Mechanics

#### **Rendering (CursorView + CursorRenderer)**
- Multiple cursor types: Hand, Normal, Custom
- ARVision glass morphism theming
- Animations: pulse, click feedback, hover glow, drag feedback
- Gaze click progress indicator
- Coordinate display overlay

#### **Position Management (CursorPositionManager)**
- Thread-safe position calculations
- Edge detection with bounce-back
- Movement smoothing via MovingAverage
- Cursor locking support

#### **Input Processing**
- **IMU:** VoiceCursorIMUIntegration → DeviceManager
- **Touch Gestures:** GestureManager (tap, double-tap, long-press, swipe, pinch)
- **Gaze:** GazeClickManager (dwell-time click)
- **Filtering:** CursorFilter (jitter elimination, adaptive smoothing)

#### **Gesture Dispatch (CursorGestureHandler)**
- Converts CursorAction → AccessibilityService gestures
- Path-based gesture creation (click, drag, scroll)
- Gesture callbacks for success/failure
- Drag duration calculation based on distance

---

## Dependencies Map

### External Module Dependencies

#### **DeviceManager (CRITICAL)**
- `IMUManager.getInstance(context)` - Sensor fusion system
- `CursorAdapter(context, consumerId)` - Physics-based cursor tracking
- **Impact:** VoiceCursor CANNOT function without DeviceManager
- **Refactoring Risk:** LOW - API is stable, well-encapsulated

#### **VoiceUIElements**
- UI components (not heavily used in examined code)
- **Refactoring Risk:** LOW

#### **SpeechRecognition**
- Voice input (used by voice integration)
- **Refactoring Risk:** LOW - Should move to CommandManager dependency

#### **LicenseManager**
- Licensing checks (not seen in examined code)
- **Refactoring Risk:** LOW

### Internal Dependencies

#### **AccessibilityService (INJECTED)**
- Required for gesture dispatch
- Currently injected via `VoiceCursorAPI.initialize(context, accessibilityService)`
- **Refactoring Impact:** NONE - Already properly injected

#### **Context**
- Required for WindowManager, SharedPreferences, Resources
- **Refactoring Impact:** NONE - Standard Android dependency

### Callbacks & Integration Points

#### **From CursorView to CursorOverlayManager**
```kotlin
onGazeAutoClick: (CursorOffset) -> Unit  // Gaze dwell click
onMenuRequest: (CursorOffset) -> Unit    // Long-press menu
onCursorMove: (CursorOffset) -> Unit     // Position updates
```

#### **From CursorOverlayManager to CursorGestureHandler**
```kotlin
executeAction(action: CursorAction, position: CursorOffset)
updateCursorPosition(position: CursorOffset)
```

#### **From IMU to CursorView**
```kotlin
setOnPositionUpdate: (CursorOffset) -> Unit
```

---

## Proposed Refactoring Plan

### Phase 1: Extract Command Logic to CommandManager (2 days)

#### **1.1: Create CursorActions in CommandManager**
**New File:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/voiceos/commands/actions/CursorActions.kt`

**Methods to Extract:**
```kotlin
object CursorActions {
    // Movement
    suspend fun moveCursor(direction: CursorDirection, distance: Float = 50f)

    // Clicks
    suspend fun click()
    suspend fun doubleClick()
    suspend fun longPress()

    // System
    suspend fun showCursor(config: CursorConfig = CursorConfig())
    suspend fun hideCursor()
    suspend fun centerCursor()
    suspend fun toggleCursor()

    // Configuration
    suspend fun showCoordinates()
    suspend fun hideCoordinates()
    suspend fun toggleCoordinates()
    suspend fun setCursorType(type: CursorType)

    // Advanced
    suspend fun showMenu()
    suspend fun openSettings()
    suspend fun calibrate()

    // Scrolling
    suspend fun scrollUp()
    suspend fun scrollDown()
}
```

**Implementation:**
- Each method calls VoiceCursorAPI directly
- No business logic - pure delegation to API
- Async/suspend for consistency with command processing

#### **1.2: Update CursorCommandHandler**
**Changes:**
- Lines 317-622: Replace inline implementations with CursorActions calls
- Example: `moveCursor() → CursorActions.moveCursor(direction, distance)`
- Keep command parsing and routing logic
- Remove direct VoiceCursor/VoiceCursorAPI calls

#### **1.3: Move CursorCommandHandler to CommandManager**
**New Location:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/voiceos/commands/handlers/CursorCommandHandler.kt`

**Rationale:**
- Command handlers belong in CommandManager
- CursorCommandHandler routes commands → executes CursorActions
- Keeps VoiceCursor module focused on cursor mechanics

#### **1.4: Update VoiceCursor Dependencies**
- Remove command handling code from VoiceCursor.kt
- VoiceCursor.kt becomes thin wrapper (or deprecate entirely)
- All external modules use VoiceCursorAPI directly

### Phase 2: Clean Up VoiceCursor Module (1 day)

#### **2.1: Deprecate VoiceCursor.kt**
- Already marked deprecated in favor of VoiceCursorAPI
- Add @Deprecated annotations with migration instructions
- Keep file for backward compatibility (6 months deprecation period)

#### **2.2: Remove Voice Integration from VoiceCursor**
- Delete `/cursor/integration/VoiceAccessibilityIntegration.kt` (placeholder stub)
- Voice integration belongs in CommandManager, not VoiceCursor
- CursorCommandHandler handles voice command routing

#### **2.3: Consolidate Helper Files**
- Review CursorHelper.kt - move utilities to appropriate core classes
- Keep VoiceCursorIMUIntegration.kt - it's cursor-specific

### Phase 3: Update CommandManager Architecture (2 days)

#### **3.1: Create Command Registry**
**New File:** `/CommandManager/src/main/java/com/augmentalis/voiceos/commands/CommandRegistry.kt`

```kotlin
object CommandRegistry {
    private val handlers = mutableMapOf<String, CommandHandler>()

    fun registerHandler(moduleId: String, handler: CommandHandler)
    fun unregisterHandler(moduleId: String)
    suspend fun routeCommand(command: String): Boolean
}
```

#### **3.2: Standardize CommandHandler Interface**
```kotlin
interface CommandHandler {
    val moduleId: String
    val supportedCommands: List<String>

    suspend fun handleCommand(command: String): Boolean
    fun canHandle(command: String): Boolean
}
```

#### **3.3: Integrate CursorCommandHandler**
- Make CursorCommandHandler implement CommandHandler interface
- Register with CommandRegistry during initialization
- Remove SimpleCommandRouter (replaced by CommandRegistry)

### Phase 4: Testing & Validation (1 day)

#### **4.1: Unit Tests**
- Test CursorActions delegation to VoiceCursorAPI
- Test command parsing in CursorCommandHandler
- Test command routing in CommandRegistry

#### **4.2: Integration Tests**
- Test voice command → CursorCommandHandler → CursorActions → VoiceCursorAPI flow
- Test cursor movement, clicks, configuration changes
- Test error handling and edge cases

#### **4.3: Manual Testing**
- Voice commands: "cursor up", "click", "show cursor", etc.
- IMU tracking and cursor movement
- Gaze click functionality
- Gesture dispatch (click, drag, scroll)

---

## Risks and Mitigation

### HIGH RISK

#### **1. DeviceManager Dependency**
**Risk:** VoiceCursor heavily depends on DeviceManager.IMUManager and CursorAdapter
**Impact:** Cannot run cursor without DeviceManager
**Mitigation:**
- DeviceManager API is stable and well-documented
- VoiceCursorIMUIntegration provides abstraction layer
- No changes needed to DeviceManager
- **Action:** Document dependency in architecture docs

#### **2. Command Routing Infrastructure**
**Risk:** No centralized command routing system currently exists
**Impact:** CursorCommandHandler uses local SimpleCommandRouter stub
**Mitigation:**
- Phase 3 creates proper CommandRegistry
- Incremental migration: CursorCommandHandler works standalone initially
- Other modules can be added later
- **Action:** Build CommandRegistry with extensibility in mind

### MEDIUM RISK

#### **3. Backward Compatibility**
**Risk:** Existing code may still use VoiceCursor.kt directly
**Impact:** Breaking changes if VoiceCursor.kt is removed
**Mitigation:**
- Keep VoiceCursor.kt as deprecated wrapper for 6 months
- Add clear migration documentation
- All new code uses VoiceCursorAPI
- **Action:** Create migration guide for developers

#### **4. Voice Integration Undefined**
**Risk:** VoiceAccessibilityIntegration is a placeholder stub
**Impact:** Voice commands may not actually work end-to-end
**Mitigation:**
- VoiceAccessibilityIntegration.kt is already a stub
- Real voice integration will be in CommandManager
- CursorCommandHandler ready for real voice input
- **Action:** Define voice integration contract with VoiceAccessibility team

### LOW RISK

#### **5. Testing Coverage**
**Risk:** Large codebase with many edge cases
**Impact:** Bugs may slip through refactoring
**Mitigation:**
- Existing code is well-structured and documented
- Clear separation points identified
- Incremental refactoring with testing at each phase
- **Action:** Write tests BEFORE refactoring each component

---

## Code Examples

### Current vs. Proposed

#### **Movement Command - CURRENT**
```kotlin
// CursorCommandHandler.kt lines 317-344
private suspend fun moveCursor(direction: CursorDirection, parameter: String?): Boolean {
    val distance = parameter?.toFloatOrNull() ?: 50f

    return withContext(Dispatchers.Main) {
        try {
            val currentPosition = voiceCursor?.getCurrentPosition() ?: CursorOffset(0f, 0f)

            val newPosition = when (direction) {
                CursorDirection.UP -> currentPosition.copy(y = currentPosition.y - distance)
                CursorDirection.DOWN -> currentPosition.copy(y = currentPosition.y + distance)
                CursorDirection.LEFT -> currentPosition.copy(x = currentPosition.x - distance)
                CursorDirection.RIGHT -> currentPosition.copy(x = currentPosition.x + distance)
            }

            voiceCursor?.updatePosition(newPosition)
            Log.d(TAG, "Moved cursor $direction by $distance pixels")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move cursor", e)
            false
        }
    }
}
```

#### **Movement Command - PROPOSED**
```kotlin
// CommandManager/CursorActions.kt
object CursorActions {
    suspend fun moveCursor(direction: CursorDirection, distance: Float = 50f): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val currentPosition = VoiceCursorAPI.getCurrentPosition() ?: return@withContext false

                val newPosition = when (direction) {
                    CursorDirection.UP -> currentPosition.copy(y = currentPosition.y - distance)
                    CursorDirection.DOWN -> currentPosition.copy(y = currentPosition.y + distance)
                    CursorDirection.LEFT -> currentPosition.copy(x = currentPosition.x - distance)
                    CursorDirection.RIGHT -> currentPosition.copy(x = currentPosition.x + distance)
                }

                // Move cursor by updating configuration with new position
                // (Note: VoiceCursorAPI doesn't have setPosition - needs enhancement)
                VoiceCursorAPI.executeAction(CursorAction.MOVE, newPosition)
                true
            } catch (e: Exception) {
                Log.e("CursorActions", "Failed to move cursor", e)
                false
            }
        }
    }
}

// CommandManager/CursorCommandHandler.kt
private suspend fun processCursorCommand(command: String): Boolean {
    val parts = command.split(" ", limit = 3)
    if (parts.size < 2) return false

    val action = parts[1]
    val parameter = if (parts.size > 2) parts[2] else null

    return when (action) {
        "up" -> CursorActions.moveCursor(CursorDirection.UP, parameter?.toFloatOrNull() ?: 50f)
        "down" -> CursorActions.moveCursor(CursorDirection.DOWN, parameter?.toFloatOrNull() ?: 50f)
        "left" -> CursorActions.moveCursor(CursorDirection.LEFT, parameter?.toFloatOrNull() ?: 50f)
        "right" -> CursorActions.moveCursor(CursorDirection.RIGHT, parameter?.toFloatOrNull() ?: 50f)
        // ... other commands
        else -> false
    }
}
```

#### **Click Command - CURRENT**
```kotlin
// CursorCommandHandler.kt lines 349-363
private suspend fun performCursorAction(action: CursorAction): Boolean {
    return withContext(Dispatchers.Main) {
        try {
            val currentPosition = voiceCursor?.getCurrentPosition()
            if (currentPosition == null) {
                return@withContext false
            }
            // TODO: Need to perform actions
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform cursor action: $action", e)
            false
        }
    }
}
```

#### **Click Command - PROPOSED**
```kotlin
// CommandManager/CursorActions.kt
object CursorActions {
    suspend fun click(): Boolean {
        return VoiceCursorAPI.click()
    }

    suspend fun doubleClick(): Boolean {
        return VoiceCursorAPI.doubleClick()
    }

    suspend fun longPress(): Boolean {
        return VoiceCursorAPI.longPress()
    }
}

// CommandManager/CursorCommandHandler.kt
private suspend fun processCursorCommand(command: String): Boolean {
    // ...
    return when (action) {
        "click" -> CursorActions.click()
        "double" -> if (parameter == "click") CursorActions.doubleClick() else false
        "long" -> if (parameter == "press" || parameter == "click") CursorActions.longPress() else false
        // ...
    }
}
```

### API Enhancement Needed

**Issue:** VoiceCursorAPI doesn't have a method to directly set cursor position for movement commands.

**Solution:** Add to VoiceCursorAPI:
```kotlin
/**
 * Move cursor to specified position with optional animation
 *
 * @param position The target position
 * @param animate Whether to animate the movement
 * @return true if move was successful, false otherwise
 */
fun moveTo(position: CursorOffset, animate: Boolean = true): Boolean {
    return overlayManager?.moveTo(position, animate) ?: false
}
```

---

## Architecture Decision Records (ADRs)

### ADR-001: Extract Command Logic to CommandManager
**Date:** 2025-10-10
**Status:** PROPOSED
**Decision:** Move all command handling logic from VoiceCursor to CommandManager
**Rationale:**
- Separation of concerns: VoiceCursor = cursor mechanics, CommandManager = command routing
- Enables other modules to register commands without depending on VoiceCursor
- Centralized command infrastructure for entire VOS4 system
- Reduces coupling between modules

**Consequences:**
- VoiceCursor becomes pure cursor mechanics library
- CommandManager gains CursorCommandHandler and CursorActions
- Need to create CommandRegistry infrastructure
- Voice integration moves from VoiceCursor to CommandManager

### ADR-002: Keep VoiceCursorAPI as Public Interface
**Date:** 2025-10-10
**Status:** ACCEPTED
**Decision:** VoiceCursorAPI remains the primary public interface for cursor operations
**Rationale:**
- Already well-designed with clean separation
- Provides all necessary cursor mechanics
- Injected dependencies (AccessibilityService) properly handled
- Clear, documented API

**Consequences:**
- All cursor operations go through VoiceCursorAPI
- CursorActions delegates to VoiceCursorAPI
- Legacy VoiceCursor.kt can be deprecated
- Consistent API surface for external modules

### ADR-003: Maintain DeviceManager Dependency
**Date:** 2025-10-10
**Status:** ACCEPTED
**Decision:** VoiceCursor continues to depend on DeviceManager for IMU tracking
**Rationale:**
- DeviceManager provides centralized, high-quality sensor fusion
- VoiceCursorIMUIntegration provides good abstraction
- No reason to duplicate IMU functionality
- DeviceManager API is stable

**Consequences:**
- VoiceCursor cannot run without DeviceManager
- Document dependency clearly in architecture
- DeviceManager must be initialized before VoiceCursor
- Benefits from DeviceManager improvements automatically

---

## API Surface Analysis

### VoiceCursorAPI - Public Methods

#### **Initialization**
```kotlin
fun initialize(context: Context, accessibilityService: AccessibilityService): Boolean
fun dispose()
fun isInitialized(): Boolean
```

#### **Visibility**
```kotlin
fun showCursor(config: CursorConfig = CursorConfig()): Boolean
fun hideCursor(): Boolean
fun toggleCursor(): Boolean
fun isVisible(): Boolean
```

#### **Position**
```kotlin
fun centerCursor(): Boolean
fun getCurrentPosition(): CursorOffset?
// NEEDED: fun moveTo(position: CursorOffset, animate: Boolean = true): Boolean
```

#### **Actions**
```kotlin
fun executeAction(action: CursorAction, position: CursorOffset? = null): Boolean
fun click(): Boolean
fun doubleClick(): Boolean
fun longPress(): Boolean
fun scrollUp(): Boolean
fun scrollDown(): Boolean
fun startDrag(): Boolean
fun endDrag(): Boolean
```

#### **Configuration**
```kotlin
fun updateConfiguration(config: CursorConfig): Boolean
```

### CursorAction Enum
```kotlin
enum class CursorAction {
    SINGLE_CLICK,
    DOUBLE_CLICK,
    LONG_PRESS,
    DRAG_START,
    DRAG_END,
    SCROLL_UP,
    SCROLL_DOWN,
    CENTER_CURSOR,
    HIDE_CURSOR,
    TOGGLE_COORDINATES,
    SHOW_HELP,
    SHOW_SETTINGS,
    CALIBRATE_CLICK
}
```

**Note:** Some actions (CENTER_CURSOR, HIDE_CURSOR, etc.) are handled by CursorView callbacks, not gesture dispatch

---

## Complexity Analysis

### Lines of Code by Component

| Component | Lines | Complexity | Refactoring Impact |
|-----------|-------|------------|-------------------|
| CursorCommandHandler | 739 | HIGH | MOVE to CommandManager |
| CursorView | 995 | MEDIUM | NO CHANGE |
| CursorOverlayManager | 392 | LOW | NO CHANGE |
| CursorPositionManager | 394 | MEDIUM | NO CHANGE |
| VoiceCursorIMUIntegration | 300 | LOW | NO CHANGE |
| CursorGestureHandler | 285 | LOW | NO CHANGE |
| VoiceCursor | 275 | LOW | DEPRECATE |
| VoiceCursorAPI | 237 | LOW | MINOR ENHANCEMENT |
| VoiceAccessibilityIntegration | 202 | LOW | DELETE (stub) |
| CursorTypes | 131 | LOW | NO CHANGE |

**Total Lines to Refactor:** ~1,216 (CursorCommandHandler + VoiceCursor + VoiceAccessibilityIntegration)
**Total Lines Unchanged:** ~3,333 (all cursor mechanics)

### Cyclomatic Complexity

**High Complexity Functions:**
- `CursorCommandHandler.processCursorCommand()` - 15+ branches
- `CursorCommandHandler.handleVoiceCommand()` - 10+ branches
- `CursorView.onDraw()` - 8+ branches
- `CursorView.handleGestureEvent()` - 12+ branches

**Refactoring Strategy:** Break down processCursorCommand() into smaller, focused functions

---

## Conclusion

### Summary

The VoiceCursor module is **well-architected for cursor mechanics** but has **tangled command handling logic** that needs extraction. The refactoring is **MODERATE complexity** with **clear separation points**.

### Strengths
1. VoiceCursorAPI provides excellent public interface
2. Cursor mechanics are cleanly separated in CursorView/CursorOverlayManager
3. Dependencies are well-managed (DeviceManager integration is clean)
4. Code is well-documented and follows VOS4 standards

### Weaknesses
1. Command handling mixed between VoiceCursor, CursorCommandHandler, VoiceAccessibilityIntegration
2. No centralized command routing infrastructure
3. VoiceCursor.kt is deprecated but still referenced
4. Voice integration is placeholder stub

### Recommendation

**PROCEED with refactoring in 4 phases over 3-5 days:**
1. Extract CursorActions to CommandManager (2 days)
2. Clean up VoiceCursor module (1 day)
3. Build CommandRegistry infrastructure (2 days)
4. Test and validate (1 day)

**Blockers:**
1. ✅ **RESOLVED:** DeviceManager dependency is stable and well-abstracted
2. ⚠️ **NEED:** CommandRegistry infrastructure in CommandManager (Phase 3)

**Next Steps:**
1. Create CursorActions.kt in CommandManager
2. Migrate command implementations from CursorCommandHandler
3. Update CursorCommandHandler to delegate to CursorActions
4. Move CursorCommandHandler to CommandManager
5. Build CommandRegistry for system-wide command routing

---

## Appendix: File Listing

### Complete Source File Tree
```
/modules/apps/VoiceCursor/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/augmentalis/voiceos/cursor/
│   │   ├── VoiceCursor.kt                           # DEPRECATE
│   │   ├── VoiceCursorAPI.kt                        # KEEP - PUBLIC API
│   │   ├── calibration/
│   │   │   └── ClickAccuracyManager.kt              # KEEP
│   │   ├── commands/
│   │   │   └── CursorCommandHandler.kt              # MOVE to CommandManager
│   │   ├── core/
│   │   │   ├── CursorAnimator.kt                    # KEEP
│   │   │   ├── CursorPositionManager.kt             # KEEP
│   │   │   ├── CursorRenderer.kt                    # KEEP
│   │   │   ├── CursorTypes.kt                       # KEEP
│   │   │   ├── GazeClickManager.kt                  # KEEP
│   │   │   ├── GestureManager.kt                    # KEEP
│   │   │   └── PositionManager.kt                   # KEEP (legacy support)
│   │   ├── filter/
│   │   │   └── CursorFilter.kt                      # KEEP
│   │   ├── help/
│   │   │   └── VoiceCursorHelpMenu.kt               # KEEP
│   │   ├── helper/
│   │   │   ├── CursorHelper.kt                      # REVIEW
│   │   │   └── VoiceCursorIMUIntegration.kt         # KEEP
│   │   ├── integration/
│   │   │   └── VoiceAccessibilityIntegration.kt     # DELETE (stub)
│   │   ├── manager/
│   │   │   ├── CursorGestureHandler.kt              # KEEP
│   │   │   └── CursorOverlayManager.kt              # KEEP
│   │   ├── ui/
│   │   │   ├── ThemeUtils.kt                        # KEEP
│   │   │   ├── VoiceCursorSettingsActivity.kt       # KEEP
│   │   │   └── VoiceCursorViewModel.kt              # KEEP
│   │   └── view/
│   │       ├── CursorMenuView.kt                    # KEEP
│   │       ├── CursorView.kt                        # KEEP
│   │       ├── EdgeVisualFeedback.kt                # KEEP
│   │       ├── FloatingHelpButton.kt                # KEEP
│   │       ├── GazeClickTestUtils.kt                # KEEP
│   │       └── GazeClickView.kt                     # KEEP
│   └── res/                                         # Resources - KEEP ALL
└── src/test/                                        # Tests - KEEP ALL
```

---

**Analysis Complete - Ready for Implementation**
