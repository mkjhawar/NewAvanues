# Cursor Axis Mapping Technical Reference

**Last Updated:** 2025-09-23 10:52:11 IST
**Component:** DeviceManager/CursorAdapter
**Version:** 1.0.0
**Status:** Active

## Overview

This document provides technical reference for the cursor movement axis mapping system in VOS4's CursorAdapter implementation. It defines the relationship between sensor data and cursor movement coordinates.

## Coordinate System Definitions

### Device Coordinate System (Sensor Input)

The IMU sensors provide orientation data in the following coordinate system:

```
Device Coordinate System (Right-handed):
- X-axis: Points to the right of the device
- Y-axis: Points up from the device
- Z-axis: Points out from the device screen (toward user)

Rotation Axes:
- Yaw (α):   Rotation around Z-axis (head turning left/right)
- Pitch (β): Rotation around X-axis (head nodding up/down)
- Roll (γ):  Rotation around Y-axis (head tilting left/right)
```

### Screen Coordinate System (Cursor Output)

The cursor movement follows standard screen coordinates:

```
Screen Coordinate System:
- X-axis: Horizontal, left to right (0 to screenWidth)
- Y-axis: Vertical, top to bottom (0 to screenHeight)
- Origin: Top-left corner of screen

Movement Directions:
- Positive X: Cursor moves right
- Negative X: Cursor moves left
- Positive Y: Cursor moves down
- Negative Y: Cursor moves up
```

## Axis Mapping Implementation

### Legacy Implementation Mapping

The legacy cursor implementation (`VoiceOsCursor.kt`) used the following mapping:

```kotlin
// Sensor data extraction (CursorOrientationProvider.kt:258-260)
val alpha = orientationBuffer[2]  // Roll
val beta = -orientationBuffer[1]  // Pitch (negated)
val gamma = orientationBuffer[0]  // Yaw

// Delta calculation
val dx = alpha - previousA   // Roll delta
val dy = beta - previousB    // Pitch delta
val dz = gamma - previousC   // Yaw delta

// Displacement calculation
val disX = Math.tan(dx) * width * scaleX + Math.tan(dz) * width * scaleZ * speed
val disY = Math.tan(dy) * height * scaleY * speed

// Position update
mX = startX + disX  // Add to X
mY = startY - disY  // Subtract from Y (invert direction)
```

### VOS4 CursorAdapter Mapping

The new implementation corrects the axis mapping for intuitive movement:

```kotlin
// Sensor data processing
val euler = relativeRotation.toEulerAngles()
val filteredYaw = maYaw.getAvg(euler.yaw, timestamp)     // α - Yaw
val filteredPitch = maPitch.getAvg(euler.pitch, timestamp) // β - Pitch
val filteredRoll = maRoll.getAvg(euler.roll, timestamp)   // γ - Roll

// Delta calculation (corrected mapping)
val dx = filteredYaw - previousYaw       // Yaw delta → controls Y movement
val dy = filteredPitch - previousPitch   // Pitch delta → controls X movement
val dz = filteredRoll - previousRoll     // Roll delta → enhances movement

// Displacement calculation (swapped for correct behavior)
val disX = tan(dy) * screenWidth * CURSOR_SCALE_Y * speedMultiplier
val disY = tan(dx) * screenHeight * CURSOR_SCALE_X +
           tan(dz) * screenHeight * CURSOR_SCALE_Z * speedMultiplier

// Position update
currentX = centerX + disX  // Add to X
currentY = centerY - disY  // Subtract from Y (maintain legacy direction)
```

## Movement Mapping Table

| Head Movement | Sensor Axis | Delta Variable | Displacement Calc | Screen Movement |
|---------------|-------------|----------------|-------------------|-----------------|
| Turn Left     | Yaw (-)     | dx (-)         | disY (-)          | Cursor Up       |
| Turn Right    | Yaw (+)     | dx (+)         | disY (+)          | Cursor Down     |
| Nod Up        | Pitch (-)   | dy (-)         | disX (-)          | Cursor Left     |
| Nod Down      | Pitch (+)   | dy (+)         | disX (+)          | Cursor Right    |
| Tilt Left     | Roll (-)    | dz (-)         | disY enhance (-)  | Slight Up       |
| Tilt Right    | Roll (+)    | dz (+)         | disY enhance (+)  | Slight Down     |

## Scale Factors and Sensitivity

### Scale Constants

```kotlin
companion object {
    private const val CURSOR_SCALE_X = 2.0f  // Horizontal movement sensitivity
    private const val CURSOR_SCALE_Y = 3.0f  // Vertical movement sensitivity
    private const val CURSOR_SCALE_Z = 2.0f  // Roll enhancement sensitivity
}
```

