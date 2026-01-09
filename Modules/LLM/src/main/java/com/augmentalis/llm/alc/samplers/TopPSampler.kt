/**
 * Top-P (Nucleus) Sampling Strategy
 *
 * Single Responsibility: Sample tokens using nucleus sampling
 *
 * Implements top-p (nucleus) sampling where we sample from the smallest set
 * of tokens whose cumulative probability exceeds p.
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.samplers

import com.augmentalis.llm.alc.interfaces.ISamplerStrategy
import com.augmentalis.llm.alc.models.SamplingParams
import kotlin.math.exp
import kotlin.random.Random

/**
 * Top-P (nucleus) sampler
 *
 * Samples from the smallest set of tokens whose cumulative probability >= p
 */
class TopPSampler : ISamplerStrategy {
    override fun sample(logits: FloatArray, params: SamplingParams): Int {
        // Handle greedy sampling (temperature = 0)
        if (params.temperature == 0f) {
            return logits.indices.maxByOrNull { logits[it] } ?: 0
        }

        // Apply temperature
        val scaledLogits = if (params.temperature != 1.0f) {
            logits.map { it / params.temperature }.toFloatArray()
        } else {
            logits
        }

        // Softmax to get probabilities
        val expLogits = scaledLogits.map { exp(it.toDouble()).toFloat() }
        val sumExp = expLogits.sum()
        val probs = expLogits.map { it / sumExp }

        // Sort indices by probability (descending)
        val sortedIndices = probs.indices.sortedByDescending { probs[it] }

        // Apply top-p filtering
        var cumProb = 0f
        val topPIndices = mutableListOf<Int>()

        for (idx in sortedIndices) {
            cumProb += probs[idx]
            topPIndices.add(idx)
            if (cumProb >= params.topP) break
        }

        // Sample from filtered distribution
        val filteredProbs = topPIndices.map { probs[it] }
        val filteredSum = filteredProbs.sum()
        val normalizedProbs = filteredProbs.map { it / filteredSum }

        // Multinomial sampling
        val random = Random.nextFloat()
        var cumulative = 0f
        for (i in topPIndices.indices) {
            cumulative += normalizedProbs[i]
            if (random < cumulative) {
                return topPIndices[i]
            }
        }

        // Fallback (should never reach here)
        return topPIndices.lastOrNull() ?: 0
    }

    override fun getName(): String = "top-p"

    override fun supportsParameter(paramName: String): Boolean {
        return paramName in setOf("temperature", "top_p", "topP")
    }
}
