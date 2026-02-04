package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class ImagePicker(override val type: String = "ImagePicker", override val id: String? = null, val selectedImage: String? = null, val allowCamera: Boolean = true, @Transient val onImageSelected: ((String) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
