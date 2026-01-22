/**
 * IHandler.kt - Interface for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP interface for action handlers.
 * Follows Single Responsibility Principle - each handler handles one category of actions.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Interface for action handlers.
 *
 * Handlers process voice commands and perform actions on the device.
 * Each handler typically handles one [ActionCategory] of actions.
 *
 * Usage:
 * ```kotlin
 * class NavigationHandler : IHandler {
 *     override val category = ActionCategory.NAVIGATION
 *     override val supportedActions = listOf("scroll up", "scroll down", ...)
 *
 *     override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
 *         // Handle navigation action
 *     }
 * }
 * ```
 */
interface IHandler {
    /**
     * The category of actions this handler processes.
     */
    val category: ActionCategory

    /**
     * List of action patterns this handler supports.
     * Used for command discovery and help systems.
     */
    val supportedActions: List<String>

    /**
     * Execute an action.
     *
     * @param command The quantized command to execute
     * @param params Optional parameters for the action
     * @return Result of the execution
     */
    suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any> = emptyMap()
    ): HandlerResult

    /**
     * Execute an action by string (legacy compatibility).
     *
     * @param action The action string to execute
     * @param params Optional parameters for the action
     * @return Result of the execution
     */
    suspend fun execute(
        action: String,
        params: Map<String, Any> = emptyMap()
    ): HandlerResult = execute(
        command = QuantizedCommand(
            phrase = action,
            actionType = CommandActionType.EXECUTE,
            targetAvid = null,
            confidence = 1.0f
        ),
        params = params
    )

    /**
     * Check if this handler can handle the given action.
     *
     * @param action The action string to check
     * @return true if this handler can process the action
     */
    fun canHandle(action: String): Boolean

    /**
     * Check if this handler can handle the given command.
     *
     * @param command The command to check
     * @return true if this handler can process the command
     */
    fun canHandle(command: QuantizedCommand): Boolean = canHandle(command.phrase)

    /**
     * Initialize the handler.
     * Called once during service startup.
     */
    suspend fun initialize() {
        // Default empty implementation
        // Handlers override if needed
    }

    /**
     * Dispose resources.
     * Called when service is destroyed.
     */
    suspend fun dispose() {
        // Default empty implementation
        // Handlers override if needed
    }
}

/**
 * Base implementation of [IHandler] with common functionality.
 *
 * Provides default canHandle implementation based on supportedActions.
 * Supports both exact matching and prefix matching for voice commands.
 */
abstract class BaseHandler : IHandler {

    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return supportedActions.any { supported ->
            val supportedLower = supported.lowercase()

            // Exact match
            normalized == supportedLower ||

            // Prefix match with space (e.g., "click submit" starts with "click ")
            normalized.startsWith(supportedLower + " ") ||

            // Handle slight voice variations - action verb match
            // e.g., "clicking" matches "click", "scrolling" matches "scroll"
            (supportedLower.length >= 4 &&
             normalized.startsWith(supportedLower.dropLast(1)) &&
             normalized.length > supportedLower.length)
        }
    }
}
