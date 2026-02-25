/**
 * LogLevelTest.kt — Unit tests for LogLevel
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests priority values, ordering, fromPriority lookup,
 * and enum completeness.
 */
package com.avanues.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogLevelTest {

    // ── Priority Values ───────────────────────────────────────────

    @Test
    fun `priority values match Android Log constants`() {
        assertEquals(2, LogLevel.VERBOSE.priority)
        assertEquals(3, LogLevel.DEBUG.priority)
        assertEquals(4, LogLevel.INFO.priority)
        assertEquals(5, LogLevel.WARN.priority)
        assertEquals(6, LogLevel.ERROR.priority)
        assertEquals(7, LogLevel.ASSERT.priority)
    }

    // ── Ordering ──────────────────────────────────────────────────

    @Test
    fun `levels are ordered by increasing priority`() {
        assertTrue(LogLevel.VERBOSE.priority < LogLevel.DEBUG.priority)
        assertTrue(LogLevel.DEBUG.priority < LogLevel.INFO.priority)
        assertTrue(LogLevel.INFO.priority < LogLevel.WARN.priority)
        assertTrue(LogLevel.WARN.priority < LogLevel.ERROR.priority)
        assertTrue(LogLevel.ERROR.priority < LogLevel.ASSERT.priority)
    }

    @Test
    fun `VERBOSE is the lowest and ASSERT is the highest priority`() {
        val sorted = LogLevel.entries.sortedBy { it.priority }
        assertEquals(LogLevel.VERBOSE, sorted.first())
        assertEquals(LogLevel.ASSERT, sorted.last())
    }

    // ── fromPriority Lookup ───────────────────────────────────────

    @Test
    fun `fromPriority returns correct level for each known priority`() {
        assertEquals(LogLevel.VERBOSE, LogLevel.fromPriority(2))
        assertEquals(LogLevel.DEBUG, LogLevel.fromPriority(3))
        assertEquals(LogLevel.INFO, LogLevel.fromPriority(4))
        assertEquals(LogLevel.WARN, LogLevel.fromPriority(5))
        assertEquals(LogLevel.ERROR, LogLevel.fromPriority(6))
        assertEquals(LogLevel.ASSERT, LogLevel.fromPriority(7))
    }

    @Test
    fun `fromPriority returns DEBUG as fallback for unknown priority`() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromPriority(0))
        assertEquals(LogLevel.DEBUG, LogLevel.fromPriority(99))
        assertEquals(LogLevel.DEBUG, LogLevel.fromPriority(-1))
    }

    @Test
    fun `fromPriority round-trips all entries`() {
        LogLevel.entries.forEach { level ->
            assertEquals(level, LogLevel.fromPriority(level.priority),
                "fromPriority(${level.priority}) should return $level")
        }
    }

    // ── Completeness ──────────────────────────────────────────────

    @Test
    fun `LogLevel has exactly 6 members`() {
        assertEquals(6, LogLevel.entries.size)
    }

    @Test
    fun `all priorities are unique`() {
        val priorities = LogLevel.entries.map { it.priority }
        assertEquals(priorities.size, priorities.distinct().size)
    }
}
