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
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.LoggingUtils
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
class HandlerRegistry : IHandlerRegistry {

    private val handlers = mutableMapOf<ActionCategory, MutableList<IHandler>>()
    private val mutex = Mutex()

    /**
     * Register a handler.
     * Handlers are added to their category's list, allowing multiple handlers per category.
     *
     * @param handler The handler to register
     */
    override suspend fun register(handler: IHandler) {
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
    override suspend fun register(category: ActionCategory, handler: IHandler) {
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
    override suspend fun unregister(handler: IHandler): Boolean {
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
    override suspend fun unregisterCategory(category: ActionCategory): Int {
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
    override suspend fun findHandler(action: String): IHandler? {
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
     * Uses canHandle(QuantizedCommand) which allows handlers to inspect metadata
     * (e.g., WebCommandHandler checks metadata["source"] == "web").
     * This is preferred over findHandler(String) for dynamic commands.
     *
     * @param command The command to find a handler for
     * @return Handler that can handle the command, or null
     */
    override suspend fun findHandler(command: QuantizedCommand): IHandler? {
        mutex.withLock {
            // Check handlers by priority order using QuantizedCommand overload
            // This allows metadata-aware handlers (e.g., WebCommandHandler) to match
            for (category in ActionCategory.PRIORITY_ORDER) {
                handlers[category]?.let { handlerList ->
                    for (handler in handlerList) {
                        if (handler.canHandle(command)) {
                            return handler
                        }
                    }
                }
            }

            // If no prioritized handler found, check all remaining categories
            return handlers.values.flatten().find { it.canHandle(command) }
        }
    }

    /**
     * Get all handlers for a specific category.
     *
     * @param category The category to get handlers for
     * @return List of handlers for the category (empty if none registered)
     */
    override suspend fun getHandlersForCategory(category: ActionCategory): List<IHandler> {
        mutex.withLock {
            return handlers[category]?.toList() ?: emptyList()
        }
    }

    /**
     * Get all registered handlers across all categories.
     *
     * Returns a defensive copy to prevent ConcurrentModificationException
     * if callers iterate while handlers are modified.
     *
     * @return List of all registered handlers (defensive copy)
     */
    override suspend fun getAllHandlers(): List<IHandler> {
        mutex.withLock {
            return handlers.values.flatten().toList()
        }
    }

    /**
     * Check if any handler can handle the action.
     *
     * @param action The action to check
     * @return true if at least one handler can handle the action
     */
    override suspend fun canHandle(action: String): Boolean {
        mutex.withLock {
            return handlers.values.flatten().any { it.canHandle(action) }
        }
    }

    /**
     * Get all supported actions across all handlers.
     *
     * @return List of all supported actions with category prefix
     */
    override suspend fun getAllSupportedActions(): List<String> {
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
    override suspend fun getSupportedActions(category: ActionCategory): List<String> {
        mutex.withLock {
            return handlers[category]?.flatMap { it.supportedActions } ?: emptyList()
        }
    }

    /**
     * Get total number of registered handlers.
     */
    override suspend fun getHandlerCount(): Int {
        mutex.withLock {
            return handlers.values.sumOf { it.size }
        }
    }

    /**
     * Get number of categories with registered handlers.
     */
    override suspend fun getCategoryCount(): Int {
        mutex.withLock {
            return handlers.size
        }
    }

    /**
     * Clear all registered handlers.
     */
    override suspend fun clear() {
        mutex.withLock {
            handlers.clear()
        }
    }

    /**
     * Initialize all registered handlers.
     *
     * Tracks critical handler failures and logs them with higher severity.
     * Critical categories: SYSTEM, NAVIGATION, UI
     *
     * @return Number of handlers successfully initialized
     */
    override suspend fun initializeAll(): Int {
        val allHandlers = mutex.withLock { handlers.values.flatten().toList() }
        var successCount = 0
        val failedCritical = mutableListOf<String>()

        allHandlers.forEach { handler ->
            try {
                handler.initialize()
                successCount++
            } catch (e: Exception) {
                val handlerName = handler::class.simpleName ?: "Unknown"
                val isCritical = handler.category in listOf(
                    ActionCategory.SYSTEM,
                    ActionCategory.NAVIGATION,
                    ActionCategory.UI
                )

                if (isCritical) {
                    failedCritical.add(handlerName)
                    LoggingUtils.e("CRITICAL: Failed to initialize $handlerName: ${e.message}", "HandlerRegistry", e)
                } else {
                    LoggingUtils.w("Failed to initialize $handlerName: ${e.message}", "HandlerRegistry", e)
                }
            }
        }

        if (failedCritical.isNotEmpty()) {
            LoggingUtils.e("WARNING: ${failedCritical.size} critical handlers failed to initialize: $failedCritical", "HandlerRegistry")
        }

        return successCount
    }

    /**
     * Dispose all registered handlers.
     *
     * @return Number of handlers successfully disposed
     */
    override suspend fun disposeAll(): Int {
        val allHandlers = mutex.withLock { handlers.values.flatten().toList() }
        var successCount = 0

        allHandlers.forEach { handler ->
            try {
                handler.dispose()
                successCount++
            } catch (e: Exception) {
                // Log error but continue disposing other handlers
                LoggingUtils.w("Error disposing ${handler::class.simpleName}: ${e.message}", "HandlerRegistry", e)
            }
        }

        return successCount
    }

    /**
     * Get debug information about registered handlers.
     */
    override suspend fun getDebugInfo(): String {
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
