package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*

/**
 * Plugin Manager
 *
 * Manages plugin lifecycle: loading, validation, sandboxing, and unloading.
 * Ensures plugins are secure and do not interfere with each other.
 */
class PluginManager(
    private val sandbox: SecuritySandbox = SecuritySandbox()
) {
    private val plugins = mutableMapOf<String, PluginHandle>()
    private val pluginLoader = PluginLoader()

    /**
     * Load a plugin from a source
     *
     * @param source Plugin source (file, data, or remote)
     * @return Result containing PluginHandle on success or exception on failure
     */
    suspend fun loadPlugin(source: PluginSource): Result<PluginHandle> {
        return try {
            // Step 1: Load plugin metadata and code
            val plugin = pluginLoader.load(source)

            // Step 2: Validate plugin
            val validation = validatePlugin(plugin)
            if (!validation.isValid) {
                return Result.failure(PluginException.ValidationException(validation.errors))
            }

            // Step 3: Check for conflicts
            if (plugins.containsKey(plugin.id)) {
                return Result.failure(PluginException.LoadException("Plugin '${plugin.id}' is already loaded"))
            }

            // Step 4: Create sandboxed environment
            val sandboxedEnvironment = sandbox.createIsolatedEnvironment(
                pluginId = plugin.id,
                permissions = plugin.metadata.permissions,
                resourceLimits = ResourceLimits.default()
            )

            // Step 5: Register components
            plugin.getComponents().forEach { definition ->
                ComponentRegistry.register(definition, pluginId = plugin.id)
            }

            // Step 6: Call plugin lifecycle
            plugin.onLoad()

            // Step 7: Create and store handle
            val handle = PluginHandle(
                id = plugin.id,
                plugin = plugin,
                sandbox = sandboxedEnvironment
            )

            plugins[plugin.id] = handle

            Result.success(handle)

        } catch (e: Exception) {
            Result.failure(PluginException.LoadException("Failed to load plugin", e))
        }
    }

    /**
     * Unload a plugin
     *
     * @param pluginId Plugin ID
     * @return true if plugin was found and unloaded
     */
    suspend fun unloadPlugin(pluginId: String): Boolean {
        val handle = plugins.remove(pluginId) ?: return false

        try {
            // Unregister components
            ComponentRegistry.unregisterAll(pluginId)

            // Call plugin lifecycle
            handle.plugin.onUnload()

            // Destroy sandbox
            sandbox.destroy(pluginId)

            return true
        } catch (e: Exception) {
            // Log error but consider plugin unloaded
            println("Error unloading plugin $pluginId: ${e.message}")
            return true
        }
    }

    /**
     * Reload a plugin (unload then load)
     *
     * @param pluginId Plugin ID
     * @param source New plugin source
     * @return Result containing new PluginHandle
     */
    suspend fun reloadPlugin(pluginId: String, source: PluginSource): Result<PluginHandle> {
        unloadPlugin(pluginId)
        return loadPlugin(source)
    }

    /**
     * Get plugin handle by ID
     */
    fun getPlugin(pluginId: String): PluginHandle? {
        return plugins[pluginId]
    }

    /**
     * Get all loaded plugins
     */
    fun getAllPlugins(): List<PluginHandle> {
        return plugins.values.toList()
    }

    /**
     * Check if plugin is loaded
     */
    fun isLoaded(pluginId: String): Boolean {
        return plugins.containsKey(pluginId)
    }

    /**
     * Validate plugin before loading
     */
    private fun validatePlugin(plugin: MagicElementPlugin): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate metadata
        if (plugin.id.isBlank()) {
            errors.add(ValidationError(field = "id", message = "Plugin ID cannot be blank"))
        }

        // Validate permissions
        val blacklisted = plugin.metadata.permissions.filter { it in Permission.BLACKLISTED }
        if (blacklisted.isNotEmpty()) {
            errors.add(ValidationError(
                field = "permissions",
                message = "Blacklisted permissions requested: ${blacklisted.joinToString()}"
            ))
        }

        // Validate components
        val components = try {
            plugin.getComponents()
        } catch (e: Exception) {
            errors.add(ValidationError(
                field = "components",
                message = "Failed to get components: ${e.message}"
            ))
            return ValidationResult.failure(errors)
        }

        if (components.isEmpty()) {
            errors.add(ValidationError(
                field = "components",
                message = "Plugin must provide at least one component"
            ))
        }

        // Validate component types are unique
        val duplicateTypes = components.groupBy { it.type }
            .filter { it.value.size > 1 }
            .keys

        if (duplicateTypes.isNotEmpty()) {
            errors.add(ValidationError(
                field = "components",
                message = "Duplicate component types: ${duplicateTypes.joinToString()}"
            ))
        }

        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }

    /**
     * Unload all plugins
     */
    suspend fun unloadAll() {
        plugins.keys.toList().forEach { pluginId ->
            unloadPlugin(pluginId)
        }
    }
}
