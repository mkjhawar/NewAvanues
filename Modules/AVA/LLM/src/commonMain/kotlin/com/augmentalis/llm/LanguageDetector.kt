/**
 * Language Detector for AVA AI
 *
 * Detects the language of user input to enable auto-model selection.
 * Uses character-based detection for fast, lightweight analysis.
 *
 * Supports detection of:
 * - English, Spanish, French, German, Italian, Portuguese (Latin script)
 * - Chinese (Simplified & Traditional)
 * - Japanese (Hiragana, Katakana, Kanji)
 * - Korean (Hangul)
 * - Arabic
 * - Hindi (Devanagari)
 * - Russian (Cyrillic)
 * - Thai
 * - Vietnamese
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.llm

import com.augmentalis.ava.platform.Logger

/**
 * Language detector using Unicode character ranges
 */
object LanguageDetector {

    private const val TAG = "LanguageDetector"

    /**
     * Detect language from text
     *
     * Returns the most likely language based on character analysis.
     *
     * @param text Input text to analyze
     * @return Detected Language
     */
    fun detect(text: String): Language {
        if (text.isBlank()) {
            return Language.ENGLISH // Default fallback
        }

        // Count characters by script type
        val scriptCounts = mutableMapOf<Script, Int>()
        var totalSignificantChars = 0

        for (char in text) {
            val script = getCharacterScript(char)
            if (script != Script.COMMON) {
                scriptCounts[script] = scriptCounts.getOrDefault(script, 0) + 1
                totalSignificantChars++
            }
        }

        // If no significant characters found, default to English
        if (totalSignificantChars == 0) {
            return Language.ENGLISH
        }

        // Find dominant script
        val dominantScript = scriptCounts.maxByOrNull { it.value }?.key
            ?: return Language.ENGLISH

        // Map script to language
        val detectedLanguage = scriptToLanguage(dominantScript, text)

        Logger.d(TAG, "Language detected: $detectedLanguage (script: $dominantScript, chars: $totalSignificantChars)")

        return detectedLanguage
    }

    /**
     * Detect language and return confidence score
     *
     * @param text Input text
     * @return Pair of Language and confidence (0.0-1.0)
     */
    fun detectWithConfidence(text: String): Pair<Language, Float> {
        if (text.isBlank()) {
            return Language.ENGLISH to 0.5f // Low confidence default
        }

        val scriptCounts = mutableMapOf<Script, Int>()
        var totalSignificantChars = 0

        for (char in text) {
            val script = getCharacterScript(char)
            if (script != Script.COMMON) {
                scriptCounts[script] = scriptCounts.getOrDefault(script, 0) + 1
                totalSignificantChars++
            }
        }

        if (totalSignificantChars == 0) {
            return Language.ENGLISH to 0.5f
        }

        // Calculate confidence based on script dominance
        val dominantScript = scriptCounts.maxByOrNull { it.value }?.key
            ?: return Language.ENGLISH to 0.5f

        val dominantCount = scriptCounts[dominantScript] ?: 0
        val confidence = dominantCount.toFloat() / totalSignificantChars

        val language = scriptToLanguage(dominantScript, text)

        return language to confidence
    }

    /**
     * Get recommended model for a given language
     *
     * Returns model ID that best supports the detected language.
     *
     * @param language Detected language
     * @return Model ID (e.g., "gemma-2b", "qwen-2.5-1.5b")
     */
    fun getRecommendedModel(language: Language): String {
        return when (language) {
            Language.ENGLISH -> "gemma-2b-it-q4f16_1" // Gemma optimized for English
            Language.CHINESE_SIMPLIFIED,
            Language.CHINESE_TRADITIONAL,
            Language.JAPANESE,
            Language.KOREAN,
            Language.THAI,
            Language.VIETNAMESE -> "qwen2.5-1.5b-instruct-q4f16_1" // Qwen for Asian languages
            Language.ARABIC -> "qwen2.5-1.5b-instruct-q4f16_1" // Qwen has Arabic support
            Language.HINDI -> "qwen2.5-1.5b-instruct-q4f16_1" // Qwen for Indic languages
            Language.RUSSIAN -> "qwen2.5-1.5b-instruct-q4f16_1" // Qwen for Cyrillic
            // European languages - Gemma has decent support
            Language.SPANISH,
            Language.FRENCH,
            Language.GERMAN,
            Language.ITALIAN,
            Language.PORTUGUESE -> "gemma-2b-it-q4f16_1" // Gemma for European languages
            Language.UNKNOWN -> "gemma-2b-it-q4f16_1" // Default to Gemma
        }
    }

