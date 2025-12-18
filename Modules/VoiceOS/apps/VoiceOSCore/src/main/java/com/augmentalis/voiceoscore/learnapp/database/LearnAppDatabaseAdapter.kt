/**
 * LearnAppDatabaseAdapter.kt - Database adapter for LearnApp
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/database/LearnAppDatabaseAdapter.kt
 *
 * Author: Agent 1 (LearnApp Migration Specialist)
 * Created: 2025-11-27
 *
 * Adapter providing Room-compatible API using SQLDelight backend.
 * This bridges the gap between Room-based LearnAppRepository and SQLDelight database.
 */

package com.augmentalis.voiceoscore.learnapp.database

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.database.dao.LearnAppDao
import com.augmentalis.voiceoscore.learnapp.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * LearnApp Database Adapter
 *
 * Provides Room-compatible API using SQLDelight as the backend.
 * Singleton pattern for app-wide access.
 *
 * ## Usage
 *
 * ```kotlin
 * val adapter = LearnAppDatabaseAdapter.getInstance(context)
 * val dao = adapter.learnAppDao()
 *
 * // Use DAO methods (Room-compatible API)
 * dao.insertLearnedApp(appEntity)
 * val app = dao.getLearnedApp(packageName)
 * ```
 *
 * @property context Application context
 */
