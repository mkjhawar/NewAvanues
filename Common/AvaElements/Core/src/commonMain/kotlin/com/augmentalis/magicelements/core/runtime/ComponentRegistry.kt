package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * ComponentRegistry
 *
 * Thread-safe registry for component definitions from built-in and plugin sources.
 * Manages component lifecycle, lookup, and validation.
 *
 * Features:
 * - Thread-safe registration/unregistration
 * - Component type conflict detection
 * - Plugin-specific component tracking
 * - Factory method pattern for component creation
 * - Built-in component support
 */
object ComponentRegistry {
    private val mutex = Mutex()
    private val registry = mutableMapOf<String, RegisteredComponent>()
    private val componentsByPlugin = mutableMapOf<String, MutableSet<String>>()

    /**
     * Registered component with metadata
     */
    data class RegisteredComponent(
        val type: String,
        val definition: ComponentDefinition,
        val pluginId: String,
        val isBuiltIn: Boolean,
        val registeredAt: Long = Clock.System.now().toEpochMilliseconds()
    )

    /**
     * Register a component definition
     *
     * @param definition Component definition with factory and validator
     * @param pluginId Plugin ID (use "builtin" for built-in components)
     * @param isBuiltIn Whether this is a built-in component (default: false)
     * @throws ComponentRegistryException if component type already exists
     */
    suspend fun register(
        definition: ComponentDefinition,
        pluginId: String,
        isBuiltIn: Boolean = false
    ) {
        mutex.withLock {
            // Check for conflicts
            val existing = registry[definition.type]
            if (existing != null) {
                // Allow overriding if:
                // 1. Existing is NOT built-in AND new is from same plugin (plugin update)
                // 2. Existing is built-in AND new is from plugin (plugin override)
                val canOverride = when {
                    !existing.isBuiltIn && existing.pluginId == pluginId -> true // Plugin update
                    existing.isBuiltIn && !isBuiltIn -> true // Plugin overrides built-in
                    else -> false
                }

                if (!canOverride) {
                    throw ComponentRegistryException.TypeConflict(
                        type = definition.type,
                        existingPlugin = existing.pluginId,
                        newPlugin = pluginId
                    )
                }
            }

            // Validate component definition
            validateDefinition(definition)

            // Register component
            val registered = RegisteredComponent(
                type = definition.type,
                definition = definition,
                pluginId = pluginId,
                isBuiltIn = isBuiltIn
            )

            registry[definition.type] = registered

            // Track by plugin
            componentsByPlugin.getOrPut(pluginId) { mutableSetOf() }.add(definition.type)
        }
    }

    /**
     * Unregister a specific component type
     *
     * @param type Component type
     * @return true if component was found and unregistered
     */
    suspend fun unregister(type: String): Boolean {
        mutex.withLock {
            val removed = registry.remove(type)
            if (removed != null) {
                componentsByPlugin[removed.pluginId]?.remove(type)
                return true
            }
            return false
        }
    }

    /**
     * Unregister all components from a specific plugin
     *
     * @param pluginId Plugin ID
     * @return Number of components unregistered
     */
    suspend fun unregisterAll(pluginId: String): Int {
        mutex.withLock {
            val types = componentsByPlugin.remove(pluginId) ?: return 0
            types.forEach { type ->
                registry.remove(type)
            }
            return types.size
        }
    }

    /**
     * Get component definition by type
     *
     * @param type Component type
     * @return ComponentDefinition or null if not found
     */
    suspend fun get(type: String): ComponentDefinition? {
        mutex.withLock {
            return registry[type]?.definition
        }
    }

    /**
     * Get registered component with metadata
     *
     * @param type Component type
     * @return RegisteredComponent or null if not found
     */
    suspend fun getRegistered(type: String): RegisteredComponent? {
        mutex.withLock {
            return registry[type]
        }
    }

    /**
     * Create a component instance from configuration
     *
     * @param config Component configuration with type and properties
     * @return Component instance
     * @throws ComponentRegistryException.NotFound if component type not registered
     * @throws ComponentRegistryException.ValidationFailed if config validation fails
     */
    suspend fun create(config: ComponentConfig): Component {
        val registered = mutex.withLock {
            registry[config.type] ?: throw ComponentRegistryException.NotFound(config.type)
        }

        // Validate configuration if validator exists
        registered.definition.validator?.let { validator ->
            val result = validator.validate(config)
            if (!result.isValid) {
                throw ComponentRegistryException.ValidationFailed(
                    type = config.type,
                    errors = result.errors
                )
            }
        }

        // Create component using factory
        return try {
            registered.definition.factory.create(config)
        } catch (e: Exception) {
            throw ComponentRegistryException.CreationFailed(
                type = config.type,
                cause = e
            )
        }
    }

