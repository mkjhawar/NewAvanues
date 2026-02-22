package com.augmentalis.alc.di

import android.content.Context
import com.augmentalis.alc.config.LLMConfig
import com.augmentalis.alc.domain.ProviderConfig
import com.augmentalis.alc.domain.ProviderType
import com.augmentalis.alc.engine.*
import com.augmentalis.alc.provider.*
import com.augmentalis.alc.response.HybridResponseGenerator
import com.augmentalis.alc.response.TemplateResponseGenerator
import com.augmentalis.llm.security.ApiKeyManager
import com.augmentalis.llm.ProviderType as LLMProviderType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt DI Module for ALC
 *
 * Provides all LLM-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ALCModule {

    @Provides
    @Singleton
    fun provideLLMConfig(): LLMConfig {
        return LLMConfig(
            defaultProvider = ProviderType.ANTHROPIC,
            fallbackProviders = listOf(ProviderType.OPENAI, ProviderType.GROQ),
            localInferenceEnabled = true,
            streamingEnabled = true
        )
    }

    @Provides
    @Singleton
    fun provideApiKeyManager(
        @ApplicationContext context: Context
    ): ApiKeyManager = ApiKeyManager(context)

    @Provides
    @Singleton
    @Named("anthropic")
    fun provideAnthropicProvider(
        apiKeyManager: ApiKeyManager
    ): ILLMProvider {
        val key = runBlocking { apiKeyManager.getApiKey(LLMProviderType.ANTHROPIC) }
            .let { result ->
                when (result) {
                    is com.augmentalis.llm.LLMResult.Success -> result.data
                    is com.augmentalis.llm.LLMResult.Error -> {
                        Timber.w("Anthropic API key not configured: ${result.message}")
                        null
                    }
                    else -> null
                }
            }
        return AnthropicProvider(
            ProviderConfig(
                type = ProviderType.ANTHROPIC,
                apiKey = key,
                model = "claude-3-5-sonnet-20241022"
            )
        )
    }

    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAIProvider(
        apiKeyManager: ApiKeyManager
    ): ILLMProvider {
        val key = runBlocking { apiKeyManager.getApiKey(LLMProviderType.OPENAI) }
            .let { result ->
                when (result) {
                    is com.augmentalis.llm.LLMResult.Success -> result.data
                    is com.augmentalis.llm.LLMResult.Error -> {
                        Timber.w("OpenAI API key not configured: ${result.message}")
                        null
                    }
                    else -> null
                }
            }
        return OpenAIProvider(
            ProviderConfig(
                type = ProviderType.OPENAI,
                apiKey = key,
                model = "gpt-4o"
            )
        )
    }

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqProvider(
        apiKeyManager: ApiKeyManager
    ): ILLMProvider {
        val key = runBlocking { apiKeyManager.getApiKey(LLMProviderType.GROQ) }
            .let { result ->
                when (result) {
                    is com.augmentalis.llm.LLMResult.Success -> result.data
                    is com.augmentalis.llm.LLMResult.Error -> {
                        Timber.w("Groq API key not configured: ${result.message}")
                        null
                    }
                    else -> null
                }
            }
        return GroqProvider(
            ProviderConfig(
                type = ProviderType.GROQ,
                apiKey = key,
                model = "llama-3.1-70b-versatile"
            )
        )
    }

    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleAIProvider(
        apiKeyManager: ApiKeyManager
    ): ILLMProvider {
        val key = runBlocking { apiKeyManager.getApiKey(LLMProviderType.GOOGLE_AI) }
            .let { result ->
                when (result) {
                    is com.augmentalis.llm.LLMResult.Success -> result.data
                    is com.augmentalis.llm.LLMResult.Error -> {
                        Timber.w("Google AI API key not configured: ${result.message}")
                        null
                    }
                    else -> null
                }
            }
        return GoogleAIProvider(
            ProviderConfig(
                type = ProviderType.GOOGLE_AI,
                apiKey = key,
                model = "gemini-1.5-pro"
            )
        )
    }

    @Provides
    @Singleton
    fun provideLocalEngine(
        @ApplicationContext context: Context
    ): IInferenceEngine {
        return ALCEngineAndroid(context)
    }

    @Provides
    @Singleton
    fun provideTemplateGenerator(): TemplateResponseGenerator {
        return TemplateResponseGenerator()
    }

    @Provides
    @Singleton
    fun provideHybridGenerator(
        templateGenerator: TemplateResponseGenerator,
        @Named("anthropic") llmProvider: ILLMProvider
    ): HybridResponseGenerator {
        return HybridResponseGenerator(templateGenerator, llmProvider)
    }

    @Provides
    @Singleton
    fun provideFallbackProvider(
        @Named("anthropic") anthropic: ILLMProvider,
        @Named("openai") openai: ILLMProvider,
        @Named("groq") groq: ILLMProvider
    ): FallbackProvider {
        return FallbackProvider(
            primary = anthropic,
            fallbacks = listOf(openai, groq)
        )
    }

    @Provides
    @Singleton
    fun provideALCManager(
        @ApplicationContext context: Context,
        config: LLMConfig,
        localEngine: IInferenceEngine,
        fallbackProvider: FallbackProvider,
        hybridGenerator: HybridResponseGenerator
    ): ALCManager {
        return ALCManager(
            context = context,
            config = config,
            localEngine = localEngine,
            cloudProvider = fallbackProvider,
            responseGenerator = hybridGenerator
        )
    }
}

/**
 * Main ALC Manager - orchestrates local and cloud inference
 */
class ALCManager(
    private val context: Context,
    private val config: LLMConfig,
    private val localEngine: IInferenceEngine,
    private val cloudProvider: ILLMProvider,
    private val responseGenerator: HybridResponseGenerator
) {
    private var useLocalInference = config.localInferenceEnabled

    suspend fun initialize(): Result<Unit> {
        return if (useLocalInference) {
            localEngine.initialize()
        } else {
            Result.success(Unit)
        }
    }

    suspend fun loadLocalModel(modelPath: String): Result<Unit> {
        return localEngine.loadModel(modelPath)
    }

    fun chat(
        messages: List<com.augmentalis.alc.domain.ChatMessage>,
        options: com.augmentalis.alc.domain.GenerationOptions = com.augmentalis.alc.domain.GenerationOptions.DEFAULT,
        preferLocal: Boolean = true
    ) = kotlinx.coroutines.flow.flow {
        val provider = if (preferLocal && useLocalInference && localEngine.isInitialized) {
            localEngine
        } else {
            cloudProvider
        }

        provider.chat(messages, options).collect { emit(it) }
    }

    fun generateWithIntent(
        messages: List<com.augmentalis.alc.domain.ChatMessage>,
        intent: String?,
        params: Map<String, String> = emptyMap(),
        options: com.augmentalis.alc.domain.GenerationOptions = com.augmentalis.alc.domain.GenerationOptions.DEFAULT
    ) = responseGenerator.generate(messages, intent, params, options)

    suspend fun getHealth(): Map<String, com.augmentalis.alc.domain.ProviderHealth> {
        return mapOf(
            "local" to localEngine.healthCheck(),
            "cloud" to cloudProvider.healthCheck()
        )
    }

    fun getStats(): EngineStats? = localEngine.getStats()

    fun setUseLocalInference(enabled: Boolean) {
        useLocalInference = enabled
    }

    suspend fun cleanup() {
        localEngine.cleanup()
    }
}
