/**
 * InstalledAppsManager.kt - Manager for installed app detection and voice command generation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-16
 *
 * Responsibilities:
 * - Detecting installed applications via PackageManager
 * - Generating voice commands for apps ("open [app name]")
 * - Monitoring app installation/uninstallation via BroadcastReceiver
 * - Caching app information for performance
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages installed applications and generates voice commands for launching them.
 *
 * This class detects all user-installed apps and creates voice commands like:
 * - "open chrome" → launches Chrome browser
 * - "open gmail" → launches Gmail app
 *
 * Features:
 * - Auto-refresh on app install/uninstall via BroadcastReceiver
 * - Caches app list for performance (updates only on package changes)
 * - Filters system apps (only exposes user-installed apps)
 * - Handles app label normalization (lowercase, spaces removed)
 *
 * Usage:
 * ```kotlin
 * val manager = InstalledAppsManager(context)
 * manager.initialize()
 * manager.appList.collect { appCommands ->
 *     // appCommands: Map<String, String>
 *     // Key: "open chrome", Value: "com.android.chrome"
 * }
 * manager.cleanup() // Call in onDestroy()
 * ```
 */
class InstalledAppsManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "InstalledAppsManager"
        private const val COMMAND_PREFIX = "open "
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * StateFlow of voice commands for installed apps.
     * Key: Voice command phrase (e.g., "open chrome")
     * Value: Package name (e.g., "com.android.chrome")
     */
    private val _appList = MutableStateFlow<Map<String, String>>(emptyMap())
    val appList: StateFlow<Map<String, String>> = _appList.asStateFlow()

    /**
     * BroadcastReceiver to monitor app install/uninstall events
     */
    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Log.d(TAG, "Package change detected: ${intent.action} - $packageName")
                    refreshAppList()
                }
            }
        }
    }

    private var isInitialized = false

    /**
     * Initialize the manager and start monitoring package changes.
     * Must be called before using appList.
     */
    fun initialize() {
        if (isInitialized) {
            Log.w(TAG, "InstalledAppsManager already initialized")
            return
        }

        Log.d(TAG, "Initializing InstalledAppsManager...")

        // Register broadcast receiver for package changes
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(packageChangeReceiver, filter)

        // Initial app list load
        refreshAppList()

        isInitialized = true
        Log.i(TAG, "✓ InstalledAppsManager initialized")
    }

    /**
     * Cleanup resources and unregister receivers.
     * Must be called in onDestroy() to prevent memory leaks.
     */
    fun cleanup() {
        if (!isInitialized) return

        Log.d(TAG, "Cleaning up InstalledAppsManager...")
        try {
            context.unregisterReceiver(packageChangeReceiver)
            _appList.value = emptyMap()
            isInitialized = false
            Log.i(TAG, "✓ InstalledAppsManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    /**
     * Refresh the app list from PackageManager.
     * Called automatically on package changes, but can be called manually if needed.
     */
    fun refreshAppList() {
        scope.launch {
            try {
                val apps = loadInstalledApps()
                _appList.value = apps
                Log.d(TAG, "App list refreshed: ${apps.size} apps")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing app list", e)
            }
        }
    }

    /**
     * Load installed apps from PackageManager.
     * Filters out system apps and creates voice commands.
     *
     * @return Map of voice commands to package names
     */
    private suspend fun loadInstalledApps(): Map<String, String> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val apps = mutableMapOf<String, String>()

        try {
            // Get all installed packages
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in packages) {
                // Filter: Only include user-installed apps (not system apps)
                if (isUserApp(appInfo)) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName

                    // Normalize app name for voice command
                    val normalizedName = normalizeAppName(appName)
                    val command = "$COMMAND_PREFIX$normalizedName"

                    // Handle duplicates: If command already exists, append package name suffix
                    val finalCommand = if (apps.containsKey(command)) {
                        "$command ${packageName.substringAfterLast('.')}"
                    } else {
                        command
                    }

                    apps[finalCommand] = packageName
                    Log.v(TAG, "Added app: $finalCommand → $packageName")
                }
            }

            Log.i(TAG, "Loaded ${apps.size} user-installed apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading installed apps", e)
        }

        return@withContext apps
    }

    /**
     * Check if app is a user-installed app (not a system app).
     *
     * @param appInfo Application info to check
     * @return true if user-installed app, false if system app
     */
    private fun isUserApp(appInfo: ApplicationInfo): Boolean {
        // System apps have FLAG_SYSTEM flag
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // Updated system apps (like Chrome) have FLAG_UPDATED_SYSTEM_APP
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        // Include: User apps OR updated system apps
        // Exclude: Pure system apps
        return !isSystemApp || isUpdatedSystemApp
    }

    /**
     * Normalize app name for voice command generation.
     * - Converts to lowercase
     * - Removes special characters
     * - Replaces multiple spaces with single space
     * - Trims whitespace
     *
     * Examples:
     * - "Google Chrome" → "google chrome"
     * - "My App (Beta)" → "my app beta"
     * - "App   Name" → "app name"
     *
     * @param appName Original app name from PackageManager
     * @return Normalized name suitable for voice commands
     */
    private fun normalizeAppName(appName: String): String {
        return appName
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ") // Remove special chars
            .replace(Regex("\\s+"), " ") // Collapse multiple spaces
            .trim()
    }

    /**
     * Get package name for a given app label.
     * Useful for resolving "open [app name]" commands.
     *
     * @param appName App label (e.g., "chrome")
     * @return Package name if found, null otherwise
     */
    fun getPackageNameForApp(appName: String): String? {
        val normalizedName = normalizeAppName(appName)
        val command = "$COMMAND_PREFIX$normalizedName"
        return _appList.value[command]
    }

    /**
     * Check if a specific app is installed.
     *
     * @param packageName Package name to check
     * @return true if installed, false otherwise
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get app label for a package name.
     *
     * @param packageName Package name
     * @return App label, or package name if label not found
     */
    fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
