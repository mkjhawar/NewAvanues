/**
 * UniversalPluginRegistry.kt - Universal plugin registry with capability-based discovery
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Extends ServiceRegistry from UniversalRPC with plugin-specific features including:
 * - Capability-based plugin discovery
 * - Plugin state management
 * - Thread-safe operations with Mutex
 * - Integration with gRPC service discovery
 */
package com.augmentalis.magiccode.plugins.universal

import com.augmentalis.universalrpc.ServiceEndpoint
import com.augmentalis.universalrpc.ServiceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Plugin registration data containing all metadata about a registered plugin.
 *
 * @property pluginId Unique plugin identifier (reverse-domain notation)
 * @property pluginName Human-readable plugin name
 * @property version Plugin version (semantic versioning)
 * @property capabilities Set of capabilities this plugin provides
 * @property state Current plugin lifecycle state
 * @property endpoint Service endpoint for gRPC communication
 * @property registeredAt Timestamp when plugin was registered
 * @property lastHealthCheck Timestamp of last successful health check
 */
data class PluginRegistration(
    val pluginId: String,
    val pluginName: String,
    val version: String,
    val capabilities: Set<PluginCapability>,
    val state: PluginState,
    val endpoint: ServiceEndpoint,
    val registeredAt: Long,
    val lastHealthCheck: Long = 0
) {
    /**
     * Check if this plugin has a specific capability.
     */
    fun hasCapability(capabilityId: String): Boolean {
        return capabilities.any { it.id == capabilityId }
    }

    /**
     * Get all capability IDs.
     */
    fun capabilityIds(): Set<String> = capabilities.map { it.id }.toSet()
}

/**
 * Universal Plugin Registry - extends ServiceRegistry with plugin-specific features.
 *
 * Integrates with UniversalRPC for gRPC-based plugin discovery and communication.
 * Provides capability-based plugin discovery, state management, and thread-safe operations.
 *
 * ## Features
 * - **Capability-based discovery**: Find plugins by what they can do
 * - **State management**: Track plugin lifecycle states
 * - **ServiceRegistry integration**: Leverages existing gRPC infrastructure
 * - **Thread-safe**: Uses Mutex for concurrent access protection
 *
 * ## Usage
 * ```kotlin
 * val serviceRegistry = ServiceRegistry()
 * val pluginRegistry = UniversalPluginRegistry(serviceRegistry)
 *
 * // Register a plugin
 * val result = pluginRegistry.register(plugin, endpoint)
 *
 * // Discover plugins by capability
 * val llmPlugins = pluginRegistry.discoverByCapability(PluginCapability.LLM_TEXT_GENERATION)
 *
 * // Get specific plugin
 * val plugin = pluginRegistry.getPlugin("com.augmentalis.llm.openai")
 * ```
 *
 * @property serviceRegistry The UniversalRPC ServiceRegistry for gRPC service discovery
 */
