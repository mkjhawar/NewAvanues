package com.augmentalis.ava.features.llm.di

import android.content.Context
import com.augmentalis.ava.core.domain.repository.TokenCacheRepository
import com.augmentalis.ava.features.llm.cache.TokenCacheManager
import com.augmentalis.ava.features.llm.inference.InferenceManager
import com.augmentalis.ava.features.llm.provider.LocalLLMProvider
import com.augmentalis.ava.features.llm.provider.CloudLLMProvider
import com.augmentalis.ava.features.llm.response.HybridResponseGenerator
import com.augmentalis.ava.features.llm.response.ResponseGenerator
import com.augmentalis.ava.features.llm.security.ApiKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for LLM feature.
 *
 * Provides:
 * - ApiKeyManager (singleton) - Secure API key storage for cloud LLM providers
 * - LocalLLMProvider (singleton) - On-device LLM model management
 * - CloudLLMProvider (singleton) - Multi-provider cloud LLM fallback
 * - TokenCacheManager (singleton) - Pre-tokenized content caching
 * - HybridResponseGenerator as ResponseGenerator implementation
 *
 * HybridResponseGenerator provides automatic fallback chain:
 * - Tries Local LLM first (30-second timeout)
 * - Falls back to Cloud LLM if local fails
 * - Falls back to template-based responses if all LLMs fail
 *
 * Updated: 2025-12-05 - Added CloudLLMProvider for cloud fallback
 */
@Module
@InstallIn(SingletonComponent::class)
object LLMModule {

    /**
     * Provides TokenCacheManager singleton.
     *
     * Caches pre-tokenized content to avoid repeated tokenization:
     * - 50x faster context building for cached content
     * - Model-aware: auto-invalidates when model changes
     * - Binary BLOB storage for efficiency
     *
     * @param tokenCacheRepository Repository for token cache persistence
     * @return TokenCacheManager singleton
     */
    @Provides
    @Singleton
    fun provideTokenCacheManager(
        tokenCacheRepository: TokenCacheRepository
    ): TokenCacheManager {
        return TokenCacheManager(tokenCacheRepository)
    }

    /**
     * Provides ApiKeyManager singleton.
     *
     * Manages secure storage and retrieval of API keys for cloud LLM providers.
     * Uses EncryptedSharedPreferences with AES-256 encryption.
     *
     * @param context Application context for encrypted storage
     * @return ApiKeyManager singleton
     */
    @Provides
    @Singleton
    fun provideApiKeyManager(
        @ApplicationContext context: Context
    ): ApiKeyManager {
        return ApiKeyManager(context)
    }

    /**
     * Provides LocalLLMProvider singleton.
     *
     * This provider manages the lifecycle of on-device LLM models.
     * Injected with TokenCacheManager for efficient context building.
     */
    @Provides
    @Singleton
    fun provideLocalLLMProvider(
        @ApplicationContext context: Context,
        tokenCacheManager: TokenCacheManager
    ): LocalLLMProvider {
        return LocalLLMProvider(context, tokenCacheManager = tokenCacheManager)
    }

    /**
     * Provides CloudLLMProvider singleton.
     *
     * This provider manages fallback to cloud LLM services when local LLM fails.
     * Supports multiple providers: OpenRouter, Anthropic, Google AI, OpenAI.
     * Includes cost tracking, rate limiting, and circuit breaker pattern.
     *
     * @param context Application context
     * @param apiKeyManager API key manager for secure key storage
     * @return CloudLLMProvider singleton
     */
    @Provides
    @Singleton
    fun provideCloudLLMProvider(
        @ApplicationContext context: Context,
        apiKeyManager: ApiKeyManager
    ): CloudLLMProvider {
        return CloudLLMProvider(context, apiKeyManager)
    }

    /**
     * Provides HybridResponseGenerator as the ResponseGenerator implementation.
     *
     * This is the optimal choice for production because:
     * 1. Automatic Local LLM → Cloud LLM → Template fallback (30-second timeout)
     * 2. Future-proof: When LLMs are ready, they automatically activate
     * 3. Built-in error recovery for all LLM failures
     * 4. ADR-014: Battery/thermal-aware backend selection via InferenceManager
     * 5. Cost-aware: Respects spending limits for cloud providers
     *
     * Architecture:
     * - Primary: LLMResponseGenerator (on-device Local LLM)
     * - Secondary: CloudLLMProvider (multi-provider cloud fallback)
     * - Tertiary: TemplateResponseGenerator (intent-to-template mapping)
     * - Constraint: InferenceManager (battery/thermal state)
     *
     * @param context Application context for initialization
     * @param localLLMProvider LocalLLMProvider for on-device LLM inference
     * @param cloudLLMProvider CloudLLMProvider for cloud fallback
     * @param inferenceManager InferenceManager for battery/thermal-aware selection
     * @return HybridResponseGenerator singleton
     */
    @Provides
    @Singleton
    fun provideResponseGenerator(
        @ApplicationContext context: Context,
        localLLMProvider: LocalLLMProvider,
        cloudLLMProvider: CloudLLMProvider,
        inferenceManager: InferenceManager
    ): ResponseGenerator {
        return HybridResponseGenerator(
            context = context,
            llmProvider = localLLMProvider,
            cloudLLMProvider = cloudLLMProvider,
            inferenceManager = inferenceManager
        )
    }
}
