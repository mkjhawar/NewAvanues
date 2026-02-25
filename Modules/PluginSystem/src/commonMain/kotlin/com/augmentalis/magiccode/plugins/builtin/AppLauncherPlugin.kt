/**
 * AppLauncherPlugin.kt - App Launcher Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Migrated plugin from VoiceOSCore's AppHandler to the Universal Plugin Architecture.
 * Handles app launching and management via voice/gaze commands.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../AppHandler.kt
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Use platform-agnostic time function from PluginTypes.kt

// =============================================================================
// App Info Data Classes
// =============================================================================

/**
 * Information about an installed application.
 *
 * Contains all metadata needed for app discovery and launching via voice commands.
 * Supports multiple aliases for flexible voice recognition.
 *
 * ## Usage
 * ```kotlin
 * val mapsApp = AppInfo(
 *     packageName = "com.google.android.apps.maps",
 *     displayName = "Google Maps",
 *     aliases = listOf("maps", "navigation", "directions")
 * )
 *
 * // Find by alias
 * handler.findApp("maps") // Returns mapsApp
 * handler.findApp("navigation") // Returns mapsApp
 * ```
 *
 * @property packageName Unique package identifier (e.g., "com.google.android.apps.maps")
 * @property displayName Human-readable name (e.g., "Google Maps")
 * @property aliases Alternative names for voice recognition (e.g., ["maps", "navigation"])
 * @property category App category for organization (optional)
 * @property iconUri URI to app icon (optional, platform-specific)
 * @property isSystemApp Whether this is a system app
 * @property launchActivity Main activity to launch (optional, for direct activity launch)
 * @since 1.0.0
 */
data class AppInfo(
    val packageName: String,
    val displayName: String,
    val aliases: List<String> = emptyList(),
    val category: String? = null,
    val iconUri: String? = null,
    val isSystemApp: Boolean = false,
    val launchActivity: String? = null
) {
    /**
     * Get all searchable names (display name + aliases).
     */
    val searchableNames: List<String>
        get() = listOf(displayName) + aliases

    /**
     * Check if this app matches a search query (case-insensitive).
     *
     * @param query Search query
     * @return true if app matches
     */
    fun matches(query: String): Boolean {
        val normalizedQuery = query.lowercase().trim()
        return displayName.lowercase() == normalizedQuery ||
                packageName.lowercase() == normalizedQuery ||
                aliases.any { it.lowercase() == normalizedQuery }
    }

    /**
     * Check if this app partially matches a search query (case-insensitive).
     *
     * @param query Search query
     * @return true if app partially matches
     */
    fun partialMatches(query: String): Boolean {
        val normalizedQuery = query.lowercase().trim()
        return displayName.lowercase().contains(normalizedQuery) ||
                aliases.any { it.lowercase().contains(normalizedQuery) }
    }

    companion object {
        /**
         * Create an AppInfo from minimal information.
         *
         * Automatically generates aliases from the display name.
         *
         * @param packageName Package identifier
         * @param displayName Human-readable name
         * @return AppInfo with auto-generated aliases
         */
        fun simple(packageName: String, displayName: String): AppInfo {
            // Generate aliases from display name
            val aliases = mutableListOf<String>()

            // Add lowercase version
            val lowerName = displayName.lowercase()
            if (lowerName != displayName) {
                aliases.add(lowerName)
            }

            // Add single-word version if multi-word
            val words = displayName.split(" ")
            if (words.size > 1) {
                // Add first word as alias if it's meaningful
                val firstWord = words.first()
                if (firstWord.length >= 3) {
                    aliases.add(firstWord.lowercase())
                }
                // Add last word as alias if it's meaningful
                val lastWord = words.last()
                if (lastWord.length >= 3 && lastWord != firstWord) {
                    aliases.add(lastWord.lowercase())
                }
            }

            return AppInfo(
                packageName = packageName,
                displayName = displayName,
                aliases = aliases.distinct()
            )
        }
    }
}

