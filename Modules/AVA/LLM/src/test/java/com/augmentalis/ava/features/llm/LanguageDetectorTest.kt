package com.augmentalis.ava.features.llm

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for LanguageDetector
 *
 * Tests language detection accuracy for all supported languages.
 */
class LanguageDetectorTest {

    @Test
    fun `test English detection`() {
        val language = LanguageDetector.detect("Hello world, how are you?")
        assertEquals(Language.ENGLISH, language)
    }

    @Test
    fun `test Spanish detection`() {
        val language = LanguageDetector.detect("Hola mundo, ¿cómo estás?")
        assertEquals(Language.SPANISH, language)
    }

    @Test
    fun `test French detection`() {
        val language = LanguageDetector.detect("Bonjour! Le maître est sûr aujourd'hui!")
        assertEquals(Language.FRENCH, language)
    }

    @Test
    fun `test German detection`() {
        val language = LanguageDetector.detect("Guten Tag, schönes Wetter draußen!")
        assertEquals(Language.GERMAN, language)
    }

    @Test
    fun `test Italian detection`() {
        val language = LanguageDetector.detect("Così bello! Un caffè per favore!")
        assertEquals(Language.ITALIAN, language)
    }

    @Test
    fun `test Portuguese detection`() {
        val language = LanguageDetector.detect("Bom dia! São muitas opções e informações importantes!")
        assertEquals(Language.PORTUGUESE, language)
    }

    @Test
    fun `test Chinese Simplified detection`() {
        val language = LanguageDetector.detect("你好世界，你好吗？")
        assertEquals(Language.CHINESE_SIMPLIFIED, language)
    }

    @Test
    fun `test Japanese detection`() {
        val language = LanguageDetector.detect("こんにちは世界、お元気ですか？")
        assertEquals(Language.JAPANESE, language)
    }

    @Test
    fun `test Korean detection`() {
        val language = LanguageDetector.detect("안녕하세요 세계, 잘 지내세요?")
        assertEquals(Language.KOREAN, language)
    }

    @Test
    fun `test Arabic detection`() {
        val language = LanguageDetector.detect("مرحبا بالعالم، كيف حالك؟")
        assertEquals(Language.ARABIC, language)
    }

    @Test
    fun `test Hindi detection`() {
        val language = LanguageDetector.detect("नमस्ते दुनिया, आप कैसे हैं?")
        assertEquals(Language.HINDI, language)
    }

    @Test
    fun `test Russian detection`() {
        val language = LanguageDetector.detect("Привет мир, как дела?")
        assertEquals(Language.RUSSIAN, language)
    }

    @Test
    fun `test Thai detection`() {
        val language = LanguageDetector.detect("สวัสดีชาวโลก คุณเป็นอย่างไร?")
        assertEquals(Language.THAI, language)
    }

    @Test
    fun `test Vietnamese detection`() {
        val language = LanguageDetector.detect("Xin chào thế giới, bạn khỏe không?")
        assertEquals(Language.VIETNAMESE, language)
    }

    @Test
    fun `test empty string returns English`() {
        val language = LanguageDetector.detect("")
        assertEquals(Language.ENGLISH, language)
    }

    @Test
    fun `test whitespace only returns English`() {
        val language = LanguageDetector.detect("   \n\t  ")
        assertEquals(Language.ENGLISH, language)
    }

    @Test
    fun `test confidence score for clear language`() {
        val (language, confidence) = LanguageDetector.detectWithConfidence("Hello world")
        assertEquals(Language.ENGLISH, language)
        assertTrue("Confidence should be high for clear language", confidence > 0.9f)
    }

    @Test
    fun `test confidence score for mixed content`() {
        val (language, confidence) = LanguageDetector.detectWithConfidence("Hello 123 !@# 你好")
        // Should still detect a dominant language, but confidence may vary
        assertNotNull(language)
        assertTrue("Confidence should be between 0 and 1", confidence in 0f..1f)
    }

