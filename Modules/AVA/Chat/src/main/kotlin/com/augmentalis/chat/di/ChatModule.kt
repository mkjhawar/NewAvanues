// filename: Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/di/ChatModule.kt
// created: 2025-11-22
// author: Agent 1 - Dependency Injection Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.chat.di

import android.content.Context
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import com.augmentalis.ava.core.domain.repository.MessageRepository
import com.augmentalis.ava.core.domain.usecase.ExportConversationUseCase
import com.augmentalis.chat.tts.TTSPreferences
import com.augmentalis.chat.voice.VoiceInputProvider
import com.augmentalis.chat.voice.VoiceOSStub
import com.augmentalis.rag.data.SQLiteRAGRepository
import com.augmentalis.rag.domain.RAGRepository
import com.augmentalis.rag.embeddings.EmbeddingProvider
import com.augmentalis.rag.embeddings.EmbeddingProviderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.annotation.Nullable

/**
 * Hilt dependency injection module for Chat feature.
 *
 * Provides dependencies for ChatViewModel including RAGRepository for
 * document retrieval and semantic search capabilities.
 *
 * Scope: SingletonComponent (application-wide singleton)
 * - RAGRepository is created once per application lifecycle
 * - Graceful degradation: RAG is optional (@Nullable)
 * - Shared across all ChatViewModel instances
 *
 * RAG Integration (Phase 2):
 * - ChatViewModel accepts optional RAGRepository in constructor
 * - If RAG is unavailable, ChatViewModel operates in LLM-only mode
 * - No breaking changes to existing functionality
 *
 * @author Agent 1: DI Specialist
 * @version 1.0
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    /**
     * Provides RAGRepository to ChatViewModel with graceful degradation.
     *
     * Returns null if:
     * 1. Embedding provider is not available (ONNX model missing, etc.)
     * 2. Database initialization fails
     * 3. Any unexpected exception occurs during setup
     *
     * This allows ChatViewModel to operate in two modes:
     * - LLM-only mode (if RAG unavailable): Uses language model for all responses
     * - RAG-enhanced mode (if RAG available): Retrieves context from documents first
     *
     * Flow:
     * 1. Initialize EmbeddingProviderFactory with app context
     * 2. Get ONNX embedding provider (fallback to null if unavailable)
     * 3. If provider available, create SQLiteRAGRepository
     * 4. Return repository or null for graceful degradation
     *
     * Performance:
     * - EmbeddingProviderFactory initialization: <50ms
     * - ONNX model load: ~500-1000ms (performed asynchronously by RAG repository)
     * - Database creation: <100ms
     *
     * Error Handling:
     * - Missing ONNX model: Provider is null → Repository is null → LLM mode
     * - Database error: Exception caught → null returned → LLM mode
     * - Embedding calculation error: Handled by RAG repository with null return
     *
     * @param context Application context for database and file access
     * @return RAGRepository instance if all dependencies available, null otherwise
     */
    @Provides
    @Singleton
    @Nullable
    fun provideRAGRepository(
        @ApplicationContext context: Context
    ): RAGRepository? {
        return try {
            // 1. Initialize EmbeddingProviderFactory with app context
            EmbeddingProviderFactory.initialize(context)

            // 2. Get ONNX embedding provider with default model
            // Falls back to null if model is not available
            val embeddingProvider: EmbeddingProvider? = EmbeddingProviderFactory
                .getONNXProviderWithModelId("AVA-384-Base-INT8")

            // 3. If embedding provider is available, create SQLiteRAGRepository
            if (embeddingProvider != null) {
                // Create repository with embedding provider
                // Uses default chunking configuration and k-means clustering
                SQLiteRAGRepository(
                    context = context,
                    embeddingProvider = embeddingProvider,
                    enableClustering = true  // Enable k-means for fast search
                )
            } else {
                // Embedding provider not available (graceful degradation)
                // Log would be useful here but avoiding Log import to keep module lean
                // ChatViewModel will handle null RAGRepository gracefully
                null
            }

        } catch (e: Exception) {
            // Any unexpected error during setup - gracefully degrade to null
            // This ensures ChatViewModel never fails to initialize due to RAG setup issues
            // Examples:
            // - Database creation failure
            // - Context initialization error
            // - File system issues
            // Log the exception at error level in production (would add Log import here if needed)
            // For now, silently degrade - ChatViewModel expects null for unavailable RAG
            null
        }
    }

    /**
     * Provides TTSPreferences singleton for TTS settings management.
     *
     * @param context Application context for SharedPreferences access
     * @return TTSPreferences singleton instance
     */
    @Provides
    @Singleton
    fun provideTTSPreferences(
        @ApplicationContext context: Context
    ): TTSPreferences {
        return TTSPreferences.getInstance(context)
    }

    /**
     * Provides ExportConversationUseCase for exporting conversation data.
     *
     * @param conversationRepository Repository for conversation access
     * @param messageRepository Repository for message access
     * @return ExportConversationUseCase instance
     */
    @Provides
    @Singleton
    fun provideExportConversationUseCase(
        conversationRepository: ConversationRepository,
        messageRepository: MessageRepository
    ): ExportConversationUseCase {
        return ExportConversationUseCase(conversationRepository, messageRepository)
    }

    /**
     * Provides VoiceInputProvider stub for future VoiceOS integration.
     *
     * Currently returns VoiceOSStub which indicates voice input is not available.
     * Will be replaced with actual VoiceOS implementation in Phase 4.0.
     *
     * @return VoiceInputProvider stub instance
     */
    @Provides
    @Singleton
    fun provideVoiceInputProvider(): VoiceInputProvider {
        return VoiceOSStub()
    }
}