/**
 * Result of an app launch attempt.
 *
 * Provides detailed information about the launch outcome for
 * feedback and error handling.
 *
 * @property success Whether the launch was successful
 * @property app Information about the app (if found)
 * @property error Error message if launch failed
 * @property launchTime Time taken to launch in milliseconds (if available)
 * @since 1.0.0
 */
data class AppLaunchResult(
    val success: Boolean,
    val app: AppInfo? = null,
    val error: String? = null,
    val launchTime: Long? = null
) {
    companion object {
        /**
         * Create a successful launch result.
         *
         * @param app The launched app
         * @param launchTime Time taken to launch (optional)
         * @return Success result
         */
        fun success(app: AppInfo, launchTime: Long? = null): AppLaunchResult {
            return AppLaunchResult(
                success = true,
                app = app,
                launchTime = launchTime
            )
        }

        /**
         * Create a failure result for app not found.
         *
         * @param query The search query that failed
         * @return Failure result
         */
        fun notFound(query: String): AppLaunchResult {
            return AppLaunchResult(
                success = false,
                error = "App not found: $query"
            )
        }

        /**
         * Create a failure result for launch error.
         *
         * @param app The app that failed to launch
         * @param error Error message
         * @return Failure result
         */
        fun launchFailed(app: AppInfo, error: String): AppLaunchResult {
            return AppLaunchResult(
                success = false,
                app = app,
                error = error
            )
        }

        /**
         * Create a failure result from exception.
         *
         * @param e The exception
         * @return Failure result
         */
        fun fromException(e: Exception): AppLaunchResult {
            return AppLaunchResult(
                success = false,
                error = "Launch error: ${e.message}"
            )
        }
    }
}

// =============================================================================
// App Launcher Interface
// =============================================================================

/**
 * Interface for platform-specific app launching.
 *
 * Implementations should provide native app launching capabilities
 * for each supported platform:
 * - Android: Uses PackageManager and Intent
 * - iOS: Uses UIApplication open URL
 * - Desktop: Uses platform-specific app launch mechanisms
 *
 * ## Thread Safety
 * All methods are suspend functions and should be thread-safe.
 * Implementations should handle dispatching to appropriate threads.
 *
 * @since 1.0.0
 * @see AppLauncherPlugin
 */
interface IAppLauncher {

    /**
     * Launch an app by package name.
     *
     * @param packageName The package identifier of the app to launch
     * @return true if launch succeeded, false otherwise
     */
    suspend fun launchApp(packageName: String): Boolean

    /**
     * Launch an app with a specific activity.
     *
     * @param packageName The package identifier
     * @param activityName The activity class name to launch
     * @return true if launch succeeded
     */
    suspend fun launchActivity(packageName: String, activityName: String): Boolean

    /**
     * Get list of installed apps on the device.
     *
     * @return List of installed app information
     */
    suspend fun getInstalledApps(): List<AppInfo>

    /**
     * Check if an app is installed.
     *
     * @param packageName Package name to check
     * @return true if app is installed
     */
    suspend fun isAppInstalled(packageName: String): Boolean

    /**
     * Get detailed info about a specific app.
     *
     * @param packageName Package name to query
     * @return AppInfo or null if not found
     */
    suspend fun getAppInfo(packageName: String): AppInfo?

    /**
     * Get recently used apps.
     *
     * @param limit Maximum number of apps to return
     * @return List of recently used apps
     */
    suspend fun getRecentApps(limit: Int = 10): List<AppInfo>
}

// =============================================================================
// App Registry
// =============================================================================

/**
 * In-memory registry of known applications.
 *
 * Provides fast lookup of apps by name, package, or alias.
 * Thread-safe for concurrent access.
 *
 * @since 1.0.0
 */
class AppRegistry : SynchronizedObject() {

    private val apps = mutableMapOf<String, AppInfo>()
    private val aliasIndex = mutableMapOf<String, String>() // alias -> packageName

    /**
     * Number of registered apps.
     */
    val size: Int get() = apps.size

