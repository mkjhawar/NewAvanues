/**
 * IExplorationNotifier.kt - User notification interface
 *
 * Defines the contract for notifying users during exploration.
 * Platform implementations handle the actual notification display.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Interface for exploration notifications.
 *
 * Implementations handle platform-specific notifications:
 * - Android: NotificationManager + NotificationChannel
 * - iOS: UNUserNotificationCenter
 * - Desktop: System tray notifications
 */
interface IExplorationNotifier {

    /**
     * Notify user about login screen detection.
     *
     * Creates a notification alerting the user that manual credential
     * input is required. Privacy note: element structures are saved,
     * but actual password/email values are NOT captured.
     *
     * @param packageName Package name of the app with login screen
     */
    fun notifyLoginScreen(packageName: String)

    /**
     * Notify user about element with generic alias.
     *
     * Shows notification when an element has no metadata (text/contentDesc/resourceId)
     * and a generic alias was assigned.
     *
     * @param uuid The UUID of the element
     * @param genericAlias The generic alias that was assigned
     * @param element The element info for display purposes
     */
    fun notifyGenericAlias(uuid: String, genericAlias: String, element: ElementInfo)

    /**
     * Notify user about exploration progress.
     *
     * @param packageName Package being explored
     * @param progressPercent Progress percentage (0-100)
     * @param screensExplored Number of screens explored
     */
    fun notifyProgress(packageName: String, progressPercent: Int, screensExplored: Int)

    /**
     * Notify user about exploration completion.
     *
     * @param packageName Package that was explored
     * @param stats Final exploration statistics
     */
    fun notifyComplete(packageName: String, stats: ExplorationStats)

    /**
     * Notify user about exploration error.
     *
     * @param packageName Package being explored
     * @param error Error that occurred
     */
    fun notifyError(packageName: String, error: Throwable)

    /**
     * Cancel all exploration-related notifications.
     */
    fun cancelAllNotifications()
}
