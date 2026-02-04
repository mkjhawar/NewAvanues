package com.augmentalis.avaelements.components.phase3.feedback
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Confirm(override val type: String = "Confirm", override val id: String? = null, val message: String, val confirmText: String = "OK", val cancelText: String = "Cancel", @Transient val onConfirm: (() -> Unit)? = null, @Transient val onCancel: (() -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
