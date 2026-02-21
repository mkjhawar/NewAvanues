package com.augmentalis.magiccode.plugins.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of PermissionStorage using NSUserDefaults.
 *
 * Stores plugin permission grants as JSON-encoded sets in NSUserDefaults.
 * NSUserDefaults does not provide hardware-backed encryption; for production
 * use Keychain-backed storage for sensitive permission data.
 *
 * ## Storage Layout
 * Each plugin's permission set is stored under the key:
 *   "plugin_perm_<pluginId>"  ->  JSON array of permission strings
 *
 * ## Thread Safety
 * NSUserDefaults synchronizes internally; no external locking required.
 *
 * @since 1.1.0
 */
actual class PermissionStorage private constructor() {

    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_PREFIX = "plugin_perm_"
        private const val MIGRATION_TIMESTAMP_KEY = "_ios_migration_timestamp_"
        private const val MIGRATION_COUNT_KEY = "_ios_migration_count_"

        /**
         * Create a new PermissionStorage instance for iOS.
         * Uses NSUserDefaults which requires no platform context parameter.
         */
        fun create(): PermissionStorage = PermissionStorage()
    }

    // ─── Key helpers ──────────────────────────────────────────────────────────

    private fun prefKey(pluginId: String): String = "$KEY_PREFIX$pluginId"

    private fun loadPermissionSet(pluginId: String): MutableSet<String> {
        val stored = userDefaults.stringForKey(prefKey(pluginId)) ?: return mutableSetOf()
        return try {
            json.decodeFromString<Set<String>>(stored).toMutableSet()
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to decode permissions for $pluginId",
                e
            )
            mutableSetOf()
        }
    }

    private fun savePermissionSet(pluginId: String, permissions: Set<String>) {
        try {
            val encoded = json.encodeToString(permissions)
            userDefaults.setObject(encoded, forKey = prefKey(pluginId))
            userDefaults.synchronize()
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to encode permissions for $pluginId",
                e
            )
        }
    }

    // ─── expect interface implementation ──────────────────────────────────────

    /**
     * Save a permission grant for [pluginId]. Idempotent — no-op if already granted.
     */
    actual fun savePermission(pluginId: String, permission: String) {
        val current = loadPermissionSet(pluginId)
        if (current.add(permission)) {
            savePermissionSet(pluginId, current)
        }
    }

    /**
     * Returns true if [pluginId] has been granted [permission].
     */
    actual fun hasPermission(pluginId: String, permission: String): Boolean {
        return loadPermissionSet(pluginId).contains(permission)
    }

    /**
     * Returns all permissions granted to [pluginId]. Empty set if none.
     */
    actual fun getAllPermissions(pluginId: String): Set<String> {
        return loadPermissionSet(pluginId)
    }

    /**
     * Revoke [permission] from [pluginId]. Idempotent — no-op if not granted.
     */
    actual fun revokePermission(pluginId: String, permission: String) {
        val current = loadPermissionSet(pluginId)
        if (current.remove(permission)) {
            savePermissionSet(pluginId, current)
        }
    }

    /**
     * Clear all permission grants for [pluginId].
     */
    actual fun clearAllPermissions(pluginId: String) {
        userDefaults.removeObjectForKey(prefKey(pluginId))
        userDefaults.synchronize()
    }

    /**
     * Returns false on iOS — NSUserDefaults does not encrypt at rest by default.
     * Use Keychain-backed storage for sensitive data in production.
     */
    actual fun isEncrypted(): Boolean = false

    /**
     * Returns the iOS-specific encryption status.
     *
     * NSUserDefaults is not hardware-backed or encrypted. Reports this honestly
     * so callers can make informed security decisions.
     */
    actual fun getEncryptionStatus(): EncryptionStatus = EncryptionStatus(
        isEncrypted = false,
        isHardwareBacked = false,
        migrationCompleted = userDefaults.objectForKey(MIGRATION_TIMESTAMP_KEY) != null,
        keyAlias = "none",
        encryptionScheme = "none",
        migratedPermissionCount = userDefaults.integerForKey(MIGRATION_COUNT_KEY).toInt()
    )

    /**
     * No-op on iOS — there is no plain-text legacy format to migrate from.
     *
     * iOS storage was introduced with NSUserDefaults from the start.
     * Reports [MigrationResult.AlreadyMigrated] with count 0 if called.
     */
    actual suspend fun migrateToEncrypted(): MigrationResult = withContext(Dispatchers.Default) {
        val existingTimestamp = userDefaults.doubleForKey(MIGRATION_TIMESTAMP_KEY)
        if (existingTimestamp > 0.0) {
            MigrationResult.AlreadyMigrated(
                migratedCount = userDefaults.integerForKey(MIGRATION_COUNT_KEY).toInt(),
                migrationTimestamp = existingTimestamp.toLong()
            )
        } else {
            // Mark as "migrated" so subsequent calls return AlreadyMigrated
            val nowMs = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
            userDefaults.setDouble(nowMs.toDouble(), forKey = MIGRATION_TIMESTAMP_KEY)
            userDefaults.setInteger(0, forKey = MIGRATION_COUNT_KEY)
            userDefaults.synchronize()
            MigrationResult.AlreadyMigrated(migratedCount = 0, migrationTimestamp = nowMs)
        }
    }
}
