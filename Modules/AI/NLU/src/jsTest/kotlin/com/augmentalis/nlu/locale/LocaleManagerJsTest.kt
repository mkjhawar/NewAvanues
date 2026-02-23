package com.augmentalis.nlu.locale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JS/Web tests for LocaleManager
 *
 * Tests locale detection, override, fallback chain, and supported locale set.
 * Note: navigator.language may not be available in Node.js test environment,
 * so getCurrentLocale() should fall back to "en-US".
 */
class LocaleManagerJsTest {

    @Test
    fun getCurrentLocale_returnsValidLocale() {
        val manager = LocaleManager()
        val locale = manager.getCurrentLocale()

        assertTrue(locale.isNotEmpty(), "Locale should not be empty")
        // Should be a valid BCP-47 format or en-US fallback
        assertTrue(
            locale.matches(Regex("[a-z]{2}(-[A-Z]{2})?")) || locale == "en-US",
            "Locale should be BCP-47 format: $locale"
        )
    }

    @Test
    fun setLocale_overridesDefault() {
        val manager = LocaleManager()

        manager.setLocale("fr-FR")
        assertEquals("fr-FR", manager.getCurrentLocale(), "User override should take priority")

        // Clean up
        manager.setLocale(null)
    }

    @Test
    fun setLocale_nullClearsOverride() {
        val manager = LocaleManager()

        manager.setLocale("de-DE")
        assertEquals("de-DE", manager.getCurrentLocale())

        manager.setLocale(null)
        // After clearing, should revert to system or en-US
        val locale = manager.getCurrentLocale()
        assertTrue(locale != "de-DE" || locale == "en-US", "Should revert after clearing override")
    }

    @Test
    fun getFallbackChain_fullLocale() {
        val manager = LocaleManager()
        val chain = manager.getFallbackChain("fr-FR")

        assertEquals(3, chain.size, "fr-FR should produce 3-element chain")
        assertEquals("fr-FR", chain[0], "First should be full locale")
        assertEquals("fr", chain[1], "Second should be language only")
        assertEquals("en-US", chain[2], "Third should be en-US fallback")
    }

    @Test
    fun getFallbackChain_languageOnly() {
        val manager = LocaleManager()
        val chain = manager.getFallbackChain("es")

        assertEquals(2, chain.size, "Language-only should produce 2-element chain")
        assertEquals("es", chain[0], "First should be language code")
        assertEquals("en-US", chain[1], "Second should be en-US fallback")
    }

    @Test
    fun getFallbackChain_enUS() {
        val manager = LocaleManager()
        val chain = manager.getFallbackChain("en-US")

        assertEquals(2, chain.size, "en-US should produce 2-element chain")
        assertEquals("en-US", chain[0])
        assertEquals("en", chain[1])
        // en-US is already in chain, so no duplicate
    }

    @Test
    fun isLocaleSupported_knownLocales() {
        val manager = LocaleManager()

        assertTrue(manager.isLocaleSupported("en-US"))
        assertTrue(manager.isLocaleSupported("es-ES"))
        assertTrue(manager.isLocaleSupported("zh-CN"))
        assertTrue(manager.isLocaleSupported("ja-JP"))
        assertTrue(manager.isLocaleSupported("hi-IN"))
        assertTrue(manager.isLocaleSupported("ar-SA"))
    }

    @Test
    fun isLocaleSupported_unknownLocale() {
        val manager = LocaleManager()

        assertFalse(manager.isLocaleSupported("xx-XX"))
        assertFalse(manager.isLocaleSupported(""))
        assertFalse(manager.isLocaleSupported("klingon"))
    }

    @Test
    fun getSupportedLocales_hasExpectedCount() {
        val manager = LocaleManager()
        val locales = manager.getSupportedLocales()

        assertTrue(locales.size >= 52, "Should support 52+ locales, got ${locales.size}")
        assertTrue(locales.contains("en-US"))
        assertTrue(locales.contains("en"))
    }
}
