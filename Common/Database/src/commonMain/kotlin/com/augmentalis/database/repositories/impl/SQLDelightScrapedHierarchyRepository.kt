/**
 * SQLDelightScrapedHierarchyRepository.kt - SQLDelight implementation of IScrapedHierarchyRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Database Migrator (Agent 2)
 * Created: 2025-11-26
 */

package com.avanues.database.repositories.impl

import com.avanues.database.VoiceOSDatabase
import com.avanues.database.dto.ScrapedHierarchyDTO
import com.avanues.database.dto.toScrapedHierarchyDTO
import com.avanues.database.repositories.IScrapedHierarchyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IScrapedHierarchyRepository.
 */
class SQLDelightScrapedHierarchyRepository(
    private val database: VoiceOSDatabase
) : IScrapedHierarchyRepository {

    private val queries = database.scrapedHierarchyQueries

    override suspend fun insert(
        parentElementHash: String,
        childElementHash: String,
        depth: Long,
        createdAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.insert(parentElementHash, childElementHash, depth, createdAt)
    }

    override suspend fun getByParent(parentElementHash: String): List<ScrapedHierarchyDTO> =
        withContext(Dispatchers.Default) {
            queries.getByParent(parentElementHash).executeAsList().map { it.toScrapedHierarchyDTO() }
        }

    override suspend fun getByChild(childElementHash: String): List<ScrapedHierarchyDTO> =
        withContext(Dispatchers.Default) {
            queries.getByChild(childElementHash).executeAsList().map { it.toScrapedHierarchyDTO() }
        }

    override suspend fun getByDepth(depth: Long): List<ScrapedHierarchyDTO> =
        withContext(Dispatchers.Default) {
            queries.getByDepth(depth).executeAsList().map { it.toScrapedHierarchyDTO() }
        }

    override suspend fun deleteByParent(parentElementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByParent(parentElementHash)
    }

    override suspend fun deleteByChild(childElementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByChild(childElementHash)
    }

    override suspend fun deleteRelationship(
        parentElementHash: String,
        childElementHash: String
    ) = withContext(Dispatchers.Default) {
        queries.deleteRelationship(parentElementHash, childElementHash)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
