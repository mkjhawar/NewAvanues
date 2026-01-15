package com.augmentalis.avacode.plugins

import com.augmentalis.avacode.plugins.PluginRegistry.PluginInfo

/**
 * Plugin persistence interface.
 *
 * Provides a platform-agnostic interface for persisting plugin information
 * across application restarts. Platform-specific implementations handle the
 * actual storage mechanism (Room database, file storage, NSUserDefaults, etc.).
 */
interface PluginPersistence {
    /**
     * Save plugin information to persistent storage.
     *
     * @param pluginInfo Plugin information to save
     * @return Result indicating success or failure with error details
     */
    suspend fun savePlugin(pluginInfo: PluginInfo): Result<Unit>

    /**
     * Load plugin information from persistent storage.
     *
     * @param pluginId Unique plugin identifier
     * @return Result containing PluginInfo if found, or error if not found or load failed
     */
    suspend fun loadPlugin(pluginId: String): Result<PluginInfo?>

    /**
     * Load all plugins from persistent storage.
     *
     * @return Result containing list of all persisted plugins, or error if load failed
     */
    suspend fun loadAllPlugins(): Result<List<PluginInfo>>

    /**
     * Delete plugin information from persistent storage.
     *
     * @param pluginId Unique plugin identifier
     * @return Result indicating success or failure with error details
     */
    suspend fun deletePlugin(pluginId: String): Result<Unit>

    /**
     * Update plugin state in persistent storage.
     *
     * This is an optimization to avoid full plugin saves when only state changes.
     *
     * @param pluginId Unique plugin identifier
     * @param state New plugin state
     * @return Result indicating success or failure with error details
     */
    suspend fun updatePluginState(pluginId: String, state: com.augmentalis.magiccode.plugins.core.PluginState): Result<Unit>

    /**
     * Check if plugin exists in persistent storage.
     *
     * @param pluginId Unique plugin identifier
     * @return Result containing true if plugin exists, false otherwise
     */
    suspend fun exists(pluginId: String): Result<Boolean>

    /**
     * Get count of persisted plugins.
     *
     * @return Result containing count of plugins in storage
     */
    suspend fun getPluginCount(): Result<Int>

    /**
     * Clear all plugins from persistent storage.
     *
     * Warning: This is a destructive operation. Should only be used for testing or cleanup.
     *
     * @return Result indicating success or failure with error details
     */
    suspend fun clearAll(): Result<Unit>
}

/**
 * Create platform-specific default plugin persistence implementation.
 *
 * @param appDataDir Application data directory for plugin storage
 * @return Platform-specific PluginPersistence instance
 */
expect fun createDefaultPluginPersistence(appDataDir: String): PluginPersistence

/**
 * In-memory plugin persistence implementation.
 *
 * Provides a non-persistent storage implementation useful for testing
 * or platforms where persistence is not needed/available.
 */
class InMemoryPluginPersistence : PluginPersistence {
    private val plugins = mutableMapOf<String, PluginInfo>()

    override suspend fun savePlugin(pluginInfo: PluginInfo): Result<Unit> {
        return try {
            plugins[pluginInfo.manifest.id] = pluginInfo
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadPlugin(pluginId: String): Result<PluginInfo?> {
        return try {
            Result.success(plugins[pluginId])
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadAllPlugins(): Result<List<PluginInfo>> {
        return try {
            Result.success(plugins.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlugin(pluginId: String): Result<Unit> {
        return try {
            plugins.remove(pluginId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePluginState(
        pluginId: String,
        state: com.augmentalis.magiccode.plugins.core.PluginState
    ): Result<Unit> {
        return try {
            val plugin = plugins[pluginId]
            if (plugin != null) {
                plugins[pluginId] = plugin.copy(state = state)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Plugin not found: $pluginId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exists(pluginId: String): Result<Boolean> {
        return try {
            Result.success(plugins.containsKey(pluginId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPluginCount(): Result<Int> {
        return try {
            Result.success(plugins.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return try {
            plugins.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
