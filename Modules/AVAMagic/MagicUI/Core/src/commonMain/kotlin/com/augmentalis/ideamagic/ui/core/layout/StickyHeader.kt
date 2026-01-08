package com.augmentalis.magicui.ui.core.layout

import com.augmentalis.magicui.components.core.*
import com.augmentalis.magicui.components.core.Position

/**
 * Header that sticks to the top/bottom while scrolling.
 *
 * StickyHeader remains visible at the top (or bottom) of its container
 * while content scrolls underneath.
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic sticky header
 * val header = StickyHeaderComponent(
 *     content = "Section A"
 * )
 *
 * // With background and elevation
 * val header = StickyHeaderComponent(
 *     content = "Categories",
 *     style = ComponentStyle(
 *         backgroundColor = Color.White,
 *         elevation = 4f
 *     )
 * )
 *
 * // Bottom sticky header
 * val header = StickyHeaderComponent(
 *     content = "Total: $99.99",
 *     position = Position.BOTTOM
 * )
 *
 * // With nested component
 * val header = StickyHeaderComponent(
 *     content = "My Header",
 *     child = SearchBarComponent()
 * )
 * ```
 *
 * @property content Header text content
 * @property child Optional child component to display
 * @property position TOP or BOTTOM (default TOP)
 * @property zIndex Stacking order (default 100)
 * @property offsetY Vertical offset from position (default 0f)
 * @since 1.0.0
 */
data class StickyHeaderComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val content: String,
    val child: Component? = null,
    val position: Position = Position.TOP,
    val zIndex: Int = 100,
    val offsetY: Float = 0f
) : Component {
    init {
        require(content.isNotBlank()) { "Content cannot be blank" }
        require(position == Position.TOP || position == Position.BOTTOM) {
            "StickyHeader position must be TOP or BOTTOM"
        }
        require(zIndex >= 0) { "zIndex must be non-negative (got $zIndex)" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    companion object {
        /**
         * Creates a sticky header at the top.
         */
        fun top(content: String, child: Component? = null) =
            StickyHeaderComponent(content = content, child = child, position = Position.TOP)

        /**
         * Creates a sticky header at the bottom.
         */
        fun bottom(content: String, child: Component? = null) =
            StickyHeaderComponent(content = content, child = child, position = Position.BOTTOM)

        /**
         * Creates a sticky section header for lists.
         */
        fun section(title: String) = StickyHeaderComponent(
            content = title,
            style = ComponentStyle(
                padding = Spacing.symmetric(
                    vertical = 8f,
                    horizontal = 16f
                ),
                backgroundColor = Color.White
            )
        )
    }
}
