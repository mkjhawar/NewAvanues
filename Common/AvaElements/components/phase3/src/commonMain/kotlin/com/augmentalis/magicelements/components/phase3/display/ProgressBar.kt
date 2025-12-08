package com.augmentalis.avaelements.components.phase3.display
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class ProgressBar(override val type: String = "ProgressBar", override val id: String? = null, val progress: Float, val showLabel: Boolean = true, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
