/**
 * AndroidLlmProcessor.kt - Android LLM Processor Implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Android implementation of ILlmProcessor using LocalLLMProvider from Modules/LLM.
 * Provides natural language fallback when NLU classification fails.
 */
package com.augmentalis.voiceoscoreng.llm

import android.content.Context
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.domain.LLMConfig
import com.augmentalis.llm.domain.GenerationOptions
import com.augmentalis.llm.domain.LLMResponse
import com.augmentalis.llm.domain.getText
import com.augmentalis.ava.core.common.Result as AvaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation of [ILlmProcessor] using LocalLLMProvider.
 *
 * Wraps the LocalLLMProvider from Modules/LLM to provide
 * natural language command interpretation as a fallback.
 *
 * Models are loaded from external storage: /sdcard/ava-ai-models/llm/
 *
 * @param context Android application context
 * @param config LLM configuration
 */
class AndroidLlmProcessor(
    private val context: Context,
    private val config: LlmConfig = LlmConfig.DEFAULT
) : ILlmProcessor {

    /** Thread-safe reference to LLM provider */
    private val llmProviderRef = AtomicReference<LocalLLMProvider?>(null)

    /** Thread-safe initialization state */
    private val initialized = AtomicBoolean(false)

    /** Thread-safe initialization failure tracking */
    private val initializationFailed = AtomicBoolean(false)

    /** Mutex for initialization to prevent concurrent init calls */
    private val initMutex = Mutex()

    override suspend fun initialize(): Result<Unit> = initMutex.withLock {
        withContext(Dispatchers.IO) {
            // Already initialized successfully
            if (initialized.get()) {
                return@withContext Result.success(Unit)
            }

            // Already failed - don't retry
            if (initializationFailed.get()) {
                return@withContext Result.failure(
                    IllegalStateException("LLM initialization previously failed")
                )
            }

            if (!config.enabled) {
                println("[AndroidLlmProcessor] LLM disabled in config")
                return@withContext Result.success(Unit)
            }

            try {
                val provider = LocalLLMProvider(
                    context = context,
                    autoModelSelection = true
                )

                val llmConfig = LLMConfig(
                    modelPath = config.modelBasePath,
                    device = "opencl",  // Use GPU if available
                    maxMemoryMB = 1024  // Conservative memory budget
                )

                val result = provider.initialize(llmConfig)

                when (result) {
                    is AvaResult.Success -> {
                        llmProviderRef.set(provider)
                        initialized.set(true)
                        println("[AndroidLlmProcessor] LLM initialized successfully")
                        Result.success(Unit)
                    }
                    is AvaResult.Error -> {
                        // P1-1 FIX: Return failure so callers know init failed
                        initializationFailed.set(true)
                        println("[AndroidLlmProcessor] LLM initialization failed: ${result.message}")
                        Result.failure(result.exception)
                    }
                }
            } catch (e: Exception) {
                initializationFailed.set(true)
                println("[AndroidLlmProcessor] LLM initialization exception: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult = withContext(Dispatchers.Default) {
        if (!config.enabled) {
            return@withContext LlmResult.NoMatch
        }

        val provider = llmProviderRef.get()
        if (provider == null || !initialized.get()) {
            return@withContext LlmResult.Error("LLM not available")
        }

        if (availableCommands.isEmpty()) {
            return@withContext LlmResult.NoMatch
        }

        // Create prompt for command interpretation
        val prompt = VoiceCommandPrompt.create(utterance, nluSchema, availableCommands)

        try {
            val response = withTimeoutOrNull(config.responseTimeout) {
                val options = GenerationOptions(
                    maxTokens = config.maxTokens,
                    temperature = config.temperature,
                    stopSequences = listOf("\n", ".", "!")
                )

                val fullResponse = StringBuilder()
                val responseFlow = provider.generateResponse(prompt, options)

                responseFlow.collect { llmResponse ->
                    when (llmResponse) {
                        is LLMResponse.Streaming -> {
                            fullResponse.append(llmResponse.chunk)
                        }
                        is LLMResponse.Complete -> {
                            // Use complete text if available
                            if (fullResponse.isEmpty()) {
                                fullResponse.append(llmResponse.fullText)
                            }
                        }
                        is LLMResponse.Error -> {
                            throw RuntimeException(llmResponse.message)
                        }
                    }
                }

                fullResponse.toString()
            }

            if (response == null) {
                println("[AndroidLlmProcessor] LLM response timeout after ${config.responseTimeout}ms")
                return@withContext LlmResult.Error("LLM response timeout")
            }

            // Parse the response to find matched command
            val matchedCommand = VoiceCommandPrompt.parseResponse(response, availableCommands)

            if (matchedCommand != null) {
                println("[AndroidLlmProcessor] Interpreted '$utterance' -> '$matchedCommand'")
                LlmResult.Interpreted(
                    matchedCommand = matchedCommand,
                    confidence = 0.75f, // LLM matches have moderate confidence
                    explanation = "Matched via LLM interpretation"
                )
            } else {
                println("[AndroidLlmProcessor] No match for '$utterance' (response: ${response.take(100)})")
                LlmResult.NoMatch
            }
        } catch (e: Exception) {
            println("[AndroidLlmProcessor] LLM interpretation error: ${e.message}")
            LlmResult.Error(e.message ?: "LLM error")
        }
    }

    override fun isAvailable(): Boolean =
        initialized.get() && config.enabled && llmProviderRef.get() != null

    override fun isModelLoaded(): Boolean =
        llmProviderRef.get() != null && initialized.get()

    /**
     * Check if initialization failed.
     * Useful for distinguishing "not initialized yet" from "failed to initialize".
     */
    fun isInitializationFailed(): Boolean = initializationFailed.get()

    override suspend fun dispose() = withContext(Dispatchers.IO) {
        try {
            llmProviderRef.get()?.cleanup()
        } catch (e: Exception) {
            println("[AndroidLlmProcessor] Cleanup error: ${e.message}")
        }
        llmProviderRef.set(null)
        initialized.set(false)
        initializationFailed.set(false)
    }
}
