package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Carousel(
    override val type: String = "Carousel",
    override val id: String? = null,
    val items: List<Component>,
    val currentIndex: Int = 0,
    val autoPlay: Boolean = false,
    val interval: Long = 3000,
    val showIndicators: Boolean = true,
    val showControls: Boolean = true,
    @Transient val onSlideChange: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}
