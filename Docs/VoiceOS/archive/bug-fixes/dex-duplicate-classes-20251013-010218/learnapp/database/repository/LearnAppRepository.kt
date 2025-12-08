/**
 * LearnAppRepository.kt - Repository pattern for LearnApp data access
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/database/repository/LearnAppRepository.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Repository providing clean API for LearnApp database operations
 */

package com.augmentalis.learnapp.database.repository

import com.augmentalis.learnapp.database.dao.LearnAppDao
import com.augmentalis.learnapp.database.entities.*
import com.augmentalis.learnapp.models.ExplorationStats
import com.augmentalis.learnapp.models.ScreenState
import com.augmentalis.learnapp.navigation.NavigationGraph
import com.augmentalis.learnapp.navigation.ScreenNode
import java.util.UUID

/**
 * LearnApp Repository
 *
 * Repository providing clean API for LearnApp database operations.
 * Abstracts Room DAO and provides high-level operations.
 *
 * @property dao LearnApp DAO
 *
 * @since 1.0.0
 */
class LearnAppRepository(
    private val dao: LearnAppDao
) {

    // ========== Learned Apps ==========

    suspend fun saveLearnedApp(
        packageName: String,
        appName: String,
        versionCode: Long,
        versionName: String,
        stats: ExplorationStats
    ) {
        val entity = LearnedAppEntity(
            packageName = packageName,
            appName = appName,
            versionCode = versionCode,
            versionName = versionName,
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = stats.totalScreens,
            totalElements = stats.totalElements,
            appHash = calculateAppHash(packageName),  // TODO: proper hash
            explorationStatus = ExplorationStatus.COMPLETE
        )

        dao.insertLearnedApp(entity)
    }

    suspend fun getLearnedApp(packageName: String): LearnedAppEntity? {
        return dao.getLearnedApp(packageName)
    }

    suspend fun isAppLearned(packageName: String): Boolean {
        return dao.getLearnedApp(packageName) != null
    }

    suspend fun getAllLearnedApps(): List<LearnedAppEntity> {
        return dao.getAllLearnedApps()
    }

    suspend fun updateAppHash(packageName: String, newHash: String) {
        dao.updateAppHash(packageName, newHash, System.currentTimeMillis())
    }

    suspend fun deleteLearnedApp(packageName: String) {
        val app = dao.getLearnedApp(packageName)
        app?.let { dao.deleteLearnedApp(it) }
    }

    // ========== Exploration Sessions ==========

    suspend fun createExplorationSession(packageName: String): String {
        val sessionId = UUID.randomUUID().toString()

        val session = ExplorationSessionEntity(
            sessionId = sessionId,
            packageName = packageName,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            durationMs = null,
            screensExplored = 0,
            elementsDiscovered = 0,
            status = SessionStatus.RUNNING
        )

        dao.insertExplorationSession(session)
        return sessionId
    }

    suspend fun completeExplorationSession(
        sessionId: String,
        stats: ExplorationStats
    ) {
        dao.updateSessionStatus(
            sessionId = sessionId,
            status = SessionStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            durationMs = stats.durationMs
        )
    }

    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity? {
        return dao.getExplorationSession(sessionId)
    }

    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity> {
        return dao.getSessionsForPackage(packageName)
    }

    // ========== Navigation Graph ==========

    suspend fun saveNavigationGraph(
        graph: NavigationGraph,
        sessionId: String
    ) {
        // Save screen states
        graph.nodes.values.forEach { screenNode ->
            val screenStateEntity = ScreenStateEntity(
                screenHash = screenNode.screenHash,
                packageName = graph.packageName,
                activityName = screenNode.activityName,
                fingerprint = screenNode.screenHash,
                elementCount = screenNode.elements.size,
                discoveredAt = screenNode.timestamp
            )

            dao.insertScreenState(screenStateEntity)
        }

        // Save navigation edges
        val edgeEntities = graph.edges.map { edge ->
            NavigationEdgeEntity(
                edgeId = UUID.randomUUID().toString(),
                packageName = graph.packageName,
                sessionId = sessionId,
                fromScreenHash = edge.fromScreenHash,
                clickedElementUuid = edge.clickedElementUuid,
                toScreenHash = edge.toScreenHash,
                timestamp = edge.timestamp
            )
        }

        dao.insertNavigationEdges(edgeEntities)
    }

    suspend fun getNavigationGraph(packageName: String): NavigationGraph? {
        val edges = dao.getNavigationGraph(packageName)
        if (edges.isEmpty()) return null

        val screenStates = dao.getScreenStatesForPackage(packageName)

        val nodes = screenStates.associate { screenState ->
            screenState.screenHash to ScreenNode(
                screenHash = screenState.screenHash,
                activityName = screenState.activityName,
                elements = emptyList(),  // TODO: fetch elements
                timestamp = screenState.discoveredAt
            )
        }

        val navigationEdges = edges.map { edge ->
            com.augmentalis.learnapp.models.NavigationEdge(
                fromScreenHash = edge.fromScreenHash,
                clickedElementUuid = edge.clickedElementUuid,
                toScreenHash = edge.toScreenHash,
                timestamp = edge.timestamp
            )
        }

        return NavigationGraph(
            packageName = packageName,
            nodes = nodes,
            edges = navigationEdges
        )
    }

    suspend fun deleteNavigationGraph(packageName: String) {
        dao.deleteNavigationGraph(packageName)
    }

    // ========== Screen States ==========

    suspend fun saveScreenState(screenState: ScreenState) {
        val entity = ScreenStateEntity(
            screenHash = screenState.hash,
            packageName = screenState.packageName,
            activityName = screenState.activityName,
            fingerprint = screenState.hash,
            elementCount = screenState.elementCount,
            discoveredAt = screenState.timestamp
        )

        dao.insertScreenState(entity)
    }

    suspend fun getScreenState(hash: String): ScreenStateEntity? {
        return dao.getScreenState(hash)
    }

    // ========== Statistics ==========

    suspend fun getAppStatistics(packageName: String): AppStatistics {
        val totalScreens = dao.getTotalScreensForPackage(packageName)
        val totalEdges = dao.getTotalEdgesForPackage(packageName)
        val sessions = dao.getSessionsForPackage(packageName)

        return AppStatistics(
            packageName = packageName,
            totalScreens = totalScreens,
            totalEdges = totalEdges,
            totalSessions = sessions.size,
            lastExplored = sessions.firstOrNull()?.startedAt
        )
    }

    // ========== Utility ==========

    private fun calculateAppHash(packageName: String): String {
        // TODO: Implement proper app hash calculation
        return packageName.hashCode().toString()
    }
}

/**
 * App Statistics
 *
 * Statistics about learned app.
 */
data class AppStatistics(
    val packageName: String,
    val totalScreens: Int,
    val totalEdges: Int,
    val totalSessions: Int,
    val lastExplored: Long?
)
