package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class RadioGroup(override val type: String = "RadioGroup", override val id: String? = null, val options: List<Pair<String, String>>, val selectedValue: String? = null, @Transient val onSelectionChange: ((String) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
