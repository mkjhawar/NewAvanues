package com.augmentalis.avamagic.registry

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central registry for DSL components.
 *
 * Maps component type names to their descriptors and factory functions.
 */
class ComponentRegistry {
    private val mutex = Mutex()
    private val components = mutableMapOf<String, ComponentDescriptor>()

    /**
     * Register a component.
     */
    suspend fun register(descriptor: ComponentDescriptor) {
        mutex.withLock {
            components[descriptor.type] = descriptor
        }
    }

    /**
     * Get component descriptor.
     */
    suspend fun get(type: String): ComponentDescriptor? {
        return mutex.withLock {
            components[type]
        }
    }

    /**
     * Get all registered components.
     */
    suspend fun getAll(): List<ComponentDescriptor> {
        return mutex.withLock {
            components.values.toList()
        }
    }

    /**
     * Check if component is registered.
     */
    suspend fun isRegistered(type: String): Boolean {
        return mutex.withLock {
            components.containsKey(type)
        }
    }

    /**
     * Unregister a component.
     */
    suspend fun unregister(type: String): Boolean {
        return mutex.withLock {
            components.remove(type) != null
        }
    }

    companion object {
        private var instance: ComponentRegistry? = null

        fun getInstance(): ComponentRegistry {
            return instance ?: synchronized(this) {
                instance ?: ComponentRegistry().also { instance = it }
            }
        }
    }
}
