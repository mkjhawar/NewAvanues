package com.augmentalis.alc.provider

import com.augmentalis.alc.domain.ProviderConfig
import com.augmentalis.alc.domain.ProviderType
import com.augmentalis.alc.engine.ILLMProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Complexity hint for routing to the appropriate provider/model tier.
 *
 * SIMPLE  - Short, low-stakes completions. Route to fastest/cheapest model.
 * MEDIUM  - Balanced tasks requiring reasoning but not heavy domain knowledge.
 * COMPLEX - Multi-step, analytical, creative, or knowledge-intensive tasks.
 */
enum class TaskComplexity {
    SIMPLE,
    MEDIUM,
    COMPLEX
}

/**
 * Result of a provider routing decision.
 *
 * @property provider The resolved ILLMProvider instance.
 * @property providerType The ProviderType that was selected.
 * @property model The model identifier that will be used.
 * @property tier The complexity tier this routing satisfies.
 */
data class RoutingDecision(
    val provider: ILLMProvider,
    val providerType: ProviderType,
    val model: String,
    val tier: TaskComplexity
)

/**
 * Unified provider factory with complexity-based routing and multi-provider fallback.
 *
 * Wraps [LLMProviderFactory] as the canonical cloud-provider creation mechanism
 * and adds a routing layer that selects the optimal provider + model for a given
 * [TaskComplexity]. If the primary tier's providers are unavailable (missing API
 * keys), the factory falls back through progressively cheaper/simpler tiers.
 *
 * Routing defaults:
 *   SIMPLE  → Groq / llama-3.1-8b-instant  (fastest, lowest cost)
 *   MEDIUM  → OpenAI / gpt-4o-mini          (balanced)
 *   COMPLEX → Anthropic / claude-sonnet-4-20250514  (most capable)
 *
 * Fallback order when a primary provider is unavailable:
 *   COMPLEX → MEDIUM → SIMPLE
 *
 * Construction:
 * ```kotlin
 * val factory = UnifiedProviderFactory(
 *     apiKeys = mapOf(
 *         "anthropic"   to "<key>",
 *         "openai"      to "<key>",
 *         "groq"        to "<key>",
 *         "google_ai"   to "<key>",
 *         "openrouter"  to "<key>",
 *         "huggingface" to "<key>"
 *     )
 * )
 * val decision = factory.resolve(TaskComplexity.COMPLEX)
 * ```
 *
 * Thread-safe: provider instances are created lazily and cached under a [Mutex].
 */
