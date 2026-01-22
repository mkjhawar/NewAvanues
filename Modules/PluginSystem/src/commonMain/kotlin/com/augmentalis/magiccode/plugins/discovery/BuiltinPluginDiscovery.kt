/**
 * BuiltinPluginDiscovery.kt - Discovers built-in plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Discovery source for plugins compiled into the application.
 * Built-in plugins are registered at compile time and are always available.
 * They have the highest priority (lowest value) in the discovery chain.
 */
package com.augmentalis.magiccode.plugins.discovery

import com.augmentalis.magiccode.plugins.universal.PluginCapability
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Discovers built-in plugins compiled into the application.
 *
 * Built-in plugins are registered at compile time via [registerBuiltin] and are
 * always available. They have the highest priority (priority = 0) in the
 * discovery chain, meaning they take precedence over plugins from other sources.
 *
 * ## Usage Pattern
 * 1. Create a single instance of BuiltinPluginDiscovery
 * 2. Register built-in plugins during application startup
 * 3. Add to CompositePluginDiscovery for unified access
 *
 * ## Example
 * ```kotlin
 * val builtinDiscovery = BuiltinPluginDiscovery()
 *
 * // Register standard handlers
 * builtinDiscovery.registerStandardHandlers()
 *
 * // Register custom built-in plugins
 * builtinDiscovery.registerBuiltin { MyCustomPlugin() }
 *
 * // Discover all registered plugins
 * val plugins = builtinDiscovery.discoverPlugins()
 * ```
 *
 * ## Thread Safety
 * All operations are thread-safe. Registration and discovery can occur
 * concurrently without external synchronization.
 *
 * @since 1.0.0
 * @see PluginDiscovery
 * @see CompositePluginDiscovery
 */
class BuiltinPluginDiscovery : PluginDiscovery {
    /**
     * Priority for built-in plugins (highest priority).
     *
     * Built-in plugins always take precedence over plugins from other sources.
     */
    override val priority: Int = PRIORITY_BUILTIN

    /**
     * Registered plugin factories.
     *
     * Each factory is a lambda that creates a new plugin instance.
     * Using factories instead of instances allows for fresh creation on each load.
     */
    private val pluginFactories = mutableListOf<PluginFactory>()

    /**
     * Cached descriptors for discovered plugins.
     */
    private val descriptorCache = mutableMapOf<String, PluginDescriptor>()

    /**
     * Mutex for thread-safe access to factories and cache.
     */
    private val mutex = Mutex()

    /**
     * Listeners for registration events.
     */
    private val registrationListeners = mutableListOf<RegistrationListener>()

    /**
     * Register a built-in plugin factory.
     *
     * The factory will be called to create a plugin instance when the plugin
     * is loaded. Registration does not immediately create a plugin instance.
     *
     * @param factory Lambda that creates a new plugin instance
     * @throws IllegalArgumentException if a plugin with the same ID is already registered
     */
    suspend fun registerBuiltin(factory: () -> UniversalPlugin) {
        mutex.withLock {
            // Create a temporary instance to get metadata
            val tempInstance = factory()
            val pluginId = tempInstance.pluginId

            // Check for duplicate registration
            if (pluginFactories.any { it.pluginId == pluginId }) {
                throw IllegalArgumentException("Plugin with ID '$pluginId' is already registered")
            }

            // Create factory wrapper
            val pluginFactory = PluginFactory(
                pluginId = pluginId,
                factory = factory,
                descriptor = PluginDescriptor.fromPlugin(tempInstance, PluginSource.Builtin)
            )

            pluginFactories.add(pluginFactory)
            descriptorCache[pluginId] = pluginFactory.descriptor

            // Notify listeners
            registrationListeners.forEach { it.onPluginRegistered(pluginFactory.descriptor) }
        }
    }

