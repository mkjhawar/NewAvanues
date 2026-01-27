/**
 * LanguagePreferences.desktop.kt - Desktop implementation using Java Preferences
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 */
package com.augmentalis.localization

import java.util.prefs.Preferences

/**
 * Desktop implementation of LanguagePreferences using Java Preferences API
 */
actual class LanguagePreferences {
    companion object {
        private const val KEY_LANGUAGE = "current_language"
    }

    private val prefs: Preferences = Preferences.userNodeForPackage(LanguagePreferences::class.java)

    actual fun getSavedLanguage(): String {
        return prefs.get(KEY_LANGUAGE, LanguageConstants.DEFAULT_LANGUAGE)
    }

    actual fun saveLanguage(languageCode: String) {
        prefs.put(KEY_LANGUAGE, languageCode)
    }
}
