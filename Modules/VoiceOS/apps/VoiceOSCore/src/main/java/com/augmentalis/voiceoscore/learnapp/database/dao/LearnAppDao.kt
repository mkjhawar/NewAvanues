/**
 * LearnAppDao.kt - DAO interface for LearnApp database operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Interface defining database operations for LearnApp.
 * Implementation uses SQLDelight adapter pattern (not Room).
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.ExplorationSessionEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.LearnedAppEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.NavigationEdgeEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.ScreenStateEntity

/**
 * LearnApp DAO Interface
 *
 * Defines database operations for LearnApp functionality.
 * This is an interface layer over SQLDelight - implementations use
 * SQLDelight queries rather than Room annotations.
 *
 * @see com.augmentalis.voiceoscore.learnapp.database.LearnAppDatabaseAdapter
 */
interface LearnAppDao {

    // ========== Transactions ==========

    /**
     * Execute operations in a database transaction
     *
     * @param block Suspend block to execute within transaction
     * @return Result of the block execution
     */
    suspend fun <R> transaction(block: suspend LearnAppDao.() -> R): R

    // ========== Learned Apps ==========

    /**
     * Insert a new learned app
     *
     * @param app LearnedAppEntity to insert
     */
    suspend fun insertLearnedApp(app: LearnedAppEntity)

    /**
     * Insert a learned app with minimal fields (for auto-detect mode)
     *
     * @param app LearnedAppEntity to insert
     * @return Row ID of inserted record, or 0 if conflict
     */
    suspend fun insertLearnedAppMinimal(app: LearnedAppEntity): Long

    /**
     * Get a learned app by package name
     *
     * @param packageName Package name to look up
     * @return LearnedAppEntity or null if not found
     */
    suspend fun getLearnedApp(packageName: String): LearnedAppEntity?

    /**
     * Get all learned apps
     *
     * @return List of all LearnedAppEntity records
     */
    suspend fun getAllLearnedApps(): List<LearnedAppEntity>

    /**
     * Update app hash after re-learning
     *
     * @param packageName Package name to update
     * @param newHash New app hash
     * @param timestamp Update timestamp
     */
    suspend fun updateAppHash(packageName: String, newHash: String, timestamp: Long)

    /**
     * Update app statistics
     *
     * @param packageName Package name to update
     * @param totalScreens New total screens count
     * @param totalElements New total elements count
     */
    suspend fun updateAppStats(packageName: String, totalScreens: Int, totalElements: Int)

    /**
     * Update a learned app
     *
     * @param app LearnedAppEntity with updated values
     */
    suspend fun updateLearnedApp(app: LearnedAppEntity)

    /**
     * Delete a learned app
     *
     * @param app LearnedAppEntity to delete
     */
    suspend fun deleteLearnedApp(app: LearnedAppEntity)

    // ========== Exploration Sessions ==========

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
     * Delete an exploration session
     *
     * @param session ExplorationSessionEntity to delete
     */
    suspend fun deleteExplorationSession(session: ExplorationSessionEntity)

    /**
     * Delete all sessions for a package
     *
     * @param packageName Package name
     */
    suspend fun deleteSessionsForPackage(packageName: String)

    /**
     * Get latest session for a package with specific status
     *
     * @param packageName Package name
     * @param status Status to filter by
     * @return Latest ExplorationSessionEntity or null
     */
    suspend fun getLatestSessionForPackage(packageName: String, status: String): ExplorationSessionEntity?

    // ========== Navigation Edges ==========

    /**
     * Insert a navigation edge
     *
     * @param edge NavigationEdgeEntity to insert
     */
    suspend fun insertNavigationEdge(edge: NavigationEdgeEntity)

    /**
     * Insert multiple navigation edges in batch
     *
     * @param edges List of NavigationEdgeEntity to insert
     */
    suspend fun insertNavigationEdges(edges: List<NavigationEdgeEntity>)

    /**
     * Get navigation graph for a package
     *
     * @param packageName Package name
     * @return List of NavigationEdgeEntity records
     */
    suspend fun getNavigationGraph(packageName: String): List<NavigationEdgeEntity>

    /**
     * Get outgoing edges from a screen
     *
     * @param screenHash Screen hash to get edges from
     * @return List of outgoing NavigationEdgeEntity records
     */
    suspend fun getOutgoingEdges(screenHash: String): List<NavigationEdgeEntity>

    /**
     * Get incoming edges to a screen
     *
     * @param screenHash Screen hash to get edges to
     * @return List of incoming NavigationEdgeEntity records
     */
    suspend fun getIncomingEdges(screenHash: String): List<NavigationEdgeEntity>

    /**
     * Get edges for a session
     *
     * @param sessionId Session ID
     * @return List of NavigationEdgeEntity records for the session
     */
    suspend fun getEdgesForSession(sessionId: String): List<NavigationEdgeEntity>

    /**
     * Delete navigation graph for a package
     *
     * @param packageName Package name
     */
    suspend fun deleteNavigationGraph(packageName: String)

    /**
     * Delete navigation edges for a session
     *
     * @param sessionId Session ID
     */
    suspend fun deleteNavigationEdgesForSession(sessionId: String)

    // ========== Screen States ==========

    /**
     * Insert a screen state
     *
     * @param state ScreenStateEntity to insert
     */
    suspend fun insertScreenState(state: ScreenStateEntity)

    /**
     * Get a screen state by hash
     *
     * @param hash Screen hash
     * @return ScreenStateEntity or null if not found
     */
    suspend fun getScreenState(hash: String): ScreenStateEntity?

    /**
     * Get all screen states for a package
     *
     * @param packageName Package name
     * @return List of ScreenStateEntity records
     */
    suspend fun getScreenStatesForPackage(packageName: String): List<ScreenStateEntity>

    /**
     * Delete a screen state
     *
     * @param state ScreenStateEntity to delete
     */
    suspend fun deleteScreenState(state: ScreenStateEntity)

    /**
     * Delete all screen states for a package
     *
     * @param packageName Package name
     */
    suspend fun deleteScreenStatesForPackage(packageName: String)

    // ========== Complex Queries ==========

    /**
     * Get total screens discovered for a package
     *
     * @param packageName Package name
     * @return Total screen count
     */
    suspend fun getTotalScreensForPackage(packageName: String): Int

    /**
     * Get total edges for a package
     *
     * @param packageName Package name
     * @return Total edge count
     */
    suspend fun getTotalEdgesForPackage(packageName: String): Int
}
