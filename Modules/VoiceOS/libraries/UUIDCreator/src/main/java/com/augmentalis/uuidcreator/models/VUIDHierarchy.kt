package com.augmentalis.uuidcreator.models

/**
 * Hierarchical relationship information (VUID migration)
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Created: 2025-12-23
 */
data class VUIDHierarchy(
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val siblings: List<String> = emptyList(),
    val depth: Int = 0,
    val path: String = "",
    val isRoot: Boolean = parent == null,
    val isLeaf: Boolean = children.isEmpty()
) {
    /**
     * Convert to deprecated UUIDHierarchy for backwards compatibility
     */
    fun toUUIDHierarchy(): UUIDHierarchy = UUIDHierarchy(
        parent = parent,
        children = children,
        siblings = siblings,
        depth = depth,
        path = path,
        isRoot = isRoot,
        isLeaf = isLeaf
    )

    companion object {
        /**
         * Convert from deprecated UUIDHierarchy
         */
        fun fromUUIDHierarchy(hierarchy: UUIDHierarchy): VUIDHierarchy = VUIDHierarchy(
            parent = hierarchy.parent,
            children = hierarchy.children,
            siblings = hierarchy.siblings,
            depth = hierarchy.depth,
            path = hierarchy.path,
            isRoot = hierarchy.isRoot,
            isLeaf = hierarchy.isLeaf
        )
    }
}
