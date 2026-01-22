/**
 * AIModelInfo.kt - Shared AI model metadata
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides standardized metadata for AI models across all plugin types.
 * Used by LLMPlugin, EmbeddingPlugin, and other AI-related plugins to
 * describe model characteristics for discovery and configuration.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.ai

import kotlinx.serialization.Serializable

/**
 * Metadata describing an AI model.
 *
 * This data class captures essential information about AI models used
 * by various plugins in the Universal Plugin Architecture. It enables:
 * - Model discovery and selection based on capabilities
 * - Resource planning based on size and requirements
 * - Version tracking for reproducibility
 * - Provider identification for licensing and API routing
 *
 * ## Usage
 * ```kotlin
 * val modelInfo = AIModelInfo(
 *     name = "Llama-3.2-3B-Instruct",
 *     version = "3.2.0",
 *     provider = "local",
 *     quantization = "q4_0",
 *     parameterCount = 3_000_000_000L,
 *     fileSizeBytes = 2_147_483_648L  // ~2GB
 * )
 * ```
 *
 * ## Provider Values
 * Common provider identifiers:
 * - `"local"` - Locally deployed model (llama.cpp, vLLM, etc.)
 * - `"openai"` - OpenAI API
 * - `"anthropic"` - Anthropic Claude API
 * - `"google"` - Google AI (Gemini)
 * - `"cohere"` - Cohere API
 * - `"huggingface"` - Hugging Face Inference API
 *
 * ## Quantization Values
 * Common quantization formats for local models:
 * - `"q4_0"` - 4-bit quantization (smallest, fastest)
 * - `"q4_k_m"` - 4-bit k-quants medium
 * - `"q5_k_m"` - 5-bit k-quants medium
 * - `"q8_0"` - 8-bit quantization
 * - `"fp16"` - 16-bit floating point
 * - `"fp32"` - 32-bit floating point (full precision)
 * - `null` - Cloud API (no local quantization)
 *
 * @property name Model name (e.g., "Llama-3.2-3B-Instruct", "gpt-4o")
 * @property version Model version string (semantic versioning preferred)
 * @property provider Provider identifier ("local", "openai", "anthropic", etc.)
 * @property quantization Quantization format for local models (null for cloud APIs)
 * @property parameterCount Number of model parameters (e.g., 7B = 7_000_000_000)
 * @property fileSizeBytes Model file size in bytes (null for cloud APIs)
 *
 * @since 1.0.0
 * @see LLMPlugin
 * @see EmbeddingPlugin
 */
