package com.augmentalis.magiccode.plugins.universal

import kotlinx.serialization.Serializable

/**
 * Type alias for UniversalPlugin for cleaner API.
 */
typealias Plugin = UniversalPlugin

/**
 * Configuration data for plugin initialization.
 *
 * Contains settings, secrets, and feature flags passed to a plugin
 * during initialization. Secrets are expected to be encrypted at rest
 * and handled securely by the plugin system.
 *
 * ## Usage
 * ```kotlin
 * val config = PluginConfig(
 *     settings = mapOf(
 *         "model" to "gpt-4",
 *         "maxTokens" to "1000",
 *         "timeout" to "30000"
 *     ),
 *     secrets = mapOf(
 *         "apiKey" to "encrypted:xxxxx"
 *     ),
 *     features = setOf("streaming", "function-calling")
 * )
 * ```
 *
 * @property settings Plugin-specific configuration settings
 * @property secrets Encrypted secrets (API keys, tokens, etc.)
 * @property features Enabled feature flags for this plugin instance
 * @since 1.0.0
 * @see UniversalPlugin.initialize
 */
@Serializable
data class PluginConfig(
    val settings: Map<String, String> = emptyMap(),
    val secrets: Map<String, String> = emptyMap(),
    val features: Set<String> = emptySet()
) {
    /**
     * Get a setting value with optional default.
     *
     * @param key Setting key
     * @param default Default value if key not found
     * @return Setting value or default
     */
    fun getSetting(key: String, default: String = ""): String {
        return settings[key] ?: default
    }

    /**
     * Get a setting as Int.
     *
     * @param key Setting key
     * @param default Default value if key not found or not parseable
     * @return Setting value as Int or default
     */
    fun getInt(key: String, default: Int = 0): Int {
        return settings[key]?.toIntOrNull() ?: default
    }

    /**
     * Get a setting as Long.
     *
     * @param key Setting key
     * @param default Default value if key not found or not parseable
     * @return Setting value as Long or default
     */
    fun getLong(key: String, default: Long = 0L): Long {
        return settings[key]?.toLongOrNull() ?: default
    }

    /**
     * Get a setting as Boolean.
     *
     * @param key Setting key
     * @param default Default value if key not found
     * @return Setting value as Boolean or default
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return settings[key]?.toBooleanStrictOrNull() ?: default
    }

    /**
     * Check if a feature is enabled.
     *
     * @param feature Feature flag name
     * @return true if feature is enabled
     */
    fun hasFeature(feature: String): Boolean = feature in features

    /**
     * Get a secret value. Handle with care.
     *
     * @param key Secret key
     * @return Secret value or null if not found
     */
    fun getSecret(key: String): String? = secrets[key]

    companion object {
        /** Empty configuration */
        val EMPTY = PluginConfig()

        /**
         * Create configuration from settings only.
         *
         * @param settings Configuration settings map
         * @return New PluginConfig with only settings
         */
        fun fromSettings(settings: Map<String, String>): PluginConfig {
            return PluginConfig(settings = settings)
        }
    }
}

/**
 * Runtime context provided to plugins during initialization.
 *
 * Contains platform-specific paths, service references, and the event bus
 * for inter-plugin communication. This context remains valid for the
 * lifetime of the plugin.
 *
 * ## Thread Safety
 * The context is immutable after creation. Service references (eventBus,
 * serviceRegistry) are thread-safe.
 *
 * @property appDataDir Path to application's data directory for persistent storage
 * @property cacheDir Path to cache directory for temporary data
 * @property serviceRegistry Reference to the UniversalRPC ServiceRegistry for service discovery
 * @property eventBus Reference to the plugin event bus for pub/sub communication
 * @property platformInfo Platform-specific information (OS, version, etc.)
 * @since 1.0.0
 * @see UniversalPlugin.initialize
 */
