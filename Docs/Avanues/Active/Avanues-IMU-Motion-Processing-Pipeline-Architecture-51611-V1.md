# IMU Motion Processing Pipeline Architecture
## Head-Tracking Cursor Control for AR/VR/Accessibility

**Date:** 2025-10-26 22:10:08 PDT
**Target:** Kotlin Multiplatform (Android/iOS)
**Performance Target:** 60 FPS (16.67ms per frame)
**Latency Target:** <20ms total (AR: <5ms ideal)

---

## 1. Pipeline Architecture Overview

### 1.1 Chain of Responsibility Pattern

```kotlin
/**
 * Base processor interface for IMU motion data pipeline
 */
interface MotionProcessor {
    /** Next processor in the chain (null = end of chain) */
    var next: MotionProcessor?

    /** Process motion data and pass to next processor */
    suspend fun process(data: MotionData): MotionData

    /** Configuration for this processor */
    val config: ProcessorConfig
}

/**
 * Motion data container with timestamp and orientation
 */
data class MotionData(
    val timestamp: Long,          // Nanoseconds since epoch
    val pitch: Float,             // Degrees around X-axis
    val roll: Float,              // Degrees around Y-axis
    val yaw: Float,               // Degrees around Z-axis
    val quaternion: Quaternion?,  // Optional quaternion representation
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Pipeline builder for composing processors
 */
class MotionPipeline private constructor(
    private val processors: List<MotionProcessor>
) {
    suspend fun process(data: MotionData): MotionData {
        var result = data
        for (processor in processors) {
            result = processor.process(result)
        }
        return result
    }

    class Builder {
        private val processors = mutableListOf<MotionProcessor>()

        fun addProcessor(processor: MotionProcessor) = apply {
            processors.add(processor)
        }

        fun build(): MotionPipeline {
            // Link processors
            for (i in 0 until processors.size - 1) {
                processors[i].next = processors[i + 1]
            }
            return MotionPipeline(processors)
        }
    }
}
```

### 1.2 Pipeline Composition Example

```kotlin
val arPipeline = MotionPipeline.Builder()
    .addProcessor(SensorFusionProcessor(config = fusionConfig))
    .addProcessor(AxisLockerProcessor(config = lockConfig))
    .addProcessor(RateLimiterProcessor(config = rateConfig))
    .addProcessor(MotionSmootherProcessor(algorithm = OneEuroFilter))
    .addProcessor(DeadZoneProcessor(config = deadZoneConfig))
    .build()

// Usage
val rawData = sensorManager.getCurrentMotion()
val processedData = arPipeline.process(rawData)
updateCursor(processedData)
```

---

## 2. Core Processors

### 2.1 AxisLockerProcessor

Locks individual axes to prevent unwanted motion on specific degrees of freedom.

```kotlin
data class AxisLockConfig(
    val lockPitch: Boolean = false,
    val lockRoll: Boolean = false,
    val lockYaw: Boolean = false,
    val resetToZero: Boolean = true  // Reset locked axes to 0 or preserve last value
) : ProcessorConfig

class AxisLockerProcessor(
    override val config: AxisLockConfig,
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private var lastUnlockedPitch = 0f
    private var lastUnlockedRoll = 0f
    private var lastUnlockedYaw = 0f

    override suspend fun process(data: MotionData): MotionData {
        val processed = data.copy(
            pitch = if (config.lockPitch) {
                if (config.resetToZero) 0f else lastUnlockedPitch
            } else {
                lastUnlockedPitch = data.pitch
                data.pitch
            },
            roll = if (config.lockRoll) {
                if (config.resetToZero) 0f else lastUnlockedRoll
            } else {
                lastUnlockedRoll = data.roll
                data.roll
            },
            yaw = if (config.lockYaw) {
                if (config.resetToZero) 0f else lastUnlockedYaw
            } else {
                lastUnlockedYaw = data.yaw
                data.yaw
            }
        )

        return next?.process(processed) ?: processed
    }
}
```

### 2.2 RateLimiterProcessor

