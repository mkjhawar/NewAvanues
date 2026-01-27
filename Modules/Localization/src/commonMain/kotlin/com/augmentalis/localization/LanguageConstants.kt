/**
 * LanguageConstants.kt - Supported languages for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-22
 * Migrated to KMP: 2026-01-28
 */
package com.augmentalis.localization

/**
 * Supported language constants for speech recognition engines
 */
object LanguageConstants {

    /**
     * Languages supported by Vosk speech recognition
     */
    val VOSK_LANGUAGES = setOf("en", "es", "fr", "de", "ru", "zh", "ja", "ko")

    /**
     * Languages supported by Vivoka speech recognition with display names
     */
    val VIVOKA_LANGUAGES = mapOf(
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "pt" to "Portuguese",
        "ru" to "Russian",
        "zh" to "Chinese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "ar" to "Arabic",
        "nl" to "Dutch",
        "pl" to "Polish",
        "tr" to "Turkish",
        "hi" to "Hindi",
        "th" to "Thai",
        "cs" to "Czech",
        "da" to "Danish",
        "fi" to "Finnish",
        "el" to "Greek",
        "he" to "Hebrew",
        "hu" to "Hungarian",
        "no" to "Norwegian",
        "sv" to "Swedish",
        "uk" to "Ukrainian",
        "bg" to "Bulgarian",
        "hr" to "Croatian",
        "ro" to "Romanian",
        "sk" to "Slovak",
        "sl" to "Slovenian",
        "et" to "Estonian",
        "lv" to "Latvian",
        "lt" to "Lithuanian",
        "is" to "Icelandic",
        "ga" to "Irish",
        "mt" to "Maltese",
        "sq" to "Albanian",
        "mk" to "Macedonian",
        "sr" to "Serbian",
        "bs" to "Bosnian",
        "cy" to "Welsh"
    )

    /**
     * Default language code
     */
    const val DEFAULT_LANGUAGE = "en"

    /**
     * Get display name for a language code
     */
    fun getDisplayName(languageCode: String): String {
        return VIVOKA_LANGUAGES[languageCode] ?: languageCode
    }

    /**
     * Check if language is supported by Vosk
     */
    fun isVoskSupported(languageCode: String): Boolean {
        return languageCode in VOSK_LANGUAGES
    }

    /**
     * Check if language is supported by Vivoka
     */
    fun isVivokaSupported(languageCode: String): Boolean {
        return languageCode in VIVOKA_LANGUAGES.keys
    }

    /**
     * Get all supported language codes
     */
    fun getSupportedLanguages(): Set<String> = VIVOKA_LANGUAGES.keys
}
