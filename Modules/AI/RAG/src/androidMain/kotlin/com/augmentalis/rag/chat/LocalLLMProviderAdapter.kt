// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/chat/LocalLLMProviderAdapter.kt
// created: 2025-11-15
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.chat

import com.augmentalis.llm.provider.LocalLLMProvider as LLMLocalProvider
import com.augmentalis.llm.GenerationOptions
import com.augmentalis.llm.LLMResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import timber.log.Timber

/**
 * Adapter that bridges RAGChatEngine's LLMProvider interface with LocalLLMProvider
 *
 * The RAG module defines its own lightweight LLMProvider interface for chat,
 * while the LLM module has a comprehensive LLMProvider interface with many features.
 * This adapter makes LocalLLMProvider compatible with RAGChatEngine's expectations.
 *
 * Architecture:
 * ```
 * RAGChatEngine → LLMProvider (RAG interface) → LocalLLMProviderAdapter
 *                                                       ↓
 *                                               LocalLLMProvider (LLM interface)
 *                                                       ↓
 *                                               ALCEngine → MLC-LLM
 * ```
 *
 * Created: 2025-11-15
 * Part of: RAG Phase 4 - LLM Integration
 */
class LocalLLMProviderAdapter(
    private val localLLMProvider: LLMLocalProvider
) : LLMProvider {

    /**
     * Generate response stream from prompt
     *
     * Delegates to LocalLLMProvider.generateResponse() and maps the Flow<LLMResponse>
     * to Flow<String> by extracting text chunks.
     *
     * @param prompt Full prompt with RAG context
     * @return Flow of text chunks
     */
    override fun generateStream(prompt: String): Flow<String> {
        Timber.d("LocalLLMProviderAdapter.generateStream() called with ${prompt.length} char prompt")

        return kotlinx.coroutines.flow.flow {
            try {
                // Call LocalLLMProvider with default generation options
                localLLMProvider.generateResponse(
                    prompt = prompt,
                    options = GenerationOptions(
                        temperature = 0.7f,       // Balanced creativity
                        maxTokens = 2048,         // Allow full responses
                        topP = 0.95f,             // Nucleus sampling
                        stopSequences = listOf(
                            "\n\nUser:",          // Stop if conversation turn detected
                            "\n\nQuestion:",      // Stop if new question detected
                        )
                    )
                ).collect { response ->
                    when (response) {
                        is LLMResponse.Streaming -> {
                            // Streaming text chunk - emit it
                            emit(response.chunk)
                        }

                        is LLMResponse.Complete -> {
                            // Generation complete - log metrics
                            Timber.d("Generation complete: ${response.usage.completionTokens} tokens, ${response.usage.totalTokens} total")
                            // Don't emit - RAGChatEngine handles completion via flow end
                        }

                        is LLMResponse.Error -> {
                            // Error occurred - log and throw
                            Timber.e("LocalLLMProvider error: ${response.message} (code: ${response.code})")
                            throw IllegalStateException("LLM generation failed: ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "LocalLLMProviderAdapter.generateStream() failed")
                throw e
            }
        }
    }

    /**
     * Generate complete response (non-streaming)
     *
     * Collects all text chunks from generateStream() and concatenates them.
     *
     * @param prompt Full prompt with RAG context
     * @return Complete generated text
     */
    override suspend fun generate(prompt: String): String {
        Timber.d("LocalLLMProviderAdapter.generate() called (non-streaming)")

        val textBuilder = StringBuilder()

        generateStream(prompt).collect { chunk ->
            textBuilder.append(chunk)
        }

        return textBuilder.toString()
    }
}
