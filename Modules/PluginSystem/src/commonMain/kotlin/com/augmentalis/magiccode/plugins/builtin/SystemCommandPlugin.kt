/**
 * SystemCommandPlugin.kt - System Command Handler as Universal Plugin
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Universal Plugin implementation for system-level actions.
 * Migrated from VoiceOSCore's SystemHandler to the Universal Plugin Architecture.
 *
 * Migration from: Modules/VoiceOSCore/src/commonMain/.../SystemHandler.kt
 */
package com.augmentalis.magiccode.plugins.builtin

import com.augmentalis.magiccode.plugins.sdk.BasePlugin
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * System Command Handler Plugin - Handles system-level actions.
 *
 * This plugin processes system commands including:
 * - Navigation: go back, go home
 * - App switching: show recents, recent apps
 * - System panels: notifications, quick settings
 * - Power actions: power menu, power dialog
 * - Security: lock screen
 *
 * ## Migration Notes
 * This plugin wraps the original SystemHandler logic from VoiceOSCore,
 * adapting it to the Universal Plugin interface while maintaining identical
 * behavior.
 *
 * ## Platform Considerations
 * System commands are highly platform-dependent. The executor interface
 * abstracts platform differences:
 * - Android: Uses AccessibilityService global actions
 * - iOS: Limited system control due to sandboxing
 * - Desktop: Uses native APIs or simulated keyboard shortcuts
 *
 * ## Usage
 * ```kotlin
 * val plugin = SystemCommandPlugin { executor }
 * plugin.initialize(config, context)
 *
 * val command = QuantizedCommand(phrase = "go home", ...)
 * if (plugin.canHandle(command, handlerContext)) {
 *     val result = plugin.handle(command, handlerContext)
 * }
 * ```
 *
 * @param executorProvider Provider function that returns the platform-specific executor.
 *        Using a provider allows lazy initialization when platform services may not be
 *        available at plugin creation time.
 *
 * @since 1.0.0
 * @see HandlerPlugin
 * @see BasePlugin
 * @see SystemCommandExecutor
 */
