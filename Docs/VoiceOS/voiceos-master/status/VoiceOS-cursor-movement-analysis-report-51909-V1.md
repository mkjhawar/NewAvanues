# Cursor Movement Analysis Report

**Document ID:** CURSOR-ANALYSIS-20250919
**Created:** 2025-09-19 14:30:58 IST
**Author:** VOS4 Development Team
**Version:** 1.0.0
**Status:** Analysis Complete

## Executive Summary

This report analyzes the cursor movement issues in the VOS4 system and compares the new implementation with the legacy system to identify root causes and propose solutions.

## Problem Statement

### Current Issue
The cursor in the VOS4 system exhibits erratic behavior:
- Rapid oscillation between screen corners (0,0) and (1920,1080)
- Inability to achieve smooth intermediate positioning
- Unresponsive to gradual device orientation changes

### Log Evidence
```
12:29:04.905 VoiceCursorIMU    D  Position update: x=0, y=0
12:29:04.905 VoiceCursorIMU    D  Position update: x=1920, y=1080
12:29:04.910 VoiceCursorIMU    D  Position update: x=1920, y=1080
12:29:04.910 VoiceCursorIMU    D  Position update: x=0, y=0
```

## Technical Analysis

### Architecture Comparison

#### New System (VOS4 - Broken)
**File:** `CursorAdapter.kt`
```kotlin
// Location: modules/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapter.kt

private suspend fun processOrientationForCursor(orientationData: OrientationData) {
    val relativeRotation = baseOrientation.inverse * orientationData.quaternion
    val euler = relativeRotation.toEulerAngles()

    val deltaX = euler.yaw * sensitivity * 50f
    val deltaY = -euler.pitch * sensitivity * 50f

    currentX = (currentX + (deltaX * 0.1f)).coerceIn(0f, screenWidth.toFloat())
    currentY = (currentY + (deltaY * 0.1f)).coerceIn(0f, screenHeight.toFloat())

    emitPosition()
}
```

#### Legacy System (Working)
**File:** `VoiceOsCursor.kt`
```kotlin
// Location: /home/naveen/Naveen/Projects/Augmentalis/VoiceOs/VoiceOs-Avanue/voiceos-app-latest/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/VoiceOsCursor.kt

fun setOrientation(mAlpha: Float, mBeta: Float, mGamma: Float, ts: Long, isLock: Boolean = false): Int {
    // Apply moving average filtering
    val alpha = maA.getAvg(mAlpha, ts)
    val beta = maB.getAvg(mBeta, ts)
    val gamma = if (mGamma < 0) -maC.getAvg(-mGamma, ts) else maC.getAvg(mGamma, ts)

    // Calculate deltas
    val dx = alpha - previousA
    val dy = beta - previousB
    val dz = gamma - previousC

    // Early return for tiny movements
    if (abs(dx) < RADIAN_TOLERANCE && abs(dy) < RADIAN_TOLERANCE && abs(dz) < RADIAN_TOLERANCE) {
        return 0
    }

    // Calculate displacement using trigonometric functions
    val speedMultiplier = speedFactor * 0.2
    val disX = Math.tan(dx.toDouble()) * mWidth * cursorScaleX +
               Math.tan(dz.toDouble()) * mWidth * cursorScaleZ * speedMultiplier
    val disY = Math.tan(dy.toDouble()) * mHeight * cursorScaleY * speedMultiplier

    // Position relative to center point
    mX = (startX + finalDisX.toFloat()).coerceIn(0f, mWidth.toFloat())
    mY = (startY - finalDisY.toFloat()).coerceIn(0f, mHeight.toFloat())

    // Reset center for next calculation
    startX = mX
    startY = mY

    return distance.toInt()
}
```

### Critical Components Analysis

#### 1. Sensor Data Filtering

**Legacy (Working):**
```kotlin
private val maA = MovingAverage(4, 300000000L)  // 4 samples, 300ms window
private val maB = MovingAverage(4, 300000000L)
private val maC = MovingAverage(4, 300000000L)

val alpha = maA.getAvg(mAlpha, ts)
val beta = maB.getAvg(mBeta, ts)
val gamma = if (mGamma < 0) -maC.getAvg(-mGamma, ts) else maC.getAvg(mGamma, ts)
```

**New (Broken):**
- No filtering applied
- Raw quaternion data processed directly
- Results in noisy, erratic movement

#### 2. Movement Calculation Algorithm

