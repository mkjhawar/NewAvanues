package com.augmentalis.chat.coordinator

import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.ActionsManager
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
) : IActionCoordinator {
    companion object {
        private const val TAG = "ActionCoordinator"
    }

    // ==================== State ====================

    private val _showAccessibilityPrompt = MutableStateFlow(false)
    override val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

    // ==================== Action Execution ====================

    /**
     * Check if actions have been initialized.
     */
    override fun isInitialized(): Boolean = actionsManager.isInitialized()

    /**
     * Initialize action handlers.
     */
    override fun initialize() = actionsManager.initialize()

    /**
     * Check if a handler exists for the given intent.
     */
    override fun hasHandler(intent: String): Boolean = actionsManager.hasHandler(intent)

    /**
     * Get category for an intent.
     * Phase 2: Database-driven lookup with fallback.
     */
    override suspend fun getCategoryForIntent(intent: String): String = actionsManager.getCategoryForIntent(intent)

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
    override suspend fun executeActionWithRouting(
        intent: String,
        category: String,
        utterance: String
    ): IActionCoordinator.ActionExecutionResult {
        Log.d(TAG, "Executing action with routing: $intent (category: $category)")

        if (!hasHandler(intent)) {
            Log.d(TAG, "No handler found for intent: $intent")
            return IActionCoordinator.ActionExecutionResult.NoHandler(intent)
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

                IActionCoordinator.ActionExecutionResult.Success(
                    message = actionResult.message ?: "Action completed successfully",
                    needsAccessibility = needsAccessibility
                )
            }
            is ActionResult.Failure -> {
                Log.w(TAG, "Action failed: ${actionResult.message}", actionResult.exception)
                IActionCoordinator.ActionExecutionResult.Failure(actionResult.message)
            }
            is ActionResult.NeedsResolution -> {
                Log.d(TAG, "Action needs app resolution for capability: ${actionResult.capability}")
                IActionCoordinator.ActionExecutionResult.NeedsResolution(
                    capability = actionResult.capability,
                    message = "I need to know which app you'd like to use for ${actionResult.capability}."
                )
            }
        }
    }

    /**
     * Execute action directly without routing (for built-in intents).
     */
    override suspend fun executeAction(intent: String, utterance: String): ActionResult {
        return actionsManager.executeAction(intent, utterance)
    }

    // ==================== Accessibility State ====================

    /**
     * Dismiss accessibility permission prompt.
     */
    override fun dismissAccessibilityPrompt() {
        _showAccessibilityPrompt.value = false
        Log.d(TAG, "Accessibility prompt dismissed")
    }

    /**
     * Check if accessibility service is enabled.
     */
    override fun isAccessibilityServiceEnabled(): Boolean = actionsManager.isAccessibilityServiceEnabled()

    // ==================== Routing Stats ====================

    /**
     * Get routing statistics.
     */
    override fun getRoutingStats(): Map<String, Any> = actionsManager.getRoutingStats()

    /**
     * Get list of registered intents.
     */
    override fun getRegisteredIntents(): List<String> = actionsManager.getRegisteredIntents()
}
