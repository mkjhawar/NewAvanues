package com.augmentalis.avaelements.components.phase3.display

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Avatar(
    override val type: String = "Avatar",
    override val id: String? = null,
    val source: String? = null,
    val text: String? = null,
    val size: AvatarSize = AvatarSize.Medium,
    val shape: AvatarShape = AvatarShape.Circle,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

enum class AvatarSize {
    Small,
    Medium,
    Large
}

enum class AvatarShape {
    Circle,
    Square,
    Rounded
}