Limits maximum rate of change with acceleration curves for smooth motion.

```kotlin
data class RateLimitConfig(
    val maxDegreesPerSecond: Float = 180f,  // Max angular velocity
    val accelerationCurve: AccelerationCurve = AccelerationCurve.LINEAR,
    val easeInDuration: Long = 100_000_000L,  // Nanoseconds (100ms)
    val easeOutDuration: Long = 100_000_000L
) : ProcessorConfig

enum class AccelerationCurve {
    LINEAR,      // Constant velocity
    EASE_IN,     // Gradual acceleration
    EASE_OUT,    // Gradual deceleration
    EASE_IN_OUT  // S-curve
}

class RateLimiterProcessor(
    override val config: RateLimitConfig,
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private var lastData: MotionData? = null

    override suspend fun process(data: MotionData): MotionData {
        val last = lastData ?: run {
            lastData = data
            return next?.process(data) ?: data
        }

        val deltaTime = (data.timestamp - last.timestamp) / 1_000_000_000f  // Convert to seconds
        val maxChange = config.maxDegreesPerSecond * deltaTime

        // Apply acceleration curve
        val curvedMaxChange = applyCurve(maxChange, deltaTime)

        val processed = data.copy(
            pitch = clampChange(last.pitch, data.pitch, curvedMaxChange),
            roll = clampChange(last.roll, data.roll, curvedMaxChange),
            yaw = clampChange(last.yaw, data.yaw, curvedMaxChange)
        )

        lastData = processed
        return next?.process(processed) ?: processed
    }

    private fun clampChange(oldValue: Float, newValue: Float, maxChange: Float): Float {
        val delta = newValue - oldValue
        return when {
            delta > maxChange -> oldValue + maxChange
            delta < -maxChange -> oldValue - maxChange
            else -> newValue
        }
    }

    private fun applyCurve(maxChange: Float, deltaTime: Float): Float {
        return when (config.accelerationCurve) {
            AccelerationCurve.LINEAR -> maxChange
            AccelerationCurve.EASE_IN -> maxChange * easeIn(deltaTime, config.easeInDuration)
            AccelerationCurve.EASE_OUT -> maxChange * easeOut(deltaTime, config.easeOutDuration)
            AccelerationCurve.EASE_IN_OUT -> maxChange * easeInOut(deltaTime)
        }
    }

    private fun easeIn(t: Float, duration: Long): Float {
        val normalized = (t * 1_000_000_000f / duration).coerceIn(0f, 1f)
        return normalized * normalized
    }

    private fun easeOut(t: Float, duration: Long): Float {
        val normalized = (t * 1_000_000_000f / duration).coerceIn(0f, 1f)
        return normalized * (2f - normalized)
    }

    private fun easeInOut(t: Float): Float {
        val normalized = t.coerceIn(0f, 1f)
        return if (normalized < 0.5f) {
            2f * normalized * normalized
        } else {
            -1f + (4f - 2f * normalized) * normalized
        }
    }
}
```

### 2.3 MotionSmootherProcessor

Multiple smoothing algorithms with pluggable strategy pattern.

```kotlin
sealed interface SmoothingAlgorithm {
    suspend fun smooth(current: MotionData, history: List<MotionData>): MotionData
}

data class SmootherConfig(
    val algorithm: SmoothingAlgorithm,
    val historySize: Int = 10  // For moving average
) : ProcessorConfig

class MotionSmootherProcessor(
    override val config: SmootherConfig,
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private val history = ArrayDeque<MotionData>(config.historySize)

    override suspend fun process(data: MotionData): MotionData {
        history.addLast(data)
        if (history.size > config.historySize) {
            history.removeFirst()
        }

        val smoothed = config.algorithm.smooth(data, history.toList())
        return next?.process(smoothed) ?: smoothed
    }
}
```

#### 2.3.1 Exponential Moving Average

