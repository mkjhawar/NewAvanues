package com.augmentalis.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageSupportTest {

    // ─── LanguageSupport tests ─────────────────────────────────────────────────

    @Test
    fun voskLanguagesContainsCoreSet() {
        val vosk = LanguageSupport.VOSK_LANGUAGES
        assertTrue(vosk.contains("en"), "English must be in Vosk set")
        assertTrue(vosk.contains("es"), "Spanish must be in Vosk set")
        assertTrue(vosk.contains("de"), "German must be in Vosk set")
        assertTrue(vosk.contains("zh"), "Chinese must be in Vosk set")
    }

    @Test
    fun vivokaLanguagesContainsMoreLanguagesThanVosk() {
        assertTrue(
            LanguageSupport.VIVOKA_LANGUAGES.size > LanguageSupport.VOSK_LANGUAGES.size,
            "Vivoka should support more languages than Vosk"
        )
    }

    @Test
    fun vivokaLanguagesHasEnglishWithDisplayName() {
        val displayName = LanguageSupport.VIVOKA_LANGUAGES["en"]
        assertEquals("English", displayName)
    }

    @Test
    fun unsupportedLanguageNotInVoskSet() {
        // "xx" is a made-up language code
        assertFalse(LanguageSupport.VOSK_LANGUAGES.contains("xx"))
    }

    @Test
    fun allVoskLanguagesAlsoInVivoka() {
        // Every offline-capable language should also be in the full Vivoka set
        LanguageSupport.VOSK_LANGUAGES.forEach { code ->
            assertTrue(
                LanguageSupport.VIVOKA_LANGUAGES.containsKey(code),
                "Vosk language '$code' missing from Vivoka map"
            )
        }
    }
}
