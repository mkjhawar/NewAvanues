package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * NoData component - Flutter Material parity
 *
 * A Material Design 3 empty state placeholder that displays when no data is available.
 * Shows icon, message, and optional action to add data or refresh.
 *
 * **Web Equivalent:** `Empty` state (MUI)
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Clear empty state icon
 * - Primary message
 * - Optional detailed description
 * - Action button (e.g., "Add Item", "Refresh")
 * - Configurable icon and styling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * NoData(
 *     message = "No items yet",
 *     description = "Add your first item to get started.",
 *     icon = "inbox",
 *     showAction = true,
 *     actionLabel = "Add Item",
 *     onAction = { /* navigate to add screen */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether no data state is visible
 * @property message Primary message text
 * @property description Optional detailed description
 * @property icon Icon name (default "inbox")
 * @property iconSize Icon size in dp (default 80)
 * @property showAction Whether to show action button
 * @property actionLabel Action button label (default "Add")
 * @property onAction Callback when action is clicked
 * @property color Optional custom color for icon
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class NoData(
    override val type: String = "NoData",
    override val id: String? = null,
    val visible: Boolean = true,
    val message: String,
    val description: String? = null,
    val icon: String = "inbox",
    val iconSize: Float = 80f,
    val showAction: Boolean = false,
    val actionLabel: String = "Add",
    @Transient
    val onAction: (() -> Unit)? = null,
    val color: String? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val desc = description?.let { ". $it" } ?: ""
        val actionText = if (showAction && onAction != null) ". $actionLabel button available" else ""
        return contentDescription ?: "Empty state: $message$desc$actionText"
    }

    /**
     * Check if has description
     */
    fun hasDescription(): Boolean = !description.isNullOrBlank()

    /**
     * Check if action is available
     */
    fun isActionAvailable(): Boolean = showAction && onAction != null

    companion object {
        /**
         * Create an empty list state
         */
        fun forEmptyList(
            itemName: String = "items",
            onAdd: (() -> Unit)? = null
        ) = NoData(
            message = "No $itemName yet",
            description = "Start by adding your first ${itemName.removeSuffix("s")}.",
            icon = "inbox",
            showAction = onAdd != null,
            actionLabel = "Add ${itemName.removeSuffix("s").replaceFirstChar { it.uppercase() }}",
            onAction = onAdd
        )

        /**
         * Create a search results empty state
         */
        fun searchEmpty(
            query: String? = null
        ) = NoData(
            message = if (query != null) "No results for \"$query\"" else "No results found",
            description = "Try adjusting your search terms.",
            icon = "search_off",
            showAction = false
        )

        /**
         * Create a favorites empty state
         */
        fun emptyFavorites(
            itemName: String = "favorites"
        ) = NoData(
            message = "No $itemName yet",
            description = "Items you favorite will appear here.",
            icon = "favorite_border",
            showAction = false
        )

        /**
         * Create a history empty state
         */
        fun emptyHistory() = NoData(
            message = "No history yet",
            description = "Your recent activity will appear here.",
            icon = "history",
            showAction = false
        )

        /**
         * Create a notifications empty state
         */
        fun emptyNotifications() = NoData(
            message = "No notifications",
            description = "You're all caught up!",
            icon = "notifications_none",
            showAction = false
        )
    }
}
