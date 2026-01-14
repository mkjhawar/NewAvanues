package com.augmentalis.avaelements.components.phase3.layout
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Spacer(override val type: String = "Spacer", override val id: String? = null, val size: Int = 16, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
