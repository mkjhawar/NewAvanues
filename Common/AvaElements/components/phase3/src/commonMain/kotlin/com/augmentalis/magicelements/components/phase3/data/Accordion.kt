package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Accordion(
    override val type: String = "Accordion",
    override val id: String? = null,
    val items: List<AccordionItem>,
    val expandedIndices: Set<Int> = emptySet(),
    val allowMultiple: Boolean = false,
    @Transient val onToggle: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class AccordionItem(
    val id: String,
    val title: String,
    val content: Component
)
