/**
 * AvidHierarchyModel.kt - Hierarchical relationship information for AVID elements
 *
 * Cross-platform KMP model for element hierarchy.
 *
 * Updated: 2026-01-18 - Migrated to KMP
 */
package com.augmentalis.avidcreator

/**
 * Hierarchical relationship information
 */
data class AvidHierarchy(
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val siblings: List<String> = emptyList(),
    val depth: Int = 0,
    val path: String = "",
    val isRoot: Boolean = parent == null,
    val isLeaf: Boolean = children.isEmpty()
)
