// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/EmbeddingProviderFactory.android.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.embeddings

import android.content.Context

/**
 * Android implementation of EmbeddingProviderFactory
 */
actual object EmbeddingProviderFactory {
    private var context: Context? = null

    /**
     * Initialize with Android context
     *
     * Must be called before creating providers
     */
    fun initialize(context: Context) {
        this.context = context
    }

    actual fun getONNXProvider(modelPath: String): EmbeddingProvider? {
        val ctx = context ?: run {
            println("EmbeddingProviderFactory not initialized. Call initialize(context) first.")
            return null
        }

        return try {
            ONNXEmbeddingProvider(ctx, modelPath)
        } catch (e: Exception) {
            println("Failed to create ONNX provider: ${e.message}")
            null
        }
    }

    /**
     * Create ONNX provider with model ID (reads from settings or uses provided modelId)
     *
     * @param modelId Model ID to use (e.g., "AVA-384-Base-INT8")
     *                If null, will try to read from settings or use default
     */
    fun getONNXProviderWithModelId(modelId: String? = null): EmbeddingProvider? {
        val ctx = context ?: run {
            println("EmbeddingProviderFactory not initialized. Call initialize(context) first.")
            return null
        }

        return try {
            ONNXEmbeddingProvider(
                context = ctx,
                modelPath = null,
                modelId = modelId ?: "AVA-384-Base-INT8"  // Default to recommended model
            )
        } catch (e: Exception) {
            println("Failed to create ONNX provider: ${e.message}")
            null
        }
    }

    actual fun getLocalLLMProvider(): EmbeddingProvider? {
        // TODO Phase 3: Implement Local LLM provider
        return null
    }

    actual fun getCloudProvider(apiKey: String, endpoint: String): EmbeddingProvider? {
        // TODO Phase 3: Implement Cloud provider
        return null
    }
}
