package com.augmentalis.avacode.plugins.core

/**
 * Base exception for all plugin-related errors.
 *
 * Serves as the root of the plugin exception hierarchy. All plugin-specific
 * exceptions extend this class, allowing catch blocks to handle all plugin
 * errors uniformly when needed.
 *
 * ## Exception Hierarchy
 * - [PluginException] (base)
 *   - [ManifestInvalidException] - Manifest parsing/validation errors
 *   - [ManifestNotFoundException] - Missing manifest file
 *   - [DependencyUnresolvedException] - Dependency resolution failures
 *   - [CircularDependencyException] - Circular dependency detection
 *   - [SignatureInvalidException] - Signature verification failures
 *   - [PermissionDeniedException] - Permission request denials
 *   - [AssetNotFoundException] - Missing asset files
 *   - [CategoryNotDeclaredException] - Undeclared asset category usage
 *   - [ThemeInvalidException] - Theme loading/validation errors
 *   - [InstallationFailedException] - Installation failures
 *   - [UpdateFailedException] - Update failures
 *   - [UninstallFailedException] - Uninstall failures
 *   - [RollbackFailedException] - Rollback failures
 *   - [PluginSizeExceededException] - Size limit violations
 *   - [NamespaceCollisionException] - Namespace conflicts
 *   - [PluginNotFoundException] - Plugin lookup failures
 *
 * ## Usage
 * ```kotlin
 * try {
 *     loader.loadPlugin(...)
 * } catch (e: PluginException) {
 *     // Handle any plugin-related error
 *     logger.error("Plugin error: ${e.message}", e)
 * }
 * ```
 *
 * @param message Error message describing what went wrong
 * @param cause Original exception that caused this error (optional)
 * @since 1.0.0
 */
open class PluginException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when plugin manifest is invalid or cannot be parsed.
 *
 * This exception is raised when:
 * - Manifest YAML syntax is invalid
 * - Required fields are missing
 * - Field values fail validation rules
 * - Schema validation fails
 *
 * @param message Error message describing the validation failure
 * @param cause Original exception (e.g., YAML parsing error)
 * @since 1.0.0
 * @see ManifestValidator
 */
class ManifestInvalidException(
    message: String,
    cause: Throwable? = null
) : PluginException(message, cause)

/**
 * Exception thrown when plugin manifest file cannot be found.
 *
 * Indicates that the plugin.yaml file is missing from the expected location.
 *
 * @param pluginId Plugin identifier or manifest path that was not found
 * @since 1.0.0
 */
class ManifestNotFoundException(
    pluginId: String
) : PluginException("Plugin manifest not found for: $pluginId")

/**
 * Exception thrown when a plugin dependency cannot be resolved.
 *
 * This occurs when:
 * - Required plugin is not installed
 * - Installed version doesn't satisfy version constraint
 * - Dependency plugin failed to load
 *
 * @param pluginId The plugin that has the unresolved dependency
 * @param dependency The dependency plugin ID that couldn't be resolved
 * @param message Additional details about the resolution failure
 * @since 1.0.0
 * @see PluginDependency
 */
class DependencyUnresolvedException(
    pluginId: String,
    dependency: String,
    message: String
) : PluginException("Cannot resolve dependency '$dependency' for plugin '$pluginId': $message")

/**
 * Exception thrown when circular dependency is detected.
 *
 * Circular dependencies are not allowed as they prevent proper plugin
 * initialization order. The cycle shows the chain of plugins that depend
 * on each other in a loop.
 *
 * @param cycle List of plugin IDs forming the circular dependency chain
 * @since 1.0.0
 * @see PluginDependency
 */
class CircularDependencyException(
    cycle: List<String>
) : PluginException("Circular dependency detected: ${cycle.joinToString(" -> ")}")

/**
 * Exception thrown when plugin signature verification fails.
 *
 * Indicates that the plugin's cryptographic signature is invalid or
 * cannot be verified, which may suggest tampering or corruption.
 *
 * @param pluginId Plugin identifier
 * @param message Details about the signature verification failure
 * @since 1.0.0
 */
class SignatureInvalidException(
    pluginId: String,
    message: String
) : PluginException("Invalid signature for plugin '$pluginId': $message")

/**
 * Exception thrown when a permission request is denied.
 *
 * Raised when a plugin requests a permission that the user has denied,
 * either explicitly or through policy restrictions.
 *
 * @param pluginId Plugin identifier requesting the permission
 * @param permission The permission that was denied
 * @since 1.0.0
 * @see Permission
 */
class PermissionDeniedException(
    pluginId: String,
    permission: Permission
) : PluginException("Permission $permission denied for plugin '$pluginId'")

/**
 * Exception thrown when an asset cannot be found or resolved.
 *
 * Indicates that a plugin requested an asset that doesn't exist or
 * cannot be accessed at the expected location.
 *
 * @param uri Asset URI that was not found
 * @param message Optional additional details about the failure
 * @since 1.0.0
 * @see PluginAssets
 */
class AssetNotFoundException(
    uri: String,
    message: String? = null
) : PluginException("Asset not found: $uri${message?.let { " - $it" } ?: ""}")

