/**
 * ALC Engine (Adaptive LLM Coordinator) - SOLID Refactored Version
 *
 * Thin orchestrator that delegates to specialized components.
 * Follows SOLID principles for maintainability and testability.
 *
 * Architecture:
 * - Single Responsibility: Only coordinates between components
 * - Open/Closed: Easy to add new providers/strategies without modification
 * - Liskov Substitution: All components are interface-based
 * - Interface Segregation: Small, focused interfaces
 * - Dependency Inversion: Depends on abstractions, not concrete implementations
 *
 * Components:
 * - IModelLoader: Handles model loading/unloading
 * - IInferenceStrategy: Executes model inference with fallback
 * - IStreamingManager: Manages streaming generation
 * - IMemoryManager: Handles memory and KV cache
 * - ISamplerStrategy: Samples tokens from logits
 *
 * Created: 2025-10-31
 * Author: AVA Team
 */

package com.augmentalis.llm.alc

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.llm.alc.interfaces.*
import com.augmentalis.llm.alc.models.*
import com.augmentalis.llm.domain.ChatMessage
import com.augmentalis.llm.domain.LLMResponse
import com.augmentalis.llm.domain.TokenUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Refactored ALC Engine - Thin orchestrator with dependency injection
 *
 * @param context Android context
 * @param modelLoader Component for loading/unloading models
 * @param inferenceStrategy Component for executing inference
 * @param streamingManager Component for streaming generation
 * @param memoryManager Component for memory management
 * @param samplerStrategy Component for token sampling
 * @param dispatcher Coroutine dispatcher for async operations
 */
