package com.augmentalis.avanues.avamagic.ui.core.data

import com.augmentalis.avanues.avamagic.components.core.*

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
    val nodes: List<TreeNode>,
    val expandedIds: Set<String> = emptySet(),
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onNodeClick: ((String) -> Unit)? = null,
    val onToggle: ((String) -> Unit)? = null
) {
    init {
        require(nodes.isNotEmpty()) { "TreeView must have at least one node" }
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