    /**
     * Register an app in the registry.
     *
     * If an app with the same package name exists, it will be updated.
     *
     * @param app App information to register
     */
    fun register(app: AppInfo) = synchronized(this) {
        apps[app.packageName] = app

        // Index aliases for fast lookup
        aliasIndex[app.displayName.lowercase()] = app.packageName
        app.aliases.forEach { alias ->
            aliasIndex[alias.lowercase()] = app.packageName
        }
    }

    /**
     * Unregister an app from the registry.
     *
     * @param packageName Package name of the app to remove
     * @return true if app was removed, false if not found
     */
    fun unregister(packageName: String): Boolean = synchronized(this) {
        val app = apps.remove(packageName) ?: return@synchronized false

        // Remove from alias index
        aliasIndex.remove(app.displayName.lowercase())
        app.aliases.forEach { alias ->
            aliasIndex.remove(alias.lowercase())
        }

        true
    }

    /**
     * Find an app by query string.
     *
     * Searches in order:
     * 1. Display name (exact match, case-insensitive)
     * 2. Package name (exact match, case-insensitive)
     * 3. Aliases (exact match, case-insensitive)
     *
     * @param query Search query
     * @return AppInfo if found, null otherwise
     */
    fun find(query: String): AppInfo? = synchronized(this) {
        val normalizedQuery = query.lowercase().trim()

        // Check alias index first (fast path)
        aliasIndex[normalizedQuery]?.let { packageName ->
            return@synchronized apps[packageName]
        }

        // Check package name
        apps[normalizedQuery.lowercase()]?.let { return@synchronized it }

        // Fallback: linear search (shouldn't be needed with proper indexing)
        apps.values.find { it.matches(query) }
    }

    /**
     * Search for apps matching a partial query.
     *
     * @param query Partial search query
     * @param limit Maximum results to return
     * @return List of matching apps
     */
    fun search(query: String, limit: Int = 10): List<AppInfo> = synchronized(this) {
        val normalizedQuery = query.lowercase().trim()

        apps.values
            .filter { it.partialMatches(query) }
            .sortedBy { app ->
                // Prioritize exact matches, then by name length
                when {
                    app.displayName.lowercase() == normalizedQuery -> 0
                    app.displayName.lowercase().startsWith(normalizedQuery) -> 1
                    app.aliases.any { it.lowercase() == normalizedQuery } -> 2
                    else -> 3
                }
            }
            .take(limit)
    }

    /**
     * Get all registered apps.
     *
     * @return List of all registered app info
     */
    fun getAll(): List<AppInfo> = synchronized(this) { apps.values.toList() }

    /**
     * Get apps by category.
     *
     * @param category Category to filter by
     * @return List of apps in the category
     */
    fun getByCategory(category: String): List<AppInfo> = synchronized(this) {
        apps.values.filter { it.category == category }
    }

    /**
     * Clear all registered apps.
     */
    fun clear() = synchronized(this) {
        apps.clear()
        aliasIndex.clear()
    }

    /**
     * Bulk register apps.
     *
     * @param appList List of apps to register
     */
    fun registerAll(appList: List<AppInfo>) = synchronized(this) {
        appList.forEach { register(it) }
    }
}

// =============================================================================
// App Launcher Plugin Implementation
// =============================================================================

/**
 * App Launcher Plugin - Universal Plugin for app launching and management.
 *
 * Handles all app-related voice/gaze commands including:
 * - Open/launch/start apps by name
 * - App discovery via aliases
 * - Auto-sync with installed apps
 * - App registry management
 *
 * ## Migration Notes
 * This plugin wraps the original AppHandler logic from VoiceOSCore,
 * adapting it to the Universal Plugin interface while maintaining identical
 * behavior and adding enhanced app registry capabilities.
 *
 * ## Usage
 * ```kotlin
 * val plugin = AppLauncherPlugin { androidAppLauncher }
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "open maps", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * ## Supported Commands
 * - "open <app>" - Open an app by name or alias
 * - "launch <app>" - Launch an app by name or alias
 * - "start <app>" - Start an app by name or alias
 * - "<app>" - Direct app name (if registered in registry)
 *
 * ## App Discovery
 * Apps can be found by:
 * - Display name (e.g., "Google Maps")
 * - Package name (e.g., "com.google.android.apps.maps")
 * - Alias (e.g., "maps", "navigation")
 *
 * @param appLauncherProvider Lazy provider for platform-specific app launcher
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 * @see IAppLauncher
 */
