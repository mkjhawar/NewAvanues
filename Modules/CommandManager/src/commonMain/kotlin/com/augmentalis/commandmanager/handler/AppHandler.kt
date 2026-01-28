/**
 * AppHandler.kt - Handles app launching and management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-09 - Auto-sync with installed apps on initialize
 * Updated: 2026-01-27 - Load localized verbs from database via IStaticCommandPersistence
 * Updated: 2026-01-27 - Added getVoicePhrases() override for speech engine registration
 *
 * KMP handler for app-level actions (launch, switch, close, etc.).
 * Localized command verbs (open, launch, start) are loaded from .avu files
 * via the static command persistence layer.
 */
package com.augmentalis.commandmanager

/**
 * Information about an installed application.
 *
 * @property packageName Unique package identifier (e.g., "com.google.android.apps.maps")
 * @property displayName Human-readable name (e.g., "Google Maps")
 * @property aliases Alternative names for voice recognition (e.g., ["maps", "navigation"])
 */
data class AppInfo(
    val packageName: String,
    val displayName: String,
    val aliases: List<String> = emptyList()
)

/**
 * Result of an app launch attempt.
 *
 * @property success Whether the launch was successful
 * @property app Information about the app (if found)
 * @property error Error message if launch failed
 */
data class AppLaunchResult(
    val success: Boolean,
    val app: AppInfo? = null,
    val error: String? = null
)

/**
 * Interface for platform-specific app launching.
 *
 * Implementations should provide native app launching capabilities
 * for each supported platform (Android, iOS, Desktop).
 */
interface IAppLauncher {
    /**
     * Launch an app by package name.
     *
     * @param packageName The package identifier of the app to launch
     * @return true if launch succeeded, false otherwise
     */
    fun launchApp(packageName: String): Boolean

    /**
     * Get list of installed apps on the device.
     *
     * @return List of installed app information
     */
    fun getInstalledApps(): List<AppInfo>
}

/**
 * Handler for app-related voice commands.
 *
 * Supports commands like:
 * - "open maps" / "open Google Maps"
 * - "launch youtube" / "launch YouTube"
 * - "start camera" / "start Camera"
 *
 * Command verbs are loaded from the database (via IStaticCommandPersistence)
 * which reads from locale-specific .avu files. This enables localization:
 * - English: "open", "launch", "start"
 * - German: "öffne", "öffnen", "starte"
 * - Spanish: "abrir", "iniciar", "lanzar"
 *
 * Apps can be found by:
 * - Display name (e.g., "Google Maps")
 * - Package name (e.g., "com.google.android.apps.maps")
 * - Alias (e.g., "maps", "navigation")
 *
 * Usage:
 * ```kotlin
 * val handler = AppHandler(androidAppLauncher, staticCommandPersistence)
 *
 * // Handle voice command
 * val result = handler.handleCommand("open maps")
 * if (result.success) {
 *     println("Opened ${result.app?.displayName}")
 * }
 * ```
 *
 * @param appLauncher Platform-specific app launcher (optional, null for simulation mode)
 * @param persistence Static command persistence for loading localized verbs (optional)
 */
