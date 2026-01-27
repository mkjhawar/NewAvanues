/**
 * LanguagePreferences.kt - Platform-agnostic language preferences interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 */
package com.augmentalis.localization

/**
 * Interface for platform-specific language preference storage
 */
expect class LanguagePreferences {
    /**
     * Get the saved language preference
     */
    fun getSavedLanguage(): String

    /**
     * Save the language preference
     */
    fun saveLanguage(languageCode: String)
}
