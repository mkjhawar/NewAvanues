/**
 * Localizer.desktop.kt - Desktop/JVM-specific localization implementation
 *
 * Uses Java Preferences API for language persistence.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.localization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/**
 * Desktop/JVM implementation of Localizer
 */
actual class Localizer private constructor() {

    actual companion object {
        private const val PREFS_NODE = "com/augmentalis/localization"
        private const val KEY_LANGUAGE = "current_language"
        private const val DEFAULT_LANGUAGE = "en"

        @Volatile
        private var instance: Localizer? = null

        actual fun getInstance(context: Any?): Localizer {
            return instance ?: synchronized(this) {
                instance ?: Localizer().also { instance = it }
            }
        }
    }

    private val prefs: Preferences = Preferences.userRoot().node(PREFS_NODE)
    private var isReady = false
    private var currentLanguage = DEFAULT_LANGUAGE

    private val _languageState = MutableStateFlow(currentLanguage)
    actual val languageState: StateFlow<String> = _languageState.asStateFlow()

    actual fun initialize(): Boolean {
        if (isReady) return true

        return try {
            currentLanguage = prefs.get(KEY_LANGUAGE, DEFAULT_LANGUAGE)
            _languageState.value = currentLanguage
            isReady = true
            println("Localizer initialized with language: $currentLanguage")
            true
        } catch (e: Exception) {
            println("Failed to initialize Localizer: ${e.message}")
            false
        }
    }

    actual fun shutdown() {
        isReady = false
        instance = null
        println("Localizer shutdown")
    }

    actual fun isReady(): Boolean = isReady

    actual fun getCurrentLanguage(): String = currentLanguage

    actual fun getSupportedLanguages(): Set<String> = LanguageSupport.VIVOKA_LANGUAGES.keys

    actual fun isLanguageSupported(languageCode: String): Boolean =
        languageCode in getSupportedLanguages()

    actual fun setLanguage(languageCode: String): Boolean {
        if (!isLanguageSupported(languageCode)) {
            println("Unsupported language: $languageCode")
            return false
        }

        val oldLanguage = currentLanguage
        currentLanguage = languageCode
        _languageState.value = languageCode

        prefs.put(KEY_LANGUAGE, languageCode)

        println("Language changed from $oldLanguage to $languageCode")
        return true
    }

    actual fun translate(key: String, vararg args: Any): String =
        TranslationProvider.translate(key, currentLanguage, *args)

    actual fun getLanguageDisplayName(languageCode: String): String =
        LanguageSupport.VIVOKA_LANGUAGES[languageCode] ?: languageCode

    actual fun isVoskSupported(languageCode: String): Boolean =
        languageCode in LanguageSupport.VOSK_LANGUAGES

    actual fun isVivokaSupported(languageCode: String): Boolean =
        languageCode in LanguageSupport.VIVOKA_LANGUAGES.keys
}
