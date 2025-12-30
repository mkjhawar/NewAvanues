package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class RangeSlider(override val type: String = "RangeSlider", override val id: String? = null, val startValue: Float, val endValue: Float, val min: Float = 0f, val max: Float = 100f, @Transient val onRangeChange: ((Float, Float) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
