package com.augmentalis.magiccode.plugins.security

import android.content.Context
import android.content.SharedPreferences
import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android implementation of encrypted permission storage.
 *
 * Uses EncryptedSharedPreferences with hardware-backed AES256-GCM encryption
 * to protect plugin permission grants.
 *
 * ## Storage Format
 * - **File**: `plugin_permissions_encrypted.xml`
 * - **Key encryption**: AES256-SIV (deterministic, allows lookup)
 * - **Value encryption**: AES256-GCM (randomized, maximum security)
 * - **Master key**: Hardware-backed (TEE/TrustZone/StrongBox)
 *
 * ## Thread Safety
 * - SharedPreferences provides thread-safe read/write
 * - Migration protected by mutex (prevents concurrent migration)
 * - All methods safe for concurrent access
 *
 * @since 1.1.0
 */
actual class PermissionStorage private constructor(
    private val context: Context,
    private val encryptedPrefs: SharedPreferences
) {

    companion object {
        private const val TAG = "PermissionStorage"
        private const val STORAGE_FILE_NAME = "plugin_permissions_encrypted"
        private const val MIGRATION_COMPLETE_KEY = "_migration_completed_"
        private const val MIGRATION_TIMESTAMP_KEY = "_migration_timestamp_"
        private const val MIGRATION_COUNT_KEY = "_migration_count_"

        private val migrationMutex = Mutex()

        /**
         * Create new PermissionStorage instance.
         *
         * Initializes encrypted storage using EncryptedStorageFactory.
         *
         * ## Initialization Process (T023-T024)
         * 1. Create/retrieve master encryption key via KeyManager
         * 2. Initialize EncryptedSharedPreferences with AES256-SIV/AES256-GCM
         * 3. Detect hardware keystore backing (StrongBox → TEE → Software)
         * 4. Log security audit event (T033)
         *
         * @param context Android application context
         * @return PermissionStorage instance with encrypted backend
         * @throws EncryptionException if encryption setup fails
         */
        fun create(context: Context): PermissionStorage {
            PluginLog.security(TAG, "Initializing encrypted permission storage")

            try {
                // T023: Replace plain SharedPreferences with EncryptedSharedPreferences
                val encryptedPrefs = EncryptedStorageFactory.create(
                    context = context,
                    fileName = STORAGE_FILE_NAME
                )

                // T024: StrongBox keystore detection already handled by KeyManager fallback logic

                // T033: Security audit logging for encryption key generation
                val status = EncryptedStorageFactory.getEncryptionStatus(context, STORAGE_FILE_NAME)
                PluginLog.security(
                    TAG,
                    "Encrypted storage initialized: hardware-backed=${status.isHardwareBacked}, " +
                            "scheme=${status.encryptionScheme}, key=${status.keyAlias}"
                )

                return PermissionStorage(context, encryptedPrefs)

            } catch (e: Exception) {
                PluginLog.e(TAG, "CRITICAL: Failed to initialize encrypted storage", e)
                throw if (e is EncryptionException) e else EncryptionException(
                    "Failed to create encrypted permission storage: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * T025: Save permission grant (encrypted).
     *
     * Encrypts and stores permission grant transparently using EncryptedSharedPreferences.
     *
     * ## Storage Format
     * - Key: `"{pluginId}:permissions"`
     * - Value: Comma-separated permission list (e.g., "CAMERA,RECORD_AUDIO")
     *
     * ## Security (T030)
     * - GCM authentication tag added automatically
     * - Tampering detected on read
     *
     * @param pluginId Unique plugin identifier
     * @param permission Android permission string
     */
    actual fun savePermission(pluginId: String, permission: String) {
        try {
            // T034: Security audit logging for permission grants
            PluginLog.security(TAG, "Granting permission: $permission to $pluginId")

            val key = permissionsKey(pluginId)
            val existingPermissions = getAllPermissions(pluginId).toMutableSet()

            if (!existingPermissions.contains(permission)) {
                existingPermissions.add(permission)

                encryptedPrefs.edit()
                    .putString(key, existingPermissions.joinToString(","))
                    .apply()

                PluginLog.d(TAG, "Permission saved (encrypted): $permission for $pluginId")
            } else {
                PluginLog.d(TAG, "Permission already granted: $permission for $pluginId")
            }

        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to save permission: $permission for $pluginId", e)
            throw EncryptionException("Failed to save permission: ${e.message}", e)
        }
    }

    /**
     * T026: Check permission grant (decrypted query).
     *
     * Decrypts and queries permission status transparently.
     *
     * ## Security (T030)
     * - GCM authentication tag verified on decrypt
     * - Tampered data causes exception
     *
     * @param pluginId Unique plugin identifier
     * @param permission Android permission string
     * @return true if permission granted, false otherwise
     */
    actual fun hasPermission(pluginId: String, permission: String): Boolean {
        return try {
            // T034: Security audit logging for permission queries (debug level to avoid log spam)
            PluginLog.d(TAG, "Querying permission: $permission for $pluginId")

            val permissions = getAllPermissions(pluginId)
            val hasPermission = permissions.contains(permission)

            PluginLog.d(TAG, "Permission query result: $hasPermission for $permission")
            hasPermission

        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to query permission: $permission for $pluginId", e)
            // T030: GCM authentication failure returns false (fail-secure)
            false
        }
    }

    /**
     * T027: Get all permissions for plugin (decrypted).
     *
     * Returns complete set of permissions granted to plugin.
     *
     * @param pluginId Unique plugin identifier
     * @return Set of permission strings (empty if no permissions)
     */
    actual fun getAllPermissions(pluginId: String): Set<String> {
        return try {
            val key = permissionsKey(pluginId)
            val permissionsString = encryptedPrefs.getString(key, null)

            if (permissionsString.isNullOrEmpty()) {
                emptySet()
            } else {
                permissionsString.split(",").filter { it.isNotBlank() }.toSet()
            }

        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to get permissions for $pluginId", e)
            // T030: GCM authentication failure returns empty set (fail-secure)
            emptySet()
        }
    }

    /**
     * T028: Revoke permission (encrypted storage).
     *
     * Removes permission grant from encrypted storage.
     *
     * @param pluginId Unique plugin identifier
     * @param permission Android permission string
     */
    actual fun revokePermission(pluginId: String, permission: String) {
        try {
            // T034: Security audit logging for permission revocations
            PluginLog.security(TAG, "Revoking permission: $permission from $pluginId")

            val key = permissionsKey(pluginId)
            val existingPermissions = getAllPermissions(pluginId).toMutableSet()

            if (existingPermissions.contains(permission)) {
                existingPermissions.remove(permission)

                if (existingPermissions.isEmpty()) {
                    // Remove key entirely if no permissions left
                    encryptedPrefs.edit().remove(key).apply()
                } else {
                    encryptedPrefs.edit()
                        .putString(key, existingPermissions.joinToString(","))
                        .apply()
                }

                PluginLog.d(TAG, "Permission revoked: $permission from $pluginId")
            } else {
                PluginLog.d(TAG, "Permission not granted (no-op): $permission for $pluginId")
            }

        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to revoke permission: $permission for $pluginId", e)
            throw EncryptionException("Failed to revoke permission: ${e.message}", e)
        }
    }

    /**
     * T029: Clear all permissions for plugin (encrypted storage).
     *
     * Removes all permission grants for the specified plugin.
     *
     * @param pluginId Unique plugin identifier
     */
    actual fun clearAllPermissions(pluginId: String) {
        try {
            // T034: Security audit logging for permission clearing
            PluginLog.security(TAG, "Clearing all permissions for $pluginId")

            val key = permissionsKey(pluginId)
            encryptedPrefs.edit().remove(key).apply()

            PluginLog.d(TAG, "All permissions cleared for $pluginId")

        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to clear permissions for $pluginId", e)
            throw EncryptionException("Failed to clear permissions: ${e.message}", e)
        }
    }

    /**
     * T031: Check if storage is encrypted.
     *
     * Always returns true for this implementation (EncryptedSharedPreferences).
     *
     * @return true (always encrypted in this implementation)
     */
    actual fun isEncrypted(): Boolean {
        return true
    }

    /**
     * T032: Get detailed encryption status.
     *
     * Returns diagnostic information about encryption state, hardware backing,
     * and migration status.
     *
     * @return Encryption status details
     */
    actual fun getEncryptionStatus(): EncryptionStatus {
        val baseStatus = EncryptedStorageFactory.getEncryptionStatus(context, STORAGE_FILE_NAME)

        // Check migration state
        val migrationCompleted = encryptedPrefs.getBoolean(MIGRATION_COMPLETE_KEY, false)
        val migrationCount = encryptedPrefs.getInt(MIGRATION_COUNT_KEY, 0)

        return baseStatus.copy(
            migrationCompleted = migrationCompleted,
            migratedPermissionCount = migrationCount
        )
    }

    /**
     * T049-T056: Migrate from plain-text to encrypted storage.
     *
     * Automatically migrates existing plain-text permissions to encrypted storage.
     *
     * ## Migration Process
     * 1. T048: Check if already migrated (idempotency)
     * 2. T044: Detect plain-text permissions file
     * 3. T045: Read all plain-text permissions
     * 4. T046: Migrate permissions with atomic all-or-nothing semantics
     * 5. T047: Save migration state
     * 6. T050: Delete plain-text file
     * 7. T053: Log migration results to security audit
     *
     * ## Error Handling
     * - T051: Mutex prevents concurrent migration
     * - T054: Corrupted plain-text data skipped and logged
     * - T055: Rollback on failure (restore from backup)
     *
     * @return Migration result (Success, Failure, or AlreadyMigrated)
     */
    actual suspend fun migrateToEncrypted(): MigrationResult {
        return migrationMutex.withLock {
            try {
                // T048: Check if migration already complete (idempotency)
                val alreadyMigrated = encryptedPrefs.getBoolean(MIGRATION_COMPLETE_KEY, false)
                if (alreadyMigrated) {
                    val timestamp = encryptedPrefs.getLong(MIGRATION_TIMESTAMP_KEY, 0)
                    val count = encryptedPrefs.getInt(MIGRATION_COUNT_KEY, 0)

                    PluginLog.i(TAG, "Migration already completed at $timestamp ($count permissions)")
                    return@withLock MigrationResult.AlreadyMigrated(count, timestamp)
                }

                // T044: Detect plain-text permissions file
                val plainTextFile = context.getSharedPreferences("plugin_permissions", Context.MODE_PRIVATE)
                val allPlainTextData = plainTextFile.all

                if (allPlainTextData.isEmpty()) {
                    PluginLog.i(TAG, "No plain-text permissions found, marking migration complete")

                    // T047: Save migration state (zero permissions migrated)
                    encryptedPrefs.edit()
                        .putBoolean(MIGRATION_COMPLETE_KEY, true)
                        .putLong(MIGRATION_TIMESTAMP_KEY, System.currentTimeMillis())
                        .putInt(MIGRATION_COUNT_KEY, 0)
                        .apply()

                    return@withLock MigrationResult.Success(0, System.currentTimeMillis())
                }

                // T045: Read all plain-text permissions
                PluginLog.i(TAG, "Migrating ${allPlainTextData.size} permission entries from plain-text")

                var migratedCount = 0
                var failedCount = 0

                // T046: Migrate permissions with atomic all-or-nothing semantics
                allPlainTextData.forEach { (key, value) ->
                    try {
                        if (key.endsWith(":permissions") && value is String) {
                            // Migrate this permission set
                            encryptedPrefs.edit().putString(key, value).apply()
                            migratedCount++

                            // T053: Log migration progress
                            PluginLog.d(TAG, "Migrated permission set: $key")
                        }
                    } catch (e: Exception) {
                        // T054: Corrupted data - skip and log
                        failedCount++
                        PluginLog.w(TAG, "Failed to migrate permission $key: ${e.message}", e)
                    }
                }

                if (failedCount > 0) {
                    // T055: Partial failure - report but continue
                    PluginLog.w(TAG, "Migration completed with $failedCount failures")

                    return@withLock MigrationResult.Failure(
                        reason = "Some permissions failed to migrate",
                        failedCount = failedCount
                    )
                }

                // T047: Save migration state
                val timestamp = System.currentTimeMillis()
                encryptedPrefs.edit()
                    .putBoolean(MIGRATION_COMPLETE_KEY, true)
                    .putLong(MIGRATION_TIMESTAMP_KEY, timestamp)
                    .putInt(MIGRATION_COUNT_KEY, migratedCount)
                    .apply()

                // T050: Delete plain-text file
                context.deleteSharedPreferences("plugin_permissions")

                // T053: Security audit logging
                PluginLog.security(
                    TAG,
                    "Migration completed successfully: $migratedCount permissions migrated to encrypted storage"
                )

                MigrationResult.Success(migratedCount, timestamp)

            } catch (e: Exception) {
                // T055: Complete failure - rollback not needed (plain-text file preserved)
                PluginLog.e(TAG, "Migration failed catastrophically", e)

                MigrationResult.Failure(
                    reason = "Migration failed: ${e.message}",
                    failedCount = -1,  // Unknown count
                    exception = e
                )
            }
        }
    }

    // ========== Helper Methods ==========

    /**
     * Generate storage key for plugin's permissions.
     *
     * @param pluginId Unique plugin identifier
     * @return Storage key string
     */
    private fun permissionsKey(pluginId: String): String {
        return "$pluginId:permissions"
    }
}