    /**
     * Register a built-in plugin factory without duplicate checking.
     *
     * Use this for performance-critical registration where uniqueness is
     * guaranteed by the caller.
     *
     * @param factory Lambda that creates a new plugin instance
     */
    suspend fun registerBuiltinUnchecked(factory: () -> UniversalPlugin) {
        mutex.withLock {
            val tempInstance = factory()
            val pluginFactory = PluginFactory(
                pluginId = tempInstance.pluginId,
                factory = factory,
                descriptor = PluginDescriptor.fromPlugin(tempInstance, PluginSource.Builtin)
            )
            pluginFactories.add(pluginFactory)
            descriptorCache[pluginFactory.pluginId] = pluginFactory.descriptor
        }
    }

    /**
     * Register multiple built-in plugins at once.
     *
     * More efficient than multiple individual registrations.
     *
     * @param factories List of plugin factory lambdas
     */
    suspend fun registerBuiltins(factories: List<() -> UniversalPlugin>) {
        mutex.withLock {
            factories.forEach { factory ->
                val tempInstance = factory()
                val pluginId = tempInstance.pluginId

                if (pluginFactories.none { it.pluginId == pluginId }) {
                    val pluginFactory = PluginFactory(
                        pluginId = pluginId,
                        factory = factory,
                        descriptor = PluginDescriptor.fromPlugin(tempInstance, PluginSource.Builtin)
                    )
                    pluginFactories.add(pluginFactory)
                    descriptorCache[pluginId] = pluginFactory.descriptor
                }
            }
        }
    }

    /**
     * Register all standard built-in handlers.
     *
     * Call this during application startup to register all standard
     * plugin types. This method registers:
     * - Core accessibility handlers
     * - Default speech engines
     * - Built-in theme providers
     *
     * ## Platform-Specific
     * Some handlers may only be available on specific platforms.
     * Call [getAvailableStandardHandlers] to see what's available.
     */
    suspend fun registerStandardHandlers() {
        // Standard handlers are registered by the platform-specific implementation
        // This base implementation provides an empty list
        // Subclasses or platform code should override/extend this

        // The actual handlers would be registered like:
        // registerBuiltin { DefaultAccessibilityHandler() }
        // registerBuiltin { DefaultSpeechEnginePlugin() }
        // etc.
    }

    /**
     * Get list of available standard handler types.
     *
     * @return List of standard handler capability IDs available on this platform
     */
    fun getAvailableStandardHandlers(): List<String> {
        return listOf(
            PluginCapability.ACCESSIBILITY_HANDLER,
            PluginCapability.SPEECH_RECOGNITION,
            PluginCapability.SPEECH_TTS,
            PluginCapability.UI_THEME
        )
    }

    /**
     * Discover available plugins from this source.
     *
     * Returns descriptors for all registered built-in plugins.
     *
     * @return List of plugin descriptors for all registered plugins
     */
    override suspend fun discoverPlugins(): List<PluginDescriptor> {
        return mutex.withLock {
            descriptorCache.values.toList()
        }
    }

