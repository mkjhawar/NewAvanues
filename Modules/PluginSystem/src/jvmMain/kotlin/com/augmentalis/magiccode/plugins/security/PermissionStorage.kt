package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.PluginLog
import java.util.prefs.Preferences

/**
 * JVM (Desktop) implementation of [PermissionStorage].
 *
 * Uses [java.util.prefs.Preferences] for persistent permission storage.
 * Permissions are stored as comma-separated sets under per-plugin preference keys.
 *
 * Storage layout:
 * ```
 * /com/augmentalis/magiccode/plugins/permissions/
 *   {pluginId}/
 *     permissions = "CAMERA,MICROPHONE,STORAGE"
 * ```
 *
 * Security: JVM Preferences are backed by the OS keychain (macOS),
 * registry (Windows), or filesystem (Linux ~/.java/.userPrefs/).
 * Not hardware-backed like Android Keystore, but OS-protected.
 */
actual class PermissionStorage {
    private val rootPrefs = Preferences.userRoot().node("/com/augmentalis/magiccode/plugins/permissions")

    companion object {
        private const val TAG = "PermissionStorage[JVM]"
        private const val PERMISSIONS_KEY = "permissions"
        private const val SEPARATOR = ","
    }

    actual fun savePermission(pluginId: String, permission: String) {
        val node = rootPrefs.node(sanitizeNodeName(pluginId))
        val existing = getPermissionSet(node)
        if (permission !in existing) {
            val updated = existing + permission
            node.put(PERMISSIONS_KEY, updated.joinToString(SEPARATOR))
            node.flush()
            PluginLog.d(TAG, "Saved permission '$permission' for plugin '$pluginId'")
        }
    }

    actual fun hasPermission(pluginId: String, permission: String): Boolean {
        val node = rootPrefs.node(sanitizeNodeName(pluginId))
        return permission in getPermissionSet(node)
    }

    actual fun getAllPermissions(pluginId: String): Set<String> {
        val node = rootPrefs.node(sanitizeNodeName(pluginId))
        return getPermissionSet(node)
    }

    actual fun revokePermission(pluginId: String, permission: String) {
        val node = rootPrefs.node(sanitizeNodeName(pluginId))
        val existing = getPermissionSet(node)
        if (permission in existing) {
            val updated = existing - permission
            if (updated.isEmpty()) {
                node.remove(PERMISSIONS_KEY)
            } else {
                node.put(PERMISSIONS_KEY, updated.joinToString(SEPARATOR))
            }
            node.flush()
            PluginLog.d(TAG, "Revoked permission '$permission' for plugin '$pluginId'")
        }
    }

    actual fun clearAllPermissions(pluginId: String) {
        try {
            val node = rootPrefs.node(sanitizeNodeName(pluginId))
            node.removeNode()
            rootPrefs.flush()
            PluginLog.d(TAG, "Cleared all permissions for plugin '$pluginId'")
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to clear permissions for plugin '$pluginId': ${e.message}", e)
        }
    }

    actual fun isEncrypted(): Boolean {
        // JVM Preferences use OS-level protection:
        // - macOS: ~/Library/Preferences/com.apple.java.util.prefs.plist (protected by file permissions)
        // - Windows: Registry HKEY_CURRENT_USER (protected by user session)
        // - Linux: ~/.java/.userPrefs/ (protected by file permissions)
        // Not AES-encrypted like Android's EncryptedSharedPreferences
        return false
    }

    actual fun getEncryptionStatus(): EncryptionStatus {
        return EncryptionStatus(
            isEncrypted = false,
            isHardwareBacked = false,
            migrationCompleted = true, // No migration needed on JVM
            keyAlias = "N/A", // JVM uses OS-native Preferences, no Keystore alias
            encryptionScheme = "OS_NATIVE", // OS-level file/registry protection
            migratedPermissionCount = 0
        )
    }

    actual suspend fun migrateToEncrypted(): MigrationResult {
        // JVM doesn't support hardware-backed encryption like Android Keystore.
        // Permissions are stored using OS-native Preferences which provide
        // user-session-level isolation. Full encryption would require a
        // JCE/BouncyCastle wrapper with password-based key derivation.
        return MigrationResult.AlreadyMigrated(
            migratedCount = 0,
            migrationTimestamp = System.currentTimeMillis()
        )
    }

    private fun getPermissionSet(node: Preferences): Set<String> {
        val raw = node.get(PERMISSIONS_KEY, "")
        return if (raw.isBlank()) emptySet() else raw.split(SEPARATOR).toSet()
    }

    private fun sanitizeNodeName(pluginId: String): String {
        // Preferences node names must be valid path components
        return pluginId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