    /**
     * Check if component type is registered
     *
     * @param type Component type
     * @return true if registered
     */
    suspend fun isRegistered(type: String): Boolean {
        mutex.withLock {
            return registry.containsKey(type)
        }
    }

    /**
     * Get all registered component types
     *
     * @return Set of component types
     */
    suspend fun getAllTypes(): Set<String> {
        mutex.withLock {
            return registry.keys.toSet()
        }
    }

    /**
     * Get all components from a specific plugin
     *
     * @param pluginId Plugin ID
     * @return List of RegisteredComponent
     */
    suspend fun getByPlugin(pluginId: String): List<RegisteredComponent> {
        mutex.withLock {
            val types = componentsByPlugin[pluginId] ?: return emptyList()
            return types.mapNotNull { registry[it] }
        }
    }

    /**
     * Get all built-in components
     *
     * @return List of RegisteredComponent
     */
    suspend fun getBuiltIn(): List<RegisteredComponent> {
        mutex.withLock {
            return registry.values.filter { it.isBuiltIn }
        }
    }

    /**
     * Get all plugin components (non-built-in)
     *
     * @return List of RegisteredComponent
     */
    suspend fun getPluginComponents(): List<RegisteredComponent> {
        mutex.withLock {
            return registry.values.filter { !it.isBuiltIn }
        }
    }

    /**
     * Get registry statistics
     *
     * @return RegistryStats
     */
    suspend fun getStats(): RegistryStats {
        mutex.withLock {
            val builtInCount = registry.values.count { it.isBuiltIn }
            val pluginCount = registry.values.count { !it.isBuiltIn }

            return RegistryStats(
                total = registry.size,
                builtIn = builtInCount,
                plugin = pluginCount,
                pluginCount = componentsByPlugin.size
            )
        }
    }

    /**
     * Clear all registrations (use with caution!)
     *
     * @param includeBuiltIn Whether to also clear built-in components (default: false)
     */
    suspend fun clear(includeBuiltIn: Boolean = false) {
        mutex.withLock {
            if (includeBuiltIn) {
                registry.clear()
                componentsByPlugin.clear()
            } else {
                // Only clear plugin components
                val toRemove = registry.values.filter { !it.isBuiltIn }.map { it.type }
                toRemove.forEach { type ->
                    val removed = registry.remove(type)
                    removed?.let { componentsByPlugin[it.pluginId]?.remove(type) }
                }
            }
        }
    }

    /**
     * Validate component definition
     */
    private fun validateDefinition(definition: ComponentDefinition) {
        require(definition.type.isNotBlank()) {
            "Component type cannot be blank"
        }

        require(definition.type.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
            "Component type must start with letter and contain only alphanumeric and underscore"
        }

        // Validate schema if present
        definition.schema?.let { schema ->
            require(schema.properties.isNotEmpty()) {
                "Component schema must define at least one property"
            }

            // Validate required properties exist in schema
            schema.requiredProperties.forEach { required ->
                require(required in schema.properties) {
                    "Required property '$required' not found in schema"
                }
            }
        }
    }

    /**
     * Batch register components
     *
     * @param definitions List of component definitions
     * @param pluginId Plugin ID
     * @param isBuiltIn Whether these are built-in components
     * @return Number of successfully registered components
     */
    suspend fun registerBatch(
        definitions: List<ComponentDefinition>,
        pluginId: String,
        isBuiltIn: Boolean = false
    ): Int {
        var successCount = 0
        definitions.forEach { definition ->
            try {
                register(definition, pluginId, isBuiltIn)
                successCount++
            } catch (e: ComponentRegistryException) {
                // Log error but continue with other components
                println("Failed to register ${definition.type}: ${e.message}")
            }
        }
        return successCount
    }

    /**
     * Registry statistics
     */
    data class RegistryStats(
        val total: Int,
        val builtIn: Int,
        val plugin: Int,
        val pluginCount: Int
    )
}

/**
 * ComponentRegistry exceptions
 */
sealed class ComponentRegistryException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Component type already registered
     */
    class TypeConflict(
        val type: String,
        val existingPlugin: String,
        val newPlugin: String
    ) : ComponentRegistryException(
        "Component type '$type' already registered by plugin '$existingPlugin' (attempted by '$newPlugin')"
    )

    /**
     * Component type not found
     */
    class NotFound(
        val type: String
    ) : ComponentRegistryException("Component type '$type' not registered")

    /**
     * Component validation failed
     */
    class ValidationFailed(
        val type: String,
        val errors: List<ValidationError>
    ) : ComponentRegistryException(
        "Component '$type' validation failed: ${errors.joinToString { it.message }}"
    )

    /**
     * Component creation failed
     */
    class CreationFailed(
        val type: String,
        cause: Throwable
    ) : ComponentRegistryException("Failed to create component '$type'", cause)
}
