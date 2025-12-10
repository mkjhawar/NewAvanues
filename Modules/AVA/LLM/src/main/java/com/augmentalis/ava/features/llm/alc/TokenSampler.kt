/**
 * Token Sampling Strategies for LLM Generation
 *
 * Provides various sampling methods to select the next token from model logits.
 * Implements temperature scaling, top-p (nucleus), and top-k sampling.
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.ava.features.llm.alc

import timber.log.Timber
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * Token sampler for LLM generation
 *
 * Converts model logits (raw scores) into actual token selections using
 * various strategies to control randomness and quality.
 */
object TokenSampler {

    /**
     * Sample next token from logits using combined strategies
     *
     * @param logits Raw model output scores for each token
     * @param temperature Controls randomness (0.0 = deterministic, 2.0 = very random)
     * @param topP Nucleus sampling threshold (0.0-1.0, typically 0.9)
     * @param topK Only consider top K tokens (typically 40-50)
     * @param repetitionPenalty Penalty for repeating tokens (1.0 = no penalty, 1.2 = slight penalty)
     * @param previousTokens Recently generated tokens for repetition penalty
     * @return Selected token ID
     */
    fun sample(
        logits: FloatArray,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.1f,
        previousTokens: List<Int> = emptyList()
    ): Int {
        require(logits.isNotEmpty()) { "Logits array cannot be empty" }
        require(temperature > 0f) { "Temperature must be positive" }
        require(topP in 0f..1f) { "TopP must be between 0 and 1" }
        require(topK > 0) { "TopK must be positive" }

        try {
            // 1. Apply repetition penalty
            val penalizedLogits = if (repetitionPenalty != 1.0f && previousTokens.isNotEmpty()) {
                applyRepetitionPenalty(logits, previousTokens, repetitionPenalty)
            } else {
                logits
            }

            // 2. Apply temperature scaling
            val scaledLogits = applyTemperature(penalizedLogits, temperature)

            // 3. Convert to probabilities via softmax
            val probabilities = softmax(scaledLogits)

            // 4. Apply top-k filtering
            val topKFiltered = if (topK < probabilities.size) {
                filterTopK(probabilities, topK)
            } else {
                probabilities.withIndex().toList()
            }

            // 5. Apply top-p (nucleus) sampling
            val topPFiltered = if (topP < 1.0f) {
                filterTopP(topKFiltered, topP)
            } else {
                topKFiltered
            }

            // 6. Sample from filtered distribution
            return weightedRandomSample(topPFiltered)

        } catch (e: Exception) {
            Timber.e(e, "Token sampling failed, falling back to greedy")
            // Fallback: greedy sampling
            return logits.indices.maxByOrNull { logits[it] } ?: 0
        }
    }

    /**
     * Greedy sampling - always pick highest probability token
     *
     * Deterministic but can be repetitive. Good for factual responses.
     */
    fun sampleGreedy(logits: FloatArray): Int {
        return logits.indices.maxByOrNull { logits[it] } ?: 0
    }

    /**
     * Apply temperature scaling to logits
     *
     * Temperature controls randomness:
     * - Low (0.1-0.5): More focused, deterministic
     * - Medium (0.7-1.0): Balanced
     * - High (1.5-2.0): More creative, random
     *
     * Formula: logit / temperature
     */
    private fun applyTemperature(logits: FloatArray, temperature: Float): FloatArray {
        if (temperature == 1.0f) return logits
        return FloatArray(logits.size) { logits[it] / temperature }
    }

    /**
     * Apply repetition penalty to discourage repeating tokens
     *
     * Reduces probability of tokens that were recently generated.
     *
     * Formula: logit / penalty (if token in previous tokens)
     */
    private fun applyRepetitionPenalty(
        logits: FloatArray,
        previousTokens: List<Int>,
        penalty: Float
    ): FloatArray {
        val penalized = logits.copyOf()
        val recentTokens = previousTokens.takeLast(20).toSet() // Last 20 tokens

        for (token in recentTokens) {
            if (token in penalized.indices) {
                penalized[token] /= penalty
            }
        }

        return penalized
    }

