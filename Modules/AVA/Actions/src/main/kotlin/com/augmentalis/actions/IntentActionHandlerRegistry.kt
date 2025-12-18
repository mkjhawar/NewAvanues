package com.augmentalis.actions

import android.content.Context
import android.util.Log

/**
 * Registry for managing intent action handlers.
 *
 * Central repository for all action handlers. Provides lookup by intent name
 * and delegates execution to the appropriate handler.
 *
 * Design:
 * - Singleton pattern for easy access
 * - Thread-safe registration (synchronized map)
 * - Graceful handling of missing handlers
 * - Extensible: new handlers can be registered at runtime
 *
 * Usage:
 * ```
 * // Register handlers (typically in Application.onCreate)
 * IntentActionHandlerRegistry.register(TimeActionHandler())
 * IntentActionHandlerRegistry.register(AlarmActionHandler())
 *
 * // Execute action
 * val result = IntentActionHandlerRegistry.executeAction(
 *     context = context,
 *     intent = "show_time",
 *     utterance = "What time is it?"
 * )
 * ```
 *
 * @see IntentActionHandler
 * @see ActionResult
 */
object IntentActionHandlerRegistry {

    private const val TAG = "IntentActionHandlerRegistry"

    /**
     * Map of intent name to handler instance.
     * Thread-safe for concurrent access.
     */
    private val handlers = mutableMapOf<String, IntentActionHandler>()

    /**
     * Register an action handler.
     *
     * @param handler Handler to register
     */
    fun register(handler: IntentActionHandler) {
        synchronized(handlers) {
            handlers[handler.intent] = handler
            Log.d(TAG, "Registered handler for intent: ${handler.intent}")
        }
    }

    /**
     * Register multiple action handlers with error handling.
     *
     * Individual handler failures are logged but don't crash the app.
     * This prevents one faulty handler from breaking all action handling.
     *
     * @param handlers Handlers to register
     */
    fun registerAll(vararg handlers: IntentActionHandler) {
        var successCount = 0
        var failCount = 0
        handlers.forEach { handler ->
            try {
                register(handler)
                successCount++
            } catch (e: Exception) {
                failCount++
                Log.e(TAG, "Failed to register handler for intent: ${handler.intent}", e)
            }
        }
        Log.i(TAG, "Handler registration complete: $successCount succeeded" +
                if (failCount > 0) ", $failCount failed" else "")
    }

    /**
     * Check if a handler exists for the given intent.
     *
     * @param intent Intent name to check
     * @return True if handler is registered
     */
    fun hasHandler(intent: String): Boolean {
        synchronized(handlers) {
            return handlers.containsKey(intent)
        }
    }

    /**
     * Get handler for the given intent.
     *
     * @param intent Intent name
     * @return Handler instance, or null if not found
     */
    fun getHandler(intent: String): IntentActionHandler? {
        synchronized(handlers) {
            return handlers[intent]
        }
    }

    /**
     * Execute action for the given intent.
     *
     * Looks up the handler and executes it, returning the result.
     * If no handler exists, returns ActionResult.Failure.
     *
     * @param context Android context
     * @param intent Intent name
     * @param utterance Original user utterance
     * @return ActionResult indicating success or failure
     */
    suspend fun executeAction(
        context: Context,
        intent: String,
        utterance: String
    ): ActionResult {
        Log.d(TAG, "Executing action for intent: $intent")

        val handler = getHandler(intent)
        if (handler == null) {
            Log.w(TAG, "No handler found for intent: $intent")
            return ActionResult.Failure("No action handler available for: $intent")
        }

        return try {
            val startTime = System.currentTimeMillis()
            val result = handler.execute(context, utterance)
            val executionTime = System.currentTimeMillis() - startTime

            Log.d(TAG, "Action executed in ${executionTime}ms: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Action execution failed for intent: $intent", e)
            ActionResult.Failure(
                message = "Failed to execute action: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Get all registered intent names.
     *
     * @return List of intent names with handlers
     */
    fun getRegisteredIntents(): List<String> {
        synchronized(handlers) {
            return handlers.keys.toList()
        }
    }

    /**
     * Clear all registered handlers (for testing).
     */
    fun clear() {
        synchronized(handlers) {
            handlers.clear()
            Log.d(TAG, "All handlers cleared")
        }
    }
}
