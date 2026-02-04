package com.augmentalis.avaelements.components.phase3.feedback
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Toast(override val type: String = "Toast", override val id: String? = null, val message: String, val position: String = "bottom", override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
