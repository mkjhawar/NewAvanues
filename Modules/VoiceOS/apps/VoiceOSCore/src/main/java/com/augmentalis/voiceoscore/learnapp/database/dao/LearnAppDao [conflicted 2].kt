/**
 * LearnAppDao.kt - DAO interface for LearnApp database operations
 * Modified: 2025-11-27 (Removed Room annotations for SQLDelight migration)
 */
package com.augmentalis.voiceoscore.learnapp.database.dao

import com.augmentalis.voiceoscore.learnapp.database.entities.*

/**
 * LearnApp DAO interface
 * Implemented by LearnAppDaoAdapter (uses SQLDelight)
 */
interface LearnAppDao {
    /**
     * Execute a block of database operations as a single transaction
     * All operations succeed together or fail together (atomicity)
     *
     * @param block The block of operations to execute
     * @return The result of the block
     */
    suspend fun <R> transaction(block: suspend LearnAppDao.() -> R): R

    // Learned Apps
    suspend fun insertLearnedApp(app: LearnedAppEntity)
    suspend fun insertLearnedAppMinimal(app: LearnedAppEntity): Long
    suspend fun getLearnedApp(packageName: String): LearnedAppEntity?
    suspend fun getAllLearnedApps(): List<LearnedAppEntity>
    suspend fun updateAppHash(packageName: String, newHash: String, timestamp: Long)
    suspend fun updateAppStats(packageName: String, totalScreens: Int, totalElements: Int)
    suspend fun deleteLearnedApp(app: LearnedAppEntity)

    // Exploration Sessions
    suspend fun insertExplorationSession(session: ExplorationSessionEntity)
    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity?
    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity>
    suspend fun updateSessionStatus(sessionId: String, status: String, completedAt: Long, durationMs: Long)
    suspend fun deleteExplorationSession(session: ExplorationSessionEntity)
    suspend fun deleteSessionsForPackage(packageName: String)
    suspend fun getLatestSessionForPackage(packageName: String, status: String): ExplorationSessionEntity?

    // Navigation Edges
    suspend fun insertNavigationEdge(edge: NavigationEdgeEntity)
    suspend fun insertNavigationEdges(edges: List<NavigationEdgeEntity>)
    suspend fun getNavigationGraph(packageName: String): List<NavigationEdgeEntity>
    suspend fun getOutgoingEdges(screenHash: String): List<NavigationEdgeEntity>
    suspend fun getIncomingEdges(screenHash: String): List<NavigationEdgeEntity>
    suspend fun getEdgesForSession(sessionId: String): List<NavigationEdgeEntity>
    suspend fun deleteNavigationGraph(packageName: String)
    suspend fun deleteNavigationEdgesForSession(sessionId: String)

    // Screen States
    suspend fun insertScreenState(state: ScreenStateEntity)
    suspend fun getScreenState(hash: String): ScreenStateEntity?
    suspend fun getScreenStatesForPackage(packageName: String): List<ScreenStateEntity>
    suspend fun deleteScreenState(state: ScreenStateEntity)
    suspend fun deleteScreenStatesForPackage(packageName: String)

    // Complex Queries
    suspend fun getTotalScreensForPackage(packageName: String): Int
    suspend fun getTotalEdgesForPackage(packageName: String): Int
}