```kotlin
class ExponentialSmoothingAlgorithm(
    private val alpha: Float = 0.2f  // 0-1: higher = less smoothing
) : SmoothingAlgorithm {

    private var lastSmoothed: MotionData? = null

    override suspend fun smooth(current: MotionData, history: List<MotionData>): MotionData {
        val last = lastSmoothed ?: run {
            lastSmoothed = current
            return current
        }

        val smoothed = current.copy(
            pitch = alpha * current.pitch + (1 - alpha) * last.pitch,
            roll = alpha * current.roll + (1 - alpha) * last.roll,
            yaw = alpha * current.yaw + (1 - alpha) * last.yaw
        )

        lastSmoothed = smoothed
        return smoothed
    }
}
```

#### 2.3.2 One Euro Filter (Recommended for Low Latency)

The One Euro Filter is speed-adaptive: low cutoff at low speeds (reduces jitter), high cutoff at high speeds (reduces lag).

**Key Parameters:**
- `minCutoff`: Minimum cutoff frequency (Hz) - lower = more smoothing at rest
- `beta`: Speed coefficient - higher = less lag during motion
- `dCutoff`: Cutoff frequency for derivative

```kotlin
class OneEuroFilterAlgorithm(
    private val minCutoff: Float = 1.0f,      // Hz
    private val beta: Float = 0.007f,         // Speed coefficient
    private val dCutoff: Float = 1.0f         // Derivative cutoff
) : SmoothingAlgorithm {

    private var lastRaw: MotionData? = null
    private var lastFiltered: MotionData? = null

    // Separate filters for each axis
    private val pitchFilter = OneEuroAxis()
    private val rollFilter = OneEuroAxis()
    private val yawFilter = OneEuroAxis()

    override suspend fun smooth(current: MotionData, history: List<MotionData>): MotionData {
        val last = lastRaw ?: run {
            lastRaw = current
            lastFiltered = current
            return current
        }

        val deltaTime = (current.timestamp - last.timestamp) / 1_000_000_000f  // Seconds
        val freq = 1f / deltaTime

        val smoothed = current.copy(
            pitch = pitchFilter.filter(current.pitch, freq),
            roll = rollFilter.filter(current.roll, freq),
            yaw = yawFilter.filter(current.yaw, freq)
        )

        lastRaw = current
        lastFiltered = smoothed
        return smoothed
    }

    private inner class OneEuroAxis {
        private var lastValue = 0f
        private var lastDerivative = 0f

        fun filter(value: Float, freq: Float): Float {
            // Calculate derivative
            val derivative = (value - lastValue) * freq
            val smoothedDerivative = exponentialSmoothing(
                derivative,
                lastDerivative,
                alpha(freq, dCutoff)
            )

            // Adaptive cutoff based on speed
            val cutoff = minCutoff + beta * kotlin.math.abs(smoothedDerivative)

            // Apply low-pass filter
            val smoothed = exponentialSmoothing(value, lastValue, alpha(freq, cutoff))

            lastValue = smoothed
            lastDerivative = smoothedDerivative
            return smoothed
        }

        private fun alpha(freq: Float, cutoff: Float): Float {
            val tau = 1f / (2f * kotlin.math.PI.toFloat() * cutoff)
            return 1f / (1f + tau * freq)
        }

        private fun exponentialSmoothing(value: Float, lastValue: Float, alpha: Float): Float {
            return alpha * value + (1f - alpha) * lastValue
        }
    }
}
```

**Tuning Guide:**
1. Set `beta = 0` and adjust `minCutoff` at low speeds to minimize jitter
2. Fix `minCutoff` and increase `beta` at high speeds to minimize lag

#### 2.3.3 Moving Average

```kotlin
class MovingAverageAlgorithm(
    private val windowSize: Int = 5
) : SmoothingAlgorithm {

    override suspend fun smooth(current: MotionData, history: List<MotionData>): MotionData {
        val window = history.takeLast(windowSize)

        return current.copy(
            pitch = window.map { it.pitch }.average().toFloat(),
            roll = window.map { it.roll }.average().toFloat(),
            yaw = window.map { it.yaw }.average().toFloat()
        )
    }
}
```

---

