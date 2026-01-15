package com.augmentalis.avacode.plugins

/**
 * Result of permission migration operation from plain-text to encrypted storage.
 *
 * Sealed class representing the three possible outcomes of migration:
 * - [Success]: All permissions migrated successfully
 * - [Failure]: Migration failed (with error details)
 * - [AlreadyMigrated]: Migration already completed previously (idempotency)
 *
 * ## Usage Example
 * ```kotlin
 * when (val result = permissionStorage.migrateToEncrypted()) {
 *     is MigrationResult.Success -> {
 *         println("Migrated ${result.migratedCount} permissions")
 *     }
 *     is MigrationResult.Failure -> {
 *         println("Migration failed: ${result.reason}")
 *     }
 *     is MigrationResult.AlreadyMigrated -> {
 *         println("Migration already complete")
 *     }
 * }
 * ```
 *
 * @since 1.1.0
 * @see PermissionStorage.migrateToEncrypted
 */
sealed class MigrationResult {
    /**
     * Migration completed successfully.
     *
     * All plain-text permissions were migrated to encrypted storage and the
     * old plain-text file was deleted.
     *
     * @property migratedCount Number of permissions successfully migrated
     * @property migrationTimestamp Unix timestamp (ms) when migration completed
     */
    data class Success(
        val migratedCount: Int,
        val migrationTimestamp: Long = System.currentTimeMillis()
    ) : MigrationResult()

    /**
     * Migration failed.
     *
     * Some or all permissions failed to migrate. The old plain-text file is
     * preserved and migration can be retried.
     *
     * @property reason Human-readable error description
     * @property failedCount Number of permissions that failed to migrate
     * @property exception Optional exception that caused the failure
     */
    data class Failure(
        val reason: String,
        val failedCount: Int,
        val exception: Throwable? = null
    ) : MigrationResult()

    /**
     * Migration already completed previously.
     *
     * Migration was attempted but the migration state indicates it already
     * completed successfully in a previous app launch (idempotency).
     *
     * @property migratedCount Number of permissions that were previously migrated
     * @property migrationTimestamp Unix timestamp (ms) when original migration completed
     */
    data class AlreadyMigrated(
        val migratedCount: Int,
        val migrationTimestamp: Long
    ) : MigrationResult()
}
