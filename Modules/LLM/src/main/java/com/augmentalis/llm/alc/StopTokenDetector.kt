/**
 * Stop Token Detector for LLM Generation
 *
 * Detects end-of-sequence (EOS) tokens for different models to properly
 * terminate text generation.
 *
 * Each model has different EOS tokens:
 * - Gemma: token ID 1 (</s>)
 * - Qwen: token ID 151643 (<|im_end|>)
 * - Llama: token ID 2 (</s>)
 * - Phi: token ID 32000 (<|endoftext|>)
 * - Mistral: token ID 2 (</s>)
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.llm.alc

import timber.log.Timber

/**
 * Stop token detector for different LLM models
 */
object StopTokenDetector {

    /**
     * Get stop tokens for a specific model
     *
     * @param modelId Model identifier (e.g., "gemma-2b-it-q4f16_1")
     * @return Set of stop token IDs
     */
    fun getStopTokens(modelId: String): Set<Int> {
        val stopTokens = when {
            modelId.contains("gemma", ignoreCase = true) -> {
                // Gemma uses token ID 1 for </s>
                setOf(1, 2) // Include both just in case
            }
            modelId.contains("qwen", ignoreCase = true) -> {
                // Qwen uses special token <|im_end|>
                setOf(151643, 151645) // <|im_end|>, <|endoftext|>
            }
            modelId.contains("llama", ignoreCase = true) -> {
                // Llama uses token ID 2 for </s>
                setOf(2, 128001, 128009) // </s>, <|end_of_text|>, <|eot_id|>
            }
            modelId.contains("phi", ignoreCase = true) -> {
                // Phi uses <|endoftext|>
                setOf(32000, 32001) // <|endoftext|>, <|end|>
            }
            modelId.contains("mistral", ignoreCase = true) -> {
                // Mistral uses token ID 2 for </s>
                setOf(2)
            }
            else -> {
                // Default: common EOS tokens
                Timber.w("Unknown model: $modelId, using default stop tokens")
                setOf(1, 2) // </s> variants
            }
        }

        Timber.d("Stop tokens for $modelId: $stopTokens")
        return stopTokens
    }

    /**
     * Check if a token is a stop token for the given model
     *
     * @param tokenId Token ID to check
     * @param modelId Model identifier
     * @return true if token is a stop token, false otherwise
     */
    fun isStopToken(tokenId: Int, modelId: String): Boolean {
        return tokenId in getStopTokens(modelId)
    }

    /**
     * Get stop sequences (text patterns) for a model
     *
     * Some models use text patterns as stop sequences in addition to token IDs.
     *
     * @param modelId Model identifier
     * @return List of stop sequence strings
     */
    fun getStopSequences(modelId: String): List<String> {
        return when {
            modelId.contains("gemma", ignoreCase = true) -> {
                listOf("</s>", "<eos>", "<end>")
            }
            modelId.contains("qwen", ignoreCase = true) -> {
                listOf("<|im_end|>", "<|endoftext|>", "Assistant:")
            }
            modelId.contains("llama", ignoreCase = true) -> {
                listOf("</s>", "<|end_of_text|>", "<|eot_id|>")
            }
            modelId.contains("phi", ignoreCase = true) -> {
                listOf("<|endoftext|>", "<|end|>", "<|assistant|>")
            }
            modelId.contains("mistral", ignoreCase = true) -> {
                listOf("</s>", "[/INST]")
            }
            else -> {
                listOf("</s>", "<end>", "<eos>")
            }
        }
    }

    /**
     * Check if text ends with a stop sequence
     *
     * @param text Generated text
     * @param modelId Model identifier
     * @return true if text ends with stop sequence, false otherwise
     */
    fun endsWithStopSequence(text: String, modelId: String): Boolean {
        val stopSequences = getStopSequences(modelId)
        return stopSequences.any { sequence ->
            text.trimEnd().endsWith(sequence)
        }
    }

    /**
     * Remove stop sequences from generated text
     *
     * Cleans up any trailing stop sequences that may have been included
     * in the final output.
     *
     * @param text Generated text
     * @param modelId Model identifier
     * @return Cleaned text
     */
    fun removeStopSequences(text: String, modelId: String): String {
        var cleanedText = text
        val stopSequences = getStopSequences(modelId)

        // Loop until no more sequences can be removed
        var changed = true
        while (changed) {
            changed = false
            for (sequence in stopSequences) {
                val newText = cleanedText.removeSuffix(sequence)
                if (newText != cleanedText) {
                    cleanedText = newText
                    changed = true
                }
            }
        }

        return cleanedText.trimEnd()
    }

