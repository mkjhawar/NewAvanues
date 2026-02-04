package com.augmentalis.avaelements.flutter.material.chips

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * InputChip component - Flutter Material parity
 *
 * A chip with avatar and delete action, typically used to represent user input
 * (e.g., tags, contacts, selected items).
 *
 * **Flutter Equivalent:** `InputChip`
 * **Material Design 3:** https://m3.material.io/components/chips/overview
 *
 * ## Features
 * - Optional leading avatar
 * - Delete/remove action button
 * - Selectable state
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Keyboard navigation support
 *
 * ## Usage Example
 * ```kotlin
 * MagicInput(
 *     label = "John Doe",
 *     avatar = "user_avatar",
 *     selected = false,
 *     onDeleted = {
 *         // Remove this chip
 *     },
 *     onSelected = { selected ->
 *         // Handle selection change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Text label displayed on the chip
 * @property avatar Optional leading icon/avatar resource name
 * @property selected Whether the chip is currently selected
 * @property enabled Whether the chip is enabled for user interaction
 * @property deleteIcon Icon to show for delete action
 * @property deleteIconColor Color of the delete icon
 * @property deleteButtonTooltipMessage Tooltip for delete button
 * @property useDeleteButtonTooltip Whether to show delete button tooltip
 * @property isEnabled Whether delete and press actions are enabled
 * @property selectedColor Background color when selected
 * @property disabledColor Background color when disabled
 * @property backgroundColor Background color when not selected
 * @property shadowColor Shadow color override
 * @property surfaceTintColor Surface tint color
 * @property selectedShadowColor Shadow color when selected
 * @property showCheckmark Whether to show checkmark when selected
 * @property checkmarkColor Color of the checkmark
 * @property side Border configuration
 * @property shape Chip shape override
 * @property elevation Chip elevation when not selected
 * @property pressElevation Elevation when pressed
 * @property padding Internal padding override
 * @property labelPadding Padding around the label
 * @property visualDensity Visual density adjustment
 * @property materialTapTargetSize Minimum tap target size
 * @property clipBehavior How to clip the chip content
 * @property focusNode Focus node for managing focus
 * @property autofocus Whether to autofocus this chip
 * @property contentDescription Accessibility description for TalkBack
 * @property onSelected Callback invoked when chip selection changes (not serialized)
 * @property onDeleted Callback invoked when delete button is pressed (not serialized)
 * @property onPressed Callback invoked when chip is pressed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MagicInput(
    override val type: String = "MagicInput",
    override val id: String? = null,
    val label: String,
    val avatar: String? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val deleteIcon: String? = "close",
    val deleteIconColor: String? = null,
    val deleteButtonTooltipMessage: String? = null,
    val useDeleteButtonTooltip: Boolean = true,
    val isEnabled: Boolean = true,
    val selectedColor: String? = null,
    val disabledColor: String? = null,
    val backgroundColor: String? = null,
    val shadowColor: String? = null,
    val surfaceTintColor: String? = null,
    val selectedShadowColor: String? = null,
    val showCheckmark: Boolean = true,
    val checkmarkColor: String? = null,
    val side: String? = null,
    val shape: String? = null,
    val elevation: Float? = null,
    val pressElevation: Float? = null,
    val padding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val labelPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val visualDensity: VisualDensity = VisualDensity.Standard,
    val materialTapTargetSize: MaterialTapTargetSize = MaterialTapTargetSize.ShrinkWrap,
    val clipBehavior: ClipBehavior = ClipBehavior.None,
    val focusNode: String? = null,
    val autofocus: Boolean = false,
    val contentDescription: String? = null,
    @Transient
    val onSelected: ((Boolean) -> Unit)? = null,
    @Transient
    val onDeleted: (() -> Unit)? = null,
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
     * Combines label with selection state for TalkBack
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label
        val state = if (selected) "selected" else "not selected"
        return "$base, $state"
    }

    /**
     * Get delete button accessibility description
     */
    fun getDeleteButtonAccessibilityDescription(): String {
        return deleteButtonTooltipMessage ?: "Remove $label"
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

    /**
     * Clip behavior for chip content
     */
    enum class ClipBehavior {
        None,
        HardEdge,
        AntiAlias,
        AntiAliasWithSaveLayer
    }

    companion object {
        /**
         * Create a simple input chip with delete action
         */
        fun simple(
            label: String,
            onDeleted: (() -> Unit)? = null
        ) = MagicInput(
            label = label,
            onDeleted = onDeleted
        )

        /**
         * Create an input chip with avatar
         */
        fun withAvatar(
            label: String,
            avatar: String,
            onDeleted: (() -> Unit)? = null
        ) = MagicInput(
            label = label,
            avatar = avatar,
            onDeleted = onDeleted
        )

        /**
         * Create a selectable input chip
         */
        fun selectable(
            label: String,
            selected: Boolean = false,
            onSelected: ((Boolean) -> Unit)? = null,
            onDeleted: (() -> Unit)? = null
        ) = MagicInput(
            label = label,
            selected = selected,
            onSelected = onSelected,
            onDeleted = onDeleted
        )

        /**
         * Create an input chip with avatar and selection
         */
        fun full(
            label: String,
            avatar: String,
            selected: Boolean = false,
            onSelected: ((Boolean) -> Unit)? = null,
            onDeleted: (() -> Unit)? = null
        ) = MagicInput(
            label = label,
            avatar = avatar,
            selected = selected,
            onSelected = onSelected,
            onDeleted = onDeleted
        )
    }
}

/**
 * Type alias for Flutter Material parity
 * InputChip is the Flutter name for this component
 */
typealias InputChip = MagicInput
