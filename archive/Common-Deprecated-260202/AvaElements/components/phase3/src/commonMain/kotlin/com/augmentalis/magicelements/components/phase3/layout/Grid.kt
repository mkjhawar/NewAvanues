package com.augmentalis.avaelements.components.phase3.layout
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Grid(override val type: String = "Grid", override val id: String? = null, val columns: Int = 2, val gap: Int = 16, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
