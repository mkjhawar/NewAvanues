/**
 * ActionPlugin.kt - Plugin interface for third-party extensions
 *
 * Part of Phase 4.1 - Q12 Decision (Plugin System - APK/JAR Loading)
 * Direct implementation with security sandboxing
 *
 * SECURITY REQUIREMENTS:
 * - All plugins MUST be signature-verified before loading
 * - Plugins execute with timeout enforcement (5s max)
 * - Plugins have limited permissions via PluginPermissions model
 * - Plugins run in sandboxed environment
 *
 * @since VOS4 Phase 4.1
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.commandmanager.plugins

import android.content.Context
import com.augmentalis.voiceoscore.commandmanager.dynamic.VoiceCommand
import com.augmentalis.voiceoscore.commandmanager.dynamic.CommandResult

/**
 * Interface that all action plugins must implement
 *
 * Plugins extend CommandManager functionality by handling custom voice commands.
 * Each plugin declares its supported commands and handles execution.
 *
 * SECURITY NOTE: Plugins are untrusted code. All execution is sandboxed
 * with timeout enforcement and limited permissions.
 *
 * Example Plugin:
 * ```kotlin
 * class WeatherPlugin : ActionPlugin {
 *     override val pluginId = "com.example.weather"
 *     override val version = "1.0.0"
 *     override val supportedCommands = listOf("weather", "forecast", "temperature")
 *
 *     override fun initialize(context: Context, permissions: PluginPermissions) {
 *         // Initialize plugin resources
 *     }
 *
 *     override suspend fun execute(command: VoiceCommand): CommandResult {
 *         return when {
 *             command.matches("weather") -> getWeather()
 *             command.matches("forecast") -> getForecast()
 *             else -> CommandResult.Error("Unknown command")
 *         }
 *     }
 *
 *     override fun shutdown() {
 *         // Clean up resources
 *     }
 * }
 * ```
 */
interface ActionPlugin {
    /**
     * Unique identifier for this plugin
     *
     * MUST follow reverse domain notation (e.g., "com.company.pluginname")
     * Used for namespace isolation and conflict detection
     */
    val pluginId: String

    /**
     * Plugin version in semantic versioning format (e.g., "1.2.3")
     *
     * Used for compatibility checking and migration
     */
    val version: String

    /**
     * Display name for this plugin
     *
     * Shown to users in plugin management UI
     */
    val name: String
        get() = pluginId.substringAfterLast('.')

    /**
     * Human-readable description of what this plugin does
     *
     * Shown to users in plugin management UI
     */
    val description: String
        get() = ""

    /**
     * Author/developer of this plugin
     */
    val author: String
        get() = "Unknown"

    /**
     * Minimum VOS version required to run this plugin
     *
     * Format: integer version code (e.g., 40100 for VOS 4.1.0)
     */
    val minVOSVersion: Int
        get() = 40000 // VOS 4.0.0

    /**
     * Plugin API version
     *
     * Used for compatibility checking with VOS plugin API.
     * Increment when using newer plugin API features.
     */
    val apiVersion: Int
        get() = 1

    /**
     * Command category this plugin handles
     *
     * Used for organizing plugins in UI and routing.
     */
    val category: String
        get() = "CUSTOM"

    /**
     * List of voice command phrases this plugin handles
     *
     * These commands will be registered with CommandManager when plugin loads.
     * Commands are case-insensitive.
     *
     * Examples: ["weather", "forecast", "temperature in london"]
     */
    val supportedCommands: List<String>

    /**
     * Permissions requested by this plugin
     *
     * Declared permissions are validated during plugin loading.
     * Requesting permissions not needed will trigger security warnings.
     */
    val requestedPermissions: List<PluginPermission>
        get() = emptyList()

    /**
     * Initialize the plugin
     *
     * Called once when plugin is loaded. Use this to:
     * - Initialize resources
     * - Set up database connections
     * - Register callbacks
     * - Validate permissions
     *
     * IMPORTANT: This method MUST complete within 10 seconds or plugin
     * will be marked as failed and unloaded.
     *
     * @param context Application context (NOT Activity context)
     * @param permissions Granted permissions for this plugin
     * @throws PluginInitializationException if initialization fails
     */
    fun initialize(context: Context, permissions: PluginPermissions)

    /**
     * Execute a voice command
     *
     * Called when a matching voice command is recognized.
     * MUST return within 5 seconds or execution will be terminated.
     *
     * SECURITY: This method runs in a sandboxed environment with:
     * - Timeout enforcement (5s max)
     * - Limited permissions (only those granted in PluginPermissions)
     * - Exception handling (crashes won't affect main app)
     *
     * @param command The voice command to execute
     * @return Result of command execution
     */
    suspend fun execute(command: VoiceCommand): CommandResult

