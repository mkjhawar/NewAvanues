// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/integration/AvaIntegrationBridge.kt
// created: 2025-11-01 23:30:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 3 - Integration Layer
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.integration

import android.content.Context
import com.augmentalis.overlay.context.ContextEngine
import com.augmentalis.overlay.controller.OverlayController
import com.augmentalis.overlay.controller.Suggestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Central integration bridge connecting overlay UI to NLU and Chat features.
 *
 * Orchestrates the complete voice interaction flow:
 * 1. Transcript from VoiceRecognizer → NLU classification
 * 2. Intent from NLU → Chat response generation
 * 3. Response from Chat → Update OverlayController
 * 4. Suggestions based on context and intent
 *
 * Architecture:
 * OverlayService → AvaIntegrationBridge → NluConnector → ChatConnector → OverlayController
 *
 * @param context Android context
 * @param controller Overlay state controller
 * @author Manoj Jhawar
 */
class AvaIntegrationBridge(
    private val context: Context,
    private val controller: OverlayController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // Wire controller callback
        controller.onSuggestionExecute = ::executeSuggestion

        // Start context monitoring
        startContextMonitoring()
    }

    // Integration connectors (lazy initialized)
    private val nluConnector by lazy { NluConnector(context) }
    private val chatConnector by lazy { ChatConnector(context) }
    private val contextEngine by lazy { ContextEngine(context) }

    // Current processing state
    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    /**
     * Process voice transcript through full pipeline
     *
     * @param transcript Voice input text
     */
    fun processTranscript(transcript: String) {
        if (_processing.value) return // Prevent concurrent processing

        scope.launch {
            try {
                _processing.value = true
                controller.onTranscript(transcript)

                // Step 1: Classify intent via NLU
                val intent = nluConnector.classifyIntent(transcript)

                // Step 2: Generate suggestions based on intent
                val suggestions = generateSuggestions(intent)
                controller.updateSuggestions(suggestions)

                // Step 3: Generate AI response via Chat
                val response = chatConnector.generateResponse(transcript, intent)
                controller.onResponse(response)

            } catch (e: Exception) {
                controller.onError("Failed to process request: ${e.message}")
            } finally {
                _processing.value = false
            }
        }
    }

    /**
     * Execute a suggestion action
     *
     * @param suggestion Selected suggestion
     */
    fun executeSuggestion(suggestion: Suggestion) {
        scope.launch {
            try {
                when (suggestion.action) {
                    "copy" -> executeCopyAction()
                    "translate" -> executeTranslateAction()
                    "search" -> executeSearchAction()
                    "summarize" -> executeSummarizeAction()
                    else -> {
                        // Custom suggestion - process as new voice input
                        processTranscript(suggestion.label)
                    }
                }
            } catch (e: Exception) {
                controller.onError("Failed to execute action: ${e.message}")
            }
        }
    }

    /**
     * Start monitoring active app context for smart suggestions
     */
    private fun startContextMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    // Detect active app every 3 seconds
                    val appContext = contextEngine.detectActiveApp()

                    if (appContext != null && !controller.expanded.value) {
                        // Update suggestions based on context (only when collapsed)
                        val smartSuggestions = contextEngine.generateSmartSuggestions(appContext)
                        val suggestions = smartSuggestions.map {
                            Suggestion(it.label, it.action, it.icon)
                        }
                        controller.updateSuggestions(suggestions)
                    }
                } catch (e: Exception) {
                    // Continue monitoring even if detection fails
                }

                delay(3000) // Check every 3 seconds
            }
        }
    }

    /**
     * Generate contextual suggestions based on intent
     */
    private fun generateSuggestions(intent: String): List<Suggestion> {
        // First try context-aware suggestions
        val appContext = contextEngine.activeApp.value
        if (appContext != null) {
            val smartSuggestions = contextEngine.generateSmartSuggestions(appContext)
            val contextSuggestions = smartSuggestions.map {
                Suggestion(it.label, it.action, it.icon)
            }

            // Merge with intent-based suggestions
            val intentSuggestions = getIntentBasedSuggestions(intent)
            return (contextSuggestions + intentSuggestions).distinctBy { it.action }.take(4)
        }

        return getIntentBasedSuggestions(intent)
    }

    /**
     * Get intent-based suggestions (fallback)
     */
    private fun getIntentBasedSuggestions(intent: String): List<Suggestion> {
        return when (intent) {
            "search", "query" -> listOf(
                Suggestion("Search web", "search"),
                Suggestion("Summarize", "summarize"),
                Suggestion("Related topics", "related")
            )
            "translate" -> listOf(
                Suggestion("Spanish", "translate_es"),
                Suggestion("French", "translate_fr"),
                Suggestion("Copy", "copy")
            )
            "reminder", "schedule" -> listOf(
                Suggestion("Set reminder", "reminder"),
                Suggestion("View calendar", "calendar"),
                Suggestion("Snooze", "snooze")
            )
            "message", "communication" -> listOf(
                Suggestion("Send", "send"),
                Suggestion("Edit", "edit"),
                Suggestion("Cancel", "cancel")
            )
            else -> listOf(
                Suggestion("Copy", "copy"),
                Suggestion("Search", "search"),
                Suggestion("Summarize", "summarize")
            )
        }
    }

    // Suggestion action handlers

    private fun executeCopyAction() {
        val response = controller.response.value
        if (response != null) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("AVA Response", response)
            clipboard.setPrimaryClip(clip)
            controller.onResponse("✓ Copied to clipboard")
        }
    }

    private fun executeTranslateAction() {
        val transcript = controller.transcript.value
        if (transcript != null) {
            processTranscript("Translate this to Spanish: $transcript")
        }
    }

    private fun executeSearchAction() {
        val transcript = controller.transcript.value
        if (transcript != null) {
            processTranscript("Search for: $transcript")
        }
    }

    private fun executeSummarizeAction() {
        val response = controller.response.value
        if (response != null) {
            processTranscript("Summarize this: $response")
        }
    }

    /**
     * Release resources
     */
    fun release() {
        // Scope will be cancelled automatically when parent is destroyed
    }
}
