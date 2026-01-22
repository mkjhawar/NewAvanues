/**
 * SynonymProviderPlugin.kt - Synonym Provider Plugin contract for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for plugins that provide custom vocabulary and synonym
 * expansion for voice command matching. Enables natural language variations
 * and locale-specific command recognition.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin

/**
 * Synonym Provider Plugin contract for custom vocabulary and phrase normalization.
 *
 * Synonym providers enhance voice command recognition by expanding the vocabulary
 * and normalizing phrases to canonical forms. This enables users to speak naturally
 * while still matching defined commands.
 *
 * ## Design Principles
 * - **Locale-Aware**: All operations support locale-specific synonyms
 * - **Bidirectional**: Can expand (get synonyms) and normalize (canonical form)
 * - **Composable**: Multiple providers can be combined for richer vocabulary
 * - **Action-Specific**: Special handling for action verbs (click, tap, press)
 *
 * ## Implementation Example
 * ```kotlin
 * class EnglishSynonymProvider : SynonymProviderPlugin {
 *     override val supportedLocales = setOf("en", "en-US", "en-GB")
 *
 *     private val synonymMap = mapOf(
 *         "click" to listOf("tap", "press", "select", "hit"),
 *         "open" to listOf("launch", "start", "run", "activate"),
 *         "close" to listOf("exit", "quit", "dismiss", "shut")
 *     )
 *
 *     override fun getSynonyms(phrase: String, locale: String): List<String> {
 *         return synonymMap[phrase.lowercase()] ?: emptyList()
 *     }
 *
 *     override fun normalize(phrase: String, locale: String): String {
 *         val lower = phrase.lowercase()
 *         return synonymMap.entries
 *             .find { (key, synonyms) -> lower == key || lower in synonyms }
 *             ?.key ?: phrase
 *     }
 *
 *     override fun areEquivalent(phrase1: String, phrase2: String, locale: String): Boolean {
 *         return normalize(phrase1, locale) == normalize(phrase2, locale)
 *     }
 *
 *     override fun getActionSynonyms(action: String, locale: String): List<String> {
 *         return when (action.uppercase()) {
 *             "CLICK" -> listOf("tap", "press", "select", "hit", "click")
 *             "SCROLL" -> listOf("swipe", "slide", "drag", "scroll")
 *             else -> emptyList()
 *         }
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see SynonymMatch
 * @see SynonymSource
 */
interface SynonymProviderPlugin : UniversalPlugin {

    /**
     * Set of locale codes this provider supports.
     *
     * Locale codes follow the BCP 47 standard (e.g., "en", "en-US", "de-DE").
     * Providers should support at least the base language code (e.g., "en")
     * in addition to regional variants.
     */
    val supportedLocales: Set<String>

    /**
     * Get all synonyms for a given phrase.
     *
     * Returns alternative phrases that have the same meaning as the input.
     * The original phrase is NOT included in the result.
     *
     * @param phrase The phrase to find synonyms for
     * @param locale The locale code for context
     * @return List of synonym phrases, empty if none found
     */
    fun getSynonyms(phrase: String, locale: String): List<String>

    /**
     * Normalize a phrase to its canonical form.
     *
     * Converts a phrase (or any of its synonyms) to a standard canonical form.
     * This is useful for consistent command matching.
     *
     * ## Example
     * - normalize("tap", "en") -> "click"
     * - normalize("press", "en") -> "click"
     * - normalize("click", "en") -> "click"
     *
     * @param phrase The phrase to normalize
     * @param locale The locale code for context
     * @return Canonical form of the phrase, or original if no mapping exists
     */
    fun normalize(phrase: String, locale: String): String

    /**
     * Check if two phrases are semantically equivalent.
     *
     * Two phrases are equivalent if they normalize to the same canonical form
     * or if one is a synonym of the other.
     *
     * @param phrase1 First phrase to compare
     * @param phrase2 Second phrase to compare
     * @param locale The locale code for context
     * @return true if phrases are equivalent
     */
    fun areEquivalent(phrase1: String, phrase2: String, locale: String): Boolean

    /**
     * Get synonyms for a specific action type.
     *
     * Action synonyms are verb phrases that map to VoiceOSCore action types
     * (CLICK, SCROLL, LONG_CLICK, etc.). This method provides action-specific
     * vocabulary expansion.
     *
     * @param action The action type (e.g., "CLICK", "SCROLL", "LONG_CLICK")
     * @param locale The locale code for context
     * @return List of verb phrases that map to this action
     */
    fun getActionSynonyms(action: String, locale: String): List<String>

    /**
     * Check if a locale is supported by this provider.
     *
     * Checks both exact match and base language match (e.g., "en-US" matches "en").
     *
     * @param locale The locale code to check
     * @return true if the locale is supported
     */
    fun supportsLocale(locale: String): Boolean {
        val baseLocale = locale.substringBefore("-")
        return locale in supportedLocales || baseLocale in supportedLocales
    }