**Legacy (Working):**
```kotlin
// Delta-based movement from previous position
val dx = alpha - previousA
val dy = beta - previousB
val dz = gamma - previousC

// Trigonometric displacement calculation
val disX = Math.tan(dx.toDouble()) * mWidth * cursorScaleX
val disY = Math.tan(dy.toDouble()) * mHeight * cursorScaleY

// Relative positioning from center
mX = (startX + finalDisX.toFloat()).coerceIn(0f, mWidth.toFloat())
mY = (startY - finalDisY.toFloat()).coerceIn(0f, mHeight.toFloat())
```

**New (Broken):**
```kotlin
// Direct multiplication without proper scaling
val deltaX = euler.yaw * sensitivity * 50f
val deltaY = -euler.pitch * sensitivity * 50f

// Cumulative positioning (causes drift)
currentX = (currentX + (deltaX * 0.1f)).coerceIn(0f, screenWidth.toFloat())
currentY = (currentY + (deltaY * 0.1f)).coerceIn(0f, screenHeight.toFloat())
```

#### 3. Tolerance and Optimization

**Legacy (Working):**
```kotlin
private const val RADIAN_TOLERANCE = 0.002f
private const val MIN_INTERVAL_NS = 8_000_000L // 8ms (~120Hz)

// Skip processing for tiny movements
if (abs(dx) < RADIAN_TOLERANCE &&
    abs(dy) < RADIAN_TOLERANCE &&
    abs(dz) < RADIAN_TOLERANCE) {
    return 0
}
```

**New (Broken):**
- No tolerance checks
- Processes every sensor update
- No throttling mechanism

### Supporting Components Analysis

#### MovingAverage Implementation
**File:** `MovingAverage.kt`

Key features:
- Circular buffer with configurable sample size and time window
- SMMA (Smoothed Moving Average) algorithm
- Automatic purging of expired samples
- Time-based and count-based filtering

```kotlin
class MovingAverage(
    private val maxSamples: Int,  // Maximum number of samples to retain
    private val maxTimeWindowNs: Long    // Maximum time (in nanoseconds)
) {
    fun getAvg(data: Float, ts: Long): Float {
        add(data, ts)
        return if (size == 0) 0f else runningSum / size
    }

    fun getSMMA(): Float {
        if (size == 0) return 0f
        var result = values[head]
        for (i in 1 until size) {
            val idx = (head + i) % maxSamples
            result += (values[idx] - result) / (i + 1)
        }
        return result
    }
}
```

#### CursorOrientationProvider
**File:** `CursorOrientationProvider.kt`

Key features:
- Multi-device sensor support (Phone, EPSON, Rokid)
- Rotation matrix calculations with coordinate system remapping
- Optimized sensor data processing with pre-allocated buffers
- Timestamp-based throttling

```kotlin
private fun calculateSensorData(values: FloatArray?, timestamp: Long) {
    // Throttle processing based on timestamp
    val currentTs = System.nanoTime()
    if (currentTs - lastTs < MIN_SENSOR_INTERVAL_NS) {
        return // Skip this update
    }

    // Coordinate system transformation
    SensorManager.getRotationMatrixFromVector(rotationMatrixBuffer, values)
    SensorManager.remapCoordinateSystem(
        rotationMatrixBuffer,
        worldAxisX,
        worldAxisY,
        adjustedRotationMatrixBuffer
    )
    SensorManager.getOrientation(adjustedRotationMatrixBuffer, orientationBuffer)

    val alpha = orientationBuffer[2]
    val beta = -orientationBuffer[1]
    val gamma = orientationBuffer[0]
}
```

## Root Cause Analysis

### Primary Issues

1. **Missing Signal Filtering**
   - **Impact:** Raw sensor noise causes erratic cursor movement
   - **Solution:** Implement MovingAverage filtering

2. **Incorrect Movement Algorithm**
   - **Impact:** Linear multiplication creates unnatural movement
   - **Solution:** Use trigonometric displacement calculation

3. **Cumulative Position Drift**
   - **Impact:** Positions accumulate errors over time
   - **Solution:** Implement delta-based relative positioning

4. **Absence of Tolerance Checks**
   - **Impact:** Processes micro-movements causing jitter
   - **Solution:** Add radian tolerance thresholds

### Secondary Issues

5. **Quaternion Complexity**
   - **Impact:** Additional conversion overhead and potential errors
   - **Assessment:** Can be maintained if properly filtered

6. **Missing Performance Optimizations**
   - **Impact:** Unnecessary processing overhead
   - **Solution:** Add timestamp-based throttling

## Recommended Solution Plan

### Phase 1: Core Algorithm Fix (High Priority)

1. **Implement MovingAverage Filtering**
   ```kotlin
   // Add to CursorAdapter.kt
   private val maYaw = MovingAverage(4, 300000000L)
   private val maPitch = MovingAverage(4, 300000000L)
   private val maRoll = MovingAverage(4, 300000000L)
   ```

