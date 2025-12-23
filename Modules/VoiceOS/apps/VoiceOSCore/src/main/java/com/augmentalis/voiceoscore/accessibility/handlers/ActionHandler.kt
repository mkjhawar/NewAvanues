/**
 * ActionHandler.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Interface for action handlers
 */
package com.augmentalis.voiceoscore.accessibility.handlers

/**
 * Interface for handlers that execute actions
 */
interface ActionHandler {
    /**
     * Execute an action
     *
     * @param category The category of the action
     * @param action The action to execute
     * @param params Optional parameters for the action
     * @return True if the action was executed successfully
     */
    fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): Boolean

    /**
     * Check if this handler can handle the given action
     *
     * @param category The category of the action
     * @param action The action to check
     * @return True if this handler can handle the action
     */
    fun canHandle(category: ActionCategory, action: String): Boolean = true

    /**
     * Get supported actions for this handler
     *
     * @return List of supported action strings
     */
    fun getSupportedActions(): List<String> = emptyList()

    /**
     * Check if this handler can handle the given action (String overload)
     *
     * @param action The action to check
     * @return True if this handler can handle the action
     */
    fun canHandle(action: String): Boolean = true

    /**
     * Initialize the handler
     * Called when the handler is registered
     */
    fun initialize() {}

    /**
     * Dispose the handler
     * Called when the handler is unregistered
     */
    fun dispose() {}
}
