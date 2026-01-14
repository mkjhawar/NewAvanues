package com.augmentalis.avamagic.ui.core.data

import com.augmentalis.avamagic.components.core.*

/**
 * List Component
 *
 * A list component for displaying items with primary text, secondary text,
 * icons, avatars, and trailing components.
 *
 * Features:
 * - Primary and secondary text
 * - Leading icons or avatars
 * - Trailing components (badges, buttons, etc.)
 * - Selection support
 * - Item click handlers
 *
 * Platform mappings:
 * - Android: RecyclerView with ListAdapter
 * - iOS: UITableView
 * - Web: Ordered/unordered list
 *
 * Usage:
 * ```kotlin
 * List(
 *     items = listOf(
 *         ListItem(
 *             id = "1",
 *             primary = "John Doe",
 *             secondary = "Software Engineer",
 *             avatar = "https://example.com/avatar.jpg"
 *         ),
 *         ListItem(
 *             id = "2",
 *             primary = "Jane Smith",
 *             secondary = "Product Manager",
 *             icon = "person"
 *         )
 *     ),
 *     selectable = true,
 *     onItemClick = { index -> /* handle click */ }
 * )
 * ```
 */
data class ListComponent(
    val items: List<ListItem>,
    val selectable: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onItemClick: ((Int) -> Unit)? = null
) {
}

/**
 * Individual list item
 */
data class ListItem(
    val id: String,
    val primary: String,
    val secondary: String? = null,
    val icon: String? = null,
    val avatar: String? = null,
    val trailing: Any? = null
)
