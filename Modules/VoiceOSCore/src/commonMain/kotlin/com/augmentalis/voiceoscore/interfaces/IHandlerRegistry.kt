/**
 * IHandlerRegistry.kt - Handler registry interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Interface for handler registration and lookup.
 * Enables dependency injection and easier testing.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Interface for handler registration and lookup.
 * Enables dependency injection and testing.
 */
interface IHandlerRegistry {
    /**
     * Register a handler.
     *
     * @param handler The handler to register
     */
    suspend fun register(handler: IHandler)

    /**
     * Register a handler for a specific category.
     *
     * @param category The category to register the handler for
     * @param handler The handler to register
     */
    suspend fun register(category: ActionCategory, handler: IHandler)

    /**
     * Unregister a specific handler.
     *
     * @param handler The handler to unregister
     * @return true if handler was found and removed
     */
    suspend fun unregister(handler: IHandler): Boolean

    /**
     * Unregister all handlers for a category.
     *
     * @param category The category to clear handlers for
     * @return Number of handlers removed
     */
    suspend fun unregisterCategory(category: ActionCategory): Int

    /**
     * Find the first handler that can handle the action.
     *
     * @param action The action to find a handler for
     * @return Handler that can handle the action, or null
     */
    suspend fun findHandler(action: String): IHandler?

    /**
     * Find the first handler that can handle the command.
     *
     * @param command The command to find a handler for
     * @return Handler that can handle the command, or null
     */
    suspend fun findHandler(command: QuantizedCommand): IHandler?

    /**
     * Get all handlers for a specific category.
     *
     * @param category The category to get handlers for
     * @return List of handlers for the category
     */
    suspend fun getHandlersForCategory(category: ActionCategory): List<IHandler>

    /**
     * Get all registered handlers across all categories.
     *
     * @return List of all registered handlers
     */
    suspend fun getAllHandlers(): List<IHandler>

    /**
     * Check if any handler can handle the action.
     *
     * @param action The action to check
     * @return true if at least one handler can handle the action
     */
    suspend fun canHandle(action: String): Boolean

    /**
     * Get all supported actions across all handlers.
     *
     * @return List of all supported actions
     */
    suspend fun getAllSupportedActions(): List<String>

    /**
     * Get supported actions for a specific category.
     *
     * @param category The category to get actions for
     * @return List of supported actions for the category
     */
    suspend fun getSupportedActions(category: ActionCategory): List<String>

    /**
     * Get total number of registered handlers.
     */
    suspend fun getHandlerCount(): Int

    /**
     * Get number of categories with registered handlers.
     */
    suspend fun getCategoryCount(): Int

    /**
     * Get all registered handler categories.
     * Used by "list commands" to show available command areas.
     *
     * @return List of ActionCategory values that have registered handlers
     */
    fun getRegisteredCategories(): List<ActionCategory>

    /**
     * Clear all registered handlers.
     */
    suspend fun clear()

    /**
     * Initialize all registered handlers.
     *
     * @return Number of handlers successfully initialized
     */
    suspend fun initializeAll(): Int

    /**
     * Dispose all registered handlers.
     *
     * @return Number of handlers successfully disposed
     */
    suspend fun disposeAll(): Int

    /**
     * Get debug information about registered handlers.
     */
    suspend fun getDebugInfo(): String
}
