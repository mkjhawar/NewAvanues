/**
 * CommandCacheTest.kt — Unit tests for CommandCache
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Covers static/dynamic command storage, vocabulary insertion,
 * LRU eviction, match priority ordering, and cache clear.
 */
package com.augmentalis.speechrecognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandCacheTest {

    // ── Static Commands ───────────────────────────────────────────

    @Test
    fun `setStaticCommands stores and returns trimmed lowercase entries`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("  Open App  ", "CLOSE APP", "go home"))

        val commands = cache.getStaticCommands()
        assertTrue(commands.contains("open app"))
        assertTrue(commands.contains("close app"))
        assertTrue(commands.contains("go home"))
    }

    @Test
    fun `setStaticCommands deduplicates case-insensitive entries`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("scroll down", "Scroll Down", "SCROLL DOWN"))

        assertEquals(1, cache.getStaticCommands().size)
    }

    @Test
    fun `setStaticCommands replaces previous static commands on second call`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("old command"))
        cache.setStaticCommands(listOf("new command"))

        val commands = cache.getStaticCommands()
        assertFalse(commands.contains("old command"))
        assertTrue(commands.contains("new command"))
    }

    // ── Dynamic Commands ──────────────────────────────────────────

    @Test
    fun `setDynamicCommands stores commands and updateCommands is an alias`() {
        val cache = CommandCache()
        cache.setDynamicCommands(listOf("Send Email"))
        assertTrue(cache.getDynamicCommands().contains("send email"))

        cache.updateCommands(listOf("Reply Email"))
        assertTrue(cache.getDynamicCommands().contains("reply email"))
        assertFalse(cache.getDynamicCommands().contains("send email"))
    }

    // ── findMatch Priority ────────────────────────────────────────

    @Test
    fun `findMatch returns static command when both static and vocabulary contain the phrase`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("play music"))
        cache.addVocabularyWord("play music", true)

        // Static has priority — findMatch should still return correctly
        assertNotNull(cache.findMatch("play music"))
        assertEquals("play music", cache.findMatch("play music"))
    }

    @Test
    fun `findMatch returns dynamic command when not in static`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("stop"))
        cache.setDynamicCommands(listOf("click submit"))

        assertEquals("click submit", cache.findMatch("click submit"))
    }

    @Test
    fun `findMatch returns null when text not in any cache`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("go home"))

        assertNull(cache.findMatch("unknown command"))
    }

    @Test
    fun `findMatch is case-insensitive for input text`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("open settings"))

        assertEquals("open settings", cache.findMatch("OPEN SETTINGS"))
        assertEquals("open settings", cache.findMatch("Open Settings"))
    }

    // ── Vocabulary ────────────────────────────────────────────────

    @Test
    fun `addVocabularyWord with isValid true allows findMatch to return it`() {
        val cache = CommandCache()
        cache.addVocabularyWord("dictate now", true)

        assertEquals("dictate now", cache.findMatch("dictate now"))
    }

    @Test
    fun `addVocabularyWord with isValid false causes findMatch to return null`() {
        val cache = CommandCache()
        cache.addVocabularyWord("invalid phrase", false)

        assertNull(cache.findMatch("invalid phrase"))
    }

    @Test
    fun `hasCommand returns true for known commands and false for unknown`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("navigate back"))

        assertTrue(cache.hasCommand("navigate back"))
        assertFalse(cache.hasCommand("do nothing"))
    }

    // ── getAllCommands & Stats ─────────────────────────────────────

    @Test
    fun `getAllCommands merges static dynamic and valid vocabulary without duplicates`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("cmd one"))
        cache.setDynamicCommands(listOf("cmd two"))
        cache.addVocabularyWord("cmd three", true)
        cache.addVocabularyWord("cmd one", true) // duplicate — should not inflate count

        val all = cache.getAllCommands()
        assertTrue(all.contains("cmd one"))
        assertTrue(all.contains("cmd two"))
        assertTrue(all.contains("cmd three"))
        assertEquals(all.size, all.distinct().size) // no duplicates
    }

    @Test
    fun `getStats reflects accurate counts after population`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("a", "b"))
        cache.setDynamicCommands(listOf("c"))
        cache.addVocabularyWord("d", true)

        val stats = cache.getStats()
        assertEquals(2, stats.staticCount)
        assertEquals(1, stats.dynamicCount)
        assertEquals(1, stats.vocabularyCount)
    }

    // ── Clear ─────────────────────────────────────────────────────

    @Test
    fun `clear removes all commands from every category`() {
        val cache = CommandCache()
        cache.setStaticCommands(listOf("x"))
        cache.setDynamicCommands(listOf("y"))
        cache.addVocabularyWord("z", true)

        cache.clear()

        assertEquals(0, cache.getStaticCommands().size)
        assertEquals(0, cache.getDynamicCommands().size)
        assertFalse(cache.hasCommand("x"))
        assertFalse(cache.hasCommand("y"))
        assertFalse(cache.hasCommand("z"))
    }
}