class ALCEngineSingleLanguage(
    private val context: Context,
    private val modelLoader: IModelLoader,
    private val inferenceStrategy: IInferenceStrategy,
    private val streamingManager: IStreamingManager,
    private val memoryManager: IMemoryManager,
    private val samplerStrategy: ISamplerStrategy,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    // Engine state
    private val engineState = AtomicInteger(EngineState.UNINITIALIZED.ordinal)
    private val mutex = Mutex()

    // Performance tracking
    private var totalTokensGenerated = 0L
    private var totalInferenceTimeMs = 0L

    /**
     * Initialize the engine with a model configuration
     */
    suspend fun initialize(config: ModelConfig): Result<Unit> = mutex.withLock {
        if (engineState.get() != EngineState.UNINITIALIZED.ordinal) {
            return Result.Error(
                message = "Engine already initialized",
                exception = IllegalStateException("Current state: ${getCurrentState()}")
            )
        }

        return try {
            Timber.d("Initializing ALC Engine with model: ${config.modelName}")
            engineState.set(EngineState.INITIALIZING.ordinal)

            // Load model via model loader
            modelLoader.loadModel(config)

            engineState.set(EngineState.READY.ordinal)
            Timber.i("ALC Engine initialized successfully")

            Result.Success(Unit)
        } catch (e: Exception) {
            engineState.set(EngineState.ERROR.ordinal)
            Timber.e(e, "Failed to initialize ALC Engine")
            Result.Error(
                message = "Initialization failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Generate streaming chat response
     *
     * Delegates to StreamingManager and converts events to domain LLMResponse
     */
    fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse> {
        // Validate state
        if (engineState.get() != EngineState.READY.ordinal) {
            return kotlinx.coroutines.flow.flow {
                emit(LLMResponse.Error(
                    message = "Engine not ready. Current state: ${getCurrentState()}",
                    code = "ENGINE_NOT_READY"
                ))
            }
        }

        // Format messages as prompt
        val prompt = formatMessagesAsPrompt(messages)

        // Convert GenerationOptions to GenerationParams
        val params = GenerationParams(
            maxTokens = options.maxTokens ?: 512,
            temperature = options.temperature,
            topP = options.topP,
            topK = 40, // Default
            stopSequences = options.stopSequences,
            repeatPenalty = 1.1f
        )

        // Stream via StreamingManager
        return streamingManager.streamGeneration(prompt, params)
            .map { event -> convertToLLMResponse(event, prompt) }
    }

    /**
     * Convert StreamEvent to LLMResponse (domain model)
     */
    private fun convertToLLMResponse(event: StreamEvent, prompt: String): LLMResponse {
        return when (event) {
            is StreamEvent.TokenGenerated -> {
                totalTokensGenerated++
                LLMResponse.Streaming(
                    chunk = event.text,
                    tokenCount = totalTokensGenerated.toInt()
                )
            }

            is StreamEvent.GenerationComplete -> {
                totalInferenceTimeMs += event.duration
                LLMResponse.Complete(
                    fullText = "", // Accumulated by caller
                    usage = TokenUsage(
                        promptTokens = estimateTokenCount(prompt),
                        completionTokens = event.totalTokens,
                        totalTokens = estimateTokenCount(prompt) + event.totalTokens
                    )
                )
            }

            is StreamEvent.Error -> {
                LLMResponse.Error(
                    message = event.message,
                    code = "GENERATION_ERROR",
                    exception = event.exception
                )
            }

            is StreamEvent.Metadata -> {
                // Skip metadata events in domain model
                LLMResponse.Streaming(chunk = "", tokenCount = 0)
            }
        }
    }

    /**
     * Stop current generation
     */
    suspend fun stop() {
        streamingManager.stopStreaming()
    }

    /**
     * Reset engine state and clear cache
     */
    suspend fun reset() {
        stop()
        memoryManager.resetCache()
        totalTokensGenerated = 0L
        totalInferenceTimeMs = 0L
        Timber.d("Engine state reset")
    }

    /**
     * Clean up resources
     */
    suspend fun cleanup() = mutex.withLock {
        try {
            Timber.d("Cleaning up ALC Engine")
            stop()
            modelLoader.unloadModel()
            memoryManager.resetCache()
            engineState.set(EngineState.UNINITIALIZED.ordinal)
            Timber.i("ALC Engine cleaned up successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
        }
    }

    /**
     * Get current engine state
     */
    fun getCurrentState(): EngineState {
        return EngineState.values()[engineState.get()]
    }

    /**
     * Check if engine is currently generating
     */
    fun isGenerating(): Boolean = streamingManager.isStreaming()

    /**
     * Get performance statistics
     */
    fun getStats(): EngineStats {
        return EngineStats(
            totalTokensGenerated = totalTokensGenerated,
            totalInferenceTimeMs = totalInferenceTimeMs,
            averageTokensPerSecond = if (totalInferenceTimeMs > 0) {
                (totalTokensGenerated * 1000.0 / totalInferenceTimeMs).toFloat()
            } else 0f,
            memoryStats = memoryManager.getCacheStats()
        )
    }

    /**
     * Get memory usage information
     */
    fun getMemoryInfo(): MemoryInfo {
        return MemoryInfo(
            currentUsageBytes = memoryManager.getCurrentMemoryUsage(),
            budgetBytes = memoryManager.getMemoryBudget(),
            utilizationPercent = (memoryManager.getCurrentMemoryUsage().toFloat() /
                                 memoryManager.getMemoryBudget() * 100),
            cacheStats = memoryManager.getCacheStats()
        )
    }

    /**
     * Format chat messages as prompt string
     */
    private fun formatMessagesAsPrompt(messages: List<ChatMessage>): String {
        return buildString {
            for (message in messages) {
                append("<|im_start|>")
                append(message.role.toApiString())
                append("\n")
                append(message.content)
                append("<|im_end|>\n")
            }
            append("<|im_start|>assistant\n")
        }
    }

    /**
     * Estimate token count (rough approximation)
     */
    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }
}

/**
 * Engine state enum
 */
enum class EngineState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    GENERATING,
    ERROR
}

/**
 * Generation options (API surface compatible with original)
 */
data class GenerationOptions(
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val topP: Float = 0.95f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopSequences: List<String> = emptyList()
)

/**
 * Engine performance stats (compatible with original)
 */
data class EngineStats(
    val totalTokensGenerated: Long,
    val totalInferenceTimeMs: Long,
    val averageTokensPerSecond: Float,
    val memoryStats: Map<String, Any> = emptyMap()
)

/**
 * Memory information
 */
data class MemoryInfo(
    val currentUsageBytes: Long,
    val budgetBytes: Long,
    val utilizationPercent: Float,
    val cacheStats: Map<String, Any>
)
