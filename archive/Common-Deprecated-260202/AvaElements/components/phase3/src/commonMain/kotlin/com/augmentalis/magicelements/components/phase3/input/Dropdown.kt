package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class Dropdown(override val type: String = "Dropdown", override val id: String? = null, val options: List<String>, val selectedValue: String? = null, val placeholder: String = "Select...", @Transient val onSelectionChange: ((String) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
