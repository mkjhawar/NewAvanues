/**
 * AppVersionManager.kt - Orchestration layer for version-aware command lifecycle
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Manages the complete workflow for app version changes:
 * - Detect version changes (via AppVersionDetector)
 * - Update version database (via IAppVersionRepository)
 * - Mark/delete commands (via IGeneratedCommandRepository)
 * - Periodic cleanup of deprecated commands
 */

package com.augmentalis.voiceoscore.version

import android.content.Context
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates app version change detection and command lifecycle management.
 *
 * ## Responsibilities:
 * 1. **Version Change Detection**: Detect when apps are updated/downgraded/uninstalled
 * 2. **Version Database Updates**: Keep app_version table in sync with installed apps
 * 3. **Command Lifecycle Management**:
 *    - Mark old commands as deprecated when app updates
 *    - Delete commands for uninstalled apps
 *    - Preserve user-approved commands during cleanup
 * 4. **Periodic Cleanup**: Remove very old deprecated commands (30+ days)
 *
 * ## Usage:
 * ```kotlin
 * val manager = AppVersionManager(
 *     context = context,
 *     detector = appVersionDetector,
 *     versionRepo = appVersionRepository,
 *     commandRepo = generatedCommandRepository
 * )
 *
 * // Check single app (e.g., after user opens Gmail)
 * manager.checkAndUpdateApp("com.google.android.gm")
 *
 * // Periodic check all tracked apps (e.g., daily background job)
 * val processed = manager.checkAllTrackedApps()
 * log.info("Processed $processed app version checks")
 *
 * // Cleanup old deprecated commands (e.g., weekly)
 * val cleaned = manager.cleanupDeprecatedCommands(
 *     olderThan = 30.days,
 *     keepUserApproved = true
 * )
 * log.info("Cleaned up $cleaned deprecated commands")
 * ```
 *
 * ## Integration Points:
 * - **AppVersionDetector**: Detects version changes via PackageManager
 * - **IAppVersionRepository**: Stores/retrieves last known app versions
 * - **IGeneratedCommandRepository**: Manages generated command lifecycle
 *
 * @property context Android context for PackageManager access
 * @property detector Version change detector
 * @property versionRepo Repository for app version tracking
 * @property commandRepo Repository for generated commands
 */
