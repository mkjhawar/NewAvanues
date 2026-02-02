package com.augmentalis.voiceoscore

/**
 * Filter File Loader - Loads garbage words and navigation icons from AVU files.
 *
 * Loads filter data from external AVU files stored in assets, allowing updates
 * without recompiling. Supports tiered loading (custom -> downloaded -> built-in).
 *
 * AVU Format for Filters:
 * - GWD:word:description - Garbage word (repetitive)
 * - GEX:text:description - Garbage exact match
 * - ICN:label:category:description - Navigation icon label
 *
 * @since 2026-02-02
 */
object FilterFileLoader {

    private const val DELIMITER = ":"

    // Cached filter data per locale
    private val repetitiveWordsCache = mutableMapOf<String, Set<String>>()
    private val exactGarbageCache = mutableMapOf<String, Set<String>>()
    private val navigationIconsCache = mutableMapOf<String, Set<String>>()

    // Built-in fallback (minimal set for when files are missing)
    private val FALLBACK_REPETITIVE_WORDS = setOf(
        "comma", "dot", "dash", "space", "tab", "enter",
        "null", "undefined", "nan", "true", "false"
    )

    private val FALLBACK_EXACT_GARBAGE = setOf(
        "undefined", "null", "nan", "NaN", "NULL",
        "[object object]", "[Object object]",
        "function", "error", "exception",
        "...", "---", "___", "true", "false", ""
    )

    private val FALLBACK_NAVIGATION_ICONS = setOf(
        "menu", "more", "options", "settings", "back", "forward",
        "home", "close", "refresh", "search", "filter", "sort",
        "add", "edit", "delete", "save", "cancel", "share",
        "send", "download", "upload", "copy", "paste",
        "call", "video", "camera", "mic", "mute",
        "play", "pause", "stop", "skip", "previous", "next",
        "star", "favorite", "bookmark", "help", "info"
    )

    /**
     * Load filter data from AVU file content.
     * Can be called from platform-specific code that reads the actual files.
     *
     * @param avuContent The AVU file content as a string
     * @param locale The locale code (e.g., "en-US")
     */
    fun loadFromAvuContent(avuContent: String, locale: String) {
        val langCode = locale.take(2).lowercase()

        val repetitiveWords = mutableSetOf<String>()
        val exactGarbage = mutableSetOf<String>()
        val navIcons = mutableSetOf<String>()

        avuContent.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("---") }
            .forEach { line ->
                val parts = splitAvu(line)
                if (parts.size >= 2) {
                    when (parts[0].uppercase()) {
                        "GWD" -> repetitiveWords.add(parts[1].lowercase())
                        "GEX" -> exactGarbage.add(parts[1])
                        "ICN" -> navIcons.add(parts[1].lowercase())
                    }
                }
            }

        // Cache the loaded data
        if (repetitiveWords.isNotEmpty()) {
            repetitiveWordsCache[langCode] = repetitiveWords
        }
        if (exactGarbage.isNotEmpty()) {
            exactGarbageCache[langCode] = exactGarbage
        }
        if (navIcons.isNotEmpty()) {
            navigationIconsCache[langCode] = navIcons
        }
    }

    /**
     * Get repetitive words for a locale.
     * Falls back to English, then to built-in fallback.
     *
     * @param locale The locale code (e.g., "en", "de", "en-US")
     * @return Set of repetitive words to filter
     */
    fun getRepetitiveWords(locale: String = "en"): Set<String> {
        val langCode = locale.take(2).lowercase()
        return repetitiveWordsCache[langCode]
            ?: repetitiveWordsCache["en"]
            ?: FALLBACK_REPETITIVE_WORDS
    }

    /**
     * Get exact garbage strings for a locale.
     * Falls back to English, then to built-in fallback.
     *
     * @param locale The locale code
     * @return Set of exact garbage strings to filter
     */
    fun getExactGarbage(locale: String = "en"): Set<String> {
        val langCode = locale.take(2).lowercase()
        return exactGarbageCache[langCode]
            ?: exactGarbageCache["en"]
            ?: FALLBACK_EXACT_GARBAGE
    }

    /**
     * Get navigation icon labels for a locale.
     * Returns combined set of current locale + English for better coverage.
     *
     * @param locale The locale code
     * @return Set of valid single-word icon labels
     */
    fun getNavigationIcons(locale: String = "en"): Set<String> {
        val langCode = locale.take(2).lowercase()
        val localized = navigationIconsCache[langCode] ?: emptySet()
        val english = navigationIconsCache["en"] ?: FALLBACK_NAVIGATION_ICONS
        return localized + english
    }

    /**
     * Check if filter data has been loaded for a locale.
     */
    fun isLoaded(locale: String): Boolean {
        val langCode = locale.take(2).lowercase()
        return repetitiveWordsCache.containsKey(langCode) ||
                navigationIconsCache.containsKey(langCode)
    }

    /**
     * Get list of loaded locales.
     */
    fun getLoadedLocales(): Set<String> {
        return (repetitiveWordsCache.keys + navigationIconsCache.keys).toSet()
    }

    /**
     * Clear all cached data (useful for testing or reloading).
     */
    fun clearCache() {
        repetitiveWordsCache.clear()
        exactGarbageCache.clear()
        navigationIconsCache.clear()
    }

    /**
     * Split AVU line by delimiter, respecting escaped delimiters.
     */
    private fun splitAvu(avu: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0

        while (i < avu.length) {
            when {
                // Check for escaped delimiter (%3A)
                i + 2 < avu.length && avu.substring(i, i + 3) == "%3A" -> {
                    current.append(":")
                    i += 3
                }
                avu[i] == ':' -> {
                    result.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(avu[i])
                    i++
                }
            }
        }
        result.add(current.toString())
        return result
    }
}