    /**
     * Shutdown the plugin
     *
     * Called when plugin is being unloaded. Use this to:
     * - Release resources
     * - Close database connections
     * - Unregister callbacks
     * - Save state
     *
     * IMPORTANT: This method MUST complete within 5 seconds.
     */
    fun shutdown()

    /**
     * Health check for plugin status
     *
     * Called periodically to verify plugin is functioning correctly.
     * Return false if plugin is in degraded state.
     *
     * Default implementation returns true (healthy).
     *
     * @return true if plugin is healthy, false if degraded
     */
    fun healthCheck(): Boolean = true

    /**
     * Handle configuration changes
     *
     * Called when plugin configuration is updated via Settings.
     * Default implementation does nothing.
     *
     * @param config New configuration map
     */
    fun onConfigurationChanged(config: Map<String, Any>) {
        // Default: no-op
    }

    /**
     * Get plugin statistics
     *
     * Return usage statistics for this plugin.
     * Used for telemetry and analytics (if user has telemetry enabled).
     *
     * @return Statistics map (e.g., {"commands_executed": 42, "success_rate": 0.95})
     */
    fun getStatistics(): Map<String, Any> = emptyMap()
}

/**
 * Permissions that plugins can request
 */
enum class PluginPermission {
    /** Access to network (HTTP/HTTPS requests) */
    NETWORK,

    /** Read/write external storage */
    STORAGE,

    /** Access location information */
    LOCATION,

    /** Perform gestures via accessibility service */
    GESTURES,

    /** Launch and control apps */
    APPS,

    /** Read notifications */
    NOTIFICATIONS,

    /** Access device sensors (accelerometer, gyroscope, etc.) */
    SENSORS,

    /** Access camera */
    CAMERA,

    /** Access microphone (beyond voice commands) */
    MICROPHONE,

    /** Access contacts */
    CONTACTS,

    /** Access calendar */
    CALENDAR,

    /** Send SMS/MMS */
    SMS,

    /** Make phone calls */
    PHONE
}

/**
 * Granted permissions for a plugin
 *
 * Immutable permission set granted to a plugin after validation.
 * Plugins can check permissions before attempting operations.
 */
data class PluginPermissions(
    private val granted: Set<PluginPermission>
) {
    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(permission: PluginPermission): Boolean =
        permission in granted

    /**
     * Get all granted permissions
     */
    fun getGrantedPermissions(): Set<PluginPermission> = granted

    /**
     * Check if multiple permissions are granted
     */
    fun hasPermissions(vararg permissions: PluginPermission): Boolean =
        permissions.all { it in granted }

    companion object {
        /** No permissions granted (safest default) */
        val NONE = PluginPermissions(emptySet())

        /** Basic permissions (gestures + apps only) */
        val BASIC = PluginPermissions(setOf(
            PluginPermission.GESTURES,
            PluginPermission.APPS
        ))

        /** All permissions (use with extreme caution) */
        val ALL = PluginPermissions(PluginPermission.values().toSet())

        /**
         * Create permissions from list
         */
        fun from(permissions: List<PluginPermission>): PluginPermissions =
            PluginPermissions(permissions.toSet())
    }
}

/**
 * Exception thrown during plugin initialization
 */
class PluginInitializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown during plugin execution
 */
class PluginExecutionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown during plugin loading
 */
class PluginLoadException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Plugin metadata extracted from plugin manifest
 */
data class PluginMetadata(
    val pluginId: String,
    val version: String,
    val name: String,
    val description: String,
    val author: String,
    val minVOSVersion: Int,
    val apiVersion: Int,
    val category: String,
    val requestedPermissions: List<PluginPermission>,
    val signatureHash: String,
    val packageName: String,
    val className: String
)

/**
 * Plugin state tracking
 */
enum class PluginState {
    /** Plugin is loaded and ready */
    LOADED,

    /** Plugin is currently initializing */
    INITIALIZING,

    /** Plugin initialization failed */
    FAILED,

    /** Plugin is disabled by user */
    DISABLED,

    /** Plugin is being unloaded */
    UNLOADING,

    /** Plugin is in degraded state (health check failed) */
    DEGRADED
}

/**
 * Plugin lifecycle listener
 *
 * Implement this to receive notifications about plugin lifecycle events.
 */
interface PluginLifecycleListener {
    /**
     * Called when a plugin is successfully loaded
     */
    fun onPluginLoaded(pluginId: String, plugin: ActionPlugin)

    /**
     * Called when a plugin fails to load
     */
    fun onPluginLoadFailed(pluginId: String, error: Throwable)

    /**
     * Called when a plugin is unloaded
     */
    fun onPluginUnloaded(pluginId: String)

    /**
     * Called when a plugin's state changes
     */
    fun onPluginStateChanged(pluginId: String, newState: PluginState)

    /**
     * Called when a plugin executes a command
     */
    fun onPluginCommandExecuted(pluginId: String, command: String, result: CommandResult)
}
