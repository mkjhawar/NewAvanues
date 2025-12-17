/**
 * ElementStateHistoryDao.kt - Data access object for element state history
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity

/**
 * Element State History DAO
 *
 * Provides database access methods for tracking and querying element state changes
 * over time including checked status, selection, enabled/disabled, visibility, and focus.
 *
 * Use Cases:
 * - Recording state changes as they occur
 * - State-aware voice commands ("check the box" vs "uncheck the box")
 * - Understanding element behavior patterns
 * - Debugging UI state issues
 * - Identifying user vs system-triggered changes
 *
 * Performance Notes:
 * - All queries use indexed columns for optimal performance
 * - Limit parameters prevent excessive memory usage
 * - Batch insert for multiple state changes
 */
@Dao
interface ElementStateHistoryDao {

    /**
     * Insert a single state change
     *
     * @param stateChange The state change to record
     * @return Row ID of inserted state change
     */
    @Insert
    suspend fun insert(stateChange: ElementStateHistoryEntity): Long

    /**
     * Insert multiple state changes in a batch
     *
     * Use for bulk operations to improve performance.
     *
     * @param stateChanges List of state changes to record
     */
    @Insert
    suspend fun insertAll(stateChanges: List<ElementStateHistoryEntity>)

    /**
     * Get all state changes for a specific element
     *
     * Ordered by most recent first. Shows complete state history.
     *
     * @param elementHash Hash of the element
     * @return List of state changes, newest first
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash ORDER BY changed_at DESC")
    suspend fun getStateHistoryForElement(elementHash: String): List<ElementStateHistoryEntity>

    /**
     * Get state changes for a specific element and state type
     *
     * Filter to only one type of state change (e.g., only checked/unchecked events).
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state (see StateType constants)
     * @return List of state changes of specified type, newest first
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType ORDER BY changed_at DESC")
    suspend fun getStateHistoryByType(elementHash: String, stateType: String): List<ElementStateHistoryEntity>

    /**
     * Get most recent state for an element and state type
     *
     * CRITICAL for state-aware commands. This tells us the current state
     * so we can generate appropriate commands.
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state to check
     * @return Most recent state change, or null if no history
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType ORDER BY changed_at DESC LIMIT 1")
    suspend fun getCurrentState(elementHash: String, stateType: String): ElementStateHistoryEntity?

    /**
     * Get all state changes for a screen
     *
     * See all state changes that occurred on a specific screen.
     *
     * @param screenHash Hash of the screen
     * @return List of state changes on this screen, newest first
     */
    @Query("SELECT * FROM element_state_history WHERE screen_hash = :screenHash ORDER BY changed_at DESC")
    suspend fun getStateHistoryForScreen(screenHash: String): List<ElementStateHistoryEntity>

    /**
     * Get state changes within a time range
     *
     * Useful for session analysis or debugging.
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return List of state changes in time range, newest first
     */
    @Query("SELECT * FROM element_state_history WHERE changed_at BETWEEN :startTime AND :endTime ORDER BY changed_at DESC")
    suspend fun getStateChangesInTimeRange(startTime: Long, endTime: Long): List<ElementStateHistoryEntity>

    /**
     * Get state changes by trigger source
     *
     * Filter to only user-triggered, system-triggered, or app-event changes.
     *
     * @param triggerSource Trigger source (see TriggerSource constants)
     * @param limit Maximum number of results (default 100)
     * @return List of state changes from specified trigger source
     */
    @Query("SELECT * FROM element_state_history WHERE triggered_by = :triggerSource ORDER BY changed_at DESC LIMIT :limit")
    suspend fun getStateChangesByTrigger(triggerSource: String, limit: Int = 100): List<ElementStateHistoryEntity>

    /**
     * Get user-triggered state changes for an element
     *
     * Only show state changes caused by user actions (clicks, voice commands, etc.).
     *
     * @param elementHash Hash of the element
     * @return List of user-triggered state changes, newest first
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash AND triggered_by LIKE 'user_%' ORDER BY changed_at DESC")
    suspend fun getUserTriggeredStateChanges(elementHash: String): List<ElementStateHistoryEntity>

    /**
     * Get count of state changes for an element
     *
     * Useful for identifying volatile elements (many state changes).
     *
     * @param elementHash Hash of the element
     * @return Total number of state changes
     */
    @Query("SELECT COUNT(*) FROM element_state_history WHERE element_hash = :elementHash")
    suspend fun getStateChangeCount(elementHash: String): Int

    /**
     * Get count of state changes by type
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state
     * @return Count of state changes of specified type
     */
    @Query("SELECT COUNT(*) FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType")
    suspend fun getStateChangeCountByType(elementHash: String, stateType: String): Int

    /**
     * Delete old state changes
     *
     * Cleanup method to prevent database bloat. Call periodically
     * to remove state changes older than a certain date.
     *
     * @param cutoffTime Timestamp - state changes before this are deleted
     * @return Number of state changes deleted
     */
    @Query("DELETE FROM element_state_history WHERE changed_at < :cutoffTime")
    suspend fun deleteOldStateChanges(cutoffTime: Long): Int

    /**
     * Get elements with frequent state changes
     *
     * Identifies volatile or dynamic UI elements based on state change frequency.
     *
     * @param threshold Minimum number of state changes to include
     * @param limit Maximum number of results (default 20)
     * @return List of element hashes sorted by state change frequency
     */
    @Query("""
        SELECT element_hash, COUNT(*) as change_count
        FROM element_state_history
        GROUP BY element_hash
        HAVING change_count >= :threshold
        ORDER BY change_count DESC
        LIMIT :limit
    """)
    suspend fun getVolatileElements(threshold: Int = 5, limit: Int = 20): List<ElementStateChangeCount>

    /**
     * Get state toggle frequency
     *
     * For binary states (checked/unchecked, visible/hidden), calculate
     * how often the state toggles.
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state
     * @return Number of times state changed (toggle count)
     */
    @Query("SELECT COUNT(*) FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType")
    suspend fun getToggleFrequency(elementHash: String, stateType: String): Int

    /**
     * Get last state change time
     *
     * When did the state last change?
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state
     * @return Timestamp of last change, or null if no history
     */
    @Query("SELECT changed_at FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType ORDER BY changed_at DESC LIMIT 1")
    suspend fun getLastStateChangeTime(elementHash: String, stateType: String): Long?

    /**
     * Check if element state changed recently
     *
     * Useful for detecting if state is stable or volatile.
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state
     * @param timeWindowMs Time window in milliseconds
     * @return True if state changed within time window
     */
    @Query("SELECT COUNT(*) > 0 FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType AND changed_at >= :cutoffTime")
    suspend fun hasStateChangedRecently(elementHash: String, stateType: String, cutoffTime: Long): Boolean

    /**
     * Get state change pattern for element
     *
     * Returns sequence of state changes showing pattern over time.
     * Useful for understanding element behavior.
     *
     * @param elementHash Hash of the element
     * @param stateType Type of state
     * @param limit Maximum number of results (default 50)
     * @return List of state values in chronological order (oldest first)
     */
    @Query("SELECT new_value FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType ORDER BY changed_at ASC LIMIT :limit")
    suspend fun getStateChangePattern(elementHash: String, stateType: String, limit: Int = 50): List<String?>
}

/**
 * Data class for element state change count results
 */
data class ElementStateChangeCount(
    val element_hash: String,
    val change_count: Int
)
