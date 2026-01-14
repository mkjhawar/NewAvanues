/**
 * MLC Inference Strategy
 *
 * Single Responsibility: Execute model inference via MLC LLM runtime
 *
 * Handles prefill/decode inference cycles using TVM-based MLC LLM runtime.
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.inference

import com.augmentalis.llm.alc.TVMModule
import com.augmentalis.llm.alc.interfaces.IInferenceStrategy
import com.augmentalis.llm.alc.models.InferenceException
import com.augmentalis.llm.alc.models.InferenceRequest
import com.augmentalis.llm.alc.models.InferenceResult
import timber.log.Timber

/**
 * Inference strategy using MLC LLM runtime
 */
class MLCInferenceStrategy(
    private val model: TVMModule
) : IInferenceStrategy {

    override suspend fun infer(request: InferenceRequest): InferenceResult {
        return try {
            val startTime = System.currentTimeMillis()

            // Execute inference
            val logits = model.forward(request.tokens.toIntArray())

            val duration = System.currentTimeMillis() - startTime
            val tokensPerSecond = if (duration > 0) {
                (request.tokens.size.toFloat() / duration) * 1000f
            } else null

            InferenceResult(
                logits = logits,
                cache = null, // KV cache managed by model internally
                tokensPerSecond = tokensPerSecond,
                metadata = mapOf(
                    "provider" to "mlc",
                    "duration_ms" to duration
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "MLC inference failed")
            throw InferenceException("MLC inference failed: ${e.message}", e)
        }
    }

    override fun isAvailable(): Boolean {
        return true // TODO: Check if MLC runtime is actually available
    }

    override fun getName(): String = "mlc"

    override fun getPriority(): Int = 0 // Highest priority
}