    /**
     * Check if a model supports a given language
     *
     * @param modelId Model identifier
     * @param language Language to check
     * @return true if model supports language, false otherwise
     */
    fun modelSupportsLanguage(modelId: String, language: Language): Boolean {
        return when {
            modelId.contains("gemma", ignoreCase = true) -> {
                // Gemma: Strong English, decent European languages
                language in listOf(
                    Language.ENGLISH,
                    Language.SPANISH,
                    Language.FRENCH,
                    Language.GERMAN,
                    Language.ITALIAN,
                    Language.PORTUGUESE
                )
            }
            modelId.contains("qwen", ignoreCase = true) -> {
                // Qwen: Multilingual, strong Asian languages
                true // Qwen supports all languages
            }
            modelId.contains("llama", ignoreCase = true) -> {
                // Llama: Strong English, good European languages
                language in listOf(
                    Language.ENGLISH,
                    Language.SPANISH,
                    Language.FRENCH,
                    Language.GERMAN,
                    Language.ITALIAN,
                    Language.PORTUGUESE,
                    Language.RUSSIAN
                )
            }
            modelId.contains("phi", ignoreCase = true) -> {
                // Phi: Primarily English
                language == Language.ENGLISH
            }
            modelId.contains("mistral", ignoreCase = true) -> {
                // Mistral: Strong English and European languages
                language in listOf(
                    Language.ENGLISH,
                    Language.SPANISH,
                    Language.FRENCH,
                    Language.GERMAN,
                    Language.ITALIAN
                )
            }
            else -> {
                // Unknown model - assume English only
                language == Language.ENGLISH
            }
        }
    }

    /**
     * Determine character script (Unicode block)
     */
    private fun getCharacterScript(char: Char): Script {
        val codePoint = char.code
        return when (codePoint) {
            in 0x0041..0x005A,  // Latin uppercase A-Z
            in 0x0061..0x007A,  // Latin lowercase a-z
            in 0x00C0..0x00FF,  // Latin Extended-A (accented characters)
            in 0x0100..0x017F -> Script.LATIN

            in 0x4E00..0x9FFF -> Script.CJK_UNIFIED  // CJK Unified Ideographs (Chinese)

            in 0x3040..0x309F -> Script.HIRAGANA  // Hiragana (Japanese)
            in 0x30A0..0x30FF -> Script.KATAKANA  // Katakana (Japanese)

            in 0xAC00..0xD7AF -> Script.HANGUL  // Hangul (Korean)

            in 0x0600..0x06FF -> Script.ARABIC  // Arabic

            in 0x0900..0x097F -> Script.DEVANAGARI  // Devanagari (Hindi)

            in 0x0400..0x04FF -> Script.CYRILLIC  // Cyrillic (Russian)

            in 0x0E00..0x0E7F -> Script.THAI  // Thai

            in 0x1E00..0x1EFF -> Script.VIETNAMESE  // Vietnamese (Latin Extended Additional)

            else -> Script.COMMON  // Numbers, punctuation, spaces, etc.
        }
    }

