package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Popover component - Flutter Material parity
 *
 * A Material Design 3 popover that displays contextual information attached to an anchor element.
 * Similar to a tooltip but supports rich content including text, images, and actions.
 *
 * **Web Equivalent:** `Popover` (MUI)
 * **Material Design 3:** https://m3.material.io/components/menus/overview
 *
 * ## Features
 * - Attaches to anchor element
 * - Supports rich content (not just text)
 * - Configurable positioning (top, bottom, left, right)
 * - Auto-dismissal on outside click
 * - Arrow pointer to anchor
 * - Material3 elevation and theming
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Popover(
 *     anchorId = "infoButton",
 *     content = "This is detailed information about the feature.",
 *     title = "Feature Info",
 *     position = PopoverPosition.BOTTOM,
 *     showArrow = true,
 *     actions = listOf(
 *         PopoverAction("Got it", onClick = { /* dismiss */ })
 *     )
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property anchorId ID of the element to attach to
 * @property visible Whether popover is visible
 * @property title Optional title text
 * @property content Main content text or description
 * @property position Positioning relative to anchor (top, bottom, left, right)
 * @property showArrow Whether to show arrow pointer
 * @property dismissible Whether clicking outside dismisses
 * @property maxWidth Maximum width in dp (default 280)
 * @property elevation Elevation level (0-5)
 * @property actions Optional list of action buttons
 * @property onDismiss Callback when popover is dismissed
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class Popover(
    override val type: String = "Popover",
    override val id: String? = null,
    val anchorId: String,
    val visible: Boolean = false,
    val title: String? = null,
    val content: String,
    val position: PopoverPosition = PopoverPosition.BOTTOM,
    val showArrow: Boolean = true,
    val dismissible: Boolean = true,
    val maxWidth: Float = 280f,
    val elevation: Int = 3,
    val actions: List<PopoverAction> = emptyList(),
    val onDismiss: (() -> Unit)? = null,
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
        val titlePart = title?.let { "$it. " } ?: ""
        val positionText = when (position) {
            PopoverPosition.TOP -> "above"
            PopoverPosition.BOTTOM -> "below"
            PopoverPosition.LEFT -> "left of"
            PopoverPosition.RIGHT -> "right of"
        }
        return contentDescription ?: "${titlePart}Popover $positionText anchor. $content"
    }

    /**
     * Check if has actions
     */
    fun hasActions(): Boolean = actions.isNotEmpty()

    companion object {
        /**
         * Create a simple info popover
         */
        fun info(
            anchorId: String,
            content: String,
            title: String = "Information",
            position: PopoverPosition = PopoverPosition.BOTTOM
        ) = Popover(
            anchorId = anchorId,
            content = content,
            title = title,
            position = position,
            visible = true
        )

        /**
         * Create a popover with action button
         */
        fun withAction(
            anchorId: String,
            content: String,
            actionLabel: String,
            onAction: () -> Unit,
            title: String? = null
        ) = Popover(
            anchorId = anchorId,
            content = content,
            title = title,
            actions = listOf(PopoverAction(actionLabel, onAction)),
            visible = true
        )
    }
}

/**
 * Popover positioning options
 */
enum class PopoverPosition {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}

/**
 * Popover action button
 */
data class PopoverAction(
    val label: String,
    val onClick: () -> Unit,
    val primary: Boolean = false
)
