package com.augmentalis.avaelements.components.phase3.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class BottomNav(
    override val type: String = "BottomNav",
    override val id: String? = null,
    val items: List<BottomNavItem>,
    val selectedIndex: Int = 0,
    @Transient val onItemSelected: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class BottomNavItem(
    val icon: String,
    val label: String,
    val badge: String? = null
)
