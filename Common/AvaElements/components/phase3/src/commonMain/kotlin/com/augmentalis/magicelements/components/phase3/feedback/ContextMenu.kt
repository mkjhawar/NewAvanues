package com.augmentalis.avaelements.components.phase3.feedback
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class ContextMenu(override val type: String = "ContextMenu", override val id: String? = null, val items: List<String>, @Transient val onItemClick: ((Int) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
