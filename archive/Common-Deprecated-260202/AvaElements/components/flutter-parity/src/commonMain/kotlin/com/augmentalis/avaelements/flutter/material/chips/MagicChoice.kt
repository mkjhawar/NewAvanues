package com.augmentalis.avaelements.flutter.material.chips

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MagicChoice component - Flutter Material parity
 *
 * A selectable magic tag for single-selection from a set of options (radio button style).
 * Only one tag in a group can be selected at a time.
 *
 * **Flutter Equivalent:** `ChoiceChip`
 * **Material Design 3:** https://m3.material.io/components/chips/overview
 *
 * ## Features
 * - Single-selection within a group
 * - Visual feedback for selected state
 * - Optional leading icon/avatar
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * // In a group:
 * Row {
 *     MagicChoice(
 *         label = "Small",
 *         selected = size == "small",
 *         onSelected = { size = "small" }
 *     )
 *     MagicChoice(
 *         label = "Medium",
 *         selected = size == "medium",
 *         onSelected = { size = "medium" }
 *     )
 *     MagicChoice(
 *         label = "Large",
 *         selected = size == "large",
 *         onSelected = { size = "large" }
 *     )
 * }
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Text label displayed on the tag
 * @property selected Whether this tag is currently selected
 * @property enabled Whether the tag is enabled for user interaction
 * @property avatar Optional leading icon/avatar resource name
 * @property selectedColor Background color when selected
 * @property disabledColor Background color when disabled
 * @property backgroundColor Background color when not selected
 * @property shadowColor Shadow color override
 * @property surfaceTintColor Surface tint color
 * @property selectedShadowColor Shadow color when selected
 * @property showCheckmark Whether to show checkmark when selected
 * @property checkmarkColor Color of the checkmark
 * @property labelStyle Text style for the label
 * @property side Border configuration
 * @property shape Chip shape override
 * @property elevation Chip elevation when not selected
 * @property pressElevation Elevation when pressed
 * @property padding Internal padding override
 * @property visualDensity Visual density adjustment
 * @property materialTapTargetSize Minimum tap target size
 * @property contentDescription Accessibility description for TalkBack
 * @property onSelected Callback invoked when tag is selected (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MagicChoice(
    override val type: String = "MagicChoice",
    override val id: String? = null,
    val label: String,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val avatar: String? = null,
    val selectedColor: String? = null,
    val disabledColor: String? = null,
    val backgroundColor: String? = null,
    val shadowColor: String? = null,
    val surfaceTintColor: String? = null,
    val selectedShadowColor: String? = null,
    val showCheckmark: Boolean = true,
    val checkmarkColor: String? = null,
    val labelStyle: String? = null,
    val side: String? = null,
    val shape: String? = null,
    val elevation: Float? = null,
    val pressElevation: Float? = null,
    val padding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val visualDensity: VisualDensity = VisualDensity.Standard,
    val materialTapTargetSize: MaterialTapTargetSize = MaterialTapTargetSize.ShrinkWrap,
    val contentDescription: String? = null,
    @Transient
    val onSelected: ((Boolean) -> Unit)? = null,
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
         * Create a selected magic choice
         */
        fun selected(
            label: String,
            onSelected: ((Boolean) -> Unit)? = null
        ) = MagicChoice(
            label = label,
            selected = true,
            onSelected = onSelected
        )

        /**
         * Create an unselected magic choice
         */
        fun unselected(
            label: String,
            onSelected: ((Boolean) -> Unit)? = null
        ) = MagicChoice(
            label = label,
            selected = false,
            onSelected = onSelected
        )

        /**
         * Create a magic choice with avatar
         */
        fun withAvatar(
            label: String,
            avatar: String,
            selected: Boolean = false,
            onSelected: ((Boolean) -> Unit)? = null
        ) = MagicChoice(
            label = label,
            avatar = avatar,
            selected = selected,
            onSelected = onSelected
        )
    }
}

/**
 * Type alias for Flutter Material parity
 * ChoiceChip is the Flutter name for this component
 */
typealias ChoiceChip = MagicChoice
