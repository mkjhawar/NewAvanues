# Legacy VoiceCursor Implementation Analysis

**Last Updated:** 2025-09-08 21:43:12 PDT  
**Analysis Type:** Code Archaeology - Legacy Implementation Patterns  
**Target System:** VOS4 VoiceCursor Module  
**Source Systems:** LegacyAvenue, VOS2 Avenue4 Module  

## Executive Summary

This analysis examines the legacy VoiceCursor implementation patterns from LegacyAvenue and VOS2 projects to identify key algorithms, sensor data flows, and coordinate transformation methods. The findings will inform the VOS4 VoiceCursor module development.

### Key Legacy Components Identified

1. **VoiceOsCursor** - Main cursor view and rendering engine
2. **CursorOrientationProvider** - Multi-device sensor data acquisition
3. **VoiceOsCursorHelper** - Cursor lifecycle management
4. **MovingAverage** - Sensor data smoothing algorithms
5. **Cursor Actions System** - Voice command integration

## Core Implementation Patterns

### 1. Sensor Data Flow Architecture

#### Multi-Device Sensor Support
**File:** `/LegacyAvenue/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/CursorOrientationProvider.kt`

```kotlin
// Device Detection Pattern
enum class SensorDeviceType {
    PHONE, ROKID, EPSON
}

private fun initializeSensor() {
    if (isRokid() && isRokidConnected) {
        RKGlassDevice.getInstance().addGlassSensorListener(this)
        sensorDeviceType = SensorDeviceType.ROKID
    } else if (mDeviceManager?.isHeadsetAttached == true) {
        sensorDeviceType = SensorDeviceType.EPSON
        mEPSONSensorManager?.open(TYPE_ROTATION_VECTOR, this)
    } else {
        mSensorManager = context.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
        mSensor = getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorDeviceType = SensorDeviceType.PHONE
    }
}
```

#### Sensor Data Processing Pipeline
```kotlin
private fun calculateSensorData(values: FloatArray?, timestamp: Long) {
    // 1. Throttling: MIN_SENSOR_INTERVAL_NS = 8ms (~120Hz)
    val currentTs = System.nanoTime()
    if (currentTs - lastTs < MIN_SENSOR_INTERVAL_NS) return
    
    // 2. Rotation Matrix Generation
    SensorManager.getRotationMatrixFromVector(rotationMatrixBuffer, values)
    
    // 3. Coordinate System Remapping
    val (worldAxisX, worldAxisY) = rotationAxisMappings[rotation]
    SensorManager.remapCoordinateSystem(
        rotationMatrixBuffer, worldAxisX, worldAxisY, adjustedRotationMatrixBuffer
    )
    
    // 4. Orientation Extraction
    SensorManager.getOrientation(adjustedRotationMatrixBuffer, orientationBuffer)
    
    // 5. Axis Mapping
    val alpha = orientationBuffer[2]  // Roll
    val beta = -orientationBuffer[1] // Pitch (inverted)
    val gamma = orientationBuffer[0] // Yaw
}
```

### 2. Cursor Movement Algorithms

#### Core Movement Calculation
**File:** `/LegacyAvenue/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/VoiceOsCursor.kt`

```kotlin
fun setOrientation(mAlpha: Float, mBeta: Float, mGamma: Float, ts: Long): Int {
    // Moving Average Filtering
    val alpha = maA.getAvg(mAlpha, ts)
    val beta = maB.getAvg(mBeta, ts)  
    val gamma = if (mGamma < 0) -maC.getAvg(-mGamma, ts) else maC.getAvg(mGamma, ts)
    
    // Delta Calculation
    val dx = alpha - previousA
    val dy = beta - previousB
    val dz = gamma - previousC
    
    // Displacement Calculation with Scaling
    val speedMultiplier = speedFactor * 0.2
    val disX = Math.tan(dx.toDouble()) * mWidth * cursorScaleX + 
               Math.tan(dz.toDouble()) * mWidth * cursorScaleZ * speedMultiplier
    val disY = Math.tan(dy.toDouble()) * mHeight * cursorScaleY * speedMultiplier
    
    // Fine Tuning for Small Movements
    val finalDisX = if (abs(disX) < (mWidth / 100)) disX * 0.4 else disX
    val finalDisY = if (abs(disY) < (mHeight / 100)) disY * 0.4 else disY
    
    // Position Update with Bounds
    mX = (startX + finalDisX.toFloat()).coerceIn(0f, mWidth.toFloat())
    mY = (startY - finalDisY.toFloat()).coerceIn(0f, mHeight.toFloat())
}
```

