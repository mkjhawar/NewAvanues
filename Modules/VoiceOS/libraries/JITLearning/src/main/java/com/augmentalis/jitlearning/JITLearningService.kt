/**
 * JITLearningService.kt - Foreground service for passive screen learning
 *
 * Runs as foreground service in VoiceOSCore process to ensure it's never killed.
 * Provides AIDL interface for coordination with LearnApp standalone app.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: JIT-LearnApp Separation (Phase 2)
 *
 * ## Architecture:
 *
 * ```
 * VoiceOSCore Process                    LearnApp Process
 * ┌─────────────────────┐                ┌──────────────────┐
 * │  VoiceOSService     │                │  LearnAppActivity│
 * │  (Accessibility)    │                │                  │
 * │         │           │                │         │        │
 * │         ▼           │                │         ▼        │
 * │  JITLearningService │◄───AIDL IPC───│  AIDL Client     │
 * │  (Foreground)       │                │  Binding         │
 * │         │           │                │                  │
 * │         ▼           │                │                  │
 * │  JustInTimeLearner  │                │                  │
 * │  (Passive Learning) │                │                  │
 * └─────────────────────┘                └──────────────────┘
 * ```
 *
 * ## Lifecycle:
 *
 * 1. **Start**: VoiceOSService starts JITLearningService on boot
 * 2. **Bind**: VoiceOSService binds to forward accessibility events
 * 3. **Running**: Service runs as foreground (notification shown)
 * 4. **Pause**: LearnApp can pause capture via IPC
 * 5. **Resume**: LearnApp resumes capture after exploration
 * 6. **Query**: LearnApp queries state for UI display
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.jitlearning

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * JIT Learning Service
 *
 * Foreground service implementing IElementCaptureService for AIDL IPC.
 * Runs passive screen learning in background without user interaction.
 */
class JITLearningService : Service() {

    companion object {
        private const val TAG = "JITLearningService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "jit_learning_service"
        private const val CHANNEL_NAME = "JIT Learning"
    }

    // Service state
    private var isPaused = false
    private var screensLearned = 0
    private var elementsDiscovered = 0
    private var currentPackageName: String? = null
    private var lastCaptureTime = 0L

    // TODO: Integrate JustInTimeLearner in Phase 4
    // private lateinit var jitLearner: JustInTimeLearner

    /**
     * AIDL Binder Implementation
     *
     * Implements IElementCaptureService interface for IPC.
     */
    private val binder = object : IElementCaptureService.Stub() {

        override fun pauseCapture() {
            Log.i(TAG, "Pause capture request via AIDL")
            isPaused = true
            // TODO: Call jitLearner.pause() in Phase 4
        }

        override fun resumeCapture() {
            Log.i(TAG, "Resume capture request via AIDL")
            isPaused = false
            // TODO: Call jitLearner.resume() in Phase 4
        }

        override fun queryState(): JITState {
            Log.d(TAG, "Query state request via AIDL")
            return JITState(
                isActive = !isPaused,
                currentPackage = currentPackageName,
                screensLearned = screensLearned,
                elementsDiscovered = elementsDiscovered,
                lastCaptureTime = lastCaptureTime
            )
        }

        override fun getLearnedScreenHashes(packageName: String): List<String> {
            Log.d(TAG, "Get learned screen hashes for: $packageName")
            // TODO: Query database for screen hashes in Phase 4
            return emptyList()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "JIT Learning Service created")

        // Create notification channel
        createNotificationChannel()

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // TODO: Initialize JustInTimeLearner in Phase 4
        // jitLearner = JustInTimeLearner(...)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "JIT Learning Service started")
        return START_STICKY  // Auto-restart if killed
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "JIT Learning Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "JIT Learning Service destroyed")
        // TODO: Cleanup JustInTimeLearner in Phase 4
    }

    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // Low importance to avoid interruptions
            ).apply {
                description = "Passive voice command learning service"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JIT Learning Active")
            .setContentText("Learning voice commands passively...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)  // TODO: Use proper icon
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Process accessibility event (called from VoiceOSService)
     *
     * TODO: Implement in Phase 4
     */
    fun onAccessibilityEvent(packageName: String, event: android.view.accessibility.AccessibilityEvent) {
        if (isPaused) return

        // TODO: Forward to JustInTimeLearner in Phase 4
        // jitLearner.onAccessibilityEvent(packageName, event)

        // Update state
        currentPackageName = packageName
        lastCaptureTime = System.currentTimeMillis()
    }
}
