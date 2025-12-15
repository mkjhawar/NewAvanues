/**
 * CleanupManager.kt - Version-aware command cleanup manager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Provides safe cleanup operations for deprecated commands with preview,
 * validation, and safety checks to prevent accidental data loss.
 */

package com.augmentalis.voiceoscore.cleanup

import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.system.measureTimeMillis

/**
 * Manages cleanup of deprecated commands with safety checks and preview capabilities.
 *
 * ## Responsibilities:
 * 1. **Preview Cleanup**: Calculate statistics before deletion
 * 2. **Safe Deletion**: Apply safety checks to prevent accidental data loss
 * 3. **Grace Period**: Only delete commands deprecated beyond threshold
 * 4. **User Protection**: Optionally preserve user-approved commands
 *
 * ## Safety Features:
 * - **90% Safety Limit**: Refuses to delete >90% of total commands
 * - **Grace Period**: 1-365 days (default: 30 days)
 * - **Dry Run Mode**: Preview deletions without executing
 * - **User-Approved Protection**: Optional preservation of user-approved commands
 * - **Transaction Safety**: All deletions in repository transactions
 *
 * ## Usage:
 * ```kotlin
 * val manager = CleanupManager(commandRepository)
 *
 * // Preview cleanup
 * val preview = manager.previewCleanup(
 *     gracePeriodDays = 30,
 *     keepUserApproved = true
 * )
 * log.info("Will delete ${preview.commandsToDelete} commands affecting ${preview.appsAffected.size} apps")
 *
 * // Execute cleanup
 * val result = manager.executeCleanup(
 *     gracePeriodDays = 30,
 *     keepUserApproved = true,
 *     dryRun = false
 * )
 * log.info("Deleted ${result.deletedCount} commands in ${result.durationMs}ms")
 *
 * // Dry run
 * val dryRun = manager.executeCleanup(dryRun = true)
 * log.info("Would delete ${dryRun.deletedCount} commands")
 * ```
 *
 * ## Integration:
 * - Called by periodic background jobs (e.g., weekly cleanup)
 * - Called before database compaction to reduce size
 * - Called by user-initiated cleanup in settings
 *
 * @property commandRepo Repository for generated command operations
 */
