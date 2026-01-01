/**
 * UUIDHierarchyEntity.kt - Room entity for UUID hierarchy storage
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDHierarchyEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room entity for storing parent-child relationships in normalized form
 */

package com.augmentalis.uuidcreator.database.entities

import androidx.room.*

/**
 * Room entity for UUID hierarchy relationships
 *
 * Stores parent-child relationships in normalized form for efficient querying.
 * Replaces the mutable children list in UUIDElement model.
 */
@Entity(
    tableName = "uuid_hierarchy",
    indices = [
        Index(value = ["parent_uuid"], name = "idx_hierarchy_parent"),
        Index(value = ["child_uuid"], name = "idx_hierarchy_child"),
        Index(value = ["depth"], name = "idx_hierarchy_depth"),
        Index(value = ["path"], name = "idx_hierarchy_path")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["parent_uuid"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["child_uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UUIDHierarchyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "parent_uuid")
    val parentUuid: String,

    @ColumnInfo(name = "child_uuid")
    val childUuid: String,

    @ColumnInfo(name = "depth")
    val depth: Int = 0,  // 0 = direct child, 1+ = nested depth

    @ColumnInfo(name = "path")
    val path: String,  // e.g., "/root/parent/child" for hierarchy traversal

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0  // Preserve child order
)
