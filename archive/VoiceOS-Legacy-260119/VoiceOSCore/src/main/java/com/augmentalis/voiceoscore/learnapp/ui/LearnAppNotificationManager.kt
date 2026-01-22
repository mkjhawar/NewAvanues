/*
 * LearnAppNotificationManager.kt - Background notification manager for LearnApp
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/LearnAppNotificationManager.kt
 *
 * Manages background notifications for LearnApp exploration when command bar is minimized.
 * Provides pause/stop controls and shows exploration progress.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-06
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.augmentalis.voiceoscore.R

/**
 * Manages background notifications for LearnApp exploration.
 *
 * Responsibilities:
 * - Shows persistent notification when command bar is hidden
 * - Displays exploration progress
 * - Provides pause/stop action buttons
 * - Handles notification tap to restore command bar
 *
 * Usage:
 * ```kotlin
 * val notificationManager = LearnAppNotificationManager(context)
 * notificationManager.showBackgroundNotification("com.example.app", 45, false)
 * notificationManager.hideNotification()
 * ```
 */
class LearnAppNotificationManager(private val context: Context) {

    companion object {
        /** Notification channel ID for LearnApp progress notifications */
        const val CHANNEL_ID = "learnapp_progress"

        /** Unique notification ID for learning notifications */
        const val LEARN_NOTIFICATION_ID = 12345

        /** Action identifier for pause intent */
        const val ACTION_PAUSE = "com.augmentalis.voiceoscore.learnapp.ACTION_PAUSE"

        /** Action identifier for resume intent */
        const val ACTION_RESUME = "com.augmentalis.voiceoscore.learnapp.ACTION_RESUME"

        /** Action identifier for stop intent */
        const val ACTION_STOP = "com.augmentalis.voiceoscore.learnapp.ACTION_STOP"

        /** Action identifier for showing command bar */
        const val ACTION_SHOW_COMMAND_BAR = "com.augmentalis.voiceoscore.learnapp.ACTION_SHOW_COMMAND_BAR"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Creates the notification channel for LearnApp progress notifications.
     *
     * Channel properties:
     * - Importance: LOW (no sound, minimal interruption)
     * - No badge
     * - Persistent for ongoing exploration
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LearnApp Progress",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows app learning progress and controls"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Shows or updates the background notification for active exploration.
     *
     * @param packageName Package name of the app being learned
     * @param progress Exploration progress percentage (0-100)
     * @param isPaused Whether exploration is currently paused
     */
    fun showBackgroundNotification(
        packageName: String,
        progress: Int,
        isPaused: Boolean
    ) {
        val appName = getAppName(packageName)
        val title = if (isPaused) "Paused: $appName" else "Learning $appName"
        val text = "$progress% complete"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_learn)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, progress, false)
            .setOngoing(!isPaused)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(createPauseResumeAction(isPaused))
            .addAction(createStopAction())
            .setContentIntent(createShowCommandBarIntent())
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        notificationManager.notify(LEARN_NOTIFICATION_ID, notification)
    }

    /**
     * Creates the pause or resume action based on current state.
     *
     * @param isPaused Current pause state
     * @return NotificationCompat.Action for pause/resume
     */
    private fun createPauseResumeAction(isPaused: Boolean): NotificationCompat.Action {
        val action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val icon = if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
        val label = if (isPaused) "Resume" else "Pause"

        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(icon, label, pendingIntent).build()
    }

    /**
     * Creates the stop action for terminating exploration.
     *
     * @return NotificationCompat.Action for stop
     */
    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(ACTION_STOP).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            "Stop",
            pendingIntent
        ).build()
    }

    /**
     * Creates the content intent for showing the command bar when notification is tapped.
     *
     * @return PendingIntent that triggers command bar display
     */
    private fun createShowCommandBarIntent(): PendingIntent {
        val intent = Intent(ACTION_SHOW_COMMAND_BAR).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Hides the background notification.
     *
     * Call this when:
     * - Command bar is shown
     * - Exploration is completed
     * - Exploration is stopped
     */
    fun hideNotification() {
        notificationManager.cancel(LEARN_NOTIFICATION_ID)
    }

    /**
     * Gets the human-readable app name from package name.
     *
     * @param packageName Package name of the app
     * @return Human-readable app name, or package name if not found
     */
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            // Fall back to package name if app info not available
            packageName
        }
    }

    /**
     * Updates notification progress without changing other properties.
     *
     * More efficient than calling showBackgroundNotification for progress-only updates.
     *
     * @param packageName Package name of the app being learned
     * @param progress New progress percentage (0-100)
     */
    fun updateProgress(packageName: String, progress: Int) {
        val appName = getAppName(packageName)
        val text = "$progress% complete"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_learn)
            .setContentTitle("Learning $appName")
            .setContentText(text)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(LEARN_NOTIFICATION_ID, notification)
    }
}
