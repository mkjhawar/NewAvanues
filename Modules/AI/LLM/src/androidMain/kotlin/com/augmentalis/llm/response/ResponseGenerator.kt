package com.augmentalis.llm.response

import com.augmentalis.nlu.IntentClassification
import kotlinx.coroutines.flow.Flow

// Import common types
import com.augmentalis.llm.ResponseChunk as CommonResponseChunk
import com.augmentalis.llm.GeneratorInfo as CommonGeneratorInfo
import com.augmentalis.llm.GeneratorType as CommonGeneratorType
import com.augmentalis.llm.ActionResult as CommonActionResult

/**
 * Android-specific response generator interface
 *
 * Uses IntentClassification from NLU module which is Android-specific.
 * For cross-platform code, use com.augmentalis.llm.ResponseGenerator instead.
 *
 * Design pattern: Strategy
 * Benefit: Easy to switch between implementations without changing ChatViewModel
 */
interface ResponseGenerator {

    /**
     * Generate response for user message
     *
     * @param userMessage Original user utterance
     * @param classification NLU classification result
     * @param context Additional context (action results, conversation history)
     * @return Flow of response chunks (streaming)
     */
    suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext = ResponseContext()
    ): Flow<ResponseChunk>

    /**
     * Check if generator is ready to use
     *
     * @return true if initialized and operational, false otherwise
     */
    fun isReady(): Boolean

    /**
     * Get generator metadata
     *
     * @return Information about this generator
     */
    fun getInfo(): GeneratorInfo
}

/**
 * Context for response generation (Android-specific)
 *
 * Contains additional information to guide response generation:
 * - Action results from executed intents
 * - Conversation history for multi-turn context
 * - User preferences and settings
 */
data class ResponseContext(
    /**
     * Result from action execution (if intent triggered an action)
     */
    val actionResult: ActionResult? = null,

    /**
     * Recent conversation history (user, assistant) pairs
     */
    val conversationHistory: List<Pair<String, String>> = emptyList(),

    /**
     * User preferences (language, formality, etc.)
     */
    val preferences: Map<String, String> = emptyMap(),

    /**
     * Additional metadata (time of day, location, etc.)
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Response chunk for streaming (Android-specific)
 *
 * Supports progressive UI updates (typewriter effect).
 * Maps to common ResponseChunk for cross-platform compatibility.
 */
sealed class ResponseChunk {
    /**
     * Text chunk
     */
    data class Text(val content: String) : ResponseChunk()

    /**
     * Complete response (final chunk)
     */
    data class Complete(
        val fullText: String,
        val metadata: Map<String, Any> = emptyMap()
    ) : ResponseChunk()

    /**
     * Error
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ResponseChunk()

    /**
     * Convert to common ResponseChunk
     */
    fun toCommon(): CommonResponseChunk = when (this) {
        is Text -> CommonResponseChunk.Text(content)
        is Complete -> CommonResponseChunk.Complete(fullText, metadata.mapValues { it.value.toString() })
        is Error -> CommonResponseChunk.Error(message, exception?.message)
    }
}

/**
 * Generator metadata (Android type alias to common)
 */
typealias GeneratorInfo = CommonGeneratorInfo

/**
 * Generator type enum (Android type alias to common)
 */
typealias GeneratorType = CommonGeneratorType

/**
 * Action result (Android type alias to common)
 */
typealias ActionResult = CommonActionResult
