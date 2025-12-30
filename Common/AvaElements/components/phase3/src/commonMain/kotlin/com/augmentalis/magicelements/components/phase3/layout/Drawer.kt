package com.augmentalis.avaelements.components.phase3.layout

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Drawer(
    override val type: String = "Drawer",
    override val id: String? = null,
    val isOpen: Boolean = false,
    val position: DrawerPosition = DrawerPosition.Left,
    val header: Component? = null,
    val items: List<DrawerItem> = emptyList(),
    val footer: Component? = null,
    @Transient val onItemClick: ((String) -> Unit)? = null,
    @Transient val onDismiss: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

enum class DrawerPosition {
    Left,
    Right,
    Top,
    Bottom
}

data class DrawerItem(
    val id: String,
    val icon: String?,
    val label: String,
    val badge: String? = null
)
