/**
 * Streaming Manager Interface
 *
 * Single Responsibility: Manage streaming token generation
 *
 * Implementations:
 * - BackpressureStreamingManager: Streaming with backpressure control
 * - BufferedStreamingManager: Streaming with buffering
 * - MockStreamingManager: For testing
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.interfaces

import com.augmentalis.llm.alc.models.GenerationParams
import com.augmentalis.llm.alc.models.StreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing streaming token generation
 */
interface IStreamingManager {
    /**
     * Stream tokens for a prompt
     *
     * @param prompt Input text prompt
     * @param params Generation parameters (temperature, top-p, max tokens, etc.)
     * @return Flow of stream events (tokens, metadata, errors)
     */
    fun streamGeneration(
        prompt: String,
        params: GenerationParams
    ): Flow<StreamEvent>

    /**
     * Stop the current streaming generation
     */
    suspend fun stopStreaming()

    /**
     * Check if streaming is currently active
     *
     * @return true if a generation is in progress
     */
    fun isStreaming(): Boolean
}
