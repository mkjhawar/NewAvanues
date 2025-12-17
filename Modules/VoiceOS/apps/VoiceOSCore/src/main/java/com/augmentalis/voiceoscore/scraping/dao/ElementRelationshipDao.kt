/**
 * ElementRelationshipDao.kt - Data Access Object for ElementRelationshipEntity
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity

/**
 * DAO for ElementRelationshipEntity
 *
 * Provides access to element relationship data for context understanding.
 */
@Dao
interface ElementRelationshipDao {

    /**
     * Insert or replace element relationship
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relationship: ElementRelationshipEntity): Long

    /**
     * Insert multiple relationships
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relationships: List<ElementRelationshipEntity>): List<Long>

    /**
     * Get relationships for a source element
     */
    @Query("SELECT * FROM element_relationships WHERE source_element_hash = :elementHash")
    suspend fun getRelationshipsForElement(elementHash: String): List<ElementRelationshipEntity>

    /**
     * Get relationships by type for a source element
     */
    @Query("SELECT * FROM element_relationships WHERE source_element_hash = :elementHash AND relationship_type = :type")
    suspend fun getRelationshipsByType(elementHash: String, type: String): List<ElementRelationshipEntity>

    /**
     * Get incoming relationships (where element is target)
     */
    @Query("SELECT * FROM element_relationships WHERE target_element_hash = :elementHash")
    suspend fun getIncomingRelationships(elementHash: String): List<ElementRelationshipEntity>

    /**
     * Get form group members for an element
     */
    @Query("""
        SELECT * FROM element_relationships
        WHERE relationship_type = 'form_group_member'
        AND (source_element_hash = :elementHash OR target_element_hash = :elementHash)
    """)
    suspend fun getFormGroupMembers(elementHash: String): List<ElementRelationshipEntity>

    /**
     * Find submit button for a form element
     */
    @Query("""
        SELECT * FROM element_relationships
        WHERE relationship_type = 'button_submits_form'
        AND target_element_hash = :formElementHash
        LIMIT 1
    """)
    suspend fun getSubmitButtonForForm(formElementHash: String): ElementRelationshipEntity?

    /**
     * Get label for an input field
     */
    @Query("""
        SELECT * FROM element_relationships
        WHERE relationship_type = 'label_for'
        AND target_element_hash = :inputElementHash
        LIMIT 1
    """)
    suspend fun getLabelForInput(inputElementHash: String): ElementRelationshipEntity?

    /**
     * Delete relationships for an element
     */
    @Query("DELETE FROM element_relationships WHERE source_element_hash = :elementHash OR target_element_hash = :elementHash")
    suspend fun deleteRelationshipsForElement(elementHash: String): Int

    /**
     * Delete relationships by type
     */
    @Query("DELETE FROM element_relationships WHERE relationship_type = :type")
    suspend fun deleteRelationshipsByType(type: String): Int

    /**
     * Delete low confidence relationships
     */
    @Query("DELETE FROM element_relationships WHERE confidence < :threshold")
    suspend fun deleteLowConfidenceRelationships(threshold: Float): Int

    /**
     * Get total relationship count
     */
    @Query("SELECT COUNT(*) FROM element_relationships")
    suspend fun getRelationshipCount(): Int

    /**
     * Get relationship count by type
     */
    @Query("SELECT COUNT(*) FROM element_relationships WHERE relationship_type = :type")
    suspend fun getRelationshipCountByType(type: String): Int

    /**
     * Get relationships for source element (alias for getRelationshipsForElement)
     * Used by tests
     */
    @Query("SELECT * FROM element_relationships WHERE source_element_hash = :elementHash")
    suspend fun getRelationshipsForSource(elementHash: String): List<ElementRelationshipEntity>
}
