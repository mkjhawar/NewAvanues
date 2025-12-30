// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/integration/ChatConnector.kt
// created: 2025-11-01 23:40:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 3 - Integration Layer
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.integration

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.llm.response.IntentTemplates
import com.augmentalis.llm.domain.*
import com.augmentalis.llm.provider.LocalLLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Connector to features:chat module for AI response generation.
 *
 * Wraps the LLM feature and provides async interface for generating
 * AI responses based on user input and classified intent.
 *
 * Integration:
 * - Uses LocalLLMProvider for on-device inference with streaming
 * - Falls back to IntentTemplates if LLM unavailable
 * - Returns formatted response text for overlay display
 *
 * @param context Android context
 * @author Manoj Jhawar
 */
class ChatConnector(private val context: Context) {

    private val llmProvider: LocalLLMProvider by lazy {
        LocalLLMProvider(context)
    }

    private var llmInitialized = false

    /**
     * Ensure LLM is initialized
     *
     * Attempts to initialize once, then uses cached result
     */
    private suspend fun ensureLlmInitialized(): Boolean {
        if (llmInitialized) return true

        // TODO: Get actual model path from ModelDownloadManager
        // For now, use placeholder path
        val modelPath = "${context.filesDir}/models/gemma-2b-it"

        val config = LLMConfig(
            modelPath = modelPath
        )

        return when (llmProvider.initialize(config)) {
            is Result.Success -> {
                llmInitialized = true
                Timber.i("LLM initialized successfully for overlay")
                true
            }
            is Result.Error -> {
                Timber.w("LLM initialization failed, will use fallback templates")
                false
            }
        }
    }

    /**
     * Generate AI response for user input
     *
     * @param text User voice input
     * @param intent Classified intent category
     * @param entities Extracted entities from user input (optional)
     * @return AI-generated response text
     */
    suspend fun generateResponse(
        text: String,
        intent: String,
        entities: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        if (ensureLlmInitialized()) {
            // Use LLM for generation with context
            val fullResponse = StringBuilder()
            var completeText: String? = null

            try {
                // Build context-aware prompt
                val contextualPrompt = buildContextualPrompt(text, intent, entities)

                llmProvider.generateResponse(contextualPrompt)
                    .collect { response ->
                        when (response) {
                            is LLMResponse.Streaming -> {
                                fullResponse.append(response.chunk)
                            }
                            is LLMResponse.Complete -> {
                                completeText = response.fullText
                            }
                            is LLMResponse.Error -> {
                                Timber.e("LLM error: ${response.message}")
                                // Fall through to template response
                            }
                        }
                    }

                // Return complete text if available, otherwise accumulated chunks
                completeText?.let { return@withContext it }
                if (fullResponse.isNotEmpty()) {
                    return@withContext fullResponse.toString()
                }
            } catch (e: Exception) {
                Timber.e(e, "LLM generation failed")
                // Fall through to template response
            }
        }

        // Fallback to template-based responses
        delay(800) // Simulate processing
        return@withContext IntentTemplates.getResponse(intent)
    }

    /**
     * Build contextual prompt with intent and entities
     */
    private fun buildContextualPrompt(
        text: String,
        intent: String,
        entities: Map<String, String>
    ): String {
        if (entities.isEmpty()) return text

        val context = StringBuilder()
        context.append("User intent: $intent\n")

        if (entities.isNotEmpty()) {
            context.append("Extracted entities:\n")
            entities.forEach { (key, value) ->
                context.append("- $key: $value\n")
            }
        }

        context.append("\nUser query: $text\n\n")
        context.append("Provide a helpful, context-aware response:")

        return context.toString()
    }

    /**
     * Generate streaming response with real-time chunks
     *
     * Uses LocalLLMProvider for true streaming inference.
     * Falls back to template response if LLM unavailable.
     *
     * @param text User input
     * @param intent Classified intent
     * @param onChunk Callback for each response chunk (called on IO dispatcher)
     */
    suspend fun generateStreamingResponse(
        text: String,
        intent: String,
        onChunk: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (ensureLlmInitialized()) {
            // Use LLM for streaming generation
            try {
                llmProvider.generateResponse(text)
                    .onEach { response ->
                        when (response) {
                            is LLMResponse.Streaming -> {
                                onChunk(response.chunk)
                            }
                            is LLMResponse.Complete -> {
                                // Final response, could emit full text if needed
                                Timber.d("Generation complete: ${response.usage.totalTokens} tokens")
                            }
                            is LLMResponse.Error -> {
                                Timber.e("LLM error during streaming: ${response.message}")
                                // Could emit error to callback here
                            }
                        }
                    }
                    .catch { e ->
                        Timber.e(e, "Exception during streaming")
                        // Fallback to template
                        delay(800)
                        onChunk(IntentTemplates.getResponse(intent))
                    }
                    .collect()

                return@withContext
            } catch (e: Exception) {
                Timber.e(e, "LLM streaming failed")
                // Fall through to template response
            }
        }

        // Fallback to template-based response (non-streaming)
        delay(800)
        val response = IntentTemplates.getResponse(intent)
        onChunk(response)
    }

    /**
     * Cleanup LLM resources
     *
     * Call when overlay is destroyed or LLM no longer needed
     */
    suspend fun cleanup() {
        if (llmInitialized) {
            llmProvider.cleanup()
            llmInitialized = false
        }
    }
}
