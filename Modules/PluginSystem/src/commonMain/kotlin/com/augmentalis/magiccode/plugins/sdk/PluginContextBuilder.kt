/**
 * PluginContextBuilder.kt - Builder for creating PluginContext instances
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides a fluent builder API for constructing PluginContext instances
 * with proper validation and platform-specific defaults.
 */
package com.augmentalis.magiccode.plugins.sdk

import com.augmentalis.magiccode.plugins.universal.PlatformInfo
import com.augmentalis.magiccode.plugins.universal.PluginContext

/**
 * Builder for creating PluginContext instances.
 *
 * Provides a fluent API for constructing PluginContext with validation
 * and sensible defaults. Supports platform-specific configuration through
 * convenience methods.
 *
 * ## Usage Example
 * ```kotlin
 * // Create a context for Android
 * val context = PluginContextBuilder.create()
 *     .appDataDir("/data/data/com.example.app/files")
 *     .cacheDir("/data/data/com.example.app/cache")
 *     .android()
 *     .osVersion("14.0")
 *     .deviceType("phone")
 *     .serviceRegistry(myServiceRegistry)
 *     .eventBus(myEventBus)
 *     .build()
 *
 * // Create a context for testing
 * val testContext = PluginContextBuilder.forTesting()
 *     .appDataDir("/tmp/test/data")
 *     .cacheDir("/tmp/test/cache")
 *     .build()
 * ```
 *
 * @since 1.0.0
 * @see PluginContext
 */
class PluginContextBuilder {

    private var appDataDir: String = ""
    private var cacheDir: String = ""
    private var serviceRegistry: Any? = null
    private var eventBus: Any? = null
    private var platform: String = "unknown"
    private var osVersion: String = ""
    private var deviceType: String = ""
    private var extras: MutableMap<String, String> = mutableMapOf()

    // =========================================================================
    // Required Properties
    // =========================================================================

    /**
     * Set the application data directory.
     *
     * This is the root directory for persistent plugin data storage.
     * Each plugin will get a subdirectory under this path.
     *
     * @param dir Absolute path to the data directory
     * @return This builder for chaining
     */
    fun appDataDir(dir: String) = apply {
        this.appDataDir = dir
    }

    /**
     * Set the cache directory.
     *
     * This is the root directory for temporary plugin data.
     * Cache contents may be cleared by the system.
     *
     * @param dir Absolute path to the cache directory
     * @return This builder for chaining
     */
    fun cacheDir(dir: String) = apply {
        this.cacheDir = dir
    }

    // =========================================================================
    // Service References
    // =========================================================================

    /**
     * Set the service registry reference.
     *
     * The service registry allows plugins to discover and access services
     * provided by other plugins or the host application.
     *
     * @param registry UniversalRPC ServiceRegistry instance
     * @return This builder for chaining
     */
    fun serviceRegistry(registry: Any) = apply {
        this.serviceRegistry = registry
    }

    /**
     * Set the event bus reference.
     *
     * The event bus allows plugins to publish and subscribe to events
     * for inter-plugin communication.
     *
     * @param bus PluginEventBus instance
     * @return This builder for chaining
     */
    fun eventBus(bus: Any) = apply {
        this.eventBus = bus
    }

    // =========================================================================
    // Platform Configuration
    // =========================================================================

    /**
     * Set the platform information.
     *
     * @param info PlatformInfo instance
     * @return This builder for chaining
     */
    fun platformInfo(info: PlatformInfo) = apply {
        this.platform = info.platform
        this.osVersion = info.osVersion
        this.deviceType = info.deviceType
        this.extras.putAll(info.extras)
    }

    /**
     * Configure for Android platform.
     *
     * @return This builder for chaining
     */
    fun android() = apply {
        this.platform = "android"
    }

    /**
     * Configure for iOS platform.
     *
     * @return This builder for chaining
     */
    fun ios() = apply {
        this.platform = "ios"
    }

    /**
     * Configure for desktop platform.
     *
     * @return This builder for chaining
     */
    fun desktop() = apply {
        this.platform = "desktop"
    }

    /**
     * Configure for web platform.
     *
     * @return This builder for chaining
     */
    fun web() = apply {
        this.platform = "web"
    }

    /**
     * Set the OS version.
     *
     * @param version OS version string (e.g., "14.0", "11.0.1")
     * @return This builder for chaining
     */
    fun osVersion(version: String) = apply {
        this.osVersion = version
    }

