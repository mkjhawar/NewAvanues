/**
 * IElementRelationshipRepository.kt - Repository interface for element relationships
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Database Migrator (Agent 2)
 * Created: 2025-11-26
 */

package com.avanues.database.repositories

import com.avanues.database.dto.ElementRelationshipDTO

/**
 * Repository interface for semantic element relationships.
 * Provides CRUD operations for element relationships (label-for, triggers, etc.)
 */
interface IElementRelationshipRepository {

    /**
     * Insert a relationship.
     */
    suspend fun insert(
        sourceElementHash: String,
        targetElementHash: String?,
        relationshipType: String,
        relationshipData: String?,
        confidence: Double,
        createdAt: Long,
        updatedAt: Long
    )

    /**
     * Update an existing relationship.
     */
    suspend fun update(
        id: Long,
        targetElementHash: String?,
        relationshipData: String?,
        confidence: Double,
        updatedAt: Long
    )

    /**
     * Get relationship by ID.
     */
    suspend fun getById(id: Long): ElementRelationshipDTO?

    /**
     * Get relationships by source element.
     */
    suspend fun getBySource(sourceElementHash: String): List<ElementRelationshipDTO>

    /**
     * Get relationships by target element.
     */
    suspend fun getByTarget(targetElementHash: String): List<ElementRelationshipDTO>

    /**
     * Get relationships by type.
     */
    suspend fun getByType(relationshipType: String): List<ElementRelationshipDTO>

    /**
     * Get relationships by source and type.
     */
    suspend fun getBySourceAndType(
        sourceElementHash: String,
        relationshipType: String
    ): List<ElementRelationshipDTO>

    /**
     * Get high-confidence relationships (>= threshold).
     */
    suspend fun getHighConfidence(confidenceThreshold: Double): List<ElementRelationshipDTO>

    /**
     * Delete relationship by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete relationships by source element.
     */
    suspend fun deleteBySource(sourceElementHash: String)

    /**
     * Delete relationships by target element.
     */
    suspend fun deleteByTarget(targetElementHash: String)

    /**
     * Delete relationships by type.
     */
    suspend fun deleteByType(relationshipType: String)

    /**
     * Delete all relationships.
     */
    suspend fun deleteAll()

    /**
     * Count all relationships.
     */
    suspend fun count(): Long
}
