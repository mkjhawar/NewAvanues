/**
 * LearnAppRepository.kt - Repository pattern for LearnApp data access
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/database/repository/LearnAppRepository.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-12-02 - Removed DAO layer, use SQLDelight directly, fixed transaction deadlock
 *
 * Repository providing clean API for LearnApp database operations using SQLDelight directly.
 *
 * CRITICAL FIX (2025-12-02): Removed all DAO abstractions and transaction wrappers
 * that caused 180-second SQLITE_BUSY deadlocks. Now uses VoiceOSDatabaseManager directly.
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.database.entities.*
import com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import com.augmentalis.voiceoscore.learnapp.navigation.NavigationGraph
import com.augmentalis.voiceoscore.learnapp.navigation.ScreenNode
import com.augmentalis.uuidcreator.core.UUIDGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * LearnApp Repository
 *
 * Repository providing clean API for LearnApp database operations.
 * Uses SQLDelight database manager directly (no DAO layer).
 *
 * Features:
 * - 4 patterns for session creation (auto-create, strict, explicit, upsert)
 * - Per-package Mutex for race condition prevention
 * - Dual-database metadata lookup (AppScrapingDatabase + PackageManager)
 * - VOS4 UUIDGenerator integration
 * - Type-safe Result types
 * - NO transaction wrappers (avoids deadlock)
 *
 * @property databaseManager VoiceOS database manager (SQLDelight)
 * @property context Application context for metadata provider
 * @property scrapedAppMetadataSource Optional interface for AppScrapingDatabase metadata lookup
 *
 * @since 1.0.0
 */
