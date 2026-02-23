package com.augmentalis.nlu.locale

import kotlinx.browser.window

/**
 * JS/Web implementation of LocaleManager
 *
 * Uses:
 * - window.navigator.language for browser locale detection
 * - localStorage for user locale override persistence
 * - BCP-47 language tags (language-COUNTRY format)
 *
 * Browser compatibility: All modern browsers support navigator.language
 */
actual class LocaleManager {

    private companion object {
        const val STORAGE_KEY = "ava_nlu_user_locale"

        /**
         * Supported locales (52+ languages)
         * Identical to Android/Desktop/iOS implementations
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
     * Get current locale from browser
     *
     * Priority:
     * 1. User override from localStorage
     * 2. Browser navigator.language (if supported)
     * 3. en-US fallback
     */
    actual fun getCurrentLocale(): String {
        // Priority 1: User override from localStorage
        val userLocale = try {
            window.localStorage.getItem(STORAGE_KEY)
        } catch (_: Exception) {
            // localStorage may throw in private browsing or when blocked
            null
        }
        if (userLocale != null) return userLocale

        // Priority 2: Browser locale
        val browserLocale = try {
            window.navigator.language
        } catch (_: Exception) {
            null
        }

        if (browserLocale != null) {
            // Normalize: browsers may return "en-US" or "en" format
            val normalized = browserLocale.replace("_", "-")
            if (isLocaleSupported(normalized)) return normalized

            // Try language-only code
            val languageOnly = normalized.split("-").firstOrNull() ?: "en"
            if (isLocaleSupported(languageOnly)) return languageOnly
        }

        return "en-US" // Ultimate fallback
    }

    /**
     * Set user locale preference in localStorage
     *
     * @param locale Locale code (e.g., "fr-FR") or null to clear override
     */
    actual fun setLocale(locale: String?) {
        try {
            if (locale == null) {
                window.localStorage.removeItem(STORAGE_KEY)
            } else {
                window.localStorage.setItem(STORAGE_KEY, locale)
            }
        } catch (_: Exception) {
            // localStorage may be unavailable — silently ignore
            console.warn("[LocaleManager] localStorage unavailable, locale override not persisted")
        }
    }

    /**
     * Get fallback chain for locale
     *
     * Creates progressively less specific fallbacks:
     * 1. Full locale (e.g., "fr-FR")
     * 2. Language only (e.g., "fr")
     * 3. en-US (ultimate fallback)
     */
    actual fun getFallbackChain(locale: String): List<String> {
        val chain = mutableListOf<String>()

        // Add full locale
        chain.add(locale)

        // Add language-only code
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