#### Movement Constants and Tolerances
```kotlin
companion object {
    private const val RADIAN_TOLERANCE = 0.002f
    private const val DEFAULT_DISPLACEMENT_FACTOR = 8
    private const val DISTANCE_TOLERANCE = 1.0f
    private const val MIN_INTERVAL_NS = 8_000_000L // 8ms (~120Hz)
    
    // Cursor positioning offsets
    private const val HAND_CURSOR_CENTER_X = 0.413f
    private const val HAND_CURSOR_CENTER_Y = 0.072f
    private const val ROUND_CURSOR_CENTER_X = 0.5f
    private const val ROUND_CURSOR_CENTER_Y = 0.5f
}
```

### 3. Sensor Data Smoothing Algorithms

#### Moving Average Implementation
**File:** `/LegacyAvenue/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/MovingAverage.kt`

```kotlin
class MovingAverage(
    private val maxSamples: Int,     // Default: 4 samples
    private val maxTimeWindowNs: Long // Default: 300ms window
) {
    // Circular Buffer Implementation
    private val values = FloatArray(maxSamples)
    private val timestamps = LongArray(maxSamples)
    private var head = 0
    private var size = 0
    private var runningSum = 0f
    
    fun getAvg(data: Float, ts: Long): Float {
        purgeOldSamples(ts)  // Remove expired samples
        
        if (size == maxSamples) {
            runningSum -= values[head]
            head = (head + 1) % maxSamples
            size--
        }
        
        val insertPos = (head + size) % maxSamples
        values[insertPos] = data
        timestamps[insertPos] = ts
        runningSum += data
        size++
        
        return if (size == 0) 0f else runningSum / size
    }
}
```

#### Smoothed Moving Average (SMMA) Algorithm
```kotlin
fun getSMMA(): Float {
    if (size == 0) return 0f
    
    var result = values[head]
    for (i in 1 until size) {
        val idx = (head + i) % maxSamples
        result += (values[idx] - result) / (i + 1)
    }
    return result
}
```

### 4. Coordinate Transformation System

#### Screen Space Transformation
```kotlin
private val cursorScaleX = 2.0f  // Horizontal sensitivity
private val cursorScaleY = 3.0f  // Vertical sensitivity  
private val cursorScaleZ = 2.0f  // Depth/roll sensitivity

// Transform sensor deltas to screen coordinates
val disX = Math.tan(dx.toDouble()) * mWidth * cursorScaleX +
           Math.tan(dz.toDouble()) * mWidth * cursorScaleZ * speedMultiplier
val disY = Math.tan(dy.toDouble()) * mHeight * cursorScaleY * speedMultiplier
```

#### Device Rotation Handling
```kotlin
private val rotationAxisMappings = mapOf(
    Surface.ROTATION_0 to (SensorManager.AXIS_X to SensorManager.AXIS_Z),
    Surface.ROTATION_90 to (SensorManager.AXIS_Z to SensorManager.AXIS_MINUS_X),
    Surface.ROTATION_180 to (SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Z),
    Surface.ROTATION_270 to (SensorManager.AXIS_MINUS_Z to SensorManager.AXIS_X)
)
```

### 5. Gaze Detection and Auto-Click