    /**
     * Set the device type.
     *
     * @param type Device type (e.g., "phone", "tablet", "desktop")
     * @return This builder for chaining
     */
    fun deviceType(type: String) = apply {
        this.deviceType = type
    }

    /**
     * Add an extra platform-specific property.
     *
     * @param key Property key
     * @param value Property value
     * @return This builder for chaining
     */
    fun extra(key: String, value: String) = apply {
        this.extras[key] = value
    }

    /**
     * Add multiple extra properties.
     *
     * @param extras Map of key-value pairs
     * @return This builder for chaining
     */
    fun extras(extras: Map<String, String>) = apply {
        this.extras.putAll(extras)
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Build the PluginContext.
     *
     * Validates that all required properties are set and constructs
     * the final PluginContext instance.
     *
     * @return Configured PluginContext
     * @throws IllegalArgumentException if required properties are missing
     */
    fun build(): PluginContext {
        require(appDataDir.isNotEmpty()) { "appDataDir is required" }
        require(cacheDir.isNotEmpty()) { "cacheDir is required" }

        val platformInfo = PlatformInfo(
            platform = platform,
            osVersion = osVersion,
            deviceType = deviceType,
            extras = extras.toMap()
        )

        return PluginContext(
            appDataDir = appDataDir,
            cacheDir = cacheDir,
            serviceRegistry = serviceRegistry ?: NoOpServiceRegistry,
            eventBus = eventBus ?: NoOpEventBus,
            platformInfo = platformInfo
        )
    }

    // =========================================================================
    // Companion Object
    // =========================================================================

    companion object {
        /**
         * Create a new PluginContextBuilder.
         *
         * @return New builder instance
         */
        fun create() = PluginContextBuilder()

        /**
         * Create a builder pre-configured for testing.
         *
         * Sets up defaults suitable for unit testing with no-op
         * service registry and event bus.
         *
         * @return Builder configured for testing
         */
        fun forTesting() = PluginContextBuilder().apply {
            appDataDir = "/tmp/plugin-test/data"
            cacheDir = "/tmp/plugin-test/cache"
            platform = "test"
        }

        /**
         * Create a builder pre-configured for Android.
         *
         * @param context Android Context (unused, for API symmetry)
         * @param dataDir Data directory path
         * @param cacheDir Cache directory path
         * @return Builder configured for Android
         */
        fun forAndroid(
            dataDir: String,
            cacheDir: String
        ) = PluginContextBuilder()
            .android()
            .appDataDir(dataDir)
            .cacheDir(cacheDir)

        /**
         * Create a builder pre-configured for iOS.
         *
         * @param documentsDir Documents directory path
         * @param cachesDir Caches directory path
         * @return Builder configured for iOS
         */
        fun forIOS(
            documentsDir: String,
            cachesDir: String
        ) = PluginContextBuilder()
            .ios()
            .appDataDir(documentsDir)
            .cacheDir(cachesDir)

        /**
         * Create a builder pre-configured for desktop.
         *
         * @param appDir Application data directory
         * @param cacheDir Cache directory
         * @return Builder configured for desktop
         */
        fun forDesktop(
            appDir: String,
            cacheDir: String
        ) = PluginContextBuilder()
            .desktop()
            .appDataDir(appDir)
            .cacheDir(cacheDir)

        // =====================================================================
        // No-Op Implementations
        // =====================================================================

        /**
         * No-operation service registry for testing and fallback.
         *
         * This object provides a placeholder when no real service registry
         * is available. It does not provide any services.
         */
        object NoOpServiceRegistry {
            override fun toString(): String = "NoOpServiceRegistry"
        }

        /**
         * No-operation event bus for testing and fallback.
         *
         * This object provides a placeholder when no real event bus
         * is available. Events published to it are discarded.
         */
        object NoOpEventBus {
            override fun toString(): String = "NoOpEventBus"
        }
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if the context is using the no-op service registry.
 *
 * @return true if no real service registry is configured
 */
fun PluginContext.hasNoOpServiceRegistry(): Boolean {
    return serviceRegistry === PluginContextBuilder.NoOpServiceRegistry
}

/**
 * Check if the context is using the no-op event bus.
 *
 * @return true if no real event bus is configured
 */
fun PluginContext.hasNoOpEventBus(): Boolean {
    return eventBus === PluginContextBuilder.NoOpEventBus
}

/**
 * Check if the context is configured for testing.
 *
 * @return true if using test configuration (no-op services)
 */
fun PluginContext.isTestContext(): Boolean {
    return hasNoOpServiceRegistry() && hasNoOpEventBus()
}
