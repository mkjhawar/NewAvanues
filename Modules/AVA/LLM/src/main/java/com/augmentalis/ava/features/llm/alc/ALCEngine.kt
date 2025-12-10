/**
 * ALC Engine (Adaptive LLM Coordinator)
 *
 * Main engine for AVA's on-device LLM inference with multilingual support.
 *
 * MULTILINGUAL STRATEGY:
 * - Phase 1: MobileBERT (25 MB, English-only) - Current
 * - Phase 2: mALBERT (82 MB, 52 languages) - Planned (see docs/active/Analysis-Multilingual-NLU-Options-251031-0030.md)
 * - English + 1 optional language at a time (per user choice)
 * - Automatic model switching when language changes
 * - Lazy loading of language-specific LLM models
 * - Memory-efficient (only one LLM loaded at a time)
 *
 * Architecture:
 * - NLU: mALBERT multilingual (handles all languages, always loaded)
 * - LLMs: Language-specific models (loaded on demand)
 * - Switching: ~5 second delay to unload old model, load new
 *
 * Created: 2025-10-31
 * Author: AVA Team
 */

package com.augmentalis.ava.features.llm.alc

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.llm.alc.interfaces.*
import com.augmentalis.ava.features.llm.alc.language.LanguagePackManager
import com.augmentalis.ava.features.llm.alc.loader.TVMModelLoader
import com.augmentalis.ava.features.llm.alc.models.ModelConfig
import com.augmentalis.ava.features.llm.domain.ChatMessage
import com.augmentalis.ava.features.llm.domain.LLMResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Main ALC Engine with multilingual support
 *
 * Manages language-specific models and provides seamless switching between languages
 */
class ALCEngine(
    private val context: Context,
    private val languagePackManager: LanguagePackManager,
    private val inferenceStrategy: IInferenceStrategy,
    private val streamingManager: IStreamingManager,
    private val memoryManager: IMemoryManager,
    private val samplerStrategy: ISamplerStrategy,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val mutex = Mutex()

    // Current active language and model
    private var currentLanguage = "en"
    private var currentModelLoader: IModelLoader? = null
    private var currentEngine: ALCEngineSingleLanguage? = null

    /**
     * Initialize with default language (English)
     */
    suspend fun initialize(): Result<Unit> {
        return switchLanguage("en")
    }

    /**
     * Switch to a different language
     *
     * @param languageCode ISO 639-1 code (e.g., "es", "fr", "ja")
     */
    suspend fun switchLanguage(languageCode: String): Result<Unit> = mutex.withLock {
        try {
            Timber.d("Switching to language: $languageCode")

            // Check if language pack is installed
            if (!languagePackManager.isLanguageInstalled(languageCode)) {
                return Result.Error(
                    message = "Language pack not installed: $languageCode",
                    exception = LanguagePackNotInstalledException(languageCode)
                )
            }

            // Don't switch if already active
            if (languageCode == currentLanguage && currentEngine != null) {
                Timber.d("Language $languageCode already active")
                return Result.Success(Unit)
            }

            // Clean up current model
            currentEngine?.cleanup()
            currentModelLoader?.unloadModel()
            memoryManager.resetCache()

            // Create new model loader for this language
            val modelConfig = getModelConfig(languageCode)
            val modelLoader = TVMModelLoader(context)

            // Create new engine instance
            val engine = ALCEngineSingleLanguage(
                context = context,
                modelLoader = modelLoader,
                inferenceStrategy = inferenceStrategy,
                streamingManager = streamingManager,
                memoryManager = memoryManager,
                samplerStrategy = samplerStrategy,
                dispatcher = dispatcher
            )

            // Initialize with language-specific model
            val initResult = engine.initialize(modelConfig)
            if (initResult is Result.Error) {
                return initResult
            }

            // Update current state
            currentLanguage = languageCode
            currentModelLoader = modelLoader
            currentEngine = engine

            Timber.i("Successfully switched to language: $languageCode")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to switch language: $languageCode")
            Result.Error(
                message = "Language switch failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Get current active language
     */
    fun getCurrentLanguage(): String = currentLanguage

    /**
     * Get list of installed languages
     */
    fun getInstalledLanguages(): List<String> {
        return languagePackManager.getInstalledLanguages()
    }

    /**
     * Check if a language is installed
     */
    fun isLanguageInstalled(languageCode: String): Boolean {
        return languagePackManager.isLanguageInstalled(languageCode)
    }

    /**
     * Chat with the current language model
     *
     * Delegates to current engine instance
     */
    fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse> {
        val engine = currentEngine ?: return kotlinx.coroutines.flow.flow {
            emit(LLMResponse.Error(
                message = "Engine not initialized. Call initialize() first.",
                code = "ENGINE_NOT_INITIALIZED"
            ))
        }

        return engine.chat(messages, options)
    }

    /**
     * Stop current generation
     */
    suspend fun stop() {
        currentEngine?.stop()
    }

    /**
     * Reset engine state
     */
    suspend fun reset() {
        currentEngine?.reset()
    }

    /**
     * Clean up all resources
     */
    suspend fun cleanup() = mutex.withLock {
        try {
            Timber.d("Cleaning up Multilingual ALC Engine")
            currentEngine?.cleanup()
            currentModelLoader?.unloadModel()
            memoryManager.resetCache()
            currentEngine = null
            currentModelLoader = null
            Timber.i("Multilingual ALC Engine cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
        }
    }

    /**
     * Get engine stats
     */
    fun getStats(): EngineStats? {
        return currentEngine?.getStats()
    }

    /**
     * Get memory info
     */
    fun getMemoryInfo(): MemoryInfo? {
        return currentEngine?.getMemoryInfo()
    }

    /**
     * Check if engine is generating
     */
    fun isGenerating(): Boolean {
        return currentEngine?.isGenerating() ?: false
    }

    /**
     * Get model configuration for a language
     */
    private fun getModelConfig(languageCode: String): ModelConfig {
        val modelPath = "${context.filesDir}/models/llm/$languageCode/"
        val modelName = getModelNameForLanguage(languageCode)

        return ModelConfig(
            modelPath = modelPath,
            modelName = modelName,
            language = languageCode,
            deviceType = "opencl",
            contextLength = 2048
        )
    }

    /**
     * Get model name for a language
     */
    private fun getModelNameForLanguage(languageCode: String): String {
        return when (languageCode) {
            "en" -> "gemma-2b-en"
            "es" -> "flor-1.3b-es"
            "fr" -> "croissant-1.3b-fr"
            "de" -> "leo-7b-de"
            "ja" -> "rinna-3.6b-ja"
            "zh" -> "qwen-1.8b-zh"
            "pt" -> "tucano-1.1b-pt"
            "it" -> "minerva-1b-it"
            "ko" -> "polyglot-1.3b-ko"
            "ar" -> "jais-1.3b-ar"
            else -> "gemma-2b-en" // fallback to English
        }
    }
}

/**
 * Exception thrown when trying to use a language pack that isn't installed
 */
class LanguagePackNotInstalledException(
    val languageCode: String
) : Exception("Language pack not installed: $languageCode. Please download it from Settings → Languages.")
