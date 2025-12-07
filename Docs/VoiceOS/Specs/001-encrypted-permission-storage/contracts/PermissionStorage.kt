/**
 * API Contract: PermissionStorage
 *
 * This file documents the public API contract for the PermissionStorage class
 * with encrypted storage support. This is NOT executable code - it's a specification
 * document written in Kotlin syntax for clarity.
 *
 * Feature: 001-encrypted-permission-storage
 * Created: 2025-10-26
 * Status: Design Phase
 *
 * IMPORTANT: This contract maintains 100% backward compatibility with the existing
 * PermissionStorage API. Encryption is transparent to callers - all existing methods
 * work identically, just with encrypted storage internally.
 */

package com.augmentalis.magiccode.plugins.security

import android.content.Context
import com.augmentalis.magiccode.plugins.core.Permission
import com.augmentalis.magiccode.plugins.core.GrantStatus

/**
 * Stores and manages plugin permission grants with hardware-backed encryption.
 *
 * ## Overview
 *
 * PermissionStorage provides secure, persistent storage for plugin permission grants
 * using AndroidX Security library's EncryptedSharedPreferences. All permission data
 * is encrypted using AES256-GCM with keys stored in Android Keystore (hardware
 * TEE/TrustZone when available).
 *
 * ## Encryption Transparency
 *
 * Encryption is completely transparent to callers. All existing methods work identically
 * to the plain-text version:
 * - `savePermission()` automatically encrypts before storing
 * - `getPermission()` automatically decrypts after reading
 * - `getAllPermissions()` returns decrypted data
 * - No API changes required for consumers
 *
 * ## Performance
 *
 * Encryption adds 3-10ms latency per operation compared to plain-text storage.
 * This is well within the <100ms command processing budget for VOS4.
 *
 * Performance optimizations:
 * - Singleton instance (created once, reused)
 * - In-memory caching of frequently accessed permissions
 * - Hardware-accelerated AES on Android 9+
 *
 * ## Migration
 *
 * On first launch after upgrade, existing plain-text permissions are automatically
 * migrated to encrypted storage. Migration is:
 * - Automatic (no user intervention)
 * - Idempotent (safe to run multiple times)
 * - Atomic (all permissions migrated or none)
 * - Non-blocking (runs on background thread)
 *
 * ## Thread Safety
 *
 * All methods are thread-safe. Concurrent access is synchronized internally.
 * Suspend functions use Dispatchers.IO for background execution.
 *
 * ## Example Usage
 *
 * ```kotlin
 * val storage = PermissionStorage(context)
 *
 * // Grant permission (encrypts automatically)
 * storage.savePermission(
 *     pluginId = "com.example.plugin",
 *     permission = Permission.ACCESSIBILITY_SERVICES,
 *     status = GrantStatus.GRANTED
 * )
 *
 * // Query permission (decrypts automatically)
 * val status = storage.getPermission(
 *     pluginId = "com.example.plugin",
 *     permission = Permission.ACCESSIBILITY_SERVICES
 * )
 * if (status == GrantStatus.GRANTED) {
 *     // Plugin has permission
 * }
 *
 * // Get all permissions for a plugin
 * val allPerms = storage.getAllPermissions("com.example.plugin")
 * allPerms.forEach { (permission, status) ->
 *     println("$permission: $status")
 * }
 * ```
 *
 * @param context Android application context (used for SharedPreferences and Keystore access)
 * @since 1.0.0
 * @see Permission
 * @see GrantStatus
 * @see EncryptionStatus
 */
class PermissionStorage(private val context: Context) {

    // ============================================================================
    // EXISTING API (Unchanged - Backward Compatible)
    // ============================================================================

    /**
     * Save a permission grant for a plugin.
     *
     * Stores the permission grant with encrypted storage. If the permission already exists
     * for this plugin, the status is updated (overwrite semantics).
     *
     * ## Encryption
     *
     * The permission is encrypted using AES256-GCM before storage:
     * - Key: `{pluginId}.{permission}` encrypted with AES256_SIV (deterministic)
     * - Value: `{status}|{timestamp}|{grantedBy}` encrypted with AES256_GCM (authenticated)
     *
     * ## Performance
     *
     * This operation takes 5-15ms including encryption and disk write.
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from multiple threads concurrently.
     *
     * ## Example
     *
     * ```kotlin
     * storage.savePermission(
     *     pluginId = "com.augmentalis.whatsapp.automation",
     *     permission = Permission.ACCESSIBILITY_SERVICES,
     *     status = GrantStatus.GRANTED
     * )
     * ```
     *
     * @param pluginId Reverse-domain plugin identifier (e.g., "com.example.plugin")
     * @param permission Type of permission to grant
     * @param status Grant status (GRANTED, DENIED, PENDING, or REVOKED)
     * @param grantedBy Optional user identifier or "system" (defaults to "user")
     * @throws IllegalArgumentException if pluginId is empty or invalid format
     * @since 1.0.0
     */
    fun savePermission(
        pluginId: String,
        permission: Permission,
        status: GrantStatus,
        grantedBy: String = "user"
    )

