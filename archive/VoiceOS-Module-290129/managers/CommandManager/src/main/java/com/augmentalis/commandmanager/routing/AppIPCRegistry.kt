/**
 * AppIPCRegistry.kt - Registry for app-specific IPC actions
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-24
 *
 * Maps app package names to their specific IPC broadcast actions.
 * Each app in the Avanues ecosystem has its own IPC action for security and isolation.
 *
 * Universal IPC Protocol v2.0.0
 * Each app listens on app-specific action: com.augmentalis.{app}.IPC.COMMAND
 *
 * Examples:
 * - WebAvanue: com.augmentalis.avanues.web.IPC.COMMAND
 * - AVA AI: com.augmentalis.ava.IPC.COMMAND
 * - AvaConnect: com.augmentalis.avaconnect.IPC.COMMAND
 */
package com.augmentalis.voiceoscore.routing

import android.util.Log

/**
 * Registry for mapping app packages to IPC actions
 *
 * This registry enables secure, isolated IPC communication by ensuring
 * each app receives broadcasts on its own dedicated action string.
 *
 * Architecture:
 * - Each app defines its own IPC action in its Application class
 * - VoiceOS CommandManager uses this registry to route commands
 * - Apps register themselves in the registry on first launch
 * - Fallback to package-based action if not registered
 */
object AppIPCRegistry {

    private const val TAG = "AppIPCRegistry"

    /**
     * Static registry of known Avanues ecosystem apps
     * Maps package name → IPC action
     */
    private val staticRegistry = mutableMapOf<String, AppIPCEntry>()

    init {
        // Initialize with known Avanues ecosystem apps
        registerKnownApps()
    }

    /**
     * Register known Avanues ecosystem apps
     * These are pre-configured to avoid discovery latency
     */
    private fun registerKnownApps() {
        // Avanues - Main launcher/hub
        register(
            packageName = "com.augmentalis.avanues",
            ipcAction = "com.augmentalis.avanues.IPC.COMMAND",
            appName = "Avanues",
            appType = AppType.UTILITY,
            description = "Avanues ecosystem launcher and hub"
        )

        // WebAvanue - Browser module
        register(
            packageName = "com.augmentalis.Avanues.web",
            ipcAction = "com.augmentalis.avanues.web.IPC.COMMAND",
            appName = "WebAvanue",
            appType = AppType.BROWSER,
            description = "Web browser with voice control"
        )

        // AVA AI - Main AI assistant
        register(
            packageName = "com.augmentalis.ava",
            ipcAction = "com.augmentalis.ava.IPC.COMMAND",
            appName = "AVA AI",
            appType = AppType.AI_ASSISTANT,
            description = "AI-powered voice assistant"
        )

        // AvaConnect - P2P communication
        register(
            packageName = "com.augmentalis.avaconnect",
            ipcAction = "com.augmentalis.avaconnect.IPC.COMMAND",
            appName = "AvaConnect",
            appType = AppType.COMMUNICATION,
            description = "Peer-to-peer voice communication"
        )

        Log.i(TAG, "Initialized AppIPCRegistry with ${staticRegistry.size} apps")
    }

    /**
     * Register an app with its IPC action
     *
     * @param packageName App package name (e.g., "com.augmentalis.avanues.web")
     * @param ipcAction IPC broadcast action (e.g., "com.augmentalis.avanues.web.IPC.COMMAND")
     * @param appName User-friendly app name (e.g., "WebAvanue")
     * @param appType Type of app (browser, assistant, etc.)
     * @param description Optional description
     */
    fun register(
        packageName: String,
        ipcAction: String,
        appName: String,
        appType: AppType,
        description: String? = null
    ) {
        val entry = AppIPCEntry(
            packageName = packageName,
            ipcAction = ipcAction,
            appName = appName,
            appType = appType,
            description = description,
            registeredAt = System.currentTimeMillis()
        )

        staticRegistry[packageName] = entry
        Log.d(TAG, "Registered: $packageName → $ipcAction ($appName)")
    }

    /**
     * Get IPC action for a package name
     *
     * @param packageName App package name
     * @return IPC action string, or null if not registered
     *
     * Examples:
     * - getIPCAction("com.augmentalis.avanues.web") → "com.augmentalis.avanues.web.IPC.COMMAND"
     * - getIPCAction("com.augmentalis.ava") → "com.augmentalis.ava.IPC.COMMAND"
     */
    fun getIPCAction(packageName: String): String? {
        return staticRegistry[packageName]?.ipcAction
    }

    /**
     * Get IPC action with fallback
     *
     * If app is not registered, generates a default IPC action based on package name.
     * This ensures commands can still be sent to new apps.
     *
     * @param packageName App package name
     * @return IPC action string (never null)
     *
     * Fallback format: {packageName}.IPC.COMMAND
     * Example: com.augmentalis.newapp → com.augmentalis.newapp.IPC.COMMAND
     */
    fun getIPCActionWithFallback(packageName: String): String {
        return staticRegistry[packageName]?.ipcAction
            ?: generateDefaultIPCAction(packageName)
    }

    /**
     * Generate default IPC action from package name
     * Used as fallback for unregistered apps
     */
    private fun generateDefaultIPCAction(packageName: String): String {
        val action = "$packageName.IPC.COMMAND"
        Log.w(TAG, "No IPC action registered for $packageName, using fallback: $action")
        return action
    }

    /**
     * Check if an app is registered
     */
    fun isRegistered(packageName: String): Boolean {
        return staticRegistry.containsKey(packageName)
    }

    /**
     * Get app entry with full metadata
     */
    fun getAppEntry(packageName: String): AppIPCEntry? {
        return staticRegistry[packageName]
    }

    /**
     * Get all registered apps
     */
    fun getAllApps(): List<AppIPCEntry> {
        return staticRegistry.values.toList()
    }

    /**
     * Get all apps of a specific type
     */
    fun getAppsByType(type: AppType): List<AppIPCEntry> {
        return staticRegistry.values.filter { it.appType == type }
    }

    /**
     * Unregister an app (for testing/cleanup)
     */
    fun unregister(packageName: String): Boolean {
        val removed = staticRegistry.remove(packageName) != null
        if (removed) {
            Log.d(TAG, "Unregistered: $packageName")
        }
        return removed
    }

    /**
     * Clear all registrations (for testing)
     */
    fun clear() {
        staticRegistry.clear()
        Log.w(TAG, "AppIPCRegistry cleared")
    }

    /**
     * Get registry statistics
     */
    fun getStats(): RegistryStats {
        val typeCount = AppType.values().associateWith { type ->
            staticRegistry.values.count { it.appType == type }
        }

        return RegistryStats(
            totalApps = staticRegistry.size,
            appsByType = typeCount,
            registeredApps = staticRegistry.keys.toList()
        )
    }
}

/**
 * App IPC registry entry
 */
data class AppIPCEntry(
    val packageName: String,
    val ipcAction: String,
    val appName: String,
    val appType: AppType,
    val description: String?,
    val registeredAt: Long
)

/**
 * App types in Avanues ecosystem
 */
enum class AppType {
    BROWSER,           // Web browsers (WebAvanue, BrowserAvanue)
    AI_ASSISTANT,      // AI assistants (AVA AI)
    COMMUNICATION,     // Communication apps (AvaConnect)
    PRODUCTIVITY,      // Productivity apps
    MEDIA,             // Media players
    UTILITY,           // Utility apps
    OTHER              // Other apps
}

/**
 * Registry statistics
 */
data class RegistryStats(
    val totalApps: Int,
    val appsByType: Map<AppType, Int>,
    val registeredApps: List<String>
)
