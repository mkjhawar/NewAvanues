/**
 * VUIDHierarchyDTO.kt - Data transfer object for UUID hierarchy
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

import com.augmentalis.database.uuid.Uuid_hierarchy

/**
 * DTO for UUID hierarchy data.
 */
data class VUIDHierarchyDTO(
    val id: Long,
    val parentUuid: String,
    val childUuid: String,
    val depth: Int,
    val path: String,
    val orderIndex: Int
)

/**
 * Convert SQLDelight entity to DTO.
 */
fun Uuid_hierarchy.toVUIDHierarchyDTO(): VUIDHierarchyDTO = VUIDHierarchyDTO(
    id = id,
    parentUuid = parent_uuid,
    childUuid = child_uuid,
    depth = depth.toInt(),
    path = path,
    orderIndex = order_index.toInt()
)