## 3. Sensor Fusion Processor

### 3.1 Complementary Filter (Recommended for Performance)

**Advantages:**
- Computationally efficient (~2x faster than Kalman)
- Easier to tune (fewer parameters)
- Accuracy within 0.4° of Kalman filter
- Excellent for 60 FPS real-time applications

```kotlin
data class ComplementaryFilterConfig(
    val alpha: Float = 0.98f,  // Gyro trust (0.95-0.99 typical)
    val gyroNoiseThreshold: Float = 0.1f  // Degrees/sec
) : ProcessorConfig

class ComplementaryFilterProcessor(
    override val config: ComplementaryFilterConfig,
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private var lastFused: MotionData? = null

    override suspend fun process(data: MotionData): MotionData {
        val last = lastFused ?: run {
            lastFused = data
            return next?.process(data) ?: data
        }

        val deltaTime = (data.timestamp - last.timestamp) / 1_000_000_000f

        // Complementary filter: high-pass gyro + low-pass accelerometer
        // gyro provides short-term accuracy, accel provides long-term stability
        val fused = data.copy(
            pitch = config.alpha * (last.pitch + data.pitch * deltaTime) +
                    (1 - config.alpha) * data.pitch,
            roll = config.alpha * (last.roll + data.roll * deltaTime) +
                   (1 - config.alpha) * data.roll,
            yaw = config.alpha * (last.yaw + data.yaw * deltaTime) +
                  (1 - config.alpha) * data.yaw
        )

        lastFused = fused
        return next?.process(fused) ?: fused
    }
}
```

### 3.2 Kalman Filter (Higher Accuracy, More Computation)

**Advantages:**
- Optimal for Gaussian noise
- Better drift correction
- More robust to sensor bias

**Disadvantages:**
- ~2x computational cost
- More complex tuning (Q and R matrices)

```kotlin
data class KalmanFilterConfig(
    val processNoise: Float = 0.01f,     // Q matrix diagonal
    val measurementNoise: Float = 0.1f   // R matrix diagonal
) : ProcessorConfig

class KalmanFilterProcessor(
    override val config: KalmanFilterConfig,
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private val pitchKalman = KalmanFilter1D(config.processNoise, config.measurementNoise)
    private val rollKalman = KalmanFilter1D(config.processNoise, config.measurementNoise)
    private val yawKalman = KalmanFilter1D(config.processNoise, config.measurementNoise)

    override suspend fun process(data: MotionData): MotionData {
        val filtered = data.copy(
            pitch = pitchKalman.filter(data.pitch),
            roll = rollKalman.filter(data.roll),
            yaw = yawKalman.filter(data.yaw)
        )

        return next?.process(filtered) ?: filtered
    }
}

/**
 * 1D Kalman filter for single axis
 */
class KalmanFilter1D(
    private val processNoise: Float,
    private val measurementNoise: Float
) {
    private var estimate = 0f
    private var errorCovariance = 1f

    fun filter(measurement: Float): Float {
        // Prediction
        val predictedError = errorCovariance + processNoise

        // Update
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        estimate += kalmanGain * (measurement - estimate)
        errorCovariance = (1 - kalmanGain) * predictedError

        return estimate
    }
}
```

---

## 4. Platform-Specific Sensor Access

### 4.1 Android (SensorManager)

```kotlin
// android/src/main/kotlin/com/augmentalis/avacode/sensors/AndroidIMUSensor.kt

class AndroidIMUSensor(
    private val context: Context
) : IMUSensor {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gyroData = FloatArray(3)
    private val accelData = FloatArray(3)
    private val magData = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    override fun start(callback: (MotionData) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        System.arraycopy(event.values, 0, gyroData, 0, 3)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelData, 0, 3)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magData, 0, 3)

                        // Compute orientation when magnetometer updates (~100Hz)
                        SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magData)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        callback(MotionData(
                            timestamp = event.timestamp,
                            pitch = Math.toDegrees(orientation[1].toDouble()).toFloat(),
                            roll = Math.toDegrees(orientation[2].toDouble()).toFloat(),
                            yaw = Math.toDegrees(orientation[0].toDouble()).toFloat(),
                            quaternion = null
                        ))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        // SENSOR_DELAY_GAME = ~50Hz, SENSOR_DELAY_FASTEST = ~200Hz
        // For 60 FPS, use SENSOR_DELAY_GAME (20ms = 50Hz, sufficient for 16.67ms frames)
        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
    }
}
```