data class PluginContext(
    val appDataDir: String,
    val cacheDir: String,
    val serviceRegistry: Any,
    val eventBus: Any,
    val platformInfo: PlatformInfo = PlatformInfo.UNKNOWN
) {
    /**
     * Get the data directory for a specific plugin.
     *
     * @param pluginId Plugin identifier
     * @return Path to plugin-specific data directory
     */
    fun getPluginDataDir(pluginId: String): String {
        return "$appDataDir/plugins/$pluginId"
    }

    /**
     * Get the cache directory for a specific plugin.
     *
     * @param pluginId Plugin identifier
     * @return Path to plugin-specific cache directory
     */
    fun getPluginCacheDir(pluginId: String): String {
        return "$cacheDir/plugins/$pluginId"
    }
}

/**
 * Platform information for plugin context.
 *
 * @property platform Platform identifier (android, ios, desktop)
 * @property osVersion Operating system version string
 * @property deviceType Device type (phone, tablet, desktop)
 * @property extras Additional platform-specific information
 */
@Serializable
data class PlatformInfo(
    val platform: String,
    val osVersion: String = "",
    val deviceType: String = "",
    val extras: Map<String, String> = emptyMap()
) {
    companion object {
        val UNKNOWN = PlatformInfo(platform = "unknown")
        val ANDROID = PlatformInfo(platform = "android")
        val IOS = PlatformInfo(platform = "ios")
        val DESKTOP = PlatformInfo(platform = "desktop")
    }
}

/**
 * Result of plugin initialization.
 *
 * A sealed class representing either successful initialization or failure.
 * On failure, indicates whether the error is recoverable.
 *
 * ## Usage
 * ```kotlin
 * when (val result = plugin.initialize(config, context)) {
 *     is InitResult.Success -> println("Initialized: ${result.message}")
 *     is InitResult.Failure -> {
 *         if (result.recoverable) {
 *             // Retry initialization
 *         } else {
 *             // Mark plugin as FAILED
 *         }
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see UniversalPlugin.initialize
 */
sealed class InitResult {
    /**
     * Successful initialization.
     *
     * @property message Human-readable success message
     * @property metadata Optional metadata about the initialization
     */
    data class Success(
        val message: String = "Initialized successfully",
        val metadata: Map<String, String> = emptyMap()
    ) : InitResult()

    /**
     * Failed initialization.
     *
     * @property error The exception that caused the failure
     * @property recoverable Whether the plugin can attempt reinitialization
     * @property diagnostics Additional diagnostic information
     */
    data class Failure(
        val error: Throwable,
        val recoverable: Boolean = true,
        val diagnostics: Map<String, String> = emptyMap()
    ) : InitResult() {
        /**
         * Get a human-readable error message.
         */
        val message: String get() = error.message ?: "Unknown error"
    }

    /**
     * Check if initialization was successful.
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if initialization failed.
     */
    fun isFailure(): Boolean = this is Failure

    companion object {
        /**
         * Create a success result.
         *
         * @param message Success message
         * @return Success result
         */
        fun success(message: String = "Initialized successfully"): InitResult {
            return Success(message)
        }

        /**
         * Create a failure result from exception.
         *
         * @param error The exception
         * @param recoverable Whether recovery is possible
         * @return Failure result
         */
        fun failure(error: Throwable, recoverable: Boolean = true): InitResult {
            return Failure(error, recoverable)
        }

        /**
         * Create a failure result from message.
         *
         * @param message Error message
         * @param recoverable Whether recovery is possible
         * @return Failure result
         */
        fun failure(message: String, recoverable: Boolean = true): InitResult {
            return Failure(RuntimeException(message), recoverable)
        }
    }
}

