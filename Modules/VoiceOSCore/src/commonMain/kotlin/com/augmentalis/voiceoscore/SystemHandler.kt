/**
 * SystemHandler.kt - Handles system-level actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Refactored: 2026-02-02 - Simplified to map-based dispatch
 *
 * KMP handler for system-level actions (back, home, recents, etc.).
 */
package com.augmentalis.voiceoscore

/**
 * Handler for system-level actions.
 *
 * Supports:
 * - Navigation: back, home, recents, notifications
 * - Quick settings
 * - Power actions
 * - Lock screen
 */
class SystemHandler(
    private val executor: SystemExecutor
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.SYSTEM

    /**
     * Command configuration: maps phrases to (executor function, success message, failure message)
     */
    private data class CommandConfig(
        val action: suspend SystemExecutor.() -> Boolean,
        val successMessage: String,
        val failureMessage: String
    )

    /**
     * Command dispatch map - phrases mapped to their configurations.
     */
    private val commandMap: Map<String, CommandConfig> = mapOf(
        // Navigation
        "go back" to CommandConfig(SystemExecutor::goBack, "Went back", "Could not go back"),
        "back" to CommandConfig(SystemExecutor::goBack, "Went back", "Could not go back"),
        "go home" to CommandConfig(SystemExecutor::goHome, "Went home", "Could not go home"),
        "home" to CommandConfig(SystemExecutor::goHome, "Went home", "Could not go home"),
        "show recents" to CommandConfig(SystemExecutor::showRecents, "Showing recent apps", "Could not show recents"),
        "recents" to CommandConfig(SystemExecutor::showRecents, "Showing recent apps", "Could not show recents"),
        "recent apps" to CommandConfig(SystemExecutor::showRecents, "Showing recent apps", "Could not show recents"),
        // Notifications
        "show notifications" to CommandConfig(SystemExecutor::showNotifications, "Showing notifications", "Could not show notifications"),
        "notifications" to CommandConfig(SystemExecutor::showNotifications, "Showing notifications", "Could not show notifications"),
        // Quick settings
        "quick settings" to CommandConfig(SystemExecutor::showQuickSettings, "Showing quick settings", "Could not show quick settings"),
        // Power
        "power menu" to CommandConfig(SystemExecutor::showPowerMenu, "Showing power menu", "Could not show power menu"),
        "power dialog" to CommandConfig(SystemExecutor::showPowerMenu, "Showing power menu", "Could not show power menu"),
        // Lock
        "lock screen" to CommandConfig(SystemExecutor::lockScreen, "Screen locked", "Could not lock screen"),
        "lock" to CommandConfig(SystemExecutor::lockScreen, "Screen locked", "Could not lock screen")
    )

    override val supportedActions: List<String> = commandMap.keys.toList()

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()
        val config = commandMap[normalizedAction] ?: return HandlerResult.notHandled()

        return if (config.action(executor)) {
            HandlerResult.success(config.successMessage)
        } else {
            HandlerResult.failure(config.failureMessage)
        }
    }
}

/**
 * Platform-specific executor for system actions.
 */
interface SystemExecutor {
    suspend fun goBack(): Boolean
    suspend fun goHome(): Boolean
    suspend fun showRecents(): Boolean
    suspend fun showNotifications(): Boolean
    suspend fun showQuickSettings(): Boolean
    suspend fun showPowerMenu(): Boolean
    suspend fun lockScreen(): Boolean
}