class UnifiedProviderFactory(
    private val apiKeys: Map<String, String>
) {
    private val mutex = Mutex()
    private val providerCache = mutableMapOf<String, ILLMProvider>()

    // ── Tier configuration ──────────────────────────────────────────────────

    /**
     * SIMPLE tier: Groq with llama-3.1-8b-instant as primary, then HuggingFace as fallback.
     * Rationale: Groq's inference is among the fastest available; 8b model is adequate for
     * classification, short summarisation, and slot-filling tasks.
     */
    private val simpleTierCandidates: List<Pair<ProviderType, String>> = listOf(
        ProviderType.GROQ         to "llama-3.1-8b-instant",
        ProviderType.HUGGINGFACE  to "meta-llama/Llama-3.2-3B-Instruct",
        ProviderType.OPENAI       to "gpt-4o-mini"
    )

    /**
     * MEDIUM tier: OpenAI gpt-4o-mini as primary, then OpenRouter or Groq mixtral as fallback.
     * Rationale: gpt-4o-mini offers strong instruction following at low latency/cost.
     */
    private val mediumTierCandidates: List<Pair<ProviderType, String>> = listOf(
        ProviderType.OPENAI      to "gpt-4o-mini",
        ProviderType.OPENROUTER  to "mistralai/mistral-7b-instruct",
        ProviderType.GROQ        to "mixtral-8x7b-32768"
    )

    /**
     * COMPLEX tier: Anthropic claude-sonnet-4-20250514 as primary, then Google Gemini 1.5 Pro,
     * then OpenAI GPT-4o as last resort.
     * Rationale: Claude Sonnet 4 provides best-in-class reasoning for complex analytical tasks.
     */
    private val complexTierCandidates: List<Pair<ProviderType, String>> = listOf(
        ProviderType.ANTHROPIC   to "claude-sonnet-4-20250514",
        ProviderType.GOOGLE_AI   to "gemini-1.5-pro",
        ProviderType.OPENAI      to "gpt-4o"
    )

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Resolve the optimal provider for the given [complexity].
     *
     * Tries each candidate in the tier's priority order, skipping those whose API
     * key is missing. Falls back down through lower tiers if no candidate in the
     * requested tier has a key configured.
     *
     * @param complexity The complexity hint for this task.
     * @return [RoutingDecision] containing the resolved provider, type, model, and tier.
     * @throws IllegalStateException if no configured provider can be found across all tiers.
     */
    suspend fun resolve(complexity: TaskComplexity): RoutingDecision = mutex.withLock {
        val primaryCandidates = candidatesForTier(complexity)
        val fallbackSequence = buildFallbackSequence(complexity)

        for ((tier, candidates) in fallbackSequence) {
            for ((type, model) in candidates) {
                val key = apiKeyFor(type) ?: continue
                val cacheKey = "${type.name}:$model"
                val provider = providerCache.getOrPut(cacheKey) {
                    LLMProviderFactory.createCloudProvider(
                        ProviderConfig(
                            type = type,
                            apiKey = key,
                            model = model
                        )
                    )
                }
                return@withLock RoutingDecision(
                    provider = provider,
                    providerType = type,
                    model = model,
                    tier = tier
                )
            }
        }

        error(
            "UnifiedProviderFactory: no configured provider found for complexity=$complexity. " +
            "Checked tiers: ${fallbackSequence.map { it.first }}. " +
            "Provide at least one API key in the apiKeys map."
        )
    }

    /**
     * Resolve a [FallbackProvider] that will automatically retry through all candidates
     * in the requested tier, then fall back to lower tiers.
     *
     * Useful when you want the factory to manage retries transparently rather than
     * handling [RoutingDecision] selection in the call site.
     *
     * @param complexity The complexity hint for this task.
     * @return A [FallbackProvider] wrapping all available candidates across tiers.
     * @throws IllegalStateException if no configured provider can be found.
     */
    suspend fun resolveWithFallback(complexity: TaskComplexity): FallbackProvider = mutex.withLock {
        val allCandidates = buildFallbackSequence(complexity)
            .flatMap { (tier, candidates) ->
                candidates.mapNotNull { (type, model) ->
                    val key = apiKeyFor(type) ?: return@mapNotNull null
                    val cacheKey = "${type.name}:$model"
                    providerCache.getOrPut(cacheKey) {
                        LLMProviderFactory.createCloudProvider(
                            ProviderConfig(type = type, apiKey = key, model = model)
                        )
                    }
                }
            }

        if (allCandidates.isEmpty()) {
            error(
                "UnifiedProviderFactory: no configured providers found for complexity=$complexity. " +
                "Provide at least one API key in the apiKeys map."
            )
        }

        FallbackProvider(
            primary = allCandidates.first(),
            fallbacks = allCandidates.drop(1)
        )
    }

    /**
     * Create a provider directly for a specific [ProviderType] and [model], bypassing routing.
     * Useful when the caller already knows exactly which provider/model to use.
     *
     * @param type The provider type to instantiate.
     * @param model The model identifier.
     * @return The [ILLMProvider] instance.
     * @throws IllegalStateException if the API key for [type] is not configured.
     */
    suspend fun createDirect(type: ProviderType, model: String): ILLMProvider = mutex.withLock {
        val key = apiKeyFor(type)
            ?: error("UnifiedProviderFactory: no API key configured for provider ${type.name}")
        val cacheKey = "${type.name}:$model"
        providerCache.getOrPut(cacheKey) {
            LLMProviderFactory.createCloudProvider(
                ProviderConfig(type = type, apiKey = key, model = model)
            )
        }
    }

    /**
     * Return the set of [ProviderType]s for which an API key is currently configured.
     */
    fun configuredProviders(): Set<ProviderType> {
        return ProviderType.entries
            .filter { apiKeyFor(it) != null }
            .toSet()
    }

    /**
     * Evict all cached provider instances. Subsequent calls to [resolve] or
     * [resolveWithFallback] will create fresh provider instances.
     * Call this after updating API keys at runtime.
     */
    suspend fun clearCache() = mutex.withLock {
        providerCache.clear()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun candidatesForTier(tier: TaskComplexity): List<Pair<ProviderType, String>> =
        when (tier) {
            TaskComplexity.SIMPLE  -> simpleTierCandidates
            TaskComplexity.MEDIUM  -> mediumTierCandidates
            TaskComplexity.COMPLEX -> complexTierCandidates
        }

    /**
     * Build a fallback sequence starting at [requestedTier] and working down to SIMPLE.
     * Returns a list of (tier, candidates) pairs deduplicated by provider+model.
     */
    private fun buildFallbackSequence(
        requestedTier: TaskComplexity
    ): List<Pair<TaskComplexity, List<Pair<ProviderType, String>>>> {
        val seen = mutableSetOf<String>()
        val sequence = mutableListOf<Pair<TaskComplexity, List<Pair<ProviderType, String>>>>()

        val tiersInOrder = when (requestedTier) {
            TaskComplexity.COMPLEX -> listOf(TaskComplexity.COMPLEX, TaskComplexity.MEDIUM, TaskComplexity.SIMPLE)
            TaskComplexity.MEDIUM  -> listOf(TaskComplexity.MEDIUM, TaskComplexity.SIMPLE)
            TaskComplexity.SIMPLE  -> listOf(TaskComplexity.SIMPLE)
        }

        for (tier in tiersInOrder) {
            val deduped = candidatesForTier(tier).filter { (type, model) ->
                seen.add("${type.name}:$model")
            }
            if (deduped.isNotEmpty()) {
                sequence.add(tier to deduped)
            }
        }

        return sequence
    }

    /**
     * Resolve an API key for the given provider type.
     * Accepts keys under the provider's lowercase name or enum name.
     */
    private fun apiKeyFor(type: ProviderType): String? {
        val key = when (type) {
            ProviderType.ANTHROPIC   -> apiKeys["anthropic"]   ?: apiKeys["ANTHROPIC"]
            ProviderType.OPENAI      -> apiKeys["openai"]      ?: apiKeys["OPENAI"]
            ProviderType.GOOGLE_AI   -> apiKeys["google_ai"]   ?: apiKeys["GOOGLE_AI"]  ?: apiKeys["google"]
            ProviderType.GROQ        -> apiKeys["groq"]        ?: apiKeys["GROQ"]
            ProviderType.HUGGINGFACE -> apiKeys["huggingface"] ?: apiKeys["HUGGINGFACE"]
            ProviderType.OPENROUTER  -> apiKeys["openrouter"]  ?: apiKeys["OPENROUTER"]
            else                     -> null
        }
        return key?.takeIf { it.isNotBlank() }
    }
}
