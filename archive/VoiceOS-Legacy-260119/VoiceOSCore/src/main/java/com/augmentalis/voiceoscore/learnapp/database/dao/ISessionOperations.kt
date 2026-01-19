/**
 * ISessionOperations.kt - Interface for exploration session database operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Purpose: Segregated interface for session operations (Interface Segregation Principle)
 * Part of Phase 1: SOLID Refactoring - Interface Segregation
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.ExplorationSessionEntity

/**
 * Interface for exploration session database operations
 *
 * Focused interface with 5 core operations for exploration sessions.
 * Follows Interface Segregation Principle - clients depend only on methods they use.
 *
 * @see LearnAppDao Aggregate interface combining all operations
 */
interface ISessionOperations {

    /**
     * Insert a new exploration session
     *
     * @param session ExplorationSessionEntity to insert
     */
    suspend fun insertExplorationSession(session: ExplorationSessionEntity)

    /**
     * Get an exploration session by ID
     *
     * @param sessionId Session ID to look up
     * @return ExplorationSessionEntity or null if not found
     */
    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity?

    /**
     * Get all sessions for a package
     *
     * @param packageName Package name to look up
     * @return List of ExplorationSessionEntity records
     */
    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity>

    /**
     * Update session status
     *
     * @param sessionId Session ID to update
     * @param status New status
     * @param completedAt Completion timestamp
     * @param durationMs Duration in milliseconds
     */
    suspend fun updateSessionStatus(sessionId: String, status: String, completedAt: Long, durationMs: Long)

    /**
     * Delete all sessions for a package
     *
     * @param packageName Package name
     */
    suspend fun deleteSessionsForPackage(packageName: String)
}
