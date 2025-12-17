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

import android.content.Context
import androidx.room.Transaction
import com.augmentalis.learnapp.database.dao.LearnAppDao
import com.augmentalis.learnapp.database.entities.*
import com.augmentalis.learnapp.models.ExplorationStats
import com.augmentalis.learnapp.models.ScreenState
import com.augmentalis.learnapp.navigation.NavigationGraph
import com.augmentalis.learnapp.navigation.ScreenNode
import com.augmentalis.uuidcreator.core.UUIDGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * LearnApp Repository
 *
 * Repository providing clean API for LearnApp database operations.
 * Abstracts Room DAO and provides high-level operations.
 *
 * Features:
 * - 4 patterns for session creation (auto-create, strict, explicit, upsert)
 * - Per-package Mutex for race condition prevention
 * - Dual-database metadata lookup (AppScrapingDatabase + PackageManager)
 * - VOS4 UUIDGenerator integration
 * - Type-safe Result types
 * - @Transaction annotations for atomicity
 *
 * @property dao LearnApp DAO
 * @property context Application context for metadata provider
 * @property scrapedAppMetadataSource Optional interface for AppScrapingDatabase metadata lookup
 *
 * @since 1.0.0
 */
class LearnAppRepository(
    private val dao: LearnAppDao,
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

    /**
     * Delete ALL exploration data for an app (complete reset)
     *
     * Deletes the learned app AND all associated data:
     * - Exploration sessions
     * - Navigation edges
     * - Screen states
     * - (Note: Foreign key CASCADE handles children automatically, but we also do explicit deletes for clarity)
     *
     * Use this when you want to completely remove an app and start fresh.
     *
     * @param packageName Package name to delete
     * @return RepositoryResult.Success if deleted, Failure if app doesn't exist or error occurs
     *
     * @since 1.0.0
     */
    @Transaction
    suspend fun deleteAppCompletely(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val app = dao.getLearnedApp(packageName)
                if (app == null) {
                    return@withLock RepositoryResult.Failure(
                        reason = "App with package '$packageName' not found in database"
                    )
                }

                android.util.Log.d("LearnAppRepository",
                    "Deleting app completely: $packageName")

                // Delete associated data first (explicit, though CASCADE should handle this)
                dao.deleteNavigationGraph(packageName)
                android.util.Log.d("LearnAppRepository", "Deleted navigation edges for $packageName")

                dao.deleteScreenStatesForPackage(packageName)
                android.util.Log.d("LearnAppRepository", "Deleted screen states for $packageName")

                dao.deleteSessionsForPackage(packageName)
                android.util.Log.d("LearnAppRepository", "Deleted exploration sessions for $packageName")

                // Finally delete the app itself
                dao.deleteLearnedApp(app)
                android.util.Log.d("LearnAppRepository", "Deleted learned app: $packageName")

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                android.util.Log.e("LearnAppRepository",
                    "Error deleting app completely: $packageName", e)
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
     * Sets exploration status to PARTIAL and clears exploration data,
     * but keeps the app entry itself.
     *
     * Use this when you want to relearn an app but keep basic metadata.
     *
     * @param packageName Package name to reset
     * @return RepositoryResult.Success if reset, Failure if app doesn't exist or error occurs
     *
     * @since 1.0.0
     */
    @Transaction
    suspend fun resetAppForRelearning(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                val app = dao.getLearnedApp(packageName)
                if (app == null) {
                    return@withLock RepositoryResult.Failure(
                        reason = "App with package '$packageName' not found in database"
                    )
                }

                android.util.Log.d("LearnAppRepository",
                    "Resetting app for relearning: $packageName")

                // Delete exploration data
                dao.deleteNavigationGraph(packageName)
                dao.deleteScreenStatesForPackage(packageName)
                dao.deleteSessionsForPackage(packageName)

                // Update app status to PARTIAL (ready for relearning)
                val updatedApp = app.copy(
                    explorationStatus = ExplorationStatus.PARTIAL,
                    lastUpdatedAt = System.currentTimeMillis(),
                    totalScreens = 0,
                    totalElements = 0
                )

                dao.insertLearnedApp(updatedApp)  // REPLACE strategy

                android.util.Log.d("LearnAppRepository",
                    "App reset for relearning: $packageName")

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                android.util.Log.e("LearnAppRepository",
                    "Error resetting app for relearning: $packageName", e)
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
     * Similar to resetAppForRelearning but doesn't change exploration status.
     * Just clears the exploration data (sessions, edges, screen states).
     *
     * @param packageName Package name to clear data for
     * @return RepositoryResult.Success if cleared, Failure if error occurs
     *
     * @since 1.0.0
     */
    @Transaction
    suspend fun clearExplorationData(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                android.util.Log.d("LearnAppRepository",
                    "Clearing exploration data for: $packageName")

                dao.deleteNavigationGraph(packageName)
                dao.deleteScreenStatesForPackage(packageName)
                dao.deleteSessionsForPackage(packageName)

                android.util.Log.d("LearnAppRepository",
                    "Exploration data cleared: $packageName")

                RepositoryResult.Success(true)
            } catch (e: Exception) {
                android.util.Log.e("LearnAppRepository",
                    "Error clearing exploration data: $packageName", e)
                RepositoryResult.Failure(
                    reason = "Error clearing data for '$packageName': ${e.message}",
                    cause = e
                )
            }
        }
    }

    // ========== Exploration Sessions ==========

    /**
     * PATTERN 1: Create Exploration Session (Safe Auto-Create) - RECOMMENDED
     *
     * Creates exploration session with automatic parent app creation if needed.
     *
     * Flow:
     * 1. Acquire per-package Mutex (prevents race conditions)
     * 2. Check if LearnedAppEntity exists
     * 3. If not exists:
     *    a. Get metadata from AppScrapingDatabase or PackageManager
     *    b. Validate package is installed
     *    c. Create LearnedAppEntity with IGNORE strategy (idempotent)
     * 4. Generate session ID using VOS4 UUIDGenerator
     * 5. Create ExplorationSessionEntity
     * 6. Return result with session ID and creation status
     *
     * Benefits:
     * - No foreign key violations
     * - No race conditions (per-package Mutex)
     * - Atomic operation (@Transaction)
     * - Idempotent (IGNORE strategy)
     * - Leverages existing metadata from AppScrapingDatabase
     * - Type-safe result
     *
     * Use when: You want automatic handling of missing parent apps (default choice)
     *
     * @param packageName Package name to create session for
     * @return SessionCreationResult with session ID or failure reason
     *
     * @since 1.0.0
     */
    @Transaction
    suspend fun createExplorationSessionSafe(packageName: String): SessionCreationResult {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                // Check if app already learned
                val existingApp = dao.getLearnedApp(packageName)
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

                    // Use IGNORE strategy (idempotent, preserves existing)
                    dao.insertLearnedAppMinimal(newApp)
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

                dao.insertExplorationSession(session)

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
     * Creates exploration session but requires parent app to already exist.
     *
     * Flow:
     * 1. Check if LearnedAppEntity exists
     * 2. If not exists, throw LearnedAppNotFoundException
     * 3. Generate session ID using VOS4 UUIDGenerator
     * 4. Create ExplorationSessionEntity
     * 5. Return session ID
     *
     * Benefits:
     * - Enforces parent app existence
     * - Clear error if parent missing
     * - No automatic creation
     * - Simple control flow
     *
     * Use when: You want to enforce that app was explicitly created first
     *
     * @param packageName Package name to create session for
     * @return Session ID
     * @throws LearnedAppNotFoundException if parent app doesn't exist
     *
     * @since 1.0.0
     */
    @Transaction
    suspend fun createExplorationSessionStrict(packageName: String): String {
        // Verify parent app exists
        dao.getLearnedApp(packageName)
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

        dao.insertExplorationSession(session)
        return sessionId
    }

    /**
     * PATTERN 3: Ensure Learned App Exists (Explicit Check + Create)
     *
     * Explicitly ensures LearnedAppEntity exists before session creation.
     *
     * Flow:
     * 1. Acquire per-package Mutex
     * 2. Check if LearnedAppEntity exists
     * 3. If not exists, create using metadata provider
     * 4. Return success/failure result
     *
     * Benefits:
     * - Explicit two-phase operation
     * - Caller controls when to create app
     * - Allows pre-validation
     * - Type-safe result
     *
     * Use when: You want explicit control over app creation before session creation
     *
     * Usage:
     * ```
     * when (val result = repository.ensureLearnedAppExists(packageName)) {
     *     is RepositoryResult.Success -> {
     *         if (result.value) println("App created")
     *         else println("App already existed")
     *         val sessionId = repository.createExplorationSessionStrict(packageName)
     *     }
     *     is RepositoryResult.Failure -> println("Error: ${result.reason}")
     * }
     * ```
     *
     * @param packageName Package name to ensure exists
     * @return RepositoryResult<Boolean> - true if created, false if already existed
     *
     * @since 1.0.0
     */
    @Transaction
    suspend fun ensureLearnedAppExists(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                // Check if already exists
                val existingApp = dao.getLearnedApp(packageName)
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

                dao.insertLearnedAppMinimal(newApp)
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
     * Creates exploration session with guaranteed app existence via upsert.
     *
     * Flow:
     * 1. Acquire per-package Mutex
     * 2. Get metadata from provider
     * 3. Upsert LearnedAppEntity (create or update)
     * 4. Generate session ID using VOS4 UUIDGenerator
     * 5. Create ExplorationSessionEntity
     * 6. Return result
     *
     * Benefits:
     * - Always creates or updates app
     * - Refreshes app metadata on every session
     * - No foreign key violations
     * - Atomic operation
     *
     * Use when: You want to always refresh app metadata when creating sessions
     *
     * @param packageName Package name to create session for
     * @return SessionCreationResult with session ID or failure reason
     *
     * @since 1.0.0
     */
    @Transaction
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
                val existingApp = dao.getLearnedApp(packageName)
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

                dao.insertLearnedApp(app) // Uses REPLACE strategy

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

                dao.insertExplorationSession(session)

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

    /**
     * DEPRECATED: Use createExplorationSessionSafe() instead
     *
     * Legacy method maintained for backward compatibility.
     * Throws exception if parent app doesn't exist.
     *
     * @deprecated Use createExplorationSessionSafe() for automatic app creation
     *             or createExplorationSessionStrict() for explicit strict mode
     */
    @Deprecated(
        message = "Use createExplorationSessionSafe() for automatic app creation " +
                "or createExplorationSessionStrict() for explicit strict mode",
        replaceWith = ReplaceWith("createExplorationSessionSafe(packageName)")
    )
    suspend fun createExplorationSession(packageName: String): String {
        return createExplorationSessionStrict(packageName)
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

    /**
     * Save single navigation edge to database
     *
     * Used for incremental persistence during exploration.
     *
     * @param packageName Package name
     * @param sessionId Session ID
     * @param fromScreenHash Source screen hash
     * @param clickedElementUuid Clicked element UUID
     * @param toScreenHash Destination screen hash
     */
    suspend fun saveNavigationEdge(
        packageName: String,
        sessionId: String,
        fromScreenHash: String,
        clickedElementUuid: String,
        toScreenHash: String
    ) {
        val edgeEntity = NavigationEdgeEntity(
            edgeId = com.augmentalis.uuidcreator.core.UUIDGenerator.generate(),
            packageName = packageName,
            sessionId = sessionId,
            fromScreenHash = fromScreenHash,
            clickedElementUuid = clickedElementUuid,
            toScreenHash = toScreenHash,
            timestamp = System.currentTimeMillis()
        )
        dao.insertNavigationEdge(edgeEntity)
    }

    suspend fun saveNavigationGraph(
        graph: NavigationGraph,
        sessionId: String
    ) {
        // FOREIGN KEY VALIDATION: Ensure parent records exist before inserting children

        // 1. Verify learned app exists (parent for screen_states and navigation_edges)
        dao.getLearnedApp(graph.packageName)
            ?: throw IllegalStateException(
                "Cannot save navigation graph: LearnedAppEntity with packageName='${graph.packageName}' not found. " +
                "Insert app first using saveLearnedApp()."
            )

        // 2. Verify exploration session exists (parent for navigation_edges)
        dao.getExplorationSession(sessionId)
            ?: throw IllegalStateException(
                "Cannot save navigation graph: ExplorationSessionEntity with sessionId='$sessionId' not found. " +
                "Create session first using createExplorationSession()."
            )

        // 3. Save screen states (depends on learned_apps)
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

        // 4. Save navigation edges (depends on learned_apps AND exploration_sessions)
        val edgeEntities = graph.edges.map { edge ->
            NavigationEdgeEntity(
                edgeId = UUIDGenerator.generate(), // VOS4 UUIDGenerator
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

    /**
     * Save screen state to database
     *
     * Ensures LearnedAppEntity exists before saving (foreign key requirement).
     * If app doesn't exist, creates it with minimal metadata.
     *
     * @param screenState Screen state to save
     */
    @Transaction
    suspend fun saveScreenState(screenState: ScreenState) {
        val mutex = getMutexForPackage(screenState.packageName)
        mutex.withLock {
            // FOREIGN KEY VALIDATION: Ensure parent LearnedAppEntity exists
            val existingApp = dao.getLearnedApp(screenState.packageName)
            if (existingApp == null) {
                // Create LearnedAppEntity with minimal info
                android.util.Log.w("LearnAppRepository",
                    "LearnedAppEntity not found for ${screenState.packageName}. Creating minimal entry.")

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

                dao.insertLearnedAppMinimal(newApp)
                android.util.Log.d("LearnAppRepository",
                    "Created LearnedAppEntity for ${screenState.packageName}")
            }

            // Now save screen state (foreign key constraint satisfied)
            val entity = ScreenStateEntity(
                screenHash = screenState.hash,
                packageName = screenState.packageName,
                activityName = screenState.activityName,
                fingerprint = screenState.hash,
                elementCount = screenState.elementCount,
                discoveredAt = screenState.timestamp
            )

            dao.insertScreenState(entity)
            android.util.Log.d("LearnAppRepository",
                "Saved ScreenStateEntity: hash=${screenState.hash}, package=${screenState.packageName}, elements=${screenState.elementCount}")
        }
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