/**
 * Exception thrown when an asset category is not declared in the manifest.
 *
 * Plugins must declare asset categories in their manifest before using them.
 * This prevents plugins from accessing undeclared resources.
 *
 * @param pluginId Plugin identifier
 * @param category Asset category that was not declared
 * @since 1.0.0
 * @see PluginAssets
 */
class CategoryNotDeclaredException(
    pluginId: String,
    category: AssetCategory
) : PluginException("Asset category $category not declared in manifest for plugin '$pluginId'")

/**
 * Exception thrown when theme loading or validation fails.
 *
 * Raised when a theme file cannot be parsed, is missing required fields,
 * or violates theme schema rules.
 *
 * @param themeId Theme identifier
 * @param message Details about the theme error
 * @param cause Original exception (e.g., YAML parsing error)
 * @since 1.0.0
 */
class ThemeInvalidException(
    themeId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Invalid theme '$themeId': $message", cause)

/**
 * Exception thrown when plugin installation fails.
 *
 * Covers failures during any part of the installation process including
 * validation, file copying, namespace creation, or registry updates.
 *
 * @param pluginId Plugin identifier
 * @param message Details about the installation failure
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 * @see PluginLoader.loadPlugin
 */
class InstallationFailedException(
    pluginId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Failed to install plugin '$pluginId': $message", cause)

/**
 * Exception thrown when plugin update fails.
 *
 * Indicates that an attempt to update a plugin to a new version failed.
 * The plugin should remain at its previous version.
 *
 * @param pluginId Plugin identifier
 * @param message Details about the update failure
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 */
class UpdateFailedException(
    pluginId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Failed to update plugin '$pluginId': $message", cause)

/**
 * Exception thrown when plugin uninstallation fails.
 *
 * Indicates that cleanup operations failed during uninstall. The plugin
 * may be left in an inconsistent state.
 *
 * @param pluginId Plugin identifier
 * @param message Details about the uninstall failure
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 * @see PluginLoader.uninstallPlugin
 */
class UninstallFailedException(
    pluginId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Failed to uninstall plugin '$pluginId': $message", cause)

/**
 * Exception thrown when a rollback operation fails.
 *
 * Indicates that attempting to restore a previous checkpoint state failed.
 * This is a critical error as it may leave the system in an inconsistent state.
 *
 * @param checkpointId Checkpoint identifier that failed to restore
 * @param message Details about the rollback failure
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 */
class RollbackFailedException(
    checkpointId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Failed to rollback to checkpoint '$checkpointId': $message", cause)

/**
 * Exception thrown when a plugin exceeds configured size limits.
 *
 * Prevents plugins from consuming excessive storage space. Size limits
 * are configured in [PluginConfig].
 *
 * @param pluginId Plugin identifier
 * @param actualSize Actual plugin size in bytes
 * @param maxSize Maximum allowed size in bytes
 * @since 1.0.0
 * @see PluginConfig
 */
class PluginSizeExceededException(
    pluginId: String,
    actualSize: Long,
    maxSize: Long
) : PluginException(
    "Plugin '$pluginId' size ($actualSize bytes) exceeds maximum allowed ($maxSize bytes)"
)

/**
 * Exception thrown when a plugin namespace collision occurs.
 *
 * Indicates that a plugin is attempting to use a namespace that is already
 * in use by another plugin. Each plugin must have a unique namespace.
 *
 * @param pluginId Plugin identifier that has a namespace collision
 * @since 1.0.0
 * @see PluginNamespace
 */
class NamespaceCollisionException(
    pluginId: String
) : PluginException("Plugin namespace collision detected for: $pluginId")

/**
 * Exception thrown when a plugin cannot be found in the registry.
 *
 * Indicates that an operation was attempted on a plugin ID that is not
 * currently registered in the system.
 *
 * @param pluginId Plugin identifier that was not found
 * @since 1.0.0
 * @see PluginRegistry
 */
class PluginNotFoundException(
    pluginId: String
) : PluginException("Plugin not found: $pluginId")

/**
 * Exception thrown for runtime plugin errors.
 *
 * Covers errors that occur during plugin execution, such as:
 * - Plugin initialization failures
 * - Invalid plugin state
 * - Resource acquisition failures
 *
 * @param pluginId Plugin identifier
 * @param message Details about the runtime error
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 */
class PluginRuntimeException(
    pluginId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Runtime error in plugin '$pluginId': $message", cause)

/**
 * Exception thrown when a security violation is detected.
 *
 * Raised when a plugin attempts to perform an operation that violates
 * security policies, such as:
 * - Accessing resources without permission
 * - Attempting to escape sandbox
 * - Signature verification failure
 *
 * @param pluginId Plugin identifier
 * @param message Details about the security violation
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 */
class SecurityViolationException(
    pluginId: String,
    message: String,
    cause: Throwable? = null
) : PluginException("Security violation in plugin '$pluginId': $message", cause)

/**
 * Exception thrown when a transaction operation fails.
 *
 * Indicates that a checkpoint-based transaction failed to complete,
 * which may require rollback to a previous state.
 *
 * @param message Details about the transaction failure
 * @param cause Original exception that caused the failure
 * @since 1.0.0
 */
class TransactionFailedException(
    message: String,
    cause: Throwable? = null
) : PluginException("Transaction failed: $message", cause)