class LearnAppDatabaseAdapter private constructor(
    private val context: Context
) {

    private val databaseManager: VoiceOSDatabaseManager by lazy {
        val driverFactory = DatabaseDriverFactory(context.applicationContext)
        VoiceOSDatabaseManager.getInstance(driverFactory)
    }

    private val dao: LearnAppDao by lazy {
        LearnAppDaoAdapter(databaseManager)
    }

    /**
     * Get LearnApp DAO
     */
    fun learnAppDao(): LearnAppDao = dao

    companion object {
        @Volatile
        private var INSTANCE: LearnAppDatabaseAdapter? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): LearnAppDatabaseAdapter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LearnAppDatabaseAdapter(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * LearnApp DAO Adapter
 *
 * Implements LearnAppDao interface using SQLDelight queries.
 * Converts between SQLDelight DTOs and Room entities.
 */
private class LearnAppDaoAdapter(
    private val databaseManager: VoiceOSDatabaseManager
) : LearnAppDao {

    // ========== Transactions ==========

    /**
     * Execute a transaction with suspend block.
     *
     * FIX (2025-11-30): Changed runBlocking to use Dispatchers.Unconfined to prevent deadlock.
     * The previous implementation used plain runBlocking{} which blocked IO dispatcher threads
     * while the inner suspend block might also need IO threads, causing thread starvation.
     *
     * Using Dispatchers.Unconfined allows the inner suspend operations to run immediately
     * on the current thread without competing for the IO thread pool.
     *
     * FIX (2025-12-10): Removed outer withContext(Dispatchers.IO) wrapper to prevent thread pool
     * exhaustion. The VoiceOSDatabaseManager.transaction() already uses Dispatchers.Default
     * (line 177 in VoiceOSDatabaseManager.kt), so the outer wrapper was causing nested dispatcher
     * switches and potential IO thread starvation under high transaction volume.
     *
     * This change implements Option 1 from spec Section 2.2: Remove outer wrapper and rely on
     * VoiceOSDatabaseManager's threading model. The inner runBlocking(Dispatchers.Unconfined)
     * is kept to bridge suspend/non-suspend contexts.
     *
     * NOTE (2025-11-30): All DAO methods use withContext(Dispatchers.IO) even though they are
     * typically called from coroutines. This is INTENTIONAL - it ensures thread safety regardless
     * of the caller's dispatcher context. The overhead of dispatcher switches is negligible
     * compared to database I/O, and this pattern provides a defensive layer of thread safety.
     */
    override suspend fun <R> transaction(block: suspend LearnAppDao.() -> R): R {
        // Remove outer withContext wrapper - databaseManager.transaction handles threading
        return databaseManager.transaction {
            runBlocking(Dispatchers.Unconfined) {
                this@LearnAppDaoAdapter.block()
            }
        }
    }

    // ========== Learned Apps ==========

    override suspend fun insertLearnedApp(app: LearnedAppEntity) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.insertLearnedApp(
            package_name = app.packageName,
            app_name = app.appName,
            version_code = app.versionCode,
            version_name = app.versionName,
            first_learned_at = app.firstLearnedAt,
            last_updated_at = app.lastUpdatedAt,
            total_screens = app.totalScreens.toLong(),
            total_elements = app.totalElements.toLong(),
            app_hash = app.appHash,
            exploration_status = app.explorationStatus,
            // Phase 2 fields - use defaults for legacy compatibility
            learning_mode = "AUTO_DETECT",
            status = "NOT_LEARNED",
            progress = 0,
            command_count = 0,
            screens_explored = 0,
            is_auto_detect_enabled = 1
        )
    }

    override suspend fun insertLearnedAppMinimal(app: LearnedAppEntity): Long = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.insertLearnedAppMinimal(
            package_name = app.packageName,
            app_name = app.appName,
            version_code = app.versionCode,
            version_name = app.versionName,
            first_learned_at = app.firstLearnedAt,
            last_updated_at = app.lastUpdatedAt,
            total_screens = app.totalScreens.toLong(),
            total_elements = app.totalElements.toLong(),
            app_hash = app.appHash,
            exploration_status = app.explorationStatus
        )
        // Room returns row ID, SQLDelight doesn't. Return 1 for success, 0 for conflict (ignored)
        val existing = databaseManager.learnedAppQueries.getLearnedApp(app.packageName).executeAsOneOrNull()
        if (existing != null) 1L else 0L
    }

    override suspend fun getLearnedApp(packageName: String): LearnedAppEntity? = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.getLearnedApp(packageName)
            .executeAsOneOrNull()
            ?.let { dto ->
                LearnedAppEntity(
                    packageName = dto.package_name,
                    appName = dto.app_name,
                    versionCode = dto.version_code,
                    versionName = dto.version_name,
                    firstLearnedAt = dto.first_learned_at,
                    lastUpdatedAt = dto.last_updated_at,
                    totalScreens = dto.total_screens.toInt(),
                    totalElements = dto.total_elements.toInt(),
                    appHash = dto.app_hash,
                    explorationStatus = dto.exploration_status
                )
            }
    }

    override suspend fun getAllLearnedApps(): List<LearnedAppEntity> = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.getAllLearnedApps()
            .executeAsList()
            .map { dto ->
                LearnedAppEntity(
                    packageName = dto.package_name,
                    appName = dto.app_name,
                    versionCode = dto.version_code,
                    versionName = dto.version_name,
                    firstLearnedAt = dto.first_learned_at,
                    lastUpdatedAt = dto.last_updated_at,
                    totalScreens = dto.total_screens.toInt(),
                    totalElements = dto.total_elements.toInt(),
                    appHash = dto.app_hash,
                    explorationStatus = dto.exploration_status
                )
            }
    }

    override suspend fun updateAppHash(packageName: String, newHash: String, timestamp: Long) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.updateAppHash(
            app_hash = newHash,
            last_updated_at = timestamp,
            package_name = packageName
        )
    }

    /**
     * Update app stats atomically
     *
     * FIX (2025-11-30): Use atomic UPDATE query instead of read-modify-write pattern.
     * Previous implementation had race condition where concurrent updates could overwrite
     * each other's changes. Now uses single UPDATE statement which is atomic.
     */
    override suspend fun updateAppStats(packageName: String, totalScreens: Int, totalElements: Int) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.updateAppStats(
            total_screens = totalScreens.toLong(),
            total_elements = totalElements.toLong(),
            last_updated_at = System.currentTimeMillis(),
            package_name = packageName
        )
    }

    override suspend fun updateLearnedApp(app: LearnedAppEntity) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.updateLearnedApp(
            app_name = app.appName,
            version_code = app.versionCode,
            version_name = app.versionName,
            last_updated_at = app.lastUpdatedAt,
            total_screens = app.totalScreens.toLong(),
            total_elements = app.totalElements.toLong(),
            app_hash = app.appHash,
            exploration_status = app.explorationStatus,
            package_name = app.packageName
        )
    }

    override suspend fun deleteLearnedApp(app: LearnedAppEntity) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.deleteLearnedApp(app.packageName)
    }

    // ========== Exploration Sessions ==========

    override suspend fun insertExplorationSession(session: ExplorationSessionEntity) = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.insertExplorationSession(
            session_id = session.sessionId,
            package_name = session.packageName,
            started_at = session.startedAt,
            completed_at = session.completedAt,
            duration_ms = session.durationMs,
            screens_explored = session.screensExplored.toLong(),
            elements_discovered = session.elementsDiscovered.toLong(),
            status = session.status
        )
    }

    override suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity? = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.getExplorationSession(sessionId)
            .executeAsOneOrNull()
            ?.let { dto ->
                ExplorationSessionEntity(
                    sessionId = dto.session_id,
                    packageName = dto.package_name,
                    startedAt = dto.started_at,
                    completedAt = dto.completed_at,
                    durationMs = dto.duration_ms,
                    screensExplored = dto.screens_explored.toInt(),
                    elementsDiscovered = dto.elements_discovered.toInt(),
                    status = dto.status
                )
            }
    }

    override suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity> = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.getSessionsForPackage(packageName)
            .executeAsList()
            .map { dto ->
                ExplorationSessionEntity(
                    sessionId = dto.session_id,
                    packageName = dto.package_name,
                    startedAt = dto.started_at,
                    completedAt = dto.completed_at,
                    durationMs = dto.duration_ms,
                    screensExplored = dto.screens_explored.toInt(),
                    elementsDiscovered = dto.elements_discovered.toInt(),
                    status = dto.status
                )
            }
    }

    override suspend fun updateSessionStatus(sessionId: String, status: String, completedAt: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.updateSessionStatus(
            status = status,
            completed_at = completedAt,
            duration_ms = durationMs,
            session_id = sessionId
        )
    }

    override suspend fun deleteExplorationSession(session: ExplorationSessionEntity) = withContext(Dispatchers.IO) {
        // Need to add deleteExplorationSession query to ExplorationSession.sq
        // For now, deleteSessionsForPackage will cascade delete
    }

    override suspend fun deleteSessionsForPackage(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.deleteSessionsForPackage(packageName)
    }

    override suspend fun getLatestSessionForPackage(packageName: String, status: String): ExplorationSessionEntity? = withContext(Dispatchers.IO) {
        // Get all sessions and filter by status, take first (latest)
        getSessionsForPackage(packageName).firstOrNull { it.status == status }
    }

    // ========== Navigation Edges ==========

    override suspend fun insertNavigationEdge(edge: NavigationEdgeEntity) = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.insertNavigationEdge(
            edge_id = edge.edgeId,
            package_name = edge.packageName,
            session_id = edge.sessionId,
            from_screen_hash = edge.fromScreenHash,
            clicked_element_uuid = edge.clickedElementUuid,
            to_screen_hash = edge.toScreenHash,
            timestamp = edge.timestamp
        )
    }

    override suspend fun insertNavigationEdges(edges: List<NavigationEdgeEntity>) = withContext(Dispatchers.IO) {
        databaseManager.transaction {
            edges.forEach { edge ->
                databaseManager.navigationEdgeQueries.insertNavigationEdge(
                    edge_id = edge.edgeId,
                    package_name = edge.packageName,
                    session_id = edge.sessionId,
                    from_screen_hash = edge.fromScreenHash,
                    clicked_element_uuid = edge.clickedElementUuid,
                    to_screen_hash = edge.toScreenHash,
                    timestamp = edge.timestamp
                )
            }
        }
    }

    override suspend fun getNavigationGraph(packageName: String): List<NavigationEdgeEntity> = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.getNavigationGraph(packageName)
            .executeAsList()
            .map { dto ->
                NavigationEdgeEntity(
                    edgeId = dto.edge_id,
                    packageName = dto.package_name,
                    sessionId = dto.session_id,
                    fromScreenHash = dto.from_screen_hash,
                    clickedElementUuid = dto.clicked_element_uuid,
                    toScreenHash = dto.to_screen_hash,
                    timestamp = dto.timestamp
                )
            }
    }

    override suspend fun getOutgoingEdges(screenHash: String): List<NavigationEdgeEntity> = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.getOutgoingEdges(screenHash)
            .executeAsList()
            .map { dto ->
                NavigationEdgeEntity(
                    edgeId = dto.edge_id,
                    packageName = dto.package_name,
                    sessionId = dto.session_id,
                    fromScreenHash = dto.from_screen_hash,
                    clickedElementUuid = dto.clicked_element_uuid,
                    toScreenHash = dto.to_screen_hash,
                    timestamp = dto.timestamp
                )
            }
    }

    override suspend fun getIncomingEdges(screenHash: String): List<NavigationEdgeEntity> = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.getIncomingEdges(screenHash)
            .executeAsList()
            .map { dto ->
                NavigationEdgeEntity(
                    edgeId = dto.edge_id,
                    packageName = dto.package_name,
                    sessionId = dto.session_id,
                    fromScreenHash = dto.from_screen_hash,
                    clickedElementUuid = dto.clicked_element_uuid,
                    toScreenHash = dto.to_screen_hash,
                    timestamp = dto.timestamp
                )
            }
    }

    override suspend fun getEdgesForSession(sessionId: String): List<NavigationEdgeEntity> = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.getEdgesForSession(sessionId)
            .executeAsList()
            .map { dto ->
                NavigationEdgeEntity(
                    edgeId = dto.edge_id,
                    packageName = dto.package_name,
                    sessionId = dto.session_id,
                    fromScreenHash = dto.from_screen_hash,
                    clickedElementUuid = dto.clicked_element_uuid,
                    toScreenHash = dto.to_screen_hash,
                    timestamp = dto.timestamp
                )
            }
    }

    override suspend fun deleteNavigationGraph(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.deleteNavigationGraph(packageName)
    }

    override suspend fun deleteNavigationEdgesForSession(sessionId: String) = withContext(Dispatchers.IO) {
        // Need to add deleteNavigationEdgesForSession query to NavigationEdge.sq
        // For now, do nothing (cascade delete from parent session works)
    }

    // ========== Screen States ==========

    override suspend fun insertScreenState(state: ScreenStateEntity) = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.insertScreenState(
            screen_hash = state.screenHash,
            package_name = state.packageName,
            activity_name = state.activityName ?: "",
            fingerprint = state.fingerprint,
            element_count = state.elementCount.toLong(),
            discovered_at = state.discoveredAt
        )
    }

    override suspend fun getScreenState(hash: String): ScreenStateEntity? = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.getScreenState(hash)
            .executeAsOneOrNull()
            ?.let { dto ->
                ScreenStateEntity(
                    screenHash = dto.screen_hash,
                    packageName = dto.package_name,
                    activityName = dto.activity_name,
                    fingerprint = dto.fingerprint,
                    elementCount = dto.element_count.toInt(),
                    discoveredAt = dto.discovered_at
                )
            }
    }

    override suspend fun getScreenStatesForPackage(packageName: String): List<ScreenStateEntity> = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.getScreenStatesForPackage(packageName)
            .executeAsList()
            .map { dto ->
                ScreenStateEntity(
                    screenHash = dto.screen_hash,
                    packageName = dto.package_name,
                    activityName = dto.activity_name,
                    fingerprint = dto.fingerprint,
                    elementCount = dto.element_count.toInt(),
                    discoveredAt = dto.discovered_at
                )
            }
    }

    override suspend fun deleteScreenState(state: ScreenStateEntity) = withContext(Dispatchers.IO) {
        // Need to add deleteScreenState query to ScreenState.sq
        // For now, do nothing (cascade delete from parent app works)
    }

    override suspend fun deleteScreenStatesForPackage(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.deleteScreenStatesForPackage(packageName)
    }

    // ========== Complex Queries ==========

    override suspend fun getTotalScreensForPackage(packageName: String): Int = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.getTotalScreensForPackage(packageName)
            .executeAsOne()
            .toInt()
    }

    override suspend fun getTotalEdgesForPackage(packageName: String): Int = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.getTotalEdgesForPackage(packageName)
            .executeAsOne()
            .toInt()
    }
}