class LearnAppRepository(
    private val databaseManager: VoiceOSDatabaseManager,
    private val context: Context? = null,
    private val scrapedAppMetadataSource: ScrapedAppMetadataSource? = null
) {

    /**
     * Per-package Mutex map for fine-grained locking
     *
     * Prevents race conditions during session creation while allowing
     * concurrent operations on different packages.
     */
    private val packageMutexes = ConcurrentHashMap<String, Mutex>()

    /**
     * Metadata provider for app metadata lookup
     *
     * Lazy initialized only if context provided.
     */
    private val metadataProvider: AppMetadataProvider? by lazy {
        context?.let { AppMetadataProvider(it, scrapedAppMetadataSource) }
    }

    /**
     * Get or create Mutex for specific package
     *
     * @param packageName Package name to get Mutex for
     * @return Mutex for this package
     */
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
    ) = withContext(Dispatchers.IO) {
        // FIX (2025-12-06): Determine status based on actual completeness
        // Previously hardcoded status="LEARNED" and progress=100 for all apps,
        // causing partially learned apps (<95%) to be incorrectly marked as fully learned.
        // This prevented consent dialog from showing on app reopen.
        val completenessThreshold = 95f  // TODO: Get from DeveloperSettings
        val isFullyLearned = stats.completeness >= completenessThreshold

        databaseManager.learnedAppQueries.insertLearnedApp(
            package_name = packageName,
            app_name = appName,
            version_code = versionCode,
            version_name = versionName,
            first_learned_at = System.currentTimeMillis(),
            last_updated_at = System.currentTimeMillis(),
            total_screens = stats.totalScreens.toLong(),
            total_elements = stats.totalElements.toLong(),
            app_hash = calculateAppHashWithVersion(packageName, versionCode, versionName),
            exploration_status = if (isFullyLearned) ExplorationStatus.COMPLETE else ExplorationStatus.PARTIAL,
            learning_mode = "AUTO_DETECT",
            status = if (isFullyLearned) "LEARNED" else "NOT_LEARNED",  // FIX: Conditional on completeness
            progress = stats.completeness.toLong(),  // FIX: Actual progress (0-100), not hardcoded 100
            command_count = stats.totalElements.toLong(),
            screens_explored = stats.totalScreens.toLong(),
            is_auto_detect_enabled = 1
        )
    }

    suspend fun getLearnedApp(packageName: String): LearnedAppEntity? = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.getLearnedApp(packageName).executeAsOneOrNull()?.let {
            LearnedAppEntity(
                packageName = it.package_name,
                appName = it.app_name,
                versionCode = it.version_code,
                versionName = it.version_name,
                firstLearnedAt = it.first_learned_at,
                lastUpdatedAt = it.last_updated_at,
                totalScreens = it.total_screens.toInt(),
                totalElements = it.total_elements.toInt(),
                appHash = it.app_hash,
                explorationStatus = it.exploration_status
            )
        }
    }

    suspend fun isAppLearned(packageName: String): Boolean {
        return getLearnedApp(packageName) != null
    }

    suspend fun getAllLearnedApps(): List<LearnedAppEntity> = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.getAllLearnedApps().executeAsList().map {
            LearnedAppEntity(
                packageName = it.package_name,
                appName = it.app_name,
                versionCode = it.version_code,
                versionName = it.version_name,
                firstLearnedAt = it.first_learned_at,
                lastUpdatedAt = it.last_updated_at,
                totalScreens = it.total_screens.toInt(),
                totalElements = it.total_elements.toInt(),
                appHash = it.app_hash,
                explorationStatus = it.exploration_status
            )
        }
    }

    suspend fun updateAppHash(packageName: String, newHash: String) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.updateAppHash(
            package_name = packageName,
            app_hash = newHash,
            last_updated_at = System.currentTimeMillis()
        )
    }

    suspend fun deleteLearnedApp(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.learnedAppQueries.deleteLearnedApp(packageName)
    }

    private suspend fun deleteLearnedApp(app: LearnedAppEntity) {
        deleteLearnedApp(app.packageName)
    }

    /**
     * Mark app as fully learned
     *
     * Updates the app's status to LEARNED, sets exploration_status to COMPLETE,
     * and updates progress to 100%.
     *
     * @param packageName Package name of the app
     * @param timestamp Timestamp when app was fully learned
     */
    suspend fun markAppAsFullyLearned(packageName: String, timestamp: Long) = withContext(Dispatchers.IO) {
        // Update status to LEARNED
        databaseManager.learnedAppQueries.updateStatus(
            package_name = packageName,
            status = "LEARNED",
            last_updated_at = timestamp
        )

        // Update exploration_status to COMPLETE (for legacy compatibility)
        // Note: Using transaction-less atomic updates to avoid deadlock
        val currentApp = getLearnedApp(packageName)
        if (currentApp != null) {
            val updatedApp = currentApp.copy(
                explorationStatus = ExplorationStatus.COMPLETE,
                lastUpdatedAt = timestamp
            )
            insertLearnedApp(updatedApp)  // REPLACE strategy
        }

        // Update progress to 100%
        val stats = getAppStatistics(packageName)
        databaseManager.learnedAppQueries.updateProgress(
            package_name = packageName,
            progress = 100,
            screens_explored = stats.totalScreens.toLong(),
            last_updated_at = timestamp
        )

        android.util.Log.i("LearnAppRepository", "✅ Marked app as fully learned: $packageName")
    }

    /**
     * Delete ALL exploration data for an app (complete reset)
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     * Each delete is atomic in SQLDelight. Mutex provides thread safety.
     *
     * @param packageName Package name to delete
     * @return RepositoryResult.Success if deleted, Failure if app doesn't exist or error occurs
     */
    suspend fun deleteAppCompletely(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val app = getLearnedApp(packageName)
                if (app == null) {
                    return@withLock RepositoryResult.Failure(
                        reason = "App with package '$packageName' not found in database"
                    )
                }

                android.util.Log.d("LearnAppRepository", "Deleting app completely: $packageName")

                // Delete associated data first (each delete is atomic)
                deleteNavigationGraph(packageName)
                android.util.Log.d("LearnAppRepository", "Deleted navigation edges for $packageName")

                deleteScreenStatesForPackage(packageName)
                android.util.Log.d("LearnAppRepository", "Deleted screen states for $packageName")

                deleteSessionsForPackage(packageName)
                android.util.Log.d("LearnAppRepository", "Deleted exploration sessions for $packageName")

                // Finally delete the app itself
                deleteLearnedApp(app)
                android.util.Log.d("LearnAppRepository", "Deleted learned app: $packageName")

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                android.util.Log.e("LearnAppRepository", "Error deleting app completely: $packageName", e)
                RepositoryResult.Failure(
                    reason = "Error deleting app '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    /**
     * Reset app exploration status (mark for relearning)
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     */
    suspend fun resetAppForRelearning(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val app = getLearnedApp(packageName)
                if (app == null) {
                    return@withLock RepositoryResult.Failure(
                        reason = "App with package '$packageName' not found in database"
                    )
                }

                android.util.Log.d("LearnAppRepository", "Resetting app for relearning: $packageName")

                // Delete exploration data
                deleteNavigationGraph(packageName)
                deleteScreenStatesForPackage(packageName)
                deleteSessionsForPackage(packageName)

                // Update app status to PARTIAL (ready for relearning)
                val updatedApp = app.copy(
                    explorationStatus = ExplorationStatus.PARTIAL,
                    lastUpdatedAt = System.currentTimeMillis(),
                    totalScreens = 0,
                    totalElements = 0
                )

                insertLearnedApp(updatedApp)  // REPLACE strategy

                android.util.Log.d("LearnAppRepository", "App reset for relearning: $packageName")

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                android.util.Log.e("LearnAppRepository", "Error resetting app for relearning: $packageName", e)
                RepositoryResult.Failure(
                    reason = "Error resetting app '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    /**
     * Clear exploration data but keep app entry
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     */
    suspend fun clearExplorationData(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                android.util.Log.d("LearnAppRepository", "Clearing exploration data for: $packageName")

                deleteNavigationGraph(packageName)
                deleteScreenStatesForPackage(packageName)
                deleteSessionsForPackage(packageName)

                android.util.Log.d("LearnAppRepository", "Exploration data cleared: $packageName")

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                android.util.Log.e("LearnAppRepository", "Error clearing exploration data: $packageName", e)
                RepositoryResult.Failure(
                    reason = "Error clearing data for '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    // ========== Exploration Sessions ==========

    suspend fun insertExplorationSession(session: ExplorationSessionEntity) = withContext(Dispatchers.IO) {
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

    suspend fun insertLearnedApp(app: LearnedAppEntity) = withContext(Dispatchers.IO) {
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
            learning_mode = "AUTO_DETECT",
            status = "NOT_LEARNED",
            progress = 0,
            command_count = 0,
            screens_explored = 0,
            is_auto_detect_enabled = 1
        )
    }

    suspend fun insertLearnedAppMinimal(app: LearnedAppEntity) = insertLearnedApp(app)

    /**
     * PATTERN 1: Create Exploration Session (Safe Auto-Create) - RECOMMENDED
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     * Mutex provides thread safety for atomic operations.
     */
    suspend fun createExplorationSessionSafe(packageName: String): SessionCreationResult {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                // Check if app already learned
                val existingApp = getLearnedApp(packageName)
                val appWasCreated: Boolean
                val metadataSource: MetadataSource?

                if (existingApp == null) {
                    // Get metadata from AppScrapingDatabase or PackageManager
                    val metadata = metadataProvider?.getMetadata(packageName)
                        ?: return@withLock SessionCreationResult.Failed(
                            reason = "Cannot get metadata for package '$packageName': " +
                                    "Package not found and no metadata provider configured",
                            cause = null
                        )

                    if (!metadata.isInstalled) {
                        return@withLock SessionCreationResult.Failed(
                            reason = "Cannot create session for package '$packageName': Package not installed",
                            cause = null
                        )
                    }

                    // Create LearnedAppEntity with minimal metadata
                    val newApp = LearnedAppEntity(
                        packageName = metadata.packageName,
                        appName = metadata.appName,
                        versionCode = metadata.versionCode,
                        versionName = metadata.versionName,
                        firstLearnedAt = System.currentTimeMillis(),
                        lastUpdatedAt = System.currentTimeMillis(),
                        totalScreens = 0,
                        totalElements = 0,
                        appHash = metadata.appHash,
                        explorationStatus = ExplorationStatus.PARTIAL
                    )

                    insertLearnedAppMinimal(newApp)
                    appWasCreated = true
                    metadataSource = metadata.source
                } else {
                    appWasCreated = false
                    metadataSource = null
                }

                // Generate session ID using VOS4 UUIDGenerator
                val sessionId = UUIDGenerator.generate()

                // Create exploration session
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

                insertExplorationSession(session)

                SessionCreationResult.Created(
                    sessionId = sessionId,
                    appWasCreated = appWasCreated,
                    metadataSource = metadataSource
                )
            } catch (e: Exception) {
                SessionCreationResult.Failed(
                    reason = "Unexpected error creating session for '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    /**
     * PATTERN 2: Create Exploration Session (Strict Mode)
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     */
    suspend fun createExplorationSessionStrict(packageName: String): String {
        // Verify parent app exists
        getLearnedApp(packageName)
            ?: throw LearnedAppNotFoundException(packageName)

        // Generate session ID using VOS4 UUIDGenerator
        val sessionId = UUIDGenerator.generate()

        // Create exploration session
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

        insertExplorationSession(session)
        return sessionId
    }

    /**
     * PATTERN 3: Ensure Learned App Exists (Explicit Check + Create)
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     */
    suspend fun ensureLearnedAppExists(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                // Check if already exists
                val existingApp = getLearnedApp(packageName)
                if (existingApp != null) {
                    return@withLock RepositoryResult.Success(false)
                }

                // Get metadata
                val metadata = metadataProvider?.getMetadata(packageName)
                    ?: return@withLock RepositoryResult.Failure(
                        reason = "Cannot get metadata for package '$packageName': " +
                                "Package not found and no metadata provider configured"
                    )

                if (!metadata.isInstalled) {
                    return@withLock RepositoryResult.Failure(
                        reason = "Package '$packageName' is not installed"
                    )
                }

                // Create LearnedAppEntity
                val newApp = LearnedAppEntity(
                    packageName = metadata.packageName,
                    appName = metadata.appName,
                    versionCode = metadata.versionCode,
                    versionName = metadata.versionName,
                    firstLearnedAt = System.currentTimeMillis(),
                    lastUpdatedAt = System.currentTimeMillis(),
                    totalScreens = 0,
                    totalElements = 0,
                    appHash = metadata.appHash,
                    explorationStatus = ExplorationStatus.PARTIAL
                )

                insertLearnedAppMinimal(newApp)
                RepositoryResult.Success(true)
            } catch (e: Exception) {
                RepositoryResult.Failure(
                    reason = "Error ensuring app exists for '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    /**
     * PATTERN 4: Create Exploration Session (Upsert Pattern)
     *
     * FIX (2025-12-02): Removed transaction wrapper that caused deadlock.
     */
    suspend fun createExplorationSessionUpsert(packageName: String): SessionCreationResult {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                // Get metadata
                val metadata = metadataProvider?.getMetadata(packageName)
                    ?: return@withLock SessionCreationResult.Failed(
                        reason = "Cannot get metadata for package '$packageName': " +
                                "Package not found and no metadata provider configured"
                    )

                if (!metadata.isInstalled) {
                    return@withLock SessionCreationResult.Failed(
                        reason = "Package '$packageName' is not installed"
                    )
                }

                // Check if app exists
                val existingApp = getLearnedApp(packageName)
                val appWasCreated = existingApp == null

                // Upsert LearnedAppEntity (REPLACE strategy)
                val app = LearnedAppEntity(
                    packageName = metadata.packageName,
                    appName = metadata.appName,
                    versionCode = metadata.versionCode,
                    versionName = metadata.versionName,
                    firstLearnedAt = existingApp?.firstLearnedAt ?: System.currentTimeMillis(),
                    lastUpdatedAt = System.currentTimeMillis(),
                    totalScreens = existingApp?.totalScreens ?: 0,
                    totalElements = existingApp?.totalElements ?: 0,
                    appHash = metadata.appHash,
                    explorationStatus = existingApp?.explorationStatus ?: ExplorationStatus.PARTIAL
                )

                insertLearnedApp(app) // Uses REPLACE strategy

                // Generate session ID using VOS4 UUIDGenerator
                val sessionId = UUIDGenerator.generate()

                // Create exploration session
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

                insertExplorationSession(session)

                SessionCreationResult.Created(
                    sessionId = sessionId,
                    appWasCreated = appWasCreated,
                    metadataSource = metadata.source
                )
            } catch (e: Exception) {
                SessionCreationResult.Failed(
                    reason = "Error creating session (upsert) for '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    @Deprecated("Use createExplorationSessionSafe()")
    suspend fun createExplorationSession(packageName: String): String {
        return createExplorationSessionStrict(packageName)
    }

    suspend fun completeExplorationSession(
        sessionId: String,
        stats: ExplorationStats
    ) = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.updateSessionStatus(
            session_id = sessionId,
            status = SessionStatus.COMPLETED,
            completed_at = System.currentTimeMillis(),
            duration_ms = stats.durationMs
        )
    }

    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity? = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.getExplorationSession(sessionId).executeAsOneOrNull()?.let {
            ExplorationSessionEntity(
                sessionId = it.session_id,
                packageName = it.package_name,
                startedAt = it.started_at,
                completedAt = it.completed_at,
                durationMs = it.duration_ms,
                screensExplored = it.screens_explored.toInt(),
                elementsDiscovered = it.elements_discovered.toInt(),
                status = it.status
            )
        }
    }

    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity> = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.getSessionsForPackage(packageName).executeAsList().map {
            ExplorationSessionEntity(
                sessionId = it.session_id,
                packageName = it.package_name,
                startedAt = it.started_at,
                completedAt = it.completed_at,
                durationMs = it.duration_ms,
                screensExplored = it.screens_explored.toInt(),
                elementsDiscovered = it.elements_discovered.toInt(),
                status = it.status
            )
        }
    }

    private suspend fun deleteSessionsForPackage(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.explorationSessionQueries.deleteSessionsForPackage(packageName)
    }

    // ========== Navigation Graph ==========

    suspend fun saveNavigationEdge(
        packageName: String,
        sessionId: String,
        fromScreenHash: String,
        clickedElementUuid: String,
        toScreenHash: String
    ) = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.insertNavigationEdge(
            edge_id = UUIDGenerator.generate(),
            package_name = packageName,
            session_id = sessionId,
            from_screen_hash = fromScreenHash,
            clicked_element_uuid = clickedElementUuid,
            to_screen_hash = toScreenHash,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun saveNavigationGraph(
        graph: NavigationGraph,
        sessionId: String
    ) {
        // FOREIGN KEY VALIDATION
        getLearnedApp(graph.packageName)
            ?: throw IllegalStateException(
                "Cannot save navigation graph: LearnedAppEntity with packageName='${graph.packageName}' not found"
            )

        getExplorationSession(sessionId)
            ?: throw IllegalStateException(
                "Cannot save navigation graph: ExplorationSessionEntity with sessionId='$sessionId' not found"
            )

        // Save screen states
        graph.nodes.values.forEach { screenNode ->
            insertScreenState(ScreenStateEntity(
                screenHash = screenNode.screenHash,
                packageName = graph.packageName,
                activityName = screenNode.activityName,
                fingerprint = screenNode.screenHash,
                elementCount = screenNode.elements.size,
                discoveredAt = screenNode.timestamp
            ))
        }

        // Save navigation edges
        graph.edges.forEach { edge ->
            saveNavigationEdge(
                packageName = graph.packageName,
                sessionId = sessionId,
                fromScreenHash = edge.fromScreenHash,
                clickedElementUuid = edge.clickedElementUuid,
                toScreenHash = edge.toScreenHash
            )
        }
    }

    suspend fun getNavigationGraph(packageName: String): NavigationGraph? = withContext(Dispatchers.IO) {
        val edges = databaseManager.navigationEdgeQueries.getNavigationGraph(packageName).executeAsList()
        if (edges.isEmpty()) return@withContext null

        val screenStates = databaseManager.screenStateQueries.getScreenStatesForPackage(packageName).executeAsList()

        val nodes = screenStates.associate { screenState ->
            screenState.screen_hash to ScreenNode(
                screenHash = screenState.screen_hash,
                activityName = screenState.activity_name ?: "",
                elements = emptyList(),
                timestamp = screenState.discovered_at
            )
        }

        val navigationEdges = edges.map { edge ->
            com.augmentalis.voiceoscore.learnapp.models.NavigationEdge(
                fromScreenHash = edge.from_screen_hash,
                clickedElementUuid = edge.clicked_element_uuid,
                toScreenHash = edge.to_screen_hash,
                timestamp = edge.timestamp
            )
        }

        NavigationGraph(
            packageName = packageName,
            nodes = nodes,
            edges = navigationEdges
        )
    }

    private suspend fun deleteNavigationGraph(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.navigationEdgeQueries.deleteNavigationGraph(packageName)
    }

    // ========== Screen States ==========

    private suspend fun insertScreenState(state: ScreenStateEntity) = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.insertScreenState(
            screen_hash = state.screenHash,
            package_name = state.packageName,
            activity_name = state.activityName ?: "",
            fingerprint = state.fingerprint,
            element_count = state.elementCount.toLong(),
            discovered_at = state.discoveredAt
        )
    }

    suspend fun saveScreenState(screenState: ScreenState) {
        try {
            val mutex = getMutexForPackage(screenState.packageName)
            mutex.withLock {
                // FIX (2025-12-02): No transaction wrapper to avoid deadlock
                val existingApp = getLearnedApp(screenState.packageName)

                if (existingApp == null) {
                    val metadata = metadataProvider?.getMetadata(screenState.packageName)
                    val newApp = LearnedAppEntity(
                        packageName = screenState.packageName,
                        appName = metadata?.appName ?: screenState.packageName,
                        versionCode = metadata?.versionCode ?: 0,
                        versionName = metadata?.versionName ?: "unknown",
                        firstLearnedAt = System.currentTimeMillis(),
                        lastUpdatedAt = System.currentTimeMillis(),
                        totalScreens = 0,
                        totalElements = 0,
                        appHash = metadata?.appHash ?: calculateAppHash(screenState.packageName),
                        explorationStatus = ExplorationStatus.PARTIAL
                    )
                    insertLearnedAppMinimal(newApp)
                }

                val entity = ScreenStateEntity(
                    screenHash = screenState.hash,
                    packageName = screenState.packageName,
                    activityName = screenState.activityName,
                    fingerprint = screenState.hash,
                    elementCount = screenState.elementCount,
                    discoveredAt = screenState.timestamp
                )

                insertScreenState(entity)
            }
        } catch (e: Exception) {
            android.util.Log.e("LearnAppRepository", "saveScreenState failed for ${screenState.packageName}", e)
            throw e
        }
    }

    suspend fun getScreenState(hash: String): ScreenStateEntity? = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.getScreenState(hash).executeAsOneOrNull()?.let {
            ScreenStateEntity(
                screenHash = it.screen_hash,
                packageName = it.package_name,
                activityName = it.activity_name,
                fingerprint = it.fingerprint,
                elementCount = it.element_count.toInt(),
                discoveredAt = it.discovered_at
            )
        }
    }

    private suspend fun deleteScreenStatesForPackage(packageName: String) = withContext(Dispatchers.IO) {
        databaseManager.screenStateQueries.deleteScreenStatesForPackage(packageName)
    }

    // ========== Statistics ==========

    suspend fun getAppStatistics(packageName: String): AppStatistics = withContext(Dispatchers.IO) {
        val totalScreens = databaseManager.screenStateQueries
            .getTotalScreensForPackage(packageName)
            .executeAsOne()
            .toInt()

        val totalEdges = databaseManager.navigationEdgeQueries
            .getTotalEdgesForPackage(packageName)
            .executeAsOne()
            .toInt()

        val sessions = getSessionsForPackage(packageName)

        AppStatistics(
            packageName = packageName,
            totalScreens = totalScreens,
            totalEdges = totalEdges,
            totalSessions = sessions.size,
            lastExplored = sessions.firstOrNull()?.startedAt
        )
    }

    // ========== Utility ==========

    private fun calculateAppHash(packageName: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(packageName.toByteArray())
        return hash.take(16).joinToString("") { "%02x".format(it) }
    }

    fun calculateAppHashWithVersion(
        packageName: String,
        versionCode: Long,
        versionName: String
    ): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val input = "$packageName:$versionCode:$versionName"
        val hash = digest.digest(input.toByteArray())
        return hash.take(16).joinToString("") { "%02x".format(it) }
    }

    // ========== Phase 2: Pause/Resume State Persistence ==========

    /**
     * Save pause state to database (Phase 2: Pause/Resume)
     *
     * Persists the exploration pause state so it can be restored after app restart.
     *
     * @param packageName Package name of the app
     * @param state Pause state to save
     */
    suspend fun savePauseState(
        packageName: String,
        state: com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState
    ) = withContext(Dispatchers.IO) {
        // Map enum to string for database storage
        val stateString = when (state) {
            com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState.RUNNING -> "RUNNING"
            com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState.PAUSED_BY_USER -> "PAUSED_USER"
            com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState.PAUSED_AUTO -> "PAUSED_AUTO"
        }

        databaseManager.learnedAppQueries.updatePauseState(
            package_name = packageName,
            pause_state = stateString,
            last_updated_at = System.currentTimeMillis()
        )

        android.util.Log.d("LearnAppRepository", "Saved pause state for $packageName: $stateString")
    }

    /**
     * Load pause state from database (Phase 2: Pause/Resume)
     *
     * Retrieves the persisted pause state for an app.
     *
     * @param packageName Package name of the app
     * @return Pause state if found, null if app not in database
     */
    suspend fun loadPauseState(
        packageName: String
    ): com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState? = withContext(Dispatchers.IO) {
        val stateString = databaseManager.learnedAppQueries.getPauseState(packageName).executeAsOneOrNull()

        if (stateString == null) {
            android.util.Log.d("LearnAppRepository", "No pause state found for $packageName")
            return@withContext null
        }

        // Map string back to enum
        val pauseState = when (stateString) {
            "PAUSED_USER" -> com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState.PAUSED_BY_USER
            "PAUSED_AUTO" -> com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState.PAUSED_AUTO
            else -> com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine.ExplorationPauseState.RUNNING
        }

        android.util.Log.d("LearnAppRepository", "Loaded pause state for $packageName: $pauseState")
        pauseState
    }
}

/**
 * App Statistics
 */
data class AppStatistics(
    val packageName: String,
    val totalScreens: Int,
    val totalEdges: Int,
    val totalSessions: Int,
    val lastExplored: Long?
)