class SystemCommandPlugin(
    private val executorProvider: () -> SystemCommandExecutor
) : BasePlugin(), HandlerPlugin {

    // =========================================================================
    // Identity
    // =========================================================================

    override val pluginId: String = PLUGIN_ID
    override val pluginName: String = "System Command Handler"
    override val version: String = "1.0.0"

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability(
            id = PluginCapability.ACCESSIBILITY_HANDLER,
            name = "System Command Handler",
            version = "1.0.0",
            interfaces = setOf("HandlerPlugin"),
            metadata = mapOf(
                "handlerType" to "SYSTEM",
                "supportsBack" to "true",
                "supportsHome" to "true",
                "supportsRecents" to "true",
                "supportsNotifications" to "true",
                "supportsQuickSettings" to "true",
                "supportsPowerMenu" to "true",
                "supportsLockScreen" to "true"
            )
        )
    )

    // =========================================================================
    // Handler Properties
    // =========================================================================

    override val handlerType: HandlerType = HandlerType.SYSTEM

    override val patterns: List<CommandPattern> = listOf(
        // Back navigation
        CommandPattern(
            regex = Regex("^(go\\s+)?back$", RegexOption.IGNORE_CASE),
            intent = "BACK",
            requiredEntities = emptySet(),
            examples = listOf("back", "go back")
        ),
        // Home navigation
        CommandPattern(
            regex = Regex("^(go\\s+)?home$", RegexOption.IGNORE_CASE),
            intent = "HOME",
            requiredEntities = emptySet(),
            examples = listOf("home", "go home")
        ),
        // Recent apps
        CommandPattern(
            regex = Regex("^(show\\s+)?(recents?|recent\\s+apps?)$", RegexOption.IGNORE_CASE),
            intent = "RECENTS",
            requiredEntities = emptySet(),
            examples = listOf("recents", "show recents", "recent apps", "show recent apps")
        ),
        // Notifications
        CommandPattern(
            regex = Regex("^(show\\s+)?notifications?$", RegexOption.IGNORE_CASE),
            intent = "NOTIFICATIONS",
            requiredEntities = emptySet(),
            examples = listOf("notifications", "show notifications")
        ),
        // Quick settings
        CommandPattern(
            regex = Regex("^(show\\s+)?quick\\s+settings$", RegexOption.IGNORE_CASE),
            intent = "QUICK_SETTINGS",
            requiredEntities = emptySet(),
            examples = listOf("quick settings", "show quick settings")
        ),
        // Power menu
        CommandPattern(
            regex = Regex("^(show\\s+)?power\\s+(menu|dialog)$", RegexOption.IGNORE_CASE),
            intent = "POWER_MENU",
            requiredEntities = emptySet(),
            examples = listOf("power menu", "power dialog", "show power menu")
        ),
        // Lock screen
        CommandPattern(
            regex = Regex("^lock(\\s+screen)?$", RegexOption.IGNORE_CASE),
            intent = "LOCK",
            requiredEntities = emptySet(),
            examples = listOf("lock", "lock screen")
        )
    )

    // =========================================================================
    // Supported Actions (for discovery)
    // =========================================================================

    /**
     * List of supported action phrases.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String> = listOf(
        "go back", "back",
        "go home", "home",
        "show recents", "recents", "recent apps",
        "show notifications", "notifications",
        "quick settings", "show quick settings",
        "power menu", "power dialog", "show power menu",
        "lock screen", "lock"
    )

    // =========================================================================
    // Executor Reference
    // =========================================================================

    private lateinit var executor: SystemCommandExecutor

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override suspend fun onInitialize(): InitResult {
        return try {
            executor = executorProvider()
            InitResult.success("SystemCommandPlugin initialized")
        } catch (e: Exception) {
            InitResult.failure(e, recoverable = true)
        }
    }

    override suspend fun onShutdown() {
        // No resources to release
    }

    override fun getHealthDiagnostics(): Map<String, String> = mapOf(
        "supportedActions" to supportedActions.size.toString(),
        "patterns" to patterns.size.toString(),
        "executorInitialized" to (::executor.isInitialized).toString()
    )

    // =========================================================================
    // Handler Implementation
    // =========================================================================

    override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val phrase = command.phrase.lowercase().trim()

        // Check pattern matches
        if (patterns.any { it.matches(phrase) }) {
            return true
        }

        // Check exact action matches (normalized)
        val normalizedSupportedActions = supportedActions.map { it.lowercase() }
        return phrase in normalizedSupportedActions
    }

    override suspend fun handle(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when (normalizedAction) {
            // Back navigation
            "go back", "back" -> {
                if (executor.goBack()) {
                    ActionResult.Success("Went back")
                } else {
                    ActionResult.Error("Could not go back")
                }
            }

            // Home navigation
            "go home", "home" -> {
                if (executor.goHome()) {
                    ActionResult.Success("Went home")
                } else {
                    ActionResult.Error("Could not go home")
                }
            }

            // Recent apps
            "show recents", "recents", "recent apps", "show recent apps" -> {
                if (executor.showRecents()) {
                    ActionResult.Success("Showing recent apps")
                } else {
                    ActionResult.Error("Could not show recents")
                }
            }

            // Notifications
            "show notifications", "notifications" -> {
                if (executor.showNotifications()) {
                    ActionResult.Success("Showing notifications")
                } else {
                    ActionResult.Error("Could not show notifications")
                }
            }

            // Quick settings
            "quick settings", "show quick settings" -> {
                if (executor.showQuickSettings()) {
                    ActionResult.Success("Showing quick settings")
                } else {
                    ActionResult.Error("Could not show quick settings")
                }
            }

            // Power menu
            "power menu", "power dialog", "show power menu", "show power dialog" -> {
                if (executor.showPowerMenu()) {
                    ActionResult.Success("Showing power menu")
                } else {
                    ActionResult.Error("Could not show power menu")
                }
            }

            // Lock screen
            "lock screen", "lock" -> {
                if (executor.lockScreen()) {
                    ActionResult.Success("Screen locked")
                } else {
                    ActionResult.Error("Could not lock screen")
                }
            }

            else -> {
                // Try pattern matching for variations
                handlePatternMatch(normalizedAction)
            }
        }
    }

    /**
     * Handle commands via pattern matching for variations not in exact match list.
     */
    private suspend fun handlePatternMatch(phrase: String): ActionResult {
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return when (pattern.intent) {
                    "BACK" -> {
                        if (executor.goBack()) {
                            ActionResult.Success("Went back")
                        } else {
                            ActionResult.Error("Could not go back")
                        }
                    }
                    "HOME" -> {
                        if (executor.goHome()) {
                            ActionResult.Success("Went home")
                        } else {
                            ActionResult.Error("Could not go home")
                        }
                    }
                    "RECENTS" -> {
                        if (executor.showRecents()) {
                            ActionResult.Success("Showing recent apps")
                        } else {
                            ActionResult.Error("Could not show recents")
                        }
                    }
                    "NOTIFICATIONS" -> {
                        if (executor.showNotifications()) {
                            ActionResult.Success("Showing notifications")
                        } else {
                            ActionResult.Error("Could not show notifications")
                        }
                    }
                    "QUICK_SETTINGS" -> {
                        if (executor.showQuickSettings()) {
                            ActionResult.Success("Showing quick settings")
                        } else {
                            ActionResult.Error("Could not show quick settings")
                        }
                    }
                    "POWER_MENU" -> {
                        if (executor.showPowerMenu()) {
                            ActionResult.Success("Showing power menu")
                        } else {
                            ActionResult.Error("Could not show power menu")
                        }
                    }
                    "LOCK" -> {
                        if (executor.lockScreen()) {
                            ActionResult.Success("Screen locked")
                        } else {
                            ActionResult.Error("Could not lock screen")
                        }
                    }
                    else -> ActionResult.Error("Unknown intent: ${pattern.intent}")
                }
            }
        }

        return ActionResult.Error("Unknown system action: $phrase")
    }

    override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
        val phrase = command.phrase.lowercase().trim()

        // Exact match with supported actions
        val normalizedSupportedActions = supportedActions.map { it.lowercase() }
        if (phrase in normalizedSupportedActions) {
            return 1.0f
        }

        // Pattern match
        for (pattern in patterns) {
            if (pattern.matches(phrase)) {
                return 0.95f
            }
        }

        // Partial match (contains system-related keywords)
        val systemKeywords = listOf(
            "back", "home", "recents", "recent",
            "notifications", "notification",
            "quick settings", "power", "lock"
        )
        if (systemKeywords.any { phrase.contains(it) }) {
            return 0.6f
        }

        return 0.0f
    }

    companion object {
        /** Plugin ID for registration and discovery */
        const val PLUGIN_ID = "com.augmentalis.voiceoscore.handler.system"
    }
}

