package com.augmentalis.avaelements.components.phase3.feedback
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Snackbar(override val type: String = "Snackbar", override val id: String? = null, val message: String, val duration: Int = 3000, @Transient val onDismiss: (() -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
