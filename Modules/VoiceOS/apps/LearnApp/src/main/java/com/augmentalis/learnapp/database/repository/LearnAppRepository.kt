/**
 * LearnAppRepository.kt - Repository pattern for LearnApp data access
 * Path: apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/repository/LearnAppRepository.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-12-18 (Migrated from Room to SQLDelight)
 *
 * Repository providing clean API for LearnApp database operations
 * Now uses SQLDelight via VoiceOSDatabaseManager instead of Room DAO
 */

package com.augmentalis.learnapp.database.repository

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.learnapp.database.entities.*
import com.augmentalis.learnapp.models.ExplorationStats
import com.augmentalis.learnapp.models.ScreenState
import com.augmentalis.learnapp.navigation.NavigationGraph
import com.augmentalis.learnapp.navigation.ScreenNode
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
 * Uses SQLDelight via VoiceOSDatabaseManager for database access.
 *
 * Features:
 * - 4 patterns for session creation (auto-create, strict, explicit, upsert)
 * - Per-package Mutex for race condition prevention
 * - Dual-database metadata lookup (AppScrapingDatabase + PackageManager)
 * - VOS4 UUIDGenerator integration
 * - Type-safe Result types
 * - Transaction support via SQLDelight
 *
 * @property dbManager VoiceOSDatabaseManager for SQLDelight access
 * @property context Application context for metadata provider
 * @property scrapedAppMetadataSource Optional interface for AppScrapingDatabase metadata lookup
 *
 * @since 1.0.0
 */
