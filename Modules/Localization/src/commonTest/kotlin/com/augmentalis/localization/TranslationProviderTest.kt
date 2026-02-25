package com.augmentalis.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TranslationProviderTest {

    // ─── TranslationProvider tests ─────────────────────────────────────────────

    @Test
    fun knownKeyInEnglishReturnsCorrectTranslation() {
        val result = TranslationProvider.translate("cmd.back", "en")
        assertEquals("back", result)
    }

    @Test
    fun knownKeyInSpanishReturnsSpanishTranslation() {
        val result = TranslationProvider.translate("cmd.home", "es")
        assertEquals("inicio", result)
    }

    @Test
    fun missingKeyFallsBackToKeyItself() {
        val key = "cmd.this.key.does.not.exist"
        val result = TranslationProvider.translate(key, "en")
        // TranslationProvider returns the key itself when not found
        assertEquals(key, result)
    }

    @Test
    fun unknownLanguageFallsBackToEnglish() {
        // "xx" is not a supported language — should fall back to "en"
        val result = TranslationProvider.translate("cmd.back", "xx")
        assertEquals("back", result)
    }

    @Test
    fun templateWithSingleArgSubstituted() {
        val result = TranslationProvider.translate("cmd.open_app", "en", "Camera")
        assertTrue(result.contains("Camera"), "Expected arg substituted in: $result")
        assertNotEquals("open %s", result)
    }

    @Test
    fun japaneseTranslationContainsExpectedCharacters() {
        val result = TranslationProvider.translate("cmd.back", "ja")
        assertEquals("戻る", result)
    }
}
