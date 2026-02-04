package com.augmentalis.voiceoscore

/**
 * Command Generator Helpers - Shared utility functions for command generation.
 *
 * Contains common functions used by CommandGenerator, ListCommandGenerator,
 * and IconCommandGenerator including:
 * - Garbage text filtering
 * - Label cleaning and normalization
 * - Element hash generation
 * - AVID generation
 *
 * @since 2026-02-02
 */
object CommandGeneratorHelpers {

    private val PARSE_DESCRIPTION_DELIMITERS = listOf(":", "|", ",", ".")

    /**
     * Language-agnostic garbage patterns (work for any locale)
     */
    private val GARBAGE_PATTERNS = listOf(
        // CSS class-like patterns: "btn-primary", "flex-row"
        Regex("^[a-z]+(-[a-z]+){2,}$", RegexOption.IGNORE_CASE),
        // Base64/hash-like strings (long alphanumeric without spaces)
        Regex("^[A-Za-z0-9+/=]{20,}$"),
        // Hex strings
        Regex("^(0x)?[a-f0-9]{8,}$", RegexOption.IGNORE_CASE),
        // Just punctuation or whitespace (language-agnostic)
        Regex("^[\\s\\p{Punct}]+$"),
        // UUID-like patterns
        Regex("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$", RegexOption.IGNORE_CASE),
        // Package names (com.example.app)
        Regex("^[a-z]+\\.[a-z]+\\.[a-z]+", RegexOption.IGNORE_CASE),
        // Object toString patterns: [object Object], Object@hash
        Regex("^\\[?object\\s*\\w*\\]?$|^\\w+@[a-f0-9]+$", RegexOption.IGNORE_CASE)
    )

    /**
     * Check if text is garbage that should not be a voice command.
     *
     * @param text The text to check
     * @param locale The locale for language-specific garbage detection (default: "en")
     * @return true if the text is garbage and should be filtered out
     */
    fun isGarbageText(text: String, locale: String = "en"): Boolean {
        val trimmed = text.trim()

        // Empty or very short (single char)
        if (trimmed.length <= 1) return true

        // Exact matches (loaded from AVU files)
        val exactGarbage = FilterFileLoader.getExactGarbage(locale)
        if (exactGarbage.any { it.equals(trimmed, ignoreCase = true) }) return true

        // Pattern matches (language-agnostic patterns)
        if (GARBAGE_PATTERNS.any { it.matches(trimmed) }) return true

        // Detect repetitive words: "comma comma com", "dot dot"
        val repetitiveWords = FilterFileLoader.getRepetitiveWords(locale)
        val words = trimmed.lowercase().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        if (words.size >= 2) {
            val firstWord = words[0]
            // If most words start with the same prefix (like "comma, comma, com")
            val samePrefix = words.count { it.startsWith(firstWord.take(3)) }
            if (samePrefix >= 2 && repetitiveWords.any { firstWord.startsWith(it.take(3)) }) {
                return true
            }
        }

        return false
    }

    /**
     * Clean and validate label text for voice commands.
     * Returns null if the text is garbage.
     *
     * @param text Raw label text
     * @param locale The locale for garbage detection (default: "en")
     * @return Cleaned text or null if garbage
     */
    fun cleanLabel(text: String, locale: String = "en"): String? {
        val trimmed = text.trim()

        // Filter garbage
        if (isGarbageText(trimmed, locale)) return null

        // Truncate very long text (likely not a button label)
        val maxLength = 50
        return if (trimmed.length > maxLength) {
            trimmed.take(maxLength).substringBeforeLast(" ") + "..."
        } else {
            trimmed
        }
    }

    /**
     * Derive a stable element hash for database FK reference.
     * Uses the same logic as generateAvid's elementHash for consistency.
     */
    fun deriveElementHash(element: ElementInfo): String {
        val hashInput = when {
            element.resourceId.isNotBlank() -> element.resourceId
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.text.isNotBlank() -> element.text
            else -> "${element.className}:${element.bounds}"
        }
        // Return a truncated hash suitable for database storage
        return hashInput.hashCode().toUInt().toString(16).padStart(8, '0')
    }