**Android Latency Optimization:**
- Use `SENSOR_DELAY_GAME` (50Hz) for 60 FPS - sufficient headroom
- Use `SENSOR_DELAY_FASTEST` only if experiencing lag (200Hz on modern devices)
- Register listeners on background thread to avoid blocking UI
- Avoid allocations in `onSensorChanged` (reuse arrays)

### 4.2 iOS (CoreMotion)

```kotlin
// ios/src/main/kotlin/com/augmentalis/avacode/sensors/IOSIMUSensor.kt

import platform.CoreMotion.*
import platform.Foundation.*

class IOSIMUSensor : IMUSensor {

    private val motionManager = CMMotionManager()

    override fun start(callback: (MotionData) -> Unit) {
        if (!motionManager.deviceMotionAvailable) {
            throw IllegalStateException("Device motion not available")
        }

        // 60 FPS = 16.67ms, set to 100Hz (10ms) for headroom
        motionManager.deviceMotionUpdateInterval = 0.01  // 100Hz

        motionManager.startDeviceMotionUpdatesUsingReferenceFrame(
            CMAttitudeReferenceFrameXArbitraryZVertical,
            toQueue = NSOperationQueue.mainQueue,
            withHandler = { motion, error ->
                if (motion != null) {
                    val attitude = motion.attitude

                    callback(MotionData(
                        timestamp = (motion.timestamp * 1_000_000_000).toLong(),  // Convert to nanos
                        pitch = Math.toDegrees(attitude.pitch).toFloat(),
                        roll = Math.toDegrees(attitude.roll).toFloat(),
                        yaw = Math.toDegrees(attitude.yaw).toFloat(),
                        quaternion = Quaternion(
                            x = attitude.quaternion.x.toFloat(),
                            y = attitude.quaternion.y.toFloat(),
                            z = attitude.quaternion.z.toFloat(),
                            w = attitude.quaternion.w.toFloat()
                        )
                    ))
                }
            }
        )
    }

    override fun stop() {
        motionManager.stopDeviceMotionUpdates()
    }
}

data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
)
```

**iOS Latency Optimization:**
- CoreMotion provides fused sensor data at ~100Hz
- Use dedicated `NSOperationQueue` for sensor updates (not main queue)
- Quaternions avoid gimbal lock and are more efficient for 3D rotations
- Reference frame `XArbitraryZVertical` best for head tracking

---

## 5. Mode Presets

### 5.1 AR Mode (Ultra-Low Latency)

```kotlin
object ARModePreset {
    fun createPipeline(): MotionPipeline {
        return MotionPipeline.Builder()
            .addProcessor(ComplementaryFilterProcessor(
                config = ComplementaryFilterConfig(alpha = 0.98f)
            ))
            .addProcessor(MotionSmootherProcessor(
                config = SmootherConfig(
                    algorithm = OneEuroFilterAlgorithm(
                        minCutoff = 1.0f,
                        beta = 0.007f  // Low beta for minimal lag
                    )
                )
            ))
            .addProcessor(RateLimiterProcessor(
                config = RateLimitConfig(
                    maxDegreesPerSecond = 360f,  // High limit for fast head turns
                    accelerationCurve = AccelerationCurve.LINEAR
                )
            ))
            .build()
    }
}
```

**AR Characteristics:**
- Target: <5ms latency
- Minimal smoothing (prefer responsiveness over jitter reduction)
- High angular velocity limits
- Complementary filter for speed

### 5.2 VR Mode (Balanced)

