/**
 * VUIDHierarchyDTO.kt - Data Transfer Object for VUID hierarchy
 *
 * Maps to the vuid_hierarchy SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing a VUID hierarchy relationship.
 *
 * @param id Auto-generated ID
 * @param parentVuid Parent VUID
 * @param childVuid Child VUID
 * @param depth Nesting depth (0 = direct child)
 * @param path Full path string for traversal
 * @param orderIndex Sibling ordering index
 */
data class VUIDHierarchyDTO(
    val id: Long = 0,
    val parentVuid: String,
    val childVuid: String,
    val depth: Int = 0,
    val path: String,
    val orderIndex: Int = 0
)
