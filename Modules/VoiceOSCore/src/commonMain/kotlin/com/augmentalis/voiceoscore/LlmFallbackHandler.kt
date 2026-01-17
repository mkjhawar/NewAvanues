/**
 * LlmFallbackHandler.kt - LLM Fallback Handler for Low Confidence NLU Results
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Handles NLU classification results with low confidence by routing to LLM providers.
 * Uses a cascading fallback strategy:
 * 1. LocalLLMProvider (on-device, privacy-first)
 * 2. CloudLLMProvider (when local fails or is unavailable)
 *
 * This handler bridges the NLU and LLM systems to provide intelligent
 * command clarification when semantic matching is uncertain.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.NluResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Result of LLM fallback processing.
 */
sealed class FallbackResult {
    /**
     * Successfully clarified the command via LLM.
     *
     * @property command The clarified/matched command
     * @property confidence Confidence score (0.0-1.0)
     * @property source Which LLM provider was used ("local" or "cloud")
     * @property explanation Optional explanation of the match
     */
    data class Clarified(
        val command: QuantizedCommand,
        val confidence: Float,
        val source: String,
        val explanation: String? = null
    ) : FallbackResult()

    /**
     * LLM generated a new action that doesn't map to existing commands.
     * This allows the system to handle novel requests.
     *
     * @property actionType The type of action to perform
     * @property parameters Parameters for the action
     * @property source Which LLM provider was used
     * @property rawResponse The raw LLM response for debugging
     */
    data class GeneratedAction(
        val actionType: String,
        val parameters: Map<String, String>,
        val source: String,
        val rawResponse: String
    ) : FallbackResult()

    /**
     * No clarification possible - neither local nor cloud could help.
     *
     * @property reason Description of why fallback failed
     * @property attemptedProviders List of providers that were tried
     */
    data class NoMatch(
        val reason: String,
        val attemptedProviders: List<String> = emptyList()
    ) : FallbackResult()

    /**
     * An error occurred during fallback processing.
     *
     * @property message Error message
     * @property provider Which provider caused the error
     * @property exception Optional underlying exception
     */
    data class Error(
        val message: String,
        val provider: String? = null,
        val exception: Throwable? = null
    ) : FallbackResult()
}

/**
 * Configuration for LLM fallback handling.
 */
data class FallbackConfig(
    /**
     * Minimum NLU confidence to trigger fallback.
     * Results with confidence below this threshold will use LLM fallback.
     */
    val confidenceThreshold: Float = 0.5f,

    /**
     * Whether to try local LLM first.
     * If false, goes directly to cloud (not recommended for privacy).
     */
    val tryLocalFirst: Boolean = true,

    /**
     * Whether to fall back to cloud if local fails.
     * Set to false for fully offline operation.
     */
    val allowCloudFallback: Boolean = true,

    /**
     * Timeout for local LLM response in milliseconds.
     */
    val localTimeout: Long = 10_000L,

    /**
     * Timeout for cloud LLM response in milliseconds.
     */
    val cloudTimeout: Long = 15_000L,

    /**
     * Whether fallback is enabled at all.
     */
    val enabled: Boolean = true,

    /**
     * Maximum number of candidate commands to send to LLM.
     * Limits context size for faster inference.
     */
    val maxCandidates: Int = 30,

    /**
     * Temperature for LLM generation.
     * Lower values = more deterministic.
     */
    val temperature: Float = 0.3f,

    /**
     * Maximum tokens for LLM response.
     */
    val maxTokens: Int = 100
) {
    companion object {
        val DEFAULT = FallbackConfig()
        val OFFLINE_ONLY = FallbackConfig(allowCloudFallback = false)
        val CLOUD_PREFERRED = FallbackConfig(tryLocalFirst = false)
        val DISABLED = FallbackConfig(enabled = false)
    }
}

/**
 * Interface for LLM fallback handling.
 *
 * Platform implementations provide access to LocalLLMProvider and CloudLLMProvider.
 * This interface defines the contract for cascading LLM fallback when NLU confidence is low.
 */
interface ILlmFallbackHandler {
    /**
     * Initialize the fallback handler.
     * Initializes both local and cloud LLM providers.
     *
     * @return Result indicating success or failure
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Process a low-confidence NLU result using LLM fallback.
     *
     * @param utterance Original user utterance
     * @param nluResult The low-confidence NLU result
     * @param candidateCommands Available commands to match against
     * @return FallbackResult with clarified command or action
     */
    suspend fun handleLowConfidence(
        utterance: String,
        nluResult: NluResult,
        candidateCommands: List<QuantizedCommand>
    ): FallbackResult

    /**
     * Process an ambiguous NLU result using LLM to disambiguate.
     *
     * @param utterance Original user utterance
     * @param ambiguousResult The ambiguous NLU result with multiple candidates
     * @return FallbackResult with clarified command
     */
    suspend fun handleAmbiguous(
        utterance: String,
        ambiguousResult: NluResult.Ambiguous
    ): FallbackResult

    /**
     * Generate an action for an utterance that doesn't match any known command.
     * This is the "creative" fallback for novel requests.
     *
     * @param utterance Original user utterance
     * @param context Optional context about the current screen/state
     * @return FallbackResult with generated action or no match
     */
    suspend fun generateAction(
        utterance: String,
        context: String? = null
    ): FallbackResult

    /**
     * Check if local LLM is available.
     */
    fun isLocalAvailable(): Boolean

    /**
     * Check if cloud LLM is available.
     */
    fun isCloudAvailable(): Boolean

    /**
     * Check if any LLM fallback is available.
     */
    fun isAvailable(): Boolean = isLocalAvailable() || isCloudAvailable()

    /**
     * Get the current configuration.
     */
    fun getConfig(): FallbackConfig

    /**
     * Update the configuration.
     */
    fun setConfig(config: FallbackConfig)

    /**
     * Dispose resources and cleanup.
     */
    suspend fun dispose()
}

/**
 * Factory for creating platform-specific LLM fallback handlers.
 *
 * Platform implementations:
 * - Android: Uses LocalLLMProvider and CloudLLMProvider from Modules/LLM
 * - iOS: Stub (TODO: llama.cpp integration)
 * - Desktop: Stub (TODO: llama.cpp JNI)
 */
expect object LlmFallbackHandlerFactory {
    /**
     * Create a platform-specific LLM fallback handler.
     *
     * @param config Fallback configuration
     * @return Platform-specific ILlmFallbackHandler implementation
     */
    fun create(config: FallbackConfig = FallbackConfig.DEFAULT): ILlmFallbackHandler
}
