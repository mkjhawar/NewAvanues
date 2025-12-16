package com.augmentalis.magiccode.plugins.persistence

import com.augmentalis.magiccode.plugins.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import platform.Foundation.*

/**
 * iOS UserDefaults-based plugin persistence implementation.
 *
 * Uses NSUserDefaults to persist plugin information. Suitable for iOS environments.
 * Each plugin is stored as a JSON string in UserDefaults for easy access and debugging.
 */
class IosUserDefaultsPluginPersistence(
    private val appDataDir: String,
    private val suiteName: String? = null
) : PluginPersistence {

    companion object {
        private const val TAG = "IosPluginPersistence"
        private const val PLUGIN_KEY_PREFIX = "plugin_"
        private const val PLUGIN_IDS_KEY = "plugin_ids"

        private val json = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
        }
    }

    private val userDefaults: NSUserDefaults = if (suiteName != null) {
        NSUserDefaults(suiteName = suiteName)
    } else {
        NSUserDefaults.standardUserDefaults
    }

    override suspend fun savePlugin(pluginInfo: PluginRegistry.PluginInfo): Result<Unit> {
        return try {
            val data = PluginPersistenceData.fromPluginInfo(pluginInfo)
            val jsonString = json.encodeToString(data)
            val key = getPluginKey(pluginInfo.manifest.id)

            userDefaults.setObject(jsonString, forKey = key)
            addToPluginIdsList(pluginInfo.manifest.id)
            userDefaults.synchronize()

            PluginLog.d(TAG, "Saved plugin to UserDefaults: ${pluginInfo.manifest.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to save plugin: ${pluginInfo.manifest.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun loadPlugin(pluginId: String): Result<PluginRegistry.PluginInfo?> {
        return try {
            val key = getPluginKey(pluginId)
            val jsonString = userDefaults.stringForKey(key)

            if (jsonString == null) {
                PluginLog.d(TAG, "Plugin not found in UserDefaults: $pluginId")
                return Result.success(null)
            }

            val data = json.decodeFromString<PluginPersistenceData>(jsonString)
            val pluginInfo = data.toPluginInfo(appDataDir)
            PluginLog.d(TAG, "Loaded plugin from UserDefaults: $pluginId")
            Result.success(pluginInfo)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to load plugin: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun loadAllPlugins(): Result<List<PluginRegistry.PluginInfo>> {
        return try {
            val pluginIds = getPluginIdsList()
            val plugins = pluginIds.mapNotNull { pluginId ->
                try {
                    val key = getPluginKey(pluginId)
                    val jsonString = userDefaults.stringForKey(key)
                    if (jsonString != null) {
                        val data = json.decodeFromString<PluginPersistenceData>(jsonString)
                        data.toPluginInfo(appDataDir)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    PluginLog.e(TAG, "Failed to load plugin: $pluginId", e)
                    null
                }
            }

            PluginLog.d(TAG, "Loaded ${plugins.size} plugins from UserDefaults")
            Result.success(plugins)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to load all plugins", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePlugin(pluginId: String): Result<Unit> {
        return try {
            val key = getPluginKey(pluginId)
            userDefaults.removeObjectForKey(key)
            removeFromPluginIdsList(pluginId)
            userDefaults.synchronize()

            PluginLog.d(TAG, "Deleted plugin from UserDefaults: $pluginId")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to delete plugin: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePluginState(pluginId: String, state: PluginState): Result<Unit> {
        return try {
            val key = getPluginKey(pluginId)
            val jsonString = userDefaults.stringForKey(key)

            if (jsonString == null) {
                return Result.failure(Exception("Plugin not found: $pluginId"))
            }

            val data = json.decodeFromString<PluginPersistenceData>(jsonString)
            val updatedData = data.copy(state = state.name)
            val updatedJson = json.encodeToString(updatedData)
            userDefaults.setObject(updatedJson, forKey = key)
            userDefaults.synchronize()

            PluginLog.d(TAG, "Updated plugin state: $pluginId -> $state")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to update plugin state: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun exists(pluginId: String): Result<Boolean> {
        return try {
            val key = getPluginKey(pluginId)
            val exists = userDefaults.stringForKey(key) != null
            Result.success(exists)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to check plugin existence: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun getPluginCount(): Result<Int> {
        return try {
            val pluginIds = getPluginIdsList()
            Result.success(pluginIds.size)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to get plugin count", e)
            Result.failure(e)
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return try {
            val pluginIds = getPluginIdsList()
            pluginIds.forEach { pluginId ->
                val key = getPluginKey(pluginId)
                userDefaults.removeObjectForKey(key)
            }
            userDefaults.removeObjectForKey(PLUGIN_IDS_KEY)
            userDefaults.synchronize()

            PluginLog.w(TAG, "Cleared all plugins from UserDefaults")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to clear all plugins", e)
            Result.failure(e)
        }
    }

    /**
     * Get UserDefaults key for a specific plugin.
     */
    private fun getPluginKey(pluginId: String): String {
        return "$PLUGIN_KEY_PREFIX$pluginId"
    }

    /**
     * Get list of all plugin IDs from UserDefaults.
     */
    private fun getPluginIdsList(): List<String> {
        val array = userDefaults.arrayForKey(PLUGIN_IDS_KEY) as? List<*>
        return array?.mapNotNull { it as? String } ?: emptyList()
    }

    /**
     * Add plugin ID to the master list.
     */
    private fun addToPluginIdsList(pluginId: String) {
        val currentIds = getPluginIdsList().toMutableSet()
        currentIds.add(pluginId)
        userDefaults.setObject(currentIds.toList(), forKey = PLUGIN_IDS_KEY)
    }

    /**
     * Remove plugin ID from the master list.
     */
    private fun removeFromPluginIdsList(pluginId: String) {
        val currentIds = getPluginIdsList().toMutableSet()
        currentIds.remove(pluginId)
        userDefaults.setObject(currentIds.toList(), forKey = PLUGIN_IDS_KEY)
    }
}

/**
 * Serializable data model for plugin persistence.
 *
 * This is a flattened representation of PluginInfo suitable for JSON serialization.
 */
@Serializable
private data class PluginPersistenceData(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String? = null,
    val entrypoint: String,
    val capabilities: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val source: String,
    val verificationLevel: String,
    val state: String,
    val loadedAt: Long,
    val namespaceBaseDir: String,
    val namespaceCacheDir: String,
    val namespaceTempDir: String,
    val namespacePreferencesFile: String,
    val namespaceDiskUsage: Long = 0L
) {
    companion object {
        fun fromPluginInfo(pluginInfo: PluginRegistry.PluginInfo): PluginPersistenceData {
            return PluginPersistenceData(
                id = pluginInfo.manifest.id,
                name = pluginInfo.manifest.name,
                version = pluginInfo.manifest.version,
                author = pluginInfo.manifest.author,
                description = pluginInfo.manifest.description,
                entrypoint = pluginInfo.manifest.entrypoint,
                capabilities = pluginInfo.manifest.capabilities,
                permissions = pluginInfo.manifest.permissions,
                source = pluginInfo.manifest.source,
                verificationLevel = pluginInfo.manifest.verificationLevel,
                state = pluginInfo.state.name,
                loadedAt = pluginInfo.loadedAt,
                namespaceBaseDir = pluginInfo.namespace.baseDir,
                namespaceCacheDir = pluginInfo.namespace.cacheDir,
                namespaceTempDir = pluginInfo.namespace.tempDir,
                namespacePreferencesFile = pluginInfo.namespace.preferencesFile,
                namespaceDiskUsage = pluginInfo.namespace.diskUsageBytes
            )
        }
    }

    fun toPluginInfo(appDataDir: String): PluginRegistry.PluginInfo {
        val manifest = PluginManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            entrypoint = entrypoint,
            capabilities = capabilities,
            dependencies = emptyList(), // Not persisted in simple storage
            permissions = permissions,
            source = source,
            verificationLevel = verificationLevel,
            assets = null,
            manifestVersion = "1.0",
            homepage = null,
            license = null
        )

        // Recreate namespace - use persisted values if available
        val namespace = PluginNamespace(
            pluginId = id,
            baseDir = namespaceBaseDir,
            cacheDir = namespaceCacheDir,
            tempDir = namespaceTempDir,
            preferencesFile = namespacePreferencesFile,
            diskUsageBytes = namespaceDiskUsage
        )

        return PluginRegistry.PluginInfo(
            manifest = manifest,
            state = PluginState.valueOf(state),
            loadedAt = loadedAt,
            namespace = namespace
        )
    }
}

/**
 * Create default iOS persistence implementation.
 */
actual fun createDefaultPluginPersistence(appDataDir: String): PluginPersistence {
    PluginLog.i("IosPluginPersistence", "Using UserDefaults-based plugin persistence for iOS platform")
    return IosUserDefaultsPluginPersistence(appDataDir)
}
