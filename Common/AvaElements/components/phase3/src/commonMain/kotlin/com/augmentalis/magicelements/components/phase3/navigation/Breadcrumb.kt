package com.augmentalis.avaelements.components.phase3.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Breadcrumb(
    override val type: String = "Breadcrumb",
    override val id: String? = null,
    val items: List<BreadcrumbItem>,
    val separator: String = "/",
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class BreadcrumbItem(
    val label: String,
    val href: String? = null,
    @Transient val onClick: (() -> Unit)? = null
)
