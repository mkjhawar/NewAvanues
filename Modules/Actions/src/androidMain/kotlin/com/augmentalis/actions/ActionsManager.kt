package com.augmentalis.actions

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.augmentalis.ava.core.domain.model.AppResolution
import com.augmentalis.ava.core.domain.resolution.AppResolverService
import com.augmentalis.ava.core.domain.resolution.PreferencePromptManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-injectable wrapper for Actions system with Intent Routing.
 *
 * Encapsulates Context dependency and provides a clean interface for ViewModels
 * to interact with the Actions system without requiring Context injection.
 *
 * ## Architecture Benefits:
 * - ViewModels no longer need Context injection
 * - Easier to test (mock ActionsManager instead of Context)
 * - Follows separation of concerns (ViewModels don't know about Android framework)
 * - Thread-safe initialization
 * - **NEW (Phase 3):** Intent routing to AVA or VoiceOS based on capability
 *
 * ## Intent Routing (Phase 3):
 * Commands are automatically routed based on capability:
 * - **AVA-capable** (connectivity, volume, media) → Execute locally
 * - **VoiceOS-only** (gestures, cursor, accessibility) → Forward via IPC
 *
 * ## Usage in ViewModels:
 * ```kotlin
 * @HiltViewModel
 * class ChatViewModel @Inject constructor(
 *     private val actionsManager: ActionsManager,  // ✅ No Context needed
 *     ...
 * ) : ViewModel() {
 *
 *     init {
 *         actionsManager.initialize()  // Thread-safe, idempotent
 *     }
 *
 *     fun handleAction(intent: String, category: String, utterance: String) {
 *         val result = actionsManager.executeActionWithRouting(intent, category, utterance)
 *         // Automatically routed to AVA or VoiceOS
 *     }
 * }
 * ```
 *
 * @param context Application context (injected by Hilt, safely scoped)
 *
 * @since 1.0.0-alpha01
 * @see ActionsInitializer
 * @see IntentActionHandlerRegistry
 * @see IntentRouter
 * @see VoiceOSConnection
 */
@Singleton
class ActionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appResolverService: AppResolverService,
    private val preferencePromptManager: PreferencePromptManager
) {

    companion object {
        private const val TAG = "ActionsManager"
    }

    /**
     * ADR-014 Phase 5: Ready state for init race condition fix.
     * Emits true once action handlers are registered and ready.
     * ChatViewModel should wait for this before processing messages.
     */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // Intent routing (Phase 3)
    // ADR-014: VoiceOSConnection must be initialized first so IntentRouter can use it
    private val voiceOSConnection by lazy { VoiceOSConnection.getInstance(context) }
    private val intentRouter by lazy { IntentRouter(context, voiceOSConnection) }

    /**
     * Initialize action handlers.
     *
     * This method is idempotent - safe to call multiple times.
     * Only the first call will actually register handlers.
     *
     * Thread-safe initialization is handled internally by ActionsInitializer.
     *
     * ADR-014 Phase 5: Sets isReady to true after initialization completes.
     */
    fun initialize() {
        ActionsInitializer.initialize(context)
        val count = IntentActionHandlerRegistry.getRegisteredIntents().size
        _isReady.value = true
        Log.i(TAG, "ActionsManager ready with $count handlers")
    }

    /**
     * Check if actions have been initialized.
     *
     * @return True if initialize() has been called successfully
     */
    fun isInitialized(): Boolean {
        return ActionsInitializer.isInitialized()
    }

    /**
     * Check if a handler exists for the given intent.
     *
     * @param intent Intent name to check
     * @return True if handler is registered
     */
    fun hasHandler(intent: String): Boolean {
        return IntentActionHandlerRegistry.hasHandler(intent)
    }

    /**
     * Execute action for the given intent.
     *
     * Delegates to IntentActionHandlerRegistry with the encapsulated Context.
     *
     * @param intent Intent name (e.g., "show_time", "set_alarm")
     * @param utterance Original user utterance
     * @return ActionResult indicating success or failure
     */
    suspend fun executeAction(intent: String, utterance: String): ActionResult {
        return IntentActionHandlerRegistry.executeAction(
            context = context,
            intent = intent,
            utterance = utterance
        )
    }

    /**
     * Get handler for the given intent.
     *
     * @param intent Intent name
     * @return Handler instance, or null if not found
     */
    fun getHandler(intent: String): IntentActionHandler? {
        return IntentActionHandlerRegistry.getHandler(intent)
    }

    /**
     * Get list of all registered intents.
     *
     * Useful for debugging or displaying available actions to user.
     *
     * @return List of registered intent names
     */
    fun getRegisteredIntents(): List<String> {
        return IntentActionHandlerRegistry.getRegisteredIntents()
    }

    /**
     * Execute action with intelligent routing (Phase 3)
     *
     * Routes the intent to appropriate execution backend:
     * - AVA-capable commands → Execute locally
     * - VoiceOS-only commands → Forward via IPC
     * - Unknown commands → Return graceful error
     *
     * @param intent Intent identifier (e.g., "wifi_on", "cursor_move_up")
     * @param category Intent category (e.g., "connectivity", "cursor")
     * @param utterance Original user utterance
     * @return ActionResult with execution outcome
     */
    suspend fun executeActionWithRouting(
        intent: String,
        category: String,
        utterance: String
    ): ActionResult {
        Log.d(TAG, "Executing with routing: $intent (category: $category)")

        // Route based on capability
        return when (val decision = intentRouter.route(intent, category)) {
            is IntentRouter.RoutingDecision.ExecuteLocally -> {
                Log.i(TAG, "Executing locally: $intent")
                executeAction(intent, utterance)
            }

            is IntentRouter.RoutingDecision.ForwardToVoiceOS -> {
                Log.i(TAG, "Forwarding to VoiceOS: $intent")

                // ADR-014 Phase 4: Check accessibility permission first
                if (!isAccessibilityServiceEnabled()) {
                    Log.w(TAG, "Accessibility service not enabled for: $intent")
                    // Return helpful message with metadata for UI to show prompt
                    return ActionResult.Success(
                        message = "To use gesture and cursor commands, please enable AVA Accessibility Service in Settings.",
                        data = mapOf(
                            "needsAccessibility" to true,
                            "intent" to intent,
                            "category" to category
                        )
                    )
                }

                // Execute via VoiceOS connection (integrated accessibility service)
                try {
                    when (val result = voiceOSConnection.executeCommand(intent, category)) {
                        is VoiceOSConnection.CommandResult.Success -> {
                            ActionResult.Success(result.message)
                        }
                        is VoiceOSConnection.CommandResult.Failure -> {
                            ActionResult.Failure(result.error)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Accessibility action failed: ${e.message}", e)
                    ActionResult.Failure("Action failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }

            is IntentRouter.RoutingDecision.VoiceOSUnavailable -> {
                Log.w(TAG, "VoiceOS unavailable for: $intent")
                ActionResult.Failure(
                    "This command requires VoiceOS accessibility service. " +
                    "Reason: ${decision.reason}"
                )
            }

            is IntentRouter.RoutingDecision.FallbackToLLM -> {
                Log.d(TAG, "Unknown category, attempting local execution: $intent")
                // Try local execution as fallback
                if (hasHandler(intent)) {
                    executeAction(intent, utterance)
                } else {
                    ActionResult.Failure("No handler found for intent: $intent")
                }
            }
        }
    }

    /**
     * Get category for an intent
     *
     * Phase 2: Uses IntentRouter to query database for category metadata.
     * Falls back to hardcoded mappings if database empty or intent not found.
     *
     * @param intent Intent identifier
     * @return Category string (e.g., "connectivity", "cursor", "unknown")
     */
    suspend fun getCategoryForIntent(intent: String): String {
        return intentRouter.getCategoryForIntent(intent)
    }

    /**
     * Get routing statistics
     *
     * @return Map with routing information
     */
    fun getRoutingStats(): Map<String, Any> {
        return intentRouter.getStats() + mapOf(
            "voiceos_installed" to voiceOSConnection.isVoiceOSInstalled(),
            "voiceos_connection" to voiceOSConnection.getConnectionState().toString()
        )
    }

    // ==================== App Resolution (Chapter 71) ====================

    /**
     * Resolve the best app for a capability.
     *
     * Part of Intelligent Resolution System (Chapter 71).
     *
     * @param capability The capability ID (e.g., "email", "sms")
     * @return AppResolution indicating which app to use or if user choice needed
     */
    suspend fun resolveApp(capability: String): AppResolution {
        return appResolverService.resolveApp(capability)
    }

    /**
     * Save user's app preference for a capability.
     *
     * @param capability The capability ID
     * @param packageName The chosen app's package name
     * @param appName The chosen app's display name
     * @param remember If true, save the preference permanently
     */
    suspend fun saveAppPreference(
        capability: String,
        packageName: String,
        appName: String,
        remember: Boolean = true
    ) {
        appResolverService.savePreference(capability, packageName, appName, remember)
    }

    /**
     * Get the PreferencePromptManager for UI coordination.
     */
    fun getPreferencePromptManager(): PreferencePromptManager = preferencePromptManager

    /**
     * Get the AppResolverService for direct access.
     */
    fun getAppResolverService(): AppResolverService = appResolverService

    // ==================== Testing ====================

    /**
     * Reset actions (for testing only).
     *
     * Clears all registered handlers and resets initialization state.
     * Should only be used in test cleanup.
     */
    internal fun reset() {
        ActionsInitializer.reset()
    }

    /**
     * Check if AVA accessibility service is enabled.
     *
     * ADR-014 Phase 4: Used for graceful fallback when accessibility commands
     * are requested but service is not enabled.
     *
     * @return True if accessibility service is enabled, false otherwise
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            // Check if any AVA accessibility service is enabled
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            // Check for AVA's package name in enabled services
            val packageName = context.packageName
            enabledServices.split(':').any { service ->
                service.startsWith(packageName) && service.contains("AccessibilityService")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status", e)
            false
        }
    }
}