    @Test
    fun `test model recommendation for English`() {
        val modelId = LanguageDetector.getRecommendedModel(Language.ENGLISH)
        assertTrue("Should recommend Gemma for English", modelId.contains("gemma", ignoreCase = true))
    }

    @Test
    fun `test model recommendation for Chinese`() {
        val modelId = LanguageDetector.getRecommendedModel(Language.CHINESE_SIMPLIFIED)
        assertTrue("Should recommend Qwen for Chinese", modelId.contains("qwen", ignoreCase = true))
    }

    @Test
    fun `test model recommendation for Japanese`() {
        val modelId = LanguageDetector.getRecommendedModel(Language.JAPANESE)
        assertTrue("Should recommend Qwen for Japanese", modelId.contains("qwen", ignoreCase = true))
    }

    @Test
    fun `test model recommendation for Korean`() {
        val modelId = LanguageDetector.getRecommendedModel(Language.KOREAN)
        assertTrue("Should recommend Qwen for Korean", modelId.contains("qwen", ignoreCase = true))
    }

    @Test
    fun `test model recommendation for Arabic`() {
        val modelId = LanguageDetector.getRecommendedModel(Language.ARABIC)
        assertTrue("Should recommend Qwen for Arabic", modelId.contains("qwen", ignoreCase = true))
    }

    @Test
    fun `test Gemma supports English`() {
        val supports = LanguageDetector.modelSupportsLanguage("gemma-2b-it-q4f16_1", Language.ENGLISH)
        assertTrue("Gemma should support English", supports)
    }

    @Test
    fun `test Gemma does not support Chinese`() {
        val supports = LanguageDetector.modelSupportsLanguage("gemma-2b-it-q4f16_1", Language.CHINESE_SIMPLIFIED)
        assertFalse("Gemma should not support Chinese", supports)
    }

    @Test
    fun `test Qwen supports all languages`() {
        val languages = listOf(
            Language.ENGLISH, Language.CHINESE_SIMPLIFIED, Language.JAPANESE,
            Language.KOREAN, Language.ARABIC, Language.SPANISH
        )

        for (language in languages) {
            val supports = LanguageDetector.modelSupportsLanguage("qwen2.5-1.5b-instruct-q4f16_1", language)
            assertTrue("Qwen should support $language", supports)
        }
    }

    @Test
    fun `test language is Latin script`() {
        assertTrue(Language.ENGLISH.isLatinScript())
        assertTrue(Language.SPANISH.isLatinScript())
        assertTrue(Language.FRENCH.isLatinScript())
        assertFalse(Language.CHINESE_SIMPLIFIED.isLatinScript())
        assertFalse(Language.ARABIC.isLatinScript())
    }

    @Test
    fun `test language is CJK`() {
        assertTrue(Language.CHINESE_SIMPLIFIED.isCJK())
        assertTrue(Language.JAPANESE.isCJK())
        assertTrue(Language.KOREAN.isCJK())
        assertFalse(Language.ENGLISH.isCJK())
        assertFalse(Language.ARABIC.isCJK())
    }

    @Test
    fun `test language is Asian`() {
        assertTrue(Language.CHINESE_SIMPLIFIED.isAsian())
        assertTrue(Language.JAPANESE.isAsian())
        assertTrue(Language.KOREAN.isAsian())
        assertTrue(Language.THAI.isAsian())
        assertTrue(Language.VIETNAMESE.isAsian())
        assertTrue(Language.HINDI.isAsian())
        assertFalse(Language.ENGLISH.isAsian())
        assertFalse(Language.SPANISH.isAsian())
    }

    @Test
    fun `test mixed language text defaults to dominant script`() {
        // English text with some Chinese characters
        val text = "Hello world 你好 this is mostly English"
        val language = LanguageDetector.detect(text)
        assertEquals("Should detect English as dominant", Language.ENGLISH, language)
    }

    @Test
    fun `test numbers and punctuation do not affect detection`() {
        val text = "123 !@# $%^ 789 ... Hello world"
        val language = LanguageDetector.detect(text)
        assertEquals(Language.ENGLISH, language)
    }
}
