/**
 * SynonymParserTest.kt - Unit tests for SynonymParser
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 */
package com.augmentalis.voiceoscoreng.synonym

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SynonymParserTest {

    @Test
    fun `test parse basic syn file`() {
        val content = """
            @meta
            language = en
            version = 1.0

            @synonyms
            click | tap, press
            scroll_up | swipe up, go up
        """.trimIndent()

        val map = SynonymParser.parse(content)

        assertEquals("en", map.languageCode)
        assertEquals(2, map.size)
        assertEquals("click", map.getCanonical("tap"))
        assertEquals("scroll_up", map.getCanonical("swipe up"))
    }

    @Test
    fun `test parse handles comments`() {
        val content = """
            # This is a comment
            @meta
            language = en
            version = 1.0

            @synonyms
            # UI Actions
            click | tap, press
        """.trimIndent()

        val map = SynonymParser.parse(content)

        assertEquals(1, map.size)
        assertEquals("click", map.getCanonical("tap"))
    }

    @Test
    fun `test parse with script metadata`() {
        val content = """
            @meta
            language = ar
            version = 1.0
            script = arabic
            direction = rtl

            @synonyms
            click | انقر, اضغط
        """.trimIndent()

        val map = SynonymParser.parse(content)

        assertEquals("ar", map.languageCode)
        assertEquals(ScriptType.ARABIC, map.metadata.script)
        assertTrue(map.metadata.isRtl)
    }

    @Test
    fun `test parse with CJK tokenizer`() {
        val content = """
            @meta
            language = ja
            version = 1.0
            script = cjk_japanese
            tokenizer = morphological

            @synonyms
            click | タップ, 押す
        """.trimIndent()

        val map = SynonymParser.parse(content)

        assertEquals(ScriptType.CJK_JAPANESE, map.metadata.script)
        assertEquals(TokenizerType.MORPHOLOGICAL, map.metadata.tokenizer)
    }

    @Test
    fun `test parse uses default language if not specified`() {
        val content = """
            @synonyms
            click | tap
        """.trimIndent()

        val map = SynonymParser.parse(content, defaultLanguage = "fr")

        assertEquals("fr", map.languageCode)
    }

    @Test
    fun `test parse throws on empty file`() {
        val content = """
            # Just comments
            # No synonyms
        """.trimIndent()

        assertFailsWith<SynonymParseException> {
            SynonymParser.parse(content)
        }
    }

    @Test
    fun `test parse handles inline without sections`() {
        val content = """
            language = en
            version = 1.0
            click | tap, press
            scroll_up | swipe up
        """.trimIndent()

        val map = SynonymParser.parse(content)

        assertEquals(2, map.size)
    }

    @Test
    fun `test serialize produces valid output`() {
        val original = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press"))
            .add("back", listOf("go back"))
            .build()

        val serialized = SynonymParser.serialize(original)
        val parsed = SynonymParser.parse(serialized)

        assertEquals(original.size, parsed.size)
        assertEquals("click", parsed.getCanonical("tap"))
        assertEquals("back", parsed.getCanonical("go back"))
    }

    @Test
    fun `test validate returns errors for invalid content`() {
        val content = """
            @synonyms
            click |
            | tap
            invalid line without pipe
        """.trimIndent()

        val errors = SynonymParser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("language") })
    }

    @Test
    fun `test validate returns empty for valid content`() {
        val content = """
            language = en
            @synonyms
            click | tap
        """.trimIndent()

        val errors = SynonymParser.validate(content)

        assertTrue(errors.isEmpty())
    }
}
