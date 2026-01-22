/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of Response Coordinator.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.chat.data.BuiltInIntents
import com.augmentalis.llm.response.ResponseContext
import com.augmentalis.nlu.IntentClassification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop (JVM) implementation of IResponseCoordinator.
 *
 * Generates responses for classified intents using:
 * - Template-based responses for known intents
 * - LLM fallback for complex/unknown queries (via LLM module)
 *
 * Features:
 * - Responder tracking (NLU vs LLM)
 * - Configurable confidence thresholds
 * - Self-learning support (ADR-013)
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class ResponseCoordinatorDesktop(
    private val llmFallbackThresholdValue: Float = 0.6f,
    private val selfLearningThresholdValue: Float = 0.4f
) : IResponseCoordinator {

    // ==================== State ====================

    private val _lastResponder = MutableStateFlow<String?>(null)
    override val lastResponder: StateFlow<String?> = _lastResponder.asStateFlow()

    private val _lastResponderTimestamp = MutableStateFlow(0L)
    override val lastResponderTimestamp: StateFlow<Long> = _lastResponderTimestamp.asStateFlow()

    private val _llmFallbackInvoked = MutableStateFlow(false)
    override val llmFallbackInvoked: StateFlow<Boolean> = _llmFallbackInvoked.asStateFlow()

    private val _llmFallbackThreshold = MutableStateFlow(llmFallbackThresholdValue)
    override val llmFallbackThreshold: StateFlow<Float> = _llmFallbackThreshold.asStateFlow()

    private val _selfLearningThreshold = MutableStateFlow(selfLearningThresholdValue)
    override val selfLearningThreshold: StateFlow<Float> = _selfLearningThreshold.asStateFlow()

    // ==================== Response Templates ====================

    private val responseTemplates: Map<String, List<String>> = mapOf(
        // Device Control
        BuiltInIntents.CONTROL_LIGHTS to listOf(
            "I'll control the lights for you.",
            "Adjusting the lights now.",
            "Lights command received."
        ),
        BuiltInIntents.CONTROL_TEMPERATURE to listOf(
            "Adjusting the temperature.",
            "Temperature control activated.",
            "I'll set the temperature for you."
        ),

        // Information
        BuiltInIntents.CHECK_WEATHER to listOf(
            "Let me check the weather for you.",
            "Getting the current weather information.",
            "Checking weather conditions."
        ),
        BuiltInIntents.SHOW_TIME to listOf(
            "The current time is displayed.",
            "Here's the current time.",
            "Showing the time now."
        ),

        // Productivity
        BuiltInIntents.SET_ALARM to listOf(
            "I'll set that alarm for you.",
            "Alarm has been scheduled.",
            "Your alarm is being set."
        ),
        BuiltInIntents.SET_REMINDER to listOf(
            "I'll remind you about that.",
            "Reminder has been set.",
            "I've noted that reminder."
        ),

        // System/Meta
        BuiltInIntents.SHOW_HISTORY to listOf(
            "Opening conversation history.",
            "Here's your conversation history.",
            "Showing previous conversations."
        ),
        BuiltInIntents.NEW_CONVERSATION to listOf(
            "Starting a fresh conversation.",
            "New conversation started.",
            "Let's begin a new chat."
        ),
        BuiltInIntents.TEACH_AVA to listOf(
            "I'm ready to learn! What would you like to teach me?",
            "Teach mode activated. Show me what you'd like me to learn.",
            "I'm listening. What should I learn?"
        ),

        // System Control
        BuiltInIntents.SYSTEM_STOP to listOf("Stopping.", "Stopped."),
        BuiltInIntents.SYSTEM_BACK to listOf("Going back.", "Back."),
        BuiltInIntents.SYSTEM_CANCEL to listOf("Cancelled.", "Operation cancelled."),
        BuiltInIntents.SYSTEM_HOME to listOf("Going home.", "Home."),
        BuiltInIntents.SYSTEM_HELP to listOf(
            "How can I help you?",
            "What do you need assistance with?",
            "I'm here to help."
        ),
        BuiltInIntents.SYSTEM_QUIT to listOf("Goodbye!", "Exiting."),
        BuiltInIntents.SYSTEM_EXIT to listOf("Exiting.", "Exit."),
        BuiltInIntents.SYSTEM_PAUSE to listOf("Paused.", "Pausing."),
        BuiltInIntents.SYSTEM_RESUME to listOf("Resuming.", "Continuing."),
        BuiltInIntents.SYSTEM_MUTE to listOf("Muted.", "Sound off."),
        BuiltInIntents.SYSTEM_UNMUTE to listOf("Unmuted.", "Sound on."),

        // Navigation
        BuiltInIntents.NAVIGATION_UP to listOf("Moving up."),
        BuiltInIntents.NAVIGATION_DOWN to listOf("Moving down."),
        BuiltInIntents.NAVIGATION_LEFT to listOf("Moving left."),
        BuiltInIntents.NAVIGATION_RIGHT to listOf("Moving right."),
        BuiltInIntents.NAVIGATION_NEXT to listOf("Next."),
        BuiltInIntents.NAVIGATION_PREVIOUS to listOf("Previous."),
        BuiltInIntents.NAVIGATION_SELECT to listOf("Selected."),
        BuiltInIntents.NAVIGATION_ENTER to listOf("Entering."),

        // Unknown/Fallback
        BuiltInIntents.UNKNOWN to listOf(
            "I'm not sure I understood that. Could you rephrase?",
            "I didn't quite catch that. Can you try again?",
            "I'm still learning! Could you explain what you need?"
        )
    )

    // ==================== Response Generation ====================

    override suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext,
        ragContext: String?,
        scope: CoroutineScope
    ): IResponseCoordinator.ResponseResult {
        // Check if we should use LLM fallback
        val shouldUseLLM = classification.confidence < _llmFallbackThreshold.value ||
                classification.intent == BuiltInIntents.UNKNOWN

        return if (shouldUseLLM) {
            // LLM fallback
            generateLLMResponse(userMessage, ragContext, context)
        } else {
            // Template-based response
            generateTemplateResponse(classification)
        }
    }

    private fun generateTemplateResponse(
        classification: IntentClassification
    ): IResponseCoordinator.ResponseResult {
        val templates = responseTemplates[classification.intent]
            ?: responseTemplates[BuiltInIntents.UNKNOWN]
            ?: listOf("I received your message.")

        val response = templates.random()

        setResponder("NLU")
        _llmFallbackInvoked.value = false

        return IResponseCoordinator.ResponseResult(
            content = response,
            wasLLMFallback = false,
            respondedBy = "NLU"
        )
    }

    private suspend fun generateLLMResponse(
        userMessage: String,
        ragContext: String?,
        context: ResponseContext
    ): IResponseCoordinator.ResponseResult {
        // TODO: Integrate with LLM module for actual LLM responses
        // For now, return a placeholder response

        setResponder("LLM")
        _llmFallbackInvoked.value = true

        // Build prompt with RAG context if available
        val prompt = if (!ragContext.isNullOrBlank()) {
            """
            Context from documents:
            $ragContext

            User message: $userMessage

            Please provide a helpful response based on the context above.
            """.trimIndent()
        } else {
            userMessage
        }

        // Placeholder LLM response
        // In production, this would call the LLM module
        val response = "I understand you're asking about: $userMessage. " +
                "Let me help you with that. (LLM integration pending)"

        return IResponseCoordinator.ResponseResult(
            content = response,
            wasLLMFallback = true,
            respondedBy = "LLM"
        )
    }

    override fun setResponder(responder: String) {
        _lastResponder.value = responder
        _lastResponderTimestamp.value = System.currentTimeMillis()
    }

    override fun resetFallbackFlag() {
        _llmFallbackInvoked.value = false
    }

    // ==================== Configuration ====================

    /**
     * Update LLM fallback threshold.
     *
     * @param threshold New threshold (0.0 to 1.0)
     */
    fun setLLMFallbackThreshold(threshold: Float) {
        _llmFallbackThreshold.value = threshold.coerceIn(0.0f, 1.0f)
    }

    /**
     * Update self-learning threshold.
     *
     * @param threshold New threshold (0.0 to 1.0)
     */
    fun setSelfLearningThreshold(threshold: Float) {
        _selfLearningThreshold.value = threshold.coerceIn(0.0f, 1.0f)
    }

    /**
     * Add or update a response template.
     *
     * @param intent Intent identifier
     * @param templates List of response templates
     */
    fun addResponseTemplates(intent: String, templates: List<String>) {
        // Note: responseTemplates is immutable, this would need to be
        // changed to mutableMapOf for runtime modification
        println("[ResponseCoordinatorDesktop] Template update requested for: $intent")
    }

    companion object {
        @Volatile
        private var INSTANCE: ResponseCoordinatorDesktop? = null

        /**
         * Get singleton instance of ResponseCoordinatorDesktop.
         *
         * @return Singleton instance
         */
        fun getInstance(): ResponseCoordinatorDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ResponseCoordinatorDesktop().also {
                    INSTANCE = it
                }
            }
        }
    }
}
