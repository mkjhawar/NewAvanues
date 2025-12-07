/**
 * VoiceOSNotificationListener.kt - Notification Listener Service for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-11-18
 *
 * Captures notifications for voice control access.
 * Provides getActiveNotifications() and dismissNotification() functionality.
 *
 * IMPORTANT: User must grant notification access permission in Settings.
 */

package com.augmentalis.commandmanager.notifications

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * NotificationListenerService for capturing and managing notifications
 *
 * Features:
 * - Captures all posted notifications
 * - Provides access to active notifications
 * - Allows dismissing notifications by key
 * - Thread-safe notification storage
 *
 * Usage:
 * 1. Add service to AndroidManifest.xml
 * 2. User grants notification access permission
 * 3. Access notifications via VoiceOSNotificationListener.instance
 */
class VoiceOSNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "VoiceOSNotificationListener"

        /** Singleton instance (set when service connects) */
        @Volatile
        var instance: VoiceOSNotificationListener? = null
            private set

        /** Check if notification listener is connected */
        val isConnected: Boolean
            get() = instance != null
    }

    /** Active notifications indexed by key */
    private val notifications = ConcurrentHashMap<String, NotificationData>()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NotificationListener created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "NotificationListener connected")

        // Load existing notifications
        try {
            val active: Array<StatusBarNotification> = activeNotifications ?: emptyArray()
            active.forEach { sbn ->
                onNotificationPosted(sbn)
            }
            Log.d(TAG, "Loaded ${notifications.size} existing notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading existing notifications", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        notifications.clear()
        Log.i(TAG, "NotificationListener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        try {
            val notification = sbn.notification
            val extras = notification.extras

            val data = NotificationData(
                key = sbn.key,
                packageName = sbn.packageName,
                id = sbn.id,
                tag = sbn.tag,
                title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                appName = getAppName(sbn.packageName),
                postTime = sbn.postTime,
                contentIntent = notification.contentIntent,
                isOngoing = sbn.isOngoing,
                isClearable = sbn.isClearable
            )

            notifications[sbn.key] = data
            Log.d(TAG, "Notification posted: ${data.appName} - ${data.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return

        notifications.remove(sbn.key)
        Log.d(TAG, "Notification removed: ${sbn.key}")
    }

    /**
     * Get all captured notifications
     *
     * @return List of notification data sorted by post time (newest first)
     */
    fun getCapturedNotifications(): List<NotificationData> {
        return notifications.values
            .sortedByDescending { it.postTime }
            .toList()
    }

    /**
     * Get notifications filtered by app
     *
     * @param packageName Package name to filter by
     * @return Notifications from specified app
     */
    fun getNotificationsForApp(packageName: String): List<NotificationData> {
        return notifications.values
            .filter { it.packageName == packageName }
            .sortedByDescending { it.postTime }
    }

    /**
     * Get notification by key
     *
     * @param key Notification key
     * @return Notification data or null if not found
     */
    fun getNotification(key: String): NotificationData? {
        return notifications[key]
    }

    /**
     * Dismiss a notification by key
     *
     * @param key Notification key to dismiss
     * @return true if dismissed, false if not found or not clearable
     */
    fun dismissNotification(key: String): Boolean {
        val notification = notifications[key] ?: return false

        if (!notification.isClearable) {
            Log.w(TAG, "Cannot dismiss non-clearable notification: $key")
            return false
        }

        try {
            cancelNotification(key)
            notifications.remove(key)
            Log.d(TAG, "Dismissed notification: $key")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification", e)
            return false
        }
    }

    /**
     * Dismiss all clearable notifications
     *
     * @return Number of notifications dismissed
     */
    fun dismissAllNotifications(): Int {
        val clearable = notifications.values.filter { it.isClearable }
        var dismissed = 0

        for (notification in clearable) {
            try {
                cancelNotification(notification.key)
                notifications.remove(notification.key)
                dismissed++
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing notification: ${notification.key}", e)
            }
        }

        Log.d(TAG, "Dismissed $dismissed notifications")
        return dismissed
    }

    /**
     * Get count of active notifications
     */
    fun getNotificationCount(): Int = notifications.size

    /**
     * Check if any notifications exist
     */
    fun hasNotifications(): Boolean = notifications.isNotEmpty()

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        notifications.clear()
        Log.i(TAG, "NotificationListener destroyed")
    }
}

/**
 * Notification data class with all relevant information
 */
data class NotificationData(
    /** Unique key for this notification */
    val key: String,

    /** Package name of the app that posted this notification */
    val packageName: String,

    /** Notification ID */
    val id: Int,

    /** Notification tag (may be null) */
    val tag: String?,

    /** Notification title */
    val title: String,

    /** Notification text content */
    val text: String,

    /** Expanded text (if using BigTextStyle) */
    val bigText: String?,

    /** Sub-text line */
    val subText: String?,

    /** Human-readable app name */
    val appName: String,

    /** Time notification was posted */
    val postTime: Long,

    /** Content intent to launch when tapped */
    val contentIntent: android.app.PendingIntent?,

    /** Whether this is an ongoing notification (like music player) */
    val isOngoing: Boolean,

    /** Whether this notification can be cleared by user */
    val isClearable: Boolean
)
