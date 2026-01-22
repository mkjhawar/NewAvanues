/**
 * PluginDiscovery.kt - Plugin Discovery Interface and Core Types
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the plugin discovery contract and related data types for the
 * Universal Plugin Architecture. Discovery sources implement this interface
 * to enable plugin loading from various sources (built-in, file system, remote).
 */
package com.augmentalis.magiccode.plugins.discovery

import com.augmentalis.magiccode.plugins.universal.PluginCapability
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import kotlinx.serialization.Serializable

/**
 * Interface for discovering plugins from various sources.
 *
 * Discovery sources scan their respective domains (built-in registry, file system,
 * network locations) for available plugins and provide mechanisms to load them.
 * Each source has a priority that determines the order in which sources are
 * queried and which source takes precedence for duplicate plugin IDs.
 *
 * ## Implementation Guidelines
 * - Discovery should be idempotent - calling [discoverPlugins] multiple times
 *   should return consistent results for unchanged plugins
 * - Loading should be lazy - [discoverPlugins] should not load plugin code,
 *   only metadata
 * - Errors during discovery of individual plugins should not prevent discovery
 *   of other plugins
 *
 * ## Priority Values
 * Lower priority values are checked first:
 * - 0: Built-in plugins (highest priority)
 * - 100: File system plugins
 * - 200: Remote/network plugins
 * - 1000+: Custom sources
 *
 * ## Usage Example
 * ```kotlin
 * class MyCustomDiscovery : PluginDiscovery {
 *     override val priority = 150
 *
 *     override suspend fun discoverPlugins(): List<PluginDescriptor> {
 *         return myCustomScan()
 *     }
 *
 *     override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
 *         return runCatching { instantiatePlugin(descriptor) }
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see PluginDescriptor
 * @see CompositePluginDiscovery
 */
interface PluginDiscovery {
    /**
     * Priority of this discovery source.
     *
     * Lower values indicate higher priority. When the same plugin ID is
     * discovered from multiple sources, the source with the lowest priority
     * value takes precedence.
     *
     * ## Standard Priority Values
     * - 0: Built-in plugins
     * - 100: File system plugins
     * - 200: Remote plugins
     * - 300: Android package plugins
     */
    val priority: Int

    /**
     * Discover available plugins from this source.
     *
     * Scans the source for available plugins and returns their descriptors.
     * This method should not load plugin code - only metadata and descriptors.
     *
     * @return List of plugin descriptors found, empty list if none found
     */
    suspend fun discoverPlugins(): List<PluginDescriptor>

    /**
     * Load a plugin from its descriptor.
     *
     * Instantiates and returns a plugin instance based on the provided descriptor.
     * The plugin should be in UNINITIALIZED state after loading - initialization
     * is handled separately by the plugin lifecycle manager.
     *
     * @param descriptor Plugin descriptor from discovery
     * @return Result containing the loaded plugin or error if loading failed
     */
    suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin>

    /**
     * Check if this source can load a specific plugin.
     *
     * Default implementation checks if the descriptor's source matches this
     * discovery source type.
     *
     * @param descriptor Plugin descriptor to check
     * @return true if this source can load the plugin
     */
    fun canLoad(descriptor: PluginDescriptor): Boolean = true

    /**
     * Refresh the discovery cache if applicable.
     *
     * For discovery sources that cache results, this method forces a refresh.
     * Default implementation does nothing.
     */
    suspend fun refresh() {}
}

/**
 * Descriptor for a discovered plugin.
 *
 * Contains all metadata about a plugin discovered by a [PluginDiscovery] source,
 * without actually loading the plugin code. Descriptors are used for:
 * - Displaying available plugins to users
 * - Dependency resolution
 * - Determining load order
 * - Plugin matching by capability
 *
 * ## Serialization
 * PluginDescriptor is serializable for caching and network transfer.
 * The [source] property uses a sealed class serializer.
 *
 * @property pluginId Unique plugin identifier in reverse-domain notation
 * @property name Human-readable plugin name for display
 * @property version Semantic version string (e.g., "1.2.3")
 * @property capabilities Set of capability IDs this plugin provides
 * @property source Source from which the plugin was discovered
 * @property metadata Additional metadata key-value pairs
 * @property description Optional plugin description
 * @property author Optional author/organization name
 * @property entrypoint Class or factory path for loading the plugin
 * @property dependencies List of plugin dependencies
 * @since 1.0.0
 * @see PluginSource
 * @see PluginCapability
 */
