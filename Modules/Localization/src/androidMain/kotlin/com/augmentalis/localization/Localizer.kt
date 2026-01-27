/**
 * Localizer.kt - Android-specific localization module wrapper
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-22
 * Migrated to KMP: 2026-01-28
 */
package com.augmentalis.localization

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow

/**
 * Android-specific Localization Module
 *
 * Provides singleton access pattern for Android apps while delegating
 * core functionality to the platform-agnostic LocalizerCore.
 */
class Localizer private constructor(context: Context) {
    companion object {
        private const val TAG = "Localizer"

        @Volatile
        private var instance: Localizer? = null

        fun getInstance(context: Context): Localizer {
            return instance ?: synchronized(this) {
                instance ?: Localizer(context.applicationContext).also { instance = it }
            }
        }

        // Re-export constants for backwards compatibility
        val VOSK_LANGUAGES = LanguageConstants.VOSK_LANGUAGES
        val VIVOKA_LANGUAGES = LanguageConstants.VIVOKA_LANGUAGES
    }

    private val preferences = LanguagePreferences(context)
    private val core = LocalizerCore(preferences)

    // Expose core properties
    val languageState: StateFlow<String> get() = core.languageState
    val name: String get() = core.name
    val version: String get() = core.version
    val description: String get() = core.description

    fun getDependencies(): List<String> = core.getDependencies()

    fun initialize(): Boolean {
        val result = core.initialize()
        if (result) {
            Log.d(TAG, "Localization module initialized with language: ${core.getCurrentLanguage()}")
        } else {
            Log.e(TAG, "Failed to initialize localization module")
        }
        return result
    }

    fun shutdown() {
        core.shutdown()
        instance = null
        Log.d(TAG, "Localization module shutdown")
    }

    fun isReady(): Boolean = core.isReady()

    fun getCurrentLanguage(): String = core.getCurrentLanguage()

    fun getSupportedLanguages(): Set<String> = core.getSupportedLanguages()

    fun isLanguageSupported(languageCode: String): Boolean = core.isLanguageSupported(languageCode)

    fun setLanguage(languageCode: String): Boolean {
        val oldLanguage = core.getCurrentLanguage()
        val result = core.setLanguage(languageCode)
        if (result) {
            Log.d(TAG, "Language changed from $oldLanguage to $languageCode")
        } else {
            Log.e(TAG, "Unsupported language: $languageCode")
        }
        return result
    }

    fun translate(key: String, vararg args: Any): String = core.translate(key, *args)

    fun getLanguageDisplayName(languageCode: String): String = core.getLanguageDisplayName(languageCode)

    fun isVoskSupported(languageCode: String): Boolean = core.isVoskSupported(languageCode)

    fun isVivokaSupported(languageCode: String): Boolean = core.isVivokaSupported(languageCode)
}
