package com.augmentalis.Avanues.web.universal.screenshot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper class for showing screenshot notifications on Android
 *
 * Features:
 * - "Screenshot saved" notification
 * - Actions: View, Share, Delete
 * - Uses notification channel (Android O+)
 */
class ScreenshotNotificationHelper(
    private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "screenshot_channel"
        private const val CHANNEL_NAME = "Screenshots"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for screenshots (Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for screenshot captures"
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification for saved screenshot
     *
     * @param filepath Path to saved screenshot
     * @param filename Screenshot filename
     */
    fun showScreenshotSavedNotification(filepath: String, filename: String) {
        val file = File(filepath)
        if (!file.exists()) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // View action
        val viewIntent = createViewIntent(file)
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            0,
            viewIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Share action
        val shareIntent = createShareIntent(file)
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            1,
            shareIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Delete action
        val deleteIntent = createDeleteIntent(filepath)
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Screenshot saved")
            .setContentText(filename)
            .setContentIntent(viewPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View",
                viewPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_share,
                "Share",
                sharePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                "Delete",
                deletePendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show notification for screenshot error
     */
    fun showScreenshotErrorNotification(error: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Screenshot failed")
            .setContentText(error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Dismiss screenshot notification
     */
    fun dismissNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Create intent to view screenshot in gallery
     */
    private fun createViewIntent(file: File): Intent {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Create intent to share screenshot
     */
    private fun createShareIntent(file: File): Intent {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, "Share Screenshot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Create intent to delete screenshot
     * This will be handled by a BroadcastReceiver
     */
    private fun createDeleteIntent(filepath: String): Intent {
        return Intent("com.augmentalis.webavanue.DELETE_SCREENSHOT").apply {
            putExtra("filepath", filepath)
        }
    }
}