    /**
     * Generate element fingerprint for targeting.
     * Format: {TypeCode}:{hash8} e.g., "BTN:a3f2e1c9"
     */
    fun generateAvid(element: ElementInfo, packageName: String): String {
        return ElementFingerprint.fromElementInfo(element, packageName)
    }

    /**
     * Normalizes a RealWear ML script string by trimming the input around the first supported delimiter.
     *
     * This function searches for the first delimiter (in the order defined by PARSE_DESCRIPTION_DELIMITERS)
     * that appears in [text]. If a delimiter is found, the input is split into exactly two parts.
     *
     * Selection logic:
     * - If the original [text] contains "hf_", returns the substring *after* the first delimiter.
     * - Otherwise, returns the substring *before* the first delimiter.
     *
     * @param text Raw ML script or description text to normalize.
     * @return A normalized string segment based on the delimiter and "hf_" presence rules.
     */
    fun normalizeRealWearMlScript(text: String): String {
        // Find the first delimiter from our list that exists in the current processedText
        val foundDelimiter = PARSE_DESCRIPTION_DELIMITERS.firstOrNull { text.contains(it) }

        if (foundDelimiter != null) {
            // If a delimiter is found, split the string.
            // limit = 2 ensures we get exactly two parts if the delimiter is present.
            val parts = text.split(foundDelimiter, limit = 2)

            val normalizedText = if ("hf_" in text) {
                // If "hf_" is present, take the part *after* the first delimiter
                parts.getOrElse(1) { parts[0] }
            } else {
                // If "hf_" is NOT present, take the part *before* the first delimiter
                parts[0]
            }
            return normalizedText
        }
        return text
    }

    /**
     * Derive the best label for voice recognition from element properties.
     * Normalizes special characters to speech-friendly equivalents.
     * Filters out garbage text that shouldn't be voice commands.
     *
     * @param element Source element
     * @param locale Locale for symbol normalization (default: "en")
     * @return Normalized label suitable for voice recognition, empty if garbage
     */
    fun deriveLabel(element: ElementInfo, locale: String = "en"): String {
        val rawLabel = when {
            element.text.isNotBlank() -> element.text
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.resourceId.isNotBlank() -> {
                element.resourceId
                    .substringAfterLast("/")
                    .replace("_", " ")
                    .replace("-", " ")
            }
            else -> ""
        }

        // Early garbage check
        if (isGarbageText(rawLabel)) return ""

        val normalized = normalizeRealWearMlScript(rawLabel)

        // Check garbage after normalization
        if (isGarbageText(normalized)) return ""

        // Normalize special characters (e.g., "&" → "and", "#" → "pound")
        val result = if (SymbolNormalizer.containsSymbols(normalized)) {
            SymbolNormalizer.normalize(normalized, locale)
        } else {
            normalized
        }

        // Final cleanup - return empty if still garbage
        return cleanLabel(result) ?: ""
    }

    /**
     * Derive action type based on element properties.
     */
    fun deriveActionType(element: ElementInfo): CommandActionType {
        val className = element.className.lowercase()
        return when {
            className.contains("edittext") || className.contains("textfield") -> CommandActionType.TYPE
            className.contains("checkbox") || className.contains("switch") -> CommandActionType.CLICK
            className.contains("button") -> CommandActionType.CLICK
            element.isScrollable -> CommandActionType.CLICK
            element.isClickable -> CommandActionType.CLICK
            else -> CommandActionType.CLICK
        }
    }

    /**
     * Calculate confidence score based on element identifiers.
     * Higher confidence for elements with more identifying information.
     */
    fun calculateConfidence(element: ElementInfo): Float {
        var confidence = 0.5f

        // Boost for having resourceId (most reliable)
        if (element.resourceId.isNotBlank()) {
            confidence += 0.2f
        }

        // Boost for content description (accessibility info)
        if (element.contentDescription.isNotBlank()) {
            confidence += 0.15f
        }

        // Boost for reasonable text length (not too short, not too long)
        val label = deriveLabel(element)
        if (label.length in 2..20) {
            confidence += 0.1f
        }

        // Small boost for being clickable
        if (element.isClickable) {
            confidence += 0.05f
        }

        return confidence.coerceIn(0f, 1f)
    }
}
