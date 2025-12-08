package com.augmentalis.avaelements.components.phase3.feedback
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Alert(override val type: String = "Alert", override val id: String? = null, val message: String, val severity: String = "info", @Transient val onClose: (() -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
