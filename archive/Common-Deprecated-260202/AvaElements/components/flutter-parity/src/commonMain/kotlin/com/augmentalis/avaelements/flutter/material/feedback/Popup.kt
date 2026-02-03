package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Popup component - Flutter Material parity
 *
 * A Material Design 3 floating popup container with flexible positioning and optional arrow pointer.
 * Displays content in a floating layer above other content.
 *
 * **Web Equivalent:** `Popover` (MUI), `Floating UI`
 * **Material Design 3:** https://m3.material.io/components/menus/overview
 *
 * ## Features
 * - Flexible positioning relative to anchor point
 * - Optional arrow/pointer indicator
 * - Auto-positioning to stay in viewport
 * - Custom offset control
 * - Dismissible on outside click or ESC
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Popup(
 *     visible = true,
 *     anchorPosition = Position.BottomCenter,
 *     content = "This is helpful information",
 *     offsetX = 0f,
 *     offsetY = 8f,
 *     showArrow = true,
 *     dismissible = true,
 *     onDismiss = { /* Handle dismiss */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether popup is visible
 * @property anchorPosition Position relative to anchor
 * @property content Content to display in popup
 * @property offsetX Horizontal offset in dp
 * @property offsetY Vertical offset in dp
 * @property showArrow Whether to show arrow pointer
 * @property arrowSize Arrow size in dp
 * @property width Optional fixed width in dp
 * @property maxWidth Maximum width in dp
 * @property elevation Shadow elevation in dp
 * @property dismissible Whether user can dismiss
 * @property backgroundColor Optional background color
 * @property contentDescription Accessibility description for TalkBack
 * @property onDismiss Callback invoked when dismissed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class Popup(
    override val type: String = "Popup",
    override val id: String? = null,
    val visible: Boolean = true,
    val anchorPosition: Position = Position.BottomCenter,
    val content: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 8f,
    val showArrow: Boolean = true,
    val arrowSize: Float = 8f,
    val width: Float? = null,
    val maxWidth: Float = 300f,
    val elevation: Float = 4f,
    val dismissible: Boolean = true,
    val backgroundColor: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onDismiss: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Popup positioning relative to anchor
     */
    enum class Position {
        TopStart, TopCenter, TopEnd,
        BottomStart, BottomCenter, BottomEnd,
        LeftStart, LeftCenter, LeftEnd,
        RightStart, RightCenter, RightEnd
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Popup"
        val dismissInfo = if (dismissible) ", dismissible" else ""
        return "$base: $content$dismissInfo"
    }

    /**
     * Check if dimensions are valid
     */
    fun areDimensionsValid(): Boolean {
        return maxWidth > 0f &&
               (width == null || width > 0f) &&
               arrowSize >= 0f &&
               elevation >= 0f
    }

    companion object {
        /**
         * Create a simple popup below anchor
         */
        fun simple(
            content: String,
            visible: Boolean = true
        ) = Popup(
            content = content,
            visible = visible
        )

        /**
         * Create a tooltip-style popup
         */
        fun tooltip(
            content: String,
            visible: Boolean = true,
            position: Position = Position.TopCenter
        ) = Popup(
            content = content,
            visible = visible,
            anchorPosition = position,
            showArrow = true,
            dismissible = true
        )

        /**
         * Create a non-dismissible popup
         */
        fun persistent(
            content: String,
            visible: Boolean = true,
            position: Position = Position.BottomCenter
        ) = Popup(
            content = content,
            visible = visible,
            anchorPosition = position,
            dismissible = false
        )
    }
}