/**
 * Platform-specific executor interface for system commands.
 *
 * This interface defines the operations that must be implemented by
 * platform-specific code to perform actual system-level actions.
 *
 * Implementations are platform-specific:
 * - Android: Uses AccessibilityService global actions (GLOBAL_ACTION_BACK, etc.)
 * - iOS: Limited due to sandboxing - may use URL schemes or Shortcuts
 * - Desktop: Uses native APIs or simulated keyboard shortcuts (Alt+F4, etc.)
 *
 * ## Android Implementation Notes
 * Most system commands on Android require the AccessibilityService to be enabled
 * and the app to have the necessary permissions. The global actions available are:
 * - GLOBAL_ACTION_BACK
 * - GLOBAL_ACTION_HOME
 * - GLOBAL_ACTION_RECENTS
 * - GLOBAL_ACTION_NOTIFICATIONS
 * - GLOBAL_ACTION_QUICK_SETTINGS
 * - GLOBAL_ACTION_POWER_DIALOG (API 21+)
 * - GLOBAL_ACTION_LOCK_SCREEN (API 28+)
 *
 * @since 1.0.0
 */
interface SystemCommandExecutor {
    /**
     * Navigate back (equivalent to pressing the back button).
     *
     * On Android: Uses GLOBAL_ACTION_BACK
     * On iOS: May trigger navigation controller pop
     * On Desktop: May simulate Escape or Alt+Left
     *
     * @return true if the back action was performed successfully
     */
    suspend fun goBack(): Boolean

