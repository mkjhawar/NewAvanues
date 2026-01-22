package com.augmentalis.avamagic.avaui.data

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.ComponentStyle
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Color
import com.augmentalis.avamagic.components.core.Spacing
import com.augmentalis.avamagic.components.core.Border
import com.augmentalis.avamagic.components.core.Shadow

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
    val type: String = "TreeView",
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
