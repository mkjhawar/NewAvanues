package com.augmentalis.avamagic.ui.core.display
import com.augmentalis.avamagic.components.core.*
data class TreeViewComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val nodes: List<TreeNode>, val expandedIds: Set<String> = emptySet()) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun toggle(nodeId: String): TreeViewComponent = if (nodeId in expandedIds) copy(expandedIds = expandedIds - nodeId) else copy(expandedIds = expandedIds + nodeId)
    fun expandAll(): TreeViewComponent = copy(expandedIds = collectAllIds(nodes))
    fun collapseAll(): TreeViewComponent = copy(expandedIds = emptySet())
    private fun collectAllIds(nodes: List<TreeNode>): Set<String> = nodes.flatMap { listOf(it.id) + collectAllIds(it.children) }.toSet()
}
data class TreeNode(val id: String, val label: String, val icon: String? = null, val children: List<TreeNode> = emptyList())
