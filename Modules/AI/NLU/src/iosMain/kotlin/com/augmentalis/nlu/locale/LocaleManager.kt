package com.augmentalis.nlu.locale

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * iOS implementation of LocaleManager
 *
 * Uses:
 * - NSUserDefaults for user locale override persistence
 * - NSLocale.currentLocale for system locale detection
 * - BCP-47 language tags (language-COUNTRY format)
 *
 * Thread safety: NSUserDefaults is thread-safe
 */
actual class LocaleManager {

    private val defaults = NSUserDefaults.standardUserDefaults

    private companion object {
        const val KEY_LOCALE = "ava_nlu_user_locale"

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
            "sv-SE", "da-DK", "no-NO", "fi-FI",
            "pl-PL", "cs-CZ", "el-GR", "tr-TR",
            "he-IL", "ro-RO", "hu-HU", "sk-SK", "uk-UA",
            // Southeast Asian languages
            "th-TH", "vi-VN", "id-ID", "ms-MY",
            // Language-only codes (fallbacks)
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi"
        )
    }

    actual fun getCurrentLocale(): String {
        // Priority 1: User override
        val userLocale = defaults.stringForKey(KEY_LOCALE)
        if (userLocale != null) {
            return userLocale
        }

        // Priority 2: System default
        val locale = NSLocale.currentLocale
        val language = locale.languageCode
        val country = locale.countryCode
        val systemLocale = if (country != null) "$language-$country" else language

        return if (isLocaleSupported(systemLocale)) {
            systemLocale
        } else if (isLocaleSupported(language)) {
            language
        } else {
            "en-US"
        }
    }

    actual fun setLocale(locale: String?) {
        if (locale == null) {
            defaults.removeObjectForKey(KEY_LOCALE)
        } else {
            defaults.setObject(locale, KEY_LOCALE)
        }
    }

    actual fun getFallbackChain(locale: String): List<String> {
        val chain = mutableListOf<String>()
        chain.add(locale)

        val parts = locale.split("-")
        if (parts.size > 1) {
            val languageOnly = parts[0]
            if (!chain.contains(languageOnly)) {
                chain.add(languageOnly)
            }
        }

        if (!chain.contains("en-US")) {
            chain.add("en-US")
        }

        return chain
    }

    actual fun isLocaleSupported(locale: String): Boolean {
        return SUPPORTED_LOCALES.contains(locale)
    }

    actual fun getSupportedLocales(): Set<String> {
        return SUPPORTED_LOCALES
    }
}
