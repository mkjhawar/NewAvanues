/**
 * SynonymSetTest.kt - Unit tests for SynonymSet, SynonymEntry, and SynonymMap
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.serialization.SynonymSet
import com.augmentalis.voiceoscore.synonym.SynonymEntry
import com.augmentalis.voiceoscore.synonym.SynonymMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class SynonymSetTest {

    // ==================== SynonymSet Creation Tests ====================

    @Test
    fun synonymSet_createsWithWordAndSynonyms() {
        val set = SynonymSet(
            word = "click submit",
            synonyms = listOf("tap submit", "press submit")
        )

        assertEquals("click submit", set.word)
        assertEquals(2, set.synonyms.size)
        assertTrue(set.synonyms.contains("tap submit"))
    }

    @Test
    fun synonymSet_emptyList() {
        val set = SynonymSet(
            word = "unique command",
            synonyms = emptyList()
        )

        assertTrue(set.synonyms.isEmpty())
    }

    @Test
    fun synonymSet_preservesOrder() {
        val synonyms = listOf("first", "second", "third")
        val set = SynonymSet(word = "original", synonyms = synonyms)

        assertEquals("first", set.synonyms[0])
        assertEquals("second", set.synonyms[1])
        assertEquals("third", set.synonyms[2])
    }

    // ==================== SynonymEntry Tests ====================

    @Test
    fun synonymEntry_createsValidEntry() {
        val entry = SynonymEntry(
            canonical = "click",
            synonyms = listOf("tap", "press", "push")
        )

        assertEquals("click", entry.canonical)
        assertEquals(3, entry.synonyms.size)
        assertTrue(entry.synonyms.contains("tap"))
    }

    @Test
    fun synonymEntry_allWordsIncludesCanonical() {
        val entry = SynonymEntry(
            canonical = "click",
            synonyms = listOf("tap", "press")
        )

        assertEquals(3, entry.allWords.size)
        assertTrue(entry.allWords.contains("click"))
        assertTrue(entry.allWords.contains("tap"))
    }

    @Test
    fun synonymEntry_matchesCanonical() {
        val entry = SynonymEntry(
            canonical = "click",
            synonyms = listOf("tap", "press")
        )

        assertTrue(entry.matches("click"))
        assertTrue(entry.matches("CLICK"))
        assertTrue(entry.matches("  Click  "))
    }

    @Test
    fun synonymEntry_matchesSynonym() {
        val entry = SynonymEntry(
            canonical = "click",
            synonyms = listOf("tap", "press")
        )

        assertTrue(entry.matches("tap"))
        assertTrue(entry.matches("TAP"))
        assertTrue(entry.matches("press"))
    }

    @Test
    fun synonymEntry_doesNotMatchUnknown() {
        val entry = SynonymEntry(
            canonical = "click",
            synonyms = listOf("tap", "press")
        )

        assertFalse(entry.matches("unknown"))
        assertFalse(entry.matches("scroll"))
    }

    @Test
    fun synonymEntry_parse_validLine() {
        val entry = SynonymEntry.parse("click | tap, press, push")

        assertNotNull(entry)
        assertEquals("click", entry.canonical)
        assertEquals(3, entry.synonyms.size)
        assertTrue(entry.synonyms.contains("tap"))
    }

    @Test
    fun synonymEntry_parse_skipsComment() {
        val entry = SynonymEntry.parse("# This is a comment")
        assertNull(entry)
    }

    @Test
    fun synonymEntry_parse_skipsEmptyLine() {
        val entry = SynonymEntry.parse("   ")
        assertNull(entry)
    }

    @Test
    fun synonymEntry_parse_invalidFormat() {
        val entry = SynonymEntry.parse("no pipe separator")
        assertNull(entry)
    }

    // ==================== SynonymMap Tests ====================

    @Test
    fun synonymMap_builderCreatesMap() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press"))
            .add("scroll_up", listOf("swipe up"))
            .build()

        assertEquals(2, map.size)
        assertEquals("en", map.languageCode)
    }

    @Test
    fun synonymMap_getCanonical_returnsMappedAction() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press"))
            .build()

        assertEquals("click", map.getCanonical("tap"))
        assertEquals("click", map.getCanonical("press"))
        assertEquals("click", map.getCanonical("click"))
    }

    @Test
    fun synonymMap_getCanonical_caseInsensitive() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertEquals("click", map.getCanonical("TAP"))
        assertEquals("click", map.getCanonical("Tap"))
    }

    @Test
    fun synonymMap_getCanonical_unknownWord() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertNull(map.getCanonical("unknown"))
    }

    @Test
    fun synonymMap_getSynonyms_returnsList() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press", "push"))
            .build()

        val synonyms = map.getSynonyms("click")
        assertEquals(3, synonyms.size)
        assertTrue(synonyms.contains("tap"))
        assertTrue(synonyms.contains("press"))
    }

    @Test
    fun synonymMap_getSynonyms_unknownCanonical() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        val synonyms = map.getSynonyms("unknown")
        assertTrue(synonyms.isEmpty())
    }

    @Test
    fun synonymMap_hasMapping() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap", "press"))
            .build()

        assertTrue(map.hasMapping("tap"))
        assertTrue(map.hasMapping("click"))
        assertFalse(map.hasMapping("unknown"))
    }

    @Test
    fun synonymMap_expand_singleWord() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertEquals("click submit", map.expand("tap submit"))
    }

    @Test
    fun synonymMap_expand_multipleWords() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .add("settings", listOf("preferences"))
            .build()

        assertEquals("click settings", map.expand("tap preferences"))
    }

    @Test
    fun synonymMap_expand_noMapping() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .build()

        assertEquals("unknown word", map.expand("unknown word"))
    }

    @Test
    fun synonymMap_expandWithMultiWord() {
        val map = SynonymMap.Builder("en")
            .add("long_click", listOf("long press", "press and hold"))
            .build()

        assertEquals("long_click submit", map.expandWithMultiWord("long press submit"))
    }

    @Test
    fun synonymMap_getAllCanonicals() {
        val map = SynonymMap.Builder("en")
            .add("click", listOf("tap"))
            .add("scroll_up", listOf("swipe up"))
            .add("scroll_down", listOf("swipe down"))
            .build()

        val canonicals = map.getAllCanonicals()
        assertEquals(3, canonicals.size)
        assertTrue(canonicals.contains("click"))
        assertTrue(canonicals.contains("scroll_up"))
    }

    @Test
    fun synonymMap_empty() {
        val map = SynonymMap.empty("en")

        assertEquals(0, map.size)
        assertEquals("en", map.languageCode)
        assertNull(map.getCanonical("anything"))
    }

    @Test
    fun synonymMap_fromEntries() {
        val entries = listOf(
            SynonymEntry("click", listOf("tap", "press")),
            SynonymEntry("scroll_up", listOf("swipe up"))
        )

        val map = SynonymMap.fromEntries("en", entries)

        assertEquals(2, map.size)
        assertEquals("click", map.getCanonical("tap"))
    }

    @Test
    fun synonymMap_builderVarargs() {
        val map = SynonymMap.Builder("en")
            .add("click", "tap", "press", "push")
            .build()

        assertEquals(3, map.getSynonyms("click").size)
    }
}
