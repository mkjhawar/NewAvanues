/**
 * Localizer.android.kt - Android-specific localization implementation
 *
 * Uses SharedPreferences for language persistence.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.localization

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of Localizer
 */
actual class Localizer private constructor(private val context: Context) {

    actual companion object {
        private const val TAG = "Localizer"
        private const val PREFS_NAME = "voiceos_localization"
        private const val KEY_LANGUAGE = "current_language"
        private const val DEFAULT_LANGUAGE = "en"

        @Volatile
        private var instance: Localizer? = null

        actual fun getInstance(context: Any?): Localizer {
            val androidContext = context as? Context
                ?: throw IllegalArgumentException("Android context required")
            return instance ?: synchronized(this) {
                instance ?: Localizer(androidContext.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var isReady = false
    private var currentLanguage = DEFAULT_LANGUAGE

    private val _languageState = MutableStateFlow(currentLanguage)
    actual val languageState: StateFlow<String> = _languageState.asStateFlow()

    actual fun initialize(): Boolean {
        if (isReady) return true

        return try {
            currentLanguage = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
            _languageState.value = currentLanguage
            isReady = true
            Log.d(TAG, "Localizer initialized with language: $currentLanguage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Localizer", e)
            false
        }
    }

    actual fun shutdown() {
        isReady = false
        instance = null
        Log.d(TAG, "Localizer shutdown")
    }

    actual fun isReady(): Boolean = isReady

    actual fun getCurrentLanguage(): String = currentLanguage

    actual fun getSupportedLanguages(): Set<String> = LanguageSupport.VIVOKA_LANGUAGES.keys

    actual fun isLanguageSupported(languageCode: String): Boolean =
        languageCode in getSupportedLanguages()

    actual fun setLanguage(languageCode: String): Boolean {
        if (!isLanguageSupported(languageCode)) {
            Log.e(TAG, "Unsupported language: $languageCode")
            return false
        }

        val oldLanguage = currentLanguage
        currentLanguage = languageCode
        _languageState.value = languageCode

        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        Log.d(TAG, "Language changed from $oldLanguage to $languageCode")
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