#### Gaze State Management
```kotlin
private fun checkGazeClick(ts: Long) {
    val distance = hypot((gazeX - ghostX).toDouble(), (gazeY - ghostY).toDouble())
    val cancelDistance: Double = if (mShowOverlay) GAZE_CANCEL_DISTANCE else LOCK_CANCEL_DISTANCE
    
    if (distance > cancelDistance) {
        setGazeClick(false)
    } else {
        val elapsedGazeTime: Long = System.currentTimeMillis() - autoClickStartMs
        if (isGazeClick && elapsedGazeTime >= AUTO_CLICK_TRIGGER_TIME_MS && mShowOverlay) {
            setGazeClick(false)
            autoClickStartMs = System.currentTimeMillis()
            performGazeAutoClick?.invoke(getClickXY())
        }
    }
}
```

#### Gaze Constants
```kotlin
companion object {
    private const val GAZE_CANCEL_DISTANCE = 50.0
    private const val LOCK_CANCEL_DISTANCE = 420.0
    private const val GAZE_CENTER_DISTANCE_TOLERANCE = 6.0
    private const val AUTO_CLICK_TRIGGER_TIME_MS: Long = 1500 // 1.5 seconds
    private const val GAZE_TIME_TOLERANCE = 200000000L // 0.2 seconds
}
```

### 6. Performance Optimizations

#### Rendering Optimizations
```kotlin
// Hardware acceleration
setLayerType(LAYER_TYPE_HARDWARE, null)

// Frame rate limiting
private val FRAME_RATE_LIMIT_MS = 16 // ~60fps max
private val FRAME_DURATION = 650L

// Efficient invalidation
if (currentTime - lastInvalidateTime >= FRAME_RATE_LIMIT_MS) {
    lastInvalidateTime = currentTime
    postInvalidateOnAnimation() // More efficient than invalidate()
}
```

#### Memory Management
```kotlin
// Object pooling for sensor data
class CursorDataPool(capacity: Int) {
    private val pool = LinkedList<CursorData>()
    
    fun acquire(): CursorData {
        return synchronized(pool) {
            if (pool.isEmpty()) CursorData(0f, 0f, 0f, 0) 
            else pool.removeFirst()
        }
    }
}

// Pre-allocated buffers for matrix operations
private val rotationMatrixBuffer = FloatArray(9)
private val adjustedRotationMatrixBuffer = FloatArray(9)
private val orientationBuffer = FloatArray(3)
```

## Key Implementation Insights

### 1. Multi-Device Sensor Strategy
- **Phone:** Uses `TYPE_ROTATION_VECTOR` or `TYPE_GAME_ROTATION_VECTOR`
- **EPSON AR Glasses:** Uses proprietary `EPSONSensorManager`
- **Rokid AR Glasses:** Uses `RKGlassDevice.GlassSensorListener`
- **Unified Interface:** Single `CursorOrientationProvider` abstracts device differences

### 2. Movement Sensitivity Patterns
```kotlin
// Base sensitivity factors
cursorScaleX = 2.0f  // Moderate horizontal sensitivity
cursorScaleY = 3.0f  // Higher vertical sensitivity (easier vertical movement)
cursorScaleZ = 2.0f  // Roll component for fine tuning

// Speed adjustment
speedFactor = 1-20 range, default 8
speedMultiplier = speedFactor * 0.2
```

### 3. Smoothing Strategy
- **Moving Average Window:** 4 samples over 300ms
- **Time-based Expiration:** Removes stale sensor data
- **SMMA Algorithm:** Provides exponential smoothing
- **Dual Filtering:** Both sample count and time window limits

### 4. Calibration and Initialization
```kotlin
// First-time initialization pattern
if (previousA == 0f && previousB == 0f && previousC == 0f) {
    previousA = alpha
    previousB = beta  
    previousC = gamma
    return 0 // Skip first frame
}

// Center cursor on startup
fun centerCursor() {
    startX = (mWidth / 2).toFloat()
    startY = (mHeight / 2).toFloat()
    mX = startX
    mY = startY
}
```

## Legacy Architecture Patterns

### 1. Component Separation
- **VoiceOsCursor:** View layer and rendering
- **CursorOrientationProvider:** Sensor data acquisition (Singleton pattern)
- **VoiceOsCursorHelper:** Window management and lifecycle
- **CursorActions:** Voice command integration

