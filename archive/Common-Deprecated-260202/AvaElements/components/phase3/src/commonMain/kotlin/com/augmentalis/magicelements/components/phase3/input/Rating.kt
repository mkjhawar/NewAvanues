package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Rating(override val type: String = "Rating", override val id: String? = null, val rating: Float = 0f, val maxRating: Int = 5, @Transient val onRatingChange: ((Float) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