    /**
     * Get the current status of a permission for a plugin.
     *
     * Retrieves the permission grant status, automatically decrypting from storage.
     * Returns `null` if the permission has never been granted or denied for this plugin.
     *
     * ## Encryption
     *
     * The permission is decrypted using AES256-GCM after retrieval from storage.
     * GCM authentication tag is verified to ensure data integrity (detects tampering).
     *
     * ## Performance
     *
     * This operation takes 3-10ms including disk read and decryption.
     *
     * ## Caching
     *
     * Frequently accessed permissions are cached in memory to improve performance.
     * Cache is invalidated when permissions are updated via savePermission().
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from multiple threads concurrently.
     *
     * ## Example
     *
     * ```kotlin
     * val status = storage.getPermission(
     *     pluginId = "com.example.plugin",
     *     permission = Permission.MICROPHONE
     * )
     *
     * when (status) {
     *     GrantStatus.GRANTED -> println("Permission granted")
     *     GrantStatus.DENIED -> println("Permission denied")
     *     GrantStatus.PENDING -> println("Permission pending user decision")
     *     GrantStatus.REVOKED -> println("Permission was revoked")
     *     null -> println("Permission never requested")
     * }
     * ```
     *
     * @param pluginId Reverse-domain plugin identifier
     * @param permission Type of permission to query
     * @return Current grant status, or `null` if permission not found
     * @throws IllegalArgumentException if pluginId is empty or invalid format
     * @since 1.0.0
     */
    fun getPermission(
        pluginId: String,
        permission: Permission
    ): GrantStatus?

    /**
     * Get all permissions for a plugin.
     *
     * Retrieves all permission grants for the specified plugin, returning a map
     * of permission to status. Returns empty map if no permissions exist for this plugin.
     *
     * ## Encryption
     *
     * All permissions are decrypted before being returned. This may take longer
     * than getPermission() for plugins with many permissions.
     *
     * ## Performance
     *
     * This operation takes 10-50ms for a plugin with 10 permissions.
     * Performance scales linearly with number of permissions.
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from multiple threads concurrently.
     *
     * ## Example
     *
     * ```kotlin
     * val allPermissions = storage.getAllPermissions("com.example.plugin")
     *
     * allPermissions.forEach { (permission, status) ->
     *     println("${permission.name}: ${status.name}")
     * }
     *
     * // Check if plugin has specific permission
     * val hasAccessibility = allPermissions[Permission.ACCESSIBILITY_SERVICES] == GrantStatus.GRANTED
     * ```
     *
     * @param pluginId Reverse-domain plugin identifier
     * @return Map of permission to status for all permissions granted/denied for this plugin
     * @throws IllegalArgumentException if pluginId is empty or invalid format
     * @since 1.0.0
     */
    fun getAllPermissions(
        pluginId: String
    ): Map<Permission, GrantStatus>

    /**
     * Revoke a previously granted permission.
     *
     * Sets the permission status to REVOKED. This is semantically different from
     * DENIED - REVOKED means the permission was previously GRANTED but has been
     * taken away, while DENIED means it was never granted.
     *
     * ## Encryption
     *
     * The revocation is encrypted and stored using the same encryption as savePermission().
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from multiple threads concurrently.
     *
     * ## Example
     *
     * ```kotlin
     * // Revoke previously granted permission
     * storage.revokePermission(
     *     pluginId = "com.example.plugin",
     *     permission = Permission.ACCESSIBILITY_SERVICES
     * )
     *
     * // Status is now REVOKED
     * val status = storage.getPermission("com.example.plugin", Permission.ACCESSIBILITY_SERVICES)
     * assert(status == GrantStatus.REVOKED)
     * ```
     *
     * @param pluginId Reverse-domain plugin identifier
     * @param permission Type of permission to revoke
     * @throws IllegalArgumentException if pluginId is empty or invalid format
     * @throws IllegalStateException if permission was never granted (cannot revoke DENIED/PENDING)
     * @since 1.0.0
     */
    fun revokePermission(
        pluginId: String,
        permission: Permission
    )

