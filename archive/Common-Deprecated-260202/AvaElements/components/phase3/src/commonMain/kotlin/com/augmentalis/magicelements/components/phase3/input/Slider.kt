package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Slider(override val type: String = "Slider", override val id: String? = null, val value: Float, val min: Float = 0f, val max: Float = 100f, val step: Float = 1f, @Transient val onValueChange: ((Float) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