@Serializable
data class PluginDescriptor(
    val pluginId: String,
    val name: String,
    val version: String,
    val capabilities: Set<String>,
    val source: PluginSource,
    val metadata: Map<String, String> = emptyMap(),
    val description: String? = null,
    val author: String? = null,
    val entrypoint: String? = null,
    val dependencies: List<PluginDependencyDescriptor> = emptyList()
) {
    /**
     * Check if this plugin has a specific capability.
     *
     * @param capabilityId Capability ID to check
     * @return true if the plugin has the capability
     */
    fun hasCapability(capabilityId: String): Boolean {
        return capabilityId in capabilities
    }

    /**
     * Check if this plugin matches a capability filter.
     *
     * @param filter Capability ID or prefix to match
     * @return true if any capability matches the filter
     */
    fun matchesCapability(filter: String): Boolean {
        return capabilities.any { it == filter || it.startsWith("$filter.") }
    }

    /**
     * Get a metadata value.
     *
     * @param key Metadata key
     * @return Metadata value or null if not found
     */
    fun getMetadata(key: String): String? = metadata[key]

    /**
     * Create a capability set from the capability IDs.
     *
     * @return Set of PluginCapability instances
     */
    fun toCapabilities(): Set<PluginCapability> {
        return capabilities.map { capId ->
            PluginCapability(
                id = capId,
                name = capId.substringAfterLast('.').replace('-', ' ').replaceFirstChar { it.uppercase() },
                version = version
            )
        }.toSet()
    }

    companion object {
        /**
         * Create a descriptor from a plugin instance.
         *
         * Useful for registering built-in plugins.
         *
         * @param plugin Plugin instance
         * @param source Discovery source
         * @return PluginDescriptor for the plugin
         */
        fun fromPlugin(plugin: UniversalPlugin, source: PluginSource = PluginSource.Builtin): PluginDescriptor {
            return PluginDescriptor(
                pluginId = plugin.pluginId,
                name = plugin.pluginName,
                version = plugin.version,
                capabilities = plugin.capabilities.map { it.id }.toSet(),
                source = source,
                metadata = mapOf(
                    "capabilityCount" to plugin.capabilities.size.toString()
                )
            )
        }
    }
}

/**
 * Plugin dependency descriptor for discovery.
 *
 * Lightweight representation of a plugin dependency for dependency resolution
 * during discovery, before plugins are loaded.
 *
 * @property pluginId Required plugin ID
 * @property versionConstraint Semver version constraint (e.g., "^1.0.0", ">=2.0.0")
 * @property optional Whether this dependency is optional
 * @since 1.0.0
 */
@Serializable
data class PluginDependencyDescriptor(
    val pluginId: String,
    val versionConstraint: String = "*",
    val optional: Boolean = false
)

/**
 * Source from which a plugin was discovered.
 *
 * Sealed class representing the different types of plugin sources.
 * Each source type contains information needed to reload the plugin.
 *
 * ## Source Types
 * - [Builtin]: Compiled into the application, always available
 * - [FileSystem]: Loaded from local file system path
 * - [Remote]: Downloaded from a remote URL
 * - [AndroidPackage]: Loaded from an installed Android package
 *
 * @since 1.0.0
 * @see PluginDescriptor
 */
@Serializable
sealed class PluginSource {
    /**
     * Built-in plugin compiled into the application.
     *
     * Built-in plugins are always available and have the highest priority.
     * They are typically core system plugins or default implementations.
     */
    @Serializable
    data object Builtin : PluginSource() {
        override fun toString(): String = "Builtin"
    }