/**
 * Health status of a plugin.
 *
 * Provides detailed health information for monitoring and diagnostics.
 * Plugins should report health status accurately to enable proper
 * lifecycle management and automatic recovery.
 *
 * ## Usage
 * ```kotlin
 * val health = plugin.healthCheck()
 * if (!health.healthy) {
 *     println("Plugin unhealthy: ${health.message}")
 *     health.diagnostics.forEach { (key, value) ->
 *         println("  $key: $value")
 *     }
 * }
 * ```
 *
 * @property healthy Whether the plugin is functioning correctly
 * @property message Human-readable status message
 * @property diagnostics Key-value pairs of diagnostic information
 * @property lastCheckTime Timestamp of this health check (epoch millis)
 * @property checkDurationMs Duration of the health check in milliseconds
 * @since 1.0.0
 * @see UniversalPlugin.healthCheck
 */
@Serializable
data class HealthStatus(
    val healthy: Boolean,
    val message: String = "",
    val diagnostics: Map<String, String> = emptyMap(),
    val lastCheckTime: Long = currentTimeMillis(),
    val checkDurationMs: Long = 0
) {
    companion object {
        /**
         * Create a healthy status.
         *
         * @param message Optional status message
         * @param diagnostics Optional diagnostic information
         * @return Healthy status
         */
        fun healthy(
            message: String = "OK",
            diagnostics: Map<String, String> = emptyMap()
        ): HealthStatus {
            return HealthStatus(
                healthy = true,
                message = message,
                diagnostics = diagnostics
            )
        }

        /**
         * Create an unhealthy status.
         *
         * @param message Error message describing the issue
         * @param diagnostics Diagnostic information for troubleshooting
         * @return Unhealthy status
         */
        fun unhealthy(
            message: String,
            diagnostics: Map<String, String> = emptyMap()
        ): HealthStatus {
            return HealthStatus(
                healthy = false,
                message = message,
                diagnostics = diagnostics
            )
        }

        /**
         * Create an unhealthy status from exception.
         *
         * @param error The exception that caused the unhealthy state
         * @return Unhealthy status with error details
         */
        fun fromError(error: Throwable): HealthStatus {
            return HealthStatus(
                healthy = false,
                message = error.message ?: "Unknown error",
                diagnostics = mapOf(
                    "errorType" to (error::class.simpleName ?: "Unknown"),
                    "stackTrace" to (error.stackTraceToString().take(500))
                )
            )
        }
    }
}

/**
 * Plugin event for inter-plugin communication.
 *
 * Events enable decoupled communication between plugins through
 * the plugin event bus. Plugins can publish events and subscribe
 * to events from other plugins.
 *
 * ## Event Types
 * Use the companion object constants for well-known event types.
 * Custom event types should follow the pattern: `domain.action`
 *
 * @property eventId Unique event identifier
 * @property sourcePluginId ID of the plugin that published this event
 * @property eventType Event type identifier
 * @property timestamp Event timestamp (epoch milliseconds)
 * @property payload Simple key-value payload
 * @property payloadJson Complex payload as JSON string
 * @since 1.0.0
 * @see UniversalPlugin.onEvent
 */
