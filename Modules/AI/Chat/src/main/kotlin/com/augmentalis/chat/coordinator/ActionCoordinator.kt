package com.augmentalis.chat.coordinator

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IntentActionRegistry
import com.augmentalis.intentactions.IntentActionsInitializer
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Delegates to IntentActionRegistry (object) and IntentActionsInitializer (object)
 * from the IntentActions module. No Hilt-injected ActionsManager needed.
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class ActionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionCoordinator {
    companion object {
        private const val TAG = "ActionCoordinator"
    }

    private val platformContext = PlatformContext(context)

    // ==================== State ====================

    private val _showAccessibilityPrompt = MutableStateFlow(false)
    override val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

    // Execution stats
    private var totalExecutions = 0
    private var successfulExecutions = 0
    private var failedExecutions = 0

    // ==================== Action Execution ====================

    /**
     * Check if actions have been initialized.
     */
    override fun isInitialized(): Boolean = IntentActionsInitializer.isInitialized()

    /**
     * Initialize action handlers.
     */
    override fun initialize() = IntentActionsInitializer.initialize()

    /**
     * Check if a handler exists for the given intent.
     */
    override fun hasHandler(intent: String): Boolean = IntentActionRegistry.hasAction(intent)

    /**
     * Get category for an intent.
     * Uses IntentActionRegistry.findByIntent() with fallback to "UNKNOWN".
     */
    override suspend fun getCategoryForIntent(intent: String): String {
        return IntentActionRegistry.findByIntent(intent)?.category?.name ?: "UNKNOWN"
    }

    /**
     * Execute an action with intelligent routing.
     *
     * Routes the intent to appropriate execution backend:
     * - AVA-capable commands -> Execute locally via IntentActionRegistry
     * - VoiceOS-only commands -> Forward via IPC or show accessibility prompt
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
        totalExecutions++

        if (!hasHandler(intent)) {
            Log.d(TAG, "No handler found for intent: $intent")
            failedExecutions++
            return IActionCoordinator.ActionExecutionResult.NoHandler(intent)
        }

        val startTime = System.currentTimeMillis()
        val entities = ExtractedEntities(query = utterance)
        val intentResult = IntentActionRegistry.execute(intent, platformContext, entities)
        val actionTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Action executed in ${actionTime}ms")

        return when (intentResult) {
            is IntentResult.Success -> {
                successfulExecutions++
                // Check if accessibility permission is needed
                val needsAccessibility = intentResult.data?.get("needsAccessibility") == true
                if (needsAccessibility) {
                    Log.d(TAG, "Accessibility service needed, showing prompt")
                    _showAccessibilityPrompt.value = true
                }

                IActionCoordinator.ActionExecutionResult.Success(
                    message = intentResult.message,
                    needsAccessibility = needsAccessibility
                )
            }
            is IntentResult.Failed -> {
                failedExecutions++
                Log.w(TAG, "Action failed: ${intentResult.reason}", intentResult.exception)
                IActionCoordinator.ActionExecutionResult.Failure(intentResult.reason)
            }
            is IntentResult.NeedsMoreInfo -> {
                Log.d(TAG, "Action needs more info: ${intentResult.missingEntity}")
                IActionCoordinator.ActionExecutionResult.NeedsResolution(
                    capability = intentResult.missingEntity.name,
                    message = intentResult.prompt
                )
            }
        }
    }

    /**
     * Execute action directly without routing (for built-in intents).
     */
    override suspend fun executeAction(intent: String, utterance: String): IntentResult {
        val entities = ExtractedEntities(query = utterance)
        return IntentActionRegistry.execute(intent, platformContext, entities)
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
     * Queries AccessibilityManager directly instead of the removed ActionsManager.
     */
    override fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName
        }
    }

    // ==================== Routing Stats ====================

    /**
     * Get routing statistics.
     */
    override fun getRoutingStats(): Map<String, Any> {
        return mapOf(
            "totalExecutions" to totalExecutions,
            "successfulExecutions" to successfulExecutions,
            "failedExecutions" to failedExecutions,
            "registeredActions" to IntentActionRegistry.getAllIntentIds().size,
            "successRate" to if (totalExecutions > 0) {
                (successfulExecutions.toFloat() / totalExecutions * 100).toInt()
            } else 0
        )
    }

    /**
     * Get list of registered intents.
     */
    override fun getRegisteredIntents(): List<String> = IntentActionRegistry.getAllIntentIds().toList()
}