class AppHandler(
    private val appLauncher: IAppLauncher? = null,
    private val persistence: IStaticCommandPersistence? = null
) : BaseHandler() {

    companion object {
        /** Command ID for app open verbs in .avu files */
        const val COMMAND_ID_OPEN_APP = "OPEN_APP"

        /** Command ID for app suffix words in .avu files */
        const val COMMAND_ID_APP_SUFFIX = "APP_SUFFIX"

        /** Default English verbs (fallback if database not available) */
        private val DEFAULT_OPEN_VERBS = listOf("open", "launch", "start")

        /** Default English suffixes (fallback if database not available) */
        private val DEFAULT_APP_SUFFIXES = listOf("app", "application")
    }

    override val category: ActionCategory = ActionCategory.APP

    /**
     * Open verbs loaded from database or defaults.
     * Populated during initialize().
     */
    private var openVerbs: List<String> = DEFAULT_OPEN_VERBS

    /**
     * App suffix words loaded from database or defaults.
     * Populated during initialize().
     */
    private var appSuffixes: List<String> = DEFAULT_APP_SUFFIXES

    /**
     * Supported actions built from loaded verbs.
     * Includes base verbs and verb + "app" combinations.
     */
    override val supportedActions: List<String>
        get() = openVerbs + openVerbs.flatMap { verb ->
            appSuffixes.map { suffix -> "$verb $suffix" }
        }

    /**
     * Check if this handler can handle the given action.
     *
     * Matches:
     * - Commands with prefix: "open maps", "launch youtube", "start calculator"
     * - Bare app names: "maps", "youtube", "stopwatch" (if app is registered)
     *
     * @param action The action string to check
     * @return true if this handler can process the action
     */
    override fun canHandle(action: String): Boolean {
        // First check standard prefix matching (from BaseHandler)
        if (super.canHandle(action)) {
            return true
        }

        // Also check if the action is a bare app name/alias
        // This allows users to say just "Stopwatch" instead of "open Stopwatch"
        return findApp(action) != null
    }

    /**
     * Registry of known apps, keyed by package name.
     */
    private val appRegistry = mutableMapOf<String, AppInfo>()

    /**
     * Execute an app command.
     *
     * @param command The quantized command to execute
     * @param params Optional parameters
     * @return Handler result
     */
    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val result = handleCommand(command.phrase)

        return if (result.success) {
            HandlerResult.success("Opened ${result.app?.displayName}")
        } else {
            HandlerResult.failure(result.error ?: "Failed to open app")
        }
    }

    /**
     * Handle an app command string.
     *
     * Supports:
     * - Prefixed commands: "open maps", "launch youtube", "start calculator"
     * - Localized prefixes loaded from database
     * - Bare app names: "maps", "stopwatch", "calculator" (if app is registered)
     *
     * @param command Full command string (e.g., "open maps" or just "maps")
     * @return Result of the app launch attempt
     */
    fun handleCommand(command: String): AppLaunchResult {
        val normalizedCommand = command.lowercase().trim()

        // Check each verb prefix (localized from database)
        for (verb in openVerbs) {
            val prefix = "$verb "
            if (normalizedCommand.startsWith(prefix)) {
                return openApp(normalizedCommand.substringAfter(prefix).trim())
            }
        }

        // Try to open as bare app name (e.g., "stopwatch", "calculator")
        val app = findApp(normalizedCommand)
        return if (app != null) {
            openApp(normalizedCommand)
        } else {
            AppLaunchResult(
                success = false,
                error = "Unknown app command: $command"
            )
        }
    }

    /**
     * Open an app by name, package name, or alias.
     *
     * @param appName Name to search for (case-insensitive)
     * @return Result of the app launch attempt
     */
    fun openApp(appName: String): AppLaunchResult {
        val normalizedName = appName.lowercase().trim()

        // Find app by display name, package name, or alias
        val app = findApp(normalizedName)
            ?: return AppLaunchResult(
                success = false,
                error = "App not found: $appName"
            )

        // Launch app (or succeed in simulation mode if no launcher)
        val launched = appLauncher?.launchApp(app.packageName) ?: true

        return if (launched) {
            AppLaunchResult(success = true, app = app)
        } else {
            AppLaunchResult(
                success = false,
                app = app,
                error = "Failed to launch ${app.displayName}"
            )
        }
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
    fun findApp(query: String): AppInfo? {
        val normalizedQuery = query.lowercase().trim()

        return appRegistry.values.find { app ->
            app.displayName.lowercase() == normalizedQuery ||
            app.packageName.lowercase() == normalizedQuery ||
            app.aliases.any { it.lowercase() == normalizedQuery }
        }
    }

    /**
     * Register an app in the registry.
     *
     * If an app with the same package name exists, it will be updated.
     *
     * @param app App information to register
     */
    fun registerApp(app: AppInfo) {
        appRegistry[app.packageName] = app
    }

    /**
     * Unregister an app from the registry.
     *
     * @param packageName Package name of the app to remove
     * @return true if app was removed, false if not found
     */
    fun unregisterApp(packageName: String): Boolean {
        return appRegistry.remove(packageName) != null
    }

    /**
     * Get all registered apps.
     *
     * @return List of all registered app info
     */
    fun getRegisteredApps(): List<AppInfo> = appRegistry.values.toList()

    /**
     * Get the count of registered apps.
     *
     * @return Number of apps in registry
     */
    fun getAppCount(): Int = appRegistry.size

    /**
     * Get all app command phrases for speech engine registration.
     *
     * Generates phrases by combining each open verb with each app name/alias.
     * For example: ["open WhatsApp", "launch WhatsApp", "open Spotify", "launch Spotify", ...]
     *
     * These phrases should be registered with the speech engine so it can
     * recognize app launch commands.
     *
     * @return List of all valid app command phrases
     */
    override fun getVoicePhrases(): List<String> = getAppPhrases()

    /**
     * Get all app command phrases for speech engine registration.
     *
     * Generates phrases by combining each open verb with each app name/alias.
     * For example: ["open WhatsApp", "launch WhatsApp", "open Spotify", "launch Spotify", ...]
     *
     * These phrases should be registered with the speech engine so it can
     * recognize app launch commands.
     *
     * @return List of all valid app command phrases
     */
    fun getAppPhrases(): List<String> {
        val phrases = mutableListOf<String>()

        for (app in appRegistry.values) {
            // Add phrases with display name
            for (verb in openVerbs) {
                phrases.add("$verb ${app.displayName.lowercase()}")
            }

            // Add phrases with each alias
            for (alias in app.aliases) {
                for (verb in openVerbs) {
                    phrases.add("$verb ${alias.lowercase()}")
                }
            }
        }

        return phrases.distinct()
    }

    /**
     * Sync registry with installed apps from the launcher.
     *
     * Fetches installed apps from the platform launcher and adds them
     * to the registry. Existing apps are updated if package matches.
     */
    fun syncInstalledApps() {
        appLauncher?.getInstalledApps()?.forEach { app ->
            registerApp(app)
        }
    }

    /**
     * Initialize the handler.
     *
     * 1. Loads localized command verbs from database (OPEN_APP, APP_SUFFIX)
     * 2. Syncs with installed apps from the platform launcher
     */
    override suspend fun initialize() {
        // Load localized verbs from database
        loadLocalizedVerbs()

        // Sync installed apps
        syncInstalledApps()

        LoggingUtils.d("[AppHandler] Initialized with ${openVerbs.size} verbs, ${appRegistry.size} apps", "AppHandler")
    }

    /**
     * Load localized command verbs from static command persistence.
     * Falls back to English defaults if persistence unavailable.
     */
    private suspend fun loadLocalizedVerbs() {
        if (persistence == null) {
            LoggingUtils.d("[AppHandler] No persistence, using default verbs", "AppHandler")
            return
        }

        try {
            // Load OPEN_APP verbs (open, launch, start, etc.)
            val verbSynonyms = persistence.getSynonymsForCommand(COMMAND_ID_OPEN_APP)
            if (verbSynonyms.isNotEmpty()) {
                openVerbs = verbSynonyms
                LoggingUtils.d("[AppHandler] Loaded ${verbSynonyms.size} open verbs from database", "AppHandler")
            }

            // Load APP_SUFFIX words (app, application, etc.)
            val suffixSynonyms = persistence.getSynonymsForCommand(COMMAND_ID_APP_SUFFIX)
            if (suffixSynonyms.isNotEmpty()) {
                appSuffixes = suffixSynonyms
                LoggingUtils.d("[AppHandler] Loaded ${suffixSynonyms.size} app suffixes from database", "AppHandler")
            }
        } catch (e: Exception) {
            LoggingUtils.w("[AppHandler] Failed to load localized verbs, using defaults: ${e.message}", "AppHandler")
        }
    }
}
