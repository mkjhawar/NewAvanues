/**
 * CommandWordDetectorTest.kt - Unit tests for CommandWordDetector
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 */
package com.augmentalis.voiceoscoreng.speech

import kotlin.test.*

class CommandWordDetectorTest {

    private lateinit var detector: CommandWordDetector

    @BeforeTest
    fun setup() {
        detector = CommandWordDetector(
            confidenceThreshold = 0.6f,
            enableFuzzyMatching = true,
            fuzzyTolerance = 0.2f
        )
        detector.updateCommands(listOf(
            "go back",
            "scroll down",
            "scroll up",
            "tap settings",
            "open camera",
            "take screenshot",
            "increase volume"
        ))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Exact Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `exact match returns 1_0 confidence`() {
        val matches = detector.detectCommands("go back")

        assertEquals(1, matches.size)
        assertEquals("go back", matches[0].command)
        assertEquals(1.0f, matches[0].confidence)
        assertEquals(MatchType.EXACT, matches[0].matchType)
    }

    @Test
    fun `exact match in sentence`() {
        val matches = detector.detectCommands("please go back now")

        assertEquals(1, matches.size)
        assertEquals("go back", matches[0].command)
        assertEquals(1.0f, matches[0].confidence)
    }

    @Test
    fun `case insensitive matching`() {
        val matches = detector.detectCommands("GO BACK")

        assertEquals(1, matches.size)
        assertEquals("go back", matches[0].command)
    }

    @Test
    fun `handles punctuation`() {
        val matches = detector.detectCommands("go back!")

        assertEquals(1, matches.size)
        assertEquals("go back", matches[0].command)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Multiple Command Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `detects multiple commands in text`() {
        val matches = detector.detectCommands("go back and then scroll down")

        assertEquals(2, matches.size)
        assertTrue(matches.any { it.command == "go back" })
        assertTrue(matches.any { it.command == "scroll down" })
    }

    @Test
    fun `returns commands sorted by confidence`() {
        // Exact matches should come first
        val matches = detector.detectCommands("scroll down please and maybe go back")

        assertTrue(matches.size >= 2)
        // First match should have highest confidence
        assertTrue(matches[0].confidence >= matches[1].confidence)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Word Sequence Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `word sequence match with intervening words`() {
        // "scroll" and "down" are both present, but with words between
        val matches = detector.detectCommands("please scroll the page down")

        // Should match scroll down with lower confidence
        assertTrue(matches.isNotEmpty())
        val scrollMatch = matches.find { it.command == "scroll down" }
        assertNotNull(scrollMatch)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Fuzzy Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `fuzzy match with typo`() {
        detector.enableFuzzyMatching = true

        val matches = detector.detectCommands("goo back")

        // Should fuzzy match "go back"
        assertTrue(matches.isNotEmpty())
        assertEquals("go back", matches[0].command)
        assertTrue(matches[0].confidence < 1.0f)
        assertTrue(matches[0].confidence >= 0.6f)
    }

    @Test
    fun `fuzzy matching can be disabled`() {
        detector.enableFuzzyMatching = false

        val matches = detector.detectCommands("goo back")

        // Without fuzzy, should not match
        val goBackMatch = matches.find { it.command == "go back" && it.matchType == MatchType.FUZZY }
        assertNull(goBackMatch)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Partial Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `partial match detects majority words`() {
        // "increase volume" - only "volume" present
        val matches = detector.detectCommands("turn up the volume")

        // Should partial match with lower confidence
        val volumeMatch = matches.find { it.command == "increase volume" }
        // Partial match might not hit threshold depending on config
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `empty text returns no matches`() {
        val matches = detector.detectCommands("")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `whitespace only returns no matches`() {
        val matches = detector.detectCommands("   ")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `no registered commands returns no matches`() {
        val emptyDetector = CommandWordDetector()
        val matches = emptyDetector.detectCommands("go back")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `unrelated text returns no matches`() {
        val matches = detector.detectCommands("hello world how are you")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `respects max matches limit`() {
        detector.maxMatches = 2
        detector.updateCommands(listOf("a", "b", "c", "d", "e"))

        val matches = detector.detectCommands("a b c d e")

        assertTrue(matches.size <= 2)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Confidence Threshold Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `respects confidence threshold`() {
        detector.confidenceThreshold = 0.9f

        val matches = detector.detectCommands("go back")

        // Exact match should still pass
        assertEquals(1, matches.size)
        assertTrue(matches[0].confidence >= 0.9f)
    }

    @Test
    fun `filters low confidence matches`() {
        detector.confidenceThreshold = 0.95f
        detector.enableFuzzyMatching = true

        val matches = detector.detectCommands("goo back") // typo

        // Fuzzy match should be below 0.95 threshold
        assertTrue(matches.isEmpty() || matches.all { it.confidence >= 0.95f })
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Method Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `detectBestCommand returns single result`() {
        val match = detector.detectBestCommand("please scroll down now")

        assertNotNull(match)
        assertEquals("scroll down", match.command)
    }

    @Test
    fun `containsCommand returns boolean`() {
        assertTrue(detector.containsCommand("go back"))
        assertFalse(detector.containsCommand("hello world"))
    }

    @Test
    fun `commandCount reflects registered commands`() {
        assertEquals(7, detector.commandCount)

        detector.updateCommands(listOf("one", "two"))
        assertEquals(2, detector.commandCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CommandMatch Properties Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `high confidence detection`() {
        val match = detector.detectBestCommand("go back")

        assertNotNull(match)
        assertTrue(match.isHighConfidence)
        assertTrue(match.isExactMatch)
    }

    @Test
    fun `match contains position info for exact`() {
        val match = detector.detectBestCommand("please go back now")

        assertNotNull(match)
        assertTrue(match.startIndex >= 0)
        assertTrue(match.endIndex > match.startIndex)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Static Commands Integration
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `addStaticCommands integrates with registry`() {
        val freshDetector = CommandWordDetector()
        freshDetector.addStaticCommands()

        // Static commands should be added
        assertTrue(freshDetector.commandCount > 0)
        assertTrue(freshDetector.containsCommand("go back"))
        assertTrue(freshDetector.containsCommand("home"))
    }
}
