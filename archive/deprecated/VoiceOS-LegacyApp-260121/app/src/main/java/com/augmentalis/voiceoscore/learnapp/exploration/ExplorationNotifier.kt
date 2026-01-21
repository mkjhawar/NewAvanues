/**
 * ExplorationNotifier.kt - Notification system for exploration events
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ExplorationNotifier.kt
 *
 * Author: Manoj Jhawar (refactored by Claude)
 * Created: 2025-12-08
 * Refactored: 2026-01-15 (SOLID extraction from ExplorationEngine.kt)
 *
 * Single Responsibility: Handles all notification-related operations during exploration,
 * including login screen alerts, generic alias notifications, and sound feedback.
 *
 * Extracted from ExplorationEngine.kt to improve maintainability and testability.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles notification operations during app exploration.
 *
 * This class is responsible for:
 * - Creating and managing notification channels
 * - Displaying login screen alerts with sound feedback
 * - Notifying users about elements with generic aliases
 * - Managing notification lifecycle
 *
 * ## Usage Example
 *
 * ```kotlin
 * val notifier = ExplorationNotifier(context, scope)
 *
 * // Notify user about login screen
 * notifier.notifyLoginScreen("com.instagram.android")
 *
 * // Notify about generic alias
 * notifier.notifyGenericAlias(uuid, "button_1", element)
 * ```
 *
 * @property context Android context for notification operations
 * @property scope CoroutineScope for async operations (sound release)
 */
class ExplorationNotifier(
    private val context: Context,
    private val scope: CoroutineScope
) {
    /**
     * Sound release delay in milliseconds
     */
    private val soundReleaseDelayMs = 300L

    /**
     * Notify user to enter credentials on login screen
     *
     * Creates a notification and plays a sound to alert the user that
     * manual credential input is required. This is a privacy-preserving
     * approach - we register element structures but DO NOT capture
     * actual passwords or email values entered by the user.
     *
     * @param packageName Package name of the app with login screen
     */
    fun notifyLoginScreen(packageName: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return

            // Create notification channel (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "LearnApp Exploration",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for app exploration events"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create notification
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Login Screen Detected")
                .setContentText("Please enter credentials for $packageName. Exploration will resume after login.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("LearnApp has detected a login screen in $packageName. " +
                            "Please manually enter your credentials. " +
                            "NOTE: Only element structures are saved - your password and email values are NOT captured. " +
                            "Exploration will automatically resume when the screen changes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .build()

            notificationManager.notify(LOGIN_NOTIFICATION_ID, notification)

            // Play notification sound
            playNotificationSound()

            android.util.Log.i(TAG,
                "User notified for login screen: $packageName (notification + sound)")

        } catch (e: Exception) {
            android.util.Log.e(TAG,
                "Failed to notify user for login screen: ${e.message}", e)
        }
    }

    /**
     * Notify user that element has no metadata and generic alias was assigned
     *
     * Shows notification allowing user to customize the alias via voice command.
     *
     * @param uuid The UUID of the element
     * @param genericAlias The generic alias that was assigned
     * @param element The element info for display purposes
     */
    fun notifyGenericAlias(uuid: String, genericAlias: String, element: ElementInfo) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return

            // Create notification channel if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "LearnApp Exploration",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about app exploration and element learning"
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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

            // Use UUID hash as notification ID to avoid duplicates
            notificationManager.notify(uuid.hashCode(), notification)

            android.util.Log.d(TAG,
                "Notified user about generic alias: $genericAlias for element at ${element.bounds}")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send generic alias notification", e)
        }
    }

    /**
     * Play notification sound for alerts
     */
    private fun playNotificationSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            scope.launch {
                delay(soundReleaseDelayMs)
                toneGenerator.release()
            }
        } catch (soundError: Exception) {
            android.util.Log.w(TAG,
                "Failed to play notification sound: ${soundError.message}")
        }
    }

    /**
     * Cancel all exploration-related notifications
     */
    fun cancelAllNotifications() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return
            notificationManager.cancel(LOGIN_NOTIFICATION_ID)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to cancel notifications: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ExplorationNotifier"

        /**
         * Notification channel ID for exploration events
         */
        const val NOTIFICATION_CHANNEL_ID = "learnapp_exploration"

        /**
         * Notification ID for login screen alerts
         */
        const val LOGIN_NOTIFICATION_ID = 1001
    }
}
