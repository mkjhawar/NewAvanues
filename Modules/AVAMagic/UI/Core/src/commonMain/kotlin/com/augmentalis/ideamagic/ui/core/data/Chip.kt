package com.augmentalis.avanues.avamagic.ui.core.data

import com.augmentalis.avanues.avamagic.components.core.*

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
    val label: String,
    val icon: String? = null,
    val deletable: Boolean = false,
    val selected: Boolean = false,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onClick: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null
) {
    init {
        require(label.isNotBlank()) { "Chip label cannot be blank" }
    }

}
