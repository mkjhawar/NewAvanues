package com.augmentalis.avaelements.components.phase3.display
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Badge(override val type: String = "Badge", override val id: String? = null, val text: String, val variant: String = "default", override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
