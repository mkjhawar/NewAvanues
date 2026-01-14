package com.augmentalis.uuidcreator.models

/**
 * Hierarchical relationship information
 */
data class VUIDHierarchy(
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val siblings: List<String> = emptyList(),
    val depth: Int = 0,
    val path: String = "",
    val isRoot: Boolean = parent == null,
    val isLeaf: Boolean = children.isEmpty()
)