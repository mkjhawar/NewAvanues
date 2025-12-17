/**
 * GeneratedCommandEntity.kt - Voice commands generated from UI elements
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
 * Entity representing a generated voice command
 *
 * This entity stores voice commands automatically generated from UI elements.
 * Commands include primary phrases, synonyms, and confidence scores. Usage
 * statistics track which commands are most effective.
 *
 * @property id Auto-generated primary key
 * @property elementHash Foreign key to ScrapedElementEntity.element_hash this command targets
 * @property commandText Primary command phrase (e.g., "click submit button")
 * @property actionType Action to perform: "click", "long_click", "type", "scroll", "focus"
 * @property confidence AI confidence score for this command (0.0-1.0)
 * @property synonyms JSON array of alternative phrases (e.g., ["send", "post", "submit"])
 * @property isUserApproved Whether user has confirmed this command works
 * @property usageCount Number of times this command has been executed
 * @property lastUsed Timestamp of last usage (milliseconds), null if never used
 * @property generatedAt Timestamp when command was generated (milliseconds)
 */
@Entity(
    tableName = "generated_commands",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("command_text"),
        Index("action_type")
    ]
)
data class GeneratedCommandEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "element_hash")
    val elementHash: String,

    @ColumnInfo(name = "command_text")
    val commandText: String,

    @ColumnInfo(name = "action_type")
    val actionType: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "synonyms")
    val synonyms: String,

    @ColumnInfo(name = "is_user_approved")
    val isUserApproved: Boolean = false,

    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    @ColumnInfo(name = "last_used")
    val lastUsed: Long? = null,

    @ColumnInfo(name = "generated_at")
    val generatedAt: Long = System.currentTimeMillis()
)
