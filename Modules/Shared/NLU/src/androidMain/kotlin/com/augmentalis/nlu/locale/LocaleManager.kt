package com.augmentalis.nlu.locale

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * Android implementation of LocaleManager
 *
 * Uses:
 * - SharedPreferences for user locale override persistence
 * - Android Locale.getDefault() for system locale detection
 * - BCP-47 language tags (language-COUNTRY format)
 *
 * Thread safety: All methods are thread-safe via SharedPreferences
 */
actual class LocaleManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_NAME = "ava_nlu_locale"
        const val KEY_LOCALE = "user_locale"

        /**
         * Supported locales (52+ languages)
         *
         * Format: BCP-47 language tags (language-COUNTRY or language)
         * Includes major languages and regional variants
         */
        val SUPPORTED_LOCALES = setOf(
            // English
            "en-US", "en-GB", "en-CA", "en-AU", "en-IN", "en-NZ", "en-ZA",

            // Spanish
            "es-ES", "es-MX", "es-AR", "es-CO", "es-CL", "es-PE",

            // French
            "fr-FR", "fr-CA", "fr-BE", "fr-CH",

            // German
            "de-DE", "de-AT", "de-CH",

            // Italian
            "it-IT", "it-CH",

            // Portuguese
            "pt-BR", "pt-PT",

            // Russian
            "ru-RU",

            // Japanese
            "ja-JP",

            // Korean
            "ko-KR",

            // Chinese
            "zh-CN", "zh-TW", "zh-HK",

            // Arabic
            "ar-SA", "ar-EG", "ar-AE",

            // Hindi & Indian languages
            "hi-IN", "bn-IN", "te-IN", "mr-IN", "ta-IN",

            // Other European languages
            "nl-NL", "nl-BE",
            "sv-SE",
            "da-DK",
            "no-NO",
            "fi-FI",
            "pl-PL",
            "cs-CZ",
            "el-GR",
            "tr-TR",
            "he-IL",
            "ro-RO",
            "hu-HU",
            "sk-SK",
            "uk-UA",

            // Southeast Asian languages
            "th-TH",
            "vi-VN",
            "id-ID",
            "ms-MY",

            // Language-only codes (fallbacks)
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi"
        )
    }

    /**
     * Get current locale
     *
     * Priority:
     * 1. User override from SharedPreferences
     * 2. System default locale (if supported)
     * 3. en-US fallback
     */
    actual fun getCurrentLocale(): String {
        // Priority 1: User override
        val userLocale = prefs.getString(KEY_LOCALE, null)
        if (userLocale != null) {
            return userLocale
        }

        // Priority 2: System default
        val systemLocale = Locale.getDefault().toLanguageTag()
        return if (isLocaleSupported(systemLocale)) {
            systemLocale
        } else {
            // Try language-only code
            val languageOnly = Locale.getDefault().language
            if (isLocaleSupported(languageOnly)) {
                languageOnly
            } else {
                "en-US" // Ultimate fallback
            }
        }
    }

    /**
     * Set user locale preference
     *
     * @param locale Locale code (e.g., "fr-FR") or null to clear override
     */
    actual fun setLocale(locale: String?) {
        if (locale == null) {
            prefs.edit().remove(KEY_LOCALE).apply()
        } else {
            prefs.edit().putString(KEY_LOCALE, locale).apply()
        }
    }

    /**
     * Get fallback chain for locale
     *
     * Creates progressively less specific fallbacks:
     * 1. Full locale (e.g., "fr-FR")
     * 2. Language only (e.g., "fr")
     * 3. en-US (ultimate fallback)
     *
     * This ensures embeddings can be loaded even if exact locale is missing.
     */
    actual fun getFallbackChain(locale: String): List<String> {
        val chain = mutableListOf<String>()

        // Add full locale (e.g., "fr-FR")
        chain.add(locale)

        // Add language only (e.g., "fr")
        val parts = locale.split("-")
        if (parts.size > 1) {
            val languageOnly = parts[0]
            if (!chain.contains(languageOnly)) {
                chain.add(languageOnly)
            }
        }

        // Add en-US as final fallback
        if (!chain.contains("en-US")) {
            chain.add("en-US")
        }

        return chain
    }

    /**
     * Check if locale is supported
     *
     * Checks exact match in supported locales set
     */
    actual fun isLocaleSupported(locale: String): Boolean {
        return SUPPORTED_LOCALES.contains(locale)
    }

    /**
     * Get all supported locales
     */
    actual fun getSupportedLocales(): Set<String> {
        return SUPPORTED_LOCALES
    }
}
