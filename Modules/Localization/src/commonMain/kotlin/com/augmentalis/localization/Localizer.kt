/**
 * Localizer.kt - Cross-platform localization manager
 *
 * Provides multi-language support for VoiceOS and related applications.
 * Supports 42+ languages with translation key-value system.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.localization

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform Localizer - manages languages and translations
 *
 * Usage:
 * ```kotlin
 * val localizer = Localizer.getInstance(context) // Android
 * val localizer = Localizer.getInstance() // Desktop/other
 *
 * localizer.initialize()
 * val translated = localizer.translate("cmd.back")
 * ```
 */
expect class Localizer {
    companion object {
        fun getInstance(context: Any? = null): Localizer
    }

    val languageState: StateFlow<String>

    fun initialize(): Boolean
    fun shutdown()
    fun isReady(): Boolean

    fun getCurrentLanguage(): String
    fun getSupportedLanguages(): Set<String>
    fun isLanguageSupported(languageCode: String): Boolean
    fun setLanguage(languageCode: String): Boolean

    fun translate(key: String, vararg args: Any): String
    fun getLanguageDisplayName(languageCode: String): String

    fun isVoskSupported(languageCode: String): Boolean
    fun isVivokaSupported(languageCode: String): Boolean
}

/**
 * Language support constants
 */
object LanguageSupport {
    // Vosk-supported languages (offline recognition)
    val VOSK_LANGUAGES = setOf("en", "es", "fr", "de", "ru", "zh", "ja", "ko")

    // Vivoka-supported languages (full list)
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
}
