/**
 * SQLDelightScreenContextRepository.kt - SQLDelight implementation of IScreenContextRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.database.dto.toScreenContextDTO
import com.augmentalis.database.repositories.IScreenContextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IScreenContextRepository.
 */
class SQLDelightScreenContextRepository(
    private val database: VoiceOSDatabase
) : IScreenContextRepository {

    private val queries = database.screenContextQueries

    override suspend fun insert(context: ScreenContextDTO) = withContext(Dispatchers.Default) {
        queries.insert(
            screenHash = context.screenHash,
            appId = context.appId,
            packageName = context.packageName,
            activityName = context.activityName,
            windowTitle = context.windowTitle,
            screenType = context.screenType,
            formContext = context.formContext,
            navigationLevel = context.navigationLevel,
            primaryAction = context.primaryAction,
            elementCount = context.elementCount,
            hasBackButton = context.hasBackButton,
            firstScraped = context.firstScraped,
            lastScraped = context.lastScraped,
            visitCount = context.visitCount
        )
    }

    override suspend fun getByHash(screenHash: String): ScreenContextDTO? = withContext(Dispatchers.Default) {
        queries.getByHash(screenHash).executeAsOneOrNull()?.toScreenContextDTO()
    }

    override suspend fun getByApp(appId: String): List<ScreenContextDTO> = withContext(Dispatchers.Default) {
        queries.getByApp(appId).executeAsList().map { it.toScreenContextDTO() }
    }

    override suspend fun getByPackage(packageName: String): List<ScreenContextDTO> = withContext(Dispatchers.Default) {
        queries.getByPackage(packageName).executeAsList().map { it.toScreenContextDTO() }
    }

    override suspend fun getByActivity(activityName: String): List<ScreenContextDTO> = withContext(Dispatchers.Default) {
        queries.getByActivity(activityName).executeAsList().map { it.toScreenContextDTO() }
    }

    override suspend fun getAll(): List<ScreenContextDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toScreenContextDTO() }
    }

    override suspend fun deleteByHash(screenHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByHash(screenHash)
    }

    override suspend fun deleteByApp(appId: String) = withContext(Dispatchers.Default) {
        queries.deleteByApp(appId)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun countByApp(appId: String): Long = withContext(Dispatchers.Default) {
        queries.countByApp(appId).executeAsOne()
    }
}
