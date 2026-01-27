/**
 * LocalizerCore.kt - Platform-agnostic localization core
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-22
 * Migrated to KMP: 2026-01-28
 */
package com.augmentalis.localization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core localization functionality - platform agnostic
 *
 * This class provides the main localization logic without any platform-specific
 * dependencies. Platform-specific storage is handled via LanguagePreferences.
 */
class LocalizerCore(
    private val preferences: LanguagePreferences
) {
    companion object {
        const val MODULE_ID = "localization"
        const val MODULE_VERSION = "2.0.0"
    }

    private var isReady = false
    private var currentLanguage = LanguageConstants.DEFAULT_LANGUAGE
    private val translationManager = TranslationManager()

    private val _languageState = MutableStateFlow(currentLanguage)
    val languageState: StateFlow<String> = _languageState.asStateFlow()

    // Module metadata
    val name: String = "Localization"
    val version: String = MODULE_VERSION
    val description: String = "Multi-language support with 42+ languages"

    fun getDependencies(): List<String> = emptyList()

    /**
     * Initialize the localization module
     */
    fun initialize(): Boolean {
        if (isReady) return true

        return try {
            // Load saved language preference
            currentLanguage = preferences.getSavedLanguage()
            _languageState.value = currentLanguage

            // Load translations for current language
            translationManager.loadTranslations(currentLanguage)

            isReady = true
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shutdown the module
     */
    fun shutdown() {
        isReady = false
    }

    /**
     * Check if module is ready
     */
    fun isReady(): Boolean = isReady

    /**
     * Get the current language code
     */
    fun getCurrentLanguage(): String = currentLanguage

    /**
     * Get all supported language codes
     */
    fun getSupportedLanguages(): Set<String> = LanguageConstants.getSupportedLanguages()

    /**
     * Check if a language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return languageCode in getSupportedLanguages()
    }

    /**
     * Set the current language
     *
     * @param languageCode ISO 639-1 language code
     * @return true if language was set successfully
     */
    fun setLanguage(languageCode: String): Boolean {
        if (!isLanguageSupported(languageCode)) {
            return false
        }

        currentLanguage = languageCode
        _languageState.value = languageCode

        // Save preference
        preferences.saveLanguage(languageCode)

        // Load new translations
        translationManager.loadTranslations(languageCode)

        return true
    }

    /**
     * Translate a key using the current language
     *
     * @param key Translation key
     * @param args Format arguments
     * @return Translated string
     */
    fun translate(key: String, vararg args: Any): String {
        return translationManager.translate(key, currentLanguage, *args)
    }

    /**
     * Get the display name for a language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return LanguageConstants.getDisplayName(languageCode)
    }

    /**
     * Check if language is supported by Vosk engine
     */
    fun isVoskSupported(languageCode: String): Boolean {
        return LanguageConstants.isVoskSupported(languageCode)
    }

    /**
     * Check if language is supported by Vivoka engine
     */
    fun isVivokaSupported(languageCode: String): Boolean {
        return LanguageConstants.isVivokaSupported(languageCode)
    }
}
