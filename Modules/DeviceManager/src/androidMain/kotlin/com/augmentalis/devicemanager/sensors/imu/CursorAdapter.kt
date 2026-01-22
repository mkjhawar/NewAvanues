/**
 * CursorAdapter.kt
 * Path: /libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/imu/CursorAdapter.kt
 *
 * Created: 2025-01-23 02:00 PDT
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Adapter to connect cursor applications with centralized IMUManager
 */

package com.augmentalis.devicemanager.sensors.imu

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Simplified interface for cursor applications to consume IMU data
 */
class CursorAdapter(
    private val context: Context,
    private val consumerId: String = "CursorApp"
) {

    companion object {
        private const val RADIAN_TOLERANCE = 0.002f
        private const val MIN_INTERVAL_NS = 8_000_000L // 8ms (~120Hz)
        private const val DEFAULT_DISPLACEMENT_FACTOR = 8
        private const val CURSOR_SCALE_X = 2.0f
        private const val CURSOR_SCALE_Y = 3.0f
        private const val CURSOR_SCALE_Z = 2.0f
        private const val CURSOR_DELAY_TIME = 0L
        private const val DISTANCE_TOLERANCE = 1.0f
    }

    val imuManager = IMUManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Cursor-specific processing
    private var baseOrientation = Quaternion.identity
    private var screenWidth = 1920
    private var screenHeight = 1080
    private var sensitivity = 1.0f

    // Enhanced cursor position tracking with legacy algorithm components
    private var currentX = 0f
    private var currentY = 0f
    private var centerX = 0f  // Center reference point
    private var centerY = 0f  // Center reference point
    private var startX = 0f
    private var startY = 0f

    // Previous orientation values for delta calculation
    private var previousYaw = 0f
    private var previousPitch = 0f
    private var previousRoll = 0f

    // MovingAverage filters for smooth sensor data
    private val maYaw = MovingAverage(4, 300000000L)  // 4 samples, 300ms window
    private val maPitch = MovingAverage(4, 300000000L)
    private val maRoll = MovingAverage(4, 300000000L)

    // Performance optimization
    private var lastProcessTime = 0L
    private var speedFactor = DEFAULT_DISPLACEMENT_FACTOR

    // Flow for cursor position updates
    private val _positionFlow = MutableSharedFlow<CursorPosition>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val positionFlow: SharedFlow<CursorPosition> = _positionFlow.asSharedFlow()

    private val lastPoint: LastPoint = LastPoint()

    private var isActive = false

    /**
     * Start cursor tracking
     */
    fun startTracking(): Boolean {
        if (isActive) return true

        val success = imuManager.startIMUTracking(consumerId)
        if (success) {
            isActive = true
            startPositionProcessing()
        }
        return success
    }

    /**
     * Stop cursor tracking
     */
    fun stopTracking() {
        if (!isActive) return

        isActive = false
        imuManager.stopIMUTracking(consumerId)
        scope.coroutineContext.cancelChildren()
    }

    /**
     * Update screen dimensions for cursor positioning
     */
    fun updateScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height

        // Center cursor and set reference points
        centerX = width / 2f
        centerY = height / 2f
        currentX = centerX
        currentY = centerY
    }

    /**
     * Set cursor sensitivity
     */
    fun setSensitivity(newSensitivity: Float) {
        sensitivity = newSensitivity.coerceIn(0.1f, 5.0f)
    }

    /**
     * Set cursor speed factor (displacement factor)
     */
    fun setSpeedFactor(newSpeedFactor: Int) {
        speedFactor = newSpeedFactor.coerceIn(1, 20)
    }

    /**
     * Center cursor and set neutral orientation
     */
    suspend fun centerCursor() {
        centerX = screenWidth / 2f
        centerY = screenHeight / 2f
        currentX = centerX
        currentY = centerY

        // Reset filters and previous values
        maYaw.clear()
        maPitch.clear()
        maRoll.clear()
        previousYaw = 0f
        previousPitch = 0f
        previousRoll = 0f

//        // Use current orientation as base
//        val currentOrientation = imuManager.getCurrentOrientation()
//        baseOrientation = currentOrientation?.quaternion ?: Quaternion.identity

        emitPosition()
    }

    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorPosition {
        return CursorPosition(currentX, currentY, System.nanoTime())
    }

    /**
     * Get IMU sensor capabilities
     */
    fun getSensorCapabilities(): SensorCapabilities {
        return imuManager.getSensorCapabilities() ?: SensorCapabilities(
            hasGameRotationVector = false,
            hasRotationVector = false,
            hasGyroscope = false,
            hasAccelerometer = false,
            hasMagnetometer = false,
            maxSampleRate = 0,
            resolution = 0.0f
        )
    }

    /**
     * Perform user calibration
     */
    suspend fun calibrate(): CalibrationResult {
        val result = imuManager.calibrateForUser()
        if (result.success) {
            baseOrientation = result.baseOrientation
        }
        return result
    }

    private fun startPositionProcessing() {
        scope.launch {
            imuManager.orientationFlow.collect { orientationData ->
                if (isActive) {
                    processOrientationForCursor(orientationData)
                }
            }
        }
    }

    // TODO: Need to add extra smoothing which is commented and added legacy algo to fix cursor movement issue
    private suspend fun processOrientationForCursor(imuData: IMUData) {
        // Skip processing if updates are too frequent
        val currentTime = System.nanoTime()
        if (currentTime - lastProcessTime < MIN_INTERVAL_NS) {
            return
        }
        lastProcessTime = currentTime

        val mAlpha = imuData.alpha
        val mBeta = imuData.beta
        val mGamma = imuData.gamma
        val ts = imuData.ts

        // Apply moving average filtering
        val alpha = maYaw.getAvg(mAlpha, ts)
        val beta = maPitch.getAvg(mBeta, ts)
        val gamma = if (mGamma < 0) -maRoll.getAvg(-mGamma, ts) else maRoll.getAvg(mGamma, ts)

        // Calculate deltas
        val dx = alpha - previousYaw
        val dy = beta - previousPitch
        val dz = gamma - previousRoll

        // Early return for tiny movements
        if (abs(dx) < RADIAN_TOLERANCE &&
            abs(dy) < RADIAN_TOLERANCE &&
            abs(dz) < RADIAN_TOLERANCE
        ) {
            return
        }

        // First-time initialization
        if (previousYaw == 0f && previousPitch == 0f && previousRoll == 0f) {
            previousYaw = alpha
            previousPitch = beta
            previousRoll = gamma
            return
        }

        // Update previous values
        previousYaw = alpha
        previousPitch = beta
        previousRoll = gamma

        // Calculate displacement efficiently
        val speedMultiplier = speedFactor * 0.2
        val disX = tan(dx.toDouble()) * screenWidth * CURSOR_SCALE_X +
                tan(dz.toDouble()) * screenWidth * CURSOR_SCALE_Z * speedMultiplier
        val disY = tan(dy.toDouble()) * screenHeight * CURSOR_SCALE_Y * speedMultiplier

        // Apply fine tuning
        val finalDisX = if (abs(disX) < (screenWidth / 100)) disX * 0.4 else disX
        val finalDisY = if (abs(disY) < (screenHeight / 100)) disY * 0.4 else disY

        // Significant movement check
        if (finalDisX != 0.0 || finalDisY != 0.0) {
            lastPoint.reset(currentX, currentY, ts)
        }

        // Update position with constraints
        currentX = (startX + finalDisX.toFloat()).coerceIn(0f, screenWidth.toFloat())
        currentY = (startY - finalDisY.toFloat()).coerceIn(0f, screenHeight.toFloat())

        // Calculate movement distance
        val deltaX = currentX - startX
        val deltaY = currentY - startY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

        startX = currentX
        startY = currentY

        if (distance > DISTANCE_TOLERANCE) {
            emitPosition()
        }

    }

    /*private suspend fun processOrientationForCursor(orientationData: OrientationData) {
        // Initialize on first orientation data if needed
        if (!isInitialized) {
            baseOrientation = orientationData.quaternion
            previousOrientation = orientationData.quaternion
            initializeCursorPosition()
            isInitialized = true
            lastMovementTime = System.currentTimeMillis()
            Log.d(TAG, "Cursor initialized with first orientation data at ($currentX, $currentY)")
            emitPosition()
            return
        }

        val currentTime = System.currentTimeMillis()

        // Calculate delta between current and previous orientation (delta-based processing)
        val deltaRotation = previousOrientation.inverse * orientationData.quaternion
        val deltaEuler = deltaRotation.toEulerAngles()

        Log.v(TAG, "Raw deltas - Yaw: ${deltaEuler.yaw}, Pitch: ${deltaEuler.pitch}, Roll: ${deltaEuler.roll}")

        // Apply dead zone to prevent jitter
        val filteredYaw = if (abs(deltaEuler.yaw) > deadZoneThreshold) deltaEuler.yaw else 0f
        val filteredPitch = if (abs(deltaEuler.pitch) > deadZoneThreshold) deltaEuler.pitch else 0f

        // Use tangent-based displacement like legacy implementation
        // tan(delta) * screenDimension * scale - this provides proper movement scaling
        val deltaX = tan(filteredYaw) * screenWidth * sensitivityX
        val deltaY = -tan(filteredPitch) * screenHeight * sensitivityY  // Negative for natural movement

        Log.v(TAG, "Calculated deltas - X: $deltaX, Y: $deltaY")

        // Store old position for movement detection
        val oldX = currentX
        val oldY = currentY

        // Apply incremental movement (cumulative positioning)
        currentX = (currentX + deltaX).coerceIn(0f, screenWidth.toFloat())
        currentY = (currentY + deltaY).coerceIn(0f, screenHeight.toFloat())

        // Check for actual movement
        val moved = abs(currentX - oldX) > 0.1f || abs(currentY - oldY) > 0.1f
        if (moved) {
            lastMovementTime = currentTime
            Log.v(TAG, "Cursor moved from ($oldX, $oldY) to ($currentX, $currentY)")
        }

        // Detect stuck cursor and trigger recalibration
        if (currentTime - lastMovementTime > stuckDetectionThreshold) {
            Log.w(TAG, "Cursor appears stuck, triggering recalibration")
            centerCursor()
            return
        }

        // Update previous orientation for next delta calculation
        previousOrientation = orientationData.quaternion

        emitPosition()
    }*/

    private suspend fun emitPosition() {
        _positionFlow.emit(
            CursorPosition(
                x = currentX,
                y = currentY,
                timestamp = System.nanoTime()
            )
        )
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        stopTracking()
        scope.cancel()
    }

    private inner class LastPoint {
        var x = 0f
        var y = 0f
        var ts // the time this position got changed.
                : Long = 0

        fun reset(x: Float, y: Float, current: Long) {
            if (CURSOR_DELAY_TIME == 0L) return
            if (ts == 0L || current - ts > CURSOR_DELAY_TIME) {
                this.x = x
                this.y = y
                ts = current
            }
        }
    }
}

/**
 * Cursor position data class
 */
data class CursorPosition(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

/**
 * Direct usage example for cursor applications
 *
 * Usage:
 * ```
 * val cursorAdapter = CursorAdapter(context, "MyApp")
 * cursorAdapter.startTracking()
 *
 * // Modern position-based updates
 * cursorAdapter.positionFlow.collect { position ->
 *     cursorView.updateCursorPosition(position.x, position.y, position.timestamp)
 * }
 *
 * // Legacy orientation-based updates (if needed)
 * cursorAdapter.imuManager.orientationFlow.collect { data ->
 *     val euler = data.quaternion.toEulerAngles()
 *     cursorView.setOrientation(euler.yaw, euler.pitch, euler.roll, data.timestamp, false)
 * }
 * ```
 */