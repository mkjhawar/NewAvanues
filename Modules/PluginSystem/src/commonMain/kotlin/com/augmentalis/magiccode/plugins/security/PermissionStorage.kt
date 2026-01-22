package com.augmentalis.magiccode.plugins.security

/**
 * Encrypted storage for plugin permissions.
 *
 * Provides hardware-backed AES256-GCM encryption for all permission grants using
 * Android Keystore and EncryptedSharedPreferences.
 *
 * ## Security Features
 * - **Hardware-backed encryption**: Keys stored in TEE/TrustZone/StrongBox
 * - **AES256-GCM**: Authenticated encryption with tamper detection
 * - **Key derivation**: Unique encryption key per device (cannot be extracted)
 * - **Backup exclusion**: Encrypted data excluded from Android backups
 *
 * ## Usage
 * ```kotlin
 * // Create storage instance
 * val storage = PermissionStorage.create(context)
 *
 * // Grant permission (automatically encrypted)
 * storage.savePermission("com.example.plugin", "android.permission.CAMERA")
 *
 * // Query permission (automatically decrypted)
 * val hasPermission = storage.hasPermission("com.example.plugin", "android.permission.CAMERA")
 *
 * // Get all permissions for plugin
 * val permissions = storage.getAllPermissions("com.example.plugin")
 *
 * // Revoke permission
 * storage.revokePermission("com.example.plugin", "android.permission.CAMERA")
 *
 * // Clear all permissions for plugin
 * storage.clearAllPermissions("com.example.plugin")
 *
 * // Check encryption status
 * val status = storage.getEncryptionStatus()
 * println("Hardware-backed: ${status.isHardwareBacked}")
 * ```
 *
 * ## Thread Safety
 * - All methods are thread-safe (EncryptedSharedPreferences handles synchronization)
 * - Concurrent access from multiple threads is supported
 * - No external synchronization required
 *
 * ## Performance
 * - Save operation: <5ms (P95)
 * - Query operation: <5ms (P95)
 * - Bulk operations: Linear scaling
 *
 * @since 1.1.0
 */
expect class PermissionStorage {
    /**
     * Save permission grant for a plugin (encrypted).
     *
     * Encrypts and stores the permission grant using hardware-backed AES256-GCM.
     * If permission already granted, this is a no-op (idempotent).
     *
     * ## Security
     * - Permission data encrypted before storage
     * - Hardware-backed encryption key (when available)
     * - GCM authentication tag prevents tampering
     *
     * ## Performance
     * - Average: 1-2ms
     * - P95: <5ms (FR-002)
     *
     * @param pluginId Unique plugin identifier
     * @param permission Android permission string (e.g., "android.permission.CAMERA")
     * @throws EncryptionException if encryption fails
     */
    fun savePermission(pluginId: String, permission: String)

    /**
     * Check if plugin has permission (decrypted query).
     *
     * Decrypts and queries the permission grant status.
     *
     * ## Security
     * - GCM authentication verifies data integrity
     * - Tampered data causes query to fail
     *
     * ## Performance
     * - Average: 1-2ms
     * - P95: <5ms (FR-002)
     *
     * @param pluginId Unique plugin identifier
     * @param permission Android permission string
     * @return true if permission granted, false otherwise
     * @throws EncryptionException if decryption fails
     */
    fun hasPermission(pluginId: String, permission: String): Boolean

    /**
     * Get all permissions for a plugin (decrypted).
     *
     * Returns the complete set of permissions granted to a plugin.
     *
     * ## Performance
     * - 100 permissions: <500ms (FR-002)
     *
     * @param pluginId Unique plugin identifier
     * @return Set of permission strings (empty if no permissions)
     * @throws EncryptionException if decryption fails
     */
    fun getAllPermissions(pluginId: String): Set<String>

    /**
     * Revoke permission from a plugin.
     *
     * Removes the permission grant from encrypted storage. If permission not granted,
     * this is a no-op (idempotent).
     *
     * @param pluginId Unique plugin identifier
     * @param permission Android permission string
     */
    fun revokePermission(pluginId: String, permission: String)

    /**
     * Clear all permissions for a plugin.
     *
     * Removes all permission grants for the specified plugin.
     *
     * @param pluginId Unique plugin identifier
     */
    fun clearAllPermissions(pluginId: String)

    /**
     * Check if permission storage is encrypted.
     *
     * Returns true if storage is using encrypted backend, false for plain-text.
     *
     * @return true if encrypted, false if plain-text
     */
    fun isEncrypted(): Boolean

    /**
     * Get detailed encryption status.
     *
     * Returns diagnostic information about encryption state, hardware backing,
     * and migration status.
     *
     * @return Encryption status details
     */
    fun getEncryptionStatus(): EncryptionStatus

    /**
     * Migrate from plain-text to encrypted storage.
     *
     * Automatically migrates existing plain-text permissions to encrypted storage.
     * This method is idempotent - safe to call multiple times.
     *
     * ## Migration Process
     * 1. Check if already migrated (skip if yes)
     * 2. Read all plain-text permissions
     * 3. Encrypt and save to new storage
     * 4. Verify migration success
     * 5. Delete plain-text file
     * 6. Mark migration complete
     *
     * ## Error Handling
     * - Corrupted plain-text data: Skip and log
     * - Encryption failure: Rollback and retry
     * - Partial migration: Resume from last successful permission
     *
     * @return Migration result (Success, Failure, or AlreadyMigrated)
     */
    suspend fun migrateToEncrypted(): MigrationResult

}
