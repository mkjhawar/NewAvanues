package com.augmentalis.avaelements.components.phase3.display

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.components.phase3.data.Orientation
import kotlinx.serialization.Transient

data class Divider(
    override val type: String = "Divider",
    override val id: String? = null,
    val orientation: Orientation = Orientation.Horizontal,
    val thickness: Float = 1f,
    val text: String? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}
