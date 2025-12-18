// filename: Universal/AVA/Features/RAG/src/desktopMain/kotlin/com/augmentalis/ava/features/rag/embeddings/EmbeddingProviderFactory.desktop.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

/**
 * Desktop (JVM) implementation of EmbeddingProviderFactory
 *
 * TODO Phase 2: Implement actual embedding providers
 */
actual object EmbeddingProviderFactory {
    actual fun getONNXProvider(modelPath: String): EmbeddingProvider? {
        // TODO: Implement ONNX provider in Phase 2
        return null
    }

    actual fun getLocalLLMProvider(): EmbeddingProvider? {
        // TODO: Implement Local LLM provider in Phase 2
        return null
    }

    actual fun getCloudProvider(apiKey: String, endpoint: String): EmbeddingProvider? {
        // TODO: Implement Cloud provider in Phase 2
        return null
    }
}
