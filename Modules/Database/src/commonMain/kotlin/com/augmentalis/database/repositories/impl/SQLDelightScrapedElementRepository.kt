/**
 * SQLDelightScrapedElementRepository.kt - SQLDelight implementation of IScrapedElementRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.dto.toScrapedElementDTO
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IScrapedElementRepository.
 */
class SQLDelightScrapedElementRepository(
    private val database: VoiceOSDatabase
) : IScrapedElementRepository {

    private val queries = database.scrapedElementQueries

    override suspend fun insert(element: ScrapedElementDTO) = withContext(Dispatchers.Default) {
        queries.insert(
            elementHash = element.elementHash,
            appId = element.appId,
            uuid = element.uuid,
            className = element.className,
            viewIdResourceName = element.viewIdResourceName,
            text = element.text,
            contentDescription = element.contentDescription,
            bounds = element.bounds,
            isClickable = element.isClickable,
            isLongClickable = element.isLongClickable,
            isEditable = element.isEditable,
            isScrollable = element.isScrollable,
            isCheckable = element.isCheckable,
            isFocusable = element.isFocusable,
            isEnabled = element.isEnabled,
            depth = element.depth,
            indexInParent = element.indexInParent,
            scrapedAt = element.scrapedAt,
            semanticRole = element.semanticRole,
            inputType = element.inputType,
            visualWeight = element.visualWeight,
            isRequired = element.isRequired,
            formGroupId = element.formGroupId,
            placeholderText = element.placeholderText,
            validationPattern = element.validationPattern,
            backgroundColor = element.backgroundColor,
            screen_hash = element.screen_hash
        )
    }

    override suspend fun getByHash(elementHash: String): ScrapedElementDTO? = withContext(Dispatchers.Default) {
        queries.getByHash(elementHash).executeAsOneOrNull()?.toScrapedElementDTO()
    }

    override suspend fun getByHashAndApp(elementHash: String, appId: String): ScrapedElementDTO? = withContext(Dispatchers.Default) {
        queries.getByHashAndApp(appId, elementHash).executeAsOneOrNull()?.toScrapedElementDTO()
    }

    override suspend fun getByUuid(appId: String, uuid: String): ScrapedElementDTO? = withContext(Dispatchers.Default) {
        queries.getByUuid(appId, uuid).executeAsOneOrNull()?.toScrapedElementDTO()
    }

    override suspend fun getByApp(appId: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getByApp(appId).executeAsList().map { it.toScrapedElementDTO() }
    }

    override suspend fun getClickable(appId: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getClickable(appId).executeAsList().map { it.toScrapedElementDTO() }
    }

    override suspend fun getEditable(appId: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getEditable(appId).executeAsList().map { it.toScrapedElementDTO() }
    }

    override suspend fun getScrollable(appId: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getScrollable(appId).executeAsList().map { it.toScrapedElementDTO() }
    }

    override suspend fun getByClass(appId: String, className: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getByClass(appId, className).executeAsList().map { it.toScrapedElementDTO() }
    }

    override suspend fun getByViewId(appId: String, viewId: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getByViewId(appId, viewId).executeAsList().map { it.toScrapedElementDTO() }
    }

    override suspend fun deleteByHash(elementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByHash(elementHash)
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
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

    override suspend fun countByScreenHash(appId: String, screenHash: String): Long = withContext(Dispatchers.Default) {
        queries.countByScreenHash(appId, screenHash).executeAsOne()
    }

    override suspend fun getByScreenHash(appId: String, screenHash: String): List<ScrapedElementDTO> = withContext(Dispatchers.Default) {
        queries.getByScreenHash(appId, screenHash).executeAsList().map { it.toScrapedElementDTO() }
    }
}
