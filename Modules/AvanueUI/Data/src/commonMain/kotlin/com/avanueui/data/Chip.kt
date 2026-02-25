package com.avanueui.data

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.ComponentStyle
import com.augmentalis.avanueui.core.Size
import com.augmentalis.avanueui.core.Color
import com.augmentalis.avanueui.core.Spacing
import com.augmentalis.avanueui.core.Border
import com.augmentalis.avanueui.core.Shadow

/**
 * Chip Component
 *
 * A chip (tag) component for displaying compact information or selections.
 *
 * Features:
 * - Label with optional icon
 * - Deletable chips (with X button)
 * - Selectable state
 * - Click and delete handlers
 *
 * Platform mappings:
 * - Android: Chip from Material Components
 * - iOS: Custom chip view
 * - Web: Badge/tag component
 *
 * Usage:
 * ```kotlin
 * Chip(
 *     label = "Technology",
 *     icon = "label",
 *     deletable = true,
 *     selected = false,
 *     onClick = { /* handle click */ },
 *     onDelete = { /* handle delete */ }
 * )
 * ```
 */
data class ChipComponent(
    val type: String = "Chip",
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
        TODO("Platform rendering not yet implemented")
    }
}
