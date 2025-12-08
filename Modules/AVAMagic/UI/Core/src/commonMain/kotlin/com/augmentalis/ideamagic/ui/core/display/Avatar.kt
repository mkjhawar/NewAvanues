package com.augmentalis.avanues.avamagic.ui.core.display
import com.augmentalis.avanues.avamagic.components.core.*
data class AvatarComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val imageUrl: String? = null, val initials: String? = null, val icon: String? = null, val size: ComponentSize = ComponentSize.MD, val shape: AvatarShape = AvatarShape.CIRCLE) : Component {
    init { if (initials != null) require(initials.length <= 3) { "Initials must be 3 characters or less" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    companion object { fun initials(text: String) = AvatarComponent(initials = text.take(2).uppercase()); fun icon(name: String) = AvatarComponent(icon = name) }
}
enum class AvatarShape { CIRCLE, SQUARE, ROUNDED }
