/**
 * GazeHandler.kt - Gaze tracking and interaction handler
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-03
 * 
 * Purpose: Migrated from Legacy Avenue GazeActions.kt
 * Handles gaze tracking, auto-click on dwell, calibration, and voice commands
 * Integrates with HUDManager/GazeTracker and VoiceCursor/GazeClickManager
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.util.Log
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import com.augmentalis.hudmanager.HUDManager
import com.augmentalis.hudmanager.spatial.GazeTracker
import com.augmentalis.hudmanager.models.GazeTarget
import com.augmentalis.hudmanager.models.Vector3D
import com.augmentalis.hudmanager.spatial.UIElement
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Handles gaze tracking and gaze-based interactions
 * Direct implementation following VOS4 patterns
 * 
 * MIGRATION NOTE: Maintains 100% Legacy Avenue functionality:
 * - GAZE_ON/GAZE_OFF commands
 * - Cursor visibility integration
 * - Enhanced with modern gaze tracking via HUDManager
 */
class GazeHandler(
    private val service: VoiceAccessibilityService
) : ActionHandler {
    
    companion object {
        private const val TAG = "GazeHandler"
        private const val DWELL_TIME_MS = 1500L
        private const val CALIBRATION_THRESHOLD = 0.8f
        private const val GAZE_STABILITY_WINDOW_MS = 300L
    }
    
    // Core gaze system integration
    private var gazeTracker: GazeTracker? = null
    private var hudManager: HUDManager? = null
    private var isGazeEnabled = false
    private var isDwellClickEnabled = true
    private var isCalibrationMode = false
    
    // Coroutine management
    private val gazeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dwellClickJob: Job? = null
    private var gazeMonitorJob: Job? = null
    
    // Gaze state tracking
    private var currentTarget: GazeTarget? = null
    private var dwellStartTime: Long = 0
    private var lastStableGaze: GazeTarget? = null
    private var gazeStabilityStartTime: Long = 0
    
    // Performance metrics
    private var clickSuccessCount = 0
    private var totalClickAttempts = 0
    private var averageGazeAccuracy = 0.85f
    
    override fun initialize() {
        Log.d(TAG, "Initializing GazeHandler")
        
        try {
            // Get HUDManager instance for gaze tracking
            hudManager = HUDManager.getInstance(service)
            gazeTracker = hudManager?.gazeTracker
            
            if (gazeTracker?.initialize() == true) {
                Log.i(TAG, "GazeTracker initialized successfully")
            } else {
                Log.w(TAG, "GazeTracker initialization failed - gaze features will be limited")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize gaze systems", e)
        }
    }
    
    override fun canHandle(action: String): Boolean {
        return when (action.lowercase()) {
            // Legacy Avenue compatibility
            "gaze_on", "gaze_off" -> true
            
            // Enhanced gaze commands
            "enable_gaze", "disable_gaze" -> true
            "gaze_click", "dwell_click" -> true
            "gaze_calibrate", "calibrate_gaze" -> true
            "gaze_center", "center_gaze" -> true
            "toggle_dwell", "dwell_toggle" -> true
            "gaze_reset", "reset_gaze" -> true
            
            // Voice-gaze fusion commands
            "look_and_click", "gaze_tap" -> true
            "where_am_i_looking", "gaze_status" -> true
            "gaze_help" -> true
            
            else -> false
        }
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        Log.d(TAG, "Executing gaze action: $action")
        
        return try {
            when (action.lowercase()) {
                // Legacy Avenue compatibility - CRITICAL for migration
                "gaze_on" -> handleGazeOn()
                "gaze_off" -> handleGazeOff()
                
                // Enhanced gaze commands
                "enable_gaze" -> handleGazeOn()
                "disable_gaze" -> handleGazeOff()
                "gaze_click", "dwell_click" -> handleGazeClick(params)
                "gaze_calibrate", "calibrate_gaze" -> handleGazeCalibration(params)
                "gaze_center", "center_gaze" -> handleGazeCenter()
                "toggle_dwell", "dwell_toggle" -> handleToggleDwell()
                "gaze_reset", "reset_gaze" -> handleGazeReset()
                
                // Voice-gaze fusion commands
                "look_and_click", "gaze_tap" -> handleLookAndClick(params)
                "where_am_i_looking", "gaze_status" -> handleGazeStatus()
                "gaze_help" -> handleGazeHelp()
                
                else -> {
                    Log.w(TAG, "Unhandled gaze action: $action")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing gaze action: $action", e)
            false
        }
    }
    
    override fun getSupportedActions(): List<String> {
        return listOf(
            // Legacy Avenue compatibility
            "gaze_on", "gaze_off",
            
            // Enhanced gaze commands
            "enable_gaze", "disable_gaze",
            "gaze_click", "dwell_click",
            "gaze_calibrate", "calibrate_gaze", 
            "gaze_center", "center_gaze",
            "toggle_dwell", "dwell_toggle",
            "gaze_reset", "reset_gaze",
            
            // Voice-gaze fusion
            "look_and_click", "gaze_tap",
            "where_am_i_looking", "gaze_status",
            "gaze_help"
        )
    }
    
    /**
     * LEGACY AVENUE COMPATIBILITY: Enable gaze tracking
     * Must check cursor visibility like original implementation
     */
    private fun handleGazeOn(): Boolean {
        Log.d(TAG, "Enabling gaze tracking")
        
        // Legacy compatibility: Check cursor visibility
        if (!isCursorVisible()) {
            Log.w(TAG, "Gaze cannot be enabled - cursor not visible")
            return false
        }
        
        return try {
            // Start gaze tracking
            gazeScope.launch {
                gazeTracker?.startTracking()
            }
            
            isGazeEnabled = true
            startGazeMonitoring()
            
            // Integrate with VoiceCursor if available
            enableCursorGaze()
            
            Log.i(TAG, "Gaze tracking enabled successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable gaze tracking", e)
            false
        }
    }
    
    /**
     * LEGACY AVENUE COMPATIBILITY: Disable gaze tracking
     */
    private fun handleGazeOff(): Boolean {
        Log.d(TAG, "Disabling gaze tracking")
        
        // Legacy compatibility: Check cursor visibility
        if (!isCursorVisible()) {
            Log.w(TAG, "Gaze already disabled - cursor not visible")
            return false
        }
        
        return try {
            // Stop gaze tracking
            gazeTracker?.stopTracking()
            isGazeEnabled = false
            
            // Stop monitoring jobs
            stopGazeMonitoring()
            
            // Disable cursor gaze integration
            disableCursorGaze()
            
            Log.i(TAG, "Gaze tracking disabled successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable gaze tracking", e)
            false
        }
    }
    
    /**
     * Handle gaze-based clicking
     */
    private fun handleGazeClick(params: Map<String, Any>): Boolean {
        if (!isGazeEnabled) {
            Log.w(TAG, "Gaze click attempted but gaze not enabled")
            return false
        }
        
        val forceClick = params["force"] as? Boolean ?: false
        
        return gazeScope.async {
            val target = getCurrentGazeTarget()
            if (target != null && (target.confidence >= CALIBRATION_THRESHOLD || forceClick)) {
                performGazeClick(target)
            } else {
                Log.w(TAG, "Gaze click failed - low confidence or no target")
                false
            }
        }.let { 
            runBlocking { it.await() }
        }
    }
    
    /**
     * Handle gaze calibration
     */
    private fun handleGazeCalibration(params: Map<String, Any>): Boolean {
        Log.d(TAG, "Starting gaze calibration")
        
        isCalibrationMode = true
        
        return try {
            // Get calibration target from parameters
            val targetX = params["targetX"] as? Float
            val targetY = params["targetY"] as? Float
            
            if (targetX != null && targetY != null) {
                performCalibration(targetX, targetY)
            } else {
                // Start interactive calibration
                startInteractiveCalibration()
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gaze calibration failed", e)
            isCalibrationMode = false
            false
        }
    }
    
    /**
     * Center gaze tracking
     */
    private fun handleGazeCenter(): Boolean {
        Log.d(TAG, "Centering gaze tracking")
        
        return try {
            // Reset gaze to screen center
            val screenCenterX = 0.5f
            val screenCenterY = 0.5f
            
            gazeTracker?.calibrateGaze(
                createUIElement(screenCenterX, screenCenterY, "center"),
                GazeTarget("screen_center", Vector3D(0f, 0f, 0f), 1.0f, 0L)
            )
            
            Log.i(TAG, "Gaze centered successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to center gaze", e)
            false
        }
    }
    
    /**
     * Toggle dwell click functionality
     */
    private fun handleToggleDwell(): Boolean {
        isDwellClickEnabled = !isDwellClickEnabled
        Log.i(TAG, "Dwell click ${if (isDwellClickEnabled) "enabled" else "disabled"}")
        
        if (!isDwellClickEnabled) {
            stopDwellClick()
        }
        
        return true
    }
    
    /**
     * Reset gaze tracking system
     */
    private fun handleGazeReset(): Boolean {
        Log.d(TAG, "Resetting gaze tracking system")
        
        return try {
            // Stop current tracking
            gazeTracker?.stopTracking()
            stopGazeMonitoring()
            
            // Reset state
            currentTarget = null
            lastStableGaze = null
            dwellStartTime = 0
            gazeStabilityStartTime = 0
            isCalibrationMode = false
            
            // Restart if was enabled
            if (isGazeEnabled) {
                gazeScope.launch {
                    gazeTracker?.startTracking()
                }
                startGazeMonitoring()
            }
            
            Log.i(TAG, "Gaze tracking system reset successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset gaze tracking", e)
            false
        }
    }
    
    /**
     * Look and click - immediate gaze-based click
     */
    private fun handleLookAndClick(@Suppress("UNUSED_PARAMETER") params: Map<String, Any>): Boolean {
        if (!isGazeEnabled) {
            Log.w(TAG, "Look and click attempted but gaze not enabled")
            return false
        }
        
        return gazeScope.async {
            val target = getCurrentGazeTarget()
            if (target != null) {
                Log.d(TAG, "Look and click at gaze target: (${target.position.x}, ${target.position.y})")
                performGazeClick(target)
            } else {
                Log.w(TAG, "Look and click failed - no gaze target")
                false
            }
        }.let {
            runBlocking { it.await() }
        }
    }
    
    /**
     * Get current gaze status
     */
    private fun handleGazeStatus(): Boolean {
        val target = currentTarget
        val status = buildString {
            appendLine("Gaze Status:")
            appendLine("- Enabled: $isGazeEnabled")
            appendLine("- Dwell Click: $isDwellClickEnabled")
            appendLine("- Calibration Mode: $isCalibrationMode")
            if (target != null) {
                appendLine("- Current Target: (${target.position.x}, ${target.position.y})")
                appendLine("- Confidence: ${(target.confidence * 100).toInt()}%")
                appendLine("- Age: ${target.dwellTime}ms")
            } else {
                appendLine("- Current Target: None")
            }
            appendLine("- Average Accuracy: ${(averageGazeAccuracy * 100).toInt()}%")
            appendLine("- Click Success Rate: ${if (totalClickAttempts > 0) (clickSuccessCount * 100 / totalClickAttempts) else 0}%")
        }
        
        Log.i(TAG, status)
        return true
    }
    
    /**
     * Show gaze help
     */
    private fun handleGazeHelp(): Boolean {
        val help = """
            Gaze Commands:
            • "gaze on" / "enable gaze" - Enable gaze tracking
            • "gaze off" / "disable gaze" - Disable gaze tracking  
            • "gaze click" / "dwell click" - Click at gaze position
            • "look and click" / "gaze tap" - Immediate gaze click
            • "calibrate gaze" - Start gaze calibration
            • "center gaze" - Reset gaze to center
            • "toggle dwell" - Enable/disable auto-click on dwell
            • "gaze status" - Show current gaze information
            • "reset gaze" - Reset gaze tracking system
        """.trimIndent()
        
        Log.i(TAG, help)
        return true
    }
    
    /**
     * Start monitoring gaze for dwell clicks
     */
    private fun startGazeMonitoring() {
        gazeMonitorJob?.cancel()
        gazeMonitorJob = gazeScope.launch {
            while (isActive && isGazeEnabled) {
                try {
                    monitorGazeForDwell()
                    delay(50) // 20 FPS monitoring
                } catch (e: Exception) {
                    Log.e(TAG, "Error in gaze monitoring", e)
                }
            }
        }
    }
    
    /**
     * Stop gaze monitoring
     */
    private fun stopGazeMonitoring() {
        gazeMonitorJob?.cancel()
        gazeMonitorJob = null
        stopDwellClick()
    }
    
    /**
     * Monitor gaze for dwell-based clicking
     */
    private suspend fun monitorGazeForDwell() {
        val target = getCurrentGazeTarget() ?: return
        
        // Update cursor position if gaze cursor integration is enabled
        if (isGazeCursorEnabled) {
            val cursorManager = service.getCursorManager()
            if (cursorManager != null) {
                // Convert normalized gaze coordinates to screen coordinates
                val screenX = (target.position.x + 1f) * 0.5f * getScreenWidth()
                val screenY = (target.position.y + 1f) * 0.5f * getScreenHeight()
                // Move cursor to current gaze position
                cursorManager.moveToPosition(screenX.toInt(), screenY.toInt())
            }
        }
        
        // Check gaze stability
        if (isGazeStable(target)) {
            if (dwellStartTime == 0L) {
                dwellStartTime = System.currentTimeMillis()
                Log.d(TAG, "Dwell started at (${target.position.x}, ${target.position.y})")
            } else {
                val dwellTime = System.currentTimeMillis() - dwellStartTime
                if (dwellTime >= DWELL_TIME_MS && isDwellClickEnabled) {
                    Log.d(TAG, "Dwell time reached, performing click")
                    performGazeClick(target)
                    dwellStartTime = 0L // Reset dwell timer
                }
            }
        } else {
            // Gaze moved, reset dwell timer
            if (dwellStartTime != 0L) {
                Log.d(TAG, "Gaze moved, resetting dwell timer")
                dwellStartTime = 0L
            }
        }
    }
    
    /**
     * Check if gaze is stable enough for dwell click
     */
    private fun isGazeStable(target: GazeTarget): Boolean {
        val lastGaze = lastStableGaze
        
        if (lastGaze == null) {
            lastStableGaze = target
            gazeStabilityStartTime = System.currentTimeMillis()
            return false
        }
        
        // Calculate distance from last stable position
        val deltaX = target.position.x - lastGaze.position.x
        val deltaY = target.position.y - lastGaze.position.y
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
        
        val isStable = distance < 0.05f && target.confidence > CALIBRATION_THRESHOLD
        
        if (isStable) {
            val stabilityTime = System.currentTimeMillis() - gazeStabilityStartTime
            return stabilityTime >= GAZE_STABILITY_WINDOW_MS
        } else {
            lastStableGaze = target
            gazeStabilityStartTime = System.currentTimeMillis()
            return false
        }
    }
    
    /**
     * Get current gaze target from HUDManager
     */
    private suspend fun getCurrentGazeTarget(): GazeTarget? {
        return try {
            currentTarget = gazeTracker?.getCurrentTarget()
            currentTarget
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current gaze target", e)
            null
        }
    }
    
    /**
     * Perform click at gaze target position
     */
    private suspend fun performGazeClick(target: GazeTarget): Boolean {
        totalClickAttempts++
        
        return try {
            // Convert normalized gaze coordinates to screen coordinates
            val screenX = (target.position.x + 1f) * 0.5f * getScreenWidth()
            val screenY = (target.position.y + 1f) * 0.5f * getScreenHeight()
            
            Log.d(TAG, "Performing gaze click at screen coordinates ($screenX, $screenY)")
            
            // If gaze cursor integration is enabled, update cursor position and click
            val success = if (isGazeCursorEnabled) {
                val cursorManager = service.getCursorManager()
                if (cursorManager != null) {
                    // Move cursor to gaze position
                    cursorManager.moveToPosition(screenX.toInt(), screenY.toInt())
                    // Click at cursor position
                    cursorManager.clickAtCursor()
                } else {
                    // Fallback to direct click
                    service.performClick(screenX, screenY)
                }
            } else {
                // Use VoiceAccessibilityService gesture dispatch
                service.performClick(screenX, screenY)
            }
            
            if (success) {
                clickSuccessCount++
                Log.i(TAG, "Gaze click successful")
            } else {
                Log.w(TAG, "Gaze click failed")
            }
            
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing gaze click", e)
            false
        }
    }
    
    /**
     * Perform gaze calibration with target position
     */
    private fun performCalibration(targetX: Float, targetY: Float) {
        gazeScope.launch {
            val currentGaze = getCurrentGazeTarget()
            if (currentGaze != null) {
                val targetElement = createUIElement(targetX, targetY, "calibration_target")
                gazeTracker?.calibrateGaze(targetElement, currentGaze)
                
                Log.i(TAG, "Gaze calibration performed for target ($targetX, $targetY)")
            }
        }
    }
    
    /**
     * Start interactive calibration process
     */
    private fun startInteractiveCalibration() {
        Log.i(TAG, "Starting interactive gaze calibration")
        // This would typically show calibration UI
        // For now, calibrate to screen center
        performCalibration(0.5f, 0.5f)
        isCalibrationMode = false
    }
    
    /**
     * Stop current dwell click operation
     */
    private fun stopDwellClick() {
        dwellClickJob?.cancel()
        dwellClickJob = null
        dwellStartTime = 0L
    }
    
    /**
     * Check if cursor is visible (Legacy Avenue compatibility)
     */
    private fun isCursorVisible(): Boolean {
        return try {
            // Try to get cursor manager from service
            val cursorManager = service.getCursorManager()
            cursorManager?.isCursorVisible() ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Could not check cursor visibility", e)
            // Default to true for gaze functionality
            true
        }
    }
    
    /**
     * Enable gaze in VoiceCursor integration
     */
    private fun enableCursorGaze() {
        try {
            val cursorManager = service.getCursorManager()
            if (cursorManager != null) {
                // Show cursor to enable gaze tracking
                cursorManager.showCursor()
                isGazeCursorEnabled = true
                Log.d(TAG, "Cursor gaze integration enabled")
            } else {
                Log.w(TAG, "CursorManager not available for gaze integration")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not enable cursor gaze integration", e)
        }
    }
    
    /**
     * Disable gaze in VoiceCursor integration
     */
    private fun disableCursorGaze() {
        try {
            val cursorManager = service.getCursorManager()
            if (cursorManager != null) {
                // Hide cursor to disable gaze tracking
                cursorManager.hideCursor()
                isGazeCursorEnabled = false
                Log.d(TAG, "Cursor gaze integration disabled")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not disable cursor gaze integration", e)
        }
    }
    
    // Flag to track if gaze cursor integration is enabled
    private var isGazeCursorEnabled = false
    
    /**
     * Helper to create UIElement for calibration
     */
    private fun createUIElement(x: Float, y: Float, id: String): UIElement {
        return UIElement(
            id = id,
            bounds = com.augmentalis.hudmanager.spatial.ElementBounds(
                left = x - 0.02f,
                top = y - 0.02f, 
                right = x + 0.02f,
                bottom = y + 0.02f
            ),
            center = com.augmentalis.hudmanager.spatial.Point2D(x, y),
            type = com.augmentalis.hudmanager.spatial.UIElementType.BUTTON
        )
    }
    
    /**
     * Get screen width in pixels
     */
    private fun getScreenWidth(): Float {
        return try {
            val displayMetrics = service.resources.displayMetrics
            displayMetrics.widthPixels.toFloat()
        } catch (e: Exception) {
            1080f // Default fallback
        }
    }
    
    /**
     * Get screen height in pixels
     */
    private fun getScreenHeight(): Float {
        return try {
            val displayMetrics = service.resources.displayMetrics
            displayMetrics.heightPixels.toFloat()
        } catch (e: Exception) {
            1920f // Default fallback
        }
    }
    
    override fun dispose() {
        Log.d(TAG, "Disposing GazeHandler")
        
        try {
            // Stop gaze tracking
            isGazeEnabled = false
            gazeTracker?.stopTracking()
            
            // Cancel all coroutines
            gazeScope.cancel()
            
            // Dispose gaze tracker
            gazeTracker?.dispose()
            
            // Reset state
            currentTarget = null
            lastStableGaze = null
            hudManager = null
            gazeTracker = null
            
            Log.i(TAG, "GazeHandler disposed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing GazeHandler", e)
        }
    }
}