package com.augmentalis.avamagic.ui.core.feedback

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.Position
import com.augmentalis.avamagic.components.core.Severity

/**
 * Centralized notification display system.
 *
 * NotificationCenter manages and displays multiple notifications in a
 * consistent location (typically top-right corner).
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic notification center
 * val center = NotificationCenterComponent(
 *     notifications = listOf(
 *         Notification("1", "New message", Severity.INFO),
 *         Notification("2", "Upload complete", Severity.SUCCESS)
 *     )
 * )
 *
 * // Custom position
 * val center = NotificationCenterComponent(
 *     notifications = notifications,
 *     position = Position.BOTTOM_RIGHT
 * )
 *
 * // With max displayed
 * val center = NotificationCenterComponent(
 *     notifications = allNotifications,
 *     maxDisplayed = 3
 * )
 * ```
 *
 * @property notifications List of notifications to display
 * @property position Screen position (default TOP_RIGHT)
 * @property maxDisplayed Maximum notifications shown (default 5)
 * @property autoRemove Auto-remove after timeout (default true)
 * @property timeout Removal timeout in ms (default 5000)
 * @since 1.0.0
 */
data class NotificationCenterComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val position: Position = Position.TOP_RIGHT,
    val maxDisplayed: Int = 5,
    val autoRemove: Boolean = true,
    val timeout: Long = 5000
) : Component {
    init {
        require(maxDisplayed > 0) { "maxDisplayed must be positive (got $maxDisplayed)" }
        require(timeout > 0) { "timeout must be positive (got $timeout)" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Adds a notification to the center.
     */
    fun addNotification(notification: Notification): NotificationCenterComponent {
        return copy(notifications = notifications + notification)
    }

    /**
     * Removes a notification by ID.
     */
    fun removeNotification(id: String): NotificationCenterComponent {
        return copy(notifications = notifications.filter { it.id != id })
    }

    /**
     * Clears all notifications.
     */
    fun clearAll(): NotificationCenterComponent {
        return copy(notifications = emptyList())
    }

    /**
     * Returns notifications to display (respecting maxDisplayed).
     */
    val displayedNotifications: List<Notification>
        get() = notifications.take(maxDisplayed)

    companion object {
        /**
         * Creates an empty notification center.
         */
        fun empty(position: Position = Position.TOP_RIGHT) =
            NotificationCenterComponent(position = position)
    }
}

/**
 * Individual notification data.
 *
 * @property id Unique notification identifier
 * @property message Notification message text
 * @property severity Notification severity level
 * @property title Optional notification title
 * @property timestamp Notification creation time (ms since epoch)
 * @property dismissible Whether user can dismiss (default true)
 */
data class Notification(
    val id: String,
    val message: String,
    val severity: Severity = Severity.INFO,
    val title: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val dismissible: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "Notification ID cannot be blank" }
        require(message.isNotBlank()) { "Notification message cannot be blank" }
    }

    companion object {
        /**
         * Creates an info notification.
         */
        fun info(id: String, message: String, title: String? = null) =
            Notification(id, message, Severity.INFO, title)

        /**
         * Creates a success notification.
         */
        fun success(id: String, message: String, title: String? = null) =
            Notification(id, message, Severity.SUCCESS, title)

        /**
         * Creates a warning notification.
         */
        fun warning(id: String, message: String, title: String? = null) =
            Notification(id, message, Severity.WARNING, title)

        /**
         * Creates an error notification.
         */
        fun error(id: String, message: String, title: String? = null) =
            Notification(id, message, Severity.ERROR, title)
    }
}