### Dynamic Sensitivity Calculation

```kotlin
val speedMultiplier = speedFactor * 0.2 * sensitivity

where:
- speedFactor: User-configurable speed (1-20, default 8)
- sensitivity: Application sensitivity (0.1-5.0, default 1.0)
- 0.2: Base multiplier for natural movement scaling
```

### Movement Scaling Formula

```kotlin
// Base displacement
displacement = tan(angle_delta) * screen_dimension * scale_factor * multiplier

// Fine tuning for small movements
finalDisplacement = if (abs(displacement) < screen_dimension/100) {
    displacement * 0.4  // Reduce sensitivity for precise control
} else {
    displacement        // Normal sensitivity for larger movements
}
```

## Coordinate Transformation Pipeline

### Step-by-Step Processing

```
1. IMU Sensor Data
   ↓
2. Quaternion → Euler Conversion
   ↓
3. Moving Average Filtering (4 samples, 300ms window)
   ↓
4. Delta Calculation (current - previous)
   ↓
5. Tolerance Check (RADIAN_TOLERANCE = 0.002f)
   ↓
6. Displacement Calculation (tan function)
   ↓
7. Scale Factor Application
   ↓
8. Fine Tuning for Small Movements
   ↓
9. Position Update with Bounds Checking
   ↓
10. Cursor Position Output
```

### Processing Constants

```kotlin
// Performance optimization
private const val MIN_INTERVAL_NS = 8_000_000L  // 8ms (~120Hz)
private const val RADIAN_TOLERANCE = 0.002f     // Minimum movement threshold

// Moving average configuration
private val maYaw = MovingAverage(4, 300000000L)    // 4 samples, 300ms
private val maPitch = MovingAverage(4, 300000000L)
private val maRoll = MovingAverage(4, 300000000L)
```

## Troubleshooting Axis Issues

### Common Problems and Solutions

1. **Inverted Movement:**
   ```kotlin
   // Problem: Cursor moves opposite to head movement
   // Solution: Check sign of displacement in position update
   currentY = centerY - finalDisY  // Note the minus sign
   ```

2. **Swapped Axes:**
   ```kotlin
   // Problem: Head left/right moves cursor up/down
   // Solution: Verify dx/dy usage in displacement calculation
   val disX = tan(dy) * ...  // Pitch controls X (horizontal)
   val disY = tan(dx) * ...  // Yaw controls Y (vertical)
   ```

3. **No Roll Enhancement:**
   ```kotlin
   // Problem: Movement feels unnatural
   // Solution: Add roll component to Y movement
   val disY = tan(dx) * ... + tan(dz) * ...
   ```

### Diagnostic Tools

```kotlin
// Debug logging for axis verification
private fun logMovementDebug(dx: Float, dy: Float, dz: Float, disX: Float, disY: Float) {
    VoiceOsLogger.d("CursorAdapter",
        "Movement: dx=$dx, dy=$dy, dz=$dz → disX=$disX, disY=$disY")
}
```

## Performance Considerations

### Optimization Techniques

1. **Throttling:** 8ms minimum interval between updates
2. **Pre-allocated Buffers:** Reuse arrays for calculations
3. **Early Returns:** Skip processing for minimal movements
4. **Efficient Math:** Use tan() function for natural angular response

### Memory Usage

- Moving average buffers: 3 × 4 samples = 12 float values
- Previous value storage: 3 float values
- Position tracking: 4 float values (current + center)
- Total: ~76 bytes per CursorAdapter instance

## Version History

### Version 1.0.0 (2025-09-23)
- Fixed axis mapping to match legacy behavior
- Corrected displacement calculation order
- Restored Y-axis direction inversion
- Added roll enhancement to Y movement

### Previous Issues (Pre-1.0.0)
- Incorrect axis assignments causing swapped movement
- Missing roll component in movement calculation
- Wrong Y-axis direction compared to legacy

## Related APIs

### Public Methods
```kotlin
fun updateScreenDimensions(width: Int, height: Int)
fun setSensitivity(newSensitivity: Float)
fun setSpeedFactor(newSpeedFactor: Int)
suspend fun centerCursor()
fun getCurrentPosition(): CursorPosition
```

### Data Classes
```kotlin
data class CursorPosition(val x: Float, val y: Float, val timestamp: Long)
```

## External Dependencies

- `IMUManager`: Provides orientation data stream
- `MovingAverage`: Smooths sensor data
- `Quaternion`: Handles orientation mathematics
- `OrientationData`: Container for sensor readings

---

**Maintainer:** DeviceManager Team
**Contact:** VOS4 Development Team
**Documentation Version:** 1.0.0