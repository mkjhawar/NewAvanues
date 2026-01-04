package com.augmentalis.magicui.ui.core.layout
import com.augmentalis.magicui.components.core.*
data class MasonryGridComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val columns: Int = 2, val gap: Float = 8f, val children: List<Component> = emptyList()) : Component {
    init { require(columns > 0) { "columns must be positive" }; require(gap >= 0) { "gap must be non-negative" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun addChild(child: Component): MasonryGridComponent = copy(children = children + child)
    companion object { fun twoColumn(gap: Float = 8f) = MasonryGridComponent(columns = 2, gap = gap); fun threeColumn(gap: Float = 8f) = MasonryGridComponent(columns = 3, gap = gap) }
}
