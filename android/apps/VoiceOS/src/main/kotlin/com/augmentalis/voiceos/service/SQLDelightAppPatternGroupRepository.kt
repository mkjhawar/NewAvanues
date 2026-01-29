/**
 * SQLDelightAppPatternGroupRepository.kt - SQLDelight implementation of IAppPatternGroupRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-26
 *
 * Part of the Hybrid Persistence system for VoiceOSCore.
 * Provides database-backed pattern group lookups using SQLDelight.
 */

package com.augmentalis.voiceos.service

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.voiceoscore.AppPatternEntry
import com.augmentalis.voiceoscore.IAppPatternGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IAppPatternGroupRepository.
 *
 * Uses the app_pattern_group table to store fallback pattern matching rules
 * loaded from ACD files.
 */
class SQLDelightAppPatternGroupRepository(
    private val database: VoiceOSDatabase
) : IAppPatternGroupRepository {

    private val queries get() = database.appPatternGroupQueries

    override suspend fun insertPattern(
        category: String,
        pattern: String,
        priority: Int,
        acdVersion: String?,
        createdAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.insert(
            category = category,
            pattern = pattern,
            priority = priority.toLong(),
            acd_version = acdVersion,
            created_at = createdAt
        )
    }

    override suspend fun getAllPatterns(): List<AppPatternEntry> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { row ->
            AppPatternEntry(
                category = row.category,
                pattern = row.pattern,
                priority = row.priority.toInt()
            )
        }
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
