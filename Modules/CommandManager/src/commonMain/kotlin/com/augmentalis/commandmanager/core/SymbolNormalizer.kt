package com.augmentalis.commandmanager

/**
 * Symbol Normalizer - Converts special characters to speech-friendly equivalents.
 *
 * Supports:
 * - Universal aliases that work in all locales (e.g., `&` → "and")
 * - Locale-specific alternatives loaded from resources
 * - Multiple aliases per symbol (e.g., `#` → "pound", "hash", "number")
 * - Bidirectional matching (symbol → spoken and spoken → symbol)
 *
 * ## Usage:
 * ```kotlin
 * // Normalize for command generation (symbol → spoken)
 * val label = SymbolNormalizer.normalize("Sound & vibration") // "Sound and vibration"
 *
 * // Check if voice input matches (supports any alias)
 * val matches = SymbolNormalizer.matchesAlias("and", "&") // true
 * val matches2 = SymbolNormalizer.matchesAlias("pound", "#") // true
 *
 * // Get all aliases for fuzzy matching
 * val aliases = SymbolNormalizer.getAliases("#") // ["pound", "hash", "number"]
 * ```
 */
object SymbolNormalizer {

    /**
     * Universal symbol aliases (work in all locales).
     * Each symbol maps to a list of spoken equivalents.
     * The FIRST alias is used for normalization; ALL are used for matching.
     */
    private val universalAliases = mapOf(
        // Logical operators
        "&" to listOf("and", "ampersand"),
        "+" to listOf("plus"),
        "/" to listOf("slash", "or", "divided by"),
        "|" to listOf("or", "pipe"),

        // Common symbols
        "#" to listOf("pound", "hash", "number", "hashtag"),
        "@" to listOf("at", "at the rate of"),
        "%" to listOf("percent", "percentage"),
        "*" to listOf("star", "asterisk", "times"),
        "!" to listOf("exclamation", "bang"),
        "?" to listOf("question", "question mark"),

        // Currency
        "$" to listOf("dollar", "dollars"),
        "€" to listOf("euro", "euros"),
        "£" to listOf("pound", "pounds", "sterling"),
        "¥" to listOf("yen", "yuan"),
        "₹" to listOf("rupee", "rupees"),

        // Math operators
        "=" to listOf("equals", "is", "equal to"),
        "-" to listOf("minus", "dash", "hyphen"),
        "×" to listOf("times", "multiplied by"),
        "÷" to listOf("divided by", "over"),
        "^" to listOf("caret", "power", "to the power of"),

        // Punctuation
        ":" to listOf("colon"),
        ";" to listOf("semicolon"),
        "_" to listOf("underscore"),
        "." to listOf("dot", "period", "point"),
        "," to listOf("comma"),
        "'" to listOf("apostrophe", "single quote"),
        "\"" to listOf("quote", "double quote"),

        // Brackets
        "(" to listOf("open paren", "left paren", "open parenthesis"),
        ")" to listOf("close paren", "right paren", "close parenthesis"),
        "[" to listOf("open bracket", "left bracket"),
        "]" to listOf("close bracket", "right bracket"),
        "{" to listOf("open brace", "left brace"),
        "}" to listOf("close brace", "right brace"),
        "<" to listOf("less than", "left angle"),
        ">" to listOf("greater than", "right angle"),

        // Technical
        "\\" to listOf("backslash"),
        "~" to listOf("tilde"),
        "`" to listOf("backtick", "grave")
    )

    /**
     * Locale-specific overrides loaded from resources.
     * Key: locale code (e.g., "es", "de", "fr", "hi")
     *
     * Locale aliases EXTEND universal aliases, allowing region-specific
     * spoken forms while preserving universal fallbacks.
     */
    private val localeAliases = mutableMapOf<String, Map<String, List<String>>>()

    /**
     * Reverse lookup: spoken word → symbol.
     * Populated from both universal and locale aliases.
     * Used for bidirectional matching.
     */
    private val spokenToSymbol = mutableMapOf<String, String>()

    init {
        // Build reverse lookup from universal aliases
        rebuildReverseLookup()
    }

    /**
     * Initialize locale-specific aliases from resources.
     * Call this during app initialization with locale data.
     *
     * @param locale Locale code (e.g., "es", "de", "fr")
     * @param aliases Map of symbol → list of spoken alternatives for this locale
     */
    fun initLocale(locale: String, aliases: Map<String, List<String>>) {
        localeAliases[locale] = aliases
        rebuildReverseLookup()
    }

    /**
     * Rebuild reverse lookup after locale changes.
     */
    private fun rebuildReverseLookup() {
        spokenToSymbol.clear()

        // Add universal aliases
        universalAliases.forEach { (symbol, aliases) ->
            aliases.forEach { spoken ->
                spokenToSymbol[spoken.lowercase()] = symbol
            }
        }

        // Add locale-specific aliases (may override universal)
        localeAliases.values.forEach { localeMap ->
            localeMap.forEach { (symbol, aliases) ->
                aliases.forEach { spoken ->
                    spokenToSymbol[spoken.lowercase()] = symbol
                }
            }
        }
    }

