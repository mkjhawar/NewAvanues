/**
 * AccessibilityServiceMonitor.kt - VoiceOS Accessibility Service Auto-Reconnection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-12 (Phase 7: Reliability Polish)
 *
 * Monitors VoiceOS accessibility service status and provides auto-reconnection alerts.
 * Shows notification when service is disconnected with intent to re-enable.
 */
package com.augmentalis.voiceoscore.accessibility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.augmentalis.voiceoscore.R

/**
 * Monitors VoiceOS accessibility service and provides reconnection alerts.
 *
 * Features:
 * - Periodic 5-second checks for service status
 * - Notification when service is disabled
 * - Direct intent to accessibility settings
 * - Auto-dismissal when service is re-enabled
 * - Lifecycle-aware monitoring
 *
 * Usage:
 * ```kotlin
 * val monitor = AccessibilityServiceMonitor(context)
 * monitor.startMonitoring()
 * // ... later
 * monitor.stopMonitoring()
 * ```
 */
class AccessibilityServiceMonitor(private val context: Context) {

    companion object {
        private const val TAG = "AccessibilityServiceMonitor"

        /** Check interval in milliseconds (5 seconds) */
        private const val CHECK_INTERVAL_MS = 5000L

        /** Notification channel ID for accessibility alerts */
        private const val CHANNEL_ID = "voiceos_accessibility_status"

        /** Notification ID for reconnection alerts */
        private const val NOTIFICATION_ID = 10001

        /** VoiceOS accessibility service component name */
        private const val SERVICE_NAME = "com.augmentalis.voiceoscore/.accessibility.VoiceOSService"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var isMonitoring = false
    private var wasServiceEnabled = false
    private var notificationShown = false

    init {
        createNotificationChannel()
    }

    /**
     * Creates the notification channel for accessibility status alerts.
     *
     * Channel properties:
     * - Importance: HIGH (requires user attention)
     * - Sound enabled
     * - Vibration enabled
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VoiceOS Service Status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when VoiceOS accessibility service is disconnected"
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Starts monitoring the accessibility service status.
     *
     * Performs periodic checks every 5 seconds to detect service disconnection.
     * Shows notification when service is disabled.
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Monitoring already started")
            return
        }

        isMonitoring = true
        wasServiceEnabled = isAccessibilityServiceEnabled()

        Log.i(TAG, "Started monitoring VoiceOS accessibility service (initial state: ${if (wasServiceEnabled) "enabled" else "disabled"})")

        // Start periodic checks
        handler.post(checkServiceRunnable)
    }

    /**
     * Stops monitoring the accessibility service status.
     *
     * Cancels all pending checks and hides any active notifications.
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "Monitoring already stopped")
            return
        }

        isMonitoring = false
        handler.removeCallbacks(checkServiceRunnable)
        hideReconnectionNotification()

        Log.i(TAG, "Stopped monitoring VoiceOS accessibility service")
    }

    /**
     * Runnable for periodic service status checks.
     */
    private val checkServiceRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return

            checkService()

            // Schedule next check
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    /**
     * Checks the current service status and handles state changes.
     *
     * Shows notification when service becomes disabled.
     * Hides notification when service becomes enabled.
     */
    private fun checkService() {
        val isEnabled = isAccessibilityServiceEnabled()

        // Detect state change
        if (wasServiceEnabled && !isEnabled) {
            // Service was enabled, now disabled
            Log.w(TAG, "VoiceOS accessibility service disconnected")
            showReconnectionNotification()
        } else if (!wasServiceEnabled && isEnabled) {
            // Service was disabled, now enabled
            Log.i(TAG, "VoiceOS accessibility service reconnected")
            hideReconnectionNotification()
        }

        wasServiceEnabled = isEnabled
    }

    /**
     * Checks if the VoiceOS accessibility service is currently enabled.
     *
     * @return true if service is enabled, false otherwise
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(SERVICE_NAME)
    }

    /**
     * Shows a notification alerting the user that the service is disconnected.
     *
     * Notification features:
     * - Persistent (ongoing)
     * - Tap to open accessibility settings
     * - Action button to directly access settings
     * - Auto-dismisses when service is re-enabled
     */
    private fun showReconnectionNotification() {
        if (notificationShown) {
            // Notification already shown, no need to show again
            return
        }

        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("VoiceOS Service Disconnected")
            .setContentText("Tap to enable VoiceOS accessibility service")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("VoiceOS accessibility service is disabled. Voice commands and cursor control will not work. Tap to enable it in accessibility settings.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_settings,
                "Enable Service",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        notificationShown = true

        Log.w(TAG, "Reconnection notification shown")
    }

    /**
     * Hides the reconnection notification.
     *
     * Called when:
     * - Service is re-enabled
     * - Monitoring is stopped
     */
    private fun hideReconnectionNotification() {
        if (!notificationShown) {
            return
        }

        notificationManager.cancel(NOTIFICATION_ID)
        notificationShown = false

        Log.i(TAG, "Reconnection notification hidden")
    }

    /**
     * Force check the service status immediately.
     *
     * Useful for manual verification or triggered checks.
     */
    fun forceCheck() {
        checkService()
    }

    /**
     * Get the current monitoring status.
     *
     * @return true if monitoring is active, false otherwise
     */
    fun isMonitoringActive(): Boolean = isMonitoring
}