    /**
     * Plugin loaded from the local file system.
     *
     * File system plugins are discovered by scanning configured directories
     * for plugin manifests. Supports hot-reload when files change.
     *
     * @property path Absolute path to the plugin directory or manifest file
     * @property manifestPath Optional path to the manifest within the plugin directory
     */
    @Serializable
    data class FileSystem(
        val path: String,
        val manifestPath: String? = null
    ) : PluginSource() {
        override fun toString(): String = "FileSystem($path)"
    }

    /**
     * Plugin loaded from a remote URL.
     *
     * Remote plugins are downloaded and cached locally before loading.
     * Supports version checking for updates.
     *
     * @property url URL to the plugin package or manifest
     * @property checksum Optional SHA-256 checksum for verification
     * @property cacheKey Cache key for local storage
     */
    @Serializable
    data class Remote(
        val url: String,
        val checksum: String? = null,
        val cacheKey: String? = null
    ) : PluginSource() {
        override fun toString(): String = "Remote($url)"
    }

    /**
     * Plugin loaded from an installed Android package.
     *
     * Android package plugins are discovered by querying the Android
     * PackageManager for packages with specific intent filters or metadata.
     *
     * @property packageName Android package name
     * @property className Entry point Activity or Service class name
     * @property versionCode Android version code for the package
     */
    @Serializable
    data class AndroidPackage(
        val packageName: String,
        val className: String? = null,
        val versionCode: Long? = null
    ) : PluginSource() {
        override fun toString(): String = "AndroidPackage($packageName)"
    }

    /**
     * Get the display name for this source type.
     *
     * @return Human-readable source type name
     */
    fun displayName(): String = when (this) {
        is Builtin -> "Built-in"
        is FileSystem -> "File System"
        is Remote -> "Remote"
        is AndroidPackage -> "Android Package"
    }
}

/**
 * Result of plugin discovery operation.
 *
 * Aggregates discovery results including found plugins and any errors
 * encountered during discovery.
 *
 * @property plugins List of successfully discovered plugin descriptors
 * @property errors List of errors encountered during discovery
 * @property sourceType Type of discovery source
 * @property durationMs Duration of the discovery operation in milliseconds
 * @since 1.0.0
 */
@Serializable
data class DiscoveryResult(
    val plugins: List<PluginDescriptor>,
    val errors: List<DiscoveryError> = emptyList(),
    val sourceType: String = "",
    val durationMs: Long = 0
) {
    /**
     * Check if discovery completed without errors.
     */
    val isSuccessful: Boolean get() = errors.isEmpty()

    /**
     * Total number of plugins discovered.
     */
    val pluginCount: Int get() = plugins.size

    /**
     * Get plugins matching a capability filter.
     *
     * @param capabilityFilter Capability ID or prefix to match
     * @return List of matching plugin descriptors
     */
    fun filterByCapability(capabilityFilter: String): List<PluginDescriptor> {
        return plugins.filter { it.matchesCapability(capabilityFilter) }
    }

    companion object {
        /**
         * Empty discovery result.
         */
        val EMPTY = DiscoveryResult(emptyList())

        /**
         * Create a result from a single error.
         *
         * @param error Error that occurred during discovery
         * @return DiscoveryResult with the error
         */
        fun fromError(error: DiscoveryError): DiscoveryResult {
            return DiscoveryResult(emptyList(), listOf(error))
        }
    }
}

/**
 * Error that occurred during plugin discovery.
 *
 * @property message Human-readable error message
 * @property source The source where the error occurred
 * @property pluginId Plugin ID if the error relates to a specific plugin
 * @property cause Underlying exception message if available
 * @since 1.0.0
 */
@Serializable
data class DiscoveryError(
    val message: String,
    val source: String = "",
    val pluginId: String? = null,
    val cause: String? = null
) {
    companion object {
        /**
         * Create an error from an exception.
         *
         * @param exception The exception that occurred
         * @param source Source description
         * @param pluginId Optional plugin ID
         * @return DiscoveryError from the exception
         */
        fun fromException(
            exception: Throwable,
            source: String = "",
            pluginId: String? = null
        ): DiscoveryError {
            return DiscoveryError(
                message = exception.message ?: "Unknown error",
                source = source,
                pluginId = pluginId,
                cause = exception.cause?.message
            )
        }
    }
}