    /**
     * Model-specific token information
     */
    data class ModelTokenInfo(
        val modelId: String,
        val eosTokenId: Int,
        val bosTokenId: Int?,
        val padTokenId: Int?,
        val stopSequences: List<String>
    )

    /**
     * Get complete token information for a model
     *
     * @param modelId Model identifier
     * @return Token information
     */
    fun getModelTokenInfo(modelId: String): ModelTokenInfo {
        return when {
            modelId.contains("gemma", ignoreCase = true) -> {
                ModelTokenInfo(
                    modelId = modelId,
                    eosTokenId = 1,
                    bosTokenId = 2,
                    padTokenId = 0,
                    stopSequences = listOf("</s>", "<eos>")
                )
            }
            modelId.contains("qwen", ignoreCase = true) -> {
                ModelTokenInfo(
                    modelId = modelId,
                    eosTokenId = 151643,
                    bosTokenId = 151644,
                    padTokenId = 151643,
                    stopSequences = listOf("<|im_end|>", "<|endoftext|>")
                )
            }
            modelId.contains("llama", ignoreCase = true) -> {
                ModelTokenInfo(
                    modelId = modelId,
                    eosTokenId = 2,
                    bosTokenId = 1,
                    padTokenId = 0,
                    stopSequences = listOf("</s>", "<|end_of_text|>")
                )
            }
            modelId.contains("phi", ignoreCase = true) -> {
                ModelTokenInfo(
                    modelId = modelId,
                    eosTokenId = 32000,
                    bosTokenId = null,
                    padTokenId = 32000,
                    stopSequences = listOf("<|endoftext|>")
                )
            }
            modelId.contains("mistral", ignoreCase = true) -> {
                ModelTokenInfo(
                    modelId = modelId,
                    eosTokenId = 2,
                    bosTokenId = 1,
                    padTokenId = 0,
                    stopSequences = listOf("</s>")
                )
            }
            else -> {
                Timber.w("Unknown model: $modelId, using default token info")
                ModelTokenInfo(
                    modelId = modelId,
                    eosTokenId = 2,
                    bosTokenId = 1,
                    padTokenId = 0,
                    stopSequences = listOf("</s>")
                )
            }
        }
    }

    /**
     * Get maximum generation length for a model
     *
     * Returns recommended max token limits based on model context length.
     *
     * @param modelId Model identifier
     * @return Recommended max tokens
     */
    fun getMaxGenerationLength(modelId: String): Int {
        return when {
            modelId.contains("gemma", ignoreCase = true) -> 2048
            modelId.contains("qwen", ignoreCase = true) -> 2048
            modelId.contains("llama", ignoreCase = true) -> 4096
            modelId.contains("phi", ignoreCase = true) -> 4096
            modelId.contains("mistral", ignoreCase = true) -> 8192
            else -> 2048 // Conservative default
        }
    }

    /**
     * Get recommended sampling config for a model
     *
     * Returns optimal sampling parameters based on model characteristics.
     *
     * @param modelId Model identifier
     * @return Sampling configuration
     */
    fun getRecommendedSamplingConfig(modelId: String): TokenSampler.SamplingConfig {
        return when {
            modelId.contains("phi", ignoreCase = true) -> {
                // Phi benefits from lower temperature (more deterministic)
                TokenSampler.SamplingConfig.PRECISE
            }
            modelId.contains("qwen", ignoreCase = true) -> {
                // Qwen performs well with balanced settings
                TokenSampler.SamplingConfig.BALANCED
            }
            modelId.contains("gemma", ignoreCase = true) -> {
                // Gemma is versatile, use balanced
                TokenSampler.SamplingConfig.BALANCED
            }
            modelId.contains("llama", ignoreCase = true) -> {
                // Llama handles creative prompts well
                TokenSampler.SamplingConfig.BALANCED
            }
            modelId.contains("mistral", ignoreCase = true) -> {
                // Mistral is good at following instructions
                TokenSampler.SamplingConfig.PRECISE
            }
            else -> {
                TokenSampler.SamplingConfig.BALANCED
            }
        }
    }
}