```kotlin
object VRModePreset {
    fun createPipeline(): MotionPipeline {
        return MotionPipeline.Builder()
            .addProcessor(KalmanFilterProcessor(
                config = KalmanFilterConfig(
                    processNoise = 0.01f,
                    measurementNoise = 0.1f
                )
            ))
            .addProcessor(MotionSmootherProcessor(
                config = SmootherConfig(
                    algorithm = OneEuroFilterAlgorithm(
                        minCutoff = 1.5f,
                        beta = 0.01f
                    )
                )
            ))
            .addProcessor(RateLimiterProcessor(
                config = RateLimitConfig(
                    maxDegreesPerSecond = 300f,
                    accelerationCurve = AccelerationCurve.EASE_IN_OUT
                )
            ))
            .build()
    }
}
```

**VR Characteristics:**
- Target: <20ms latency
- More smoothing than AR (reduce nausea)
- Kalman filter for better accuracy
- Ease curves for natural feel

### 5.3 Accessibility Mode (Maximum Smoothing)

```kotlin
object AccessibilityModePreset {
    fun createPipeline(): MotionPipeline {
        return MotionPipeline.Builder()
            .addProcessor(KalmanFilterProcessor(
                config = KalmanFilterConfig(
                    processNoise = 0.005f,
                    measurementNoise = 0.2f
                )
            ))
            .addProcessor(AxisLockerProcessor(
                config = AxisLockConfig(
                    lockRoll = true  // Lock roll for easier control
                )
            ))
            .addProcessor(MotionSmootherProcessor(
                config = SmootherConfig(
                    algorithm = OneEuroFilterAlgorithm(
                        minCutoff = 2.0f,  // High cutoff = more smoothing
                        beta = 0.02f
                    )
                )
            ))
            .addProcessor(RateLimiterProcessor(
                config = RateLimitConfig(
                    maxDegreesPerSecond = 120f,  // Slow movement
                    accelerationCurve = AccelerationCurve.EASE_IN_OUT,
                    easeInDuration = 200_000_000L,  // 200ms ease
                    easeOutDuration = 200_000_000L
                )
            ))
            .addProcessor(DeadZoneProcessor(
                config = DeadZoneConfig(threshold = 2.0f)  // 2-degree dead zone
            ))
            .build()
    }
}

// Dead zone processor for tremor compensation
data class DeadZoneConfig(
    val threshold: Float = 1.0f  // Degrees
) : ProcessorConfig

class DeadZoneProcessor(
    override val config: DeadZoneConfig,
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private var centerPitch = 0f
    private var centerRoll = 0f
    private var centerYaw = 0f

    override suspend fun process(data: MotionData): MotionData {
        val processed = data.copy(
            pitch = applyDeadZone(data.pitch, centerPitch),
            roll = applyDeadZone(data.roll, centerRoll),
            yaw = applyDeadZone(data.yaw, centerYaw)
        )

        return next?.process(processed) ?: processed
    }

    private fun applyDeadZone(value: Float, center: Float): Float {
        val delta = value - center
        return if (kotlin.math.abs(delta) < config.threshold) {
            center
        } else {
            value
        }
    }

    fun recenter(data: MotionData) {
        centerPitch = data.pitch
        centerRoll = data.roll
        centerYaw = data.yaw
    }
}
```

**Accessibility Characteristics:**
- Maximum smoothing and stability
- Axis locking options
- Dead zone for tremor compensation
- Slow, predictable movement
- Easy recenter functionality

---

## 6. Performance Optimization Techniques

### 6.1 Memory Management

```kotlin
// Use object pools to avoid allocations at 60 FPS
class MotionDataPool(size: Int = 60) {
    private val pool = ArrayDeque<MotionData>(size)

    fun obtain(
        timestamp: Long,
        pitch: Float,
        roll: Float,
        yaw: Float
    ): MotionData {
        return pool.removeFirstOrNull()?.copy(
            timestamp = timestamp,
            pitch = pitch,
            roll = roll,
            yaw = yaw
        ) ?: MotionData(timestamp, pitch, roll, yaw, null)
    }

    fun recycle(data: MotionData) {
        pool.addLast(data)
    }
}
```

