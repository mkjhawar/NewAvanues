package com.augmentalis.llm

import kotlinx.coroutines.flow.Flow

/**
 * Result type for LLM operations
 *
 * Cross-platform result wrapper for success/error handling.
 */
sealed class LLMResult<out T> {
    data class Success<T>(val data: T) : LLMResult<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : LLMResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.cause
}

/**
 * Main interface for LLM (Large Language Model) providers
 *
 * This interface abstracts different LLM implementations:
 * - LocalLLMProvider: On-device inference (Android: MLC/GGUF, Desktop: ONNX/Ollama)
 * - CloudLLMProvider: Cloud API calls (Gemini, GPT, Claude, etc.)
 * - HybridLLMProvider: Intelligent routing between local and cloud
 *
 * All methods are suspend functions to support coroutine-based async operations.
 * Responses are streamed via Flow<LLMResponse> for real-time UI updates.
 */
interface LLMProvider {

    /**
     * Initialize the LLM provider
     *
     * For local providers: Load model into memory
     * For cloud providers: Validate API keys and connectivity
     *
     * @param config Configuration for the provider
     * @return LLMResult.Success if initialization succeeds, LLMResult.Error otherwise
     */
    suspend fun initialize(config: LLMConfig): LLMResult<Unit>

    /**
     * Generate a streaming response to a single prompt
     *
     * This is a stateless operation - no conversation history is maintained.
     * Use chat() for multi-turn conversations.
     *
     * @param prompt User's input text
     * @param options Generation options (temperature, max tokens, etc.)
     * @return Flow of LLMResponse (stream) as they're generated
     */
    suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse>

    /**
     * Generate a streaming response within a conversation context
     *
     * This maintains conversation history for multi-turn dialogs.
     * The provider handles formatting messages into the model's expected format.
     *
     * @param messages Conversation history (system, user, assistant messages)
     * @param options Generation options
     * @return Flow of LLMResponse (stream) as they're generated
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse>

    /**
     * Stop any ongoing generation
     *
     * This cancels the current inference and stops the streaming response.
     */
    suspend fun stop()

    /**
     * Reset the provider state
     *
     * Clears conversation history, cancels ongoing generations.
     * Model remains loaded in memory.
     */
    suspend fun reset()

    /**
     * Clean up resources and unload the model
     *
     * For local providers: Unload model from memory
     * For cloud providers: Close network connections
     *
     * Call this when the LLM is no longer needed (e.g., app shutdown)
     */
    suspend fun cleanup()

    /**
     * Check if the provider is currently processing a request
     *
     * @return true if generating, false otherwise
     */
    fun isGenerating(): Boolean

    /**
     * Get provider metadata (name, version, capabilities)
     *
     * @return Provider information
     */
    fun getInfo(): LLMProviderInfo

    /**
     * Check provider health status
     *
     * Performs a lightweight health check to verify the provider is operational.
     * For cloud providers: Pings the API endpoint
     * For local providers: Checks if model is loaded
     *
     * @return LLMResult.Success with ProviderHealth if healthy, LLMResult.Error otherwise
     */
    suspend fun checkHealth(): LLMResult<ProviderHealth>

    /**
     * Estimate cost for a given number of tokens
     *
     * For cloud providers: Returns actual cost based on provider pricing
     * For local providers: Returns 0.0 (no cost)
     *
     * @param inputTokens Number of input tokens
     * @param outputTokens Number of output tokens
     * @return Estimated cost in USD
     */
    fun estimateCost(inputTokens: Int, outputTokens: Int): Double

    // ==================== Command Interpretation (VoiceOS AI Integration) ====================

    /**
     * Interpret a voice command utterance using LLM
     *
     * Maps natural language utterances to available commands when traditional
     * NLU classification confidence is low. This enables handling of:
     * - Complex multi-step commands
     * - Ambiguous phrasings
     * - Context-dependent commands
     *
     * @param utterance The user's spoken command (transcribed text)
     * @param availableCommands List of command identifiers the system can execute
     * @param context Optional context about the current state (e.g., "on home screen")
     * @return CommandInterpretationResult indicating match, no-match, or error
     */
    suspend fun interpretCommand(
        utterance: String,
        availableCommands: List<String>,
        context: String? = null
    ): CommandInterpretationResult

    /**
     * Clarify a command when multiple candidates match
     *
     * When the NLU identifies multiple possible commands with similar confidence,
     * the LLM can help disambiguate by asking the user clarifying questions
     * or selecting the most likely intent based on context.
     *
     * @param utterance The user's spoken command (transcribed text)
     * @param candidates List of candidate command identifiers to choose from
     * @return ClarificationResult with selected command or clarification question
     */
    suspend fun clarifyCommand(
        utterance: String,
        candidates: List<String>
    ): ClarificationResult
}
