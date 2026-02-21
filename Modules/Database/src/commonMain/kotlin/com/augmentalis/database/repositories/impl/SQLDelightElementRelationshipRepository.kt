/**
 * SQLDelightElementRelationshipRepository.kt - SQLDelight implementation of IElementRelationshipRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ElementRelationshipDTO
import com.augmentalis.database.dto.toElementRelationshipDTO
import com.augmentalis.database.repositories.IElementRelationshipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IElementRelationshipRepository.
 */
class SQLDelightElementRelationshipRepository(
    private val database: VoiceOSDatabase
) : IElementRelationshipRepository {

    private val queries = database.elementRelationshipQueries

    override suspend fun insert(
        sourceElementHash: String,
        targetElementHash: String?,
        relationshipType: String,
        relationshipData: String?,
        confidence: Double,
        createdAt: Long,
        updatedAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.insert(
            sourceElementHash,
            targetElementHash,
            relationshipType,
            relationshipData,
            confidence,
            createdAt,
            updatedAt
        )
    }

    override suspend fun update(
        id: Long,
        targetElementHash: String?,
        relationshipData: String?,
        confidence: Double,
        updatedAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.update(targetElementHash, relationshipData, confidence, updatedAt, id)
    }

    override suspend fun getById(id: Long): ElementRelationshipDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toElementRelationshipDTO()
    }

    override suspend fun getBySource(sourceElementHash: String): List<ElementRelationshipDTO> =
        withContext(Dispatchers.Default) {
            queries.getBySource(sourceElementHash).executeAsList().map { it.toElementRelationshipDTO() }
        }

    override suspend fun getByTarget(targetElementHash: String): List<ElementRelationshipDTO> =
        withContext(Dispatchers.Default) {
            queries.getByTarget(targetElementHash).executeAsList().map { it.toElementRelationshipDTO() }
        }

    override suspend fun getByType(relationshipType: String): List<ElementRelationshipDTO> =
        withContext(Dispatchers.Default) {
            queries.getByType(relationshipType).executeAsList().map { it.toElementRelationshipDTO() }
        }

    override suspend fun getBySourceAndType(
        sourceElementHash: String,
        relationshipType: String
    ): List<ElementRelationshipDTO> = withContext(Dispatchers.Default) {
        queries.getBySourceAndType(sourceElementHash, relationshipType)
            .executeAsList()
            .map { it.toElementRelationshipDTO() }
    }

    override suspend fun getHighConfidence(confidenceThreshold: Double): List<ElementRelationshipDTO> =
        withContext(Dispatchers.Default) {
            queries.getHighConfidence(confidenceThreshold).executeAsList().map { it.toElementRelationshipDTO() }
        }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteBySource(sourceElementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteBySource(sourceElementHash)
    }

    override suspend fun deleteByTarget(targetElementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByTarget(targetElementHash)
    }

    override suspend fun deleteByType(relationshipType: String) = withContext(Dispatchers.Default) {
        queries.deleteByType(relationshipType)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
