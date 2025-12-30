package com.augmentalis.avaelements.components.phase1.layout

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.avaelements.core.types.Alignment
import com.augmentalis.avaelements.core.types.Arrangement
import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Transient

/**
 * Row component for horizontal layout
 *
 * Arranges child components horizontally (left to right by default).
 * Supports various arrangement strategies (start, center, end, space-between, etc.)
 * and vertical alignment options.
 *
 * @property id Unique identifier for the component
 * @property children List of child components to arrange horizontally
 * @property horizontalArrangement How children are distributed horizontally
 * @property verticalAlignment How children are aligned vertically
 * @property spacing Space between children
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Row(
    override val type: String = "Row",
    override val id: String? = null,
    val children: List<Component> = emptyList(),
    val horizontalArrangement: Arrangement = Arrangement.Start,
    val verticalAlignment: Alignment = Alignment.CenterStart,
    val spacing: Spacing = Spacing.Zero,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    companion object {
        /**
         * Create a row with centered children
         */
        fun centered(
            children: List<Component>,
            spacing: Float = 0f
        ) = Row(
            children = children,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Center,
            spacing = Spacing.all(spacing)
        )

        /**
         * Create a row with space between children
         */
        fun spaceBetween(
            children: List<Component>,
            verticalAlignment: Alignment = Alignment.CenterStart
        ) = Row(
            children = children,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = verticalAlignment
        )

        /**
         * Create a row with evenly spaced children
         */
        fun spaceEvenly(
            children: List<Component>,
            verticalAlignment: Alignment = Alignment.CenterStart
        ) = Row(
            children = children,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = verticalAlignment
        )
    }
}
