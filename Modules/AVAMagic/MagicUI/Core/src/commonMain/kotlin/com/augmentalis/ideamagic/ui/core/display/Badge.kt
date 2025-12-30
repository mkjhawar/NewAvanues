package com.augmentalis.magicui.ui.core.display
import com.augmentalis.magicui.components.core.*
data class BadgeComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val content: String, val color: Color = Color.Blue, val size: ComponentSize = ComponentSize.SM, val dot: Boolean = false) : Component {
    init { require(content.isNotBlank()) { "Badge content cannot be blank" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    companion object { fun notification(count: Int) = BadgeComponent(content = if (count > 99) "99+" else count.toString(), color = Color.Red); fun status(text: String, color: Color) = BadgeComponent(content = text, color = color) }
}
