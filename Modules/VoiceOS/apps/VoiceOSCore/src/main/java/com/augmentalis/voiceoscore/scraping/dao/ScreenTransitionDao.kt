/**
 * ScreenTransitionDao.kt - Data Access Object for ScreenTransitionEntity
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
import com.augmentalis.voiceoscore.scraping.entities.ScreenTransitionEntity

/**
 * DAO for ScreenTransitionEntity
 *
 * Provides access to screen transition data for navigation flow analysis.
 */
@Dao
interface ScreenTransitionDao {

    /**
     * Insert or replace screen transition
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transition: ScreenTransitionEntity): Long

    /**
     * Get transition between two screens
     */
    @Query("SELECT * FROM screen_transitions WHERE from_screen_hash = :fromHash AND to_screen_hash = :toHash")
    suspend fun getTransition(fromHash: String, toHash: String): ScreenTransitionEntity?

    /**
     * Record a screen transition (increment count if exists, insert if new)
     */
    suspend fun recordTransition(fromHash: String, toHash: String, transitionTime: Long? = null) {
        val existing = getTransition(fromHash, toHash)

        if (existing != null) {
            // Update existing transition
            val newCount = existing.transitionCount + 1
            val newAvgTime = if (transitionTime != null && existing.avgTransitionTime != null) {
                (existing.avgTransitionTime + transitionTime) / 2
            } else {
                existing.avgTransitionTime ?: transitionTime
            }

            val updated = existing.copy(
                transitionCount = newCount,
                lastTransition = System.currentTimeMillis(),
                avgTransitionTime = newAvgTime
            )
            insert(updated)
        } else {
            // Create new transition
            val newTransition = ScreenTransitionEntity(
                fromScreenHash = fromHash,
                toScreenHash = toHash,
                transitionCount = 1,
                avgTransitionTime = transitionTime
            )
            insert(newTransition)
        }
    }

    /**
     * Get all transitions from a screen (outgoing)
     */
    @Query("SELECT * FROM screen_transitions WHERE from_screen_hash = :screenHash ORDER BY transition_count DESC")
    suspend fun getOutgoingTransitions(screenHash: String): List<ScreenTransitionEntity>

    /**
     * Get all transitions to a screen (incoming)
     */
    @Query("SELECT * FROM screen_transitions WHERE to_screen_hash = :screenHash ORDER BY transition_count DESC")
    suspend fun getIncomingTransitions(screenHash: String): List<ScreenTransitionEntity>

    /**
     * Get most common transitions (top navigation flows)
     */
    @Query("SELECT * FROM screen_transitions ORDER BY transition_count DESC LIMIT :limit")
    suspend fun getMostCommonTransitions(limit: Int = 20): List<ScreenTransitionEntity>

    /**
     * Get recent transitions
     */
    @Query("SELECT * FROM screen_transitions ORDER BY last_transition DESC LIMIT :limit")
    suspend fun getRecentTransitions(limit: Int = 20): List<ScreenTransitionEntity>

    /**
     * Delete transitions older than timestamp
     */
    @Query("DELETE FROM screen_transitions WHERE last_transition < :timestamp")
    suspend fun deleteOldTransitions(timestamp: Long): Int

    /**
     * Get total transition count
     */
    @Query("SELECT COUNT(*) FROM screen_transitions")
    suspend fun getTransitionCount(): Int
}
