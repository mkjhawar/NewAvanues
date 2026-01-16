/**
 * ExplorationNotifier.kt - Android exploration notifications
 *
 * Handles notification system for exploration events including
 * login screen alerts, generic alias notifications, and sound feedback.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android implementation of exploration notifications.
 *
 * Creates notifications for:
 * - Login screen detection
 * - Generic alias assignments
 * - Exploration progress/completion
 *
 * @property context Android context
 * @property scope Coroutine scope for async operations
 * @property config Exploration configuration
 */
class ExplorationNotifier(
    private val context: Context,
    private val scope: CoroutineScope,
    private val config: ExplorationConfig = ExplorationConfig.DEFAULT
) : IExplorationNotifier {

    private val notificationManager: NotificationManager? by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LearnApp Exploration",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for app exploration events"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun notifyLoginScreen(packageName: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Login Screen Detected")
                .setContentText("Please enter credentials for $packageName")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("LearnApp has detected a login screen in $packageName. " +
                            "Please manually enter your credentials. " +
                            "NOTE: Only element structures are saved - your password and email values are NOT captured. " +
                            "Exploration will automatically resume when the screen changes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .build()

            notificationManager?.notify(LOGIN_NOTIFICATION_ID, notification)

            if (config.soundEnabled) {
                playNotificationSound()
            }

            Log.i(TAG, "User notified for login screen: $packageName")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify user for login screen: ${e.message}", e)
        }
    }

    override fun notifyGenericAlias(avid: String, genericAlias: String, element: ElementInfo) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Unnamed Element Found")
                .setContentText("${element.className.substringAfterLast('.')} has no label. Voice command: \"$genericAlias\"")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Element Type: ${element.className.substringAfterLast('.')}\n" +
                            "Assigned Name: \"$genericAlias\"\n" +
                            "Position: ${element.bounds}\n\n" +
                            "You can customize this later in Settings."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(avid.hashCode(), notification)

            Log.d(TAG, "Notified user about generic alias: $genericAlias")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send generic alias notification", e)
        }
    }

    override fun notifyProgress(packageName: String, progressPercent: Int, screensExplored: Int) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle("Exploring $packageName")
                .setContentText("$progressPercent% complete - $screensExplored screens explored")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(100, progressPercent, false)
                .build()

            notificationManager?.notify(PROGRESS_NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress notification", e)
        }
    }

    override fun notifyComplete(packageName: String, stats: ExplorationStats) {
        try {
            cancelAllNotifications()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Exploration Complete")
                .setContentText("$packageName: ${stats.totalScreens} screens, ${stats.completeness.toInt()}% learned")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("App: $packageName\n" +
                            "Screens: ${stats.totalScreens}\n" +
                            "Elements: ${stats.totalElements}\n" +
                            "Duration: ${stats.durationMs / 1000}s\n" +
                            "Completeness: ${stats.completeness.toInt()}%"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(COMPLETE_NOTIFICATION_ID, notification)

            if (config.soundEnabled) {
                playNotificationSound()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show completion notification", e)
        }
    }

    override fun notifyError(packageName: String, error: Throwable) {
        try {
            cancelAllNotifications()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Exploration Failed")
                .setContentText("Error exploring $packageName")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Failed to explore $packageName:\n${error.message}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(ERROR_NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show error notification", e)
        }
    }

    override fun cancelAllNotifications() {
        try {
            notificationManager?.cancel(LOGIN_NOTIFICATION_ID)
            notificationManager?.cancel(PROGRESS_NOTIFICATION_ID)
            notificationManager?.cancel(COMPLETE_NOTIFICATION_ID)
            notificationManager?.cancel(ERROR_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notifications: ${e.message}")
        }
    }

    private fun playNotificationSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            scope.launch {
                delay(300L)
                toneGenerator.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play notification sound: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ExplorationNotifier"
        private const val CHANNEL_ID = "learnapp_exploration"
        private const val LOGIN_NOTIFICATION_ID = 1001
        private const val PROGRESS_NOTIFICATION_ID = 1002
        private const val COMPLETE_NOTIFICATION_ID = 1003
        private const val ERROR_NOTIFICATION_ID = 1004
    }
}
