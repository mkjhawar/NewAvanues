package com.augmentalis.avaelements.components.phase3.layout

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Tabs(
    override val type: String = "Tabs",
    override val id: String? = null,
    val tabs: List<Tab>,
    val selectedIndex: Int = 0,
    @Transient val onTabSelected: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class Tab(
    val label: String,
    val icon: String? = null,
    val content: Component? = null
)