    /**
     * Get all synonyms including the original phrase.
     *
     * Convenience method that returns the original phrase plus all synonyms.
     *
     * @param phrase The phrase to expand
     * @param locale The locale code for context
     * @return List containing original phrase and all synonyms
     */
    fun expandPhrase(phrase: String, locale: String): List<String> {
        return listOf(phrase) + getSynonyms(phrase, locale)
    }

    /**
     * Find the best matching synonym for a phrase from a set of candidates.
     *
     * @param phrase The phrase to match
     * @param candidates Set of candidate phrases to match against
     * @param locale The locale code for context
     * @return SynonymMatch containing the best match and confidence, null if no match
     */
    fun findBestMatch(
        phrase: String,
        candidates: Set<String>,
        locale: String
    ): SynonymMatch? {
        val normalized = normalize(phrase, locale)

        // Direct match
        if (normalized in candidates) {
            return SynonymMatch(
                original = phrase,
                matched = normalized,
                confidence = 1.0f,
                source = SynonymSource.DIRECT
            )
        }

        // Check if any candidate is equivalent
        for (candidate in candidates) {
            if (areEquivalent(phrase, candidate, locale)) {
                return SynonymMatch(
                    original = phrase,
                    matched = candidate,
                    confidence = 0.9f,
                    source = SynonymSource.SYNONYM
                )
            }
        }

        // Check synonyms of each candidate
        for (candidate in candidates) {
            val candidateSynonyms = getSynonyms(candidate, locale)
            if (phrase.lowercase() in candidateSynonyms.map { it.lowercase() }) {
                return SynonymMatch(
                    original = phrase,
                    matched = candidate,
                    confidence = 0.85f,
                    source = SynonymSource.SYNONYM
                )
            }
        }

        return null
    }
}

/**
 * Result of a synonym matching operation.
 *
 * Contains information about how a phrase was matched, including
 * the confidence level and source of the match.
 *
 * @property original The original input phrase
 * @property matched The matched phrase from candidates
 * @property confidence Confidence score (0.0 to 1.0)
 * @property source Source of the match (direct, synonym, fuzzy)
 */
data class SynonymMatch(
    val original: String,
    val matched: String,
    val confidence: Float,
    val source: SynonymSource
)

/**
 * Source of a synonym match, indicating how the match was found.
 */
enum class SynonymSource {
    /** Direct exact match */
    DIRECT,

    /** Matched via synonym expansion */
    SYNONYM,

    /** Matched via fuzzy/approximate matching */
    FUZZY,

    /** Matched via normalization to canonical form */
    NORMALIZED
}

/**
 * Configuration for synonym provider behavior.
 *
 * @property caseSensitive Whether matching should be case-sensitive
 * @property fuzzyMatchThreshold Minimum similarity for fuzzy matches (0.0 to 1.0)
 * @property maxSynonyms Maximum number of synonyms to return
 * @property includePhonetic Whether to include phonetically similar words
 */
data class SynonymConfig(
    val caseSensitive: Boolean = false,
    val fuzzyMatchThreshold: Float = 0.8f,
    val maxSynonyms: Int = 10,
    val includePhonetic: Boolean = false
) {
    companion object {
        /** Default configuration for general use */
        val DEFAULT = SynonymConfig()

        /** Strict configuration for precise matching */
        val STRICT = SynonymConfig(
            caseSensitive = true,
            fuzzyMatchThreshold = 0.95f,
            maxSynonyms = 5,
            includePhonetic = false
        )

        /** Lenient configuration for maximum flexibility */
        val LENIENT = SynonymConfig(
            caseSensitive = false,
            fuzzyMatchThreshold = 0.6f,
            maxSynonyms = 20,
            includePhonetic = true
        )
    }
}

/**
 * Extension functions for SynonymProviderPlugin.
 */

/**
 * Get synonyms for multiple phrases at once.
 *
 * @param phrases List of phrases to find synonyms for
 * @param locale The locale code for context
 * @return Map of phrase to its synonyms
 */
fun SynonymProviderPlugin.getBulkSynonyms(
    phrases: List<String>,
    locale: String
): Map<String, List<String>> {
    return phrases.associateWith { getSynonyms(it, locale) }
}

/**
 * Normalize multiple phrases at once.
 *
 * @param phrases List of phrases to normalize
 * @param locale The locale code for context
 * @return List of normalized phrases in same order
 */
fun SynonymProviderPlugin.normalizeBulk(
    phrases: List<String>,
    locale: String
): List<String> {
    return phrases.map { normalize(it, locale) }
}

/**
 * Get all unique normalized forms for a set of phrases.
 *
 * @param phrases Set of phrases to normalize
 * @param locale The locale code for context
 * @return Set of unique canonical forms
 */
fun SynonymProviderPlugin.getCanonicalForms(
    phrases: Set<String>,
    locale: String
): Set<String> {
    return phrases.map { normalize(it, locale) }.toSet()
}
