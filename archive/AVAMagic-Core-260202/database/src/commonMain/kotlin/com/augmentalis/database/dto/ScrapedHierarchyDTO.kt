/**
 * ScrapedHierarchyDTO.kt - DTO for UI element parent-child hierarchies
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Database Migrator (Agent 2)
 * Created: 2025-11-26
 *
 * Data Transfer Object for scraped element hierarchies.
 * Maps to ScrapedHierarchy.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.element.Scraped_hierarchy

/**
 * DTO for scraped element hierarchy (parent-child relationships)
 */
data class ScrapedHierarchyDTO(
    val id: Long,
    val parentElementHash: String,
    val childElementHash: String,
    val depth: Long = 0,
    val createdAt: Long = 0
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Scraped_hierarchy.toScrapedHierarchyDTO(): ScrapedHierarchyDTO {
    return ScrapedHierarchyDTO(
        id = id,
        parentElementHash = parentElementHash,
        childElementHash = childElementHash,
        depth = depth,
        createdAt = createdAt
    )
}
