package com.augmentalis.avamagic.lifecycle

/**
 * Manages app resources and cleanup.
 */
class ResourceManager {
    private val resources = mutableMapOf<String, ManagedResource>()

    /**
     * Register a resource.
     */
    fun register(id: String, resource: ManagedResource) {
        resources[id] = resource
    }

    /**
     * Get a resource.
     */
    fun get(id: String): ManagedResource? {
        return resources[id]
    }

    /**
     * Release a specific resource.
     */
    suspend fun release(id: String) {
        resources[id]?.let { resource ->
            resource.release()
            resources.remove(id)
        }
    }

    /**
     * Release all resources.
     */
    suspend fun releaseAll() {
        resources.values.forEach { it.release() }
        resources.clear()
    }

    /**
     * Get resource count.
     */
    fun count(): Int = resources.size
}

interface ManagedResource {
    suspend fun release()
}

/**
 * Simple resource wrapper.
 */
class SimpleResource(
    private val onRelease: suspend () -> Unit
) : ManagedResource {
    override suspend fun release() {
        onRelease()
    }
}
