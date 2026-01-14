/**
 * Multi-Provider Inference Strategy
 *
 * Single Responsibility: Try multiple inference backends with automatic fallback
 *
 * Implements the Strategy pattern with fallback logic:
 * 1. Try highest priority provider (MLC LLM)
 * 2. If fails, try next priority (llama.cpp)
 * 3. Continue until success or all providers exhausted
 *
 * Open/Closed Principle: Easy to add new providers without modifying existing code
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.inference

import com.augmentalis.llm.alc.interfaces.IInferenceStrategy
import com.augmentalis.llm.alc.models.InferenceException
import com.augmentalis.llm.alc.models.InferenceRequest
import com.augmentalis.llm.alc.models.InferenceResult
import timber.log.Timber

/**
 * Inference strategy that tries multiple providers with fallback
 *
 * @param providers List of inference strategies, ordered by priority
 */
class MultiProviderInferenceStrategy(
    private val providers: List<IInferenceStrategy>
) : IInferenceStrategy {

    init {
        require(providers.isNotEmpty()) { "At least one provider must be specified" }

        // Sort providers by priority (lowest number = highest priority)
        providers.sortedBy { it.getPriority() }

        Timber.d("Initialized MultiProviderInferenceStrategy with ${providers.size} providers:")
        providers.forEach { provider ->
            Timber.d("  - ${provider.getName()} (priority: ${provider.getPriority()}, available: ${provider.isAvailable()})")
        }
    }

    override suspend fun infer(request: InferenceRequest): InferenceResult {
        val availableProviders = providers.filter { it.isAvailable() }

        if (availableProviders.isEmpty()) {
            throw InferenceException("No inference providers are available")
        }

        var lastException: Exception? = null

        // Try each provider in priority order
        for (provider in availableProviders) {
            try {
                Timber.d("Trying inference with provider: ${provider.getName()}")
                val result = provider.infer(request)

                // Add provider metadata to result
                val enhancedMetadata = result.metadata.toMutableMap()
                enhancedMetadata["provider_used"] = provider.getName()
                enhancedMetadata["provider_priority"] = provider.getPriority()

                return result.copy(metadata = enhancedMetadata)

            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Provider ${provider.getName()} failed, trying next...")
            }
        }

        // All providers failed
        val triedProviders = availableProviders.joinToString(", ") { it.getName() }
        throw InferenceException(
            "All inference providers failed. Tried: $triedProviders. Last error: ${lastException?.message}",
            lastException
        )
    }

    override fun isAvailable(): Boolean {
        return providers.any { it.isAvailable() }
    }

    override fun getName(): String {
        val providerNames = providers.joinToString("+") { it.getName() }
        return "multi($providerNames)"
    }

    override fun getPriority(): Int {
        // Multi-provider has same priority as its highest priority child
        return providers.minOfOrNull { it.getPriority() } ?: Int.MAX_VALUE
    }

    /**
     * Get list of available providers
     */
    fun getAvailableProviders(): List<String> {
        return providers.filter { it.isAvailable() }.map { it.getName() }
    }

    /**
     * Get provider statistics
     */
    fun getProviderStats(): Map<String, Map<String, Any>> {
        return providers.associate { provider ->
            provider.getName() to mapOf(
                "available" to provider.isAvailable(),
                "priority" to provider.getPriority()
            )
        }
    }
}
