package com.augmentalis.avaelements.flutter.material.chips

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ActionChip component - Flutter Material parity
 *
 * A compact button-like chip that triggers an action when pressed.
 * Similar to a button but with chip styling.
 *
 * **Flutter Equivalent:** `ActionChip`
 * **Material Design 3:** https://m3.material.io/components/chips/overview
 *
 * ## Features
 * - Compact, button-like appearance
 * - Optional leading icon/avatar
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MagicAction(
 *     label = "Send",
 *     avatar = "send_icon",
 *     onPressed = {
 *         // Handle action
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Text label displayed on the chip
 * @property avatar Optional leading icon/avatar resource name
 * @property tooltip Optional tooltip text
 * @property enabled Whether the chip is enabled for user interaction
 * @property backgroundColor Background color override
 * @property disabledColor Background color when disabled
 * @property shadowColor Shadow color override
 * @property surfaceTintColor Surface tint color
 * @property elevation Chip elevation (shadow depth)
 * @property pressElevation Elevation when pressed
 * @property side Border configuration
 * @property shape Chip shape override
 * @property clipBehavior How to clip the chip content
 * @property focusNode Focus node for managing focus
 * @property autofocus Whether to autofocus this chip
 * @property padding Internal padding override
 * @property visualDensity Visual density adjustment
 * @property materialTapTargetSize Minimum tap target size
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when chip is pressed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MagicAction(
    override val type: String = "MagicAction",
    override val id: String? = null,
    val label: String,
    val avatar: String? = null,
    val tooltip: String? = null,
    val enabled: Boolean = true,
    val backgroundColor: String? = null,
    val disabledColor: String? = null,
    val shadowColor: String? = null,
    val surfaceTintColor: String? = null,
    val elevation: Float? = null,
    val pressElevation: Float? = null,
    val side: String? = null,
    val shape: String? = null,
    val clipBehavior: ClipBehavior = ClipBehavior.None,
    val focusNode: String? = null,
    val autofocus: Boolean = false,
    val padding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val visualDensity: VisualDensity = VisualDensity.Standard,
    val materialTapTargetSize: MaterialTapTargetSize = MaterialTapTargetSize.ShrinkWrap,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: tooltip ?: label
    }

    /**
     * Clip behavior for chip content
     */
    enum class ClipBehavior {
        None,
        HardEdge,
        AntiAlias,
        AntiAliasWithSaveLayer
    }

    /**
     * Visual density adjustment
     */
    enum class VisualDensity {
        Compact,
        Standard,
        Comfortable
    }

    /**
     * Material tap target size
     */
    enum class MaterialTapTargetSize {
        /** 48dp minimum (Material guideline) */
        PadOrExpand,

        /** Actual size, may be smaller */
        ShrinkWrap
    }

    companion object {
        /**
         * Create a simple action chip
         */
        fun simple(
            label: String,
            onPressed: (() -> Unit)? = null
        ) = MagicAction(
            label = label,
            onPressed = onPressed
        )

        /**
         * Create an action chip with avatar
         */
        fun withAvatar(
            label: String,
            avatar: String,
            onPressed: (() -> Unit)? = null
        ) = MagicAction(
            label = label,
            avatar = avatar,
            onPressed = onPressed
        )

        /**
         * Create an action chip with tooltip
         */
        fun withTooltip(
            label: String,
            tooltip: String,
            onPressed: (() -> Unit)? = null
        ) = MagicAction(
            label = label,
            tooltip = tooltip,
            onPressed = onPressed
        )
    }
}

/**
 * Type alias for Flutter Material parity
 * ActionChip is the Flutter name for this component
 */
typealias ActionChip = MagicAction
