/**
 * SystemHandler.kt - Handles system-level actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP handler for system-level actions (back, home, recents, etc.).
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.QuantizedCommand

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

    override val supportedActions: List<String> = listOf(
        "go back", "back",
        "go home", "home",
        "show recents", "recents", "recent apps",
        "show notifications", "notifications",
        "quick settings",
        "power menu", "power dialog",
        "lock screen", "lock"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when (normalizedAction) {
            "go back", "back" -> {
                if (executor.goBack()) {
                    HandlerResult.success("Went back")
                } else {
                    HandlerResult.failure("Could not go back")
                }
            }

            "go home", "home" -> {
                if (executor.goHome()) {
                    HandlerResult.success("Went home")
                } else {
                    HandlerResult.failure("Could not go home")
                }
            }

            "show recents", "recents", "recent apps" -> {
                if (executor.showRecents()) {
                    HandlerResult.success("Showing recent apps")
                } else {
                    HandlerResult.failure("Could not show recents")
                }
            }

            "show notifications", "notifications" -> {
                if (executor.showNotifications()) {
                    HandlerResult.success("Showing notifications")
                } else {
                    HandlerResult.failure("Could not show notifications")
                }
            }

            "quick settings" -> {
                if (executor.showQuickSettings()) {
                    HandlerResult.success("Showing quick settings")
                } else {
                    HandlerResult.failure("Could not show quick settings")
                }
            }

            "power menu", "power dialog" -> {
                if (executor.showPowerMenu()) {
                    HandlerResult.success("Showing power menu")
                } else {
                    HandlerResult.failure("Could not show power menu")
                }
            }

            "lock screen", "lock" -> {
                if (executor.lockScreen()) {
                    HandlerResult.success("Screen locked")
                } else {
                    HandlerResult.failure("Could not lock screen")
                }
            }

            else -> HandlerResult.notHandled()
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
