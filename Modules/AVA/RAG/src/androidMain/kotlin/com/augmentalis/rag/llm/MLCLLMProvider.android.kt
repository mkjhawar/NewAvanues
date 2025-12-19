// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/llm/MLCLLMProvider.android.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.llm

import android.content.Context
import com.augmentalis.rag.chat.LLMProvider
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber

/**
 * Android implementation of RAG LLMProvider using MLC-LLM
 *
 * Bridges RAGChatEngine with AVA's existing LocalLLMProvider (ALC Engine).
 *
 * Architecture:
 * RAGChatEngine → MLCLLMProvider (this) → LocalLLMProvider → ALCEngine → MLC-LLM
 *
 * Performance:
 * - Streaming: Token-by-token generation (~20 tokens/sec on CPU, ~50 tokens/sec on GPU)
 * - Latency: <100ms first token
 * - Memory: ~512MB for Gemma-2b-it
 *
 * Usage:
 * ```kotlin
 * val chatEngine = RAGChatEngine(
 *     ragRepository = repository,
 *     llmProvider = MLCLLMProvider(context),
 *     config = ChatConfig()
 * )
 *
 * chatEngine.ask("How do I reset the device?").collect { response ->
 *     when (response) {
 *         is ChatResponse.Streaming -> displayText(response.text)
 *         is ChatResponse.Complete -> displaySources(response.sources)
 *     }
 * }
 * ```
 */
class MLCLLMProvider(
    private val context: Context,
    private val modelPath: String? = null,
    private val modelId: String = "AVA-GEM-2B-Q4"  // Default: proprietary AVA filename
) : LLMProvider {

    private val localProvider = LocalLLMProvider(context)
    private var isInitialized = false

    // Model ID mapping: AVA proprietary names → original names
    private val modelIdMap = mapOf(
        "AVA-GEM-2B-Q4" to "gemma-2b-it-q4f16_1-android",
        "AVA-PHI-3B-Q4" to "phi-2-q4f16_1-android",
        "AVA-MST-7B-Q4" to "mistral-7b-instruct-v0.2-q4f16_1-android",
        "AVA-TNY-1B-Q4" to "tinyllama-1.1b-chat-q4",
        "AVA-LLM-7B-Q4" to "llama-2-7b-chat-q4f16_1-android"
    )

    /**
     * Initialize MLC-LLM with model
     *
     * @param modelPath Optional path to model file/directory
     *                  Default: /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4.tar
     */
    suspend fun initialize(modelPath: String? = this.modelPath): Result<Unit> {
        return try {
            val actualModelPath = modelPath ?: findModelPath()

            val config = LLMConfig(
                modelPath = actualModelPath
            )

            val result = localProvider.initialize(config)
            if (result is com.augmentalis.ava.core.common.Result.Success) {
                isInitialized = true
                Timber.i("MLCLLMProvider initialized successfully with model: $actualModelPath")
                Result.success(Unit)
            } else {
                val error = (result as com.augmentalis.ava.core.common.Result.Error).exception
                Timber.e(error, "Failed to initialize MLCLLMProvider")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "MLCLLMProvider initialization failed")
            Result.failure(Exception("Failed to initialize MLC-LLM: ${e.message}", e))
        }
    }

    /**
     * Generate response stream from prompt
     *
     * Converts RAG prompt to chat format and streams LLM response.
     * Filters out metadata and returns only text chunks.
     *
     * @param prompt Full prompt with RAG context
     * @return Flow of text chunks (streaming)
     */
    override fun generateStream(prompt: String): Flow<String> {
        if (!isInitialized) {
            Timber.w("MLCLLMProvider not initialized, returning empty flow")
            return kotlinx.coroutines.flow.flow {
                emit("Error: LLM not initialized. Please initialize the provider first.")
            }
        }

        return kotlinx.coroutines.flow.flow {
            try {
                // Convert to chat format
                val messages = listOf(
                    ChatMessage(
                        role = MessageRole.USER,
                        content = prompt
                    )
                )

                val options = GenerationOptions(
                    temperature = 0.7f,
                    maxTokens = 512,
                    topP = 0.95f,
                    stopSequences = listOf("\n\nUser:", "\n\nQuestion:")
                )

                // Stream response from LocalLLMProvider
                localProvider.chat(messages, options).collect { response ->
                    when (response) {
                        is LLMResponse.Streaming -> {
                            // Emit text chunk
                            emit(response.chunk)
                        }
                        is LLMResponse.Complete -> {
                            // Final chunk - do nothing (RAGChatEngine handles completion)
                            Timber.d("LLM generation complete")
                        }
                        is LLMResponse.Error -> {
                            // Emit error as text
                            emit("\n\n[Error: ${response.message}]")
                            Timber.e("LLM error: ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Stream generation failed")
                emit("\n\n[Error: ${e.message}]")
            }
        }
    }

    /**
     * Generate complete response (non-streaming)
     *
     * Collects all streamed chunks into a single string.
     * Use this when you need the full response at once.
     *
     * @param prompt Full prompt with RAG context
     * @return Complete generated text
     */
    override suspend fun generate(prompt: String): String {
        val chunks = mutableListOf<String>()
        generateStream(prompt).collect { chunk ->
            chunks.add(chunk)
        }
        return chunks.joinToString("")
    }

    /**
     * Stop generation
     */
    suspend fun stop() {
        localProvider.stop()
    }

    /**
     * Reset conversation state
     */
    suspend fun reset() {
        localProvider.reset()
    }

    /**
     * Cleanup resources
     */
    suspend fun cleanup() {
        localProvider.cleanup()
        isInitialized = false
    }

    /**
     * Check if currently generating
     */
    fun isGenerating(): Boolean {
        return localProvider.isGenerating()
    }

    /**
     * Get provider info
     */
    fun getInfo(): LLMProviderInfo {
        return localProvider.getInfo()
    }

    /**
     * Find model path with AVA proprietary name fallback
     *
     * Priority:
     * 1. Check for AVA proprietary filename (e.g., AVA.GEM.Q4.tar)
     * 2. Fall back to original filename for backward compatibility
     */
    private fun findModelPath(): String {
        val llmDir = java.io.File(context.getExternalFilesDir(null), "models/llm").apply {
            if (!exists()) {
                mkdirs()
            }
        }

        // Try AVA proprietary filename first
        val avaExtensions = listOf(".tar", ".gguf", ".bin")
        for (ext in avaExtensions) {
            val avaModelFile = java.io.File(llmDir, "$modelId$ext")
            if (avaModelFile.exists()) {
                Timber.i("Found AVA model: ${avaModelFile.absolutePath}")
                return avaModelFile.absolutePath
            }
        }

        // Fall back to original filename for backward compatibility
        val originalModelId = modelIdMap[modelId] ?: modelId
        for (ext in avaExtensions) {
            val originalModelFile = java.io.File(llmDir, "$originalModelId$ext")
            if (originalModelFile.exists()) {
                Timber.i("Found original model: ${originalModelFile.absolutePath}")
                return originalModelFile.absolutePath
            }
        }

        // If not found, return default path with AVA name (will trigger helpful error)
        val defaultPath = java.io.File(llmDir, "$modelId.tar").absolutePath
        Timber.w("Model not found, using default path: $defaultPath")
        return defaultPath
    }
}
