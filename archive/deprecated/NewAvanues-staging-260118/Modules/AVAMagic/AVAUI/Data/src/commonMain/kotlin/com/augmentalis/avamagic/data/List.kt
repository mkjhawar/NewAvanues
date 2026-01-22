package com.augmentalis.avamagic.avaui.data

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.ComponentStyle
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Color
import com.augmentalis.avamagic.components.core.Spacing
import com.augmentalis.avamagic.components.core.Border
import com.augmentalis.avamagic.components.core.Shadow

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
    val type: String = "List",
    val items: List<ListItem>,
    val selectable: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onItemClick: ((Int) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
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
    val trailing: Component? = null
)
