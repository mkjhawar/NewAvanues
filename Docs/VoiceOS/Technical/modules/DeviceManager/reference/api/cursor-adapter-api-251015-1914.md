# CursorAdapter API Reference
**Module:** DeviceManager  
**Package:** `com.augmentalis.devicemanager.sensors.imu`  
**Version:** 1.2.0  
**Last Updated:** 2025-01-28

## Overview
`CursorAdapter` transforms IMU orientation data into screen coordinates for cursor positioning using delta-based processing and tangent scaling.

## Class Definition
```kotlin
class CursorAdapter(
    private val screenWidth: Int = 1920,
    private val screenHeight: Int = 1080
)
```

## Public Methods

### Core Functionality

#### `suspend fun processOrientationForCursor(orientationData: OrientationData)`
Processes orientation data to update cursor position using delta-based calculation.

**Parameters:**
- `orientationData`: Current device orientation as quaternion

**Behavior:**
- Calculates delta from previous orientation
- Converts to Euler angles
- Applies tangent-based scaling
- Updates cursor position cumulatively
- Implements dead zone filtering

**Example:**
```kotlin
val adapter = CursorAdapter(1920, 1080)
adapter.processOrientationForCursor(orientationData)
val position = adapter.getCursorPosition()
```

#### `fun getCursorPosition(): CursorPosition`
Returns the current cursor position.

**Returns:** `CursorPosition` with x,y coordinates

**Example:**
```kotlin
val position = adapter.getCursorPosition()
println("Cursor at: ${position.x}, ${position.y}")
```

### Configuration

#### `fun updateScreenDimensions(width: Int, height: Int)`
Updates screen dimensions and re-centers cursor.

**Parameters:**
- `width`: Screen width in pixels
- `height`: Screen height in pixels

**Behavior:**
- Validates dimensions (must be > 0)
- Re-centers cursor at new center point
- Logs dimension change

**Example:**
```kotlin
adapter.updateScreenDimensions(2560, 1440)
```

#### `fun setSensitivity(x: Float, y: Float)`
Sets movement sensitivity for each axis.

**Parameters:**
- `x`: Horizontal sensitivity (0.5-5.0)
- `y`: Vertical sensitivity (0.5-5.0)

**Default Values:**
- X: 2.0
- Y: 3.0

**Example:**
```kotlin
adapter.setSensitivity(2.5f, 3.5f)
```

### Calibration

#### `fun centerCursor()`
Centers the cursor on screen.

**Behavior:**
- Sets position to (screenWidth/2, screenHeight/2)
- Resets movement tracking
- Logs centering action

**Example:**
```kotlin
adapter.centerCursor()
```

#### `fun forceRecalibration()`
Forces complete recalibration of the cursor system.

**Behavior:**
- Centers cursor
- Resets base orientation
- Clears previous orientation
- Resets movement timer

**Example:**
```kotlin
if (cursorStuck) {
    adapter.forceRecalibration()
}
```

#### `fun resetOrientation()`
Resets the base orientation to current device orientation.

**Behavior:**
- Next orientation becomes new reference
- Useful for drift correction

**Example:**
```kotlin
// User holds device in neutral position
adapter.resetOrientation()
```

### Debugging

#### `fun getCursorState(): String`
Returns detailed state information for debugging.

**Returns:** String containing:
- Current position
- Screen dimensions
- Sensitivity values
- Initialization state
- Base orientation
- Last movement time

**Example:**
```kotlin
Log.d("Debug", adapter.getCursorState())
// Output: "CursorState: pos=(960.0,540.0), screen=1920x1080, ..."
```

#### `fun enableDebugLogging(enable: Boolean)`
Enables or disables verbose debug logging.

**Parameters:**
- `enable`: true to enable debug logs

**Example:**
```kotlin
adapter.enableDebugLogging(true)
// Now logs detailed calculation steps
```

## Properties

### Read-Only Properties

#### `val isInitialized: Boolean`
Indicates if adapter is properly initialized with valid base orientation.

#### `val lastMovementTime: Long`
Timestamp of last detected cursor movement (milliseconds).

#### `val currentX: Float`
Current X coordinate (read-only).

#### `val currentY: Float`
Current Y coordinate (read-only).

### Configuration Properties

#### `var sensitivityX: Float`
Horizontal movement sensitivity (default: 2.0).

#### `var sensitivityY: Float`
Vertical movement sensitivity (default: 3.0).

#### `var deadZoneThreshold: Float`
Minimum angle change to register movement (default: 0.001 radians).

#### `var stuckDetectionThreshold: Long`
Time without movement before auto-recalibration (default: 5000ms).

## Data Classes

### CursorPosition
```kotlin
data class CursorPosition(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)
```

### OrientationData
```kotlin
data class OrientationData(
    val quaternion: Quaternion,
    val timestamp: Long,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
)
```

## Mathematical Model

### Delta-Based Processing
```kotlin
// Core algorithm (simplified)
deltaRotation = previousOrientation⁻¹ × currentOrientation
deltaEuler = toEulerAngles(deltaRotation)

// Tangent-based displacement
deltaX = tan(deltaEuler.yaw) × screenWidth × sensitivityX
deltaY = -tan(deltaEuler.pitch) × screenHeight × sensitivityY

// Cumulative positioning
position = clamp(position + delta, screenBounds)
```

### Coordinate System
- Origin: Top-left corner (0,0)
- X-axis: Horizontal, increases rightward
- Y-axis: Vertical, increases downward
- Bounds: [0, screenWidth] × [0, screenHeight]

## Thread Safety
- All methods are suspend functions or thread-safe
- Uses atomic operations for position updates
- Safe to call from any coroutine context

## Performance Characteristics
- Processing time: < 2ms per frame
- Memory usage: ~2KB state
- Update rate: Up to 120Hz
- Latency: < 16ms end-to-end

## Error Handling

### Invalid Orientation Data
- Skips frame if quaternion is invalid
- Maintains previous position
- Logs warning

### Screen Dimension Validation
- Rejects dimensions ≤ 0
- Maintains previous valid dimensions
- Logs error

### Stuck Cursor Recovery
- Auto-detects after 5 seconds
- Triggers automatic recalibration
- Centers cursor and resets base

## Usage Example

```kotlin
class CursorController {
    private val cursorAdapter = CursorAdapter()
    private val imuManager = IMUManager(context)
    
    init {
        // Set up IMU data flow
        imuManager.orientationFlow.collect { orientationData ->
            cursorAdapter.processOrientationForCursor(orientationData)
            updateUI(cursorAdapter.getCursorPosition())
        }
    }
    
    fun handleScreenRotation(width: Int, height: Int) {
        cursorAdapter.updateScreenDimensions(width, height)
    }
    
    fun recalibrate() {
        cursorAdapter.forceRecalibration()
    }
}
```

## Migration Notes

### From Legacy (Before v1.2.0)
If using CursorAdapter before the fix:
1. Remove any 0.1x multipliers in position updates
2. Ensure initialization calls `centerCursor()`
3. Update to use delta-based processing
4. Adjust sensitivity values (old: ~10, new: 2-3)

## Best Practices
1. Always initialize screen dimensions before processing
2. Call `centerCursor()` on first launch
3. Implement stuck detection in production
4. Use debug logging during development
5. Test with various screen sizes
6. Validate quaternion data before processing

## See Also
- [IMUManager API](./IMUManager-API.md)
- [IMUMathUtils API](./IMUMathUtils-API.md)
- [VoiceCursor Integration Guide](../../voicecursor/developer-manual/Developer-Guide.md)

---
**API Version:** 1.2.0  
**Status:** Stable  
**Last Review:** 2025-01-28