package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class TreeView(
    override val type: String = "TreeView",
    override val id: String? = null,
    val nodes: List<TreeNode>,
    val expandedIds: Set<String> = emptySet(),
    @Transient val onNodeClick: ((String) -> Unit)? = null,
    @Transient val onToggle: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class TreeNode(
    val id: String,
    val label: String,
    val icon: String? = null,
    val children: List<TreeNode> = emptyList()
)
