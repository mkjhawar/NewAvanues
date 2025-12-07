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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Simplified interface for cursor applications to consume IMU data
 */
class CursorAdapter(
    private val context: Context,
    private val consumerId: String = "CursorApp"
) {
    
    val imuManager = IMUManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Cursor-specific processing
    private var baseOrientation = Quaternion.identity
    private var screenWidth = 1920
    private var screenHeight = 1080
    private var sensitivity = 1.0f
    
    // Cursor position tracking
    private var currentX = 0f
    private var currentY = 0f
    
    // Flow for cursor position updates
    private val _positionFlow = MutableSharedFlow<CursorPosition>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val positionFlow: SharedFlow<CursorPosition> = _positionFlow.asSharedFlow()
    
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
        
        // Center cursor
        currentX = width / 2f
        currentY = height / 2f
    }
    
    /**
     * Set cursor sensitivity
     */
    fun setSensitivity(newSensitivity: Float) {
        sensitivity = newSensitivity.coerceIn(0.1f, 5.0f)
    }
    
    /**
     * Center cursor and set neutral orientation
     */
    suspend fun centerCursor() {
        currentX = screenWidth / 2f
        currentY = screenHeight / 2f
        
        // Use current orientation as base
        val currentOrientation = imuManager.getCurrentOrientation()
        baseOrientation = currentOrientation?.quaternion ?: Quaternion.identity
        
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
    
    private suspend fun processOrientationForCursor(orientationData: OrientationData) {
        // Calculate relative rotation from base orientation
        val relativeRotation = baseOrientation.inverse * orientationData.quaternion
        val euler = relativeRotation.toEulerAngles()
        
        // Convert to screen coordinates with proper sensitivity scaling
        // Increased multiplier from 0.3f to 1.2f for better full-screen movement
        val deltaX = euler.yaw * sensitivity * screenWidth * 1.2f
        val deltaY = -euler.pitch * sensitivity * screenHeight * 1.2f
        
        // Apply movement incrementally rather than from center
        // This allows cumulative movement to reach all screen edges
        currentX = (currentX + (deltaX * 0.1f)).coerceIn(0f, screenWidth.toFloat())
        currentY = (currentY + (deltaY * 0.1f)).coerceIn(0f, screenHeight.toFloat())
        
        emitPosition()
    }
    
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