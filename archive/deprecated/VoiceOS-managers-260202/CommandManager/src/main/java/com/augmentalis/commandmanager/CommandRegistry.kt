/**
 * CommandRegistry.kt
 *
 * Created: 2025-10-10 19:08 PDT
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Centralized command routing registry for VOS4 system-wide command distribution
 * Features: Thread-safe handler registration, command routing, handler discovery
 * Location: CommandManager module
 *
 * Changelog:
 * - v1.0.0 (2025-10-10): Initial implementation with thread-safe operations
 */

package com.augmentalis.commandmanager

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for system-wide command routing in VOS4.
 *
 * This singleton manages all CommandHandler implementations across modules,
 * providing thread-safe registration and routing of voice commands.
 *
 * Architecture:
 * - Singleton pattern: Single global registry
 * - Thread-safe: ConcurrentHashMap for concurrent access
 * - First-match routing: First handler that canHandle() wins
 * - Fail-safe: Returns false if no handler found (allows fallback)
 *
 * Usage Pattern:
 * ```kotlin
 * // Registration (typically in module initialization)
 * val handler = CursorCommandHandler()
 * CommandRegistry.registerHandler(handler.moduleId, handler)
 *
 * // Routing (from VoiceOSService or CommandManager)
 * val success = CommandRegistry.routeCommand("cursor up")
 *
 * // Cleanup (module shutdown)
 * CommandRegistry.unregisterHandler("voicecursor")
 * ```
 *
 * Thread Safety:
 * - All public methods are thread-safe
 * - ConcurrentHashMap handles concurrent registration/routing
 * - No external synchronization needed
 *
 * Performance:
 * - Registration/Unregistration: O(1)
 * - Routing: O(n) where n = number of handlers (typically < 20)
 * - Future optimization: Command prefix indexing for O(1) lookup
 *
 * @since 1.0.0
 */
object CommandRegistry {
    private const val TAG = "CommandRegistry"

    /**
     * Thread-safe map of moduleId -> CommandHandler
     * ConcurrentHashMap ensures safe concurrent access without external locking
     */
    private val handlers = ConcurrentHashMap<String, CommandHandler>()

    /**
     * Register a command handler for a specific module.
     *
     * Thread-safe: Can be called from any thread.
     * Idempotent: Registering same moduleId overwrites previous handler (logged as warning).
     *
     * @param moduleId Unique identifier for the module (e.g., "voicecursor")
     * @param handler Implementation of CommandHandler for this module
     * @throws IllegalArgumentException if moduleId is blank
     */
    fun registerHandler(moduleId: String, handler: CommandHandler) {
        require(moduleId.isNotBlank()) { "moduleId cannot be blank" }

        val previous = handlers.put(moduleId, handler)

        if (previous != null) {
            Log.w(TAG, "Handler for '$moduleId' was replaced. Previous handler overwritten.")
        } else {
            Log.i(TAG, "Registered handler for '$moduleId' with ${handler.supportedCommands.size} commands")
        }
    }

    /**
     * Unregister a command handler for a specific module.
     *
     * Thread-safe: Can be called from any thread.
     * Idempotent: Unregistering non-existent moduleId is safe (no-op).
     *
     * @param moduleId Unique identifier for the module to unregister
     */
    fun unregisterHandler(moduleId: String) {
        val removed = handlers.remove(moduleId)

        if (removed != null) {
            Log.i(TAG, "Unregistered handler for '$moduleId'")
        } else {
            Log.d(TAG, "No handler found to unregister for '$moduleId'")
        }
    }

    /**
     * Route a voice command to the appropriate handler.
     *
     * Algorithm:
     * 1. Normalize command (lowercase, trim)
     * 2. Iterate through all handlers
     * 3. Check canHandle() for each handler
     * 4. First handler that canHandle() → call handleCommand()
     * 5. Return result
     *
     * Thread-safe: Concurrent routing calls are safe.
     * Performance: O(n) where n = number of handlers.
     *
     * Error Handling:
     * - If handler throws exception → caught, logged, returns false
     * - If no handler found → logs warning, returns false
     * - If handler returns false → logs debug, returns false
     *
     * @param command The voice command text to route
     * @return true if command was successfully handled, false otherwise
     */
    suspend fun routeCommand(command: String): Boolean {
        if (command.isBlank()) {
            Log.w(TAG, "Cannot route blank command")
            return false
        }

        val normalizedCommand = command.lowercase().trim()
        Log.d(TAG, "Routing command: '$normalizedCommand'")

        // Iterate through handlers to find match
        for ((moduleId, handler) in handlers) {
            try {
                if (handler.canHandle(normalizedCommand)) {
                    Log.d(TAG, "Handler '$moduleId' can handle: '$normalizedCommand'")

                    val result = handler.handleCommand(normalizedCommand)

                    if (result) {
                        Log.i(TAG, "Command '$normalizedCommand' handled successfully by '$moduleId'")
                        return true
                    } else {
                        Log.d(TAG, "Handler '$moduleId' returned false for: '$normalizedCommand'")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Handler '$moduleId' threw exception for '$normalizedCommand'", e)
                // Continue to next handler instead of failing completely
            }
        }

        Log.w(TAG, "No handler found for command: '$normalizedCommand'")
        return false
    }

    /**
     * Get a specific handler by moduleId.
     *
     * Thread-safe: Safe to call concurrently.
     *
     * Use Cases:
     * - Direct module access (bypass routing)
     * - Testing/debugging
     * - Module introspection
     *
     * @param moduleId The unique module identifier
     * @return The CommandHandler if registered, null otherwise
     */
    fun getHandler(moduleId: String): CommandHandler? {
        return handlers[moduleId]
    }

    /**
     * Get all registered handlers.
     *
     * Thread-safe: Returns snapshot of current handlers.
     *
     * Use Cases:
     * - UI display of available modules
     * - Help system generation
     * - System diagnostics
     *
     * @return List of all registered CommandHandler instances
     */
    fun getAllHandlers(): List<CommandHandler> {
        return handlers.values.toList()
    }

    /**
     * Get all supported commands across all modules.
     *
     * Aggregates supportedCommands from all registered handlers.
     * Useful for generating help text or command autocomplete.
     *
     * @return Flattened list of all supported command patterns
     */
    fun getAllSupportedCommands(): List<String> {
        return handlers.values.flatMap { it.supportedCommands }
    }

    /**
     * Check if any handler is registered for a moduleId.
     *
     * @param moduleId The module identifier to check
     * @return true if a handler is registered, false otherwise
     */
    fun isHandlerRegistered(moduleId: String): Boolean {
        return handlers.containsKey(moduleId)
    }

    /**
     * Get count of registered handlers.
     *
     * @return Number of currently registered handlers
     */
    fun getHandlerCount(): Int {
        return handlers.size
    }

    /**
     * Clear all registered handlers.
     *
     * WARNING: This should only be used for testing or system shutdown.
     * In production, prefer unregisterHandler() for individual cleanup.
     */
    fun clearAllHandlers() {
        val count = handlers.size
        handlers.clear()
        Log.w(TAG, "Cleared all $count handlers from registry")
    }
}
