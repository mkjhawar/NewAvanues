/**
 * SynonymMap.kt - Core synonym lookup data structure
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 *
 * Provides bidirectional synonym lookup:
 * - word → canonical action
 * - canonical → all synonyms
 */
package com.augmentalis.voiceoscore

/**
 * Core synonym map providing fast bidirectional lookup.
 *
 * Usage:
 * ```kotlin
 * val map = SynonymMap.Builder("en")
 *     .add("click", listOf("tap", "press", "push"))
 *     .add("scroll_up", listOf("swipe up", "go up"))
 *     .build()
 *
 * map.getCanonical("tap")       // Returns "click"
 * map.getSynonyms("click")      // Returns ["tap", "press", "push"]
 * map.expand("tap settings")    // Returns "click settings"
 * ```
 *
 * @property metadata Language metadata for this synonym map
 * @property entries All synonym entries in this map
 */
class SynonymMap private constructor(
    val metadata: LanguageMetadata,
    private val entries: List<SynonymEntry>,
    private val wordToCanonical: Map<String, String>,
    private val canonicalToSynonyms: Map<String, List<String>>
) {
    /**
     * Number of canonical actions in this map.
     */
    val size: Int get() = entries.size

    /**
     * Language code for this map.
     */
    val languageCode: String get() = metadata.languageCode

    /**
     * Get the canonical action for a word.
     *
     * @param word The word to look up (case-insensitive)
     * @return Canonical action, or null if not found
     */
    fun getCanonical(word: String): String? {
        return wordToCanonical[word.lowercase().trim()]
    }

    /**
     * Get all synonyms for a canonical action.
     *
     * @param canonical The canonical action
     * @return List of synonyms, or empty list if not found
     */
    fun getSynonyms(canonical: String): List<String> {
        return canonicalToSynonyms[canonical.lowercase().trim()] ?: emptyList()
    }

    /**
     * Check if a word has a synonym mapping.
     */
    fun hasMapping(word: String): Boolean {
        return wordToCanonical.containsKey(word.lowercase().trim())
    }

    /**
     * Expand a phrase by replacing synonyms with canonical actions.
     *
     * Example: "tap submit button" → "click submit button"
     *
     * @param phrase The phrase to expand
     * @return Phrase with synonyms replaced by canonical actions
     */
    fun expand(phrase: String): String {
        val words = phrase.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            getCanonical(word) ?: word
        }
    }

    /**
     * Expand a phrase, also handling multi-word synonyms.
     *
     * Example: "long press submit" → "long_click submit"
     *
     * @param phrase The phrase to expand
     * @return Phrase with synonyms replaced by canonical actions
     */
    fun expandWithMultiWord(phrase: String): String {
        var result = phrase.lowercase().trim()

        // Try multi-word synonyms first (longer matches take priority)
        val sortedByLength = wordToCanonical.entries
            .filter { it.key.contains(" ") }
            .sortedByDescending { it.key.length }

        for ((synonym, canonical) in sortedByLength) {
            if (result.contains(synonym)) {
                result = result.replace(synonym, canonical)
            }
        }

        // Then single-word synonyms
        val words = result.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            if (!word.contains("_")) { // Don't re-expand canonical actions
                getCanonical(word) ?: word
            } else {
                word
            }
        }
    }

    /**
     * Get all canonical actions in this map.
     */
    fun getAllCanonicals(): List<String> {
        return entries.map { it.canonical }
    }

    /**
     * Get all entries in this map.
     */
    fun getAllEntries(): List<SynonymEntry> {
        return entries.toList()
    }

    /**
     * Builder for creating SynonymMap instances.
     */
    class Builder(
        private val languageCode: String,
        private var metadata: LanguageMetadata? = null
    ) {
        private val entries = mutableListOf<SynonymEntry>()

        /**
         * Set language metadata.
         */
        fun metadata(meta: LanguageMetadata) = apply {
            this.metadata = meta
        }

        /**
         * Add a synonym entry.
         *
         * @param canonical The canonical action name
         * @param synonyms List of synonyms for this action
         */
        fun add(canonical: String, synonyms: List<String>) = apply {
            entries.add(SynonymEntry(canonical.lowercase(), synonyms.map { it.lowercase() }))
        }

        /**
         * Add a synonym entry.
         *
         * @param canonical The canonical action name
         * @param synonyms Vararg synonyms for this action
         */
        fun add(canonical: String, vararg synonyms: String) = apply {
            add(canonical, synonyms.toList())
        }

        /**
         * Add a pre-built SynonymEntry.
         */
        fun add(entry: SynonymEntry) = apply {
            entries.add(entry)
        }

        /**
         * Add all entries from another SynonymMap (for merging).
         */
        fun addAll(other: SynonymMap) = apply {
            entries.addAll(other.entries)
        }

        /**
         * Build the SynonymMap.
         */
        fun build(): SynonymMap {
            val meta = metadata ?: LanguageMetadata.forLanguage(languageCode)

            // Build word→canonical index
            val wordToCanonical = mutableMapOf<String, String>()
            for (entry in entries) {
                // Map canonical to itself
                wordToCanonical[entry.canonical] = entry.canonical

                // Map each synonym to canonical
                for (synonym in entry.synonyms) {
                    wordToCanonical[synonym] = entry.canonical
                }
            }

            // Build canonical→synonyms index
            val canonicalToSynonyms = entries.associate { entry ->
                entry.canonical to entry.synonyms
            }

            return SynonymMap(
                metadata = meta,
                entries = entries.toList(),
                wordToCanonical = wordToCanonical,
                canonicalToSynonyms = canonicalToSynonyms
            )
        }
    }

    companion object {
        /**
         * Create an empty SynonymMap for a language.
         */
        fun empty(languageCode: String): SynonymMap {
            return Builder(languageCode).build()
        }

        /**
         * Create a SynonymMap from a list of entries.
         */
        fun fromEntries(languageCode: String, entries: List<SynonymEntry>): SynonymMap {
            val builder = Builder(languageCode)
            entries.forEach { builder.add(it) }
            return builder.build()
        }
    }
}
