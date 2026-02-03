package com.augmentalis.avaelements.components.phase3.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class AppBar(
    override val type: String = "AppBar",
    override val id: String? = null,
    val title: String,
    val navigationIcon: String? = null,
    val actions: List<AppBarAction> = emptyList(),
    val elevation: Int = 1,
    @Transient val onNavigationClick: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class AppBarAction(
    val icon: String,
    val label: String? = null,
    @Transient val onClick: () -> Unit = {}
)
