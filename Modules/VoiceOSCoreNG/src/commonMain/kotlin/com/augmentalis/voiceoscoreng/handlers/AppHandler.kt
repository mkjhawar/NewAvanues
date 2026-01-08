/**
 * AppHandler.kt - Handles app launching and management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP handler for app-level actions (launch, switch, close, etc.).
 * Phase 12 of the VoiceOSCoreNG handler system.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand

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
 * Apps can be found by:
 * - Display name (e.g., "Google Maps")
 * - Package name (e.g., "com.google.android.apps.maps")
 * - Alias (e.g., "maps", "navigation")
 *
 * Usage:
 * ```kotlin
 * val handler = AppHandler(androidAppLauncher)
 *
 * // Handle voice command
 * val result = handler.handleCommand("open maps")
 * if (result.success) {
 *     println("Opened ${result.app?.displayName}")
 * }
 *
 * // Register custom app
 * handler.registerApp(AppInfo("com.myapp", "My App", listOf("myapp")))
 *
 * // Sync with installed apps
 * handler.syncInstalledApps()
 * ```
 *
 * @param appLauncher Platform-specific app launcher (optional, null for simulation mode)
 */
class AppHandler(
    private val appLauncher: IAppLauncher? = null
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.APP

    override val supportedActions: List<String> = listOf(
        "open", "launch", "start",
        "open app", "launch app", "start app"
    )

    /**
     * Registry of known apps, keyed by package name.
     */
    private val appRegistry = mutableMapOf<String, AppInfo>()

    init {
        registerCommonApps()
    }

    /**
     * Register common apps with their aliases.
     * These are pre-populated for better out-of-box voice recognition.
     */
    private fun registerCommonApps() {
        registerApp(
            AppInfo(
                packageName = "com.google.android.apps.maps",
                displayName = "Google Maps",
                aliases = listOf("maps", "navigation")
            )
        )
        registerApp(
            AppInfo(
                packageName = "com.google.android.youtube",
                displayName = "YouTube",
                aliases = listOf("youtube", "videos")
            )
        )
        registerApp(
            AppInfo(
                packageName = "com.android.chrome",
                displayName = "Chrome",
                aliases = listOf("chrome", "browser", "internet")
            )
        )
        registerApp(
            AppInfo(
                packageName = "com.android.settings",
                displayName = "Settings",
                aliases = listOf("settings", "preferences")
            )
        )
        registerApp(
            AppInfo(
                packageName = "com.android.dialer",
                displayName = "Phone",
                aliases = listOf("phone", "dialer", "call")
            )
        )
        registerApp(
            AppInfo(
                packageName = "com.android.mms",
                displayName = "Messages",
                aliases = listOf("messages", "sms", "texting")
            )
        )
        registerApp(
            AppInfo(
                packageName = "com.android.camera",
                displayName = "Camera",
                aliases = listOf("camera", "photo")
            )
        )
    }

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
     * Supports prefixes: "open", "launch", "start"
     *
     * @param command Full command string (e.g., "open maps")
     * @return Result of the app launch attempt
     */
    fun handleCommand(command: String): AppLaunchResult {
        val normalizedCommand = command.lowercase().trim()

        return when {
            normalizedCommand.startsWith("open ") -> {
                openApp(normalizedCommand.substringAfter("open "))
            }
            normalizedCommand.startsWith("launch ") -> {
                openApp(normalizedCommand.substringAfter("launch "))
            }
            normalizedCommand.startsWith("start ") -> {
                openApp(normalizedCommand.substringAfter("start "))
            }
            else -> AppLaunchResult(
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
}
