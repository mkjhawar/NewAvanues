/**
 * LanguagePreferences.android.kt - Android implementation using SharedPreferences
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 */
package com.augmentalis.localization

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of LanguagePreferences using SharedPreferences
 */
actual class LanguagePreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "voiceos_localization"
        private const val KEY_LANGUAGE = "current_language"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun getSavedLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LanguageConstants.DEFAULT_LANGUAGE)
            ?: LanguageConstants.DEFAULT_LANGUAGE
    }

    actual fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
}
