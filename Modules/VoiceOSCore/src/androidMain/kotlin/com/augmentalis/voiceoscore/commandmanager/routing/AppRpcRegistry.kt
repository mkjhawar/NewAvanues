/**
 * AppRpcRegistry.kt - Registry for app-specific RPC actions
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-24
 * Renamed: 2026-02-02 (IPC → RPC)
 *
 * Maps app package names to their specific RPC actions.
 * Each app in the Avanues ecosystem has its own RPC action for security and isolation.
 *
 * Universal RPC Protocol v2.0.0
 * Each app listens on app-specific action: com.augmentalis.{app}.RPC.COMMAND
 *
 * Examples:
 * - WebAvanue: com.augmentalis.webavanue.RPC.COMMAND
 * - AVA AI: com.augmentalis.ava.RPC.COMMAND
 * - AvaConnect: com.augmentalis.avaconnect.RPC.COMMAND
 */
package com.augmentalis.voiceoscore.commandmanager.routing

import android.util.Log

/**
 * Registry for mapping app packages to RPC actions
 *
 * This registry enables secure, isolated RPC communication by ensuring
 * each app receives broadcasts on its own dedicated action string.
 *
 * Architecture:
 * - Each app defines its own RPC action in its Application class
 * - VoiceOS CommandManager uses this registry to route commands
 * - Apps register themselves in the registry on first launch
 * - Fallback to package-based action if not registered
 */
object AppRpcRegistry {

    private const val TAG = "AppRpcRegistry"

    /**
     * Static registry of known Avanues ecosystem apps
     * Maps package name → RPC action
     */
    private val staticRegistry = mutableMapOf<String, AppRpcEntry>()

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
            rpcAction = "com.augmentalis.avanues.RPC.COMMAND",
            appName = "Avanues",
            appType = AppType.UTILITY,
            description = "Avanues ecosystem launcher and hub"
        )

        // WebAvanue - Browser module
        register(
            packageName = "com.augmentalis.webavanue",
            rpcAction = "com.augmentalis.webavanue.RPC.COMMAND",
            appName = "WebAvanue",
            appType = AppType.BROWSER,
            description = "Web browser with voice control"
        )

        // AVA AI - Main AI assistant
        register(
            packageName = "com.augmentalis.ava",
            rpcAction = "com.augmentalis.ava.RPC.COMMAND",
            appName = "AVA AI",
            appType = AppType.AI_ASSISTANT,
            description = "AI-powered voice assistant"
        )

        // AvaConnect - P2P communication
        register(
            packageName = "com.augmentalis.avaconnect",
            rpcAction = "com.augmentalis.avaconnect.RPC.COMMAND",
            appName = "AvaConnect",
            appType = AppType.COMMUNICATION,
            description = "Peer-to-peer voice communication"
        )

        Log.i(TAG, "Initialized AppRpcRegistry with ${staticRegistry.size} apps")
    }

    /**
     * Register an app with its RPC action
     *
     * @param packageName App package name (e.g., "com.augmentalis.webavanue")
     * @param rpcAction RPC broadcast action (e.g., "com.augmentalis.webavanue.RPC.COMMAND")
     * @param appName User-friendly app name (e.g., "WebAvanue")
     * @param appType Type of app (browser, assistant, etc.)
     * @param description Optional description
     */
    fun register(
        packageName: String,
        rpcAction: String,
        appName: String,
        appType: AppType,
        description: String? = null
    ) {
        val entry = AppRpcEntry(
            packageName = packageName,
            rpcAction = rpcAction,
            appName = appName,
            appType = appType,
            description = description,
            registeredAt = System.currentTimeMillis()
        )

        staticRegistry[packageName] = entry
        Log.d(TAG, "Registered: $packageName → $rpcAction ($appName)")
    }

    /**
     * Get RPC action for a package name
     *
     * @param packageName App package name
     * @return RPC action string, or null if not registered
     *
     * Examples:
     * - getRpcAction("com.augmentalis.webavanue") → "com.augmentalis.webavanue.RPC.COMMAND"
     * - getRpcAction("com.augmentalis.ava") → "com.augmentalis.ava.RPC.COMMAND"
     */
    fun getRpcAction(packageName: String): String? {
        return staticRegistry[packageName]?.rpcAction
    }

    /**
     * Get RPC action with fallback
     *
     * If app is not registered, generates a default RPC action based on package name.
     * This ensures commands can still be sent to new apps.
     *
     * @param packageName App package name
     * @return RPC action string (never null)
     *
     * Fallback format: {packageName}.RPC.COMMAND
     * Example: com.augmentalis.newapp → com.augmentalis.newapp.RPC.COMMAND
     */
    fun getRpcActionWithFallback(packageName: String): String {
        return staticRegistry[packageName]?.rpcAction
            ?: generateDefaultRpcAction(packageName)
    }

    /**
     * Generate default RPC action from package name
     * Used as fallback for unregistered apps
     */
    private fun generateDefaultRpcAction(packageName: String): String {
        val action = "$packageName.RPC.COMMAND"
        Log.w(TAG, "No RPC action registered for $packageName, using fallback: $action")
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
    fun getAppEntry(packageName: String): AppRpcEntry? {
        return staticRegistry[packageName]
    }

    /**
     * Get all registered apps
     */
    fun getAllApps(): List<AppRpcEntry> {
        return staticRegistry.values.toList()
    }

    /**
     * Get all apps of a specific type
     */
    fun getAppsByType(type: AppType): List<AppRpcEntry> {
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
        Log.w(TAG, "AppRpcRegistry cleared")
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
 * App RPC registry entry
 */
data class AppRpcEntry(
    val packageName: String,
    val rpcAction: String,
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
