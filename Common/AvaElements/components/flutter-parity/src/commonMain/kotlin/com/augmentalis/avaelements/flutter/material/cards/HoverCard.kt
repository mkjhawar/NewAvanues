package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * HoverCard component - Flutter Material parity
 *
 * A Material Design 3 card with hover effects and actions that appear on interaction.
 * Commonly used in interactive grids, dashboards, and portfolio displays.
 *
 * **Web Equivalent:** `HoverCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Elevation change on hover/press
 * - Reveal actions on hover/press
 * - Scale/zoom animation
 * - Overlay with additional info
 * - Quick action buttons
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility (actions always available)
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * HoverCard(
 *     imageUrl = "https://example.com/project.jpg",
 *     title = "Project Alpha",
 *     description = "A revolutionary new approach",
 *     hoverElevation = 8f,
 *     scaleOnHover = 1.05f,
 *     actions = listOf(
 *         HoverCardAction("edit", "Edit", "edit"),
 *         HoverCardAction("share", "Share", "share"),
 *         HoverCardAction("delete", "Delete", "delete")
 *     ),
 *     onActionPressed = { actionId ->
 *         // Handle action
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property imageUrl Optional background/featured image
 * @property title Card title
 * @property description Optional description text
 * @property elevation Default elevation
 * @property hoverElevation Elevation when hovered/pressed
 * @property scaleOnHover Scale factor on hover (1.0 = no scale)
 * @property showOverlay Whether to show overlay on hover
 * @property overlayColor Optional overlay color
 * @property actions List of quick actions
 * @property actionsPosition Position of actions (bottom, top, center)
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when card is pressed (not serialized)
 * @property onActionPressed Callback invoked when action is pressed (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class HoverCard(
    override val type: String = "HoverCard",
    override val id: String? = null,
    val imageUrl: String? = null,
    val title: String,
    val description: String? = null,
    val elevation: Float = 1f,
    val hoverElevation: Float = 4f,
    val scaleOnHover: Float = 1.0f,
    val showOverlay: Boolean = true,
    val overlayColor: String? = null,
    val actions: List<HoverCardAction> = emptyList(),
    val actionsPosition: ActionsPosition = ActionsPosition.Bottom,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onActionPressed: ((String) -> Unit)? = null,
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
        val base = contentDescription ?: title
        val desc = if (description != null) ", $description" else ""
        val actionsInfo = if (actions.isNotEmpty()) ", ${actions.size} actions available" else ""
        return "$base$desc$actionsInfo"
    }

    /**
     * Hover card action
     */
    data class HoverCardAction(
        val id: String,
        val label: String,
        val icon: String? = null,
        val enabled: Boolean = true
    )

    /**
     * Actions position
     */
    enum class ActionsPosition {
        /** Actions at top */
        Top,

        /** Actions at center */
        Center,

        /** Actions at bottom */
        Bottom
    }

    companion object {
        /**
         * Create a simple hover card
         */
        fun simple(
            title: String,
            description: String? = null,
            imageUrl: String? = null,
            onPressed: (() -> Unit)? = null
        ) = HoverCard(
            title = title,
            description = description,
            imageUrl = imageUrl,
            onPressed = onPressed
        )

        /**
         * Create a hover card with actions
         */
        fun withActions(
            title: String,
            description: String? = null,
            imageUrl: String? = null,
            actions: List<HoverCardAction>,
            onActionPressed: ((String) -> Unit)? = null
        ) = HoverCard(
            title = title,
            description = description,
            imageUrl = imageUrl,
            actions = actions,
            onActionPressed = onActionPressed
        )
    }
}