class AppLauncherPlugin(
    private val appLauncherProvider: () -> IAppLauncher?
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "App Launcher Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "App Launcher Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin", "IAppLauncher"),
            metadata = mapOf(
                "handlerType" to "NAVIGATION",
                "supportsAppLaunch" to "true",
                "supportsAliases" to "true",
                "supportsAutoSync" to "true"
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.NAVIGATION

    override val patterns: List<CommandPattern> = listOf(
        // Open patterns
        CommandPattern(
            regex = Regex("^open\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "OPEN",
            requiredEntities = setOf("app"),
            examples = listOf("open maps", "open Google Maps", "open settings")
        ),
        CommandPattern(
            regex = Regex("^launch\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "LAUNCH",
            requiredEntities = setOf("app"),
            examples = listOf("launch youtube", "launch YouTube", "launch camera")
        ),
        CommandPattern(
            regex = Regex("^start\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "START",
            requiredEntities = setOf("app"),
            examples = listOf("start calculator", "start browser", "start email")
        ),
        // Alternative patterns
        CommandPattern(
            regex = Regex("^go to\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "OPEN",
            requiredEntities = setOf("app"),
            examples = listOf("go to settings", "go to home")
        ),
        CommandPattern(
            regex = Regex("^run\\s+(.+)$", RegexOption.IGNORE_CASE),
            intent = "OPEN",
            requiredEntities = setOf("app"),
            examples = listOf("run calculator")
        )
    )

    // =========================================================================
    // Supported Actions (for discovery)
    // =========================================================================

    /**
     * List of supported action prefixes.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String> = listOf(
        "open", "launch", "start", "go to", "run"
    )

    // =========================================================================
    // App Registry
    // =========================================================================

    /**
     * Registry of known apps.
     */
    val appRegistry = AppRegistry()

    // =========================================================================
    // Provider Reference
    // =========================================================================

    private var appLauncher: IAppLauncher? = null

    // =========================================================================
    // Sync State
    // =========================================================================

    private var lastSyncTime: Long = 0
    private val _syncState = MutableStateFlow(false)

    /**
     * Observable flow indicating sync in progress.
     */
    val syncInProgress: StateFlow<Boolean> get() = _syncState

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            appLauncher = appLauncherProvider()

            // Auto-sync with installed apps
            syncInstalledApps()

            InitResult.success("AppLauncherPlugin initialized with ${appRegistry.size} apps")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        appRegistry.clear()
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "launcherAvailable" to (appLauncher != null).toString(),
        "registeredApps" to appRegistry.size.toString(),
        "lastSyncTime" to lastSyncTime.toString(),
        "syncInProgress" to _syncState.value.toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()

        // Check pattern matches first
        if (patterns.any { it.matches(phrase) }) {
            return true
        }

        // Check for supported action prefixes
        if (supportedActions.any { phrase.startsWith("$it ") }) {
            return true
        }

        // Check if the entire phrase is a known app name/alias
        if (appRegistry.find(phrase) != null) {
            return true
        }

        return false
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val result = handleCommand(command.phrase)

        return if (result.success) {
            ActionResult.Success("Opened ${result.app?.displayName ?: "app"}")
        } else {
            ActionResult.Error(result.error ?: "Failed to open app")
        }
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Pattern match with supported action prefix
        for (action in supportedActions) {
            if (phrase.startsWith("$action ")) {
                val appName = phrase.substringAfter("$action ").trim()
                if (appName.isNotEmpty()) {
                    // Higher confidence if app is in registry
                    return if (appRegistry.find(appName) != null) 1.0f else 0.8f
                }
            }
        }

        // Direct app name match
        if (appRegistry.find(phrase) != null) {
            return 0.9f
        }

        // Partial matches
        if (appRegistry.search(phrase).isNotEmpty()) {
            return 0.6f
        }

        return 0.0f
    }

    // =========================================================================
    // Command Handling
    // =========================================================================

    /**
     * Handle an app command string.
     *
     * Supports:
     * - Prefixed commands: "open maps", "launch youtube", "start calculator"
     * - Bare app names: "maps", "stopwatch", "calculator" (if app is registered)
     *
     * @param command Full command string (e.g., "open maps" or just "maps")
     * @return Result of the app launch attempt
     */
    suspend fun handleCommand(command: String): AppLaunchResult {
        val normalizedCommand = command.lowercase().trim()

        // Extract app name from command
        val appName = when {
            normalizedCommand.startsWith("open ") -> normalizedCommand.substringAfter("open ")
            normalizedCommand.startsWith("launch ") -> normalizedCommand.substringAfter("launch ")
            normalizedCommand.startsWith("start ") -> normalizedCommand.substringAfter("start ")
            normalizedCommand.startsWith("go to ") -> normalizedCommand.substringAfter("go to ")
            normalizedCommand.startsWith("run ") -> normalizedCommand.substringAfter("run ")
            else -> {
                // Try as bare app name
                val app = appRegistry.find(normalizedCommand)
                if (app != null) {
                    return openApp(normalizedCommand)
                }
                return AppLaunchResult(
                    success = false,
                    error = "Unknown app command: $command"
                )
            }
        }

        return openApp(appName)
    }

    /**
     * Open an app by name, package name, or alias.
     *
     * @param appName Name to search for (case-insensitive)
     * @return Result of the app launch attempt
     */
    suspend fun openApp(appName: String): AppLaunchResult {
        val normalizedName = appName.lowercase().trim()

        // Find app by display name, package name, or alias
        val app = appRegistry.find(normalizedName)
            ?: return AppLaunchResult.notFound(appName)

        // Launch app (or succeed in simulation mode if no launcher)
        return try {
            val launcher = appLauncher
            if (launcher == null) {
                // Simulation mode - no launcher available
                AppLaunchResult.success(app)
            } else {
                val startTime = currentTimeMillis()
                val launched = launcher.launchApp(app.packageName)
                val launchTime = currentTimeMillis() - startTime

                if (launched) {
                    AppLaunchResult.success(app, launchTime)
                } else {
                    AppLaunchResult.launchFailed(app, "Failed to launch ${app.displayName}")
                }
            }
        } catch (e: Exception) {
            AppLaunchResult(
                success = false,
                app = app,
                error = "Launch error: ${e.message}"
            )
        }
    }

    // =========================================================================
    // App Registry Operations
    // =========================================================================

    /**
     * Find an app by query string.
     *
     * Searches in order:
     * 1. Display name (exact match, case-insensitive)
     * 2. Package name (exact match, case-insensitive)
     * 3. Aliases (exact match, case-insensitive)
     *
     * @param query Search query
     * @return AppInfo if found, null otherwise
     */
    fun findApp(query: String): AppInfo? {
        return appRegistry.find(query)
    }

    /**
     * Search for apps matching a partial query.
     *
     * @param query Partial search query
     * @param limit Maximum results to return
     * @return List of matching apps
     */
    fun searchApps(query: String, limit: Int = 10): List<AppInfo> {
        return appRegistry.search(query, limit)
    }

    /**
     * Register an app in the registry.
     *
     * If an app with the same package name exists, it will be updated.
     *
     * @param app App information to register
     */
    fun registerApp(app: AppInfo) {
        appRegistry.register(app)
    }

    /**
     * Unregister an app from the registry.
     *
     * @param packageName Package name of the app to remove
     * @return true if app was removed, false if not found
     */
    fun unregisterApp(packageName: String): Boolean {
        return appRegistry.unregister(packageName)
    }

    /**
     * Get all registered apps.
     *
     * @return List of all registered app info
     */
    fun getRegisteredApps(): List<AppInfo> = appRegistry.getAll()

    /**
     * Get the count of registered apps.
     *
     * @return Number of apps in registry
     */
    fun getAppCount(): Int = appRegistry.size

    // =========================================================================
    // Sync Operations
    // =========================================================================

    /**
     * Sync registry with installed apps from the launcher.
     *
     * Fetches installed apps from the platform launcher and adds them
     * to the registry. Existing apps are updated if package matches.
     */
    suspend fun syncInstalledApps() {
        _syncState.value = true
        try {
            appLauncher?.getInstalledApps()?.forEach { app ->
                appRegistry.register(app)
            }
            lastSyncTime = currentTimeMillis()
        } finally {
            _syncState.value = false
        }
    }

    /**
     * Force a full re-sync of the app registry.
     *
     * Clears the registry and re-fetches all installed apps.
     */
    suspend fun forceSync() {
        _syncState.value = true
        try {
            appRegistry.clear()
            syncInstalledApps()
        } finally {
            _syncState.value = false
        }
    }

    /**
     * Get the time of last sync operation.
     *
     * @return Epoch milliseconds of last sync, 0 if never synced
     */
    fun getLastSyncTime(): Long = lastSyncTime

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.commandmanager.handler.applauncher"

        /** Default sync interval in milliseconds (1 hour) */
        const val DEFAULT_SYNC_INTERVAL = 3600000L
    }
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create an AppLauncherPlugin with a pre-configured launcher.
 *
 * @param launcher The app launcher implementation
 * @return Configured AppLauncherPlugin
 */
fun createAppLauncherPlugin(launcher: IAppLauncher): AppLauncherPlugin {
    return AppLauncherPlugin { launcher }
}

/**
 * Create an AppLauncherPlugin with a lazy launcher provider.
 *
 * Useful when the launcher depends on platform services that may
 * not be available at plugin creation time.
 *
 * @param launcherProvider Function that returns the launcher when needed
 * @return Configured AppLauncherPlugin
 */
fun createAppLauncherPlugin(launcherProvider: () -> IAppLauncher?): AppLauncherPlugin {
    return AppLauncherPlugin(launcherProvider)
}

/**
 * Create an AppLauncherPlugin in simulation mode (no actual launching).
 *
 * Useful for testing or UI previews where actual app launching is not needed.
 *
 * @param initialApps Initial apps to register
 * @return Configured AppLauncherPlugin in simulation mode
 */
fun createSimulatedAppLauncherPlugin(initialApps: List<AppInfo> = emptyList()): AppLauncherPlugin {
    val plugin = AppLauncherPlugin { null }
    initialApps.forEach { plugin.appRegistry.register(it) }
    return plugin
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock app launcher for testing AppLauncherPlugin.
 *
 * Records all launch operations and can be configured with test apps.
 * Useful for unit testing without requiring actual platform app services.
 *
 * ## Usage
 * ```kotlin
 * val mockLauncher = MockAppLauncher(shouldSucceed = true)
 * mockLauncher.addApp(AppInfo("com.test.app", "Test App", listOf("test")))
 *
 * val plugin = createAppLauncherPlugin(mockLauncher)
 *
 * // Execute launch
 * plugin.handle(command, context)
 *
 * // Verify actions
 * assertEquals(listOf("launchApp(com.test.app)"), mockLauncher.actions)
 * ```
 *
 * @param shouldSucceed Whether launch operations should succeed
 * @param initialApps Initial apps to populate the mock launcher
 * @since 1.0.0
 */
class MockAppLauncher(
    private val shouldSucceed: Boolean = true,
    initialApps: List<AppInfo> = emptyList()
) : IAppLauncher {

    private val _actions = mutableListOf<String>()
    private val _apps = mutableMapOf<String, AppInfo>()
    private val _recentApps = mutableListOf<AppInfo>()

    init {
        initialApps.forEach { _apps[it.packageName] = it }
    }

    /** List of recorded actions in format "methodName(params)" */
    val actions: List<String> get() = _actions.toList()

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    /** Add an app to the mock launcher */
    fun addApp(app: AppInfo) {
        _apps[app.packageName] = app
    }

    /** Remove an app from the mock launcher */
    fun removeApp(packageName: String) {
        _apps.remove(packageName)
    }

    /** Clear all apps */
    fun clearApps() {
        _apps.clear()
    }

    override suspend fun launchApp(packageName: String): Boolean {
        _actions.add("launchApp($packageName)")

        // Track in recent apps
        _apps[packageName]?.let { app ->
            _recentApps.remove(app)
            _recentApps.add(0, app)
        }

        return shouldSucceed && _apps.containsKey(packageName)
    }

    override suspend fun launchActivity(packageName: String, activityName: String): Boolean {
        _actions.add("launchActivity($packageName, $activityName)")
        return shouldSucceed && _apps.containsKey(packageName)
    }

    override suspend fun getInstalledApps(): List<AppInfo> {
        _actions.add("getInstalledApps()")
        return _apps.values.toList()
    }

    override suspend fun isAppInstalled(packageName: String): Boolean {
        _actions.add("isAppInstalled($packageName)")
        return _apps.containsKey(packageName)
    }

    override suspend fun getAppInfo(packageName: String): AppInfo? {
        _actions.add("getAppInfo($packageName)")
        return _apps[packageName]
    }

    override suspend fun getRecentApps(limit: Int): List<AppInfo> {
        _actions.add("getRecentApps($limit)")
        return _recentApps.take(limit)
    }
}

/**
 * Pre-built common apps for testing.
 *
 * Provides a set of commonly used apps with realistic aliases
 * for comprehensive testing scenarios.
 */
object TestApps {

    val MAPS = AppInfo(
        packageName = "com.google.android.apps.maps",
        displayName = "Google Maps",
        aliases = listOf("maps", "navigation", "directions"),
        category = "Travel"
    )

    val YOUTUBE = AppInfo(
        packageName = "com.google.android.youtube",
        displayName = "YouTube",
        aliases = listOf("youtube", "videos"),
        category = "Entertainment"
    )

    val CHROME = AppInfo(
        packageName = "com.android.chrome",
        displayName = "Chrome",
        aliases = listOf("chrome", "browser", "web"),
        category = "Productivity"
    )

    val CALCULATOR = AppInfo(
        packageName = "com.google.android.calculator",
        displayName = "Calculator",
        aliases = listOf("calculator", "calc"),
        category = "Tools"
    )

    val CAMERA = AppInfo(
        packageName = "com.android.camera",
        displayName = "Camera",
        aliases = listOf("camera", "photo"),
        category = "Photography"
    )

    val SETTINGS = AppInfo(
        packageName = "com.android.settings",
        displayName = "Settings",
        aliases = listOf("settings", "preferences", "config"),
        category = "System",
        isSystemApp = true
    )

    val CLOCK = AppInfo(
        packageName = "com.google.android.deskclock",
        displayName = "Clock",
        aliases = listOf("clock", "alarm", "timer", "stopwatch"),
        category = "Tools"
    )

    val MESSAGES = AppInfo(
        packageName = "com.google.android.apps.messaging",
        displayName = "Messages",
        aliases = listOf("messages", "sms", "text"),
        category = "Communication"
    )

    val PHONE = AppInfo(
        packageName = "com.google.android.dialer",
        displayName = "Phone",
        aliases = listOf("phone", "dialer", "call"),
        category = "Communication"
    )

    val CONTACTS = AppInfo(
        packageName = "com.google.android.contacts",
        displayName = "Contacts",
        aliases = listOf("contacts", "people", "address book"),
        category = "Communication"
    )

    /**
     * Get all test apps as a list.
     */
    fun all(): List<AppInfo> = listOf(
        MAPS, YOUTUBE, CHROME, CALCULATOR, CAMERA,
        SETTINGS, CLOCK, MESSAGES, PHONE, CONTACTS
    )

    /**
     * Create a pre-configured MockAppLauncher with all test apps.
     */
    fun createMockLauncher(shouldSucceed: Boolean = true): MockAppLauncher {
        return MockAppLauncher(shouldSucceed, all())
    }
}