    /**
     * Map script to most likely language
     */
    private fun scriptToLanguage(script: Script, text: String): Language {
        return when (script) {
            Script.LATIN -> {
                // Check for language-specific patterns
                // Check more specific languages first (Portuguese, Italian) before broader ones (French)
                when {
                    containsVietnameseChars(text) -> Language.VIETNAMESE
                    containsPortugueseChars(text) -> Language.PORTUGUESE  // ã, õ are unique
                    containsItalianChars(text) -> Language.ITALIAN        // ì, ò are unique
                    containsSpanishChars(text) -> Language.SPANISH
                    containsGermanChars(text) -> Language.GERMAN
                    containsFrenchChars(text) -> Language.FRENCH          // Broader char set
                    else -> Language.ENGLISH // Default Latin script
                }
            }
            Script.CJK_UNIFIED -> {
                // Distinguish Chinese variants (simplified vs traditional)
                // For now, default to simplified
                Language.CHINESE_SIMPLIFIED
            }
            Script.HIRAGANA, Script.KATAKANA -> Language.JAPANESE
            Script.HANGUL -> Language.KOREAN
            Script.ARABIC -> Language.ARABIC
            Script.DEVANAGARI -> Language.HINDI
            Script.CYRILLIC -> Language.RUSSIAN
            Script.THAI -> Language.THAI
            Script.VIETNAMESE -> Language.VIETNAMESE
            Script.COMMON -> Language.UNKNOWN
        }
    }

    // Language-specific character patterns
    private fun containsVietnameseChars(text: String): Boolean =
        text.any { it in "ăâđêôơưĂÂĐÊÔƠƯ" }

    private fun containsSpanishChars(text: String): Boolean =
        text.contains("ñ", ignoreCase = true) || text.contains("¿") || text.contains("¡")

    private fun containsFrenchChars(text: String): Boolean =
        text.any { it in "àâæçèéêëîïôùûüÿœÀÂÆÇÈÉÊËÎÏÔÙÛÜŸŒ" }

    private fun containsGermanChars(text: String): Boolean =
        text.any { it in "äöüßÄÖÜ" }

    private fun containsPortugueseChars(text: String): Boolean =
        text.any { it in "ãõçÃÕÇ" }

    private fun containsItalianChars(text: String): Boolean =
        text.any { it in "àèéìòùÀÈÉÌÒÙ" }
}

/**
 * Script (writing system) enum
 */
private enum class Script {
    LATIN,           // English, Spanish, French, German, etc.
    CJK_UNIFIED,     // Chinese characters
    HIRAGANA,        // Japanese phonetic
    KATAKANA,        // Japanese phonetic
    HANGUL,          // Korean
    ARABIC,          // Arabic
    DEVANAGARI,      // Hindi, Sanskrit
    CYRILLIC,        // Russian, Ukrainian, etc.
    THAI,            // Thai
    VIETNAMESE,      // Vietnamese (Latin with diacritics)
    COMMON           // Numbers, punctuation, whitespace
}

/**
 * Supported languages
 */
enum class Language(val displayName: String, val iso6391: String) {
    ENGLISH("English", "en"),
    SPANISH("Spanish", "es"),
    FRENCH("French", "fr"),
    GERMAN("German", "de"),
    ITALIAN("Italian", "it"),
    PORTUGUESE("Portuguese", "pt"),
    CHINESE_SIMPLIFIED("Chinese (Simplified)", "zh-Hans"),
    CHINESE_TRADITIONAL("Chinese (Traditional)", "zh-Hant"),
    JAPANESE("Japanese", "ja"),
    KOREAN("Korean", "ko"),
    ARABIC("Arabic", "ar"),
    HINDI("Hindi", "hi"),
    RUSSIAN("Russian", "ru"),
    THAI("Thai", "th"),
    VIETNAMESE("Vietnamese", "vi"),
    UNKNOWN("Unknown", "");

    /**
     * Check if this language uses Latin script
     */
    fun isLatinScript(): Boolean {
        return this in listOf(
            ENGLISH, SPANISH, FRENCH, GERMAN,
            ITALIAN, PORTUGUESE, VIETNAMESE
        )
    }

    /**
     * Check if this language uses CJK characters
     */
    fun isCJK(): Boolean {
        return this in listOf(
            CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL,
            JAPANESE, KOREAN
        )
    }

    /**
     * Check if this language is Asian
     */
    fun isAsian(): Boolean {
        return this in listOf(
            CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL,
            JAPANESE, KOREAN, THAI, VIETNAMESE, HINDI
        )
    }
}