@Serializable
data class AIModelInfo(
    val name: String,
    val version: String,
    val provider: String,
    val quantization: String? = null,
    val parameterCount: Long? = null,
    val fileSizeBytes: Long? = null
) {
    /**
     * Check if this is a locally deployed model.
     *
     * @return true if provider is "local"
     */
    fun isLocal(): Boolean = provider == PROVIDER_LOCAL

    /**
     * Check if this is a cloud-based API model.
     *
     * @return true if provider is not "local"
     */
    fun isCloud(): Boolean = provider != PROVIDER_LOCAL

    /**
     * Check if the model has quantization applied.
     *
     * @return true if quantization is specified
     */
    fun isQuantized(): Boolean = quantization != null

    /**
     * Get a human-readable description of model size.
     *
     * @return String like "3B parameters" or "Unknown size"
     */
    fun sizeDescription(): String {
        return parameterCount?.let { params ->
            when {
                params >= 1_000_000_000_000 -> "${params / 1_000_000_000_000}T parameters"
                params >= 1_000_000_000 -> "${params / 1_000_000_000}B parameters"
                params >= 1_000_000 -> "${params / 1_000_000}M parameters"
                else -> "$params parameters"
            }
        } ?: "Unknown size"
    }

    /**
     * Get a human-readable description of file size.
     *
     * @return String like "2.1 GB" or "N/A"
     */
    fun fileSizeDescription(): String {
        return fileSizeBytes?.let { size ->
            when {
                size >= 1_073_741_824 -> String.format("%.1f GB", size / 1_073_741_824.0)
                size >= 1_048_576 -> String.format("%.1f MB", size / 1_048_576.0)
                size >= 1024 -> String.format("%.1f KB", size / 1024.0)
                else -> "$size bytes"
            }
        } ?: "N/A"
    }

    /**
     * Get a formatted display name including version and quantization.
     *
     * @return Formatted name like "Llama-3.2-3B-Instruct v3.2.0 (q4_0)"
     */
    fun displayName(): String {
        val quantSuffix = quantization?.let { " ($it)" } ?: ""
        return "$name v$version$quantSuffix"
    }

    companion object {
        // ============================================
        // Provider Constants
        // ============================================

        /** Locally deployed model */
        const val PROVIDER_LOCAL = "local"

        /** OpenAI API */
        const val PROVIDER_OPENAI = "openai"

        /** Anthropic Claude API */
        const val PROVIDER_ANTHROPIC = "anthropic"

        /** Google AI (Gemini) */
        const val PROVIDER_GOOGLE = "google"

        /** Cohere API */
        const val PROVIDER_COHERE = "cohere"

        /** Hugging Face Inference API */
        const val PROVIDER_HUGGINGFACE = "huggingface"

        /** Ollama local runner */
        const val PROVIDER_OLLAMA = "ollama"

        // ============================================
        // Quantization Constants
        // ============================================

        /** 4-bit quantization (most compressed) */
        const val QUANT_Q4_0 = "q4_0"

        /** 4-bit k-quants medium */
        const val QUANT_Q4_K_M = "q4_k_m"

        /** 5-bit k-quants medium */
        const val QUANT_Q5_K_M = "q5_k_m"

        /** 8-bit quantization */
        const val QUANT_Q8_0 = "q8_0"

        /** 16-bit floating point */
        const val QUANT_FP16 = "fp16"

        /** 32-bit floating point (full precision) */
        const val QUANT_FP32 = "fp32"

        // ============================================
        // Factory Methods
        // ============================================

        /**
         * Create model info for a local GGUF model.
         *
         * @param name Model name
         * @param version Model version
         * @param quantization Quantization format
         * @param parameterCount Number of parameters
         * @param fileSizeBytes File size in bytes
         * @return AIModelInfo configured for local deployment
         */
        fun local(
            name: String,
            version: String,
            quantization: String,
            parameterCount: Long? = null,
            fileSizeBytes: Long? = null
        ): AIModelInfo = AIModelInfo(
            name = name,
            version = version,
            provider = PROVIDER_LOCAL,
            quantization = quantization,
            parameterCount = parameterCount,
            fileSizeBytes = fileSizeBytes
        )

        /**
         * Create model info for an OpenAI model.
         *
         * @param modelId OpenAI model ID (e.g., "gpt-4o", "gpt-4o-mini")
         * @param version API version or model version
         * @return AIModelInfo configured for OpenAI
         */
        fun openai(
            modelId: String,
            version: String = "latest"
        ): AIModelInfo = AIModelInfo(
            name = modelId,
            version = version,
            provider = PROVIDER_OPENAI
        )

        /**
         * Create model info for an Anthropic Claude model.
         *
         * @param modelId Claude model ID (e.g., "claude-3-5-sonnet-20241022")
         * @param version API version or model version
         * @return AIModelInfo configured for Anthropic
         */
        fun anthropic(
            modelId: String,
            version: String = "latest"
        ): AIModelInfo = AIModelInfo(
            name = modelId,
            version = version,
            provider = PROVIDER_ANTHROPIC
        )

        /**
         * Create model info for an Ollama-served model.
         *
         * @param modelName Ollama model name (e.g., "llama3.2:3b")
         * @param version Model version
         * @param parameterCount Number of parameters
         * @return AIModelInfo configured for Ollama
         */
        fun ollama(
            modelName: String,
            version: String = "latest",
            parameterCount: Long? = null
        ): AIModelInfo = AIModelInfo(
            name = modelName,
            version = version,
            provider = PROVIDER_OLLAMA,
            parameterCount = parameterCount
        )
    }
}
