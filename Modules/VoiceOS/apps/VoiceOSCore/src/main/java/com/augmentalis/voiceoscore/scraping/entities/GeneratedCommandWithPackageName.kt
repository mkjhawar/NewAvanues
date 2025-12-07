/**
 * GeneratedCommandWithPackageName.kt - DAO result class for commands with package names
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Data class to hold command with package name from JOIN query
 * Used by GeneratedCommandDao.getCommandWithPackageName()
 *
 * This class represents the result of a JOIN query across three tables:
 * generated_commands -> scraped_elements -> scraped_apps
 */
data class GeneratedCommandWithPackageName(
    val id: Long,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Float,
    val synonyms: String,
    val isUserApproved: Boolean,
    val usageCount: Int,
    val lastUsed: Long?,
    val generatedAt: Long,
    val packageName: String
)
