/**
 * ScrapedHierarchyEntity.kt - Parent-child relationships for accessibility tree
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing parent-child relationships in accessibility tree
 *
 * This entity captures the hierarchical structure of the UI, allowing the system
 * to understand element relationships and context. Essential for commands like
 * "click the button in the dialog" or "scroll the list".
 *
 * @property id Auto-generated primary key
 * @property parentElementId Foreign key to parent ScrapedElementEntity
 * @property childElementId Foreign key to child ScrapedElementEntity
 * @property childOrder Order of child among siblings (for maintaining layout order)
 * @property depth Depth difference between parent and child (usually 1)
 */
@Entity(
    tableName = "scraped_hierarchy",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_element_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_element_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parent_element_id"),
        Index("child_element_id")
    ]
)
data class ScrapedHierarchyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "parent_element_id")
    val parentElementId: Long,

    @ColumnInfo(name = "child_element_id")
    val childElementId: Long,

    @ColumnInfo(name = "child_order")
    val childOrder: Int,

    @ColumnInfo(name = "depth")
    val depth: Int = 1
)
