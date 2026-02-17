/**
 * NotificationActions.kt - Notification interaction actions
 * Created: 2025-10-10 20:00 PDT
 * Module: CommandManager
 *
 * Purpose: Notification reading, opening, and dismissing via voice commands
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Notification interaction actions
 * Handles reading, opening, and dismissing notifications via voice
 */
object NotificationActions {

    private const val TAG = "NotificationActions"

    /**
     * Read Notifications Action
     * Announces active notifications via TTS
     */
    class ReadNotificationsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult = withContext(Dispatchers.Main) {
            return@withContext try {
                val notifications = getActiveNotifications(context)

                if (notifications.isEmpty()) {
                    announceViaAccessibility(accessibilityService, "No active notifications")
                    createSuccessResult(command, "No active notifications")
                } else {
                    val notificationText = buildNotificationSummary(notifications)
                    announceViaAccessibility(accessibilityService, notificationText)
                    createSuccessResult(
                        command,
                        "Read ${notifications.size} notification(s)",
                        mapOf("count" to notifications.size, "notifications" to notifications)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read notifications", e)
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to read notifications: ${e.message}")
            }
        }

        private fun buildNotificationSummary(notifications: List<NotificationInfo>): String {
            return buildString {
                append("You have ${notifications.size} notification")
                if (notifications.size != 1) append("s")
                append(". ")

                notifications.take(5).forEachIndexed { index, notif ->
                    append("${index + 1}. ")
                    append("${notif.appName}: ${notif.title}. ")
                    if (notif.text.isNotEmpty()) {
                        append("${notif.text}. ")
                    }
                }

                if (notifications.size > 5) {
                    append("And ${notifications.size - 5} more.")
                }
            }
        }
    }

    /**
     * Open Notification Action
     * Opens a specific notification by index or content
     */
    class OpenNotificationAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult = withContext(Dispatchers.Main) {
            return@withContext try {
                val index = getNumberParameter(command, "index")?.toInt() ?: 1
                val notifications = getActiveNotifications(context)

                if (notifications.isEmpty()) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No notifications to open")
                } else if (index < 1 || index > notifications.size) {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Invalid notification index: $index")
                } else {
                    val notification = notifications[index - 1]

                    // Open notification using PendingIntent
                    notification.pendingIntent?.send()

                    announceViaAccessibility(accessibilityService, "Opening notification from ${notification.appName}")
                    createSuccessResult(command, "Opened notification ${index}: ${notification.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification", e)
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to open notification: ${e.message}")
            }
        }
    }

    /**
     * Dismiss Notification Action
     * Dismisses a specific notification by index
     */
    class DismissNotificationAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult = withContext(Dispatchers.Main) {
            return@withContext try {
                val index = getNumberParameter(command, "index")?.toInt() ?: 1
                val notifications = getActiveNotifications(context)

                if (notifications.isEmpty()) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No notifications to dismiss")
                } else if (index < 1 || index > notifications.size) {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Invalid notification index: $index")
                } else {
                    val notification = notifications[index - 1]

                    // Dismiss notification using key
                    cancelNotification(context, notification.key)

                    announceViaAccessibility(accessibilityService, "Notification dismissed")
                    createSuccessResult(command, "Dismissed notification ${index}: ${notification.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss notification", e)
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to dismiss notification: ${e.message}")
            }
        }
    }

    /**
     * Dismiss All Notifications Action
     * Clears all active notifications
     */
    class DismissAllNotificationsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult = withContext(Dispatchers.Main) {
            return@withContext try {
                val notifications = getActiveNotifications(context)

                if (notifications.isEmpty()) {
                    createSuccessResult(command, "No notifications to dismiss")
                } else {
                    // Dismiss all notifications
                    notifications.forEach { notification ->
                        try {
                            cancelNotification(context, notification.key)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to dismiss notification: ${notification.key}", e)
                        }
                    }

                    announceViaAccessibility(accessibilityService, "All notifications dismissed")
                    createSuccessResult(command, "Dismissed ${notifications.size} notification(s)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss all notifications", e)
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to dismiss notifications: ${e.message}")
            }
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    /**
     * Get active notifications from NotificationListenerService
     */
    private fun getActiveNotifications(@Suppress("UNUSED_PARAMETER") context: Context): List<NotificationInfo> {
        return try {
            val listener = com.augmentalis.voiceoscore.managers.commandmanager.notifications.VoiceOSNotificationListener.instance
            if (listener == null) {
                Log.w(TAG, "NotificationListener not connected. User must grant notification access.")
                return emptyList()
            }

            listener.getCapturedNotifications().map { data ->
                NotificationInfo(
                    key = data.key,
                    appName = data.appName,
                    title = data.title,
                    text = data.text,
                    timestamp = data.postTime,
                    pendingIntent = data.contentIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active notifications", e)
            emptyList()
        }
    }

    /**
     * Cancel/dismiss a specific notification by key
     */
    private fun cancelNotification(@Suppress("UNUSED_PARAMETER") context: Context, key: String) {
        val listener = com.augmentalis.voiceoscore.managers.commandmanager.notifications.VoiceOSNotificationListener.instance
        if (listener == null) {
            Log.w(TAG, "NotificationListener not connected")
            return
        }

        listener.dismissNotification(key)
        Log.d(TAG, "Dismissed notification: $key")
    }

    /**
     * Announce text via AccessibilityService (TalkBack compatible)
     */
    @Suppress("DEPRECATION")
    private fun announceViaAccessibility(service: AccessibilityService?, text: String) {
        service?.let {
            // Use accessibility announcement for TalkBack compatibility
            val event = android.view.accessibility.AccessibilityEvent.obtain()
            event.eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
            event.className = javaClass.name
            event.packageName = service.packageName
            event.text.add(text)
            service.serviceInfo?.feedbackType?.let { feedbackType ->
                if (feedbackType and android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN != 0) {
                    // TalkBack is active, use announcement
                    service.rootInActiveWindow?.let { root ->
                        root.performAction(
                            android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS.id
                        )
                    }
                }
            }
        }
    }

    /**
     * Notification information data class
     */
    data class NotificationInfo(
        val key: String,
        val appName: String,
        val title: String,
        val text: String,
        val timestamp: Long,
        val pendingIntent: android.app.PendingIntent?
    )
}
