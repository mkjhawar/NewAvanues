package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class EmptyState(
    override val type: String = "EmptyState",
    override val id: String? = null,
    val icon: String? = null,
    val title: String,
    val description: String? = null,
    val action: Component? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}
