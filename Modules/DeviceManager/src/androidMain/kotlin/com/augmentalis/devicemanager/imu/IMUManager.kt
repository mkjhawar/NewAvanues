// Author: Manoj Jhawar
// Purpose: Centralized IMU data management for all VOS4 applications

package com.augmentalis.devicemanager.imu

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Centralized IMU Manager for VOS4
 * Provides high-quality motion data to all applications using advanced sensor fusion
 *
 * COT Analysis: Singleton Pattern with Capabilities Injection
 * - Singleton required for shared resource management across all consumers
 * - DeviceCapabilities injection solves chicken-egg problem:
 *   1. Manager created first time with context
 *   2. Capabilities injected separately once DeviceDetector runs
 *   3. All sensor checks use injected capabilities instead of direct queries
 * - This approach eliminates redundant sensor detection while maintaining singleton pattern
 */
class IMUManager private constructor(
    private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "IMUManager"
        private const val SENSOR_DELAY_MICROS = 16 * 1000 // 16ms

        @Volatile
        private var INSTANCE: IMUManager? = null

        fun getInstance(context: Context): IMUManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IMUManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        private const val MIN_SENSOR_INTERVAL_NS = 8_000_000L // 8ms (~120Hz)
    }

    // COT: Capabilities injection for singleton pattern
    // Since this is a singleton, we can't inject capabilities through constructor
    // Instead, we provide a method to inject them after instance creation
    // This allows DeviceDetector to provide capabilities without circular dependencies
    private var deviceCapabilities: DeviceDetector.DeviceCapabilities? = null

    // Initialize to ACCURACY_LOW so sensor events flow immediately after registerListener().
    // The default value 0 (SENSOR_STATUS_UNRELIABLE) would block ALL events until
    // onAccuracyChanged fires, which on some devices (RealWear smart glasses) may be delayed.
    private var mLastAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW

    /**
     * Inject device capabilities for sensor availability checks
     * Must be called before starting IMU tracking
     */
    fun injectCapabilities(capabilities: DeviceDetector.DeviceCapabilities) {
        synchronized(this) {
            this.deviceCapabilities = capabilities
            Log.d(TAG, "Device capabilities injected - sensors available: ${capabilities.sensors.hasAccelerometer}, ${capabilities.sensors.hasGyroscope}, ${capabilities.sensors.hasMagnetometer}")
        }
    }

    // Sensor management
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Suppress("DEPRECATION")
    private var display: Display = (context.getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
    private var rotation: Int = display.rotation
    private val rotationMatrixBuffer = FloatArray(9)
    private val adjustedRotationMatrixBuffer = FloatArray(9)
    private val orientationBuffer = FloatArray(3)
    private val rotationAxisMappings = mapOf(
        Surface.ROTATION_0 to (SensorManager.AXIS_X to SensorManager.AXIS_Z),
        Surface.ROTATION_90 to (SensorManager.AXIS_Z to SensorManager.AXIS_MINUS_X),
        Surface.ROTATION_180 to (SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Z),
        Surface.ROTATION_270 to (SensorManager.AXIS_MINUS_Z to SensorManager.AXIS_X)
    )
    private val lastProcessedTimestamp = AtomicLong(0)
    private var activeSensors = mutableSetOf<Sensor>()
    private val imuDataPool = IMUDataPool(10)

    // COT: Lazy sensor initialization based on capabilities
    // Instead of immediately querying sensors, we wait for capabilities injection
    // This prevents redundant sensor detection and uses centralized capability data
    private val rotationSensor: Sensor? by lazy {
        if (deviceCapabilities?.sensors?.hasGyroscope == true || deviceCapabilities?.sensors?.hasMagnetometer == true) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        } else {
            null
        }
    }

    private val gyroscopeSensor: Sensor? by lazy {
        if (deviceCapabilities?.sensors?.hasGyroscope == true) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        } else null
    }

    private val accelerometerSensor: Sensor? by lazy {
        if (deviceCapabilities?.sensors?.hasAccelerometer == true) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else null
    }

    private val magnetometerSensor: Sensor? by lazy {
        if (deviceCapabilities?.sensors?.hasMagnetometer == true) {
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        } else null
    }

    // Enhanced tracking components
    private val sensorFusion = EnhancedSensorFusion()
    private val motionPredictor = MotionPredictor()
    private val adaptiveFilter = AdaptiveFilter()
    private val calibrationManager = CalibrationManager()

    // Data flow for reactive programming
    private val _orientationFlow = MutableSharedFlow<IMUData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val orientationFlow: SharedFlow<IMUData> = _orientationFlow.asSharedFlow()

    private val _motionFlow = MutableSharedFlow<MotionData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val motionFlow: SharedFlow<MotionData> = _motionFlow.asSharedFlow()

    // Thread management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isActive = false

    // Consumer tracking
    private val activeConsumers = mutableSetOf<String>()
    private val consumerLock = Any()

    /**
     * Start IMU data collection
     * COT: Capabilities check prevents starting without proper device support
     */
    fun startIMUTracking(consumerId: String): Boolean {
        synchronized(consumerLock) {
            // COT: Ensure capabilities are injected before starting
            if (deviceCapabilities == null) {
                Log.e(TAG, "Cannot start IMU tracking - device capabilities not injected")
                return false
            }

            // COT: Check if device has any required IMU sensors
            val hasAnySensor = deviceCapabilities?.sensors?.let {
                it.hasAccelerometer || it.hasGyroscope || it.hasMagnetometer
            } ?: false

            if (!hasAnySensor) {
                Log.w(TAG, "Cannot start IMU tracking - no IMU sensors available on device")
                return false
            }

            activeConsumers.add(consumerId)

            if (isActive) {
                Log.d(TAG, "IMU already active, added consumer: $consumerId")
                return true
            }

            Log.i(TAG, "Starting IMU tracking for first consumer: $consumerId")
            return startSensors()
        }
    }

    /**
     * Stop IMU data collection for specific consumer
     */
    fun stopIMUTracking(consumerId: String) {
        synchronized(consumerLock) {
            activeConsumers.remove(consumerId)

            if (activeConsumers.isEmpty()) {
                Log.i(TAG, "No active consumers, stopping IMU tracking")
                stopSensors()
            } else {
                Log.d(TAG, "Removed consumer: $consumerId, ${activeConsumers.size} remaining")
            }
        }
    }

    /**
     * Get current orientation data synchronously
     */
    fun getCurrentOrientation(): IMUData? {
        return orientationFlow.replayCache.firstOrNull()
    }

    /**
     * Get current motion data synchronously
     */
    fun getCurrentMotion(): MotionData? {
        return motionFlow.replayCache.firstOrNull()
    }

    /**
     * Get available sensor capabilities from injected device capabilities
     * COT: No longer performs sensor detection - uses centralized capability data
     */
    fun getSensorCapabilities(): SensorCapabilities? {
        val caps = deviceCapabilities?.sensors ?: return null

        // COT: Build capabilities from injected data, not direct sensor queries
        return SensorCapabilities(
            hasGameRotationVector = caps.hasGyroscope && caps.hasMagnetometer,
            hasRotationVector = caps.hasGyroscope && caps.hasMagnetometer,
            hasGyroscope = caps.hasGyroscope,
            hasAccelerometer = caps.hasAccelerometer,
            hasMagnetometer = caps.hasMagnetometer,
            maxSampleRate = rotationSensor?.maxDelay ?: 0,
            resolution = rotationSensor?.resolution ?: 0f
        )
    }

    /**
     * Calibrate IMU for specific user
     */
    suspend fun calibrateForUser(): CalibrationResult {
        return calibrationManager.performCalibration(this)
    }

    private fun startSensors(): Boolean {
        try {
            var sensorsStarted = 0
            val caps = deviceCapabilities?.sensors

            // COT: Only attempt to start sensors that capabilities indicate are available
            // This prevents unnecessary sensor queries and registration attempts

            // Priority 1: Rotation vector sensors (best for orientation)
            if (caps?.hasGyroscope == true && caps.hasMagnetometer) {
                rotationSensor?.let { sensor ->
                    val success = sensorManager.registerListener(
                        this, sensor, SENSOR_DELAY_MICROS
                    )
                    if (success) {
                        activeSensors.add(sensor)
                        sensorsStarted++
                        Log.d(TAG, "Started rotation sensor: ${sensor.name}")
                    }
                }
            }

            // Priority 2: Individual sensors for fusion
            if (caps?.hasGyroscope == true && sensorsStarted ==0) {
                gyroscopeSensor?.let { sensor ->
                    val success = sensorManager.registerListener(
                        this, sensor, SENSOR_DELAY_MICROS
                    )
                    if (success) {
                        activeSensors.add(sensor)
                        sensorsStarted++
                        Log.d(TAG, "Started gyroscope sensor")
                    }
                }
            }

            if (caps?.hasAccelerometer == true && sensorsStarted ==0) {
                accelerometerSensor?.let { sensor ->
                    val success = sensorManager.registerListener(
                        this, sensor, SENSOR_DELAY_MICROS
                    )
                    if (success) {
                        activeSensors.add(sensor)
                        sensorsStarted++
                        Log.d(TAG, "Started accelerometer sensor")
                    }
                }
            }

            if (caps?.hasMagnetometer == true && sensorsStarted ==0) {
                magnetometerSensor?.let { sensor ->
                    val success = sensorManager.registerListener(
                        this, sensor, SENSOR_DELAY_MICROS
                    )
                    if (success) {
                        activeSensors.add(sensor)
                        sensorsStarted++
                        Log.d(TAG, "Started magnetometer sensor")
                    }
                }
            }

            isActive = sensorsStarted > 0

            if (isActive) {
                Log.i(TAG, "IMU tracking started with $sensorsStarted sensors")
            } else {
                Log.e(TAG, "Failed to start any IMU sensors")
            }

            return isActive
        } catch (e: Exception) {
            Log.e(TAG, "Error starting IMU sensors", e)
            return false
        }
    }

    private fun stopSensors() {
        try {
            sensorManager.unregisterListener(this)
            activeSensors.clear()
            isActive = false
            Log.i(TAG, "IMU tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping IMU sensors", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE){
            return
        }
        scope.launch {
            try {
                when (event.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR,
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        processRotationVector(event.values, event.timestamp, event.accuracy)
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        processGyroscope(event)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        processAccelerometer(event)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        processMagnetometer(event)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing sensor event", e)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name}, accuracy: $accuracy")
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy
        }
    }

    private suspend fun processRotationVector(values: FloatArray?, timestamp: Long, accuracy: Int) {
        if (values == null) return

        // Throttle processing based on timestamp
        val currentTs = System.nanoTime()
        val lastTs = lastProcessedTimestamp.get()

        if (currentTs - lastTs < MIN_SENSOR_INTERVAL_NS) {
            return // Skip this update
        }

        // Update timestamp atomically, only one thread will succeed
        if (!lastProcessedTimestamp.compareAndSet(lastTs, currentTs)) {
            return
        }

        // Reuse pre-allocated buffers
        SensorManager.getRotationMatrixFromVector(rotationMatrixBuffer, values)

        // Get axis mapping from cache
        val (worldAxisX, worldAxisY) = rotationAxisMappings[rotation]
            ?: (SensorManager.AXIS_X to SensorManager.AXIS_Z)

        // Reuse buffer for adjusted matrix
        SensorManager.remapCoordinateSystem(
            rotationMatrixBuffer,
            worldAxisX,
            worldAxisY,
            adjustedRotationMatrixBuffer
        )

        // Reuse orientation buffer
        SensorManager.getOrientation(adjustedRotationMatrixBuffer, orientationBuffer)

        val alpha = orientationBuffer[2]
        val beta = -orientationBuffer[1]
        val gamma = orientationBuffer[0]

        if (alpha.isNaN() || beta.isNaN() || gamma.isNaN()) return

        // Get instance from pool and populate
        val cursorData = imuDataPool.acquire().apply {
            this.alpha = alpha
            this.beta = beta
            this.gamma = gamma
            this.ts = timestamp
        }

        // Emit to flow
        _orientationFlow.emit(cursorData)
    }

    // TODO:  
    /*private suspend fun processRotationVector(event: SensorEvent) {
        val quaternion = Quaternion.fromRotationVector(event.values)
        val timestamp = event.timestamp

        // Apply sensor fusion and filtering
        val fusedOrientation = sensorFusion.processRotationVector(quaternion, timestamp)
        val filteredOrientation = adaptiveFilter.filterOrientation(fusedOrientation, timestamp)
        val predictedOrientation = motionPredictor.predictOrientation(filteredOrientation, timestamp)

        // Create orientation data
        val orientationData = OrientationData(
            quaternion = predictedOrientation,
            eulerAngles = predictedOrientation.toEulerAngles(),
            timestamp = timestamp,
            accuracy = event.accuracy
        )

        // Emit to flow
        _orientationFlow.emit(orientationData)
    }*/

    private suspend fun processGyroscope(event: SensorEvent) {
        val angularVelocity = Vector3(event.values[0], event.values[1], event.values[2])
        sensorFusion.processGyroscope(angularVelocity, event.timestamp)

        val motionData = MotionData(
            angularVelocity = angularVelocity,
            timestamp = event.timestamp,
            accuracy = event.accuracy
        )

        _motionFlow.emit(motionData)
    }

    private suspend fun processAccelerometer(event: SensorEvent) {
        val acceleration = Vector3(event.values[0], event.values[1], event.values[2])
        sensorFusion.processAccelerometer(acceleration, event.timestamp)
    }

    private suspend fun processMagnetometer(event: SensorEvent) {
        val magneticField = Vector3(event.values[0], event.values[1], event.values[2])
        sensorFusion.processMagnetometer(magneticField, event.timestamp)
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        synchronized(consumerLock) {
            activeConsumers.clear()
            stopSensors()
            scope.cancel()
            INSTANCE = null
            Log.i(TAG, "IMU Manager disposed")
        }
    }
}

/**
 * Data classes for IMU information
 */
data class OrientationData(
    val quaternion: Quaternion,
    val eulerAngles: EulerAngles,
    val timestamp: Long,
    val accuracy: Int
)

data class MotionData(
    val angularVelocity: Vector3,
    val timestamp: Long,
    val accuracy: Int
)

data class SensorCapabilities(
    val hasGameRotationVector: Boolean,
    val hasRotationVector: Boolean,
    val hasGyroscope: Boolean,
    val hasAccelerometer: Boolean,
    val hasMagnetometer: Boolean,
    val maxSampleRate: Int,
    val resolution: Float
)

data class CalibrationResult(
    val success: Boolean,
    val baseOrientation: Quaternion,
    val message: String
)