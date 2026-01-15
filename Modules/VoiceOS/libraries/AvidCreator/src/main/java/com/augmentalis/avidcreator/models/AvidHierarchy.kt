/**
 * AvidHierarchy.kt - Hierarchical relationship information for AVID elements
 *
 * Updated: 2026-01-15 - Migrated to AVID naming
 */
package com.augmentalis.avidcreator.models

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

// Backward compatibility alias
@Deprecated("Use AvidHierarchy instead", ReplaceWith("AvidHierarchy"))
typealias VUIDHierarchy = AvidHierarchy
