package com.augmentalis.avaelements.components.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle

/**
 * Chip Component (alias for MagicTag)
 * Used by DSL builders for tag/chip functionality.
 */
data class ChipComponent(
    override val type: String = "Chip",
    val label: String,
    val icon: String? = null,
    val deletable: Boolean = false,
    val selected: Boolean = false,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onClick: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null
) : Component {
    init {
        require(label.isNotBlank()) { "Chip label cannot be blank" }
    }

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