    /**
     * Navigate to the home screen.
     *
     * On Android: Uses GLOBAL_ACTION_HOME
     * On iOS: Not available due to sandboxing
     * On Desktop: May minimize all windows or show desktop
     *
     * @return true if the home action was performed successfully
     */
    suspend fun goHome(): Boolean

    /**
     * Show the recent apps/task switcher.
     *
     * On Android: Uses GLOBAL_ACTION_RECENTS
     * On iOS: Not available due to sandboxing
     * On Desktop: May use Alt+Tab or Mission Control
     *
     * @return true if recents were shown successfully
     */
    suspend fun showRecents(): Boolean

    /**
     * Show the notification panel/shade.
     *
     * On Android: Uses GLOBAL_ACTION_NOTIFICATIONS
     * On iOS: Not available due to sandboxing
     * On Desktop: May show notification center
     *
     * @return true if notifications were shown successfully
     */
    suspend fun showNotifications(): Boolean

    /**
     * Show quick settings panel.
     *
     * On Android: Uses GLOBAL_ACTION_QUICK_SETTINGS
     * On iOS: Control Center (not directly accessible)
     * On Desktop: May show system tray or control panel
     *
     * @return true if quick settings were shown successfully
     */
    suspend fun showQuickSettings(): Boolean

    /**
     * Show the power menu/dialog.
     *
     * On Android: Uses GLOBAL_ACTION_POWER_DIALOG (API 21+)
     * On iOS: Not available
     * On Desktop: May show shutdown dialog
     *
     * @return true if power menu was shown successfully
     */
    suspend fun showPowerMenu(): Boolean

    /**
     * Lock the screen.
     *
     * On Android: Uses GLOBAL_ACTION_LOCK_SCREEN (API 28+) or DevicePolicyManager
     * On iOS: Not available
     * On Desktop: Uses platform-specific lock mechanism
     *
     * @return true if the screen was locked successfully
     */
    suspend fun lockScreen(): Boolean
}

// =============================================================================
// Factory Functions
// =============================================================================

/**
 * Create a SystemCommandPlugin with a pre-configured executor.
 *
 * @param executor The system command executor implementation
 * @return Configured SystemCommandPlugin
 */
fun createSystemCommandPlugin(
    executor: SystemCommandExecutor
): SystemCommandPlugin {
    return SystemCommandPlugin { executor }
}

/**
 * Create a SystemCommandPlugin with a lazy executor provider.
 *
 * Useful when the executor depends on platform services that may
 * not be available at plugin creation time.
 *
 * @param executorProvider Function that returns the executor when needed
 * @return Configured SystemCommandPlugin
 */
fun createSystemCommandPlugin(
    executorProvider: () -> SystemCommandExecutor
): SystemCommandPlugin {
    return SystemCommandPlugin(executorProvider)
}

// =============================================================================
// Testing Support
// =============================================================================

/**
 * Mock executor for testing SystemCommandPlugin.
 *
 * Records all actions and can be configured to succeed or fail.
 * Useful for unit testing command handling without platform dependencies.
 *
 * ## Usage
 * ```kotlin
 * val mockExecutor = MockSystemCommandExecutor(shouldSucceed = true)
 * val plugin = createSystemCommandPlugin(mockExecutor)
 *
 * // Execute command
 * plugin.handle(command, context)
 *
 * // Verify actions
 * assertEquals(listOf("goBack"), mockExecutor.actions)
 * ```
 *
 * @param shouldSucceed Whether operations should succeed or fail
 */
