package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class ListComponent(
    override val type: String = "List",
    override val id: String? = null,
    val items: List<ListItem>,
    val selectable: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    @Transient val onItemClick: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class ListItem(
    val id: String,
    val primary: String,
    val secondary: String? = null,
    val icon: String? = null,
    val avatar: String? = null,
    val trailing: Component? = null
)