    /**
     * Normalize text by replacing symbols with primary spoken equivalent.
     * Used during command generation to create speech-friendly phrases.
     *
     * @param text Original text with symbols (e.g., "Display size & text")
     * @param locale Locale code for locale-specific normalization (default: "en")
     * @return Normalized text (e.g., "Display size and text")
     */
    fun normalize(text: String, locale: String = "en"): String {
        var result = text

        // Get aliases for this locale, falling back to universal
        val aliasMap = localeAliases[locale] ?: universalAliases

        aliasMap.forEach { (symbol, spoken) ->
            // Use the first (primary) spoken form for normalization
            val primary = spoken.firstOrNull() ?: return@forEach
            // Replace symbol with spoken form, adding spaces for word boundaries
            result = result.replace(symbol, " $primary ")
        }

        // Also process universal aliases not in locale map
        if (localeAliases.containsKey(locale)) {
            universalAliases.forEach { (symbol, spoken) ->
                if (!aliasMap.containsKey(symbol)) {
                    val primary = spoken.firstOrNull() ?: return@forEach
                    result = result.replace(symbol, " $primary ")
                }
            }
        }

        // Clean up multiple spaces and trim
        return result.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Get all aliases for a symbol (for fuzzy matching).
     * Includes both universal and locale-specific aliases.
     *
     * @param symbol The symbol to look up (e.g., "#")
     * @param locale Locale code for locale-specific aliases
     * @return List of spoken alternatives (e.g., ["pound", "hash", "number"])
     */
    fun getAliases(symbol: String, locale: String = "en"): List<String> {
        val localeSpecific = localeAliases[locale]?.get(symbol) ?: emptyList()
        val universal = universalAliases[symbol] ?: emptyList()

        // Combine locale-specific (priority) with universal (fallback)
        return (localeSpecific + universal).distinct()
    }

    /**
     * Get all known symbols that have aliases.
     */
    fun getAllSymbols(): Set<String> {
        return universalAliases.keys + localeAliases.values.flatMap { it.keys }
    }

    /**
     * Check if spoken text matches any alias for a symbol.
     * Used for bidirectional matching during voice input processing.
     *
     * @param spoken The spoken word (e.g., "and", "pound")
     * @param symbol The symbol to match against (e.g., "&", "#")
     * @param locale Locale code for locale-specific matching
     * @return True if spoken matches any alias for the symbol
     */
    fun matchesAlias(spoken: String, symbol: String, locale: String = "en"): Boolean {
        val normalizedSpoken = spoken.lowercase().trim()
        val aliases = getAliases(symbol, locale)
        return aliases.any { it.lowercase() == normalizedSpoken }
    }

    /**
     * Find the symbol for a spoken word (reverse lookup).
     * Used to convert voice input back to symbols if needed.
     *
     * @param spoken The spoken word (e.g., "and", "pound")
     * @return The corresponding symbol, or null if not found
     */
    fun findSymbolForSpoken(spoken: String): String? {
        return spokenToSymbol[spoken.lowercase().trim()]
    }

    /**
     * Normalize both the input and phrase for comparison.
     * Used for matching voice input against command phrases.
     *
     * @param voiceInput User's voice input
     * @param phrase Command phrase to match against
     * @param locale Locale for normalization
     * @return Pair of (normalizedInput, normalizedPhrase) for comparison
     */
    fun normalizeForComparison(
        voiceInput: String,
        phrase: String,
        locale: String = "en"
    ): Pair<String, String> {
        return Pair(
            normalize(voiceInput, locale).lowercase(),
            normalize(phrase, locale).lowercase()
        )
    }

    /**
     * Check if voice input matches a phrase, considering symbol aliases.
     * Handles both directions:
     * - User says "sound and vibration" → matches "Sound & vibration"
     * - User says "display size ampersand text" → also matches
     *
     * @param voiceInput User's voice input
     * @param phrase Command phrase to match
     * @param locale Locale for matching
     * @return True if inputs are equivalent after normalization
     */
    fun matchWithAliases(
        voiceInput: String,
        phrase: String,
        locale: String = "en"
    ): Boolean {
        val (normalizedInput, normalizedPhrase) = normalizeForComparison(voiceInput, phrase, locale)
        return normalizedInput == normalizedPhrase
    }

    /**
     * Generate all possible phrase variations for speech engine grammar.
     * This allows the speech engine to recognize any alias form.
     *
     * @param phrase Original phrase (e.g., "Sound & vibration")
     * @param locale Locale for aliases
     * @return List of phrase variations (e.g., ["Sound and vibration", "Sound & vibration"])
     */
    fun generatePhraseVariations(phrase: String, locale: String = "en"): List<String> {
        val variations = mutableSetOf<String>()

        // Add normalized version (primary)
        variations.add(normalize(phrase, locale))

        // Add original (in case it's different)
        variations.add(phrase)

        // For each symbol in the phrase, generate variations with each alias
        getAllSymbols().forEach { symbol ->
            if (phrase.contains(symbol)) {
                getAliases(symbol, locale).forEach { alias ->
                    val variation = phrase.replace(symbol, " $alias ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    variations.add(variation)
                }
            }
        }

        return variations.toList()
    }

    /**
     * Check if text contains any symbols that should be normalized.
     */
    fun containsSymbols(text: String): Boolean {
        return getAllSymbols().any { text.contains(it) }
    }

    /**
     * Get statistics about symbol usage (for debugging).
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "universalSymbols" to universalAliases.size,
            "locales" to localeAliases.keys.toList(),
            "totalSpokenMappings" to spokenToSymbol.size
        )
    }
}