2. **Replace Movement Algorithm**
   ```kotlin
   // Delta-based calculation
   val dx = filteredYaw - previousYaw
   val dy = filteredPitch - previousPitch

   // Trigonometric displacement
   val disX = Math.tan(dx.toDouble()) * screenWidth * sensitivity
   val disY = Math.tan(dy.toDouble()) * screenHeight * sensitivity

   // Relative positioning
   currentX = (centerX + disX.toFloat()).coerceIn(0f, screenWidth.toFloat())
   currentY = (centerY - disY.toFloat()).coerceIn(0f, screenHeight.toFloat())
   ```

3. **Add Tolerance Checks**
   ```kotlin
   private const val RADIAN_TOLERANCE = 0.002f

   if (abs(dx) < RADIAN_TOLERANCE && abs(dy) < RADIAN_TOLERANCE) {
       return // Skip tiny movements
   }
   ```

### Phase 2: Integration Improvements (Medium Priority)

1. **Maintain DeviceManager Architecture**
   - Keep centralized IMU management
   - Preserve Flow-based reactive updates
   - Maintain multi-consumer support

2. **Add Performance Optimizations**
   - Implement frame rate limiting
   - Add timestamp-based throttling
   - Optimize memory allocations

### Phase 3: Advanced Features (Low Priority)

1. **Enhanced Calibration**
   - User-specific calibration support
   - Adaptive sensitivity adjustment
   - Environmental compensation

2. **Improved Error Handling**
   - Graceful sensor failure recovery
   - Better error logging and diagnostics
   - Fallback mechanisms

## Implementation Priority Matrix

| Component | Priority | Effort | Impact | Dependencies |
|-----------|----------|---------|---------|--------------|
| MovingAverage Filtering | Critical | Low | High | None |
| Delta-based Movement | Critical | Medium | High | Filtering |
| Tolerance Checks | High | Low | Medium | Movement Algorithm |
| Trigonometric Displacement | High | Medium | High | Delta Calculation |
| Performance Throttling | Medium | Low | Medium | None |
| Enhanced Calibration | Low | High | Medium | Core Algorithm |

## Testing Strategy

### Unit Tests
- MovingAverage algorithm validation
- Delta calculation accuracy
- Tolerance threshold verification
- Trigonometric displacement correctness

### Integration Tests
- End-to-end cursor movement validation
- Multi-device sensor compatibility
- Performance benchmarking
- Memory usage verification

### User Acceptance Tests
- Smooth cursor movement validation
- Responsiveness testing
- Accuracy assessment
- Edge case handling

## Risk Assessment

### Technical Risks
- **Risk:** Algorithm complexity increase
- **Mitigation:** Incremental implementation with fallback options

- **Risk:** Performance degradation
- **Mitigation:** Performance profiling and optimization

### Integration Risks
- **Risk:** Breaking existing functionality
- **Mitigation:** Comprehensive testing and gradual rollout

- **Risk:** Device compatibility issues
- **Mitigation:** Multi-device testing matrix

## Conclusion

The cursor movement issues in VOS4 stem from fundamental algorithmic differences between the new and legacy implementations. The legacy system's success relies on:

1. **Sophisticated signal filtering** through MovingAverage
2. **Natural movement calculation** using trigonometric functions
3. **Delta-based positioning** to prevent drift
4. **Performance optimizations** through tolerance checks

By implementing these proven components within the modern DeviceManager architecture, we can achieve reliable cursor movement while maintaining the benefits of the new system design.

## Appendix

### File References
- **New Implementation:** `/modules/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/sensors/imu/CursorAdapter.kt`
- **Legacy Implementation:** `/home/naveen/Naveen/Projects/Augmentalis/VoiceOs/VoiceOs-Avanue/voiceos-app-latest/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/VoiceOsCursor.kt`
- **MovingAverage:** `/home/naveen/Naveen/Projects/Augmentalis/VoiceOs/VoiceOs-Avanue/voiceos-app-latest/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/MovingAverage.kt`
- **Orientation Provider:** `/home/naveen/Naveen/Projects/Augmentalis/VoiceOs/VoiceOs-Avanue/voiceos-app-latest/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/CursorOrientationProvider.kt`

### Log Analysis Location
- **Log File:** `/home/naveen/Desktop/logs.txt`
- **Issue Pattern:** Rapid oscillation between (0,0) and (1920,1080)
- **Frequency:** Multiple updates per millisecond

---

**Document Control:**
- **Last Updated:** 2025-09-19 14:30:58 IST
- **Review Status:** Draft - Pending Technical Review
- **Distribution:** VOS4 Development Team, Technical Leads
- **Classification:** Internal Technical Document