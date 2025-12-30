package com.augmentalis.avaelements.components.phase3.input
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient
data class SearchBar(override val type: String = "SearchBar", override val id: String? = null, val query: String = "", val placeholder: String = "Search...", @Transient val onQueryChange: ((String) -> Unit)? = null, @Transient val onSearch: ((String) -> Unit)? = null, override val style: ComponentStyle? = null, @Transient override val modifiers: List<Modifier> = emptyList()) : Component { override fun render(renderer: Renderer) = renderer.render(this) }
