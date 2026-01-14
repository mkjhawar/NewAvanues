package com.augmentalis.avaelements.components.phase1.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ScrollView component for scrollable content
 *
 * A container that allows its child content to be scrolled when it exceeds
 * the available space. Supports both vertical and horizontal scrolling.
 *
 * @property id Unique identifier for the component
 * @property child Child component to make scrollable
 * @property direction Scroll direction (vertical or horizontal)
 * @property showScrollbar Whether to show scrollbar
 * @property scrollbarAlwaysVisible Whether scrollbar is always visible or auto-hide
 * @property onScroll Callback invoked when scroll position changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class ScrollView(
    override val type: String = "ScrollView",
    override val id: String? = null,
    val child: Component? = null,
    val direction: Direction = Direction.Vertical,
    val showScrollbar: Boolean = true,
    val scrollbarAlwaysVisible: Boolean = false,
    @Transient
    val onScroll: ((Float, Float) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Scroll direction enumeration
     */
    
    enum class Direction {
        /** Scroll vertically (up/down) */
        Vertical,

        /** Scroll horizontally (left/right) */
        Horizontal,

        /** Scroll both directions */
        Both
    }

    companion object {
        /**
         * Create a vertical scroll view
         */
        fun vertical(
            child: Component,
            showScrollbar: Boolean = true
        ) = ScrollView(
            child = child,
            direction = Direction.Vertical,
            showScrollbar = showScrollbar
        )

        /**
         * Create a horizontal scroll view
         */
        fun horizontal(
            child: Component,
            showScrollbar: Boolean = true
        ) = ScrollView(
            child = child,
            direction = Direction.Horizontal,
            showScrollbar = showScrollbar
        )

        /**
         * Create a bidirectional scroll view
         */
        fun both(
            child: Component,
            showScrollbar: Boolean = true
        ) = ScrollView(
            child = child,
            direction = Direction.Both,
            showScrollbar = showScrollbar
        )
    }
}