    /**
     * Clear all permissions for a plugin.
     *
     * Removes all permission grants for the specified plugin. This is typically used
     * when uninstalling a plugin.
     *
     * ## Encryption
     *
     * All encrypted entries for this plugin are deleted from storage.
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from multiple threads concurrently.
     *
     * ## Example
     *
     * ```kotlin
     * // Uninstalling plugin - clear all permissions
     * storage.clearAllPermissions("com.example.plugin")
     *
     * // Verify all permissions cleared
     * val remaining = storage.getAllPermissions("com.example.plugin")
     * assert(remaining.isEmpty())
     * ```
     *
     * @param pluginId Reverse-domain plugin identifier
     * @throws IllegalArgumentException if pluginId is empty or invalid format
     * @since 1.0.0
     */
    fun clearAllPermissions(
        pluginId: String
    )

    // ============================================================================
    // NEW API (Encryption Management)
    // ============================================================================

    /**
     * Migrate existing plain-text permissions to encrypted storage.
     *
     * This method is called automatically on first app launch after upgrading to
     * the encrypted storage version. It:
     * 1. Checks if migration already completed (idempotency)
     * 2. Reads all plain-text permissions from old storage
     * 3. Writes them to encrypted storage
     * 4. Deletes the old plain-text file
     * 5. Saves migration state to prevent re-migration
     *
     * ## Idempotency
     *
     * Safe to call multiple times. If migration already completed, returns immediately
     * with AlreadyMigrated result.
     *
     * ## Atomicity
     *
     * Migration is atomic - either all permissions are migrated successfully, or
     * none are (rollback on failure). The old plain-text file is only deleted after
     * successful migration of all permissions.
     *
     * ## Performance
     *
     * Migration takes 50-100ms for 10 permissions (one-time cost on first launch).
     * Runs on background thread (Dispatchers.IO), does not block UI.
     *
     * ## Error Handling
     *
     * If migration fails:
     * - Old plain-text file is preserved
     * - Migration state remains "not completed"
     * - Returns MigrationResult.Failure with error details
     * - User can retry migration manually
     *
     * ## Thread Safety
     *
     * Thread-safe. Uses mutex to prevent concurrent migration attempts.
     *
     * ## Example
     *
     * ```kotlin
     * // Automatic migration on app launch
     * lifecycleScope.launch {
     *     when (val result = storage.migrateToEncrypted()) {
     *         is MigrationResult.Success -> {
     *             Log.i(TAG, "Migrated ${result.migratedCount} permissions")
     *         }
     *         is MigrationResult.Failure -> {
     *             Log.e(TAG, "Migration failed: ${result.reason} (${result.failedCount} failed)")
     *         }
     *         is MigrationResult.AlreadyMigrated -> {
     *             Log.d(TAG, "Migration already completed (${result.migratedCount} permissions)")
     *         }
     *     }
     * }
     * ```
     *
     * @return MigrationResult indicating success/failure/already-migrated
     * @since 1.1.0
     */
    suspend fun migrateToEncrypted(): MigrationResult

    /**
     * Check if permission storage is using encryption.
     *
     * Returns true if EncryptedSharedPreferences is active and permissions are
     * encrypted, false if using plain-text storage (pre-migration or encryption disabled).
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from any thread.
     *
     * ## Example
     *
     * ```kotlin
     * if (storage.isEncrypted()) {
     *     Log.i(TAG, "Permissions are encrypted")
     * } else {
     *     Log.w(TAG, "Permissions are NOT encrypted - migration needed")
     * }
     * ```
     *
     * @return true if encryption is active, false otherwise
     * @since 1.1.0
     */
    fun isEncrypted(): Boolean

    /**
     * Get detailed encryption status information.
     *
     * Returns comprehensive information about the encryption state including:
     * - Whether encryption is active
     * - Whether keys are hardware-backed (TEE/TrustZone)
     * - Whether migration has completed
     * - Key alias used for encryption
     *
     * Useful for diagnostics, logging, and security audits.
     *
     * ## Thread Safety
     *
     * Thread-safe. Can be called from any thread.
     *
     * ## Example
     *
     * ```kotlin
     * val status = storage.getEncryptionStatus()
     *
     * Log.i(TAG, """
     *     Encryption Status:
     *     - Encrypted: ${status.isEncrypted}
     *     - Hardware-Backed: ${status.isHardwareBacked}
     *     - Migration Complete: ${status.migrationCompleted}
     *     - Key Alias: ${status.keyAlias}
     * """.trimIndent())
     *
     * if (!status.isHardwareBacked) {
     *     Log.w(TAG, "WARNING: Encryption is NOT hardware-backed (using software keystore)")
     * }
     * ```
     *
     * @return EncryptionStatus with detailed encryption information
     * @since 1.1.0
     */
    fun getEncryptionStatus(): EncryptionStatus
}

