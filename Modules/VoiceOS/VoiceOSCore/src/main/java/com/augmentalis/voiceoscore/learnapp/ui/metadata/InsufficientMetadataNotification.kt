/**
 * InsufficientMetadataNotification.kt - Notification overlay manager
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/metadata/InsufficientMetadataNotification.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Manages insufficient metadata notification overlays
 */

package com.augmentalis.voiceoscore.learnapp.ui.metadata

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.metadata.MetadataNotificationItem
import com.augmentalis.voiceoscore.learnapp.metadata.MetadataNotificationQueue

/**
 * Insufficient Metadata Notification
 *
 * Manages overlay notifications for elements with insufficient metadata.
 * Integrates with WindowManager to show non-blocking notifications.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val notification = InsufficientMetadataNotification(context, queue)
 *
 * // Show notification for next item in queue
 * notification.showNextNotification(
 *     onLabelProvided = { item, label ->
 *         saveManualLabel(item, label)
 *     },
 *     onSkip = {
 *         // Continue exploration
 *     },
 *     onSkipAll = {
 *         // Skip all remaining notifications
 *     }
 * )
 *
 * // Hide notification
 * notification.hideNotification()
 * ```
 *
 * @property context Application context
 * @property queue Notification queue manager
 *
 * @since 1.0.0
 */
class InsufficientMetadataNotification(
    private val context: Context,
    private val queue: MetadataNotificationQueue
) {
    /**
     * Window manager
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Current notification view
     */
    private var currentNotificationView: MetadataNotificationView? = null

    /**
     * Current notification item
     */
    private var currentItem: MetadataNotificationItem? = null

    /**
     * Notification visible state
     */
    private var isVisible = false

    /**
     * Show next notification from queue
     *
     * @param onLabelProvided Callback when user provides manual label
     * @param onSkip Callback when user skips notification
     * @param onSkipAll Callback when user skips all notifications
     */
    fun showNextNotification(
        onLabelProvided: (MetadataNotificationItem, String) -> Unit,
        onSkip: () -> Unit,
        onSkipAll: () -> Unit
    ) {
        // Get next notification
        val item = queue.getNextNotification() ?: return

        // Hide existing notification if any
        if (isVisible) {
            hideNotification()
        }

        // Create notification view
        val notificationView = MetadataNotificationView(context).apply {
            setNotificationItem(item)
            setQueueSize(queue.getCurrentSize())

            // Setup callbacks
            setOnSkipClickListener {
                queue.skipNotification(item)
                onSkip()
                hideNotification()

                // Show next if available
                if (queue.isReadyToShow()) {
                    showNextNotification(onLabelProvided, onSkip, onSkipAll)
                }
            }

            setOnSkipAllClickListener {
                queue.skipAllForSession()
                onSkipAll()
                hideNotification()
            }

            setOnProvideLabelClickListener {
                // Show manual label dialog
                showManualLabelDialog(item, onLabelProvided, onSkip, onSkipAll)
            }

            setOnCloseClickListener {
                // Same as skip
                queue.skipNotification(item)
                onSkip()
                hideNotification()

                // Show next if available
                if (queue.isReadyToShow()) {
                    showNextNotification(onLabelProvided, onSkip, onSkipAll)
                }
            }
        }

        // Create layout params for overlay
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Position at bottom of screen
            gravity = android.view.Gravity.BOTTOM
        }

        // Add view to window
        try {
            windowManager.addView(notificationView, params)
            currentNotificationView = notificationView
            currentItem = item
            isVisible = true
        } catch (e: Exception) {
            // Failed to add view (permission issue?)
            e.printStackTrace()
        }
    }

    /**
     * Show manual label dialog
     *
     * @param item Notification item
     * @param onLabelProvided Callback when label provided
     * @param onSkip Callback when skipped
     * @param onSkipAll Callback when skip all
     */
    private fun showManualLabelDialog(
        item: MetadataNotificationItem,
        onLabelProvided: (MetadataNotificationItem, String) -> Unit,
        onSkip: () -> Unit,
        onSkipAll: () -> Unit
    ) {
        val dialog = ManualLabelDialog(context)
        dialog.showDialog(
            item = item,
            onSave = { label ->
                onLabelProvided(item, label)
                hideNotification()

                // Show next if available
                if (queue.isReadyToShow()) {
                    showNextNotification(onLabelProvided, onSkip, onSkipAll)
                }
            },
            onCancel = {
                // Treat cancel as skip
                queue.skipNotification(item)
                onSkip()
                hideNotification()

                // Show next if available
                if (queue.isReadyToShow()) {
                    showNextNotification(onLabelProvided, onSkip, onSkipAll)
                }
            }
        )
    }

    /**
     * Hide notification
     */
    fun hideNotification() {
        currentNotificationView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View already removed
            }
        }

        currentNotificationView = null
        currentItem = null
        isVisible = false
    }

    /**
     * Check if notification is visible
     *
     * @return true if visible
     */
    fun isNotificationVisible(): Boolean {
        return isVisible
    }

    /**
     * Get current notification item
     *
     * @return Current item or null
     */
    fun getCurrentItem(): MetadataNotificationItem? {
        return currentItem
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        hideNotification()
    }
}
