# VoiceCursor Developer Reference

**Last Updated**: 2025-10-23 21:45 PDT
**Module**: VoiceCursor
**Location**: `/modules/apps/VoiceCursor`
**Package**: `com.augmentalis.voiceos.cursor`

---

## Architecture Overview

VoiceCursor provides an accessible on-screen cursor controlled by head movement (IMU) and voice commands. The module follows VOS4 direct implementation pattern with no interfaces. Modern implementation uses **VoiceCursorAPI** as the public interface, with legacy **VoiceCursor** class maintained for backward compatibility. Command handling has been migrated to CommandManager module as of v2.0.0.

---

## Core Components

| Component | Purpose | Key Methods |
|-----------|---------|-------------|
| **VoiceCursorAPI** | Public API singleton for external modules | `initialize()`, `showCursor()`, `hideCursor()`, `moveTo()`, `executeAction()` |
| **VoiceCursor** (Legacy) | Original entry point, now deprecated | `startCursor()`, `stopCursor()`, `centerCursor()` (all deprecated) |
| **CursorOverlayManager** | Manages overlay lifecycle without service | `showCursor()`, `hideCursor()`, `updateConfiguration()`, `executeAction()` |
| **CursorView** | Custom View rendering cursor with ARVision theme | `updateCursorStyle()`, `centerCursor()`, `moveCursorTo()`, `startTracking()` |
| **CursorPositionManager** | Thread-safe position calculations | `updatePosition()`, `centerCursor()`, `updateScreenDimensions()` |
| **CursorRenderer** | Renders cursor graphics (Hand/Normal/Custom) | `drawCursor()`, `updateStyle()` |
| **GazeClickManager** | Gaze dwell-time auto-clicking | `startGazeTracking()`, `cancelGazeClick()` |
| **CursorFilter** | Jitter filtering and motion smoothing | `filterPosition()`, `updateConfig()` |
| **VoiceCursorIMUIntegration** | DeviceManager IMU integration for head tracking | `start()`, `stop()`, `setSensitivity()`, `calibrate()` |
| **CursorGestureHandler** | Accessibility gesture dispatch | `executeAction()`, `updateCursorPosition()` |
| **CursorCommandHandler** (Deprecated) | Voice command routing (moved to CommandManager) | `handleVoiceCommand()` (use CommandManager instead) |

---

## Top 10 Public APIs

### 1. Initialize System
```kotlin
VoiceCursorAPI.initialize(context: Context, accessibilityService: AccessibilityService): Boolean
```

### 2. Show Cursor
```kotlin
VoiceCursorAPI.showCursor(config: CursorConfig = CursorConfig()): Boolean
```

### 3. Hide Cursor
```kotlin
VoiceCursorAPI.hideCursor(): Boolean
```

### 4. Move Cursor
```kotlin
VoiceCursorAPI.moveTo(position: CursorOffset, animate: Boolean = true): Boolean
```

### 5. Execute Action
```kotlin
VoiceCursorAPI.executeAction(action: CursorAction, position: CursorOffset? = null): Boolean
```

### 6. Update Configuration
```kotlin
VoiceCursorAPI.updateConfiguration(config: CursorConfig): Boolean
```

### 7. Get Current Position
```kotlin
VoiceCursorAPI.getCurrentPosition(): CursorOffset?
```

### 8. Center Cursor
```kotlin
VoiceCursorAPI.centerCursor(): Boolean
```

### 9. Check Visibility
```kotlin
VoiceCursorAPI.isVisible(): Boolean
```

### 10. Cleanup
```kotlin
VoiceCursorAPI.dispose()
```

---

## Data Types

### CursorType (Sealed Class)
- `CursorType.Hand` - Hand-shaped cursor
- `CursorType.Normal` - Round crosshair cursor
- `CursorType.Custom` - Custom cursor style

### CursorOffset (Value Class)
```kotlin
@JvmInline
value class CursorOffset(x: Float, y: Float)
```
Zero-allocation position container.

### CursorConfig (Data Class)
```kotlin
data class CursorConfig(
    val type: CursorType = CursorType.Normal,
    val color: Int = 0xFF007AFF.toInt(),
    val size: Int = 48,
    val speed: Int = 8,
    val gazeClickDelay: Long = 1500L,
    val showCoordinates: Boolean = false,
    val jitterFilterEnabled: Boolean = true,
    val filterStrength: FilterStrength = FilterStrength.Medium,
    val motionSensitivity: Float = 0.7f
)
```

