/**
 * VoiceCursor.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/VoiceCursor.kt
 *
 * Created: 2025-01-26 00:00 PST
 * Last Modified: 2025-10-26 02:50 PDT
 * Author: VOS4 Development Team
 * Version: 1.2.0 (LEGACY - Deprecated Methods Removed)
 *
 * **MIGRATION NOTICE:**
 * This class is in LEGACY mode. New implementations should use VoiceCursorAPI directly.
 *
 * **Recommended Usage:**
 * - For cursor mechanics: Use VoiceCursorAPI (com.augmentalis.voiceos.cursor.VoiceCursorAPI)
 * - For voice commands: Use CommandManager (com.augmentalis.commandmanager.handlers.CursorCommandHandler)
 *
 * **What Changed (2025-10-10):**
 * - Command handling logic moved to CommandManager module
 * - Voice integration removed from VoiceCursor (use CommandManager instead)
 * - VoiceCursor now focuses solely on cursor mechanics
 *
 * Purpose: Main entry point for VoiceCursor functionality (LEGACY SUPPORT)
 * Module: VoiceCursor System
 *
 * Changelog:
 * - v1.2.0 (2025-10-26): Removed 6 deprecated methods (startCursor, stopCursor, updateConfig, centerCursor, showCursor, hideCursor) - no callers found
 * - v1.1.0 (2025-10-10): Marked as LEGACY, command logic extracted to CommandManager
 * - v1.0.0 (2025-01-26 00:00 PST): Initial creation for VOS4 module
 */

package com.augmentalis.voiceos.cursor

import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceos.cursor.core.*
import com.augmentalis.voiceos.cursor.helper.VoiceCursorIMUIntegration
import kotlinx.coroutines.*

/**
 * Main VoiceCursor class following VOS4 direct implementation pattern
 * No interfaces - direct implementation only
 *
 * **LEGACY CLASS:**
 * This class is maintained for backward compatibility only.
 * New implementations should use VoiceCursorAPI for cursor mechanics
 * and CommandManager for voice command handling.
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


        Log.d(TAG, "VoiceCursor initialization complete - voice integration handled by CommandManager")
    }
    
    /**
     * Toggle cursor type between Hand and Normal
     */
    fun toggleCursorType() {
        val newConfig = cursorConfig.copy(type = cursorConfig.type.toggle())
        VoiceCursorAPI.updateConfiguration(newConfig)
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
     * Cleanup resources
     */
    fun dispose() {
        Log.d(TAG, "Disposing VoiceCursor resources")

        // Stop cursor system
        VoiceCursorAPI.hideCursor()

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