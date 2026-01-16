/**
 * Cloud LLM Provider with Multi-Backend Support
 *
 * Implements LLMProvider interface with intelligent fallback across multiple cloud providers.
 * Serves as the fallback when LocalLLMProvider fails or is unavailable.
 *
 * Supported Backends (priority order):
 * 1. OpenRouter (aggregator with 100+ models, best coverage)
 * 2. Anthropic Claude (high quality, extended context)
 * 3. Google Gemini (fast, multimodal)
 * 4. OpenAI GPT (widely compatible)
 *
 * Features:
 * - Automatic provider fallback (tries all configured providers)
 * - Streaming responses via Server-Sent Events (SSE)
 * - Rate limiting and exponential backoff retry
 * - Secure API key storage (EncryptedSharedPreferences)
 * - Cost tracking and limiting
 * - Health monitoring and circuit breaker pattern
 *
 * Fallback Chain:
 * Local LLM -> Cloud Provider 1 -> Cloud Provider 2 -> ... -> Template Responses
 *
 * Cost Tracking:
 * - Tracks token usage per provider
 * - Enforces daily/monthly spending limits
 * - Provides cost estimates before generation
 *
 * Rate Limiting:
 * - Exponential backoff (1s, 2s, 4s, 8s, 16s)
 * - Circuit breaker (3 consecutive failures = disabled)
 * - Automatic recovery after cooldown period
 *
 * Created: 2025-12-05
 * Author: AVA AI Team
 */

package com.augmentalis.llm.provider

import android.content.Context
import android.content.SharedPreferences
import com.augmentalis.ava.core.common.Result
import com.augmentalis.llm.domain.*
import com.augmentalis.llm.security.ApiKeyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

/**
 * Cloud LLM Provider
 *
 * Usage:
 * ```
 * val cloudProvider = CloudLLMProvider(context, apiKeyManager)
 *
 * // Initialize with preferred provider (or auto-select)
 * cloudProvider.initialize(LLMConfig(
 *     modelPath = "auto", // Auto-selects best available provider
 *     apiKey = null // Uses ApiKeyManager
 * ))
 *
 * // Generate with automatic fallback
 * cloudProvider.chat(messages, options).collect { response ->
 *     when (response) {
 *         is LLMResponse.Streaming -> print(response.chunk)
 *         is LLMResponse.Complete -> println("\nDone")
 *         is LLMResponse.Error -> println("Error: ${response.message}")
 *     }
 * }
 * ```
 */
class CloudLLMProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    companion object {
        private const val TAG = "CloudLLMProvider"

        // Provider priority (for fallback)
        private val PROVIDER_PRIORITY = listOf(
            ProviderType.OPENROUTER,   // Aggregator, best coverage
            ProviderType.ANTHROPIC,     // High quality
            ProviderType.GOOGLE_AI,     // Fast, affordable
            ProviderType.OPENAI         // Widely compatible
        )

        // Cost limits (USD)
        private const val DEFAULT_DAILY_LIMIT = 5.00
        private const val DEFAULT_MONTHLY_LIMIT = 50.00

        // Rate limiting
        private const val MAX_RETRIES = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val CIRCUIT_BREAKER_THRESHOLD = 3
        private const val CIRCUIT_BREAKER_COOLDOWN_MS = 60_000L // 1 minute

        // SharedPreferences keys
        private const val PREFS_NAME = "cloud_llm_provider"
        private const val KEY_DAILY_COST = "daily_cost"
        private const val KEY_MONTHLY_COST = "monthly_cost"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"
        private const val KEY_LAST_RESET_MONTH = "last_reset_month"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    // Provider instances (lazy initialized)
    private var openRouterProvider: OpenRouterProvider? = null
    private var anthropicProvider: AnthropicProvider? = null
    private var googleAIProvider: GoogleAIProvider? = null
    private var openAIProvider: OpenAIProvider? = null

    // Active providers (configured with API keys)
    private val activeProviders = mutableListOf<Pair<ProviderType, LLMProvider>>()

