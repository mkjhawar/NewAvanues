/**
 * HandlerRegistry.kt - Registry for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Purpose: Single Responsibility - Manage handler registration and lookup
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for managing action handlers
 * Follows Single Responsibility Principle - only handles handler registration and lookup
 */
class HandlerRegistry {

    companion object {
        private const val TAG = "HandlerRegistry"
    }

    private val handlers = ConcurrentHashMap<ActionCategory, MutableList<ActionHandler>>()

    /**
     * Register a handler for a specific category
     * Allows multiple handlers per category for delegation pattern
     *
     * @param category The category to register the handler for
     * @param handler The handler to register
     */
    fun registerHandler(category: ActionCategory, handler: ActionHandler) {
        handlers.computeIfAbsent(category) { mutableListOf() }.add(handler)
        Log.d(TAG, "Registered ${handler.javaClass.simpleName} for category: $category")
    }

    /**
     * Unregister a specific handler
     *
     * @param handler The handler to unregister
     * @return True if handler was found and removed
     */
    fun unregisterHandler(handler: ActionHandler): Boolean {
        var removed = false
        handlers.values.forEach { handlerList ->
            if (handlerList.remove(handler)) {
                removed = true
            }
        }
        return removed
    }

    /**
     * Unregister all handlers for a category
     *
     * @param category The category to clear handlers for
     * @return Number of handlers removed
     */
    fun unregisterCategory(category: ActionCategory): Int {
        val handlerList = handlers.remove(category)
        return handlerList?.size ?: 0
    }

    /**
     * Find the first handler that can handle the action
     * Uses priority order for category lookup
     *
     * @param action The action to find a handler for
     * @return Handler that can handle the action, or null
     */
    fun findHandler(action: String): ActionHandler? {
        // Priority order for category lookup
        val priorityOrder = listOf(
            ActionCategory.SYSTEM,      // System commands have highest priority
            ActionCategory.NAVIGATION,  // Navigation next
            ActionCategory.APP,         // App launching
            ActionCategory.GAZE,        // Gaze interactions have high priority
            ActionCategory.GESTURE,     // Gesture interactions
            ActionCategory.UI,          // UI interaction
            ActionCategory.DEVICE,      // Device control
            ActionCategory.INPUT,       // Text input
            ActionCategory.CUSTOM       // Custom last
        )

        // Check handlers by priority order
        for (category in priorityOrder) {
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

    /**
     * Get all handlers for a specific category
     *
     * @param category The category to get handlers for
     * @return List of handlers for the category (empty if none registered)
     */
    fun getHandlersForCategory(category: ActionCategory): List<ActionHandler> {
        return handlers[category]?.toList() ?: emptyList()
    }

    /**
     * Get all registered handlers across all categories
     *
     * @return List of all registered handlers
     */
    fun getAllHandlers(): List<ActionHandler> {
        return handlers.values.flatten()
    }

    /**
     * Check if any handler can handle the action
     *
     * @param action The action to check
     * @return True if at least one handler can handle the action
     */
    fun canHandle(action: String): Boolean {
        return handlers.values.flatten().any { it.canHandle(action) }
    }

    /**
     * Get all supported actions across all handlers
     *
     * @return List of all supported actions with category prefix
     */
    fun getAllSupportedActions(): List<String> {
        return handlers.flatMap { (category, handlerList) ->
            handlerList.flatMap { handler ->
                handler.getSupportedActions().map { action ->
                    "${category.name.lowercase()}: $action"
                }
            }
        }
    }

    /**
     * Get supported actions for a specific category
     *
     * @param category The category to get actions for
     * @return List of supported actions for the category
     */
    fun getSupportedActions(category: ActionCategory): List<String> {
        return handlers[category]?.flatMap { it.getSupportedActions() } ?: emptyList()
    }

    /**
     * Get the category for a handler
     *
     * @param handler The handler to find the category for
     * @return Category of the handler, or CUSTOM if not found
     */
    fun getCategoryForHandler(handler: ActionHandler): ActionCategory {
        return handlers.entries.find { it.value.contains(handler) }?.key
            ?: ActionCategory.CUSTOM
    }

    /**
     * Get total number of registered handlers
     */
    fun getHandlerCount(): Int {
        return handlers.values.sumOf { it.size }
    }

    /**
     * Get number of categories with registered handlers
     */
    fun getCategoryCount(): Int {
        return handlers.size
    }

    /**
     * Clear all registered handlers
     */
    fun clear() {
        handlers.clear()
        Log.d(TAG, "All handlers cleared")
    }

    /**
     * Initialize all registered handlers
     *
     * @return Number of handlers successfully initialized
     */
    fun initializeAll(): Int {
        var successCount = 0
        handlers.values.flatten().forEach { handler ->
            try {
                handler.initialize()
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ${handler.javaClass.simpleName}", e)
            }
        }
        return successCount
    }

    /**
     * Dispose all registered handlers
     *
     * @return Number of handlers successfully disposed
     */
    fun disposeAll(): Int {
        var successCount = 0
        handlers.values.flatten().forEach { handler ->
            try {
                handler.dispose()
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing ${handler.javaClass.simpleName}", e)
            }
        }
        return successCount
    }

    /**
     * Get debug information about registered handlers
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("HandlerRegistry Debug Info")
            appendLine("Total handlers: ${getHandlerCount()}")
            appendLine("Categories: ${getCategoryCount()}")
            handlers.forEach { (category, handlerList) ->
                appendLine("  $category (${handlerList.size} handlers):")
                handlerList.forEach { handler ->
                    appendLine("    - ${handler.javaClass.simpleName}")
                }
            }
        }
    }
}