class CleanupManager(
    private val commandRepo: IGeneratedCommandRepository
) {

    companion object {
        /**
         * Maximum percentage of commands that can be deleted in single operation.
         * Safety limit to prevent accidental mass deletion.
         */
        private const val MAX_DELETE_PERCENTAGE = 0.90

        /**
         * Minimum grace period in days.
         */
        private const val MIN_GRACE_PERIOD_DAYS = 1

        /**
         * Maximum grace period in days.
         */
        private const val MAX_GRACE_PERIOD_DAYS = 365

        /**
         * Milliseconds in one day.
         */
        private const val MILLIS_PER_DAY = 86400000L
    }

    /**
     * Preview statistics for cleanup operation.
     *
     * Provides detailed information about what will be deleted without
     * actually performing the deletion.
     *
     * @property commandsToDelete Number of commands that will be deleted
     * @property appsAffected List of unique app package names affected
     * @property databaseSizeReduction Estimated database size reduction in bytes
     * @property oldestCommandDate Timestamp of oldest command to be deleted
     * @property newestCommandDate Timestamp of newest command to be deleted
     */
    data class CleanupPreview(
        val commandsToDelete: Int,
        val appsAffected: List<String>,
        val databaseSizeReduction: Long,
        val oldestCommandDate: Long,
        val newestCommandDate: Long
    )

    /**
     * Result of cleanup operation.
     *
     * @property deletedCount Number of commands actually deleted
     * @property preservedCount Number of commands preserved (user-approved)
     * @property gracePeriodDays Grace period used for cleanup
     * @property keepUserApproved Whether user-approved commands were preserved
     * @property errors List of error messages encountered during cleanup
     * @property durationMs Total operation duration in milliseconds
     */
    data class CleanupResult(
        val deletedCount: Int,
        val preservedCount: Int,
        val gracePeriodDays: Int,
        val keepUserApproved: Boolean,
        val errors: List<String>,
        val durationMs: Long = 0L
    )

    /**
     * Preview cleanup operation without executing.
     *
     * Calculates statistics for deprecated commands that would be deleted
     * based on grace period and user approval settings.
     *
     * ## Algorithm:
     * 1. Calculate cutoff timestamp (current time - grace period)
     * 2. Query deprecated commands older than cutoff
     * 3. Filter by user-approved status if requested
     * 4. Calculate statistics (count, apps, size, date range)
     *
     * ## Performance:
     * - Uses repository queries (O(n) where n = deprecated commands)
     * - Runs on Dispatchers.Default for CPU-bound work
     * - Typical execution: <100ms for 1000 commands
     *
     * @param gracePeriodDays Number of days before deprecated commands are eligible for deletion (1-365)
     * @param keepUserApproved If true, user-approved commands are preserved regardless of age
     * @return CleanupPreview with statistics
     * @throws IllegalArgumentException if gracePeriodDays is outside valid range
     */
    suspend fun previewCleanup(
        gracePeriodDays: Int = 30,
        keepUserApproved: Boolean = true
    ): CleanupPreview = withContext(Dispatchers.Default) {
        // Validate input
        require(gracePeriodDays in MIN_GRACE_PERIOD_DAYS..MAX_GRACE_PERIOD_DAYS) {
            "gracePeriodDays must be between $MIN_GRACE_PERIOD_DAYS and $MAX_GRACE_PERIOD_DAYS, got $gracePeriodDays"
        }

        // Calculate cutoff timestamp
        val cutoffTimestamp = System.currentTimeMillis() - (gracePeriodDays * MILLIS_PER_DAY)

        // Get all commands to analyze
        val allCommands = commandRepo.getAll()

        // Filter deprecated commands older than cutoff
        val eligibleForDeletion = allCommands.filter { command ->
            val isDeprecated = command.isDeprecated == 1L
            val isOldEnough = (command.lastVerified ?: command.createdAt) < cutoffTimestamp
            val shouldDelete = if (keepUserApproved) {
                command.isUserApproved == 0L
            } else {
                true
            }

            isDeprecated && isOldEnough && shouldDelete
        }

        // Calculate statistics
        val commandsToDelete = eligibleForDeletion.size
        val appsAffected = eligibleForDeletion.map { it.appId }.distinct().sorted()

        // Estimate database size reduction
        // Average command: ~200 bytes (elementHash + commandText + synonyms + metadata)
        val avgCommandSize = 200L
        val databaseSizeReduction = commandsToDelete * avgCommandSize

        // Find date range
        val timestamps = eligibleForDeletion.mapNotNull { it.lastVerified ?: it.createdAt }
        val oldestCommandDate = timestamps.minOrNull() ?: 0L
        val newestCommandDate = timestamps.maxOrNull() ?: 0L

        CleanupPreview(
            commandsToDelete = commandsToDelete,
            appsAffected = appsAffected,
            databaseSizeReduction = databaseSizeReduction,
            oldestCommandDate = oldestCommandDate,
            newestCommandDate = newestCommandDate
        )
    }

    /**
     * Execute cleanup operation with safety checks.
     *
     * Deletes deprecated commands older than grace period with comprehensive
     * safety checks to prevent accidental data loss.
     *
     * ## Safety Checks:
     * 1. **Input Validation**: gracePeriodDays in valid range (1-365)
     * 2. **90% Safety Limit**: Refuses to delete >90% of total commands
     * 3. **Transaction Safety**: Repository handles transaction rollback on error
     * 4. **Error Collection**: Non-fatal errors collected, operation continues
     *
     * ## Algorithm:
     * 1. Validate input parameters
     * 2. Preview cleanup to get deletion count
     * 3. Calculate total commands and deletion percentage
     * 4. Apply 90% safety check
     * 5. If dry run: return preview as result
     * 6. Otherwise: call repository deletion method
     * 7. Collect errors and measure duration
     *
     * ## Thread Safety:
     * - Runs on Dispatchers.Default
     * - Repository methods are transaction-safe
     * - No shared mutable state
     *
     * ## Performance:
     * - Preview: O(n) where n = total commands
     * - Deletion: O(m) where m = commands to delete
     * - Typical execution: 100-500ms for 1000 deletions
     *
     * @param gracePeriodDays Number of days before deprecated commands are eligible for deletion (1-365)
     * @param keepUserApproved If true, user-approved commands are preserved
     * @param dryRun If true, calculate what would be deleted without executing
     * @return CleanupResult with deletion statistics and errors
     * @throws IllegalArgumentException if gracePeriodDays is outside valid range
     * @throws IllegalStateException if attempting to delete >90% of commands
     */
    suspend fun executeCleanup(
        gracePeriodDays: Int = 30,
        keepUserApproved: Boolean = true,
        dryRun: Boolean = false
    ): CleanupResult = withContext(Dispatchers.Default) {
        // Validate input
        require(gracePeriodDays in MIN_GRACE_PERIOD_DAYS..MAX_GRACE_PERIOD_DAYS) {
            "gracePeriodDays must be between $MIN_GRACE_PERIOD_DAYS and $MAX_GRACE_PERIOD_DAYS, got $gracePeriodDays"
        }

        val errors = mutableListOf<String>()
        var deletedCount = 0
        var preservedCount = 0

        val durationMs = measureTimeMillis {
            try {
                // Get preview to calculate safety metrics
                val preview = previewCleanup(gracePeriodDays, keepUserApproved)
                val totalCommands = commandRepo.count().toInt()

                // Safety check: refuse to delete >90% of commands
                val deletePercentage = if (totalCommands > 0) {
                    preview.commandsToDelete.toDouble() / totalCommands.toDouble()
                } else {
                    0.0
                }

                if (deletePercentage > MAX_DELETE_PERCENTAGE) {
                    throw IllegalStateException(
                        "Safety limit exceeded: attempting to delete ${preview.commandsToDelete} of $totalCommands commands " +
                        "(${(deletePercentage * 100).toInt()}% > ${(MAX_DELETE_PERCENTAGE * 100).toInt()}%). " +
                        "This may indicate a configuration error. Aborting cleanup."
                    )
                }

                // Calculate preserved count (total - to delete)
                preservedCount = totalCommands - preview.commandsToDelete

                // If dry run, return preview data
                if (dryRun) {
                    deletedCount = preview.commandsToDelete
                    return@measureTimeMillis
                }

                // Execute actual deletion
                val cutoffTimestamp = System.currentTimeMillis() - (gracePeriodDays * MILLIS_PER_DAY)

                deletedCount = commandRepo.deleteDeprecatedCommands(
                    olderThan = cutoffTimestamp,
                    keepUserApproved = keepUserApproved
                )

                // Verify deletion count matches preview (within tolerance)
                val countDifference = abs(deletedCount - preview.commandsToDelete)
                if (countDifference > 10) {
                    errors.add(
                        "Warning: Deleted count ($deletedCount) differs from preview (${preview.commandsToDelete}) by $countDifference commands"
                    )
                }

            } catch (e: IllegalStateException) {
                // Re-throw safety limit errors (don't swallow)
                throw e
            } catch (e: Exception) {
                errors.add("Cleanup failed: ${e.message}")
                // Allow partial results to be returned
            }
        }

        CleanupResult(
            deletedCount = deletedCount,
            preservedCount = preservedCount,
            gracePeriodDays = gracePeriodDays,
            keepUserApproved = keepUserApproved,
            errors = errors,
            durationMs = durationMs
        )
    }
}
