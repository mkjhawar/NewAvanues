/**
 * HandlerRegistry.kt - Registry for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP registry for managing handler registration and priority-based lookup.
 * Follows Single Responsibility Principle - only handles handler registration and lookup.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registry for managing action handlers.
 *
 * Provides priority-based handler lookup and lifecycle management.
 *
 * Usage:
 * ```kotlin
 * val registry = HandlerRegistry()
 * registry.register(NavigationHandler())
 * registry.register(UIHandler())
 *
 * // Find handler for action
 * val handler = registry.findHandler("scroll up")
 * handler?.execute(command, params)
 * ```
 */
class HandlerRegistry {

    private val handlers = mutableMapOf<ActionCategory, MutableList<IHandler>>()
    private val mutex = Mutex()

    /**
     * Register a handler.
     * Handlers are added to their category's list, allowing multiple handlers per category.
     *
     * @param handler The handler to register
     */
    suspend fun register(handler: IHandler) {
        mutex.withLock {
            handlers.getOrPut(handler.category) { mutableListOf() }.add(handler)
        }
    }

    /**
     * Register a handler for a specific category.
     * Allows registering a handler to a different category than its default.
     *
     * @param category The category to register the handler for
     * @param handler The handler to register
     */
    suspend fun register(category: ActionCategory, handler: IHandler) {
        mutex.withLock {
            handlers.getOrPut(category) { mutableListOf() }.add(handler)
        }
    }

    /**
     * Unregister a specific handler.
     *
     * @param handler The handler to unregister
     * @return true if handler was found and removed
     */
    suspend fun unregister(handler: IHandler): Boolean {
        mutex.withLock {
            var removed = false
            handlers.values.forEach { handlerList ->
                if (handlerList.remove(handler)) {
                    removed = true
                }
            }
            return removed
        }
    }

    /**
     * Unregister all handlers for a category.
     *
     * @param category The category to clear handlers for
     * @return Number of handlers removed
     */
    suspend fun unregisterCategory(category: ActionCategory): Int {
        mutex.withLock {
            val handlerList = handlers.remove(category)
            return handlerList?.size ?: 0
        }
    }

    /**
     * Find the first handler that can handle the action.
     * Uses priority order for category lookup.
     *
     * @param action The action to find a handler for
     * @return Handler that can handle the action, or null
     */
    suspend fun findHandler(action: String): IHandler? {
        mutex.withLock {
            // Check handlers by priority order
            for (category in ActionCategory.PRIORITY_ORDER) {
                handlers[category]?.let { handlerList ->
                    for (handler in handlerList) {
                        if (handler.canHandle(action)) {
                            return handler
                        }
                    }
                }
            }

            // If no prioritized handler found, check all remaining categories
            return handlers.values.flatten().find { it.canHandle(action) }
        }
    }

    /**
     * Find the first handler that can handle the command.
     *
     * @param command The command to find a handler for
     * @return Handler that can handle the command, or null
     */
    suspend fun findHandler(command: QuantizedCommand): IHandler? {
        return findHandler(command.phrase)
    }

    /**
     * Get all handlers for a specific category.
     *
     * @param category The category to get handlers for
     * @return List of handlers for the category (empty if none registered)
     */
    suspend fun getHandlersForCategory(category: ActionCategory): List<IHandler> {
        mutex.withLock {
            return handlers[category]?.toList() ?: emptyList()
        }
    }

    /**
     * Get all registered handlers across all categories.
     *
     * @return List of all registered handlers
     */
    suspend fun getAllHandlers(): List<IHandler> {
        mutex.withLock {
            return handlers.values.flatten()
        }
    }

    /**
     * Check if any handler can handle the action.
     *
     * @param action The action to check
     * @return true if at least one handler can handle the action
     */
    suspend fun canHandle(action: String): Boolean {
        mutex.withLock {
            return handlers.values.flatten().any { it.canHandle(action) }
        }
    }

    /**
     * Get all supported actions across all handlers.
     *
     * @return List of all supported actions with category prefix
     */
    suspend fun getAllSupportedActions(): List<String> {
        mutex.withLock {
            return handlers.flatMap { (category, handlerList) ->
                handlerList.flatMap { handler ->
                    handler.supportedActions.map { action ->
                        "${category.name.lowercase()}: $action"
                    }
                }
            }
        }
    }

    /**
     * Get supported actions for a specific category.
     *
     * @param category The category to get actions for
     * @return List of supported actions for the category
     */
    suspend fun getSupportedActions(category: ActionCategory): List<String> {
        mutex.withLock {
            return handlers[category]?.flatMap { it.supportedActions } ?: emptyList()
        }
    }

    /**
     * Get total number of registered handlers.
     */
    suspend fun getHandlerCount(): Int {
        mutex.withLock {
            return handlers.values.sumOf { it.size }
        }
    }

    /**
     * Get number of categories with registered handlers.
     */
    suspend fun getCategoryCount(): Int {
        mutex.withLock {
            return handlers.size
        }
    }

    /**
     * Clear all registered handlers.
     */
    suspend fun clear() {
        mutex.withLock {
            handlers.clear()
        }
    }

    /**
     * Initialize all registered handlers.
     *
     * @return Number of handlers successfully initialized
     */
    suspend fun initializeAll(): Int {
        val allHandlers = mutex.withLock { handlers.values.flatten().toList() }
        var successCount = 0

        allHandlers.forEach { handler ->
            try {
                handler.initialize()
                successCount++
            } catch (e: Exception) {
                // Log error but continue initializing other handlers
                println("Failed to initialize ${handler::class.simpleName}: ${e.message}")
            }
        }

        return successCount
    }

    /**
     * Dispose all registered handlers.
     *
     * @return Number of handlers successfully disposed
     */
    suspend fun disposeAll(): Int {
        val allHandlers = mutex.withLock { handlers.values.flatten().toList() }
        var successCount = 0

        allHandlers.forEach { handler ->
            try {
                handler.dispose()
                successCount++
            } catch (e: Exception) {
                // Log error but continue disposing other handlers
                println("Error disposing ${handler::class.simpleName}: ${e.message}")
            }
        }

        return successCount
    }

    /**
     * Get debug information about registered handlers.
     */
    suspend fun getDebugInfo(): String {
        mutex.withLock {
            return buildString {
                appendLine("HandlerRegistry Debug Info")
                appendLine("Total handlers: ${handlers.values.sumOf { it.size }}")
                appendLine("Categories: ${handlers.size}")
                handlers.forEach { (category, handlerList) ->
                    appendLine("  $category (${handlerList.size} handlers):")
                    handlerList.forEach { handler ->
                        appendLine("    - ${handler::class.simpleName}")
                    }
                }
            }
        }
    }
}
