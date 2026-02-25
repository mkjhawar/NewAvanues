/**
 * ISynonymProvider.kt - Abstraction for synonym lookup with NLM hook
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 *
 * Interface for synonym providers, allowing future NLM integration.
 */
package com.augmentalis.voiceoscore

/**
 * Provider interface for synonym lookup.
 *
 * Allows plugging in different synonym sources:
 * - Static synonym packs (.syn/.qsyn files)
 * - NLM-based semantic matching
 * - Hybrid approaches
 *
 * Usage:
 * ```kotlin
 * // Static provider (default)
 * val provider = StaticSynonymProvider(loader)
 * provider.getCanonical("tap", "en")  // Returns "click"
 *
 * // Future NLM provider
 * val nlmProvider = NlmSynonymProvider(staticProvider, nlmEngine)
 * nlmProvider.getCanonical("push button", "en")  // Uses semantic matching
 * ```
 */
interface ISynonymProvider {

    /**
     * Get the canonical action for a word/phrase.
     *
     * @param word The word or phrase to look up
     * @param language ISO 639-1 language code
     * @return Canonical action, or null if no mapping found
     */
    fun getCanonical(word: String, language: String): String?

    /**
     * Get all synonyms for a canonical action.
     *
     * @param canonical The canonical action
     * @param language ISO 639-1 language code
     * @return List of synonyms, or empty list if not found
     */
    fun getSynonyms(canonical: String, language: String): List<String>

    /**
     * Expand a phrase by replacing synonyms with canonical actions.
     *
     * @param phrase The phrase to expand
     * @param language ISO 639-1 language code
     * @return Expanded phrase
     */
    fun expand(phrase: String, language: String): String

    /**
     * Check if NLM-based resolution is available.
     *
     * @return true if NLM can be used for ambiguous cases
     */
    fun isNlmAvailable(): Boolean = false

    /**
     * Resolve ambiguous input using NLM.
     *
     * Only called when [isNlmAvailable] returns true.
     *
     * @param input The ambiguous input
     * @param candidates List of possible canonical actions
     * @param language ISO 639-1 language code
     * @return Best matching canonical action, or null
     */
    suspend fun nlmResolve(
        input: String,
        candidates: List<String>,
        language: String
    ): String? = null

    /**
     * Get supported languages.
     *
     * @return List of ISO 639-1 language codes
     */
    fun getSupportedLanguages(): List<String>

    /**
     * Check if a language is supported.
     *
     * @param language ISO 639-1 language code
     * @return true if language has synonym mappings
     */
    fun isLanguageSupported(language: String): Boolean =
        language in getSupportedLanguages()
}

/**
 * Static synonym provider using loaded SynonymMaps.
 *
 * Default implementation that uses pre-loaded synonym packs.
 */
class StaticSynonymProvider(
    private val loader: SynonymLoader
) : ISynonymProvider {

    private val cache = mutableMapOf<String, SynonymMap?>()

    override fun getCanonical(word: String, language: String): String? {
        return getMap(language)?.getCanonical(word)
    }

    override fun getSynonyms(canonical: String, language: String): List<String> {
        return getMap(language)?.getSynonyms(canonical) ?: emptyList()
    }

    override fun expand(phrase: String, language: String): String {
        return getMap(language)?.expandWithMultiWord(phrase) ?: phrase
    }

    override fun getSupportedLanguages(): List<String> {
        return loader.getAvailableLanguages()
    }

    private fun getMap(language: String): SynonymMap? {
        return cache.getOrPut(language) {
            loader.load(language)
        }
    }

    /**
     * Clear the cache (call when synonym packs are updated).
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Preload synonyms for specified languages.
     */
    fun preload(languages: List<String>) {
        languages.forEach { getMap(it) }
    }
}

/**
 * Composite provider that combines static and NLM-based resolution.
 *
 * Usage:
 * ```kotlin
 * val provider = CompositeSynonymProvider(
 *     staticProvider = StaticSynonymProvider(loader),
 *     nlmResolver = { input, candidates, lang -> nlmEngine.resolve(input, candidates) }
 * )
 * ```
 */
class CompositeSynonymProvider(
    private val staticProvider: ISynonymProvider,
    private val nlmResolver: (suspend (String, List<String>, String) -> String?)? = null
) : ISynonymProvider {

    override fun getCanonical(word: String, language: String): String? {
        return staticProvider.getCanonical(word, language)
    }

    override fun getSynonyms(canonical: String, language: String): List<String> {
        return staticProvider.getSynonyms(canonical, language)
    }

    override fun expand(phrase: String, language: String): String {
        return staticProvider.expand(phrase, language)
    }

    override fun isNlmAvailable(): Boolean = nlmResolver != null

    override suspend fun nlmResolve(
        input: String,
        candidates: List<String>,
        language: String
    ): String? {
        return nlmResolver?.invoke(input, candidates, language)
    }

    override fun getSupportedLanguages(): List<String> {
        return staticProvider.getSupportedLanguages()
    }
}
