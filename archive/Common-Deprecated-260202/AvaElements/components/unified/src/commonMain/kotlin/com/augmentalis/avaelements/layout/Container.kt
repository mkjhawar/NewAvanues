package com.augmentalis.avaelements.layout

import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.avaelements.core.types.Alignment

/**
 * Container component for wrapping and styling child components
 *
 * A flexible container that can hold a single child component and apply
 * styling, padding, alignment, and other layout properties.
 *
 * @property id Unique identifier for the component
 * @property child Child component to contain
 * @property alignment Alignment of child within container
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Container(
    override val type: String = "Container",
    override val id: String? = null,
    val child: Component? = null,
    val alignment: Alignment = Alignment.Center,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    companion object {
        /**
         * Create a container with centered child
         */
        fun centered(
            child: Component,
            style: ComponentStyle? = null
        ) = Container(
            child = child,
            alignment = Alignment.Center,
            style = style
        )

        /**
         * Create a padded container
         */
        fun padded(
            child: Component,
            padding: Float,
            style: ComponentStyle? = null
        ) = Container(
            child = child,
            style = style,
            modifiers = listOf(Modifier.Padding(com.augmentalis.avaelements.core.types.Spacing.all(padding)))
        )
    }
}
