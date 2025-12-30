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
 * Column component for vertical layout
 *
 * Arranges child components vertically (top to bottom by default).
 * Supports various arrangement strategies (start, center, end, space-between, etc.)
 * and horizontal alignment options.
 *
 * @property id Unique identifier for the component
 * @property children List of child components to arrange vertically
 * @property verticalArrangement How children are distributed vertically
 * @property horizontalAlignment How children are aligned horizontally
 * @property spacing Space between children
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Column(
    override val type: String = "Column",
    override val id: String? = null,
    val children: List<Component> = emptyList(),
    val verticalArrangement: Arrangement = Arrangement.Start,
    val horizontalAlignment: Alignment = Alignment.CenterStart,
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
         * Create a column with centered children
         */
        fun centered(
            children: List<Component>,
            spacing: Float = 0f
        ) = Column(
            children = children,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Center,
            spacing = Spacing.all(spacing)
        )

        /**
         * Create a column with space between children
         */
        fun spaceBetween(
            children: List<Component>,
            horizontalAlignment: Alignment = Alignment.CenterStart
        ) = Column(
            children = children,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = horizontalAlignment
        )

        /**
         * Create a column with evenly spaced children
         */
        fun spaceEvenly(
            children: List<Component>,
            horizontalAlignment: Alignment = Alignment.CenterStart
        ) = Column(
            children = children,
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = horizontalAlignment
        )
    }
}
