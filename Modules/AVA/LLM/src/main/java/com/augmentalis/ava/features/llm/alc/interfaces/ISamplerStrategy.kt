/**
 * Sampler Strategy Interface
 *
 * Single Responsibility: Sample next token from logits
 *
 * Implementations:
 * - TopPSampler: Top-p (nucleus) sampling
 * - TopKSampler: Top-k sampling
 * - GreedySampler: Always pick highest probability
 * - TemperatureSampler: Temperature-based sampling
 * - CombinedSampler: Combine multiple strategies
 *
 * Created: 2025-10-31
 */

package com.augmentalis.ava.features.llm.alc.interfaces

import com.augmentalis.ava.features.llm.alc.models.SamplingParams

/**
 * Strategy for sampling next token from logits
 */
interface ISamplerStrategy {
    /**
     * Sample next token from logits distribution
     *
     * @param logits Probability distribution over vocabulary
     * @param params Sampling parameters (temperature, top-p, top-k, etc.)
     * @return Selected token ID
     */
    fun sample(logits: FloatArray, params: SamplingParams): Int

    /**
     * Get the name of this sampler
     *
     * @return Sampler identifier (e.g., "top-p", "greedy", "temperature")
     */
    fun getName(): String

    /**
     * Check if this sampler supports a given parameter
     *
     * @param paramName Parameter name (e.g., "temperature", "top_p")
     * @return true if the parameter is used by this sampler
     */
    fun supportsParameter(paramName: String): Boolean
}
