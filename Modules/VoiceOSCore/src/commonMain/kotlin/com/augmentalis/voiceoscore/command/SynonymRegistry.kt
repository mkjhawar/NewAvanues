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
        SynonymEntry("stop", listOf("halt", "cancel", "enough"))
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
     * Total number of synonym entries.
     */
    val count: Int get() = entries.size

    /**
     * Total number of individual synonym words across all entries.
     */
    val totalSynonyms: Int get() = entries.sumOf { it.synonyms.size }
}
