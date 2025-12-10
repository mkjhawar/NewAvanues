package com.augmentalis.ava.features.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.features.chat.data.BuiltInIntents
import com.augmentalis.ava.features.llm.response.IntentTemplates
import com.augmentalis.ava.features.llm.response.ResponseChunk
import com.augmentalis.ava.features.llm.response.ResponseContext
import com.augmentalis.ava.features.llm.response.ResponseGenerator
import com.augmentalis.ava.features.llm.teacher.LLMResponseParser
import com.augmentalis.ava.features.nlu.IntentClassification
import com.augmentalis.ava.features.nlu.NLUSelfLearner
import com.augmentalis.ava.features.nlu.learning.IntentLearningManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Response Coordinator - Single Responsibility: LLM/template response generation
 *
 * Extracted from ChatViewModel as part of SOLID refactoring (P0).
 * Handles all response generation operations:
 * - LLM streaming responses with template fallback
 * - Self-learning from LLM responses (ADR-013)
 * - Response cleanup and metadata extraction
 *
 * @param responseGenerator LLM-based response generator with template fallback
 * @param learningManager Legacy learning system for fallback
 * @param nluSelfLearner New self-learning system (ADR-013)
 * @param chatPreferences User preferences for thresholds
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class ResponseCoordinator @Inject constructor(
    private val responseGenerator: ResponseGenerator,
    private val learningManager: IntentLearningManager,
    private val nluSelfLearner: NLUSelfLearner,
    private val chatPreferences: ChatPreferences
) {
    companion object {
        private const val TAG = "ResponseCoordinator"
    }

    // ==================== State ====================

    private val _lastResponder = MutableStateFlow<String?>(null)
    val lastResponder: StateFlow<String?> = _lastResponder.asStateFlow()

    private val _lastResponderTimestamp = MutableStateFlow(0L)
    val lastResponderTimestamp: StateFlow<Long> = _lastResponderTimestamp.asStateFlow()

    private val _llmFallbackInvoked = MutableStateFlow(false)
    val llmFallbackInvoked: StateFlow<Boolean> = _llmFallbackInvoked.asStateFlow()

    // ==================== Thresholds ====================

    val llmFallbackThreshold: StateFlow<Float> = chatPreferences.llmFallbackThreshold
    val selfLearningThreshold: StateFlow<Float> = chatPreferences.selfLearningThreshold

    // ==================== Response Generation ====================

    /**
     * Response generation result containing content and metadata.
     */
    data class ResponseResult(
        val content: String,
        val wasLLMFallback: Boolean,
        val respondedBy: String
    )

    /**
     * Generate a response for the given classification and context.
     *
     * Handles:
     * - LLM streaming with template fallback
     * - Responder tracking (NLU vs LLM)
     * - Self-learning from low confidence responses
     *
     * @param userMessage User input text
     * @param classification NLU classification result
     * @param context Response context including conversation history
     * @param ragContext Optional RAG context to include
     * @param scope Coroutine scope for self-learning coroutine
     * @return ResponseResult with content and metadata
     */
    suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext,
        ragContext: String? = null,
        scope: CoroutineScope
    ): ResponseResult {
        _llmFallbackInvoked.value = false

        val responseContentBuilder = StringBuilder()
        var responseGenerationError: String? = null

        // Prepare user message with RAG context if available
        val userMessageForLLM = if (ragContext != null) {
            buildPromptWithContext(userMessage, ragContext)
        } else {
            userMessage
        }

        Log.d(TAG, "Generating response using ResponseGenerator")
        Log.d(TAG, "  Intent: ${classification.intent}, Confidence: ${classification.confidence}")
        if (ragContext != null) {
            Log.d(TAG, "  RAG context: ${ragContext.length} chars")
        }

        // Stream response from LLM
        try {
            responseGenerator.generateResponse(
                userMessage = userMessageForLLM,
                classification = classification,
                context = context
            ).collect { chunk ->
                when (chunk) {
                    is ResponseChunk.Text -> {
                        responseContentBuilder.append(chunk.content)
                        Log.d(TAG, "Response chunk received: ${chunk.content}")
                    }
                    is ResponseChunk.Complete -> {
                        responseContentBuilder.clear()
                        responseContentBuilder.append(chunk.fullText)
                        Log.d(TAG, "Response complete: ${chunk.fullText}")
                    }
                    is ResponseChunk.Error -> {
                        responseGenerationError = chunk.message
                        Log.e(TAG, "Response generation error: ${chunk.message}", chunk.exception)
                    }
                }
            }
        } catch (e: Exception) {
            responseGenerationError = "Unexpected error: ${e.message}"
            Log.e(TAG, "Exception during response generation", e)
        }

        // Use generated response or fallback to unknown template
        val rawResponseContent = if (responseContentBuilder.isEmpty() && responseGenerationError != null) {
            Log.w(TAG, "Response generation failed, using fallback unknown template")
            IntentTemplates.getResponse(BuiltInIntents.UNKNOWN)
        } else {
            responseContentBuilder.toString()
        }

        // Determine if LLM fallback was triggered
        val currentLLMFallbackThreshold = llmFallbackThreshold.value
        val isLowConfidence = classification.confidence < currentLLMFallbackThreshold

        return if (isLowConfidence) {
            handleLowConfidenceResponse(
                rawResponseContent = rawResponseContent,
                userMessage = userMessage,
                classification = classification,
                scope = scope,
                currentThreshold = currentLLMFallbackThreshold
            )
        } else {
            handleHighConfidenceResponse(
                rawResponseContent = rawResponseContent,
                classification = classification,
                currentThreshold = currentLLMFallbackThreshold
            )
        }
    }

    /**
     * Handle low confidence response - trigger LLM fallback and learning.
     */
    private suspend fun handleLowConfidenceResponse(
        rawResponseContent: String,
        userMessage: String,
        classification: IntentClassification,
        scope: CoroutineScope,
        currentThreshold: Float
    ): ResponseResult {
        Log.w(TAG, "╔════════════════════════════════════════════════════════")
        Log.w(TAG, "║ *** LLM FALLBACK TRIGGERED ***")
        Log.w(TAG, "║ NLU confidence: ${classification.confidence} < $currentThreshold")
        Log.w(TAG, "║ Intent classified: ${classification.intent}")
        Log.w(TAG, "║ Routing to LLM for response generation")
        Log.w(TAG, "╚════════════════════════════════════════════════════════")

        _llmFallbackInvoked.value = true
        setResponder("LLM")

        // Attempt self-learning
        Log.d(TAG, "Low confidence, attempting to learn from LLM response")
        val llmTeacherResult = LLMResponseParser.parse(rawResponseContent)
        val learned = if (llmTeacherResult != null && LLMResponseParser.hasTeachingMetadata(rawResponseContent)) {
            Log.i(TAG, "ADR-013: Using new LLM Teacher format")
            scope.launch {
                val selfLearned = nluSelfLearner.learnFromLLM(
                    utterance = userMessage,
                    intent = llmTeacherResult.intent,
                    confidence = llmTeacherResult.confidence,
                    variations = llmTeacherResult.variations
                )
                if (selfLearned) {
                    Log.i(TAG, "ADR-013: Successfully self-learned intent '${llmTeacherResult.intent}'")
                }
            }
            true
        } else {
            learningManager.learnFromResponse(
                userMessage = userMessage,
                llmResponse = rawResponseContent
            )
        }

        if (learned) {
            Log.i(TAG, "Successfully learned intent from LLM response")
        }

        // Clean response
        val cleanedContent = if (llmTeacherResult != null && LLMResponseParser.hasTeachingMetadata(rawResponseContent)) {
            LLMResponseParser.extractResponseOnly(rawResponseContent)
        } else {
            learningManager.cleanResponse(rawResponseContent)
        }

        return ResponseResult(
            content = cleanedContent,
            wasLLMFallback = true,
            respondedBy = "LLM"
        )
    }

    /**
     * Handle high confidence response.
     */
    private fun handleHighConfidenceResponse(
        rawResponseContent: String,
        classification: IntentClassification,
        currentThreshold: Float
    ): ResponseResult {
        val respondedBy = if (classification.confidence >= currentThreshold) {
            Log.i(TAG, "*** NLU HANDLED (Template Response, Confidence: ${classification.confidence} >= $currentThreshold) ***")
            setResponder("NLU")
            "NLU"
        } else {
            Log.i(TAG, "*** LLM HANDLED (No NLU Classification) ***")
            setResponder("LLM")
            "LLM"
        }

        return ResponseResult(
            content = rawResponseContent,
            wasLLMFallback = false,
            respondedBy = respondedBy
        )
    }

    /**
     * Set the last responder and timestamp.
     */
    fun setResponder(responder: String) {
        _lastResponder.value = responder
        _lastResponderTimestamp.value = System.currentTimeMillis()
    }

    /**
     * Reset fallback flag (called at start of each message).
     */
    fun resetFallbackFlag() {
        _llmFallbackInvoked.value = false
    }

    /**
     * Build a prompt with RAG context.
     */
    private fun buildPromptWithContext(userMessage: String, ragContext: String): String {
        return """
            |Based on the following context, please answer the question.
            |
            |Context:
            |$ragContext
            |
            |Question: $userMessage
            """.trimMargin()
    }
}