class LearnAppRepository(
    private val dbManager: VoiceOSDatabaseManager,
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

    // SQLDelight query accessors
    private val learnedAppQueries get() = dbManager.learnedAppQueries
    private val explorationSessionQueries get() = dbManager.explorationSessionQueries
    private val navigationEdgeQueries get() = dbManager.navigationEdgeQueries
    private val screenStateQueries get() = dbManager.screenStateQueries

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
    ) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        learnedAppQueries.insertLearnedApp(
            package_name = packageName,
            app_name = appName,
            version_code = versionCode.toLong(),
            version_name = versionName,
            first_learned_at = now,
            last_updated_at = now,
            total_screens = stats.totalScreens.toLong(),
            total_elements = stats.totalElements.toLong(),
            app_hash = calculateAppHash(packageName),
            exploration_status = ExplorationStatus.COMPLETE,
            learning_mode = "AUTO_DETECT",
            status = "LEARNED",
            progress = 100,
            command_count = 0,
            screens_explored = stats.totalScreens.toLong(),
            is_auto_detect_enabled = 1
        )
    }

    suspend fun getLearnedApp(packageName: String): LearnedAppEntity? = withContext(Dispatchers.Default) {
        learnedAppQueries.getLearnedApp(packageName).executeAsOneOrNull()?.let { row ->
            LearnedAppEntity(
                packageName = row.package_name,
                appName = row.app_name,
                versionCode = row.version_code,
                versionName = row.version_name,
                firstLearnedAt = row.first_learned_at,
                lastUpdatedAt = row.last_updated_at,
                totalScreens = row.total_screens.toInt(),
                totalElements = row.total_elements.toInt(),
                appHash = row.app_hash,
                explorationStatus = row.exploration_status
            )
        }
    }

    suspend fun isAppLearned(packageName: String): Boolean = withContext(Dispatchers.Default) {
        learnedAppQueries.getLearnedApp(packageName).executeAsOneOrNull() != null
    }

    suspend fun getAllLearnedApps(): List<LearnedAppEntity> = withContext(Dispatchers.Default) {
        learnedAppQueries.getAllLearnedApps().executeAsList().map { row ->
            LearnedAppEntity(
                packageName = row.package_name,
                appName = row.app_name,
                versionCode = row.version_code,
                versionName = row.version_name,
                firstLearnedAt = row.first_learned_at,
                lastUpdatedAt = row.last_updated_at,
                totalScreens = row.total_screens.toInt(),
                totalElements = row.total_elements.toInt(),
                appHash = row.app_hash,
                explorationStatus = row.exploration_status
            )
        }
    }

    suspend fun updateAppHash(packageName: String, newHash: String) = withContext(Dispatchers.Default) {
        learnedAppQueries.updateAppHash(newHash, System.currentTimeMillis(), packageName)
    }

    suspend fun deleteLearnedApp(packageName: String) = withContext(Dispatchers.Default) {
        learnedAppQueries.deleteLearnedApp(packageName)
    }

    /**
     * Delete ALL exploration data for an app (complete reset)
     *
     * Deletes the learned app AND all associated data:
     * - Exploration sessions
     * - Navigation edges
     * - Screen states
     * - (Note: Foreign key CASCADE handles children automatically)
     *
     * Use this when you want to completely remove an app and start fresh.
     *
     * @param packageName Package name to delete
     * @return RepositoryResult.Success if deleted, Failure if app doesn't exist or error occurs
     *
     * @since 1.0.0
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

                android.util.Log.d("LearnAppRepository",
                    "Deleting app completely: $packageName")

                // Use transaction for atomicity
                withContext(Dispatchers.Default) {
                    dbManager.transaction {
                        // Delete associated data first (explicit, though CASCADE should handle this)
                        navigationEdgeQueries.deleteNavigationGraph(packageName)
                        android.util.Log.d("LearnAppRepository", "Deleted navigation edges for $packageName")

                        screenStateQueries.deleteScreenStatesForPackage(packageName)
                        android.util.Log.d("LearnAppRepository", "Deleted screen states for $packageName")

                        explorationSessionQueries.deleteSessionsForPackage(packageName)
                        android.util.Log.d("LearnAppRepository", "Deleted exploration sessions for $packageName")

                        // Finally delete the app itself
                        learnedAppQueries.deleteLearnedApp(packageName)
                        android.util.Log.d("LearnAppRepository", "Deleted learned app: $packageName")
                    }
                }

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

                android.util.Log.d("LearnAppRepository",
                    "Resetting app for relearning: $packageName")

                withContext(Dispatchers.Default) {
                    dbManager.transaction {
                        // Delete exploration data
                        navigationEdgeQueries.deleteNavigationGraph(packageName)
                        screenStateQueries.deleteScreenStatesForPackage(packageName)
                        explorationSessionQueries.deleteSessionsForPackage(packageName)

                        // Update app status to PARTIAL (ready for relearning)
                        learnedAppQueries.updateStatus(
                            ExplorationStatus.PARTIAL,
                            System.currentTimeMillis(),
                            packageName
                        )
                        learnedAppQueries.updateAppStats(
                            total_screens = 0,
                            total_elements = 0,
                            last_updated_at = System.currentTimeMillis(),
                            package_name = packageName
                        )
                    }
                }

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
    suspend fun clearExplorationData(packageName: String): RepositoryResult<Boolean> {
        val mutex = getMutexForPackage(packageName)
        return mutex.withLock {
            try {
                android.util.Log.d("LearnAppRepository",
                    "Clearing exploration data for: $packageName")

                withContext(Dispatchers.Default) {
                    dbManager.transaction {
                        navigationEdgeQueries.deleteNavigationGraph(packageName)
                        screenStateQueries.deleteScreenStatesForPackage(packageName)
                        explorationSessionQueries.deleteSessionsForPackage(packageName)
                    }
                }

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
     * - Atomic operation (transaction)
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

                    // Create LearnedAppEntity with minimal metadata (IGNORE strategy)
                    val now = System.currentTimeMillis()
                    withContext(Dispatchers.Default) {
                        learnedAppQueries.insertLearnedAppMinimal(
                            package_name = metadata.packageName,
                            app_name = metadata.appName,
                            version_code = metadata.versionCode.toLong(),
                            version_name = metadata.versionName,
                            first_learned_at = now,
                            last_updated_at = now,
                            total_screens = 0,
                            total_elements = 0,
                            app_hash = metadata.appHash,
                            exploration_status = ExplorationStatus.PARTIAL
                        )
                    }
                    appWasCreated = true
                    metadataSource = metadata.source
                } else {
                    appWasCreated = false
                    metadataSource = null
                }

                // Generate session ID using VOS4 UUIDGenerator
                val sessionId = UUIDGenerator.generate()

                // Create exploration session
                val now = System.currentTimeMillis()
                withContext(Dispatchers.Default) {
                    explorationSessionQueries.insertExplorationSession(
                        session_id = sessionId,
                        package_name = packageName,
                        started_at = now,
                        completed_at = null,
                        duration_ms = null,
                        screens_explored = 0,
                        elements_discovered = 0,
                        status = SessionStatus.RUNNING
                    )
                }

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
    suspend fun createExplorationSessionStrict(packageName: String): String {
        // Verify parent app exists
        getLearnedApp(packageName)
            ?: throw LearnedAppNotFoundException(packageName)

        // Generate session ID using VOS4 UUIDGenerator
        val sessionId = UUIDGenerator.generate()

        // Create exploration session
        val now = System.currentTimeMillis()
        withContext(Dispatchers.Default) {
            explorationSessionQueries.insertExplorationSession(
                session_id = sessionId,
                package_name = packageName,
                started_at = now,
                completed_at = null,
                duration_ms = null,
                screens_explored = 0,
                elements_discovered = 0,
                status = SessionStatus.RUNNING
            )
        }
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
                val now = System.currentTimeMillis()
                withContext(Dispatchers.Default) {
                    learnedAppQueries.insertLearnedAppMinimal(
                        package_name = metadata.packageName,
                        app_name = metadata.appName,
                        version_code = metadata.versionCode.toLong(),
                        version_name = metadata.versionName,
                        first_learned_at = now,
                        last_updated_at = now,
                        total_screens = 0,
                        total_elements = 0,
                        app_hash = metadata.appHash,
                        exploration_status = ExplorationStatus.PARTIAL
                    )
                }
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
                val now = System.currentTimeMillis()
                withContext(Dispatchers.Default) {
                    learnedAppQueries.insertLearnedApp(
                        package_name = metadata.packageName,
                        app_name = metadata.appName,
                        version_code = metadata.versionCode.toLong(),
                        version_name = metadata.versionName,
                        first_learned_at = existingApp?.firstLearnedAt ?: now,
                        last_updated_at = now,
                        total_screens = existingApp?.totalScreens?.toLong() ?: 0,
                        total_elements = existingApp?.totalElements?.toLong() ?: 0,
                        app_hash = metadata.appHash,
                        exploration_status = existingApp?.explorationStatus ?: ExplorationStatus.PARTIAL,
                        learning_mode = "AUTO_DETECT",
                        status = "LEARNING",
                        progress = 0,
                        command_count = 0,
                        screens_explored = existingApp?.totalScreens?.toLong() ?: 0,
                        is_auto_detect_enabled = 1
                    )
                }

                // Generate session ID using VOS4 UUIDGenerator
                val sessionId = UUIDGenerator.generate()

                // Create exploration session
                withContext(Dispatchers.Default) {
                    explorationSessionQueries.insertExplorationSession(
                        session_id = sessionId,
                        package_name = packageName,
                        started_at = now,
                        completed_at = null,
                        duration_ms = null,
                        screens_explored = 0,
                        elements_discovered = 0,
                        status = SessionStatus.RUNNING
                    )
                }

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
    ) = withContext(Dispatchers.Default) {
        explorationSessionQueries.updateSessionStatus(
            status = SessionStatus.COMPLETED,
            completed_at = System.currentTimeMillis(),
            duration_ms = stats.durationMs,
            session_id = sessionId
        )
    }

    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity? = withContext(Dispatchers.Default) {
        explorationSessionQueries.getExplorationSession(sessionId).executeAsOneOrNull()?.let { row ->
            ExplorationSessionEntity(
                sessionId = row.session_id,
                packageName = row.package_name,
                startedAt = row.started_at,
                completedAt = row.completed_at,
                durationMs = row.duration_ms,
                screensExplored = row.screens_explored.toInt(),
                elementsDiscovered = row.elements_discovered.toInt(),
                status = row.status
            )
        }
    }

    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity> = withContext(Dispatchers.Default) {
        explorationSessionQueries.getSessionsForPackage(packageName).executeAsList().map { row ->
            ExplorationSessionEntity(
                sessionId = row.session_id,
                packageName = row.package_name,
                startedAt = row.started_at,
                completedAt = row.completed_at,
                durationMs = row.duration_ms,
                screensExplored = row.screens_explored.toInt(),
                elementsDiscovered = row.elements_discovered.toInt(),
                status = row.status
            )
        }
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
    ) = withContext(Dispatchers.Default) {
        navigationEdgeQueries.insertNavigationEdge(
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
    ) = withContext(Dispatchers.Default) {
        // FOREIGN KEY VALIDATION: Ensure parent records exist before inserting children

        // 1. Verify learned app exists (parent for screen_states and navigation_edges)
        learnedAppQueries.getLearnedApp(graph.packageName).executeAsOneOrNull()
            ?: throw IllegalStateException(
                "Cannot save navigation graph: LearnedAppEntity with packageName='${graph.packageName}' not found. " +
                "Insert app first using saveLearnedApp()."
            )

        // 2. Verify exploration session exists (parent for navigation_edges)
        explorationSessionQueries.getExplorationSession(sessionId).executeAsOneOrNull()
            ?: throw IllegalStateException(
                "Cannot save navigation graph: ExplorationSessionEntity with sessionId='$sessionId' not found. " +
                "Create session first using createExplorationSession()."
            )

        // Use transaction for atomicity
        dbManager.transaction {
            // 3. Save screen states (depends on learned_apps)
            graph.nodes.values.forEach { screenNode ->
                screenStateQueries.insertScreenState(
                    screen_hash = screenNode.screenHash,
                    package_name = graph.packageName,
                    activity_name = screenNode.activityName ?: "",
                    fingerprint = screenNode.screenHash,
                    element_count = screenNode.elements.size.toLong(),
                    discovered_at = screenNode.timestamp
                )
            }

            // 4. Save navigation edges (depends on learned_apps AND exploration_sessions)
            graph.edges.forEach { edge ->
                navigationEdgeQueries.insertNavigationEdge(
                    edge_id = UUIDGenerator.generate(),
                    package_name = graph.packageName,
                    session_id = sessionId,
                    from_screen_hash = edge.fromScreenHash,
                    clicked_element_uuid = edge.clickedElementUuid,
                    to_screen_hash = edge.toScreenHash,
                    timestamp = edge.timestamp
                )
            }
        }
    }

    suspend fun getNavigationGraph(packageName: String): NavigationGraph? = withContext(Dispatchers.Default) {
        val edges = navigationEdgeQueries.getNavigationGraph(packageName).executeAsList()
        if (edges.isEmpty()) return@withContext null

        val screenStates = screenStateQueries.getScreenStatesForPackage(packageName).executeAsList()

        // Note: Elements are stored in ElementsRegistry, not per-screen.
        // ScreenNode.elements is used for graph visualization stats only.
        // For element queries, use ElementsRegistry.getElementsForPackage()
        val nodes = screenStates.associate { row ->
            row.screen_hash to ScreenNode(
                screenHash = row.screen_hash,
                activityName = row.activity_name,
                elements = emptyList(),  // Elements stored in ElementsRegistry
                timestamp = row.discovered_at
            )
        }

        val navigationEdges = edges.map { edge ->
            com.augmentalis.learnapp.models.NavigationEdge(
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

    suspend fun deleteNavigationGraph(packageName: String) = withContext(Dispatchers.Default) {
        navigationEdgeQueries.deleteNavigationGraph(packageName)
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
    suspend fun saveScreenState(screenState: ScreenState) {
        val mutex = getMutexForPackage(screenState.packageName)
        mutex.withLock {
            withContext(Dispatchers.Default) {
                // FOREIGN KEY VALIDATION: Ensure parent LearnedAppEntity exists
                val existingApp = learnedAppQueries.getLearnedApp(screenState.packageName).executeAsOneOrNull()
                if (existingApp == null) {
                    // Create LearnedAppEntity with minimal info
                    android.util.Log.w("LearnAppRepository",
                        "LearnedAppEntity not found for ${screenState.packageName}. Creating minimal entry.")

                    val metadata = metadataProvider?.getMetadata(screenState.packageName)
                    val now = System.currentTimeMillis()

                    learnedAppQueries.insertLearnedAppMinimal(
                        package_name = screenState.packageName,
                        app_name = metadata?.appName ?: screenState.packageName,
                        version_code = metadata?.versionCode?.toLong() ?: 0,
                        version_name = metadata?.versionName ?: "unknown",
                        first_learned_at = now,
                        last_updated_at = now,
                        total_screens = 0,
                        total_elements = 0,
                        app_hash = metadata?.appHash ?: calculateAppHash(screenState.packageName),
                        exploration_status = ExplorationStatus.PARTIAL
                    )
                    android.util.Log.d("LearnAppRepository",
                        "Created LearnedAppEntity for ${screenState.packageName}")
                }

                // Now save screen state (foreign key constraint satisfied)
                screenStateQueries.insertScreenState(
                    screen_hash = screenState.hash,
                    package_name = screenState.packageName,
                    activity_name = screenState.activityName ?: "",
                    fingerprint = screenState.hash,
                    element_count = screenState.elementCount.toLong(),
                    discovered_at = screenState.timestamp
                )
                android.util.Log.d("LearnAppRepository",
                    "Saved ScreenStateEntity: hash=${screenState.hash}, package=${screenState.packageName}, elements=${screenState.elementCount}")
            }
        }
    }

    suspend fun getScreenState(hash: String): ScreenStateEntity? = withContext(Dispatchers.Default) {
        screenStateQueries.getScreenState(hash).executeAsOneOrNull()?.let { row ->
            ScreenStateEntity(
                screenHash = row.screen_hash,
                packageName = row.package_name,
                activityName = row.activity_name,
                fingerprint = row.fingerprint,
                elementCount = row.element_count.toInt(),
                discoveredAt = row.discovered_at
            )
        }
    }

    // ========== Statistics ==========

    suspend fun getAppStatistics(packageName: String): AppStatistics = withContext(Dispatchers.Default) {
        val totalScreens = screenStateQueries.getTotalScreensForPackage(packageName).executeAsOne()
        val totalEdges = navigationEdgeQueries.getTotalEdgesForPackage(packageName).executeAsOne()
        val sessions = explorationSessionQueries.getSessionsForPackage(packageName).executeAsList()

        AppStatistics(
            packageName = packageName,
            totalScreens = totalScreens.toInt(),
            totalEdges = totalEdges.toInt(),
            totalSessions = sessions.size,
            lastExplored = sessions.firstOrNull()?.started_at
        )
    }

    // ========== Utility ==========

    /**
     * Calculate app hash from package name
     *
     * Uses MD5 hash for consistency with AppScrapingDatabase.
     * When version is needed, use the overloaded version with versionCode.
     *
     * @param packageName Package name
     * @return MD5 hash string
     */
    private fun calculateAppHash(packageName: String): String {
        val bytes = java.security.MessageDigest.getInstance("MD5")
            .digest(packageName.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
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
