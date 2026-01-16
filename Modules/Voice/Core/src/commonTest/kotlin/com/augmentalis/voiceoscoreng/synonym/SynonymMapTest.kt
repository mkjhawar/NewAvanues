/**
 * SynonymMapTest.kt - Unit tests for SynonymMap
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 */
package com.augmentalis.voiceoscoreng.synonym

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SynonymMapTest {

    @Test
    fun `test builder creates valid map`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press", "push"))
            .add("scroll_up", listOf("swipe up", "go up"))
            .build()

        assertEquals(2, map.size)
        assertEquals("en", map.languageCode)
    }

    @Test
    fun `test getCanonical returns correct action for synonym`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press"))
            .build()

        assertEquals("click", map.getCanonical("tap"))
        assertEquals("click", map.getCanonical("press"))
        assertEquals("click", map.getCanonical("click"))
    }

    @Test
    fun `test getCanonical is case insensitive`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertEquals("click", map.getCanonical("TAP"))
        assertEquals("click", map.getCanonical("Tap"))
        assertEquals("click", map.getCanonical("tap"))
    }

    @Test
    fun `test getCanonical returns null for unknown word`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertNull(map.getCanonical("unknown"))
    }

    @Test
    fun `test getSynonyms returns all synonyms`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press", "push"))
            .build()

        val synonyms = map.getSynonyms("click")
        assertEquals(3, synonyms.size)
        assertTrue("tap" in synonyms)
        assertTrue("press" in synonyms)
        assertTrue("push" in synonyms)
    }

    @Test
    fun `test expand replaces single word synonyms`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertEquals("click submit", map.expand("tap submit"))
    }

    @Test
    fun `test expandWithMultiWord replaces multi-word synonyms`() {
        val map = SynonymMap.Builder("en")
            .add("long_click", listOf("long press", "press and hold"))
            .build()

        assertEquals("long_click submit", map.expandWithMultiWord("long press submit"))
        assertEquals("long_click submit", map.expandWithMultiWord("press and hold submit"))
    }

    @Test
    fun `test expand preserves non-synonym words`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertEquals("click the submit button", map.expand("tap the submit button"))
    }

    @Test
    fun `test empty map returns null for all lookups`() {
        val map = SynonymMap.empty("en")

        assertNull(map.getCanonical("tap"))
        assertTrue(map.getSynonyms("click").isEmpty())
        assertEquals("tap submit", map.expand("tap submit"))
    }

    @Test
    fun `test hasMapping returns correct result`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertTrue(map.hasMapping("tap"))
        assertTrue(map.hasMapping("click"))
        assertTrue(!map.hasMapping("unknown"))
    }

    @Test
    fun `test getAllCanonicals returns all actions`() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .add("scroll_up", listOf("swipe up"))
            .add("back", listOf("go back"))
            .build()

        val canonicals = map.getAllCanonicals()
        assertEquals(3, canonicals.size)
        assertTrue("click" in canonicals)
        assertTrue("scroll_up" in canonicals)
        assertTrue("back" in canonicals)
    }

    @Test
    fun `test metadata is preserved`() {
        val metadata = LanguageMetadata(
            languageCode = "ja",
            languageName = "Japanese",
            script = ScriptType.CJK_JAPANESE,
            tokenizer = TokenizerType.MORPHOLOGICAL
        )

        val map = SynonymMap.Builder("ja")
            .metadata(metadata)
            .add("click", listOf("タップ"))
            .build()

        assertEquals("ja", map.metadata.languageCode)
        assertEquals("Japanese", map.metadata.languageName)
        assertEquals(ScriptType.CJK_JAPANESE, map.metadata.script)
        assertEquals(TokenizerType.MORPHOLOGICAL, map.metadata.tokenizer)
    }
}
