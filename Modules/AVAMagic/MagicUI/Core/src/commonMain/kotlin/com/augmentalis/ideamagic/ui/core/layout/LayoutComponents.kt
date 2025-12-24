package com.augmentalis.magicui.ui.core.layout

import com.augmentalis.magicui.components.core.*

/**
 * Column layout component - arranges children vertically
 */
data class ColumnComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val children: List<Component> = emptyList(),
    val spacing: Float = 0f,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Start,
    val verticalArrangement: VerticalArrangement = VerticalArrangement.Top,
    val arrangement: Arrangement = Arrangement.Top // For mapper compatibility
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Row layout component - arranges children horizontally
 */
data class RowComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val children: List<Component> = emptyList(),
    val spacing: Float = 0f,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
    val horizontalArrangement: HorizontalArrangement = HorizontalArrangement.Start,
    val arrangement: Arrangement = Arrangement.Start // For mapper compatibility
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Container component - basic box for grouping and styling content
 */
data class ContainerComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val child: Component? = null,
    val children: List<Component> = emptyList(),
    val contentAlignment: Alignment = Alignment.TopStart
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * ScrollView component - scrollable container
 */
data class ScrollViewComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val child: Component? = null, // For mapper compatibility
    val children: List<Component> = emptyList(),
    val direction: ScrollDirection = ScrollDirection.Vertical,
    val orientation: Orientation = Orientation.Vertical, // For mapper compatibility
    val showScrollbar: Boolean = true,
    val bounces: Boolean = true
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Card component - elevated container with rounded corners
 */
data class CardComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val child: Component? = null,
    val children: List<Component> = emptyList(),
    val elevation: Float = 4f,
    val cornerRadius: Float = 8f,
    val outlined: Boolean = false
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

// Alignment enums

enum class HorizontalAlignment {
    Start,
    Center,
    End
}

enum class VerticalAlignment {
    Top,
    Center,
    Bottom
}

enum class HorizontalArrangement {
    Start,
    Center,
    End,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

enum class VerticalArrangement {
    Top,
    Center,
    Bottom,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

enum class ScrollDirection {
    Vertical,
    Horizontal,
    Both
}

// Unified Arrangement enum for mappers
enum class Arrangement {
    Start,
    Center,
    End,
    Top,
    Bottom,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

// Orientation enum for ScrollView mapper
enum class Orientation {
    Vertical,
    Horizontal
}
