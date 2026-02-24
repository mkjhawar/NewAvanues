/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of Action Coordinator.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.intentactions.IntentResult
import com.augmentalis.chat.data.BuiltInIntents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop (JVM) implementation of IActionCoordinator.
 *
 * On desktop platforms, actions can be executed via:
 * - System commands (ProcessBuilder)
 * - Desktop integrations (file operations, browser launch, etc.)
 * - REST API calls to external services
 *
 * This implementation provides the interface with stub functionality
 * for core intents and can be extended with actual desktop actions.
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class ActionCoordinatorDesktop : IActionCoordinator {

    // ==================== State ====================

    private val _showAccessibilityPrompt = MutableStateFlow(false)
    override val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

    // ==================== Internal State ====================

    private var initialized = false

    // Registered action handlers
    private val actionHandlers = mutableMapOf<String, suspend (String) -> IntentResult>()

    // Execution stats
    private var totalExecutions = 0
    private var successfulExecutions = 0
    private var failedExecutions = 0

    init {
        // Register built-in handlers
        registerBuiltInHandlers()
    }

    private fun registerBuiltInHandlers() {
        // System commands
        actionHandlers[BuiltInIntents.SYSTEM_STOP] = { _ ->
            IntentResult.Success("Stopping current operation")
        }

        actionHandlers[BuiltInIntents.SYSTEM_BACK] = { _ ->
            IntentResult.Success("Going back")
        }

        actionHandlers[BuiltInIntents.SYSTEM_CANCEL] = { _ ->
            IntentResult.Success("Cancelled")
        }

        actionHandlers[BuiltInIntents.SYSTEM_HOME] = { _ ->
            IntentResult.Success("Returning to home")
        }

        actionHandlers[BuiltInIntents.SYSTEM_HELP] = { _ ->
            IntentResult.Success("Showing help information")
        }

        actionHandlers[BuiltInIntents.SYSTEM_QUIT] = { _ ->
            IntentResult.Success("Quitting application")
        }

        actionHandlers[BuiltInIntents.SYSTEM_EXIT] = { _ ->
            IntentResult.Success("Exiting current mode")
        }

        actionHandlers[BuiltInIntents.SYSTEM_PAUSE] = { _ ->
            IntentResult.Success("Paused")
        }

        actionHandlers[BuiltInIntents.SYSTEM_RESUME] = { _ ->
            IntentResult.Success("Resumed")
        }

        actionHandlers[BuiltInIntents.SYSTEM_MUTE] = { _ ->
            IntentResult.Success("Muted")
        }

        actionHandlers[BuiltInIntents.SYSTEM_UNMUTE] = { _ ->
            IntentResult.Success("Unmuted")
        }

        // Meta commands
        actionHandlers[BuiltInIntents.SHOW_HISTORY] = { _ ->
            IntentResult.Success("Showing conversation history")
        }

        actionHandlers[BuiltInIntents.NEW_CONVERSATION] = { _ ->
            IntentResult.Success("Starting new conversation")
        }

        actionHandlers[BuiltInIntents.TEACH_AVA] = { _ ->
            IntentResult.Success("Entering teach mode")
        }

        // Information queries (stubs - would integrate with actual services)
        actionHandlers[BuiltInIntents.CHECK_WEATHER] = { utterance ->
            IntentResult.Success("Weather check requested: $utterance")
        }

        actionHandlers[BuiltInIntents.SHOW_TIME] = { _ ->
            val time = java.time.LocalTime.now()
            IntentResult.Success("The current time is ${time.hour}:${time.minute.toString().padStart(2, '0')}")
        }

        // Productivity (stubs)
        actionHandlers[BuiltInIntents.SET_ALARM] = { utterance ->
            IntentResult.Success("Alarm setting requested: $utterance")
        }

        actionHandlers[BuiltInIntents.SET_REMINDER] = { utterance ->
            IntentResult.Success("Reminder setting requested: $utterance")
        }

        // Device control (stubs - would integrate with smart home APIs)
        actionHandlers[BuiltInIntents.CONTROL_LIGHTS] = { utterance ->
            IntentResult.Success("Light control requested: $utterance")
        }

        actionHandlers[BuiltInIntents.CONTROL_TEMPERATURE] = { utterance ->
            IntentResult.Success("Temperature control requested: $utterance")
        }

        println("[ActionCoordinatorDesktop] Registered ${actionHandlers.size} built-in handlers")
    }

    // ==================== IActionCoordinator Implementation ====================

    override fun isInitialized(): Boolean = initialized

    override fun initialize() {
        if (!initialized) {
            initialized = true
            println("[ActionCoordinatorDesktop] Initialized with ${actionHandlers.size} handlers")
        }
    }

    override fun hasHandler(intent: String): Boolean {
        return actionHandlers.containsKey(intent)
    }

    override suspend fun getCategoryForIntent(intent: String): String {
        return BuiltInIntents.getCategory(intent)
    }

    override suspend fun executeActionWithRouting(
        intent: String,
        category: String,
        utterance: String
    ): IActionCoordinator.ActionExecutionResult {
        totalExecutions++

        // Check if we have a handler
        val handler = actionHandlers[intent]
        if (handler == null) {
            failedExecutions++
            return IActionCoordinator.ActionExecutionResult.NoHandler(intent)
        }

        return try {
            val result = handler(utterance)
            when (result) {
                is IntentResult.Success -> {
                    successfulExecutions++
                    IActionCoordinator.ActionExecutionResult.Success(
                        message = result.message,
                        needsAccessibility = false
                    )
                }
                is IntentResult.Failed -> {
                    failedExecutions++
                    IActionCoordinator.ActionExecutionResult.Failure(
                        message = result.reason
                    )
                }
                is IntentResult.NeedsMoreInfo -> {
                    IActionCoordinator.ActionExecutionResult.NeedsResolution(
                        capability = result.missingEntity.name,
                        message = result.prompt
                    )
                }
            }
        } catch (e: Exception) {
            failedExecutions++
            IActionCoordinator.ActionExecutionResult.Failure(
                message = "Action execution error: ${e.message}"
            )
        }
    }

    override suspend fun executeAction(intent: String, utterance: String): IntentResult {
        val handler = actionHandlers[intent]
            ?: return IntentResult.Failed("No handler for intent: $intent")

        return try {
            handler(utterance)
        } catch (e: Exception) {
            IntentResult.Failed("Execution error: ${e.message}", e)
        }
    }

    override fun dismissAccessibilityPrompt() {
        _showAccessibilityPrompt.value = false
    }

    override fun isAccessibilityServiceEnabled(): Boolean {
        // Desktop doesn't have Android-style accessibility services
        // Return true as there's no such restriction on desktop
        return true
    }

    override fun getRoutingStats(): Map<String, Any> {
        return mapOf(
            "totalExecutions" to totalExecutions,
            "successfulExecutions" to successfulExecutions,
            "failedExecutions" to failedExecutions,
            "registeredHandlers" to actionHandlers.size,
            "successRate" to if (totalExecutions > 0) {
                (successfulExecutions.toFloat() / totalExecutions * 100).toInt()
            } else 0
        )
    }

    override fun getRegisteredIntents(): List<String> {
        return actionHandlers.keys.toList()
    }

    // ==================== Desktop-Specific Methods ====================

    /**
     * Register a custom action handler.
     *
     * @param intent Intent identifier
     * @param handler Suspend function to handle the action
     */
    fun registerHandler(intent: String, handler: suspend (String) -> IntentResult) {
        actionHandlers[intent] = handler
        println("[ActionCoordinatorDesktop] Registered handler for: $intent")
    }

    /**
     * Unregister an action handler.
     *
     * @param intent Intent identifier
     */
    fun unregisterHandler(intent: String) {
        actionHandlers.remove(intent)
        println("[ActionCoordinatorDesktop] Unregistered handler for: $intent")
    }

    /**
     * Execute a system command (desktop-specific).
     *
     * @param command Command to execute
     * @return IntentResult with command output
     */
    suspend fun executeSystemCommand(command: List<String>): IntentResult {
        return try {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                IntentResult.Success(output.ifEmpty { "Command executed successfully" })
            } else {
                IntentResult.Failed("Command failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            IntentResult.Failed("Command execution error: ${e.message}", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ActionCoordinatorDesktop? = null

        /**
         * Get singleton instance of ActionCoordinatorDesktop.
         *
         * @return Singleton instance
         */
        fun getInstance(): ActionCoordinatorDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActionCoordinatorDesktop().also {
                    INSTANCE = it
                }
            }
        }
    }
}
