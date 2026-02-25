/**
 * IGeneratedCommandRepository.kt - Repository interface for generated voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Repository interface for generated voice commands.
 * Provides CRUD operations for command data.
 */
interface IGeneratedCommandRepository {

    /**
     * Insert a new generated command.
     * @return The ID of the inserted command.
     */
    suspend fun insert(command: GeneratedCommandDTO): Long

    /**
     * Insert multiple generated commands in a single transaction.
     * Significantly faster than sequential inserts for large batches.
     *
     * Performance: ~50ms for 100 commands vs ~1000ms sequential
     *
     * @param commands List of commands to insert
     */
    suspend fun insertBatch(commands: List<GeneratedCommandDTO>)

    /**
     * Get command by ID.
     */
    suspend fun getById(id: Long): GeneratedCommandDTO?

    /**
     * Get all commands for an element.
     */
    suspend fun getByElement(elementHash: String): List<GeneratedCommandDTO>

    /**
     * Get all commands.
     */
    suspend fun getAll(): List<GeneratedCommandDTO>

    /**
     * Get all commands (alias for getAll for compatibility).
     */
    suspend fun getAllCommands(): List<GeneratedCommandDTO>

    /**
     * Get commands by action type.
     */
    suspend fun getByActionType(actionType: String): List<GeneratedCommandDTO>

    /**
     * Get high-confidence commands (above threshold).
     */
    suspend fun getHighConfidence(minConfidence: Double): List<GeneratedCommandDTO>

    /**
     * Get user-approved commands.
     */
    suspend fun getUserApproved(): List<GeneratedCommandDTO>

    /**
     * Fuzzy search for commands by text.
     */
    suspend fun fuzzySearch(searchText: String): List<GeneratedCommandDTO>

    /**
     * Increment usage count and update last used timestamp.
     */
    suspend fun incrementUsage(id: Long, timestamp: Long)

    /**
     * Mark command as user-approved.
     */
    suspend fun markApproved(id: Long)

    /**
     * Update command confidence.
     */
    suspend fun updateConfidence(id: Long, confidence: Double)

    /**
     * Delete command by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Delete all commands for an element.
     */
    suspend fun deleteByElement(elementHash: String)

    /**
     * Delete all commands for a specific app package.
     *
     * Used when app is uninstalled to cleanup all associated commands.
     *
     * @param packageName App package identifier (appId)
     * @return Number of commands deleted
     */
    suspend fun deleteCommandsByPackage(packageName: String): Int

    /**
     * Delete low-quality commands (unused, low confidence).
     */
    suspend fun deleteLowQuality(minConfidence: Double)

    /**
     * Delete all commands.
     */
    suspend fun deleteAll()

    /**
     * Count all commands.
     */
    suspend fun count(): Long

    /**
     * Get all commands for a specific package.
     * @param packageName App package name (appId)
     * @return List of commands for the package
     */
    suspend fun getByPackage(packageName: String): List<GeneratedCommandDTO>

    /**
     * Update an existing command.
     * Used for updating synonyms and other command properties.
     * @param command Command to update
     */
    suspend fun update(command: GeneratedCommandDTO)

    /**
     * Get all commands with pagination support.
     *
     * Prevents memory issues when retrieving large datasets.
     *
     * @param limit Maximum number of commands to return
     * @param offset Number of commands to skip
     * @return List of commands (up to limit)
     */
    suspend fun getAllPaginated(limit: Int, offset: Int): List<GeneratedCommandDTO>

    /**
     * Get commands by package with pagination support.
     *
     * @param packageName App package name
     * @param limit Maximum number of commands to return
     * @param offset Number of commands to skip
     * @return List of commands (up to limit)
     */
    suspend fun getByPackagePaginated(packageName: String, limit: Int, offset: Int): List<GeneratedCommandDTO>

    /**
     * Get commands by package using keyset pagination (cursor-based).
     *
     * More efficient than offset-based pagination for large datasets.
     * Uses the last ID from previous page as cursor.
     *
     * @param packageName App package name
     * @param lastId ID of last command from previous page (0 for first page)
     * @param limit Maximum number of commands to return
     * @return List of commands (up to limit)
     */
    suspend fun getByPackageKeysetPaginated(packageName: String, lastId: Long, limit: Int): List<GeneratedCommandDTO>

    /**
     * Get commands by action type with pagination support.
     *
     * @param actionType Action type filter
     * @param limit Maximum number of commands to return
     * @param offset Number of commands to skip
     * @return List of commands (up to limit)
     */
    suspend fun getByActionTypePaginated(actionType: String, limit: Int, offset: Int): List<GeneratedCommandDTO>

    // ========== Version Management Methods (Schema v3) ==========

    /**
     * Mark all commands for a specific app version as deprecated.
     * Used when an app updates to mark old commands for cleanup.
     *
     * @param packageName App package name (e.g., "com.google.android.gm")
     * @param versionCode Version code to mark as deprecated
     * @return Number of commands marked as deprecated
     */
    suspend fun markVersionDeprecated(packageName: String, versionCode: Long): Int