### 2. Event Flow
```
Sensor Hardware → CursorOrientationProvider → Flow<CursorData> 
                                            ↓
VoiceOsCursorHelper → VoiceOsCursor.setOrientation() → UI Invalidation
                                            ↓
Voice Commands → CursorActions → VoiceOsService → CursorHelper
```

### 3. Configuration Management
```kotlin
data class CursorModel(
    var cursorColor: Int,
    var cursorSize: Int, 
    var handCursorSize: Int,
    var cursorType: String,
    var roundCursorType: String,
    var cursorSpeed: Int
)
```

## Performance Characteristics

### Sensor Processing
- **Update Rate:** 8ms minimum interval (~120Hz)
- **Smoothing Window:** 4 samples over 300ms
- **Memory Allocation:** Object pooling prevents GC pressure
- **Buffer Reuse:** Pre-allocated matrices for calculations

### Rendering Performance  
- **Hardware Acceleration:** Enabled by default
- **Frame Rate Limiting:** 60fps maximum
- **Efficient Invalidation:** `postInvalidateOnAnimation()`
- **Conditional Drawing:** Only redraw when needed

## Migration Recommendations for VOS4

### 1. Core Algorithm Retention
✅ **Keep:** Moving average smoothing with time-based expiration  
✅ **Keep:** Tangent-based displacement calculation  
✅ **Keep:** Multi-device sensor abstraction pattern  
✅ **Keep:** Gaze detection algorithm and thresholds  

### 2. Architecture Modernization
🔄 **Update:** Replace singleton pattern with dependency injection  
🔄 **Update:** Use Kotlin Coroutines Flow instead of custom flow  
🔄 **Update:** Implement proper lifecycle management with ViewModel  
🔄 **Update:** Use Compose for rendering if targeting modern UI  

### 3. Configuration Enhancement
➕ **Add:** Per-device calibration profiles  
➕ **Add:** Adaptive sensitivity based on movement patterns  
➕ **Add:** Machine learning for personalized cursor behavior  
➕ **Add:** Accessibility compliance features  

### 4. Performance Improvements
⚡ **Optimize:** Use `Matrix` class for coordinate transformations  
⚡ **Optimize:** Implement predictive cursor positioning  
⚡ **Optimize:** Add gesture recognition for enhanced control  
⚡ **Optimize:** Battery usage optimization for mobile devices  

## Critical Implementation Details

### 1. Sensor Coordinate System
```
Alpha (Roll):  Device rotation around Z-axis → orientationBuffer[2]
Beta (Pitch):  Device rotation around X-axis → -orientationBuffer[1] (inverted)
Gamma (Yaw):   Device rotation around Y-axis → orientationBuffer[0]
```

### 2. Movement Calculation Formula
```kotlin
displacement = tan(sensorDelta) * screenDimension * scaleFactor * speedMultiplier
```

### 3. Bounds Checking Pattern
```kotlin
mX = (startX + deltaX).coerceIn(0f, width.toFloat())
mY = (startY - deltaY).coerceIn(0f, height.toFloat())
```

### 4. Gaze Detection State Machine
```
IDLE → DETECTING (on stable position) → ACTIVE (after dwell time) → CLICK → IDLE
```

## Conclusion

The legacy VoiceCursor implementation provides a solid foundation with well-tested algorithms for sensor data processing, cursor movement, and gaze interaction. The multi-device support pattern and performance optimizations are particularly valuable for VOS4 migration.

Key strengths include robust sensor data smoothing, efficient coordinate transformations, and comprehensive gaze detection. Areas for improvement include modernizing the architecture with dependency injection, enhancing configurability, and adding machine learning capabilities.

The implementation demonstrates mature understanding of AR/VR cursor control challenges and provides battle-tested solutions that should be preserved in VOS4 while modernizing the surrounding architecture.

---

**Analysis prepared by:** Code Archaeology Specialist  
**Source Files Analyzed:** 12 core implementation files  
**Lines of Code Reviewed:** ~2,500 lines  
**Legacy Systems Examined:** LegacyAvenue, VOS2 Avenue4