    /**
     * Softmax function - convert logits to probabilities
     *
     * Ensures all probabilities sum to 1.0
     */
    private fun softmax(logits: FloatArray): FloatArray {
        // Subtract max for numerical stability
        val maxLogit = logits.maxOrNull() ?: 0f
        val expScores = FloatArray(logits.size) { exp((logits[it] - maxLogit).toDouble()).toFloat() }
        val sumExp = expScores.sum()

        return FloatArray(expScores.size) { expScores[it] / sumExp }
    }

    /**
     * Top-K filtering - keep only top K highest probability tokens
     *
     * Reduces distribution to K most likely candidates.
     */
    private fun filterTopK(
        probabilities: FloatArray,
        k: Int
    ): List<IndexedValue<Float>> {
        return probabilities.withIndex()
            .sortedByDescending { it.value }
            .take(k)
    }

    /**
     * Top-P (nucleus) sampling - keep tokens until cumulative probability >= P
     *
     * Dynamically adjusts number of candidates based on probability mass.
     * More robust than top-k for varying distributions.
     *
     * @param candidates List of (index, probability) pairs (should be sorted)
     * @param p Cumulative probability threshold (typically 0.9-0.95)
     */
    private fun filterTopP(
        candidates: List<IndexedValue<Float>>,
        p: Float
    ): List<IndexedValue<Float>> {
        // Sort by probability (descending) if not already
        val sorted = candidates.sortedByDescending { it.value }

        var cumulativeProb = 0f
        val selected = mutableListOf<IndexedValue<Float>>()

        for (candidate in sorted) {
            selected.add(candidate)
            cumulativeProb += candidate.value

            if (cumulativeProb >= p) {
                break
            }
        }

        // Always include at least one token
        if (selected.isEmpty() && sorted.isNotEmpty()) {
            selected.add(sorted.first())
        }

        return selected
    }

    /**
     * Weighted random sampling from filtered distribution
     *
     * Randomly selects token based on probability weights.
     */
    private fun weightedRandomSample(candidates: List<IndexedValue<Float>>): Int {
        if (candidates.isEmpty()) {
            throw IllegalStateException("No candidates for sampling")
        }

        if (candidates.size == 1) {
            return candidates.first().index
        }

        // Renormalize probabilities to sum to 1.0
        val totalProb = candidates.sumOf { it.value.toDouble() }.toFloat()
        val normalizedProbs = candidates.map { it.index to it.value / totalProb }

        // Generate random number in [0, 1)
        val random = Random.nextFloat()

        // Select token based on cumulative probability
        var cumulative = 0f
        for ((index, prob) in normalizedProbs) {
            cumulative += prob
            if (random < cumulative) {
                return index
            }
        }

        // Fallback (should never reach here due to floating point precision)
        return normalizedProbs.last().first
    }

    /**
     * Sampling configuration presets
     */
    data class SamplingConfig(
        val temperature: Float,
        val topP: Float,
        val topK: Int,
        val repetitionPenalty: Float
    ) {
        companion object {
            /**
             * Deterministic, focused responses
             * Good for: factual Q&A, code generation
             */
            val PRECISE = SamplingConfig(
                temperature = 0.3f,
                topP = 0.9f,
                topK = 40,
                repetitionPenalty = 1.1f
            )

            /**
             * Balanced creativity and coherence
             * Good for: general conversation, assistance
             */
            val BALANCED = SamplingConfig(
                temperature = 0.8f,
                topP = 0.95f,
                topK = 50,
                repetitionPenalty = 1.15f
            )

            /**
             * Creative, diverse responses
             * Good for: storytelling, brainstorming
             */
            val CREATIVE = SamplingConfig(
                temperature = 1.2f,
                topP = 0.98f,
                topK = 100,
                repetitionPenalty = 1.2f
            )

            /**
             * Very deterministic, greedy-like
             * Good for: factual extraction, translation
             */
            val GREEDY = SamplingConfig(
                temperature = 0.1f,
                topP = 0.8f,
                topK = 10,
                repetitionPenalty = 1.0f
            )
        }
    }
}