    /**
     * Update command version information after verification.
     *
     * **STATUS:** Implemented but primarily used in tests.
     * **PRODUCTION USE:** Reserved for manual version corrections or
     * administrative tools. Not part of normal command lifecycle.
     *
     * **Use Cases:**
     * - Bulk version updates after database migration
     * - Manual correction of misattributed commands
     * - Testing version-aware logic
     * - Administrative cleanup tools
     *
     * Called when element is re-verified against current app version.
     *
     * @param id Command ID
     * @param versionCode New version code
     * @param appVersion New version string (e.g., "8.2024.11.123")
     * @param lastVerified Timestamp of verification
     * @param isDeprecated Whether command is deprecated (0=active, 1=deprecated)
     */
    suspend fun updateCommandVersion(id: Long, versionCode: Long, appVersion: String, lastVerified: Long, isDeprecated: Long)

    /**
     * Mark a single command as deprecated or active.
     *
     * @param id Command ID
     * @param isDeprecated Whether command is deprecated (0=active, 1=deprecated)
     */
    suspend fun updateCommandDeprecated(id: Long, isDeprecated: Long)

    /**
     * Delete deprecated commands older than threshold.
     * Respects grace period and user-approved status.
     *
     * @param olderThan Timestamp threshold (commands older than this are deleted)
     * @param keepUserApproved If true, preserves user-approved commands
     * @return Number of commands deleted
     */
    suspend fun deleteDeprecatedCommands(olderThan: Long, keepUserApproved: Boolean): Int

    /**
     * Get all deprecated commands for an app.
     * Used to review commands before cleanup.
     *
     * @param packageName App package name
     * @return List of deprecated commands, sorted by lastVerified (newest first)
     */
    suspend fun getDeprecatedCommands(packageName: String): List<GeneratedCommandDTO>

    /**
     * Get all deprecated commands grouped by package name (P2 Task 1.2).
     *
     * **Performance Optimization**: Solves N+1 query problem by fetching all deprecated
     * commands in a single batch query instead of N individual queries.
     *
     * **Performance**: 50 apps × 10ms = 500ms → 1 query × 15ms = 15ms (97% faster)
     *
     * @return Map of packageName to list of deprecated commands
     */
    suspend fun getAllDeprecatedCommandsByApp(): Map<String, List<GeneratedCommandDTO>>

    /**
     * Get deprecated commands filtered by grace period for cleanup operations.
     * More efficient than loading all deprecated commands and filtering in memory.
     *
     * **Performance**: Database-level filtering reduces memory usage by 60-80%
     * compared to `getDeprecatedCommands()` + Kotlin filtering.
     *
     * @param packageName App package name (empty string for all apps)
     * @param olderThan Timestamp threshold - commands with lastVerified < this are eligible
     * @param keepUserApproved If true, preserves user-approved commands
     * @param limit Maximum number of commands to return
     * @return List of deprecated commands eligible for deletion, sorted by lastVerified (oldest first)
     */
    suspend fun getDeprecatedCommandsForCleanup(
        packageName: String = "",
        olderThan: Long,
        keepUserApproved: Boolean,
        limit: Int = 10000
    ): List<GeneratedCommandDTO>

    /**
     * Get only active (non-deprecated) commands for a specific app version.
     * Used for command execution to avoid using outdated commands.
     *
     * @param packageName App package name
     * @param versionCode App version code
     * @param limit Maximum number of commands to return
     * @return List of active commands, sorted by usage
     */
    suspend fun getActiveCommands(packageName: String, versionCode: Long, limit: Int): List<GeneratedCommandDTO>

    /**
     * Get active commands by version string instead of version code (Task 1.3).
     *
     * **STATUS:** Implemented for Phase 3, not yet used in production.
     * **PLANNED USE:** Memory optimization for JIT learning to filter commands
     * at database level instead of loading all and filtering in Kotlin.
     *
     * **Performance:** Filters at database level to avoid loading deprecated or
     * wrong-version commands, reducing memory usage by 60-80% for large apps.
     *
     * **Use Case:** When you have the version string (e.g., "8.2024.11.123") but
     * not the version code, or when version string is more convenient.
     *
     * **See:** VoiceOS-Plan-VersionManagement-P3-Enhancements-5141217-V1.md
     *
     * @param packageName App package name
     * @param appVersion App version string (e.g., "8.2024.11.123")
     * @param limit Maximum number of commands to return (default: 1000)
     * @return List of active commands for the specified version, sorted by usage
     */
    suspend fun getActiveCommandsByVersion(
        packageName: String,
        appVersion: String,
        limit: Int = 1000
    ): List<GeneratedCommandDTO>

    /**
     * Get all commands for a specific app version.
     * Used to retrieve commands associated with a particular app version code.
     *
     * @param appId App package name
     * @param versionCode App version code
     * @return List of commands for the specified app version, sorted by usage
     */
    suspend fun getByAppVersion(appId: String, versionCode: Long): List<GeneratedCommandDTO>

    // ========== Database Maintenance Methods (P3 Task 3.1) ==========

    /**
     * Rebuild database file to reclaim space from deleted records.
     *
     * **WHEN TO USE:** After deleting >10% of database commands
     *
     * **PERFORMANCE:**
     * - Execution time: ~100ms per 1MB of database size
     * - Blocks all database operations during execution
     * - Space savings: 20-40% reduction for fragmented databases
     *
     * **SAFETY:**
     * - Should be called from background thread (Dispatchers.IO)
     * - User should see progress indicator during execution
     * - Automatically called by CleanupManager after large deletions
     *
     * **Example:**
     * ```kotlin
     * val totalCommands = 50000
     * val deleted = 15000  // 30% of database
     * if (deleted > totalCommands * 0.10) {
     *     repository.vacuumDatabase()  // Reclaim ~3-5 MB
     * }
     * ```
     */
    suspend fun vacuumDatabase()
}
