/**
 * GeneratedCommandEntity.kt - Voice commands generated from UI elements
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 2)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing a generated voice command
 *
 * This entity stores voice commands automatically generated from UI elements.
 * Commands include primary phrases, synonyms, and confidence scores. Usage
 * statistics track which commands are most effective.
 *
 * @property id Auto-generated primary key
 * @property elementHash Foreign key to ScrapedElementEntity.elementHash
 * @property commandText Primary command phrase (e.g., "click submit button")
 * @property actionType Action to perform: "click", "long_click", "type", "scroll", "focus"
 * @property confidence AI confidence score for this command (0.0-1.0)
 * @property synonyms JSON array of alternative phrases
 * @property isUserApproved Whether user has confirmed this command works
 * @property usageCount Number of times this command has been executed
 * @property lastUsed Timestamp of last usage (milliseconds), null if never used
 * @property generatedAt Timestamp when command was generated (milliseconds)
 */
data class GeneratedCommandEntity(
    val id: Long = 0,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Float,
    val synonyms: String,
    val isUserApproved: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val generatedAt: Long = System.currentTimeMillis()
)