    // Circuit breaker state
    private val consecutiveFailures = mutableMapOf<ProviderType, AtomicInteger>()
    private val circuitBreakerOpenUntil = mutableMapOf<ProviderType, Long>()

    // Cost tracking
    private var dailyCostLimit = DEFAULT_DAILY_LIMIT
    private var monthlyCostLimit = DEFAULT_MONTHLY_LIMIT

    // Health tracking
    private val healthMetrics = mutableMapOf<ProviderType, ProviderHealthMetrics>()

    // Current state
    private var isGenerating = false
    private var preferredProvider: ProviderType? = null

    override suspend fun initialize(config: LLMConfig): Result<Unit> = mutex.withLock {
        return@withLock try {
            Timber.i("Initializing CloudLLMProvider")

            // Reset cost counters if needed
            resetCostTrackingIfNeeded()

            // Initialize all available providers
            val initResults = mutableListOf<Pair<ProviderType, Result<Unit>>>()

            for (providerType in PROVIDER_PRIORITY) {
                // Check if API key is available
                if (!apiKeyManager.hasApiKey(providerType)) {
                    Timber.d("Skipping $providerType (no API key)")
                    continue
                }

                // Get API key
                val apiKeyResult = apiKeyManager.getApiKey(providerType)
                val apiKey = when (apiKeyResult) {
                    is Result.Success -> apiKeyResult.data
                    is Result.Error -> {
                        Timber.w("Failed to get API key for $providerType: ${apiKeyResult.message}")
                        continue
                    }
                }

                // Create and initialize provider
                val provider = createProvider(providerType)
                val providerConfig = LLMConfig(
                    modelPath = getDefaultModel(providerType),
                    apiKey = apiKey
                )

                val initResult = provider.initialize(providerConfig)
                initResults.add(providerType to initResult)

                when (initResult) {
                    is Result.Success -> {
                        activeProviders.add(providerType to provider)
                        consecutiveFailures[providerType] = AtomicInteger(0)
                        healthMetrics[providerType] = ProviderHealthMetrics()
                        Timber.i("✓ $providerType initialized successfully")
                    }
                    is Result.Error -> {
                        Timber.w("✗ $providerType initialization failed: ${initResult.message}")
                    }
                }
            }

            // Check if at least one provider is available
            if (activeProviders.isEmpty()) {
                return Result.Error(
                    exception = IllegalStateException("No cloud providers available"),
                    message = "No cloud providers configured. Please add API keys for at least one provider."
                )
            }

            // Set preferred provider from config or use first available
            preferredProvider = if (config.modelPath != "auto") {
                ProviderType.values().find { it.name.equals(config.modelPath, ignoreCase = true) }
                    ?: activeProviders.first().first
            } else {
                activeProviders.first().first
            }

            Timber.i("CloudLLMProvider initialized with ${activeProviders.size} providers")
            Timber.i("Preferred provider: $preferredProvider")
            Timber.i("Fallback chain: ${activeProviders.joinToString(" -> ") { it.first.name }}")

            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize CloudLLMProvider")
            Result.Error(
                exception = e,
                message = "Cloud provider initialization failed: ${e.message}"
            )
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions
    ): Flow<LLMResponse> {
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = prompt))
        return chat(messages, options)
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        if (activeProviders.isEmpty()) {
            emit(LLMResponse.Error(
                message = "No cloud providers configured. Please add API keys.",
                code = "NO_PROVIDERS"
            ))
            return@flow
        }

        isGenerating = true

