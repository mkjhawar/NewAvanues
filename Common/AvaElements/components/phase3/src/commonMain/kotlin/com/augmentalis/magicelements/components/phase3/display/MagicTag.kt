package com.augmentalis.avaelements.components.phase3.display
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class MagicTag(override val type: String = "MagicTag", override val id: String? = null, val label: String, val icon: String? = null, @Transient val onDelete: (() -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