### 6.2 Coroutine Optimization

```kotlin
// Dedicated dispatcher for sensor processing
val sensorDispatcher = newSingleThreadContext("SensorThread")

class OptimizedMotionPipeline(
    private val pipeline: MotionPipeline,
    private val scope: CoroutineScope
) {
    private val channel = Channel<MotionData>(Channel.CONFLATED)  // Drop old data

    init {
        scope.launch(sensorDispatcher) {
            for (data in channel) {
                val processed = pipeline.process(data)
                // Send to UI thread
                withContext(Dispatchers.Main) {
                    updateUI(processed)
                }
            }
        }
    }

    fun submit(data: MotionData) {
        channel.trySend(data)  // Non-blocking
    }
}
```

### 6.3 Frame Pacing

```kotlin
class FramePacer(
    private val targetFps: Int = 60
) {
    private val frameTime = 1_000_000_000L / targetFps  // Nanoseconds
    private var lastFrameTime = 0L

    fun shouldProcessFrame(timestamp: Long): Boolean {
        if (timestamp - lastFrameTime >= frameTime) {
            lastFrameTime = timestamp
            return true
        }
        return false
    }
}

// Usage in sensor callback
val pacer = FramePacer(60)

sensorManager.onMotionUpdate { data ->
    if (pacer.shouldProcessFrame(data.timestamp)) {
        pipeline.submit(data)
    }
}
```

### 6.4 Predictive Tracking (Advanced)

For ultra-low latency AR, predict future head position:

```kotlin
class PredictiveProcessor(
    private val predictionMs: Long = 10,  // Predict 10ms ahead
    override var next: MotionProcessor? = null
) : MotionProcessor {

    private val history = ArrayDeque<MotionData>(5)

    override suspend fun process(data: MotionData): MotionData {
        history.addLast(data)
        if (history.size > 5) history.removeFirst()

        if (history.size < 2) return next?.process(data) ?: data

        // Calculate velocity
        val last = history[history.size - 2]
        val deltaTime = (data.timestamp - last.timestamp) / 1_000_000f  // ms

        val pitchVelocity = (data.pitch - last.pitch) / deltaTime
        val rollVelocity = (data.roll - last.roll) / deltaTime
        val yawVelocity = (data.yaw - last.yaw) / deltaTime

        // Predict
        val predicted = data.copy(
            pitch = data.pitch + pitchVelocity * predictionMs,
            roll = data.roll + rollVelocity * predictionMs,
            yaw = data.yaw + yawVelocity * predictionMs
        )

        return next?.process(predicted) ?: predicted
    }
}
```

---

## 7. Testing and Benchmarking

### 7.1 Latency Measurement

```kotlin
class LatencyBenchmark {
    private val latencies = mutableListOf<Long>()

    fun measurePipeline(pipeline: MotionPipeline, testData: List<MotionData>) {
        testData.forEach { data ->
            val start = System.nanoTime()
            runBlocking { pipeline.process(data) }
            val end = System.nanoTime()
            latencies.add(end - start)
        }

        println("Average latency: ${latencies.average() / 1_000_000} ms")
        println("95th percentile: ${latencies.sorted()[latencies.size * 95 / 100] / 1_000_000} ms")
        println("Max latency: ${latencies.max() / 1_000_000} ms")
    }
}
```

### 7.2 Smoothness Metrics

```kotlin
fun calculateJitter(data: List<MotionData>): Float {
    val deltas = data.zipWithNext { a, b ->
        kotlin.math.abs(b.pitch - a.pitch) +
        kotlin.math.abs(b.roll - a.roll) +
        kotlin.math.abs(b.yaw - a.yaw)
    }
    return deltas.average().toFloat()
}
```

---

## 8. Configuration Management

### 8.1 Intent-Based External Configuration

