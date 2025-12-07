/**
 * VoiceCursor.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/VoiceCursor.kt
 * 
 * Created: 2025-01-26 00:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Main entry point for VoiceCursor functionality
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-26 00:00 PST): Initial creation for VOS4 module
 */

package com.augmentalis.voiceos.cursor

import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceos.cursor.core.*
import com.augmentalis.voiceos.cursor.service.VoiceCursorOverlayService
import com.augmentalis.voiceos.cursor.helper.VoiceCursorIMUIntegration
import com.augmentalis.voiceos.cursor.integration.VoiceAccessibilityIntegration
import kotlinx.coroutines.*

/**
 * Main VoiceCursor class following VOS4 direct implementation pattern
 * No interfaces - direct implementation only
 */
class VoiceCursor(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceCursor"
        private var instance: VoiceCursor? = null
        
        fun getInstance(context: Context): VoiceCursor {
            return instance ?: synchronized(this) {
                instance ?: VoiceCursor(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    // Configuration and state
    private var cursorConfig = CursorConfig()
    private var isEnabled = false
    private var currentPosition = CursorOffset(0f, 0f)
    
    // Integration components
    private var imuIntegration: VoiceCursorIMUIntegration? = null
    private var voiceIntegration: VoiceAccessibilityIntegration? = null
    
    // Coroutine scope for async operations
    private val cursorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Initialize VoiceCursor with configuration
     */
    fun initialize(config: CursorConfig = CursorConfig()) {
        Log.d(TAG, "Initializing VoiceCursor with config: $config")
        this.cursorConfig = config
        
        // Initialize IMU integration with DeviceManager
        imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
            setOnPositionUpdate { position ->
                currentPosition = position
            }
        }
        
        // Initialize voice command integration
        initializeVoiceIntegration()
    }
    
    /**
     * Initialize voice command integration
     */
    private fun initializeVoiceIntegration() {
        cursorScope.launch {
            try {
                voiceIntegration = VoiceAccessibilityIntegration.getInstance(context)
                if (voiceIntegration?.initialize() == true) {
                    voiceIntegration?.registerCommands()
                    Log.d(TAG, "Voice integration initialized successfully")
                } else {
                    Log.w(TAG, "Failed to initialize voice integration")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing voice integration", e)
            }
        }
    }
    
    /**
     * Start cursor overlay service
     */
    fun startCursor(): Boolean {
        return try {
            Log.d(TAG, "Starting cursor with config: $cursorConfig")
            val serviceIntent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                putExtra("cursor_config", cursorConfig)
            }
            context.startForegroundService(serviceIntent)
            isEnabled = true
            
            // Start IMU tracking
            imuIntegration?.start()
            
            Log.d(TAG, "Cursor service started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start cursor service", e)
            false
        }
    }
    
    /**
     * Stop cursor overlay service
     */
    fun stopCursor(): Boolean {
        return try {
            Log.d(TAG, "Stopping cursor service")
            val serviceIntent = Intent(context, VoiceCursorOverlayService::class.java)
            context.stopService(serviceIntent)
            isEnabled = false
            
            // Stop IMU tracking
            imuIntegration?.stop()
            
            Log.d(TAG, "Cursor service stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop cursor service", e)
            false
        }
    }
    
    /**
     * Update cursor configuration
     */
    fun updateConfig(config: CursorConfig) {
        Log.d(TAG, "Updating cursor config: $config")
        this.cursorConfig = config
        
        // Update service if running
        if (isEnabled) {
            val serviceIntent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                putExtra("action", "update_config")
                putExtra("config", config)
            }
            context.startService(serviceIntent)
            Log.d(TAG, "Sent config update to service")
        }
        
        // Update IMU sensitivity based on speed
        imuIntegration?.setSensitivity(config.speed / 10.0f)
        Log.d(TAG, "Updated IMU sensitivity to: ${config.speed / 10.0f}")
    }
    
    /**
     * Toggle cursor type between Hand and Normal
     */
    fun toggleCursorType() {
        val newConfig = cursorConfig.copy(type = cursorConfig.type.toggle())
        updateConfig(newConfig)
    }
    
    /**
     * Center cursor on screen
     */
    fun centerCursor() {
        if (isEnabled) {
            val serviceIntent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                putExtra("action", "center_cursor")
            }
            context.startService(serviceIntent)
        }
    }
    
    /**
     * Show cursor
     */
    fun showCursor() {
        if (isEnabled) {
            val serviceIntent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                putExtra("action", "show_cursor")
            }
            context.startService(serviceIntent)
        }
    }
    
    /**
     * Hide cursor
     */
    fun hideCursor() {
        if (isEnabled) {
            val serviceIntent = Intent(context, VoiceCursorOverlayService::class.java).apply {
                putExtra("action", "hide_cursor")
            }
            context.startService(serviceIntent)
        }
    }
    
    /**
     * Get current configuration
     */
    fun getConfig(): CursorConfig = cursorConfig
    
    /**
     * Get current configuration (alternative method name)
     */
    fun getCurrentConfig(): CursorConfig = cursorConfig
    
    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorOffset = currentPosition
    
    /**
     * Update cursor position
     */
    fun updatePosition(position: CursorOffset) {
        currentPosition = position
        Log.d(TAG, "Cursor position updated: $position")
    }
    
    /**
     * Calibrate cursor tracking system
     */
    suspend fun calibrate(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val success = imuIntegration?.calibrate() ?: false
                Log.d(TAG, if (success) "Calibration successful" else "Calibration failed")
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error during calibration", e)
                false
            }
        }
    }
    
    /**
     * Check if cursor is enabled
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Get voice integration status
     */
    fun isVoiceIntegrationReady(): Boolean {
        return voiceIntegration?.isReady() ?: false
    }
    
    /**
     * Process voice command directly
     */
    suspend fun processVoiceCommand(command: String): Boolean {
        return voiceIntegration?.processVoiceCommand(command) ?: false
    }
    
    /**
     * Get supported voice commands
     */
    fun getSupportedVoiceCommands(): List<String> {
        return voiceIntegration?.getCommandPatterns() ?: emptyList()
    }
    
    /**
     * Cleanup resources
     */
    fun dispose() {
        Log.d(TAG, "Disposing VoiceCursor resources")
        
        // Stop cursor system
        stopCursor()
        
        // Clean up voice integration
        voiceIntegration?.shutdown()
        voiceIntegration = null
        
        // Clean up IMU integration
        imuIntegration?.dispose()
        imuIntegration = null
        
        // Cancel coroutines
        cursorScope.cancel()
        
        // Clear instance
        instance = null
        
        Log.d(TAG, "VoiceCursor disposed successfully")
    }
}