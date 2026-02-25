/**
 * SystemHandler.kt - Handles system-level actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
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

    override val supportedActions: List<String> = listOf(
        "go back", "back", "navigate back", "previous screen",
        "go home", "home", "navigate home", "open home",
        "show recents", "recents", "recent apps", "show recent apps", "open recents", "app switcher",
        "show notifications", "notifications", "notification panel",
        "quick settings",
        "power menu", "power dialog",
        "lock screen", "lock",
        "open app drawer", "app drawer", "all apps"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when (normalizedAction) {
            "go back", "back", "navigate back", "previous screen" -> {
                if (executor.goBack()) {
                    HandlerResult.success("Went back")
                } else {
                    HandlerResult.failure("Could not go back")
                }
            }

            "go home", "home", "navigate home", "open home" -> {
                if (executor.goHome()) {
                    HandlerResult.success("Went home")
                } else {
                    HandlerResult.failure("Could not go home")
                }
            }

            "show recents", "recents", "recent apps", "show recent apps", "open recents" , "app switcher" -> {
                if (executor.showRecents()) {
                    HandlerResult.success("Showing recent apps")
                } else {
                    HandlerResult.failure("Could not show recents")
                }
            }

            "show notifications", "notifications", "notification panel" -> {
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

            "open app drawer", "app drawer", "all apps" -> {
                if (executor.openAppDrawer()) {
                    HandlerResult.success("Opening app drawer")
                } else {
                    HandlerResult.failure("Could not open app drawer")
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
    suspend fun openAppDrawer(): Boolean
}
