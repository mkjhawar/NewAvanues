package com.augmentalis.avaelements.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Component Registry
 *
 * Central registry for all components (built-in and plugin-provided).
 * Manages component registration, lookup, and lifecycle.
 */
object ComponentRegistry {
    private val components = mutableMapOf<String, ComponentDefinition>()
    private val pluginComponents = mutableMapOf<String, MutableSet<String>>() // pluginId -> componentTypes
    private val listeners = mutableListOf<RegistryListener>()
    private val mutex = Mutex()

    /**
     * Register a component definition
     *
     * @param definition Component definition to register
     * @param pluginId Optional plugin ID if component is from a plugin
     * @throws IllegalStateException if component type already registered
     */
    suspend fun register(definition: ComponentDefinition, pluginId: String? = null) {
        mutex.withLock {
            require(!components.containsKey(definition.type)) {
                "Component type '${definition.type}' is already registered"
            }

            components[definition.type] = definition

            if (pluginId != null) {
                pluginComponents.getOrPut(pluginId) { mutableSetOf() }.add(definition.type)
            }

            notifyListeners(RegistryEvent.Registered(definition.type, pluginId))
        }
    }

    /**
     * Unregister a component by type
     *
     * @param type Component type to unregister
     * @return true if component was found and removed
     */
    suspend fun unregister(type: String): Boolean {
        mutex.withLock {
            val removed = components.remove(type) != null

            if (removed) {
                // Remove from plugin components
                pluginComponents.values.forEach { it.remove(type) }
                notifyListeners(RegistryEvent.Unregistered(type))
            }

            return removed
        }
    }

    /**
     * Unregister all components from a plugin
     *
     * @param pluginId Plugin ID
     * @return Number of components unregistered
     */
    suspend fun unregisterAll(pluginId: String): Int {
        mutex.withLock {
            val types = pluginComponents.remove(pluginId) ?: return 0
            types.forEach { type ->
                components.remove(type)
                notifyListeners(RegistryEvent.Unregistered(type))
            }
            return types.size
        }
    }

    /**
     * Unregister all components from a plugin instance
     */
    suspend fun unregisterAll(plugin: MagicElementPlugin): Int {
        return unregisterAll(plugin.id)
    }

    /**
     * Get component definition by type
     *
     * @param type Component type
     * @return Component definition or null if not found
     */
    suspend fun get(type: String): ComponentDefinition? {
        mutex.withLock {
            return components[type]
        }
    }

    /**
     * Check if component type is registered
     */
    suspend fun contains(type: String): Boolean {
        mutex.withLock {
            return components.containsKey(type)
        }
    }

    /**
     * Get all registered component types
     */
    suspend fun getAllTypes(): Set<String> {
        mutex.withLock {
            return components.keys.toSet()
        }
    }

    /**
     * Get all component types from a plugin
     */
    suspend fun getPluginTypes(pluginId: String): Set<String> {
        mutex.withLock {
            return pluginComponents[pluginId]?.toSet() ?: emptySet()
        }
    }

    /**
     * Create component instance
     *
     * @param type Component type
     * @param config Component configuration
     * @return Component instance
     * @throws IllegalArgumentException if type not registered
     */
    suspend fun create(type: String, config: ComponentConfig): Component {
        val definition = get(type) ?: throw IllegalArgumentException("Unknown component type: $type")

        // Validate configuration if validator provided
        definition.validator?.let { validator ->
            val result = validator.validate(config)
            if (!result.isValid) {
                throw PluginException.ValidationException(result.errors)
            }
        }

        return definition.factory.create(config)
    }

    /**
     * Notify all listeners when plugin components are reloaded
     */
    suspend fun notifyReload(pluginId: String) {
        mutex.withLock {
            notifyListeners(RegistryEvent.PluginReloaded(pluginId))
        }
    }

    /**
     * Add a registry listener
     */
    suspend fun addListener(listener: RegistryListener) {
        mutex.withLock {
            listeners.add(listener)
        }
    }

    /**
     * Remove a registry listener
     */
    suspend fun removeListener(listener: RegistryListener) {
        mutex.withLock {
            listeners.remove(listener)
        }
    }

    private fun notifyListeners(event: RegistryEvent) {
        listeners.forEach { it.onRegistryEvent(event) }
    }

    /**
     * Clear all components (for testing)
     */
    internal suspend fun clear() {
        mutex.withLock {
            components.clear()
            pluginComponents.clear()
            listeners.clear()
        }
    }
}

/**
 * Registry event listener
 */
fun interface RegistryListener {
    fun onRegistryEvent(event: RegistryEvent)
}

/**
 * Registry events
 */
sealed class RegistryEvent {
    data class Registered(val type: String, val pluginId: String?) : RegistryEvent()
    data class Unregistered(val type: String) : RegistryEvent()
    data class PluginReloaded(val pluginId: String) : RegistryEvent()
}