@Serializable
data class PluginEvent(
    val eventId: String,
    val sourcePluginId: String,
    val eventType: String,
    val timestamp: Long = currentTimeMillis(),
    val payload: Map<String, String> = emptyMap(),
    val payloadJson: String? = null
) {
    /**
     * Get a payload value.
     *
     * @param key Payload key
     * @return Payload value or null
     */
    fun getPayload(key: String): String? = payload[key]

    /**
     * Check if this event matches a type filter.
     *
     * @param typePrefix Event type prefix to match
     * @return true if event type matches
     */
    fun matchesType(typePrefix: String): Boolean {
        return eventType == typePrefix || eventType.startsWith("$typePrefix.")
    }

    companion object {
        // ============================================
        // Plugin Lifecycle Events
        // ============================================

        /** Plugin was registered with the system */
        const val TYPE_PLUGIN_REGISTERED = "plugin.registered"

        /** Plugin was unregistered from the system */
        const val TYPE_PLUGIN_UNREGISTERED = "plugin.unregistered"

        /** Plugin state changed */
        const val TYPE_STATE_CHANGED = "plugin.state.changed"

        /** Plugin configuration changed */
        const val TYPE_CONFIG_CHANGED = "plugin.config.changed"

        /** Plugin health status changed */
        const val TYPE_HEALTH_CHANGED = "plugin.health.changed"

        // ============================================
        // Capability Events
        // ============================================

        /** A capability became available */
        const val TYPE_CAPABILITY_AVAILABLE = "capability.available"

        /** A capability became unavailable */
        const val TYPE_CAPABILITY_UNAVAILABLE = "capability.unavailable"

        // ============================================
        // Accessibility Events
        // ============================================

        /** Voice command received */
        const val TYPE_VOICE_COMMAND = "accessibility.voice.command"

        /** Gaze target detected */
        const val TYPE_GAZE_TARGET = "accessibility.gaze.target"

        /** Screen/UI changed */
        const val TYPE_SCREEN_CHANGED = "accessibility.screen.changed"

        /** Focus changed to new element */
        const val TYPE_FOCUS_CHANGED = "accessibility.focus.changed"

        /** Action executed on element */
        const val TYPE_ACTION_EXECUTED = "accessibility.action.executed"

        /**
         * Create a state change event.
         *
         * @param pluginId Source plugin ID
         * @param oldState Previous state
         * @param newState New state
         * @return State change event
         */
        fun stateChange(
            pluginId: String,
            oldState: PluginState,
            newState: PluginState
        ): PluginEvent {
            return PluginEvent(
                eventId = generateEventId(),
                sourcePluginId = pluginId,
                eventType = TYPE_STATE_CHANGED,
                payload = mapOf(
                    "oldState" to oldState.name,
                    "newState" to newState.name
                )
            )
        }

        /**
         * Create a capability event.
         *
         * @param pluginId Source plugin ID
         * @param capabilityId Capability ID
         * @param available Whether capability is available
         * @return Capability event
         */
        fun capabilityChange(
            pluginId: String,
            capabilityId: String,
            available: Boolean
        ): PluginEvent {
            return PluginEvent(
                eventId = generateEventId(),
                sourcePluginId = pluginId,
                eventType = if (available) TYPE_CAPABILITY_AVAILABLE else TYPE_CAPABILITY_UNAVAILABLE,
                payload = mapOf("capabilityId" to capabilityId)
            )
        }

        private fun generateEventId(): String {
            // Simple ID generation - in production, use UUID
            return "evt_${currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}

/**
 * Filter for event subscriptions.
 *
 * Allows subscribers to filter events by type, source plugin, or exclusion list.
 * All filter criteria use AND logic - an event must match all non-empty criteria.
 *
 * ## Usage
 * ```kotlin
 * // Filter for voice commands only
 * val filter = EventFilter.forTypes(PluginEvent.TYPE_VOICE_COMMAND)
 *
 * // Filter for events from specific plugins
 * val pluginFilter = EventFilter.forPlugins("com.example.speech", "com.example.nlu")
 *
 * // Combine filters
 * val combined = filter.and(pluginFilter)
 * ```
 *
 * @property eventTypes Set of event types to include (empty = all types)
 * @property sourcePlugins Set of source plugin IDs to include (empty = all plugins)
 * @property excludePlugins Set of plugin IDs to exclude from results
 * @since 1.0.0
 * @see PluginEventBus
 */
@Serializable
data class EventFilter(
    val eventTypes: Set<String> = emptySet(),
    val sourcePlugins: Set<String> = emptySet(),
    val excludePlugins: Set<String> = emptySet()
) {
    companion object {
        /**
         * Filter that matches all events.
         */
        val ALL = EventFilter()

        /**
         * Create a filter for specific event types.
         *
         * @param types Event types to match
         * @return Filter matching only the specified event types
         */
        fun forTypes(vararg types: String): EventFilter =
            EventFilter(eventTypes = types.toSet())

        /**
         * Create a filter for specific source plugins.
         *
         * @param pluginIds Plugin IDs to receive events from
         * @return Filter matching only events from the specified plugins
         */
        fun forPlugins(vararg pluginIds: String): EventFilter =
            EventFilter(sourcePlugins = pluginIds.toSet())

        /**
         * Create a filter that excludes specific plugins.
         *
         * @param pluginIds Plugin IDs to exclude
         * @return Filter excluding events from the specified plugins
         */
        fun excluding(vararg pluginIds: String): EventFilter =
            EventFilter(excludePlugins = pluginIds.toSet())
    }

    /**
     * Check if an event matches this filter.
     *
     * An event matches if:
     * 1. eventTypes is empty OR event.eventType is in eventTypes
     * 2. sourcePlugins is empty OR event.sourcePluginId is in sourcePlugins
     * 3. event.sourcePluginId is NOT in excludePlugins
     *
     * @param event The event to check
     * @return true if the event matches all filter criteria
     */
    fun matches(event: PluginEvent): Boolean {
        // Check event type filter (empty = match all)
        if (eventTypes.isNotEmpty() && event.eventType !in eventTypes) {
            return false
        }

        // Check source plugin filter (empty = match all)
        if (sourcePlugins.isNotEmpty() && event.sourcePluginId !in sourcePlugins) {
            return false
        }

        // Check exclusion list
        if (event.sourcePluginId in excludePlugins) {
            return false
        }

        return true
    }

    /**
     * Combine this filter with another using AND logic.
     *
     * - Event types are intersected (if both non-empty)
     * - Source plugins are intersected (if both non-empty)
     * - Excluded plugins are combined (union)
     *
     * @param other The filter to combine with
     * @return A new filter combining both criteria
     */
    fun and(other: EventFilter): EventFilter = EventFilter(
        eventTypes = when {
            eventTypes.isEmpty() -> other.eventTypes
            other.eventTypes.isEmpty() -> eventTypes
            else -> eventTypes.intersect(other.eventTypes)
        },
        sourcePlugins = when {
            sourcePlugins.isEmpty() -> other.sourcePlugins
            other.sourcePlugins.isEmpty() -> sourcePlugins
            else -> sourcePlugins.intersect(other.sourcePlugins)
        },
        excludePlugins = excludePlugins + other.excludePlugins
    )
}

/**
 * Platform-agnostic current time provider.
 * Uses expect/actual pattern for multiplatform compatibility.
 */
expect fun currentTimeMillis(): Long

/**
 * Plugin host interface for managing plugin lifecycle.
 *
 * Provides the core operations for loading, unloading, and querying plugins.
 * Platform-specific implementations provide the actual plugin management logic.
 *
 * @param T Platform-specific context type
 */
interface IPluginHost<T : Any> {

    /**
     * Get a plugin by its ID.
     *
     * @param pluginId The unique plugin identifier
     * @return The plugin instance or null if not loaded
     */
    fun getPlugin(pluginId: String): UniversalPlugin?

    /**
     * Get all loaded plugins.
     *
     * @return List of all currently loaded plugins
     */
    fun getLoadedPlugins(): List<UniversalPlugin>

    /**
     * Check if a plugin is loaded.
     *
     * @param pluginId The unique plugin identifier
     * @return true if the plugin is loaded
     */
    fun isPluginLoaded(pluginId: String): Boolean

    /**
     * Get plugins that provide a specific capability.
     *
     * @param capabilityId Capability identifier to search for
     * @return List of plugins providing the capability
     */
    fun getPluginsByCapability(capabilityId: String): List<UniversalPlugin>

    /**
     * Load a plugin.
     *
     * @param pluginId The plugin identifier to load
     * @param config Optional plugin configuration
     * @return Result containing the loaded plugin or failure
     */
    suspend fun loadPlugin(pluginId: String, config: PluginConfig = PluginConfig.EMPTY): Result<UniversalPlugin>

    /**
     * Unload a plugin.
     *
     * @param pluginId The plugin identifier to unload
     * @return Result indicating success or failure
     */
    suspend fun unloadPlugin(pluginId: String): Result<Unit>
}
