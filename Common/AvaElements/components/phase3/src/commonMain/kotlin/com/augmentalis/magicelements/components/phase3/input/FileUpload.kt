package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class FileUpload(override val type: String = "FileUpload", override val id: String? = null, val selectedFiles: List<String> = emptyList(), val multiple: Boolean = false, @Transient val onFilesSelected: ((List<String>) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
