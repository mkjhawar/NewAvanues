package com.augmentalis.avanueui.ui.core.layout

import com.augmentalis.avanueui.core.*

/**
 * Scaffold - App structure foundation with AppBar, FAB, Drawer coordination
 */
data class ScaffoldComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val topBar: Component? = null,
    val bottomBar: Component? = null,
    val floatingActionButton: Component? = null,
    val floatingActionButtonPosition: FabPosition = FabPosition.END,
    val drawer: Component? = null,
    val content: Component
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

enum class FabPosition {
    START, CENTER, END
}

/**
 * LazyColumn - Virtualized vertical scrolling for large lists
 */
data class LazyColumnComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<Component> = emptyList(),
    val verticalArrangement: VerticalArrangement = VerticalArrangement.Top,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Start,
    val reverseLayout: Boolean = false
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * LazyRow - Virtualized horizontal scrolling for large lists
 */
data class LazyRowComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<Component> = emptyList(),
    val horizontalArrangement: HorizontalArrangement = HorizontalArrangement.Start,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
    val reverseLayout: Boolean = false
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Spacer - Fixed-size space between elements
 */
data class SpacerComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val width: Float? = null,
    val height: Float? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Box - Basic container with sizing and positioning
 */
data class BoxComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val contentAlignment: ContentAlignment = ContentAlignment.TopStart,
    val children: List<Component> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Surface - Elevation and theming foundation
 */
data class SurfaceComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val color: Color? = null,
    val contentColor: Color? = null,
    val tonalElevation: Float = 0f,
    val shadowElevation: Float = 0f,
    val shape: Shape = Shape.Rectangle,
    val child: Component? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

enum class Shape {
    Rectangle, RoundedSmall, RoundedMedium, RoundedLarge, Circle
}

enum class ContentAlignment {
    TopStart, TopCenter, TopEnd,
    CenterStart, Center, CenterEnd,
    BottomStart, BottomCenter, BottomEnd
}
