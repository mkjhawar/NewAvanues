package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * HoverCard component - Flutter Material parity
 *
 * A Material Design 3 hover-triggered card that displays additional information.
 * Shows rich content when hovering over or focusing on a trigger element.
 *
 * **Web Equivalent:** `Popover` on hover (MUI), `HoverCard` (Radix UI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Hover-triggered display (with touch fallback)
 * - Rich content support (text, icons, actions)
 * - Auto-positioning to stay in viewport
 * - Configurable delay before showing
 * - Smooth fade-in/out transitions
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * HoverCard(
 *     triggerContent = "Hover me",
 *     cardTitle = "Additional Info",
 *     cardContent = "This is detailed information shown on hover",
 *     showDelay = 500,
 *     hideDelay = 200,
 *     position = HoverCard.Position.Top
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property triggerContent Content that triggers the hover card
 * @property cardTitle Title displayed in hover card
 * @property cardContent Main content in hover card
 * @property cardIcon Optional icon shown in card
 * @property showDelay Delay before showing (ms)
 * @property hideDelay Delay before hiding (ms)
 * @property position Preferred position relative to trigger
 * @property width Optional fixed width in dp
 * @property maxWidth Maximum width in dp
 * @property elevation Shadow elevation in dp
 * @property actions Optional list of action buttons
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class HoverCard(
    override val type: String = "HoverCard",
    override val id: String? = null,
    val triggerContent: String,
    val cardTitle: String,
    val cardContent: String,
    val cardIcon: String? = null,
    val showDelay: Int = 500,
    val hideDelay: Int = 200,
    val position: Position = Position.Top,
    val width: Float? = null,
    val maxWidth: Float = 300f,
    val elevation: Float = 4f,
    val actions: List<Action> = emptyList(),
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * HoverCard positioning relative to trigger
     */
    enum class Position {
        Top, Bottom, Left, Right, Auto
    }

    /**
     * Action button for hover card
     */
    data class Action(
        val label: String,
        @Transient
        val onClick: (() -> Unit)? = null
    )

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Hover card"
        val actionsInfo = if (actions.isNotEmpty()) ", ${actions.size} actions available" else ""
        return "$base: $cardTitle. $cardContent$actionsInfo"
    }

    /**
     * Validate timing parameters
     */
    fun areTimingsValid(): Boolean {
        return showDelay >= 0 && showDelay <= 5000 &&
               hideDelay >= 0 && hideDelay <= 5000
    }

    /**
     * Validate dimensions
     */
    fun areDimensionsValid(): Boolean {
        return maxWidth > 0f &&
               (width == null || width > 0f) &&
               elevation >= 0f
    }

    companion object {
        /** Default show delay in milliseconds */
        const val DEFAULT_SHOW_DELAY = 500

        /** Default hide delay in milliseconds */
        const val DEFAULT_HIDE_DELAY = 200

        /** Default max width */
        const val DEFAULT_MAX_WIDTH = 300f

        /**
         * Create a simple hover card
         */
        fun simple(
            triggerContent: String,
            cardTitle: String,
            cardContent: String
        ) = HoverCard(
            triggerContent = triggerContent,
            cardTitle = cardTitle,
            cardContent = cardContent
        )

        /**
         * Create a hover card with icon
         */
        fun withIcon(
            triggerContent: String,
            cardTitle: String,
            cardContent: String,
            icon: String
        ) = HoverCard(
            triggerContent = triggerContent,
            cardTitle = cardTitle,
            cardContent = cardContent,
            cardIcon = icon
        )

        /**
         * Create a hover card with actions
         */
        fun withActions(
            triggerContent: String,
            cardTitle: String,
            cardContent: String,
            actions: List<Action>
        ) = HoverCard(
            triggerContent = triggerContent,
            cardTitle = cardTitle,
            cardContent = cardContent,
            actions = actions
        )

        /**
         * Create a quick hover card (shorter delays)
         */
        fun quick(
            triggerContent: String,
            cardTitle: String,
            cardContent: String
        ) = HoverCard(
            triggerContent = triggerContent,
            cardTitle = cardTitle,
            cardContent = cardContent,
            showDelay = 200,
            hideDelay = 100
        )
    }
}