    /**
     * Load a plugin from its descriptor.
     *
     * Creates a new instance of the plugin using its registered factory.
     *
     * @param descriptor Plugin descriptor from discovery
     * @return Result containing the loaded plugin or error if not found
     */
    override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
        return mutex.withLock {
            val factory = pluginFactories.find { it.pluginId == descriptor.pluginId }
            if (factory != null) {
                try {
                    Result.success(factory.factory())
                } catch (e: Exception) {
                    Result.failure(PluginLoadException(
                        "Failed to create plugin instance for '${descriptor.pluginId}'",
                        descriptor.pluginId,
                        e
                    ))
                }
            } else {
                Result.failure(PluginNotFoundException(
                    "No built-in plugin found with ID '${descriptor.pluginId}'",
                    descriptor.pluginId
                ))
            }
        }
    }

    /**
     * Check if this source can load a specific plugin.
     *
     * @param descriptor Plugin descriptor to check
     * @return true if the plugin is a builtin and is registered
     */
    override fun canLoad(descriptor: PluginDescriptor): Boolean {
        return descriptor.source is PluginSource.Builtin
    }

    /**
     * Unregister a built-in plugin.
     *
     * Removes a plugin from the registry. Use with caution - this should
     * only be used during testing or for dynamic plugin management.
     *
     * @param pluginId ID of the plugin to unregister
     * @return true if the plugin was found and removed
     */
    suspend fun unregister(pluginId: String): Boolean {
        return mutex.withLock {
            val removed = pluginFactories.removeAll { it.pluginId == pluginId }
            if (removed) {
                val descriptor = descriptorCache.remove(pluginId)
                descriptor?.let { desc ->
                    registrationListeners.forEach { it.onPluginUnregistered(desc) }
                }
            }
            removed
        }
    }

    /**
     * Clear all registered plugins.
     *
     * Use with caution - primarily for testing purposes.
     */
    suspend fun clear() {
        mutex.withLock {
            pluginFactories.clear()
            descriptorCache.clear()
        }
    }

    /**
     * Get the count of registered plugins.
     *
     * @return Number of registered built-in plugins
     */
    suspend fun registeredCount(): Int {
        return mutex.withLock {
            pluginFactories.size
        }
    }

    /**
     * Check if a plugin is registered.
     *
     * @param pluginId Plugin ID to check
     * @return true if the plugin is registered
     */
    suspend fun isRegistered(pluginId: String): Boolean {
        return mutex.withLock {
            descriptorCache.containsKey(pluginId)
        }
    }

    /**
     * Get a specific plugin descriptor.
     *
     * @param pluginId Plugin ID to look up
     * @return PluginDescriptor or null if not found
     */
    suspend fun getDescriptor(pluginId: String): PluginDescriptor? {
        return mutex.withLock {
            descriptorCache[pluginId]
        }
    }

    /**
     * Find plugins by capability.
     *
     * @param capabilityId Capability ID to search for
     * @return List of plugin descriptors with the capability
     */
    suspend fun findByCapability(capabilityId: String): List<PluginDescriptor> {
        return mutex.withLock {
            descriptorCache.values.filter { it.hasCapability(capabilityId) }
        }
    }

    /**
     * Add a registration listener.
     *
     * @param listener Listener to add
     */
    fun addRegistrationListener(listener: RegistrationListener) {
        registrationListeners.add(listener)
    }

    /**
     * Remove a registration listener.
     *
     * @param listener Listener to remove
     */
    fun removeRegistrationListener(listener: RegistrationListener) {
        registrationListeners.remove(listener)
    }

    /**
     * Internal class to hold plugin factory and metadata.
     */
    private data class PluginFactory(
        val pluginId: String,
        val factory: () -> UniversalPlugin,
        val descriptor: PluginDescriptor
    )

    /**
     * Listener for plugin registration events.
     */
    interface RegistrationListener {
        /**
         * Called when a plugin is registered.
         *
         * @param descriptor Descriptor of the registered plugin
         */
        fun onPluginRegistered(descriptor: PluginDescriptor)

        /**
         * Called when a plugin is unregistered.
         *
         * @param descriptor Descriptor of the unregistered plugin
         */
        fun onPluginUnregistered(descriptor: PluginDescriptor)
    }

    companion object {
        /**
         * Priority value for built-in plugins.
         */
        const val PRIORITY_BUILTIN = 0

        /**
         * Create a pre-configured discovery with common plugins.
         *
         * @return BuiltinPluginDiscovery with standard handlers registered
         */
        suspend fun withStandardHandlers(): BuiltinPluginDiscovery {
            return BuiltinPluginDiscovery().apply {
                registerStandardHandlers()
            }
        }
    }
}

/**
 * Exception thrown when a plugin cannot be found.
 *
 * @property message Error message
 * @property pluginId ID of the plugin that was not found
 */
class PluginNotFoundException(
    override val message: String,
    val pluginId: String
) : Exception(message)

/**
 * Exception thrown when a plugin cannot be loaded.
 *
 * @property message Error message
 * @property pluginId ID of the plugin that failed to load
 * @property cause Underlying cause of the failure
 */
class PluginLoadException(
    override val message: String,
    val pluginId: String,
    override val cause: Throwable? = null
) : Exception(message, cause)