```kotlin
// JSON configuration file
data class PipelineConfig(
    val mode: String,  // "ar", "vr", "accessibility", "custom"
    val processors: List<ProcessorSpec>
)

data class ProcessorSpec(
    val type: String,  // "axis_locker", "rate_limiter", etc.
    val enabled: Boolean = true,
    val config: Map<String, Any>
)

// Example JSON
"""
{
  "mode": "ar",
  "processors": [
    {
      "type": "complementary_filter",
      "enabled": true,
      "config": {
        "alpha": 0.98
      }
    },
    {
      "type": "one_euro_filter",
      "enabled": true,
      "config": {
        "minCutoff": 1.0,
        "beta": 0.007
      }
    }
  ]
}
"""

class ConfigurablePipelineFactory {
    fun create(configJson: String): MotionPipeline {
        val config = Json.decodeFromString<PipelineConfig>(configJson)

        return when (config.mode) {
            "ar" -> ARModePreset.createPipeline()
            "vr" -> VRModePreset.createPipeline()
            "accessibility" -> AccessibilityModePreset.createPipeline()
            "custom" -> buildCustomPipeline(config.processors)
            else -> throw IllegalArgumentException("Unknown mode: ${config.mode}")
        }
    }

    private fun buildCustomPipeline(specs: List<ProcessorSpec>): MotionPipeline {
        val builder = MotionPipeline.Builder()

        specs.filter { it.enabled }.forEach { spec ->
            val processor = createProcessor(spec.type, spec.config)
            builder.addProcessor(processor)
        }

        return builder.build()
    }
}
```

---

## 9. References and Resources

### Academic Papers
- **One Euro Filter**: Casiez et al., "1€ Filter: A Simple Speed-based Low-pass Filter for Noisy Input in Interactive Systems" (CHI 2012)
- **Sensor Fusion**: Madgwick, "An efficient orientation filter for IMU and MARG sensor arrays" (2010)
- **Complementary vs Kalman**: "Performance Comparison of Experimental-based Kalman Filter and Complementary Filter for IMU Sensor Fusion"

### Platform Documentation
- **Android**: [Motion Sensors Guide](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion)
- **iOS**: [Core Motion Framework](https://developer.apple.com/documentation/coremotion)

### Performance Targets
- **AR**: <5ms motion-to-photon latency
- **VR**: <20ms motion-to-photon latency
- **Sensor Fusion**: Can reduce tracking latency to ~1ms
- **Android**: SENSOR_DELAY_GAME = 50Hz, sufficient for 60 FPS
- **iOS**: CoreMotion = 100Hz native

### Key Insights
1. **One Euro Filter** is optimal for low-latency head tracking (better than EMA or simple moving average)
2. **Complementary Filter** offers 98% of Kalman accuracy at 50% computational cost
3. **Predictive tracking** can reduce perceived latency by 10-20ms
4. **Frame pacing** is critical - process at consistent 60 FPS even if sensors run faster
5. **Object pooling** prevents GC pauses at high frame rates

---

## 10. Implementation Roadmap

### Phase 1: Core Infrastructure
1. Define `MotionProcessor` interface and `MotionData` class
2. Implement `MotionPipeline` builder with chain of responsibility
3. Create platform-specific sensor abstractions (Android/iOS)

### Phase 2: Basic Processors
1. `AxisLockerProcessor`
2. `RateLimiterProcessor` with acceleration curves
3. `ExponentialSmoothingAlgorithm`

### Phase 3: Advanced Filters
1. `OneEuroFilterAlgorithm` (priority for low latency)
2. `ComplementaryFilterProcessor`
3. `KalmanFilterProcessor` (optional)

### Phase 4: Mode Presets
1. AR Mode configuration
2. VR Mode configuration
3. Accessibility Mode with dead zone

### Phase 5: Optimization
1. Object pooling for MotionData
2. Coroutine-based processing pipeline
3. Frame pacing and predictive tracking
4. Latency benchmarking

### Phase 6: Configuration
1. JSON-based pipeline configuration
2. Runtime mode switching
3. User-customizable parameters

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**AvaCode Plugin System - Motion Processing Pipeline**
**2025-10-26 22:10:08 PDT**
