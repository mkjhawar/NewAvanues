/**
 * LifecycleCoordinator.kt - Service lifecycle and foreground service management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-22
 *
 * P2-8d: Extracts lifecycle management from VoiceOSService.
 * Manages hybrid foreground service approach and app lifecycle observation.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Lifecycle Coordinator
 *
 * Manages VoiceOSService lifecycle concerns:
 * - Hybrid foreground service (Android 12+ background mic access)
 * - App foreground/background state tracking
 * - ProcessLifecycleOwner observation
 * - Foreground service start/stop optimization
 *
 * Hybrid approach: Only runs foreground service on Android 12+ when:
 * - App is in background AND
 * - Voice session is active
 *
 * @param context Service context for foreground service operations
 */
class LifecycleCoordinator(
    private val context: Context
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "LifecycleCoordinator"
        private const val ACTION_START_MIC = "com.augmentalis.voiceos.START_MIC"
        private const val ACTION_STOP_MIC = "com.augmentalis.voiceos.STOP_MIC"
    }

    // Foreground service state
    @Volatile
    private var foregroundServiceActive = false

    // App lifecycle state
    @Volatile
    private var appInBackground = false

    // Voice session state (set externally)
    @Volatile
    private var voiceSessionActive = false

    // ============================================================
    // Lifecycle Registration
    // ============================================================

    /**
     * Register with ProcessLifecycleOwner
     *
     * Call from VoiceOSService.onServiceConnected()
     */
    fun register() {
        try {
            Log.d(TAG, "Registering with ProcessLifecycleOwner...")
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            Log.i(TAG, "✓ Registered with ProcessLifecycleOwner for lifecycle events")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering lifecycle observer", e)
        }
    }

    /**
     * Unregister from ProcessLifecycleOwner
     *
     * Call from VoiceOSService.onDestroy()
     * Prevents memory leak (Leak signature: bd0178976084c8549ea1a5e0417e0d6ffe34eaa3)
     */
    fun unregister() {
        try {
            Log.d(TAG, "Unregistering from ProcessLifecycleOwner...")
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            Log.i(TAG, "✓ ProcessLifecycleOwner observer unregistered (memory leak fixed)")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error unregistering lifecycle observer", e)
        }
    }

    // ============================================================
    // Lifecycle Observer Callbacks
    // ============================================================

    /**
     * Called when app moves to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App moved to foreground")
        appInBackground = false
        evaluateForegroundServiceNeed()
    }

    /**
     * Called when app moves to background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App moved to background")
        appInBackground = true
        evaluateForegroundServiceNeed()
    }

    // ============================================================
    // Voice Session State
    // ============================================================

    /**
     * Update voice session state
     *
     * Call when voice recognition starts/stops
     *
     * @param active true if voice session is active
     */
    fun setVoiceSessionActive(active: Boolean) {
        if (voiceSessionActive != active) {
            Log.d(TAG, "Voice session state changed: $active")
            voiceSessionActive = active
            evaluateForegroundServiceNeed()
        }
    }

    // ============================================================
    // Foreground Service Management (Hybrid Approach)
    // ============================================================

    /**
     * Evaluate whether ForegroundService is needed
     *
     * Hybrid approach - only starts ForegroundService on Android 12+ when:
     * - App is in background AND
     * - Voice session is active
     *
     * This minimizes battery drain and memory usage while maintaining
     * background mic access compliance.
     */
    private fun evaluateForegroundServiceNeed() {
        val needsForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && appInBackground
                && voiceSessionActive
                && !foregroundServiceActive

        val shouldStopForeground = foregroundServiceActive && (!appInBackground || !voiceSessionActive)

        when {
            needsForeground -> {
                Log.d(TAG, "Starting ForegroundService (Android 12+ background requirement)")
                startForegroundService()
            }

            shouldStopForeground -> {
                Log.d(TAG, "Stopping ForegroundService (no longer needed)")
                stopForegroundService()
            }

            else -> {
                Log.v(TAG, "ForegroundService state: needed=$needsForeground, active=$foregroundServiceActive")
            }
        }
    }

    /**
     * Start foreground service when needed
     *
     * Called automatically by evaluateForegroundServiceNeed()
     */
    private fun startForegroundService() {
        if (foregroundServiceActive) return

        try {
            val intent = Intent(context, context.javaClass).apply {
                action = ACTION_START_MIC
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            foregroundServiceActive = true
            Log.i(TAG, "ForegroundService started for background mic access")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ForegroundService", e)
            foregroundServiceActive = false
        }
    }

    /**
     * Stop foreground service when no longer needed
     *
     * Called automatically by evaluateForegroundServiceNeed()
     */
    private fun stopForegroundService() {
        if (!foregroundServiceActive) return

        try {
            val intent = Intent(context, context.javaClass).apply {
                action = ACTION_STOP_MIC
            }
            context.startService(intent)

            foregroundServiceActive = false
            Log.i(TAG, "ForegroundService stopped (no longer needed)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ForegroundService", e)
        }
    }

    // ============================================================
    // State Queries
    // ============================================================

    /**
     * Check if foreground service is currently active
     *
     * @return true if foreground service is running
     */
    fun isForegroundServiceActive(): Boolean = foregroundServiceActive

    /**
     * Check if app is currently in background
     *
     * @return true if app is in background
     */
    fun isAppInBackground(): Boolean = appInBackground

    /**
     * Check if voice session is currently active
     *
     * @return true if voice session is active
     */
    fun isVoiceSessionActive(): Boolean = voiceSessionActive
}
