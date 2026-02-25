/**
 * LanguageMetadata.kt - Language configuration for synonym packs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 *
 * Contains metadata about a language's writing system and tokenization needs.
 */
package com.augmentalis.voiceoscore

/**
 * Script type for a language.
 */
enum class ScriptType {
    /** Latin alphabet (English, Spanish, French, German, etc.) */
    LATIN,

    /** Cyrillic alphabet (Russian, Ukrainian, etc.) */
    CYRILLIC,

    /** Arabic script (Arabic, Persian, Urdu) - RTL */
    ARABIC,

    /** Devanagari script (Hindi, Sanskrit, Marathi) */
    DEVANAGARI,

    /** CJK - Chinese characters */
    CJK_CHINESE,

    /** CJK - Japanese (Kanji, Hiragana, Katakana) */
    CJK_JAPANESE,

    /** Korean Hangul */
    CJK_KOREAN,

    /** Thai script */
    THAI,

    /** Hebrew script - RTL */
    HEBREW,

    /** Greek alphabet */
    GREEK,

    /** Unknown/Other */
    OTHER
}

/**
 * Tokenization strategy for a language.
 */
enum class TokenizerType {
    /** Split by whitespace (Latin, Cyrillic, Arabic, Devanagari) */
    WHITESPACE,

    /** Morphological analysis needed (Japanese) */
    MORPHOLOGICAL,

    /** Character-based with word boundaries (Chinese) */
    CHARACTER_BOUNDARY,

    /** Syllable-based (Korean) */
    SYLLABLE,

    /** No word boundaries (Thai) */
    NO_BOUNDARY
}

/**
 * Metadata for a language's synonym pack.
 *
 * @property languageCode ISO 639-1 language code (e.g., "en", "ja", "zh")
 * @property languageName Human-readable name (e.g., "English", "Japanese")
 * @property script The script type used by this language
 * @property tokenizer The tokenization strategy for this language
 * @property isRtl Whether the language is right-to-left
 * @property version Version of the synonym pack
 */
data class LanguageMetadata(
    val languageCode: String,
    val languageName: String = "",
    val script: ScriptType = ScriptType.LATIN,
    val tokenizer: TokenizerType = TokenizerType.WHITESPACE,
    val isRtl: Boolean = false,
    val version: String = "1.0"
) {
    companion object {
        /**
         * Default metadata for common languages.
         */
        val DEFAULTS: Map<String, LanguageMetadata> = mapOf(
            "en" to LanguageMetadata(
                languageCode = "en",
                languageName = "English",
                script = ScriptType.LATIN,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "es" to LanguageMetadata(
                languageCode = "es",
                languageName = "Spanish",
                script = ScriptType.LATIN,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "fr" to LanguageMetadata(
                languageCode = "fr",
                languageName = "French",
                script = ScriptType.LATIN,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "de" to LanguageMetadata(
                languageCode = "de",
                languageName = "German",
                script = ScriptType.LATIN,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "pt" to LanguageMetadata(
                languageCode = "pt",
                languageName = "Portuguese",
                script = ScriptType.LATIN,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "it" to LanguageMetadata(
                languageCode = "it",
                languageName = "Italian",
                script = ScriptType.LATIN,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "ru" to LanguageMetadata(
                languageCode = "ru",
                languageName = "Russian",
                script = ScriptType.CYRILLIC,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "hi" to LanguageMetadata(
                languageCode = "hi",
                languageName = "Hindi",
                script = ScriptType.DEVANAGARI,
                tokenizer = TokenizerType.WHITESPACE
            ),
            "ar" to LanguageMetadata(
                languageCode = "ar",
                languageName = "Arabic",
                script = ScriptType.ARABIC,
                tokenizer = TokenizerType.WHITESPACE,
                isRtl = true
            ),
            "he" to LanguageMetadata(
                languageCode = "he",
                languageName = "Hebrew",
                script = ScriptType.HEBREW,
                tokenizer = TokenizerType.WHITESPACE,
                isRtl = true
            ),
            "ja" to LanguageMetadata(
                languageCode = "ja",
                languageName = "Japanese",
                script = ScriptType.CJK_JAPANESE,
                tokenizer = TokenizerType.MORPHOLOGICAL
            ),
            "zh" to LanguageMetadata(
                languageCode = "zh",
                languageName = "Chinese (Mandarin)",
                script = ScriptType.CJK_CHINESE,
                tokenizer = TokenizerType.CHARACTER_BOUNDARY
            ),
            "ko" to LanguageMetadata(
                languageCode = "ko",
                languageName = "Korean",
                script = ScriptType.CJK_KOREAN,
                tokenizer = TokenizerType.SYLLABLE
            ),
            "th" to LanguageMetadata(
                languageCode = "th",
                languageName = "Thai",
                script = ScriptType.THAI,
                tokenizer = TokenizerType.NO_BOUNDARY
            )
        )

        /**
         * Get metadata for a language, or create default if unknown.
         */
        fun forLanguage(languageCode: String): LanguageMetadata {
            return DEFAULTS[languageCode.lowercase()] ?: LanguageMetadata(
                languageCode = languageCode.lowercase(),
                languageName = languageCode.uppercase(),
                script = ScriptType.OTHER,
                tokenizer = TokenizerType.WHITESPACE
            )
        }

        /**
         * Check if a language requires platform tokenizer (CJK, Thai).
         */
        fun requiresPlatformTokenizer(languageCode: String): Boolean {
            val meta = forLanguage(languageCode)
            return meta.tokenizer != TokenizerType.WHITESPACE
        }
    }
}
