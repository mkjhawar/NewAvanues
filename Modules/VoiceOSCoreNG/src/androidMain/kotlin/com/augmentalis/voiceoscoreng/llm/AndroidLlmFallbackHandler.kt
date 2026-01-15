/**
 * AndroidLlmFallbackHandler.kt - Android LLM Fallback Handler Implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Android implementation of ILlmFallbackHandler using LocalLLMProvider and CloudLLMProvider
 * from Modules/LLM. Implements cascading fallback strategy for low-confidence NLU results.
 */
package com.augmentalis.voiceoscoreng.llm

import android.content.Context
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.provider.CloudLLMProvider
import com.augmentalis.llm.domain.LLMConfig
import com.augmentalis.llm.domain.LLMResponse
import com.augmentalis.llm.domain.GenerationOptions
import com.augmentalis.llm.domain.ChatMessage
import com.augmentalis.llm.domain.MessageRole
import com.augmentalis.llm.security.ApiKeyManager
import com.augmentalis.voiceoscoreng.nlu.NluResult
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
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
 * Android implementation of [ILlmFallbackHandler].
 *
 * Uses LocalLLMProvider for on-device inference (privacy-first) with
 * CloudLLMProvider as a fallback when local fails or is unavailable.
 *
 * @param context Android application context
 * @param config Fallback configuration
 */
