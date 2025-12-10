/**
 * Inference Strategy Interface
 *
 * Single Responsibility: Execute model inference
 *
 * Implementations:
 * - MLCInferenceStrategy: Use MLC LLM runtime
 * - LlamaCppInferenceStrategy: Use llama.cpp backend
 * - OllamaInferenceStrategy: Use Ollama API
 * - MultiProviderInferenceStrategy: Try multiple providers with fallback
 *
 * Created: 2025-10-31
 */

package com.augmentalis.ava.features.llm.alc.interfaces

import com.augmentalis.ava.features.llm.alc.models.InferenceRequest
import com.augmentalis.ava.features.llm.alc.models.InferenceResult

/**
 * Strategy for executing model inference
 */
interface IInferenceStrategy {
    /**
     * Run inference on input tokens
     *
     * @param request Inference request (tokens, cache, config)
     * @return Inference result (logits, updated cache, metadata)
     * @throws InferenceException if inference fails
     */
    suspend fun infer(request: InferenceRequest): InferenceResult

    /**
     * Check if this strategy is available
     *
     * @return true if the inference backend is available (e.g., library loaded)
     */
    fun isAvailable(): Boolean

    /**
     * Get the name of this strategy
     *
     * @return Strategy identifier (e.g., "mlc", "llama.cpp", "ollama")
     */
    fun getName(): String

    /**
     * Get priority for fallback ordering
     *
     * @return Priority (0 = highest, higher numbers = lower priority)
     */
    fun getPriority(): Int
}