### CursorAction (Enum)
- `SINGLE_CLICK`, `DOUBLE_CLICK`, `LONG_PRESS`
- `SCROLL_UP`, `SCROLL_DOWN`
- `DRAG_START`, `DRAG_END`

### FilterStrength (Enum)
- `Low` (30%), `Medium` (60%), `High` (90%)

---

## Integration Points

- **CommandManager**: Voice command routing (replaces deprecated CursorCommandHandler)
- **DeviceManager**: IMU sensor integration via VoiceCursorIMUIntegration
- **AccessibilityService**: Gesture dispatch for clicks/actions
- **VoiceOSCore**: Lifecycle management and service coordination
- **SharedPreferences**: Configuration persistence (`voice_cursor_prefs`)

---

## Extension Guide

### 1. Add New Cursor Action
```kotlin
// 1. Add to CursorAction enum
enum class CursorAction { ..., NEW_ACTION }

// 2. Implement in CursorGestureHandler
fun executeAction(action: CursorAction, position: CursorOffset): Boolean {
    when (action) {
        CursorAction.NEW_ACTION -> performNewAction(position)
        ...
    }
}

// 3. Expose via VoiceCursorAPI
fun VoiceCursorAPI.newAction(): Boolean {
    return executeAction(CursorAction.NEW_ACTION)
}
```

### 2. Add Custom Cursor Type
```kotlin
// 1. Add to CursorType sealed class
sealed class CursorType {
    object NewType : CursorType()
}

// 2. Update CursorRenderer.drawCursor()
when (config.type) {
    CursorType.NewType -> drawNewTypeCursor(canvas, paint)
    ...
}
```

### 3. Integrate New Input Source
```kotlin
// 1. Create integration class
class NewInputIntegration(context: Context) {
    fun setOnPositionUpdate(callback: (CursorOffset) -> Unit)
    fun start()
    fun stop()
}

// 2. Add to CursorOverlayManager.initializeIMU()
newInputIntegration = NewInputIntegration(context).apply {
    setOnPositionUpdate { position ->
        cursorView?.moveCursorTo(position)
    }
    start()
}
```

### 4. Add Configuration Option
```kotlin
// 1. Update CursorConfig data class
data class CursorConfig(
    ...,
    val newOption: Boolean = false
)

// 2. Update SharedPreferences persistence
prefs.getBoolean("new_option", false)

// 3. Apply in CursorView.updateCursorStyle()
```

### 5. Register Voice Command (via CommandManager)
```kotlin
// Commands now handled by CommandManager module
// See: /modules/managers/CommandManager/handlers/CursorCommandHandler.kt
```

---

## Migration Notes

### Legacy → Modern API
```kotlin
// OLD (Deprecated)
val voiceCursor = VoiceCursor.getInstance(context)
voiceCursor.initialize()
voiceCursor.startCursor()

// NEW (Recommended)
VoiceCursorAPI.initialize(context, accessibilityService)
VoiceCursorAPI.showCursor()
```

### Command Handler Migration
```kotlin
// OLD (Deprecated - in VoiceCursor module)
CursorCommandHandler.getInstance(context)
    .handleVoiceCommand("cursor click")

// NEW (Moved to CommandManager)
// Use CommandManager.handlers.CursorCommandHandler
// Located: /modules/managers/CommandManager/
```

---

## Performance Notes

- **Zero-allocation**: CursorOffset uses value class (no heap allocation)
- **Thread-safe**: CursorPositionManager uses @Volatile and synchronization
- **Filtering**: Jitter filtering reduces noise in IMU data
- **Coroutines**: Non-blocking async operations with structured concurrency
- **Lifecycle**: Proper cleanup via dispose() prevents memory leaks

---

## Quick Links
- [User Reference](./user-reference.md)
- [CommandManager](../CommandManager/README.md)
- [DeviceManager](../DeviceManager/README.md)
- [VOS4 Coding Standards](/Volumes/M Drive/Coding/vos4/docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md)

---

**Version**: 1.0.0
**Created**: 2025-10-23 21:45 PDT
**Author**: VOS4 Documentation Specialist
