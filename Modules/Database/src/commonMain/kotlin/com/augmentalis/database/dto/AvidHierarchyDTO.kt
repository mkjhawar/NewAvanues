/**
 * AvidHierarchyDTO.kt - Data Transfer Object for AVID hierarchy
 *
 * Maps to the avid_hierarchy SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing an AVID hierarchy relationship.
 *
 * @param id Auto-generated ID
 * @param parentAvid Parent AVID
 * @param childAvid Child AVID
 * @param depth Nesting depth (0 = direct child)
 * @param path Full path string for traversal
 * @param orderIndex Sibling ordering index
 */
data class AvidHierarchyDTO(
    val id: Long = 0,
    val parentAvid: String,
    val childAvid: String,
    val depth: Int = 0,
    val path: String,
    val orderIndex: Int = 0
)
