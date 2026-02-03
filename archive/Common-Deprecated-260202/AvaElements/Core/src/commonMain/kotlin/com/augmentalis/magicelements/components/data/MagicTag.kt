package com.augmentalis.avaelements.components.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * MagicTag Component
 *
 * A magic tag component for displaying compact information or selections.
 * Branded interactive component following the new naming convention.
 *
 * Features:
 * - Label with optional icon
 * - Deletable tags (with X button)
 * - Selectable state
 * - Click and delete handlers
 *
 * Platform mappings:
 * - Android: Chip from Material Components
 * - iOS: Custom tag view
 * - Web: Badge/tag component
 *
 * Usage:
 * ```kotlin
 * MagicTag(
 *     label = "Technology",
 *     icon = "label",
 *     deletable = true,
 *     selected = false,
 *     onClick = { /* handle click */ },
 *     onDelete = { /* handle delete */ }
 * )
 * ```
 */
data class MagicTagComponent(
    override val type: String = "MagicTag",
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
        require(label.isNotBlank()) { "MagicTag label cannot be blank" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}
