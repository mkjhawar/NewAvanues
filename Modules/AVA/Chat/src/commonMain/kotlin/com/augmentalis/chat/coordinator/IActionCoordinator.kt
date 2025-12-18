package com.augmentalis.chat.coordinator

import com.augmentalis.actions.ActionResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Action Coordinator Interface - Cross-platform action execution
 *
 * Abstracts action execution for cross-platform use in KMP.
 * Provides:
 * - Action handler checking and execution
 * - Routing decisions (local vs VoiceOS)
 * - Accessibility permission state
 *
 * @see ActionCoordinator for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
interface IActionCoordinator {
    // ==================== State ====================

    /**
     * Indicates whether to show the accessibility permission prompt.
     */
    val showAccessibilityPrompt: StateFlow<Boolean>

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
    fun isInitialized(): Boolean

    /**
     * Initialize action handlers.
     */
    fun initialize()

    /**
     * Check if a handler exists for the given intent.
     */
    fun hasHandler(intent: String): Boolean

    /**
     * Get category for an intent.
     * Phase 2: Database-driven lookup with fallback.
     */
    suspend fun getCategoryForIntent(intent: String): String

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
    ): ActionExecutionResult

    /**
     * Execute action directly without routing (for built-in intents).
     */
    suspend fun executeAction(intent: String, utterance: String): ActionResult

    // ==================== Accessibility State ====================

    /**
     * Dismiss accessibility permission prompt.
     */
    fun dismissAccessibilityPrompt()

    /**
     * Check if accessibility service is enabled.
     */
    fun isAccessibilityServiceEnabled(): Boolean

    // ==================== Routing Stats ====================

    /**
     * Get routing statistics.
     */
    fun getRoutingStats(): Map<String, Any>

    /**
     * Get list of registered intents.
     */
    fun getRegisteredIntents(): List<String>
}