class AndroidLlmFallbackHandler(
    private val context: Context,
    private var config: FallbackConfig = FallbackConfig.DEFAULT
) : ILlmFallbackHandler {

    /** Thread-safe reference to local LLM provider */
    private val localProviderRef = AtomicReference<LocalLLMProvider?>(null)

    /** Thread-safe reference to cloud LLM provider */
    private val cloudProviderRef = AtomicReference<CloudLLMProvider?>(null)

    /** Thread-safe initialization state for local */
    private val localInitialized = AtomicBoolean(false)

    /** Thread-safe initialization state for cloud */
    private val cloudInitialized = AtomicBoolean(false)

    /** Mutex for initialization to prevent concurrent init calls */
    private val initMutex = Mutex()

    /** API key manager for cloud providers */
    private val apiKeyManager by lazy { ApiKeyManager(context) }

    override suspend fun initialize(): Result<Unit> = initMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!config.enabled) {
                println("[AndroidLlmFallbackHandler] Fallback disabled in config")
                return@withContext Result.success(Unit)
            }

            val errors = mutableListOf<String>()

            // Initialize local LLM provider
            if (config.tryLocalFirst || !config.allowCloudFallback) {
                try {
                    val localProvider = LocalLLMProvider(
                        context = context,
                        autoModelSelection = true
                    )

                    val llmConfig = LLMConfig(
                        modelPath = "/sdcard/ava-ai-models/llm",
                        device = "opencl",
                        maxMemoryMB = 1024
                    )

                    val result = localProvider.initialize(llmConfig)

                    if (result.isSuccess) {
                        localProviderRef.set(localProvider)
                        localInitialized.set(true)
                        println("[AndroidLlmFallbackHandler] Local LLM initialized successfully")
                    } else {
                        val errorMsg = try {
                            result.getOrThrow()
                            "Unknown error"
                        } catch (e: Throwable) {
                            e.message ?: "Local LLM initialization failed"
                        }
                        errors.add("Local: $errorMsg")
                        println("[AndroidLlmFallbackHandler] Local LLM initialization failed: $errorMsg")
                    }
                } catch (e: Exception) {
                    errors.add("Local: ${e.message}")
                    println("[AndroidLlmFallbackHandler] Local LLM exception: ${e.message}")
                }
            }

            // Initialize cloud LLM provider if allowed
            if (config.allowCloudFallback) {
                try {
                    val cloudProvider = CloudLLMProvider(
                        context = context,
                        apiKeyManager = apiKeyManager
                    )

                    val cloudConfig = LLMConfig(
                        modelPath = "auto" // Auto-selects best available provider
                    )

                    val result = cloudProvider.initialize(cloudConfig)

                    if (result.isSuccess) {
                        cloudProviderRef.set(cloudProvider)
                        cloudInitialized.set(true)
                        println("[AndroidLlmFallbackHandler] Cloud LLM initialized successfully")
                    } else {
                        val errorMsg = try {
                            result.getOrThrow()
                            "Unknown error"
                        } catch (e: Throwable) {
                            e.message ?: "Cloud LLM initialization failed"
                        }
                        errors.add("Cloud: $errorMsg")
                        println("[AndroidLlmFallbackHandler] Cloud LLM initialization failed: $errorMsg")
                    }
                } catch (e: Exception) {
                    errors.add("Cloud: ${e.message}")
                    println("[AndroidLlmFallbackHandler] Cloud LLM exception: ${e.message}")
                }
            }

            // Success if at least one provider is available
            if (localInitialized.get() || cloudInitialized.get()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(
                    "No LLM providers available. Errors: ${errors.joinToString("; ")}"
                ))
            }
        }
    }

    override suspend fun handleLowConfidence(
        utterance: String,
        nluResult: NluResult,
        candidateCommands: List<QuantizedCommand>
    ): FallbackResult = withContext(Dispatchers.Default) {
        if (!config.enabled) {
            return@withContext FallbackResult.NoMatch(
                reason = "Fallback disabled",
                attemptedProviders = emptyList()
            )
        }

        if (candidateCommands.isEmpty()) {
            return@withContext FallbackResult.NoMatch(
                reason = "No candidate commands available",
                attemptedProviders = emptyList()
            )
        }

        val attemptedProviders = mutableListOf<String>()

        // Extract confidence from NLU result for logging
        val nluConfidence = when (nluResult) {
            is NluResult.Match -> nluResult.confidence
            else -> 0f
        }
        println("[AndroidLlmFallbackHandler] Processing low-confidence NLU result: $nluConfidence for '$utterance'")

        // Build prompt with candidate commands
        val commandPhrases = candidateCommands
            .take(config.maxCandidates)
            .map { it.phrase }
        val prompt = buildClarificationPrompt(utterance, commandPhrases)

        // Try local LLM first if configured
        if (config.tryLocalFirst && localInitialized.get()) {
            attemptedProviders.add("local")
            val localResult = tryLocalLlm(prompt, commandPhrases, candidateCommands)
            if (localResult != null) {
                return@withContext localResult
            }
            println("[AndroidLlmFallbackHandler] Local LLM did not return a match, trying cloud")
        }

        // Fall back to cloud if allowed
        if (config.allowCloudFallback && cloudInitialized.get()) {
            attemptedProviders.add("cloud")
            val cloudResult = tryCloudLlm(prompt, commandPhrases, candidateCommands)
            if (cloudResult != null) {
                return@withContext cloudResult
            }
        }

        FallbackResult.NoMatch(
            reason = "Neither local nor cloud LLM could clarify the command",
            attemptedProviders = attemptedProviders
        )
    }

    override suspend fun handleAmbiguous(
        utterance: String,
        ambiguousResult: NluResult.Ambiguous
    ): FallbackResult = withContext(Dispatchers.Default) {
        if (!config.enabled) {
            return@withContext FallbackResult.NoMatch(
                reason = "Fallback disabled",
                attemptedProviders = emptyList()
            )
        }

        val candidates = ambiguousResult.candidates
        if (candidates.isEmpty()) {
            return@withContext FallbackResult.NoMatch(
                reason = "No candidates in ambiguous result",
                attemptedProviders = emptyList()
            )
        }

        println("[AndroidLlmFallbackHandler] Disambiguating ${candidates.size} candidates for '$utterance'")

        val attemptedProviders = mutableListOf<String>()

        // Build disambiguation prompt
        val candidateInfo = candidates.map { (cmd, score) ->
            "${cmd.phrase} (confidence: ${String.format("%.2f", score)})"
        }
        val prompt = buildDisambiguationPrompt(utterance, candidateInfo)
        val commandPhrases = candidates.map { it.first.phrase }
        val candidateCommands = candidates.map { it.first }

        // Try local LLM first
        if (config.tryLocalFirst && localInitialized.get()) {
            attemptedProviders.add("local")
            val localResult = tryLocalLlm(prompt, commandPhrases, candidateCommands)
            if (localResult != null) {
                return@withContext localResult
            }
        }

        // Fall back to cloud
        if (config.allowCloudFallback && cloudInitialized.get()) {
            attemptedProviders.add("cloud")
            val cloudResult = tryCloudLlm(prompt, commandPhrases, candidateCommands)
            if (cloudResult != null) {
                return@withContext cloudResult
            }
        }

        // If LLM can't decide, return the highest confidence candidate
        val bestCandidate = candidates.maxByOrNull { it.second }
        if (bestCandidate != null) {
            return@withContext FallbackResult.Clarified(
                command = bestCandidate.first,
                confidence = bestCandidate.second * 0.9f, // Slight penalty for ambiguity
                source = "heuristic",
                explanation = "Selected highest confidence candidate after LLM disambiguation failed"
            )
        }

        FallbackResult.NoMatch(
            reason = "Could not disambiguate candidates",
            attemptedProviders = attemptedProviders
        )
    }

    override suspend fun generateAction(
        utterance: String,
        context: String?
    ): FallbackResult = withContext(Dispatchers.Default) {
        if (!config.enabled) {
            return@withContext FallbackResult.NoMatch(
                reason = "Fallback disabled",
                attemptedProviders = emptyList()
            )
        }

        println("[AndroidLlmFallbackHandler] Generating action for novel request: '$utterance'")

        val attemptedProviders = mutableListOf<String>()
        val prompt = buildActionGenerationPrompt(utterance, context)

        // Try local LLM first
        if (config.tryLocalFirst && localInitialized.get()) {
            attemptedProviders.add("local")
            val localResult = tryGenerateAction(prompt, "local", config.localTimeout)
            if (localResult != null) {
                return@withContext localResult
            }
        }

        // Fall back to cloud
        if (config.allowCloudFallback && cloudInitialized.get()) {
            attemptedProviders.add("cloud")
            val cloudResult = tryGenerateAction(prompt, "cloud", config.cloudTimeout)
            if (cloudResult != null) {
                return@withContext cloudResult
            }
        }

        FallbackResult.NoMatch(
            reason = "Could not generate action for utterance",
            attemptedProviders = attemptedProviders
        )
    }

    override fun isLocalAvailable(): Boolean = localInitialized.get()

    override fun isCloudAvailable(): Boolean = cloudInitialized.get()

    override fun getConfig(): FallbackConfig = config

    override fun setConfig(config: FallbackConfig) {
        this.config = config
    }

    override suspend fun dispose() = withContext(Dispatchers.IO) {
        try {
            localProviderRef.get()?.cleanup()
        } catch (e: Exception) {
            println("[AndroidLlmFallbackHandler] Local cleanup error: ${e.message}")
        }

        try {
            cloudProviderRef.get()?.cleanup()
        } catch (e: Exception) {
            println("[AndroidLlmFallbackHandler] Cloud cleanup error: ${e.message}")
        }

        localProviderRef.set(null)
        cloudProviderRef.set(null)
        localInitialized.set(false)
        cloudInitialized.set(false)
    }

    // ==================== Private Helpers ====================

    /**
     * Try to get a clarification from local LLM.
     */
    private suspend fun tryLocalLlm(
        prompt: String,
        commandPhrases: List<String>,
        candidateCommands: List<QuantizedCommand>
    ): FallbackResult.Clarified? {
        val provider = localProviderRef.get() ?: return null

        return try {
            val response = withTimeoutOrNull(config.localTimeout) {
                executeGeneration(provider, prompt)
            }

            if (response != null) {
                parseAndMatchCommand(response, commandPhrases, candidateCommands, "local")
            } else {
                println("[AndroidLlmFallbackHandler] Local LLM timeout")
                null
            }
        } catch (e: Exception) {
            println("[AndroidLlmFallbackHandler] Local LLM error: ${e.message}")
            null
        }
    }

    /**
     * Try to get a clarification from cloud LLM.
     */
    private suspend fun tryCloudLlm(
        prompt: String,
        commandPhrases: List<String>,
        candidateCommands: List<QuantizedCommand>
    ): FallbackResult.Clarified? {
        val provider = cloudProviderRef.get() ?: return null

        return try {
            val response = withTimeoutOrNull(config.cloudTimeout) {
                executeCloudGeneration(provider, prompt)
            }

            if (response != null) {
                parseAndMatchCommand(response, commandPhrases, candidateCommands, "cloud")
            } else {
                println("[AndroidLlmFallbackHandler] Cloud LLM timeout")
                null
            }
        } catch (e: Exception) {
            println("[AndroidLlmFallbackHandler] Cloud LLM error: ${e.message}")
            null
        }
    }

    /**
     * Execute generation on local LLM provider.
     */
    private suspend fun executeGeneration(
        provider: LocalLLMProvider,
        prompt: String
    ): String {
        val options = GenerationOptions(
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            stopSequences = listOf("\n", ".", "!")
        )

        val responseBuilder = StringBuilder()
        provider.generateResponse(prompt, options).collect { llmResponse ->
            when (llmResponse) {
                is LLMResponse.Streaming -> responseBuilder.append(llmResponse.chunk)
                is LLMResponse.Complete -> {
                    if (responseBuilder.isEmpty()) {
                        responseBuilder.append(llmResponse.fullText)
                    }
                }
                is LLMResponse.Error -> throw RuntimeException(llmResponse.message)
            }
        }

        return responseBuilder.toString()
    }

    /**
     * Execute generation on cloud LLM provider.
     */
    private suspend fun executeCloudGeneration(
        provider: CloudLLMProvider,
        prompt: String
    ): String {
        val options = GenerationOptions(
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            stopSequences = listOf("\n", ".", "!")
        )

        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = prompt)
        )

        val responseBuilder = StringBuilder()
        provider.chat(messages, options).collect { llmResponse ->
            when (llmResponse) {
                is LLMResponse.Streaming -> responseBuilder.append(llmResponse.chunk)
                is LLMResponse.Complete -> {
                    if (responseBuilder.isEmpty()) {
                        responseBuilder.append(llmResponse.fullText)
                    }
                }
                is LLMResponse.Error -> throw RuntimeException(llmResponse.message)
            }
        }

        return responseBuilder.toString()
    }

    /**
     * Parse LLM response and match to command.
     */
    private fun parseAndMatchCommand(
        response: String,
        commandPhrases: List<String>,
        candidateCommands: List<QuantizedCommand>,
        source: String
    ): FallbackResult.Clarified? {
        val matchedPhrase = VoiceCommandPrompt.parseResponse(response, commandPhrases)

        if (matchedPhrase != null) {
            val command = candidateCommands.find { it.phrase.equals(matchedPhrase, ignoreCase = true) }
            if (command != null) {
                println("[AndroidLlmFallbackHandler] $source LLM matched: '${command.phrase}'")
                return FallbackResult.Clarified(
                    command = command,
                    confidence = if (source == "local") 0.75f else 0.8f,
                    source = source,
                    explanation = "Matched via $source LLM interpretation"
                )
            }
        }

        return null
    }

    /**
     * Try to generate an action from the LLM response.
     */
    private suspend fun tryGenerateAction(
        prompt: String,
        source: String,
        timeout: Long
    ): FallbackResult.GeneratedAction? {
        val provider = if (source == "local") localProviderRef.get() else cloudProviderRef.get()
        if (provider == null) return null

        return try {
            val response = withTimeoutOrNull(timeout) {
                if (source == "local") {
                    executeGeneration(provider as LocalLLMProvider, prompt)
                } else {
                    executeCloudGeneration(provider as CloudLLMProvider, prompt)
                }
            }

            if (response != null) {
                parseActionResponse(response, source)
            } else {
                null
            }
        } catch (e: Exception) {
            println("[AndroidLlmFallbackHandler] $source action generation error: ${e.message}")
            null
        }
    }

    /**
     * Parse action generation response.
     */
    private fun parseActionResponse(response: String, source: String): FallbackResult.GeneratedAction? {
        // Parse JSON-like response from LLM
        // Expected format: ACTION: action_type | PARAMS: key=value,key=value
        val trimmed = response.trim()

        // Look for ACTION: pattern
        val actionMatch = Regex("""ACTION:\s*(\w+)""", RegexOption.IGNORE_CASE).find(trimmed)
        val actionType = actionMatch?.groupValues?.getOrNull(1)

        if (actionType == null || actionType.equals("none", ignoreCase = true)) {
            return null
        }

        // Parse parameters
        val paramsMatch = Regex("""PARAMS:\s*(.+)""", RegexOption.IGNORE_CASE).find(trimmed)
        val paramsStr = paramsMatch?.groupValues?.getOrNull(1) ?: ""
        val parameters = paramsStr.split(",")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else null
            }
            .toMap()

        return FallbackResult.GeneratedAction(
            actionType = actionType,
            parameters = parameters,
            source = source,
            rawResponse = trimmed
        )
    }

    // ==================== Prompt Builders ====================

    /**
     * Build prompt for command clarification.
     */
    private fun buildClarificationPrompt(utterance: String, commands: List<String>): String = buildString {
        appendLine("You are a voice command interpreter for an accessibility app.")
        appendLine("The user said: \"$utterance\"")
        appendLine()
        appendLine("Available commands:")
        commands.forEach { appendLine("- $it") }
        appendLine()
        appendLine("Match the user input to the most appropriate command.")
        appendLine("Consider synonyms, paraphrases, and natural language variations.")
        appendLine("Respond with ONLY the exact command phrase, nothing else.")
        appendLine("If no command matches, respond with: NO_MATCH")
        appendLine()
        append("Response: ")
    }

    /**
     * Build prompt for disambiguation.
     */
    private fun buildDisambiguationPrompt(utterance: String, candidates: List<String>): String = buildString {
        appendLine("You are a voice command interpreter.")
        appendLine("The user said: \"$utterance\"")
        appendLine()
        appendLine("Multiple commands could match. Choose the BEST one:")
        candidates.forEach { appendLine("- $it") }
        appendLine()
        appendLine("Select the command that best matches the user's intent.")
        appendLine("Respond with ONLY the exact command phrase (without the confidence score).")
        appendLine()
        append("Best match: ")
    }

    /**
     * Build prompt for action generation.
     */
    private fun buildActionGenerationPrompt(utterance: String, context: String?): String = buildString {
        appendLine("You are an AI assistant for a voice-controlled accessibility app.")
        appendLine("The user said: \"$utterance\"")
        if (!context.isNullOrBlank()) {
            appendLine()
            appendLine("Current context: $context")
        }
        appendLine()
        appendLine("Determine what action the user wants to perform.")
        appendLine("Respond in this format:")
        appendLine("ACTION: action_type")
        appendLine("PARAMS: key1=value1,key2=value2")
        appendLine()
        appendLine("Action types: tap, scroll, navigate, open_app, type_text, search, settings, none")
        appendLine("If you cannot determine an action, respond with: ACTION: none")
        appendLine()
        append("Response: ")
    }
}
