package com.augmentalis.avacode.plugins.security

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*

/**
 * iOS implementation of PermissionStorage.
 *
 * Uses UserDefaults for storing permission states.
 * Each plugin's permissions are stored as a JSON string.
 *
 * TODO: For production, consider:
 * - CoreData for better querying and relationships
 * - Keychain for sensitive permissions
 * - iCloud sync support
 * - Migration support for schema changes
 */
class IosPermissionStorage : PermissionStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        private const val KEY_PREFIX = "plugin_permission_"
    }

    /**
     * Save permission state for a plugin.
     */
    override suspend fun save(state: PluginPermissionState) {
        try {
            val key = "$KEY_PREFIX${state.pluginId}"
            val jsonString = json.encodeToString(state)

            userDefaults.setObject(jsonString, forKey = key)
            userDefaults.synchronize()
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to save permission state for ${state.pluginId}",
                e
            )
        }
    }

    /**
     * Load permission state for a plugin.
     */
    override suspend fun load(pluginId: String): PluginPermissionState? {
        try {
            val key = "$KEY_PREFIX$pluginId"
            val jsonString = userDefaults.stringForKey(key) ?: return null

            return json.decodeFromString<PluginPermissionState>(jsonString)
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to load permission state for $pluginId",
                e
            )
            return null
        }
    }

    /**
     * Delete permission state for a plugin.
     */
    override suspend fun delete(pluginId: String) {
        try {
            val key = "$KEY_PREFIX$pluginId"
            userDefaults.removeObjectForKey(key)
            userDefaults.synchronize()
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to delete permission state for $pluginId",
                e
            )
        }
    }

    /**
     * Load all permission states.
     *
     * NOTE: UserDefaults doesn't provide easy enumeration of keys with a prefix.
     * We would need to store a list of all plugin IDs separately.
     *
     * TODO: Consider using CoreData or a plist file for better enumeration support.
     */
    override suspend fun loadAll(): Map<String, PluginPermissionState> {
        val result = mutableMapOf<String, PluginPermissionState>()

        try {
            // Get all keys from UserDefaults
            val allKeys = userDefaults.dictionaryRepresentation().keys

            allKeys.forEach { key ->
                if (key is String && key.startsWith(KEY_PREFIX)) {
                    val jsonString = userDefaults.stringForKey(key)
                    if (jsonString != null) {
                        try {
                            val state = json.decodeFromString<PluginPermissionState>(jsonString)
                            result[state.pluginId] = state
                        } catch (e: Exception) {
                            // Skip invalid entries
                            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                                "PermissionStorage",
                                "Failed to load permission state from key $key",
                                e
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to load all permission states",
                e
            )
        }

        return result
    }
}

/**
 * Factory for creating iOS PermissionStorage instances.
 */
actual object PermissionStorageFactory {
    /**
     * Create a PermissionStorage instance.
     * iOS uses NSUserDefaults which doesn't require configuration.
     */
    actual fun create(): PermissionStorage {
        return IosPermissionStorage()
    }
}