// ============================================================================
// Supporting Types
// ============================================================================

/**
 * Result of permission migration operation.
 *
 * Sealed class representing the three possible outcomes of migration:
 * - Success: All permissions migrated successfully
 * - Failure: Migration failed (with error details)
 * - AlreadyMigrated: Migration already completed previously (idempotency)
 *
 * @since 1.1.0
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
     * preserved and can be retried.
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

/**
 * Detailed encryption status information.
 *
 * Contains diagnostic information about the current state of permission
 * encryption, useful for logging, security audits, and troubleshooting.
 *
 * @property isEncrypted Whether permissions are currently encrypted
 * @property isHardwareBacked Whether encryption keys are stored in hardware (TEE/TrustZone/StrongBox)
 * @property migrationCompleted Whether migration from plain-text to encrypted has completed
 * @property keyAlias Alias of the master key in Android Keystore
 * @property encryptionScheme Description of encryption scheme (e.g., "AES256_GCM")
 * @property migratedPermissionCount Number of permissions migrated (0 if not migrated)
 * @since 1.1.0
 */
data class EncryptionStatus(
    val isEncrypted: Boolean,
    val isHardwareBacked: Boolean,
    val migrationCompleted: Boolean,
    val keyAlias: String,
    val encryptionScheme: String = "AES256_GCM",
    val migratedPermissionCount: Int = 0
)

// ============================================================================
// Error Handling
// ============================================================================

/**
 * Exception thrown when encryption operation fails.
 *
 * Wraps underlying encryption failures (keystore unavailable, key invalidated,
 * GCM authentication failure, etc.) with context-specific error messages.
 *
 * @param message Human-readable error description
 * @param cause Underlying exception that caused the encryption failure
 * @since 1.1.0
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when migration operation fails.
 *
 * Indicates that migration from plain-text to encrypted storage failed and
 * should be retried or investigated.
 *
 * @param message Human-readable error description
 * @param failedCount Number of permissions that failed to migrate
 * @param cause Underlying exception that caused the migration failure
 * @since 1.1.0
 */
class MigrationException(
    message: String,
    val failedCount: Int,
    cause: Throwable? = null
) : Exception(message, cause)

// ============================================================================
// API Contract Summary
// ============================================================================

/**
 * ## API Contract Summary
 *
 * ### Existing Methods (Unchanged):
 * - `savePermission(pluginId, permission, status, grantedBy)` - Save/update permission (encrypted)
 * - `getPermission(pluginId, permission)` - Query permission status (decrypted)
 * - `getAllPermissions(pluginId)` - Get all permissions for plugin (decrypted)
 * - `revokePermission(pluginId, permission)` - Revoke permission (encrypted)
 * - `clearAllPermissions(pluginId)` - Delete all permissions for plugin
 *
 * ### New Methods:
 * - `suspend migrateToEncrypted()` - Migrate plain-text to encrypted (one-time)
 * - `isEncrypted()` - Check if encryption is active
 * - `getEncryptionStatus()` - Get detailed encryption diagnostics
 *
 * ### Backward Compatibility:
 * - 100% API compatible with plain-text version
 * - Encryption is transparent to callers
 * - Automatic migration preserves existing permissions
 * - Performance overhead: <5ms per operation (within budget)
 *
 * ### Security Guarantees:
 * - AES256-GCM encryption (NIST-approved)
 * - Hardware-backed keys (TEE/TrustZone/StrongBox on API 28+)
 * - Tamper detection (GCM authentication tag)
 * - Keys excluded from backups (device-bound)
 * - Unreadable via ADB without device unlock
 *
 * ### Thread Safety:
 * - All methods thread-safe
 * - Suspend functions use Dispatchers.IO
 * - Internal synchronization with mutex
 * - Safe for concurrent access
 *
 * ### Error Handling:
 * - Throws IllegalArgumentException for invalid inputs
 * - Throws EncryptionException for encryption failures
 * - Throws MigrationException for migration failures
 * - Logs all errors with PluginLog
 *
 * ### Performance:
 * - First creation: 100-300ms (one-time)
 * - savePermission: 5-15ms (includes encryption + disk write)
 * - getPermission: 3-10ms (includes disk read + decryption)
 * - getAllPermissions: 10-50ms for 10 permissions
 * - migrateToEncrypted: 50-100ms for 10 permissions (one-time)
 *
 * ### Testing:
 * - Unit tests: 15+ tests (>90% coverage)
 * - Integration tests: 5+ tests
 * - Performance benchmarks: 3 tests
 * - Manual acceptance tests: 4 tests
 */
