package com.augmentalis.avacode.plugins

import com.augmentalis.avacode.plugins.PluginPersistence
import com.augmentalis.avacode.plugins.InMemoryPluginPersistence
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central registry for managing plugin metadata and lifecycle state.
 *
 * The PluginRegistry maintains an in-memory index of all installed plugins,
 * tracking their manifest data, current state, and registration information.
 * It provides thread-safe access to plugin metadata and supports optional
 * persistence through [PluginPersistence].
 *
 * ## Thread Safety
 * All public methods are thread-safe and use internal mutex synchronization
 * to prevent race conditions during concurrent access.
 *
 * ## Performance Optimization
 * The registry maintains multiple indexes for efficient lookups:
 * - By plugin state ([PluginState])
 * - By plugin source ([PluginSource])
 * - By verification level ([DeveloperVerificationLevel])
 *
 * ## Usage
 * ```kotlin
 * // Create registry with optional persistence
 * val registry = PluginRegistry(persistence = InMemoryPluginPersistence())
 *
 * // Register a plugin
 * val manifest = PluginManifest(...)
 * val namespace = PluginNamespace.create("com.example.plugin", "/data")
 * registry.register(manifest, namespace)
 *
 * // Query plugins
 * val allPlugins = registry.getAllPlugins()
 * val activePlugins = registry.getPluginsByState(PluginState.ACTIVE)
 * ```
 *
 * @property persistence Optional persistence layer for plugin state.
 *                       If null, plugins are only stored in-memory (default behavior).
 * @since 1.0.0
 * @see PluginLoader
 * @see PluginManifest
 * @see PluginPersistence
 */