class UniversalPluginRegistry(
    private val serviceRegistry: ServiceRegistry
) {
    private val _plugins = MutableStateFlow<Map<String, PluginRegistration>>(emptyMap())

    /** Flow of all registered plugins */
    val plugins: StateFlow<Map<String, PluginRegistration>> = _plugins.asStateFlow()

    private val _capabilityIndex = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    /** Flow of capability-to-plugin mappings for efficient discovery */
    val capabilityIndex: StateFlow<Map<String, Set<String>>> = _capabilityIndex.asStateFlow()

    private val mutex = Mutex()

    /**
     * Get the current list of all registered plugins.
     */
    val allPlugins: Flow<List<PluginRegistration>>
        get() = _plugins.map { it.values.toList() }

    /**
     * Get count of registered plugins.
     */
    val pluginCount: Int
        get() = _plugins.value.size

    /**
     * Register a plugin with capabilities and endpoint.
     *
     * Registers the plugin in both the UniversalPluginRegistry and the underlying
     * ServiceRegistry for gRPC discovery. Updates capability indexes for efficient
     * capability-based lookups.
     *
     * @param plugin The plugin instance implementing UniversalPlugin interface
     * @param endpoint Service endpoint for gRPC communication
     * @return Result containing the PluginRegistration on success, or failure with error
     */
    suspend fun register(
        plugin: UniversalPlugin,
        endpoint: ServiceEndpoint
    ): Result<PluginRegistration> = mutex.withLock {
        try {
            // Check for existing registration
            if (_plugins.value.containsKey(plugin.pluginId)) {
                return@withLock Result.failure(
                    IllegalStateException("Plugin already registered: ${plugin.pluginId}")
                )
            }

            val registration = PluginRegistration(
                pluginId = plugin.pluginId,
                pluginName = plugin.pluginName,
                version = plugin.version,
                capabilities = plugin.capabilities,
                state = plugin.state,
                endpoint = endpoint,
                registeredAt = currentTimeMillis()
            )

            // Add to main registry
            val current = _plugins.value.toMutableMap()
            current[plugin.pluginId] = registration
            _plugins.value = current

            // Update capability index
            updateCapabilityIndex(plugin.pluginId, plugin.capabilities)

            // Register endpoint in ServiceRegistry for gRPC discovery
            val serviceEndpoint = endpoint.copy(
                serviceName = plugin.pluginId,
                metadata = endpoint.metadata + mapOf(
                    "type" to "plugin",
                    "version" to plugin.version,
                    "capabilities" to plugin.capabilities.joinToString(",") { it.id }
                )
            )
            serviceRegistry.registerRemote(serviceEndpoint)

            Result.success(registration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register a plugin using registration data directly.
     *
     * Alternative registration method when you have PluginRegistration data
     * rather than a UniversalPlugin instance.
     *
     * @param registration Plugin registration data
     * @return Result containing the PluginRegistration on success
     */
    suspend fun register(registration: PluginRegistration): Result<PluginRegistration> = mutex.withLock {
        try {
            if (_plugins.value.containsKey(registration.pluginId)) {
                return@withLock Result.failure(
                    IllegalStateException("Plugin already registered: ${registration.pluginId}")
                )
            }

            // Add to main registry
            val current = _plugins.value.toMutableMap()
            current[registration.pluginId] = registration
            _plugins.value = current

            // Update capability index
            updateCapabilityIndex(registration.pluginId, registration.capabilities)

            // Register endpoint in ServiceRegistry
            val serviceEndpoint = registration.endpoint.copy(
                serviceName = registration.pluginId,
                metadata = registration.endpoint.metadata + mapOf(
                    "type" to "plugin",
                    "version" to registration.version,
                    "capabilities" to registration.capabilities.joinToString(",") { it.id }
                )
            )
            serviceRegistry.registerRemote(serviceEndpoint)

            Result.success(registration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Discover plugins by capability.
     *
     * Finds all active plugins that provide a specific capability.
     * Uses the capability index for efficient O(1) lookup of plugin IDs,
     * then filters to only return active plugins.
     *
     * @param capabilityId The capability identifier to search for
     * @return List of PluginRegistrations that provide the capability and are active
     */
    fun discoverByCapability(capabilityId: String): List<PluginRegistration> {
        val pluginIds = _capabilityIndex.value[capabilityId] ?: emptySet()
        return pluginIds.mapNotNull { _plugins.value[it] }
            .filter { it.state == PluginState.ACTIVE }
    }

    /**
     * Discover plugins by multiple capabilities (must have all).
     *
     * @param capabilityIds Set of capability identifiers the plugin must have
     * @return List of active plugins that have ALL specified capabilities
     */
    fun discoverByCapabilities(capabilityIds: Set<String>): List<PluginRegistration> {
        if (capabilityIds.isEmpty()) return emptyList()

        // Get plugins that have the first capability
        val firstCapability = capabilityIds.first()
        val candidates = discoverByCapability(firstCapability)

        // Filter to only those that have all capabilities
        return candidates.filter { registration ->
            capabilityIds.all { capId -> registration.hasCapability(capId) }
        }
    }

    /**
     * Discover plugins by any of the specified capabilities.
     *
     * @param capabilityIds Set of capability identifiers (plugin must have at least one)
     * @return List of active plugins that have ANY of the specified capabilities
     */
    fun discoverByAnyCapability(capabilityIds: Set<String>): List<PluginRegistration> {
        val matchingPluginIds = capabilityIds.flatMap { capId ->
            _capabilityIndex.value[capId] ?: emptySet()
        }.toSet()

        return matchingPluginIds.mapNotNull { _plugins.value[it] }
            .filter { it.state == PluginState.ACTIVE }
    }

    /**
     * Get plugin by ID.
     *
     * @param pluginId The unique plugin identifier
     * @return PluginRegistration if found, null otherwise
     */
    fun getPlugin(pluginId: String): PluginRegistration? {
        return _plugins.value[pluginId]
    }

    /**
     * Get all plugins in a specific state.
     *
     * @param state The plugin state to filter by
     * @return List of plugins in the specified state
     */
    fun getPluginsByState(state: PluginState): List<PluginRegistration> {
        return _plugins.value.values.filter { it.state == state }
    }

    /**
     * Get all active plugins.
     *
     * Convenience method equivalent to getPluginsByState(PluginState.ACTIVE).
     *
     * @return List of active plugins
     */
    fun getActivePlugins(): List<PluginRegistration> {
        return getPluginsByState(PluginState.ACTIVE)
    }

    /**
     * Update plugin state.
     *
     * Updates the lifecycle state of a registered plugin.
     * This is typically called by the PluginLifecycleManager when
     * plugin state transitions occur.
     *
     * @param pluginId The plugin identifier
     * @param newState The new plugin state
     * @return true if the state was updated, false if plugin not found
     */
    suspend fun updateState(pluginId: String, newState: PluginState): Boolean = mutex.withLock {
        val current = _plugins.value.toMutableMap()
        val registration = current[pluginId]

        if (registration != null) {
            current[pluginId] = registration.copy(state = newState)
            _plugins.value = current
            true
        } else {
            false
        }
    }

    /**
     * Update plugin health check timestamp.
     *
     * @param pluginId The plugin identifier
     * @param timestamp Health check timestamp (defaults to current time)
     * @return true if updated, false if plugin not found
     */
    suspend fun updateHealthCheck(
        pluginId: String,
        timestamp: Long = currentTimeMillis()
    ): Boolean = mutex.withLock {
        val current = _plugins.value.toMutableMap()
        val registration = current[pluginId]

        if (registration != null) {
            current[pluginId] = registration.copy(lastHealthCheck = timestamp)
            _plugins.value = current
            true
        } else {
            false
        }
    }

    /**
     * Unregister a plugin.
     *
     * Removes the plugin from both the UniversalPluginRegistry and the
     * underlying ServiceRegistry. Also removes the plugin from capability indexes.
     *
     * @param pluginId The plugin identifier to unregister
     * @return true if the plugin was unregistered, false if not found
     */
    suspend fun unregister(pluginId: String): Boolean = mutex.withLock {
        val current = _plugins.value.toMutableMap()
        val removed = current.remove(pluginId)

        if (removed != null) {
            _plugins.value = current

            // Remove from capability index
            removeFromCapabilityIndex(pluginId, removed.capabilities)

            // Unregister from ServiceRegistry
            serviceRegistry.unregister(pluginId)
            true
        } else {
            false
        }
    }

    /**
     * Check if a plugin is registered.
     *
     * @param pluginId The plugin identifier
     * @return true if the plugin is registered
     */
    fun isRegistered(pluginId: String): Boolean {
        return _plugins.value.containsKey(pluginId)
    }

    /**
     * Check if a capability is available from any active plugin.
     *
     * @param capabilityId The capability identifier
     * @return true if at least one active plugin provides this capability
     */
    fun isCapabilityAvailable(capabilityId: String): Boolean {
        return discoverByCapability(capabilityId).isNotEmpty()
    }

    /**
     * Get the service endpoint for a registered plugin.
     *
     * Retrieves the gRPC service endpoint associated with a plugin,
     * which can be used for direct communication with the plugin.
     *
     * @param pluginId The unique plugin identifier
     * @return ServiceEndpoint if the plugin is registered, null otherwise
     */
    fun getEndpoint(pluginId: String): ServiceEndpoint? {
        return _plugins.value[pluginId]?.endpoint
    }

    /**
     * Update the service endpoint for a registered plugin.
     *
     * This is useful during hot reload when a plugin's endpoint may change
     * (e.g., new port assignment after restart). The endpoint is updated
     * in both the plugin registration and the underlying ServiceRegistry.
     *
     * @param pluginId The unique plugin identifier
     * @param endpoint The new service endpoint
     * @return true if the endpoint was updated, false if plugin not found
     */
    suspend fun updateEndpoint(pluginId: String, endpoint: ServiceEndpoint): Boolean = mutex.withLock {
        val current = _plugins.value.toMutableMap()
        val registration = current[pluginId]

        if (registration != null) {
            // Update the registration with new endpoint
            current[pluginId] = registration.copy(endpoint = endpoint)
            _plugins.value = current

            // Update in ServiceRegistry - unregister old and register new
            serviceRegistry.unregister(pluginId)
            val serviceEndpoint = endpoint.copy(
                serviceName = pluginId,
                metadata = endpoint.metadata + mapOf(
                    "type" to "plugin",
                    "version" to registration.version,
                    "capabilities" to registration.capabilities.joinToString(",") { it.id }
                )
            )
            serviceRegistry.registerRemote(serviceEndpoint)

            true
        } else {
            false
        }
    }

    /**
     * Get all available capabilities from active plugins.
     *
     * @return Set of all capability IDs available from active plugins
     */
    fun getAvailableCapabilities(): Set<String> {
        return _plugins.value.values
            .filter { it.state == PluginState.ACTIVE }
            .flatMap { it.capabilityIds() }
            .toSet()
    }

    /**
     * Clear all registrations.
     *
     * Removes all plugins from the registry. Use with caution.
     */
    suspend fun clear() = mutex.withLock {
        // Unregister all from ServiceRegistry
        _plugins.value.keys.forEach { pluginId ->
            serviceRegistry.unregister(pluginId)
        }

        _plugins.value = emptyMap()
        _capabilityIndex.value = emptyMap()
    }

    /**
     * Update the capability index when a plugin is registered.
     *
     * Must be called within mutex lock.
     */
    private fun updateCapabilityIndex(pluginId: String, capabilities: Set<PluginCapability>) {
        val current = _capabilityIndex.value.toMutableMap()
        capabilities.forEach { cap ->
            val existing = current[cap.id] ?: emptySet()
            current[cap.id] = existing + pluginId
        }
        _capabilityIndex.value = current
    }

    /**
     * Remove a plugin from capability indexes.
     *
     * Must be called within mutex lock.
     */
    private fun removeFromCapabilityIndex(pluginId: String, capabilities: Set<PluginCapability>) {
        val current = _capabilityIndex.value.toMutableMap()
        capabilities.forEach { cap ->
            current[cap.id]?.let { plugins ->
                val updated = plugins - pluginId
                if (updated.isEmpty()) {
                    current.remove(cap.id)
                } else {
                    current[cap.id] = updated
                }
            }
        }
        _capabilityIndex.value = current
    }

    companion object {
        /** Well-known service name for the plugin registry */
        const val SERVICE_PLUGIN_REGISTRY = "com.augmentalis.plugin.registry"

        /** Default port for plugin registry gRPC service */
        const val DEFAULT_PORT_PLUGIN_REGISTRY = 50060

        private const val TAG = "UniversalPluginRegistry"
    }
}
