package com.augmentalis.avaelements.components.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * TreeView Component
 *
 * A tree view component for displaying hierarchical data with
 * expandable/collapsible nodes.
 *
 * Features:
 * - Hierarchical node structure
 * - Expand/collapse functionality
 * - Icons for nodes
 * - Node selection
 *
 * Platform mappings:
 * - Android: RecyclerView with hierarchy
 * - iOS: UIOutlineView
 * - macOS: NSOutlineView
 * - Web: Tree navigation
 *
 * Usage:
 * ```kotlin
 * TreeView(
 *     nodes = listOf(
 *         TreeNode(
 *             id = "1",
 *             label = "Root",
 *             icon = "folder",
 *             children = listOf(
 *                 TreeNode("1.1", "Child 1", icon = "file"),
 *                 TreeNode("1.2", "Child 2", icon = "file")
 *             )
 *         )
 *     ),
 *     expandedIds = setOf("1"),
 *     onNodeClick = { id -> /* handle click */ }
 * )
 * ```
 */
data class TreeViewComponent(
    override val type: String = "TreeView",
    val nodes: List<TreeNode>,
    val expandedIds: Set<String> = emptySet(),
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onNodeClick: ((String) -> Unit)? = null,
    val onToggle: ((String) -> Unit)? = null
) : Component {
    init {
        require(nodes.isNotEmpty()) { "TreeView must have at least one node" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Tree node with optional children
 */
data class TreeNode(
    val id: String,
    val label: String,
    val icon: String? = null,
    val children: List<TreeNode> = emptyList()
)
