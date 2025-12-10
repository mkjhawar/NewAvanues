package com.augmentalis.ava.features.chat.coordinator

import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.ActionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Action Coordinator - Single Responsibility: Action execution flow
 *
 * Extracted from ChatViewModel as part of SOLID refactoring (P0).
 * Handles all action-related operations:
 * - Action handler checking and execution
 * - Routing decisions (local vs VoiceOS)
 * - Accessibility permission state
 *
 * @param actionsManager Actions system wrapper with intent routing
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class ActionCoordinator @Inject constructor(
    private val actionsManager: ActionsManager
) {
    companion object {
        private const val TAG = "ActionCoordinator"
    }

    // ==================== State ====================

    private val _showAccessibilityPrompt = MutableStateFlow(false)
    val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

    // ==================== Action Execution ====================

    /**
     * Result of action execution with routing.
     */
    sealed class ActionExecutionResult {
        /**
         * Action executed successfully.
         */
        data class Success(
            val message: String,
            val needsAccessibility: Boolean = false
        ) : ActionExecutionResult()

        /**
         * Action execution failed.
         */
        data class Failure(
            val message: String
        ) : ActionExecutionResult()

        /**
         * Action needs app resolution (Chapter 71).
         */
        data class NeedsResolution(
            val capability: String,
            val message: String
        ) : ActionExecutionResult()

        /**
         * No handler found for intent.
         */
        data class NoHandler(
            val intent: String
        ) : ActionExecutionResult()
    }

    /**
     * Check if actions have been initialized.
     */
    fun isInitialized(): Boolean = actionsManager.isInitialized()

    /**
     * Initialize action handlers.
     */
    fun initialize() = actionsManager.initialize()

    /**
     * Check if a handler exists for the given intent.
     */
    fun hasHandler(intent: String): Boolean = actionsManager.hasHandler(intent)

    /**
     * Get category for an intent.
     * Phase 2: Database-driven lookup with fallback.
     */
    suspend fun getCategoryForIntent(intent: String): String = actionsManager.getCategoryForIntent(intent)

    /**
     * Execute an action with intelligent routing.
     *
     * Routes the intent to appropriate execution backend:
     * - AVA-capable commands → Execute locally
     * - VoiceOS-only commands → Forward via IPC or show accessibility prompt
     *
     * @param intent Intent identifier
     * @param category Intent category
     * @param utterance Original user utterance
     * @return ActionExecutionResult with outcome details
     */
    suspend fun executeActionWithRouting(
        intent: String,
        category: String,
        utterance: String
    ): ActionExecutionResult {
        Log.d(TAG, "Executing action with routing: $intent (category: $category)")

        if (!hasHandler(intent)) {
            Log.d(TAG, "No handler found for intent: $intent")
            return ActionExecutionResult.NoHandler(intent)
        }

        val startTime = System.currentTimeMillis()
        val actionResult = actionsManager.executeActionWithRouting(
            intent = intent,
            category = category,
            utterance = utterance
        )
        val actionTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Action executed in ${actionTime}ms")

        return when (actionResult) {
            is ActionResult.Success -> {
                // Check if accessibility permission is needed
                val needsAccessibility = actionResult.data?.get("needsAccessibility") == true
                if (needsAccessibility) {
                    Log.d(TAG, "Accessibility service needed, showing prompt")
                    _showAccessibilityPrompt.value = true
                }

                ActionExecutionResult.Success(
                    message = actionResult.message ?: "Action completed successfully",
                    needsAccessibility = needsAccessibility
                )
            }
            is ActionResult.Failure -> {
                Log.w(TAG, "Action failed: ${actionResult.message}", actionResult.exception)
                ActionExecutionResult.Failure(actionResult.message)
            }
            is ActionResult.NeedsResolution -> {
                Log.d(TAG, "Action needs app resolution for capability: ${actionResult.capability}")
                ActionExecutionResult.NeedsResolution(
                    capability = actionResult.capability,
                    message = "I need to know which app you'd like to use for ${actionResult.capability}."
                )
            }
        }
    }

    /**
     * Execute action directly without routing (for built-in intents).
     */
    suspend fun executeAction(intent: String, utterance: String): ActionResult {
        return actionsManager.executeAction(intent, utterance)
    }

    // ==================== Accessibility State ====================

    /**
     * Dismiss accessibility permission prompt.
     */
    fun dismissAccessibilityPrompt() {
        _showAccessibilityPrompt.value = false
        Log.d(TAG, "Accessibility prompt dismissed")
    }

    /**
     * Check if accessibility service is enabled.
     */
    fun isAccessibilityServiceEnabled(): Boolean = actionsManager.isAccessibilityServiceEnabled()

    // ==================== Routing Stats ====================

    /**
     * Get routing statistics.
     */
    fun getRoutingStats(): Map<String, Any> = actionsManager.getRoutingStats()

    /**
     * Get list of registered intents.
     */
    fun getRegisteredIntents(): List<String> = actionsManager.getRegisteredIntents()
}
