package com.augmentalis.avaelements.flutter.material.chips

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MagicFilter component - Flutter Material parity
 *
 * A selectable magic tag for filtering content, following Material Design 3 specifications.
 * When selected, displays a checkmark and applies selected styling.
 *
 * **Flutter Equivalent:** `FilterChip`
 * **Material Design 3:** https://m3.material.io/components/chips/overview
 *
 * ## Features
 * - Selection state with visual feedback
 * - Checkmark indicator when selected
 * - Optional leading avatar/icon
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MagicFilter(
 *     label = "Category",
 *     selected = true,
 *     onSelected = { selected ->
 *         // Handle selection change
 *     },
 *     enabled = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Text label displayed on the tag
 * @property selected Whether the tag is currently selected
 * @property enabled Whether the tag is enabled for user interaction
 * @property showCheckmark Whether to show checkmark when selected (default: true)
 * @property avatar Optional avatar/icon resource name
 * @property contentDescription Accessibility description for TalkBack
 * @property onSelected Callback invoked when selection changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MagicFilter(
    override val type: String = "MagicFilter",
    override val id: String? = null,
    val label: String,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val showCheckmark: Boolean = true,
    val avatar: String? = null,
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

    companion object {
        /**
         * Create a selected magic filter
         */
        fun selected(
            label: String,
            onSelected: ((Boolean) -> Unit)? = null
        ) = MagicFilter(
            label = label,
            selected = true,
            onSelected = onSelected
        )

        /**
         * Create an unselected magic filter
         */
        fun unselected(
            label: String,
            onSelected: ((Boolean) -> Unit)? = null
        ) = MagicFilter(
            label = label,
            selected = false,
            onSelected = onSelected
        )

        /**
         * Create a magic filter with avatar
         */
        fun withAvatar(
            label: String,
            avatar: String,
            selected: Boolean = false,
            onSelected: ((Boolean) -> Unit)? = null
        ) = MagicFilter(
            label = label,
            avatar = avatar,
            selected = selected,
            onSelected = onSelected
        )
    }
}

/**
 * Type alias for Flutter Material parity
 * FilterChip is the Flutter name for this component
 */
typealias FilterChip = MagicFilter
