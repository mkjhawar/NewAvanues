package com.augmentalis.avaelements.components.phase1.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Transient

/**
 * List component for displaying collections of items
 *
 * A performant list component that renders collections of items efficiently.
 * Supports vertical and horizontal layouts, dividers, and item click handling.
 *
 * @property id Unique identifier for the component
 * @property items List of components to display
 * @property direction Layout direction (vertical or horizontal)
 * @property spacing Space between items
 * @property showDivider Whether to show dividers between items
 * @property onItemClick Callback invoked when item is clicked (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class List(
    override val type: String = "List",
    override val id: String? = null,
    val items: kotlin.collections.List<Component> = emptyList(),
    val direction: Direction = Direction.Vertical,
    val spacing: Spacing = Spacing.Zero,
    val showDivider: Boolean = false,
    @Transient
    val onItemClick: ((Int, Component) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: kotlin.collections.List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * List direction enumeration
     */
    
    enum class Direction {
        /** Vertical list (items stacked top to bottom) */
        Vertical,

        /** Horizontal list (items arranged left to right) */
        Horizontal
    }

    companion object {
        /**
         * Create a vertical list
         */
        fun vertical(
            items: kotlin.collections.List<Component>,
            spacing: Float = 0f,
            showDivider: Boolean = false
        ) = List(
            items = items,
            direction = Direction.Vertical,
            spacing = Spacing.all(spacing),
            showDivider = showDivider
        )

        /**
         * Create a horizontal list
         */
        fun horizontal(
            items: kotlin.collections.List<Component>,
            spacing: Float = 0f
        ) = List(
            items = items,
            direction = Direction.Horizontal,
            spacing = Spacing.all(spacing),
            showDivider = false
        )

        /**
         * Create a vertical list with dividers
         */
        fun withDividers(
            items: kotlin.collections.List<Component>,
            spacing: Float = 0f
        ) = List(
            items = items,
            direction = Direction.Vertical,
            spacing = Spacing.all(spacing),
            showDivider = true
        )
    }
}
