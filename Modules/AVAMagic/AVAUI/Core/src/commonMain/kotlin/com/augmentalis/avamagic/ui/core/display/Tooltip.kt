package com.augmentalis.avamagic.ui.core.display
import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.Position
data class TooltipComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val text: String, val position: Position = Position.TOP, val child: Component? = null) : Component {
    init { require(text.isNotBlank()) { "Tooltip text cannot be blank" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
