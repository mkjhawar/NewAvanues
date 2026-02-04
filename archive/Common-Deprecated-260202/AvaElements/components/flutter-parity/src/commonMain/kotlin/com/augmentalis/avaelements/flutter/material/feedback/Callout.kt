package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Callout component - Flutter Material parity
 *
 * A Material Design 3 highlighted callout box with directional arrow pointer.
 * Used for important notifications, tips, or contextual help.
 *
 * **Web Equivalent:** `Alert` with arrow (MUI), `Callout` (Ant Design)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Directional arrow on any side (top, bottom, left, right)
 * - Title and body content
 * - Optional icon
 * - Colored background based on variant
 * - Optional close button
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Callout(
 *     title = "Pro Tip",
 *     message = "You can use keyboard shortcuts to navigate faster",
 *     variant = Callout.Variant.Info,
 *     arrowPosition = Callout.ArrowPosition.Top,
 *     icon = "lightbulb",
 *     dismissible = true,
 *     onDismiss = { /* Handle dismiss */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Callout title
 * @property message Callout message content
 * @property variant Visual variant (info, success, warning, error)
 * @property arrowPosition Position of arrow pointer
 * @property icon Optional icon name
 * @property dismissible Whether to show close button
 * @property elevation Shadow elevation in dp
 * @property contentDescription Accessibility description for TalkBack
 * @property onDismiss Callback invoked when dismissed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class Callout(
    override val type: String = "Callout",
    override val id: String? = null,
    val title: String,
    val message: String,
    val variant: Variant = Variant.Info,
    val arrowPosition: ArrowPosition = ArrowPosition.Top,
    val icon: String? = null,
    val dismissible: Boolean = false,
    val elevation: Float = 2f,
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
     * Callout visual variants
     */
    enum class Variant {
        Info,
        Success,
        Warning,
        Error
    }

    /**
     * Arrow position on callout box
     */
    enum class ArrowPosition {
        Top, Bottom, Left, Right, None
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Callout"
        val variantText = variant.name.lowercase()
        val dismissInfo = if (dismissible) ", dismissible" else ""
        return "$base: $variantText, $title, $message$dismissInfo"
    }

    /**
     * Get default icon for variant if none specified
     */
    fun getEffectiveIcon(): String? {
        return icon ?: when (variant) {
            Variant.Info -> "info"
            Variant.Success -> "check_circle"
            Variant.Warning -> "warning"
            Variant.Error -> "error"
        }
    }

    companion object {
        /**
         * Create an info callout
         */
        fun info(
            title: String,
            message: String,
            arrowPosition: ArrowPosition = ArrowPosition.Top
        ) = Callout(
            title = title,
            message = message,
            variant = Variant.Info,
            arrowPosition = arrowPosition
        )

        /**
         * Create a success callout
         */
        fun success(
            title: String,
            message: String,
            arrowPosition: ArrowPosition = ArrowPosition.Top
        ) = Callout(
            title = title,
            message = message,
            variant = Variant.Success,
            arrowPosition = arrowPosition
        )

        /**
         * Create a warning callout
         */
        fun warning(
            title: String,
            message: String,
            arrowPosition: ArrowPosition = ArrowPosition.Top
        ) = Callout(
            title = title,
            message = message,
            variant = Variant.Warning,
            arrowPosition = arrowPosition
        )

        /**
         * Create an error callout
         */
        fun error(
            title: String,
            message: String,
            arrowPosition: ArrowPosition = ArrowPosition.Top
        ) = Callout(
            title = title,
            message = message,
            variant = Variant.Error,
            arrowPosition = arrowPosition
        )
    }
}
