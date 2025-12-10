/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.ava.features.llm.alc.loader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * Model Configuration Loader
 *
 * Loads model configuration from mlc-chat-config.json or ava-model-config.json.
 * Extracts critical parameters including stop tokens for proper generation termination.
 *
 * Fix for Issue P0-3: Stop tokens were hardcoded as [0, 1, 2] which doesn't match
 * actual model vocabularies. This loader extracts the correct stop tokens from
 * the model configuration file.
 *
 * Created: 2025-11-30
 */
object ModelConfigLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Model configuration data class
     */
    @Serializable
    data class ModelConfig(
        @SerialName("model")
        val model: String? = null,

        @SerialName("model_type")
        val modelType: String? = null,

        @SerialName("vocab_size")
        val vocabSize: Int = 32000,

        @SerialName("context_window_size")
        val contextWindowSize: Int = 2048,

        @SerialName("temperature")
        val temperature: Float = 0.7f,

        @SerialName("top_p")
        val topP: Float = 0.95f,

        // Stop token configuration
        @SerialName("eos_token_id")
        val eosTokenId: Int = 1,

        @SerialName("bos_token_id")
        val bosTokenId: Int = 0,

        @SerialName("pad_token_id")
        val padTokenId: Int = 0,

        @SerialName("stop_token_ids")
        val stopTokenIds: List<Int> = emptyList(),

        @SerialName("stop_tokens")
        val stopTokens: List<Int>? = null,

        // Tokenizer info
        @SerialName("tokenizer_files")
        val tokenizerFiles: List<String>? = null
    ) {
        /**
         * Get all stop token IDs for this model.
         *
         * Combines:
         * - stop_token_ids (explicit list)
         * - stop_tokens (alternate name)
         * - eos_token_id (end of sequence)
         *
         * @return Set of all stop token IDs
         */
        fun getAllStopTokens(): Set<Int> {
            val tokens = mutableSetOf<Int>()

            // Add EOS token (always a stop token)
            tokens.add(eosTokenId)

            // Add explicit stop token IDs
            tokens.addAll(stopTokenIds)

            // Add alternate stop tokens field
            stopTokens?.let { tokens.addAll(it) }

            return tokens
        }
    }

    /**
     * Load model configuration from directory.
     *
     * Searches for configuration files in order:
     * 1. ava-model-config.json (AVA format)
     * 2. mlc-chat-config.json (MLC-LLM format)
     *
     * @param modelDir Model directory path
     * @return ModelConfig or null if not found
     */
    fun loadConfig(modelDir: File): ModelConfig? {
        return try {
            // Try AVA format first
            val avaConfig = File(modelDir, "ava-model-config.json")
            if (avaConfig.exists()) {
                Timber.d("Loading AVA model config from ${avaConfig.name}")
                return parseConfig(avaConfig)
            }

            // Fall back to MLC-LLM format
            val mlcConfig = File(modelDir, "mlc-chat-config.json")
            if (mlcConfig.exists()) {
                Timber.d("Loading MLC model config from ${mlcConfig.name}")
                return parseConfig(mlcConfig)
            }

            Timber.w("No model config found in ${modelDir.name}")
            null

        } catch (e: Exception) {
            Timber.e(e, "Failed to load model config from ${modelDir.name}")
            null
        }
    }

    /**
     * Load model configuration from path string.
     *
     * @param modelPath Model directory path
     * @return ModelConfig or null if not found
     */
    fun loadConfig(modelPath: String): ModelConfig? {
        return loadConfig(File(modelPath))
    }

    /**
     * Parse configuration file.
     */
    private fun parseConfig(configFile: File): ModelConfig {
        val configText = configFile.readText()
        val config = json.decodeFromString<ModelConfig>(configText)

        Timber.d("Parsed model config: model=${config.model}, " +
                "vocab_size=${config.vocabSize}, " +
                "eos=${config.eosTokenId}, " +
                "stop_tokens=${config.getAllStopTokens()}")

        return config
    }

    /**
     * Get stop tokens for a model directory.
     *
     * Convenience method that loads config and extracts stop tokens.
     *
     * @param modelDir Model directory
     * @return Set of stop token IDs, or default [1] if config not found
     */
    fun getStopTokens(modelDir: File): Set<Int> {
        val config = loadConfig(modelDir)
        return config?.getAllStopTokens() ?: DEFAULT_STOP_TOKENS
    }

    /**
     * Get stop tokens for a model path.
     *
     * @param modelPath Model directory path
     * @return Set of stop token IDs
     */
    fun getStopTokens(modelPath: String): Set<Int> {
        return getStopTokens(File(modelPath))
    }

    /**
     * Default stop tokens (used when config not available).
     *
     * Token ID 1 is EOS for most models (Gemma, Llama 2, etc.)
     * Token ID 2 is often EOS for Llama 3.x
     */
    private val DEFAULT_STOP_TOKENS = setOf(1, 2)

    /**
     * Known stop tokens by model family.
     *
     * Used as fallback when config parsing fails.
     */
    val KNOWN_STOP_TOKENS = mapOf(
        "gemma" to setOf(1, 107),           // <eos>, <end_of_turn>
        "gemma-2" to setOf(1, 107),
        "gemma-3" to setOf(1, 107),
        "llama" to setOf(2),                 // Llama 2
        "llama-3" to setOf(128001, 128009),  // Llama 3.x
        "qwen" to setOf(151643, 151645),     // Qwen 2.5/3
        "phi" to setOf(32000, 32001),        // Phi-2/3
        "mistral" to setOf(2)                // Mistral
    )

    /**
     * Get stop tokens by model family name.
     *
     * @param modelFamily Model family name (e.g., "gemma", "llama-3")
     * @return Set of known stop tokens for this family
     */
    fun getStopTokensByFamily(modelFamily: String): Set<Int> {
        val normalizedFamily = modelFamily.lowercase()
        return KNOWN_STOP_TOKENS.entries
            .firstOrNull { normalizedFamily.contains(it.key) }
            ?.value
            ?: DEFAULT_STOP_TOKENS
    }
}
