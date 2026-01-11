/**
 * ILlmProcessor.kt - LLM Processor Interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Platform-agnostic interface for LLM (Large Language Model) processing.
 * Provides natural language fallback when NLU classification fails.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * Interface for LLM (Large Language Model) processing.
 *
 * Provides natural language command interpretation as a fallback
 * when registry lookup and NLU classification fail.
 * Android implementation wraps LocalLLMProvider from Modules/LLM.
 */
interface ILlmProcessor {
    /**
     * Initialize the LLM processor.
     * Discovers and loads models from external storage.
     *
     * @return Result indicating success or failure
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Interpret a natural language command.
     *
     * @param utterance The voice input text to interpret
     * @param nluSchema Schema describing available commands
     * @param availableCommands List of command phrases to match against
     * @return LlmResult with interpretation or no match
     */
    suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult

    /**
     * Check if the LLM processor is available and initialized.
     */
    fun isAvailable(): Boolean

    /**
     * Check if an LLM model is loaded and ready.
     */
    fun isModelLoaded(): Boolean

    /**
     * Dispose resources and cleanup.
     */
    suspend fun dispose()
}

/**
 * Result of LLM interpretation.
 */
sealed class LlmResult {
    /**
     * Successfully interpreted the command.
     */
    data class Interpreted(
        val matchedCommand: String,
        val confidence: Float,
        val explanation: String? = null
    ) : LlmResult()

    /**
     * No matching command could be determined.
     */
    data object NoMatch : LlmResult()

    /**
     * An error occurred during interpretation.
     */
    data class Error(val message: String) : LlmResult()
}

/**
 * Configuration for LLM processing.
 */
data class LlmConfig(
    /** Base path for LLM models on external storage */
    val modelBasePath: String = "/sdcard/ava-ai-models/llm",
    /** Response timeout in milliseconds */
    val responseTimeout: Long = 10_000L,
    /** Maximum tokens to generate */
    val maxTokens: Int = 50,
    /** Temperature for generation (lower = more deterministic) */
    val temperature: Float = 0.3f,
    /** Whether LLM is enabled */
    val enabled: Boolean = true
) {
    companion object {
        val DEFAULT = LlmConfig()
        val DISABLED = LlmConfig(enabled = false)
    }
}
