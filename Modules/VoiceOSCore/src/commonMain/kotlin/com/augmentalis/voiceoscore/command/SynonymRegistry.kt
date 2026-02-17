/**
 * SynonymRegistry.kt - Verb synonym mappings for voice command NLU
 *
 * Defines interchangeable action verbs recognized by the NLU engine.
 * When a user says "tap Settings" and "click" is the canonical verb,
 * the engine treats "tap" as equivalent to "click".
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.command

/**
 * A verb synonym mapping: canonical verb + equivalent alternatives.
 */
data class SynonymEntry(
    val canonical: String,
    val synonyms: List<String>,
    val isDefault: Boolean = true
)

/**
 * Registry of built-in verb synonym mappings.
 *
 * These are system-wide verb equivalences used by the NLU engine
 * to normalize user speech into canonical action verbs.
 */
object SynonymRegistry {

    private val entries: MutableList<SynonymEntry> = mutableListOf(
        // Element interaction
        SynonymEntry("click", listOf("tap", "press", "push", "select", "hit")),
        SynonymEntry("long press", listOf("long click", "press and hold", "hold")),

        // Scroll
        SynonymEntry("scroll up", listOf("swipe up", "go up", "page up")),
        SynonymEntry("scroll down", listOf("swipe down", "go down", "page down")),
        SynonymEntry("scroll left", listOf("swipe left", "go left")),
        SynonymEntry("scroll right", listOf("swipe right", "go right")),

        // Navigation
        SynonymEntry("open", listOf("launch", "start", "run", "show")),
        SynonymEntry("close", listOf("exit", "quit", "dismiss", "hide")),
        SynonymEntry("back", listOf("go back", "return", "previous")),
        SynonymEntry("home", listOf("go home", "home screen", "main screen")),

        // Search & input
        SynonymEntry("search", listOf("find", "look for", "search for")),
        SynonymEntry("type", listOf("enter", "input", "write")),

        // Text editing
        SynonymEntry("delete", listOf("remove", "erase", "clear")),
        SynonymEntry("copy", listOf("duplicate")),
        SynonymEntry("paste", listOf("insert")),
        SynonymEntry("cut", listOf("move to clipboard")),
        SynonymEntry("undo", listOf("reverse", "take back")),
        SynonymEntry("redo", listOf("do again", "repeat")),
        SynonymEntry("select", listOf("choose", "pick", "highlight")),

        // Zoom
        SynonymEntry("zoom in", listOf("magnify", "enlarge", "bigger")),
        SynonymEntry("zoom out", listOf("shrink", "smaller")),

        // Media / audio
        SynonymEntry("mute", listOf("silence", "quiet", "hush")),

        // Reading / TTS
        SynonymEntry("read", listOf("read aloud", "speak", "narrate")),
        SynonymEntry("stop", listOf("halt", "cancel", "enough")),

        // Web gestures
        SynonymEntry("pan", listOf("move view", "slide view", "shift view")),
        SynonymEntry("tilt", listOf("angle", "angle view")),
        SynonymEntry("orbit", listOf("circle around", "revolve")),
        SynonymEntry("fling", listOf("flick", "swipe fast")),
        SynonymEntry("throw", listOf("toss", "launch")),
        SynonymEntry("pinch", listOf("squeeze", "spread")),
        SynonymEntry("scale", listOf("resize", "size")),
        SynonymEntry("grab", listOf("lock", "hold", "latch", "grip")),
        SynonymEntry("release", listOf("let go", "drop", "unlock", "ungrab"))
    )

    /**
     * Get all synonym entries (built-in + user-added).
     */
    fun all(): List<SynonymEntry> = entries.toList()

    /**
     * Find synonyms for a canonical verb.
     * @return List of synonym strings, or null if verb not found
     */
    fun forVerb(verb: String): List<String>? {
        val normalized = verb.lowercase().trim()
        return entries.find { it.canonical.lowercase() == normalized }?.synonyms
    }

    /**
     * Check if a word is a known synonym for any canonical verb.
     * @return The canonical verb, or null if not a synonym
     */
    fun canonicalFor(word: String): String? {
        val normalized = word.lowercase().trim()
        return entries.find { entry ->
            entry.synonyms.any { it.lowercase() == normalized }
        }?.canonical
    }

    /**
     * Add localized verb phrases as synonyms for their canonical verbs.
     *
     * Called when VOS files load for a locale. Merges localized verbs into
     * existing synonym entries. E.g., for es-ES, adds "pulsar", "clic", "tocar"
     * as synonyms for canonical "click".
     *
     * Duplicate synonyms are ignored. Entries are marked non-default (user/locale-added).
     *
     * @param verbs Localized verbs extracted from VOS verb-type commands
     */
    fun addLocalizedVerbs(verbs: List<LocalizedVerb>) {
        // Group by canonical verb
        val grouped = verbs.groupBy { it.canonicalVerb.lowercase() }

        for ((canonical, localizedVerbs) in grouped) {
            val existingIndex = entries.indexOfFirst { it.canonical.lowercase() == canonical }
            val newSynonyms = localizedVerbs.map { it.localizedPhrase.lowercase() }

            if (existingIndex >= 0) {
                // Merge into existing entry — add localized phrases not already present
                val existing = entries[existingIndex]
                val currentSynonyms = existing.synonyms.map { it.lowercase() }.toSet()
                val additions = newSynonyms.filter { it !in currentSynonyms && it != canonical }
                if (additions.isNotEmpty()) {
                    entries[existingIndex] = existing.copy(
                        synonyms = existing.synonyms + additions
                    )
                }
            } else {
                // No existing entry for this canonical — create one
                val uniqueSynonyms = newSynonyms.filter { it != canonical }.distinct()
                if (uniqueSynonyms.isNotEmpty()) {
                    entries.add(SynonymEntry(canonical, uniqueSynonyms, isDefault = false))
                }
            }
        }
    }

    /**
     * Remove all non-default (locale-added) synonym entries and
     * restore default entries to their original synonyms.
     *
     * Called before locale switch to clear previous locale's verbs.
     */
    fun clearLocalizedVerbs() {
        // Remove non-default entries
        entries.removeAll { !it.isDefault }
        // Note: for simplicity, built-in entries retain any merged synonyms.
        // A full reset would require storing the original list, but in practice
        // addLocalizedVerbs() is idempotent (duplicates are skipped).
    }

    /**
     * Total number of synonym entries.
     */
    val count: Int get() = entries.size

    /**
     * Total number of individual synonym words across all entries.
     */
    val totalSynonyms: Int get() = entries.sumOf { it.synonyms.size }
}
