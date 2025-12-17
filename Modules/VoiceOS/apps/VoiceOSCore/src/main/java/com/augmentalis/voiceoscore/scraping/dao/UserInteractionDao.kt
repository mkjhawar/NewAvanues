/**
 * UserInteractionDao.kt - Data access object for user interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.augmentalis.voiceoscore.scraping.entities.UserInteractionEntity

/**
 * User Interaction DAO
 *
 * Provides database access methods for tracking and querying user interactions
 * with UI elements including clicks, long presses, swipes, and focus events.
 *
 * Use Cases:
 * - Recording user interactions as they occur
 * - Querying interaction history for specific elements
 * - Finding recently interacted elements
 * - Time-based interaction analysis
 * - Element importance scoring based on interaction frequency
 *
 * Performance Notes:
 * - All queries use indexed columns for optimal performance
 * - Limit parameters prevent excessive memory usage
 * - Batch insert for multiple interactions
 */
@Dao
interface UserInteractionDao {

    /**
     * Insert a single user interaction
     *
     * @param interaction The interaction to record
     * @return Row ID of inserted interaction
     */
    @Insert
    suspend fun insert(interaction: UserInteractionEntity): Long

    /**
     * Insert multiple user interactions in a batch
     *
     * Use for bulk operations to improve performance.
     *
     * @param interactions List of interactions to record
     */
    @Insert
    suspend fun insertAll(interactions: List<UserInteractionEntity>)

    /**
     * Get all interactions for a specific element
     *
     * Ordered by most recent first. Useful for understanding
     * how a specific element has been used over time.
     *
     * @param elementHash Hash of the element
     * @return List of interactions, newest first
     */
    @Query("SELECT * FROM user_interactions WHERE element_hash = :elementHash ORDER BY interaction_time DESC")
    suspend fun getInteractionsForElement(elementHash: String): List<UserInteractionEntity>

    /**
     * Get all interactions for a specific screen
     *
     * Ordered by most recent first. Useful for understanding
     * screen-level interaction patterns.
     *
     * @param screenHash Hash of the screen
     * @return List of interactions on this screen, newest first
     */
    @Query("SELECT * FROM user_interactions WHERE screen_hash = :screenHash ORDER BY interaction_time DESC")
    suspend fun getInteractionsForScreen(screenHash: String): List<UserInteractionEntity>

    /**
     * Get interactions by type
     *
     * Find all interactions of a specific type (click, long_press, etc.)
     * Limited to prevent excessive results.
     *
     * @param type Interaction type (see InteractionType constants)
     * @param limit Maximum number of results (default 100)
     * @return List of interactions of specified type, newest first
     */
    @Query("SELECT * FROM user_interactions WHERE interaction_type = :type ORDER BY interaction_time DESC LIMIT :limit")
    suspend fun getInteractionsByType(type: String, limit: Int = 100): List<UserInteractionEntity>

    /**
     * Get most recent interactions across all elements
     *
     * Useful for showing recent user activity or debugging.
     *
     * @param limit Maximum number of results (default 100)
     * @return List of recent interactions, newest first
     */
    @Query("SELECT * FROM user_interactions ORDER BY interaction_time DESC LIMIT :limit")
    suspend fun getRecentInteractions(limit: Int = 100): List<UserInteractionEntity>

    /**
     * Get interactions within a time range
     *
     * Useful for time-based analysis or session tracking.
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return List of interactions in time range, newest first
     */
    @Query("SELECT * FROM user_interactions WHERE interaction_time BETWEEN :startTime AND :endTime ORDER BY interaction_time DESC")
    suspend fun getInteractionsInTimeRange(startTime: Long, endTime: Long): List<UserInteractionEntity>

    /**
     * Get interaction count for an element
     *
     * Useful for importance scoring - frequently interacted elements
     * are likely more important to the user.
     *
     * @param elementHash Hash of the element
     * @return Total number of interactions with this element
     */
    @Query("SELECT COUNT(*) FROM user_interactions WHERE element_hash = :elementHash")
    suspend fun getInteractionCount(elementHash: String): Int

    /**
     * Get interaction count for an element by type
     *
     * Count interactions of a specific type for an element.
     *
     * @param elementHash Hash of the element
     * @param type Interaction type
     * @return Count of interactions of specified type
     */
    @Query("SELECT COUNT(*) FROM user_interactions WHERE element_hash = :elementHash AND interaction_type = :type")
    suspend fun getInteractionCountByType(elementHash: String, type: String): Int

    /**
     * Delete old interactions
     *
     * Cleanup method to prevent database bloat. Call periodically
     * to remove interactions older than a certain date.
     *
     * @param cutoffTime Timestamp - interactions before this are deleted
     * @return Number of interactions deleted
     */
    @Query("DELETE FROM user_interactions WHERE interaction_time < :cutoffTime")
    suspend fun deleteOldInteractions(cutoffTime: Long): Int

    /**
     * Get last interaction for an element
     *
     * Find the most recent interaction with a specific element.
     * Useful for determining recency of use.
     *
     * @param elementHash Hash of the element
     * @return Most recent interaction, or null if never interacted
     */
    @Query("SELECT * FROM user_interactions WHERE element_hash = :elementHash ORDER BY interaction_time DESC LIMIT 1")
    suspend fun getLastInteraction(elementHash: String): UserInteractionEntity?

    /**
     * Get most interacted elements on a screen
     *
     * Finds elements on a screen sorted by interaction frequency.
     * Useful for identifying important UI elements.
     *
     * @param screenHash Hash of the screen
     * @param limit Maximum number of results (default 20)
     * @return List of element hashes sorted by interaction count
     */
    @Query("""
        SELECT element_hash, COUNT(*) as interaction_count
        FROM user_interactions
        WHERE screen_hash = :screenHash
        GROUP BY element_hash
        ORDER BY interaction_count DESC
        LIMIT :limit
    """)
    suspend fun getMostInteractedElements(screenHash: String, limit: Int = 20): List<ElementInteractionCount>

    /**
     * Get successful vs failed interaction ratio
     *
     * @param elementHash Hash of the element
     * @return Pair of (successful count, failed count)
     */
    @Query("""
        SELECT
            SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful,
            SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed
        FROM user_interactions
        WHERE element_hash = :elementHash
    """)
    suspend fun getSuccessFailureRatio(elementHash: String): InteractionRatio?

    /**
     * Get average visibility duration before interaction
     *
     * Measures average "decision time" for an element.
     * Short duration = quick decision, long duration = slower decision.
     *
     * @param elementHash Hash of the element
     * @return Average visibility duration in milliseconds, or null if no data
     */
    @Query("SELECT AVG(visibility_duration) FROM user_interactions WHERE element_hash = :elementHash AND visibility_duration IS NOT NULL")
    suspend fun getAverageVisibilityDuration(elementHash: String): Long?
}

/**
 * Data class for element interaction count results
 */
data class ElementInteractionCount(
    val element_hash: String,
    val interaction_count: Int
)

/**
 * Data class for success/failure ratio results
 */
data class InteractionRatio(
    val successful: Int,
    val failed: Int
)