class MockSystemCommandExecutor(
    private val shouldSucceed: Boolean = true
) : SystemCommandExecutor {

    private val _actions = mutableListOf<String>()

    /** List of recorded actions */
    val actions: List<String> get() = _actions.toList()

    /** Clear recorded actions */
    fun clearActions() = _actions.clear()

    override suspend fun goBack(): Boolean {
        _actions.add("goBack")
        return shouldSucceed
    }

    override suspend fun goHome(): Boolean {
        _actions.add("goHome")
        return shouldSucceed
    }

    override suspend fun showRecents(): Boolean {
        _actions.add("showRecents")
        return shouldSucceed
    }

    override suspend fun showNotifications(): Boolean {
        _actions.add("showNotifications")
        return shouldSucceed
    }

    override suspend fun showQuickSettings(): Boolean {
        _actions.add("showQuickSettings")
        return shouldSucceed
    }

    override suspend fun showPowerMenu(): Boolean {
        _actions.add("showPowerMenu")
        return shouldSucceed
    }

    override suspend fun lockScreen(): Boolean {
        _actions.add("lockScreen")
        return shouldSucceed
    }
}

/**
 * Mock executor that tracks execution counts and allows per-action configuration.
 *
 * Useful for testing scenarios where different actions should have different
 * success/failure behaviors.
 *
 * ## Usage
 * ```kotlin
 * val executor = ConfigurableMockSystemExecutor().apply {
 *     setResult("goBack", true)
 *     setResult("goHome", false) // Simulate home action failure
 * }
 * ```
 */
class ConfigurableMockSystemExecutor : SystemCommandExecutor {

    private val _actionCounts = mutableMapOf<String, Int>()
    private val _actionResults = mutableMapOf<String, Boolean>()
    private val _defaultResult: Boolean = true

    /** Map of action names to execution counts */
    val actionCounts: Map<String, Int> get() = _actionCounts.toMap()

    /** Total number of actions executed */
    val totalActions: Int get() = _actionCounts.values.sum()

    /**
     * Configure the result for a specific action.
     *
     * @param action Action name (goBack, goHome, etc.)
     * @param result Whether the action should succeed
     */
    fun setResult(action: String, result: Boolean) {
        _actionResults[action] = result
    }

    /**
     * Reset all configuration and counts.
     */
    fun reset() {
        _actionCounts.clear()
        _actionResults.clear()
    }

    private fun recordAction(action: String): Boolean {
        _actionCounts[action] = (_actionCounts[action] ?: 0) + 1
        return _actionResults[action] ?: _defaultResult
    }

    override suspend fun goBack(): Boolean = recordAction("goBack")
    override suspend fun goHome(): Boolean = recordAction("goHome")
    override suspend fun showRecents(): Boolean = recordAction("showRecents")
    override suspend fun showNotifications(): Boolean = recordAction("showNotifications")
    override suspend fun showQuickSettings(): Boolean = recordAction("showQuickSettings")
    override suspend fun showPowerMenu(): Boolean = recordAction("showPowerMenu")
    override suspend fun lockScreen(): Boolean = recordAction("lockScreen")
}

/**
 * Stub executor that always fails - useful for testing error handling.
 */
object FailingSystemCommandExecutor : SystemCommandExecutor {
    override suspend fun goBack(): Boolean = false
    override suspend fun goHome(): Boolean = false
    override suspend fun showRecents(): Boolean = false
    override suspend fun showNotifications(): Boolean = false
    override suspend fun showQuickSettings(): Boolean = false
    override suspend fun showPowerMenu(): Boolean = false
    override suspend fun lockScreen(): Boolean = false
}

/**
 * Stub executor that always succeeds - useful for simple integration tests.
 */
object SuccessfulSystemCommandExecutor : SystemCommandExecutor {
    override suspend fun goBack(): Boolean = true
    override suspend fun goHome(): Boolean = true
    override suspend fun showRecents(): Boolean = true
    override suspend fun showNotifications(): Boolean = true
    override suspend fun showQuickSettings(): Boolean = true
    override suspend fun showPowerMenu(): Boolean = true
    override suspend fun lockScreen(): Boolean = true
}
