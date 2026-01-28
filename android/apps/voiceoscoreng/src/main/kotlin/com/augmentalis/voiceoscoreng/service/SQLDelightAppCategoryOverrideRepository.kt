/**
 * SQLDelightAppCategoryOverrideRepository.kt - SQLDelight implementation of IAppCategoryRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-26
 *
 * Part of the Hybrid Persistence system for VoiceOSCore.
 * Provides database-backed app category lookups using SQLDelight.
 */

package com.augmentalis.voiceoscoreng.service

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.commandmanager.AppCategoryEntry
import com.augmentalis.commandmanager.IAppCategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IAppCategoryRepository.
 *
 * Uses the app_category_override table to store curated app categories
 * loaded from ACD files and learned at runtime.
 */
class SQLDelightAppCategoryOverrideRepository(
    private val database: VoiceOSDatabase
) : IAppCategoryRepository {

    private val queries get() = database.appCategoryOverrideQueries

    override suspend fun upsertCategory(
        packageName: String,
        category: String,
        source: String,
        confidence: Float,
        acdVersion: String?,
        createdAt: Long,
        updatedAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.upsert(
            package_name = packageName,
            category = category,
            source = source,
            confidence = confidence.toDouble(),
            acd_version = acdVersion,
            created_at = createdAt,
            updated_at = updatedAt
        )
    }

    override suspend fun getCategory(packageName: String): AppCategoryEntry? =
        withContext(Dispatchers.Default) {
            queries.getByPackage(packageName).executeAsOneOrNull()?.let { row ->
                AppCategoryEntry(
                    packageName = row.package_name,
                    category = row.category,
                    source = row.source,
                    confidence = row.confidence.toFloat(),
                    acdVersion = row.acd_version
                )
            }
        }

    override suspend fun getLoadedAcdVersion(): String? = withContext(Dispatchers.Default) {
        queries.getAcdVersion().executeAsOneOrNull()?.acd_version
    }

    override suspend fun deleteSystemEntries() = withContext(Dispatchers.Default) {
        queries.deleteSystemEntries()
    }

    override suspend fun getAllEntries(): List<AppCategoryEntry> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { row ->
            AppCategoryEntry(
                packageName = row.package_name,
                category = row.category,
                source = row.source,
                confidence = row.confidence.toFloat(),
                acdVersion = row.acd_version
            )
        }
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
