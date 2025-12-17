/**
 * SQLDelightScrapedAppRepository.kt - SQLDelight implementation of IScrapedAppRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.toScrapedAppDTO
import com.augmentalis.database.repositories.IScrapedAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IScrapedAppRepository.
 */
class SQLDelightScrapedAppRepository(
    private val database: VoiceOSDatabase
) : IScrapedAppRepository {

    private val queries = database.scrapedAppQueries

    override suspend fun insert(app: ScrapedAppDTO) = withContext(Dispatchers.Default) {
        queries.insert(
            appId = app.appId,
            packageName = app.packageName,
            versionCode = app.versionCode,
            versionName = app.versionName,
            appHash = app.appHash,
            isFullyLearned = app.isFullyLearned,
            learnCompletedAt = app.learnCompletedAt,
            scrapingMode = app.scrapingMode,
            scrapeCount = app.scrapeCount,
            elementCount = app.elementCount,
            commandCount = app.commandCount,
            firstScrapedAt = app.firstScrapedAt,
            lastScrapedAt = app.lastScrapedAt
        )
    }

    override suspend fun getById(appId: String): ScrapedAppDTO? = withContext(Dispatchers.Default) {
        queries.getById(appId).executeAsOneOrNull()?.toScrapedAppDTO()
    }

    override suspend fun getByPackage(packageName: String): ScrapedAppDTO? = withContext(Dispatchers.Default) {
        queries.getByPackage(packageName).executeAsOneOrNull()?.toScrapedAppDTO()
    }

    override suspend fun getAll(): List<ScrapedAppDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toScrapedAppDTO() }
    }

    override suspend fun getFullyLearned(): List<ScrapedAppDTO> = withContext(Dispatchers.Default) {
        queries.getFullyLearned().executeAsList().map { it.toScrapedAppDTO() }
    }

    override suspend fun updateStats(
        appId: String,
        scrapeCount: Long,
        elementCount: Long,
        commandCount: Long,
        lastScrapedAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.updateStats(scrapeCount, elementCount, commandCount, lastScrapedAt, appId)
    }

    override suspend fun markFullyLearned(appId: String, learnCompletedAt: Long) = withContext(Dispatchers.Default) {
        queries.markFullyLearned(learnCompletedAt, appId)
    }

    override suspend fun deleteById(appId: String) = withContext(Dispatchers.Default) {
        queries.deleteById(appId)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