class AppVersionManager(
    private val context: Context,
    private val detector: AppVersionDetector,
    private val versionRepo: IAppVersionRepository,
    private val commandRepo: IGeneratedCommandRepository
) {

    /**
     * Check and process version change for a single app.
     *
     * ## Workflow:
     * 1. Detect version change (via AppVersionDetector)
     * 2. Process change based on type:
     *    - **Updated**: Mark old commands deprecated, update version
     *    - **Downgraded**: Mark current commands deprecated, update version
     *    - **FirstInstall**: Store initial version
     *    - **AppNotInstalled**: Delete all commands, remove version record
     *    - **NoChange**: Update last_checked timestamp
     *
     * ## Thread Safety:
     * All operations are atomic within their respective repositories.
     * Use Dispatchers.Default for CPU-bound work.
     *
     * @param packageName App package identifier
     * @return VersionChange indicating what was processed
     */
    suspend fun checkAndUpdateApp(packageName: String): VersionChange = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        // Detect version change
        val change = detector.detectVersionChange(packageName)

        // Process based on change type
        when (change) {
            is VersionChange.Updated -> {
                // App was updated - mark old commands for re-verification
                val markedCount = commandRepo.markVersionDeprecated(
                    packageName = packageName,
                    versionCode = change.previous.versionCode
                )

                // Update stored version
                versionRepo.upsertAppVersion(
                    packageName = packageName,
                    versionName = change.current.versionName,
                    versionCode = change.current.versionCode
                )

                // Log for monitoring
                logVersionChange(
                    "Updated: $packageName ${change.previous.versionName} → ${change.current.versionName} " +
                            "(marked $markedCount commands deprecated)"
                )
            }

            is VersionChange.Downgraded -> {
                // App was downgraded (rare) - treat similar to update
                val markedCount = commandRepo.markVersionDeprecated(
                    packageName = packageName,
                    versionCode = change.previous.versionCode
                )

                // Update stored version
                versionRepo.upsertAppVersion(
                    packageName = packageName,
                    versionName = change.current.versionName,
                    versionCode = change.current.versionCode
                )

                // Log for monitoring
                logVersionChange(
                    "Downgraded: $packageName ${change.previous.versionName} → ${change.current.versionName} " +
                            "(marked $markedCount commands deprecated)"
                )
            }

            is VersionChange.FirstInstall -> {
                // New app - store initial version
                versionRepo.upsertAppVersion(
                    packageName = packageName,
                    versionName = change.current.versionName,
                    versionCode = change.current.versionCode
                )

                // Log for monitoring
                logVersionChange("FirstInstall: $packageName ${change.current.versionName}")
            }

            is VersionChange.AppNotInstalled -> {
                // App was uninstalled - cleanup
                val deletedCommands = commandRepo.deleteCommandsByPackage(packageName)
                val deletedVersion = versionRepo.deleteAppVersion(packageName)

                // Log for monitoring
                logVersionChange(
                    "AppNotInstalled: $packageName " +
                            "(deleted $deletedCommands commands, version record: $deletedVersion)"
                )
            }

            is VersionChange.NoChange -> {
                // Same version - just update last_checked timestamp
                versionRepo.updateLastChecked(packageName)

                // No logging needed for NoChange (too noisy)
            }
        }

        change
    }

    /**
     * Check and process version changes for all tracked apps.
     *
     * Queries all apps with version records in the database and checks
     * each one for updates/uninstalls. Useful for:
     * - Daily background job to detect app updates
     * - VoiceOS service startup to sync app states
     * - Manual "check for updates" user action
     *
     * ## Performance:
     * - Queries all tracked apps (1 DB query)
     * - Checks each app (N PackageManager calls + N DB updates)
     * - Typical: 20-50 apps, ~2-5 seconds total
     *
     * @return Number of apps processed
     */
    suspend fun checkAllTrackedApps(): Int = withContext(Dispatchers.Default) {
        val changes = detector.detectAllVersionChanges()

        // Process each change
        changes.forEach { change ->
            // checkAndUpdateApp is already implemented above,
            // but we need to call the appropriate logic based on change type
            // to avoid redundant detection
            processVersionChange(change)
        }

        changes.size
    }

    /**
     * Process a detected version change.
     *
     * Helper method to process a VersionChange without re-detecting.
     * Used by checkAllTrackedApps() to avoid redundant PackageManager calls.
     *
     * @param change Detected version change
     */
    private suspend fun processVersionChange(change: VersionChange) {
        when (change) {
            is VersionChange.Updated -> {
                val markedCount = commandRepo.markVersionDeprecated(
                    packageName = change.packageName,
                    versionCode = change.previous.versionCode
                )

                versionRepo.upsertAppVersion(
                    packageName = change.packageName,
                    versionName = change.current.versionName,
                    versionCode = change.current.versionCode
                )

                logVersionChange(
                    "Updated: ${change.packageName} ${change.previous.versionName} → ${change.current.versionName} " +
                            "(marked $markedCount commands deprecated)"
                )
            }

            is VersionChange.Downgraded -> {
                val markedCount = commandRepo.markVersionDeprecated(
                    packageName = change.packageName,
                    versionCode = change.previous.versionCode
                )

                versionRepo.upsertAppVersion(
                    packageName = change.packageName,
                    versionName = change.current.versionName,
                    versionCode = change.current.versionCode
                )

                logVersionChange(
                    "Downgraded: ${change.packageName} ${change.previous.versionName} → ${change.current.versionName} " +
                            "(marked $markedCount commands deprecated)"
                )
            }

            is VersionChange.FirstInstall -> {
                versionRepo.upsertAppVersion(
                    packageName = change.packageName,
                    versionName = change.current.versionName,
                    versionCode = change.current.versionCode
                )

                logVersionChange("FirstInstall: ${change.packageName} ${change.current.versionName}")
            }

            is VersionChange.AppNotInstalled -> {
                val deletedCommands = commandRepo.deleteCommandsByPackage(change.packageName)
                val deletedVersion = versionRepo.deleteAppVersion(change.packageName)

                logVersionChange(
                    "AppNotInstalled: ${change.packageName} " +
                            "(deleted $deletedCommands commands, version record: $deletedVersion)"
                )
            }

            is VersionChange.NoChange -> {
                versionRepo.updateLastChecked(change.packageName)
            }
        }
    }

    /**
     * Clean up deprecated commands that are very old.
     *
     * Deletes commands that were marked deprecated more than X days ago.
     * This prevents indefinite accumulation of old commands.
     *
     * ## Grace Period:
     * Commands are kept for a grace period (typically 30 days) to allow:
     * - User to continue using familiar commands during transition
     * - VoiceOS to re-verify commands are still valid
     * - Rollback if user downgrades app
     *
     * ## User-Approved Commands:
     * Commands that users explicitly approved/corrected are preserved by default.
     * These are valuable training data and should not be lost.
     *
     * ## Usage:
     * ```kotlin
     * // Weekly cleanup job
     * val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
     * val cleaned = manager.cleanupDeprecatedCommands(
     *     olderThan = thirtyDaysAgo,
     *     keepUserApproved = true
     * )
     * log.info("Cleaned up $cleaned old deprecated commands")
     * ```
     *
     * @param olderThan Timestamp (epoch millis) - delete commands deprecated before this
     * @param keepUserApproved If true, preserve user-approved commands even if deprecated
     * @return Number of commands deleted
     */
    suspend fun cleanupDeprecatedCommands(
        olderThan: Long,
        keepUserApproved: Boolean = true
    ): Int = withContext(Dispatchers.Default) {
        require(olderThan > 0) { "olderThan must be positive" }

        val deletedCount = commandRepo.deleteDeprecatedCommands(
            olderThan = olderThan,
            keepUserApproved = keepUserApproved
        )

        if (deletedCount > 0) {
            logVersionChange(
                "Cleanup: Deleted $deletedCount deprecated commands " +
                        "(older than ${formatTimestamp(olderThan)}, keepUserApproved=$keepUserApproved)"
            )
        }

        deletedCount
    }

    /**
     * Get statistics about deprecated commands.
     *
     * Useful for monitoring and dashboards.
     *
     * @return Map of packageName to count of deprecated commands
     */
    suspend fun getDeprecatedCommandStats(): Map<String, Int> = withContext(Dispatchers.Default) {
        // Get all deprecated commands grouped by package
        val allVersions = versionRepo.getAllAppVersions()

        allVersions.keys.associateWith { packageName ->
            val deprecated = commandRepo.getDeprecatedCommands(packageName)
            deprecated.size
        }.filterValues { it > 0 } // Only return packages with deprecated commands
    }

    /**
     * Get statistics about version tracking.
     *
     * @return VersionStats with counts and metrics
     */
    suspend fun getVersionStats(): VersionStats = withContext(Dispatchers.Default) {
        val allVersions = versionRepo.getAllAppVersions()
        val totalCommands = commandRepo.count()

        VersionStats(
            trackedApps = allVersions.size,
            totalCommands = totalCommands.toInt(),
            deprecatedCommands = getDeprecatedCommandStats().values.sum()
        )
    }

    /**
     * Force re-check of a specific app even if version unchanged.
     *
     * Useful for:
     * - User manually requests re-scan
     * - Debugging command issues
     * - Testing version detection
     *
     * @param packageName App to re-check
     */
    suspend fun forceRecheckApp(packageName: String) = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        // Get current installed version
        val installedVersion = detector.isAppInstalled(packageName)

        if (!installedVersion) {
            // App not installed - cleanup
            val change = VersionChange.AppNotInstalled(packageName)
            processVersionChange(change)
        } else {
            // App installed - just update last_checked
            versionRepo.updateLastChecked(packageName)
            logVersionChange("ForceRecheck: $packageName (no changes detected)")
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Log version change event.
     *
     * Currently uses println for simplicity. In production, should use
     * proper logging framework (Timber, Logcat, etc.)
     *
     * @param message Log message
     */
    private fun logVersionChange(message: String) {
        println("[AppVersionManager] $message")
        // TODO: Replace with proper logging framework
        // Timber.i(message)
    }

    /**
     * Format timestamp for human-readable logging.
     *
     * @param timestamp Epoch millis
     * @return Formatted string (e.g., "2025-12-14 14:30:00")
     */
    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        return format.format(date)
    }
}

/**
 * Statistics about version tracking and command lifecycle.
 *
 * @property trackedApps Number of apps being tracked
 * @property totalCommands Total number of commands in database
 * @property deprecatedCommands Number of commands marked as deprecated
 */
data class VersionStats(
    val trackedApps: Int,
    val totalCommands: Int,
    val deprecatedCommands: Int
) {
    /**
     * Percentage of commands that are deprecated.
     */
    val deprecationRate: Double
        get() = if (totalCommands > 0) {
            (deprecatedCommands.toDouble() / totalCommands.toDouble()) * 100.0
        } else {
            0.0
        }

    override fun toString(): String {
        return "VersionStats(trackedApps=$trackedApps, totalCommands=$totalCommands, " +
                "deprecatedCommands=$deprecatedCommands, deprecationRate=${"%.2f".format(deprecationRate)}%)"
    }
}
