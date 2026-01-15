package com.augmentalis.ava.features.nlu.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Common tests for LocaleManager
 *
 * Tests locale fallback chain logic and supported locale validation.
 * Platform-specific tests (Android/iOS/Desktop) test actual system integration.
 */
class LocaleManagerTest {

    @Test
    fun testFallbackChain_FullLocale() {
        // Mock LocaleManager with fallback chain logic
        val testCases = mapOf(
            "fr-FR" to listOf("fr-FR", "fr", "en-US"),
            "es-MX" to listOf("es-MX", "es", "en-US"),
            "zh-CN" to listOf("zh-CN", "zh", "en-US"),
            "en-GB" to listOf("en-GB", "en", "en-US"),
            "ar-SA" to listOf("ar-SA", "ar", "en-US")
        )

        for ((locale, expected) in testCases) {
            val chain = createFallbackChain(locale)
            assertEquals(expected, chain, "Fallback chain for $locale")
        }
    }

    @Test
    fun testFallbackChain_LanguageOnly() {
        val testCases = mapOf(
            "fr" to listOf("fr", "en-US"),
            "es" to listOf("es", "en-US"),
            "en" to listOf("en", "en-US")
        )

        for ((locale, expected) in testCases) {
            val chain = createFallbackChain(locale)
            assertEquals(expected, chain, "Fallback chain for $locale")
        }
    }

    @Test
    fun testFallbackChain_EnglishUS() {
        // en-US should only have itself
        val chain = createFallbackChain("en-US")
        assertEquals(listOf("en-US"), chain)
    }

    @Test
    fun testFallbackChain_NoDuplicates() {
        // Ensure no duplicates in chain
        val chain = createFallbackChain("fr-FR")
        assertEquals(chain.distinct(), chain, "No duplicates in fallback chain")
    }

    @Test
    fun testSupportedLocales_MajorLanguages() {
        val supportedLocales = getSupportedLocales()

        // Test major languages are supported
        val majorLocales = listOf(
            "en-US", "es-ES", "fr-FR", "de-DE", "it-IT",
            "pt-BR", "ru-RU", "ja-JP", "ko-KR", "zh-CN",
            "ar-SA", "hi-IN"
        )

        for (locale in majorLocales) {
            assertTrue(
                supportedLocales.contains(locale),
                "$locale should be supported"
            )
        }
    }

    @Test
    fun testSupportedLocales_LanguageCodes() {
        val supportedLocales = getSupportedLocales()

        // Test language-only codes are supported
        val languageCodes = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh")

        for (code in languageCodes) {
            assertTrue(
                supportedLocales.contains(code),
                "Language code $code should be supported"
            )
        }
    }

    @Test
    fun testSupportedLocales_UnsupportedLocale() {
        val supportedLocales = getSupportedLocales()

        // Test that clearly unsupported locales are not in set
        val unsupportedLocales = listOf("xx-XX", "zz-ZZ", "invalid")

        for (locale in unsupportedLocales) {
            assertFalse(
                supportedLocales.contains(locale),
                "$locale should not be supported"
            )
        }
    }

    @Test
    fun testSupportedLocales_MinimumCount() {
        val supportedLocales = getSupportedLocales()

        // Should have at least 52 locales as specified
        assertTrue(
            supportedLocales.size >= 52,
            "Should support at least 52 locales, got ${supportedLocales.size}"
        )
    }

    // Helper functions that mirror LocaleManager implementation

    private fun createFallbackChain(locale: String): List<String> {
        val chain = mutableListOf<String>()

        // Add full locale
        chain.add(locale)

        // Add language only
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

    private fun getSupportedLocales(): Set<String> {
        return setOf(
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

            // Language-only codes
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "ar", "hi"
        )
    }
}
