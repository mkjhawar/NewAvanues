package com.augmentalis.avaelements.core

import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Serializable

/**
 * AvaElements Plugin System
 *
 * Enables dynamic loading of user-created components without recompilation.
 * Plugins are sandboxed and validated for security.
 */

/**
 * Base plugin interface for AvaElements
 */
interface MagicElementPlugin {
    /**
     * Unique plugin identifier (e.g., "com.example.custombutton")
     */
    val id: String

    /**
     * Plugin metadata
     */
    val metadata: PluginMetadata

    /**
     * Get all components provided by this plugin
     */
    fun getComponents(): List<ComponentDefinition>

    /**
     * Called when plugin is loaded
     */
    fun onLoad() {}

    /**
     * Called when plugin is unloaded
     */
    fun onUnload() {}
}

/**
 * Plugin metadata
 */
@Serializable
data class PluginMetadata(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val minSdkVersion: String,
    val permissions: Set<Permission> = emptySet(),
    val dependencies: List<String> = emptyList()
) {
    init {
        require(id.isNotBlank()) { "Plugin ID cannot be blank" }
        require(version.matches(Regex("\\d+\\.\\d+\\.\\d+"))) { "Version must be in format X.Y.Z" }
        require(minSdkVersion.matches(Regex("\\d+\\.\\d+\\.\\d+"))) { "SDK version must be in format X.Y.Z" }
    }
}

/**
 * Component definition within a plugin
 */
data class ComponentDefinition(
    val type: String,
    val factory: ComponentFactory,
    val validator: ComponentValidator? = null,
    val schema: ComponentSchema? = null
)

/**
 * Factory for creating component instances
 */
fun interface ComponentFactory {
    fun create(config: ComponentConfig): Component
}

/**
 * Validator for component configuration
 */
fun interface ComponentValidator {
    fun validate(config: ComponentConfig): ValidationResult
}

/**
 * Component schema definition
 */
@Serializable
data class ComponentSchema(
    val properties: Map<String, PropertySchema>,
    val requiredProperties: Set<String> = emptySet()
)

/**
 * Property schema definition
 */
@Serializable
data class PropertySchema(
    val type: PropertyType,
    val description: String? = null,
    val defaultValue: String? = null,
    val allowedValues: List<String>? = null
)

/**
 * Property types
 */
@Serializable
enum class PropertyType {
    STRING,
    NUMBER,
    BOOLEAN,
    COLOR,
    SIZE,
    LIST,
    OBJECT
}

/**
 * Component configuration
 */
data class ComponentConfig(
    val id: String,
    val type: String,
    val properties: Map<String, Any?> = emptyMap(),
    val style: ComponentStyle? = null,
    val modifiers: List<Modifier> = emptyList()
) {
    inline fun <reified T> get(key: String): T? {
        return properties[key] as? T
    }

    inline fun <reified T> getOrDefault(key: String, default: T): T {
        return get(key) ?: default
    }
}

/**
 * Validation result
 */
@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(isValid = true)
        fun failure(vararg errors: ValidationError) = ValidationResult(isValid = false, errors.toList())
        fun failure(errors: List<ValidationError>) = ValidationResult(isValid = false, errors)
    }
}

/**
 * Validation error
 */
@Serializable
data class ValidationError(
    val field: String? = null,
    val message: String,
    val code: String? = null
)

/**
 * Plugin permissions
 */
@Serializable
enum class Permission {
    /**
     * Read theme colors and styles
     */
    READ_THEME,

    /**
     * Read user preferences
     */
    READ_USER_PREFERENCES,

    /**
     * Show notifications
     */
    SHOW_NOTIFICATION,

    /**
     * Access clipboard
     */
    ACCESS_CLIPBOARD;

    companion object {
        /**
         * Permissions that are always denied (blacklisted)
         */
        val BLACKLISTED = setOf<Permission>(
            // Network access
            // File system access
            // System APIs
        )
    }
}

/**
 * Plugin source
 */
sealed class PluginSource {
    data class File(val path: String) : PluginSource()
    data class Data(val content: String, val format: Format) : PluginSource() {
        enum class Format {
            YAML,
            JSON,
            KOTLIN
        }
    }
    data class Remote(val url: String) : PluginSource()

    val requestedPermissions: Set<Permission>
        get() = emptySet() // Will be extracted from plugin manifest
}

/**
 * Plugin handle for management
 */
data class PluginHandle(
    val id: String,
    val plugin: MagicElementPlugin,
    val sandbox: Any? = null // Platform-specific sandbox environment
)

/**
 * Plugin exceptions
 */
sealed class PluginException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class LoadException(message: String, cause: Throwable? = null) : PluginException(message, cause)
    class ValidationException(val errors: List<ValidationError>) : PluginException("Validation failed: ${errors.joinToString { it.message }}")
    class SecurityException(message: String) : PluginException(message)
    class NotFound(val pluginId: String) : PluginException("Plugin not found: $pluginId")
}