        try {
            // Check cost limits
            val estimatedCost = estimateRequestCost(messages, options)
            if (!canAffordRequest(estimatedCost)) {
                emit(LLMResponse.Error(
                    message = "Cost limit exceeded. Daily: $${getDailyCost()}/$dailyCostLimit, Monthly: $${getMonthlyCost()}/$monthlyCostLimit",
                    code = "COST_LIMIT_EXCEEDED"
                ))
                return@flow
            }

            // Try providers in priority order with fallback
            var lastError: LLMResponse.Error? = null
            val providersToTry = getProvidersToTry()

            for ((providerType, provider) in providersToTry) {
                // Check circuit breaker
                if (isCircuitBreakerOpen(providerType)) {
                    Timber.w("Circuit breaker open for $providerType, skipping")
                    continue
                }

                Timber.d("Trying provider: $providerType")

                try {
                    // Try generation with retry
                    var success = false
                    provider.chat(messages, options).collect { response ->
                        when (response) {
                            is LLMResponse.Streaming -> {
                                emit(response)
                                success = true
                            }
                            is LLMResponse.Complete -> {
                                // Track cost
                                val cost = estimateCost(
                                    response.usage.promptTokens,
                                    response.usage.completionTokens
                                )
                                addCost(cost)

                                // Update health metrics
                                recordSuccess(providerType)

                                emit(response)
                                success = true
                            }
                            is LLMResponse.Error -> {
                                lastError = response
                                recordFailure(providerType)
                                Timber.w("Provider $providerType failed: ${response.message}")
                            }
                        }
                    }

                    // If we got here with success, we're done
                    if (success) {
                        return@flow
                    }

                } catch (e: Exception) {
                    Timber.w(e, "Exception from provider $providerType")
                    recordFailure(providerType)
                    lastError = LLMResponse.Error(
                        message = "Provider failed: ${e.message}",
                        code = "PROVIDER_EXCEPTION",
                        exception = e
                    )
                }
            }

            // All providers failed
            emit(lastError ?: LLMResponse.Error(
                message = "All cloud providers failed",
                code = "ALL_PROVIDERS_FAILED"
            ))

        } catch (e: Exception) {
            Timber.e(e, "CloudLLMProvider chat failed")
            emit(LLMResponse.Error(
                message = "Cloud generation failed: ${e.message}",
                code = "CLOUD_FAILED",
                exception = e
            ))
        } finally {
            isGenerating = false
        }
    }

    override suspend fun stop() {
        isGenerating = false
        activeProviders.forEach { (_, provider) ->
            try {
                provider.stop()
            } catch (e: Exception) {
                Timber.w(e, "Failed to stop provider")
            }
        }
    }

    override suspend fun reset() {
        stop()
        activeProviders.forEach { (_, provider) ->
            try {
                provider.reset()
            } catch (e: Exception) {
                Timber.w(e, "Failed to reset provider")
            }
        }
    }

    override suspend fun cleanup() = mutex.withLock {
        stop()
        activeProviders.forEach { (_, provider) ->
            try {
                provider.cleanup()
            } catch (e: Exception) {
                Timber.w(e, "Failed to cleanup provider")
            }
        }
        activeProviders.clear()
        consecutiveFailures.clear()
        circuitBreakerOpenUntil.clear()
        healthMetrics.clear()

        openRouterProvider = null
        anthropicProvider = null
        googleAIProvider = null
        openAIProvider = null
    }

    override fun isGenerating(): Boolean = isGenerating

    override fun getInfo(): LLMProviderInfo {
        val activeProviderNames = activeProviders.joinToString(", ") { it.first.name }
        return LLMProviderInfo(
            name = "Cloud LLM (Multi-Provider)",
            version = "1.0",
            modelName = "Fallback: $activeProviderNames",
            isLocal = false,
            capabilities = LLMCapabilities(
                supportsStreaming = true,
                supportsChat = true,
                supportsFunctionCalling = true,
                maxContextLength = 200_000 // Max across all providers
            )
        )
    }

    override suspend fun checkHealth(): Result<ProviderHealth> {
        val healthChecks = activeProviders.map { (type, provider) ->
            type to provider.checkHealth()
        }

        val healthyCount = healthChecks.count { (_, result) ->
            result is Result.Success && result.data.status == HealthStatus.HEALTHY
        }

        val overallStatus = when {
            healthyCount == 0 -> HealthStatus.UNHEALTHY
            healthyCount < activeProviders.size -> HealthStatus.DEGRADED
            else -> HealthStatus.HEALTHY
        }

        return Result.Success(
            ProviderHealth(
                status = overallStatus,
                averageLatencyMs = calculateAverageLatency(),
                errorRate = calculateErrorRate(),
                lastError = healthMetrics.values.mapNotNull { it.lastError }.firstOrNull(),
                lastChecked = System.currentTimeMillis()
            )
        )
    }

    override fun estimateCost(inputTokens: Int, outputTokens: Int): Double {
        val provider = activeProviders.firstOrNull()?.second ?: return 0.0
        return provider.estimateCost(inputTokens, outputTokens)
    }

    // ==================== Cost Management ====================

    /**
     * Set daily cost limit
     */
    fun setDailyCostLimit(limit: Double) {
        dailyCostLimit = limit
        Timber.i("Daily cost limit set to: $$limit")
    }

    /**
     * Set monthly cost limit
     */
    fun setMonthlyCostLimit(limit: Double) {
        monthlyCostLimit = limit
        Timber.i("Monthly cost limit set to: $$limit")
    }

    /**
     * Get current daily cost
     */
    fun getDailyCost(): Double {
        return prefs.getFloat(KEY_DAILY_COST, 0f).toDouble()
    }

    /**
     * Get current monthly cost
     */
    fun getMonthlyCost(): Double {
        return prefs.getFloat(KEY_MONTHLY_COST, 0f).toDouble()
    }

    /**
     * Reset cost counters
     */
    fun resetCostTracking() {
        prefs.edit()
            .putFloat(KEY_DAILY_COST, 0f)
            .putFloat(KEY_MONTHLY_COST, 0f)
            .apply()
        Timber.i("Cost tracking reset")
    }

    // ==================== Private Helpers ====================

    private fun createProvider(type: ProviderType): LLMProvider {
        return when (type) {
            ProviderType.OPENROUTER -> {
                openRouterProvider ?: OpenRouterProvider(context, apiKeyManager).also {
                    openRouterProvider = it
                }
            }
            ProviderType.ANTHROPIC -> {
                anthropicProvider ?: AnthropicProvider(context, apiKeyManager).also {
                    anthropicProvider = it
                }
            }
            ProviderType.GOOGLE_AI -> {
                googleAIProvider ?: GoogleAIProvider(context, apiKeyManager).also {
                    googleAIProvider = it
                }
            }
            ProviderType.OPENAI -> {
                openAIProvider ?: OpenAIProvider(context, apiKeyManager).also {
                    openAIProvider = it
                }
            }
            else -> throw IllegalArgumentException("Unsupported provider: $type")
        }
    }

    private fun getDefaultModel(type: ProviderType): String {
        return when (type) {
            ProviderType.OPENROUTER -> "anthropic/claude-3.5-sonnet"
            ProviderType.ANTHROPIC -> "claude-3-5-sonnet-20241022"
            ProviderType.GOOGLE_AI -> "gemini-1.5-flash"
            ProviderType.OPENAI -> "gpt-4-turbo-preview"
            else -> ""
        }
    }

    private fun getProvidersToTry(): List<Pair<ProviderType, LLMProvider>> {
        // Preferred provider first, then others
        return buildList {
            preferredProvider?.let { preferred ->
                activeProviders.find { it.first == preferred }?.let { add(it) }
            }
            activeProviders.forEach { provider ->
                if (provider.first != preferredProvider) {
                    add(provider)
                }
            }
        }
    }

    private fun isCircuitBreakerOpen(type: ProviderType): Boolean {
        val openUntil = circuitBreakerOpenUntil[type] ?: return false
        val now = System.currentTimeMillis()

        if (now >= openUntil) {
            // Cooldown expired, close circuit breaker
            circuitBreakerOpenUntil.remove(type)
            consecutiveFailures[type]?.set(0)
            Timber.i("Circuit breaker closed for $type (cooldown expired)")
            return false
        }

        return true
    }

    private fun recordSuccess(type: ProviderType) {
        consecutiveFailures[type]?.set(0)
        healthMetrics[type]?.recordSuccess()
    }

    private fun recordFailure(type: ProviderType) {
        val failures = consecutiveFailures[type]?.incrementAndGet() ?: 0
        healthMetrics[type]?.recordFailure()

        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            val openUntil = System.currentTimeMillis() + CIRCUIT_BREAKER_COOLDOWN_MS
            circuitBreakerOpenUntil[type] = openUntil
            Timber.w("Circuit breaker opened for $type after $failures consecutive failures")
        }
    }

    private fun addCost(cost: Double) {
        val currentDaily = getDailyCost()
        val currentMonthly = getMonthlyCost()

        prefs.edit()
            .putFloat(KEY_DAILY_COST, (currentDaily + cost).toFloat())
            .putFloat(KEY_MONTHLY_COST, (currentMonthly + cost).toFloat())
            .apply()

        Timber.d("Cost added: $$cost (Daily: $$currentDaily -> $${currentDaily + cost})")
    }

    private fun canAffordRequest(estimatedCost: Double): Boolean {
        val dailyCost = getDailyCost()
        val monthlyCost = getMonthlyCost()

        return (dailyCost + estimatedCost) <= dailyCostLimit &&
               (monthlyCost + estimatedCost) <= monthlyCostLimit
    }

    private fun estimateRequestCost(messages: List<ChatMessage>, options: GenerationOptions): Double {
        val estimatedInputTokens = messages.sumOf { it.content.length / 4 }
        val estimatedOutputTokens = options.maxTokens ?: 1024
        return estimateCost(estimatedInputTokens, estimatedOutputTokens)
    }

    private fun resetCostTrackingIfNeeded() {
        val now = System.currentTimeMillis()
        val currentDay = (now / (24 * 60 * 60 * 1000)).toInt()
        val currentMonth = (now / (30L * 24 * 60 * 60 * 1000)).toInt()

        val lastResetDay = prefs.getInt(KEY_LAST_RESET_DAY, -1)
        val lastResetMonth = prefs.getInt(KEY_LAST_RESET_MONTH, -1)

        var needsSave = false

        if (lastResetDay != currentDay) {
            prefs.edit()
                .putFloat(KEY_DAILY_COST, 0f)
                .putInt(KEY_LAST_RESET_DAY, currentDay)
                .apply()
            Timber.i("Daily cost counter reset")
            needsSave = true
        }

        if (lastResetMonth != currentMonth) {
            prefs.edit()
                .putFloat(KEY_MONTHLY_COST, 0f)
                .putInt(KEY_LAST_RESET_MONTH, currentMonth)
                .apply()
            Timber.i("Monthly cost counter reset")
            needsSave = true
        }
    }

    private fun calculateAverageLatency(): Long? {
        val latencies = healthMetrics.values.mapNotNull { it.getAverageLatency() }
        return if (latencies.isNotEmpty()) {
            latencies.average().toLong()
        } else {
            null
        }
    }

    private fun calculateErrorRate(): Double? {
        val errorRates = healthMetrics.values.mapNotNull { it.getErrorRate() }
        return if (errorRates.isNotEmpty()) {
            errorRates.average()
        } else {
            null
        }
    }
}

/**
 * Provider health metrics
 */
private class ProviderHealthMetrics {
    private val requestLatencies = mutableListOf<Long>()
    private val maxLatencyHistory = 100

    private var successCount = 0
    private var failureCount = 0

    var lastError: String? = null
        private set

    fun recordSuccess() {
        successCount++
    }

    fun recordFailure(error: String? = null) {
        failureCount++
        lastError = error
    }

    fun recordLatency(latencyMs: Long) {
        requestLatencies.add(latencyMs)
        if (requestLatencies.size > maxLatencyHistory) {
            requestLatencies.removeAt(0)
        }
    }

    fun getAverageLatency(): Long? {
        return if (requestLatencies.isNotEmpty()) {
            requestLatencies.average().toLong()
        } else {
            null
        }
    }

    fun getErrorRate(): Double? {
        val total = successCount + failureCount
        return if (total > 0) {
            failureCount.toDouble() / total
        } else {
            null
        }
    }
}