class PluginRegistry(
    private val persistence: PluginPersistence? = null
) {
    private val mutex = Mutex()
    private val plugins = mutableMapOf<String, PluginInfo>()

    // Indexes for efficient lookups
    private val pluginsByState = mutableMapOf<PluginState, MutableSet<String>>()
    private val pluginsBySource = mutableMapOf<PluginSource, MutableSet<String>>()
    private val pluginsByVerificationLevel = mutableMapOf<DeveloperVerificationLevel, MutableSet<String>>()

    companion object {
        private const val TAG = "PluginRegistry"
    }

    /**
     * Plugin information tracked by registry.
     *
     * Contains all metadata and runtime state for a registered plugin.
     *
     * @property manifest The plugin's manifest containing metadata and requirements
     * @property state Current lifecycle state of the plugin
     * @property loadedAt Timestamp (milliseconds since epoch) when plugin was registered
     * @property namespace Plugin's isolated namespace for storage and resources
     * @since 1.0.0
     */
    data class PluginInfo(
        val manifest: PluginManifest,
        val state: PluginState,
        val loadedAt: Long,
        val namespace: PluginNamespace
    )

    /**
     * Register a plugin in the registry.
     *
     * If a plugin with the same ID is already registered, this method returns false
     * without modifying the registry. The plugin is initially registered in the
     * [PluginState.INSTALLED] state. If persistence is configured, the plugin
     * data is also saved to persistent storage.
     *
     * ## Example
     * ```kotlin
     * val manifest = PluginManifest(
     *     id = "com.example.plugin",
     *     name = "Example Plugin",
     *     version = "1.0.0",
     *     // ... other fields
     * )
     * val namespace = PluginNamespace.create("com.example.plugin", "/data")
     * val success = registry.register(manifest, namespace)
     * ```
     *
     * @param manifest Plugin manifest containing metadata and requirements
     * @param namespace Plugin namespace for isolated storage
     * @return true if registration succeeded, false if plugin ID already exists
     * @see unregister
     * @see updateState
     */
    suspend fun register(manifest: PluginManifest, namespace: PluginNamespace): Boolean {
        return mutex.withLock {
            if (plugins.containsKey(manifest.id)) {
                PluginLog.w(TAG, "Plugin already registered: ${manifest.id}")
                false
            } else {
                val info = PluginInfo(
                    manifest = manifest,
                    state = PluginState.INSTALLED,
                    loadedAt = System.currentTimeMillis(),
                    namespace = namespace
                )
                plugins[manifest.id] = info

                // Update indexes
                addToIndex(manifest.id, info)

                // Persist to storage if available
                persistence?.let { persistence ->
                    val result = persistence.savePlugin(info)
                    result.onFailure { error ->
                        PluginLog.e(TAG, "Failed to persist plugin: ${manifest.id}", error)
                    }
                }

                PluginLog.i(TAG, "Registered plugin: ${manifest.id} v${manifest.version}")
                true
            }
        }
    }

    /**
     * Unregister a plugin from the registry.
     *
     * Removes the plugin from the in-memory registry and all indexes. If persistence
     * is configured, also deletes the plugin from persistent storage.
     *
     * @param pluginId Plugin identifier to unregister
     * @return true if unregistered successfully, false if plugin not found
     * @see register
     */
    suspend fun unregister(pluginId: String): Boolean {
        return mutex.withLock {
            val info = plugins.remove(pluginId)
            if (info != null) {
                // Update indexes
                removeFromIndex(pluginId, info)

                // Delete from persistence if available
                persistence?.let { persistence ->
                    val result = persistence.deletePlugin(pluginId)
                    result.onFailure { error ->
                        PluginLog.e(TAG, "Failed to delete plugin from persistence: $pluginId", error)
                    }
                }

                PluginLog.i(TAG, "Unregistered plugin: $pluginId")
                true
            } else {
                PluginLog.w(TAG, "Plugin not found for unregister: $pluginId")
                false
            }
        }
    }

    /**
     * Update plugin state.
     *
     * Updates the plugin's lifecycle state and maintains index consistency.
     * If persistence is configured, the state change is persisted to storage.
     *
     * ## State Transitions
     * Common state transitions:
     * - INSTALLED -> ACTIVE (plugin activation)
     * - ACTIVE -> DISABLED (plugin deactivation)
     * - ACTIVE -> UPDATING (plugin update started)
     * - UPDATING -> ACTIVE (update completed)
     * - * -> FAILED (error during any operation)
     *
     * @param pluginId Plugin identifier
     * @param newState New plugin state to set
     * @return true if updated successfully, false if plugin not found
     * @see PluginState
     * @see getPluginsByState
     */
    suspend fun updateState(pluginId: String, newState: PluginState): Boolean {
        return mutex.withLock {
            val oldInfo = plugins[pluginId]
            if (oldInfo != null) {
                val newInfo = oldInfo.copy(state = newState)
                plugins[pluginId] = newInfo

                // Update state index
                pluginsByState[oldInfo.state]?.remove(pluginId)
                pluginsByState.getOrPut(newState) { mutableSetOf() }.add(pluginId)

                // Persist state change if available
                persistence?.let { persistence ->
                    val result = persistence.updatePluginState(pluginId, newState)
                    result.onFailure { error ->
                        PluginLog.e(TAG, "Failed to persist state update: $pluginId", error)
                    }
                }

                PluginLog.d(TAG, "Updated plugin state: $pluginId -> $newState")
                true
            } else {
                PluginLog.w(TAG, "Plugin not found for state update: $pluginId")
                false
            }
        }
    }

    /**
     * Get plugin information by ID.
     *
     * @param pluginId Plugin identifier to look up
     * @return [PluginInfo] containing plugin metadata and state, or null if not found
     * @see getAllPlugins
     * @see isRegistered
     */
    suspend fun getPlugin(pluginId: String): PluginInfo? {
        return mutex.withLock {
            plugins[pluginId]
        }
    }

    /**
     * Get all registered plugins.
     *
     * Returns a snapshot of all currently registered plugins. The returned list
     * is a copy and modifications to it will not affect the registry.
     *
     * @return Immutable list of all plugin information
     * @see getPlugin
     * @see getPluginsByState
     * @see getPluginCount
     */
    suspend fun getAllPlugins(): List<PluginInfo> {
        return mutex.withLock {
            plugins.values.toList()
        }
    }

    /**
     * Get plugins by state (optimized with index).
     *
     * Uses an internal index for O(1) lookup performance. This is more efficient
     * than filtering all plugins when querying by state.
     *
     * ## Example
     * ```kotlin
     * // Get all active plugins
     * val activePlugins = registry.getPluginsByState(PluginState.ACTIVE)
     *
     * // Get all failed plugins
     * val failedPlugins = registry.getPluginsByState(PluginState.FAILED)
     * ```
     *
     * @param state Plugin state to filter by
     * @return List of plugins in the specified state (may be empty)
     * @see PluginState
     * @see updateState
     */
    suspend fun getPluginsByState(state: PluginState): List<PluginInfo> {
        return mutex.withLock {
            val pluginIds = pluginsByState[state] ?: emptySet()
            pluginIds.mapNotNull { plugins[it] }
        }
    }

    /**
     * Get plugins by source (optimized with index).
     *
     * Uses an internal index for efficient lookup by plugin source.
     * Useful for filtering plugins by their origin (pre-bundled, store, third-party).
     *
     * @param source Plugin source to filter by
     * @return List of plugins from the specified source (may be empty)
     * @see PluginSource
     * @see getPluginsByVerificationLevel
     */
    suspend fun getPluginsBySource(source: PluginSource): List<PluginInfo> {
        return mutex.withLock {
            val pluginIds = pluginsBySource[source] ?: emptySet()
            pluginIds.mapNotNull { plugins[it] }
        }
    }

    /**
     * Get plugins by verification level (optimized with index).
     *
     * Uses an internal index for efficient lookup by developer verification level.
     * Useful for security-related queries (e.g., finding all unverified plugins).
     *
     * @param level Verification level to filter by
     * @return List of plugins with the specified verification level (may be empty)
     * @see DeveloperVerificationLevel
     * @see getPluginsBySource
     */
    suspend fun getPluginsByVerificationLevel(level: DeveloperVerificationLevel): List<PluginInfo> {
        return mutex.withLock {
            val pluginIds = pluginsByVerificationLevel[level] ?: emptySet()
            pluginIds.mapNotNull { plugins[it] }
        }
    }

    /**
     * Check if a plugin is registered.
     *
     * Performs a fast O(1) lookup to determine if a plugin with the given ID exists.
     *
     * @param pluginId Plugin identifier to check
     * @return true if registered, false otherwise
     * @see getPlugin
     * @see register
     */
    suspend fun isRegistered(pluginId: String): Boolean {
        return mutex.withLock {
            plugins.containsKey(pluginId)
        }
    }

    /**
     * Get total plugin count.
     *
     * Returns the total number of currently registered plugins across all states.
     *
     * @return Number of registered plugins
     * @see getAllPlugins
     */
    suspend fun getPluginCount(): Int {
        return mutex.withLock {
            plugins.size
        }
    }

    /**
     * Clear all plugins from registry.
     *
     * Removes all registered plugins from the in-memory registry and all indexes.
     * Optionally clears persistent storage as well.
     *
     * **Warning**: This is a destructive operation that should only be used for
     * testing or complete system reset. All plugin state will be lost.
     *
     * @param clearPersistence If true, also clear persistent storage (default: false)
     * @see register
     * @see unregister
     */
    suspend fun clear(clearPersistence: Boolean = false) {
        mutex.withLock {
            PluginLog.w(TAG, "Clearing all plugins from registry")
            plugins.clear()
            pluginsByState.clear()
            pluginsBySource.clear()
            pluginsByVerificationLevel.clear()

            // Clear persistence if requested
            if (clearPersistence) {
                persistence?.let { persistence ->
                    val result = persistence.clearAll()
                    result.onFailure { error ->
                        PluginLog.e(TAG, "Failed to clear persistence", error)
                    }
                }
            }
        }
    }

    /**
     * Load plugins from persistent storage.
     *
     * This method should be called on app startup to restore previously registered plugins.
     * Plugins are loaded from persistence and registered in the in-memory registry.
     *
     * @return Result containing count of successfully loaded plugins, or error if load failed
     */
    suspend fun loadFromPersistence(): Result<Int> {
        if (persistence == null) {
            PluginLog.d(TAG, "No persistence configured, skipping load")
            return Result.success(0)
        }

        return try {
            val result = persistence.loadAllPlugins()
            result.fold(
                onSuccess = { persistedPlugins ->
                    var successCount = 0
                    var failureCount = 0

                    mutex.withLock {
                        persistedPlugins.forEach { pluginInfo ->
                            try {
                                // Add to in-memory registry
                                plugins[pluginInfo.manifest.id] = pluginInfo

                                // Update indexes
                                addToIndex(pluginInfo.manifest.id, pluginInfo)

                                successCount++
                                PluginLog.d(TAG, "Loaded plugin from persistence: ${pluginInfo.manifest.id}")
                            } catch (e: Exception) {
                                failureCount++
                                PluginLog.e(TAG, "Failed to load plugin: ${pluginInfo.manifest.id}", e)
                            }
                        }
                    }

                    if (failureCount > 0) {
                        PluginLog.w(TAG, "Loaded $successCount plugins from persistence ($failureCount failed)")
                    } else {
                        PluginLog.i(TAG, "Loaded $successCount plugins from persistence")
                    }

                    Result.success(successCount)
                },
                onFailure = { error ->
                    PluginLog.e(TAG, "Failed to load plugins from persistence", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            PluginLog.e(TAG, "Unexpected error loading plugins from persistence", e)
            Result.failure(e)
        }
    }

    /**
     * Add plugin to indexes.
     *
     * Must be called within mutex lock.
     */
    private fun addToIndex(pluginId: String, info: PluginInfo) {
        // Add to state index
        pluginsByState.getOrPut(info.state) { mutableSetOf() }.add(pluginId)

        // Add to source index
        val source = PluginSource.valueOf(info.manifest.source.uppercase())
        pluginsBySource.getOrPut(source) { mutableSetOf() }.add(pluginId)

        // Add to verification level index
        val verificationLevel = DeveloperVerificationLevel.valueOf(
            info.manifest.verificationLevel.uppercase()
        )
        pluginsByVerificationLevel.getOrPut(verificationLevel) { mutableSetOf() }.add(pluginId)
    }

    /**
     * Remove plugin from indexes.
     *
     * Must be called within mutex lock.
     */
    private fun removeFromIndex(pluginId: String, info: PluginInfo) {
        // Remove from state index
        pluginsByState[info.state]?.remove(pluginId)

        // Remove from source index
        val source = PluginSource.valueOf(info.manifest.source.uppercase())
        pluginsBySource[source]?.remove(pluginId)

        // Remove from verification level index
        val verificationLevel = DeveloperVerificationLevel.valueOf(
            info.manifest.verificationLevel.uppercase()
        )
        pluginsByVerificationLevel[verificationLevel]?.remove(pluginId)
    }
}
