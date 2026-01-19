/**
 * GeneratedCommandEntity.kt - Voice commands generated from UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Migrated to SQLDelight: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing a generated voice command
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq
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
 * @property createdAt Timestamp when command was generated (milliseconds)
 * @property appId Application ID for the command
 * @property appVersion Application version string
 * @property versionCode Application version code
 * @property lastVerified Timestamp when command was last verified
 * @property isDeprecated Whether command is deprecated for current app version
 */
data class GeneratedCommandEntity(
    val id: Long = 0,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Double,
    val synonyms: String?,
    val isUserApproved: Long = 0,
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val appId: String = "",
    val appVersion: String = "",
    val versionCode: Long = 0,
    val lastVerified: Long? = null,
    val isDeprecated: Long = 0
)
