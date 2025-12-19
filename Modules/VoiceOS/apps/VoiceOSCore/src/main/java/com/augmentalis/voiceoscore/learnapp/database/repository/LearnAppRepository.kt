/**
 * LearnAppRepository.kt - Repository pattern for LearnApp data access
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Repository providing clean API for LearnApp database operations.
 * Uses SQLDelight via LearnAppDao adapter pattern.
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import android.content.Context
import com.augmentalis.voiceoscore.learnapp.database.dao.LearnAppDao
import com.augmentalis.voiceoscore.learnapp.database.repository.MetadataSource
import com.augmentalis.voiceoscore.learnapp.database.entities.LearnedAppEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.ExplorationSessionEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.NavigationEdgeEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.ScreenStateEntity
import com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * LearnApp Repository
 *
 * Repository providing clean API for LearnApp database operations.
 * Uses SQLDelight adapter for database access.
 *
 * @property dao LearnApp DAO (SQLDelight adapter)
 * @property context Application context for metadata operations
 */
class LearnAppRepository(
    private val dao: LearnAppDao,
    private val context: Context? = null
) {
    private val packageMutexes = ConcurrentHashMap<String, Mutex>()

    private fun getMutexForPackage(packageName: String): Mutex {
        return packageMutexes.getOrPut(packageName) { Mutex() }
    }

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
            appHash = calculateAppHash(packageName),
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

    suspend fun deleteAppCompletely(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val app = dao.getLearnedApp(packageName)
                if (app == null) {
                    return@withLock RepositoryResult.Failure(
                        reason = "App with package '$packageName' not found"
                    )
                }

                dao.transaction {
                    dao.deleteNavigationGraph(packageName)
                    dao.deleteScreenStatesForPackage(packageName)
                    dao.deleteSessionsForPackage(packageName)
                    dao.deleteLearnedApp(app)
                }

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                RepositoryResult.Failure(
                    reason = "Error deleting app: ${e.message}",
                    cause = e
                )
            }
        }
    }

    // ========== Exploration Sessions ==========

    suspend fun createExplorationSessionSafe(packageName: String): SessionCreationResult {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val existingApp = dao.getLearnedApp(packageName)
                var appWasCreated = false
                val sessionId = UUID.randomUUID().toString()

                dao.transaction {
                    if (existingApp == null) {
                        val newApp = LearnedAppEntity(
                            packageName = packageName,
                            appName = packageName,
                            versionCode = 0,
                            versionName = "unknown",
                            firstLearnedAt = System.currentTimeMillis(),
                            lastUpdatedAt = System.currentTimeMillis(),
                            totalScreens = 0,
                            totalElements = 0,
                            appHash = calculateAppHash(packageName),
                            explorationStatus = ExplorationStatus.PARTIAL
                        )
                        dao.insertLearnedAppMinimal(newApp)
                        appWasCreated = true
                    }

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
                }

                SessionCreationResult.Created(
                    sessionId = sessionId,
                    appWasCreated = appWasCreated,
                    metadataSource = null
                )
            } catch (e: Exception) {
                SessionCreationResult.Failed(
                    reason = "Error creating session: ${e.message}",
                    cause = e
                )
            }
        }
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

    suspend fun saveNavigationEdge(
        packageName: String,
        sessionId: String,
        fromScreenHash: String,
        clickedElementUuid: String,
        toScreenHash: String
    ) {
        val edgeEntity = NavigationEdgeEntity(
            edgeId = UUID.randomUUID().toString(),
            packageName = packageName,
            sessionId = sessionId,
            fromScreenHash = fromScreenHash,
            clickedElementUuid = clickedElementUuid,
            toScreenHash = toScreenHash,
            timestamp = System.currentTimeMillis()
        )
        dao.insertNavigationEdge(edgeEntity)
    }

    suspend fun getNavigationGraph(packageName: String): List<NavigationEdgeEntity> {
        return dao.getNavigationGraph(packageName)
    }

    suspend fun deleteNavigationGraph(packageName: String) {
        dao.deleteNavigationGraph(packageName)
    }

    // ========== Screen States ==========

    suspend fun saveScreenState(screenState: ScreenState) {
        val mutex = getMutexForPackage(screenState.packageName)
        mutex.withLock {
            dao.transaction {
                val existingApp = dao.getLearnedApp(screenState.packageName)
                if (existingApp == null) {
                    val newApp = LearnedAppEntity(
                        packageName = screenState.packageName,
                        appName = screenState.packageName,
                        versionCode = 0,
                        versionName = "unknown",
                        firstLearnedAt = System.currentTimeMillis(),
                        lastUpdatedAt = System.currentTimeMillis(),
                        totalScreens = 0,
                        totalElements = 0,
                        appHash = calculateAppHash(screenState.packageName),
                        explorationStatus = ExplorationStatus.PARTIAL
                    )
                    dao.insertLearnedAppMinimal(newApp)
                }

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
        }
    }

    suspend fun getScreenState(hash: String): ScreenStateEntity? {
        return dao.getScreenState(hash)
    }

    suspend fun getScreenStatesForPackage(packageName: String): List<ScreenStateEntity> {
        return dao.getScreenStatesForPackage(packageName)
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

    // ========== App Learning Status ==========

    /**
     * Reset app for relearning
     *
     * Clears exploration data (sessions, screen states, navigation edges)
     * and sets exploration status to PARTIAL, allowing re-exploration.
     *
     * @param packageName Package name of the app
     * @return RepositoryResult indicating success or failure
     */
    suspend fun resetAppForRelearning(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val app = dao.getLearnedApp(packageName)
                if (app == null) {
                    return@withLock RepositoryResult.Failure(
                        reason = "App with package '$packageName' not found"
                    )
                }

                dao.transaction {
                    // Delete exploration data but keep the app entry
                    dao.deleteNavigationGraph(packageName)
                    dao.deleteScreenStatesForPackage(packageName)
                    dao.deleteSessionsForPackage(packageName)

                    // Reset app status to PARTIAL
                    dao.updateLearnedApp(app.copy(
                        explorationStatus = ExplorationStatus.PARTIAL,
                        totalScreens = 0,
                        totalElements = 0,
                        lastUpdatedAt = System.currentTimeMillis()
                    ))
                }

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                RepositoryResult.Failure(
                    reason = "Error resetting app: ${e.message}",
                    cause = e
                )
            }
        }
    }

    /**
     * Mark app as fully learned
     *
     * @param packageName Package name of the app
     * @param timestamp When the app was fully learned
     */
    suspend fun markAppAsFullyLearned(packageName: String, timestamp: Long) {
        val mutex = getMutexForPackage(packageName)
        mutex.withLock {
            val existing = dao.getLearnedApp(packageName)
            if (existing != null) {
                dao.updateLearnedApp(existing.copy(
                    explorationStatus = ExplorationStatus.COMPLETE,
                    lastUpdatedAt = timestamp
                ))
            }
        }
    }

    /**
     * Save pause state for session
     *
     * @param sessionId Session ID
     * @param pauseState Pause state string
     */
    suspend fun savePauseState(sessionId: String, pauseState: String) {
        val session = dao.getExplorationSession(sessionId)
        if (session != null) {
            dao.updateSessionStatus(
                sessionId = sessionId,
                status = if (pauseState == "PAUSED") SessionStatus.PAUSED else session.status,
                completedAt = session.completedAt ?: 0L,
                durationMs = session.durationMs ?: 0L
            )
        }
    }

    /**
     * Save exploration metrics
     *
     * @param packageName Package name
     * @param stats Exploration statistics
     */
    suspend fun saveMetrics(packageName: String, stats: ExplorationStats) {
        val mutex = getMutexForPackage(packageName)
        mutex.withLock {
            val existing = dao.getLearnedApp(packageName)
            if (existing != null) {
                dao.updateLearnedApp(existing.copy(
                    totalScreens = stats.totalScreens,
                    totalElements = stats.totalElements,
                    lastUpdatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    // ========== Utility ==========

    private fun calculateAppHash(packageName: String): String {
        return packageName.hashCode().toString()
    }
}

// ========== Result Types ==========

/**
 * Generic repository result type
 */
sealed class RepositoryResult<T> {
    data class Success<T>(val value: T) : RepositoryResult<T>()
    data class Failure<T>(
        val reason: String,
        val cause: Throwable? = null
    ) : RepositoryResult<T>()
}

/**
 * Result of session creation operation
 */
sealed class SessionCreationResult {
    data class Created(
        val sessionId: String,
        val appWasCreated: Boolean,
        val metadataSource: MetadataSource?
    ) : SessionCreationResult()

    data class Failed(
        val reason: String,
        val cause: Throwable? = null
    ) : SessionCreationResult()
}

// Note: MetadataSource is defined in AppMetadataProvider.kt

/**
 * App statistics summary
 */
data class AppStatistics(
    val packageName: String,
    val totalScreens: Int,
    val totalEdges: Int,
    val totalSessions: Int,
    val lastExplored: Long?
)

/**
 * Exploration status constants
 */
object ExplorationStatus {
    const val PARTIAL = "PARTIAL"
    const val COMPLETE = "COMPLETE"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val FAILED = "FAILED"
}

/**
 * Session status constants
 */
object SessionStatus {
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val PAUSED = "PAUSED"
    const val CANCELLED = "CANCELLED"
    const val FAILED = "FAILED"
}
