/**
 * LearnAppDao.kt - Room DAO for LearnApp database operations
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/database/dao/LearnAppDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room DAO interface for LearnApp database operations
 */

package com.augmentalis.learnapp.database.dao

import androidx.room.*
import com.augmentalis.learnapp.database.entities.*

/**
 * LearnApp DAO
 *
 * Room DAO for all LearnApp database operations.
 *
 * @since 1.0.0
 */
@Dao
interface LearnAppDao {

    // ========== Learned Apps ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearnedApp(app: LearnedAppEntity)

    @Query("SELECT * FROM learned_apps WHERE package_name = :packageName")
    suspend fun getLearnedApp(packageName: String): LearnedAppEntity?

    @Query("SELECT * FROM learned_apps")
    suspend fun getAllLearnedApps(): List<LearnedAppEntity>

    @Query("UPDATE learned_apps SET app_hash = :newHash, last_updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun updateAppHash(packageName: String, newHash: String, timestamp: Long)

    @Query("UPDATE learned_apps SET total_screens = :totalScreens, total_elements = :totalElements WHERE package_name = :packageName")
    suspend fun updateAppStats(packageName: String, totalScreens: Int, totalElements: Int)

    @Delete
    suspend fun deleteLearnedApp(app: LearnedAppEntity)

    // ========== Exploration Sessions ==========

    @Insert
    suspend fun insertExplorationSession(session: ExplorationSessionEntity)

    @Query("SELECT * FROM exploration_sessions WHERE session_id = :sessionId")
    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity?

    @Query("SELECT * FROM exploration_sessions WHERE package_name = :packageName ORDER BY started_at DESC")
    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity>

    @Query("UPDATE exploration_sessions SET status = :status, completed_at = :completedAt, duration_ms = :durationMs WHERE session_id = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String, completedAt: Long, durationMs: Long)

    @Delete
    suspend fun deleteExplorationSession(session: ExplorationSessionEntity)

    // ========== Navigation Edges ==========

    @Insert
    suspend fun insertNavigationEdge(edge: NavigationEdgeEntity)

    @Insert
    suspend fun insertNavigationEdges(edges: List<NavigationEdgeEntity>)

    @Query("SELECT * FROM navigation_edges WHERE package_name = :packageName")
    suspend fun getNavigationGraph(packageName: String): List<NavigationEdgeEntity>

    @Query("SELECT * FROM navigation_edges WHERE from_screen_hash = :screenHash")
    suspend fun getOutgoingEdges(screenHash: String): List<NavigationEdgeEntity>

    @Query("SELECT * FROM navigation_edges WHERE to_screen_hash = :screenHash")
    suspend fun getIncomingEdges(screenHash: String): List<NavigationEdgeEntity>

    @Query("SELECT * FROM navigation_edges WHERE session_id = :sessionId")
    suspend fun getEdgesForSession(sessionId: String): List<NavigationEdgeEntity>

    @Query("DELETE FROM navigation_edges WHERE package_name = :packageName")
    suspend fun deleteNavigationGraph(packageName: String)

    // ========== Screen States ==========

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScreenState(state: ScreenStateEntity)

    @Query("SELECT * FROM screen_states WHERE screen_hash = :hash")
    suspend fun getScreenState(hash: String): ScreenStateEntity?

    @Query("SELECT * FROM screen_states WHERE package_name = :packageName")
    suspend fun getScreenStatesForPackage(packageName: String): List<ScreenStateEntity>

    @Delete
    suspend fun deleteScreenState(state: ScreenStateEntity)

    // ========== Complex Queries ==========

    @Query("""
        SELECT COUNT(*)
        FROM screen_states
        WHERE package_name = :packageName
    """)
    suspend fun getTotalScreensForPackage(packageName: String): Int

    @Query("""
        SELECT COUNT(*)
        FROM navigation_edges
        WHERE package_name = :packageName
    """)
    suspend fun getTotalEdgesForPackage(packageName: String): Int

    @Query("""
        SELECT * FROM exploration_sessions
        WHERE package_name = :packageName
        AND status = :status
        ORDER BY started_at DESC
        LIMIT 1
    """)
    suspend fun getLastSessionWithStatus(packageName: String, status: String): ExplorationSessionEntity?
}
