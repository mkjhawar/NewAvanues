/**
 * VoiceCommandPromptTest.kt - Unit tests for VoiceCommandPrompt
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Tests for LLM prompt generation and response parsing.
 */
package com.augmentalis.voiceoscoreng.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceCommandPromptTest {

    private val sampleCommands = listOf(
        "open settings",
        "go back",
        "scroll down",
        "click submit",
        "navigate home",
        "increase volume",
        "decrease brightness"
    )

    // ═══════════════════════════════════════════════════════════════════════
    // Constants Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `constants have expected values`() {
        assertEquals(50, VoiceCommandPrompt.MAX_COMMANDS_FULL_PROMPT)
        assertEquals(30, VoiceCommandPrompt.MAX_COMMANDS_CONCISE_PROMPT)
        assertEquals(500, VoiceCommandPrompt.MAX_SCHEMA_CHARS)
        assertEquals(3, VoiceCommandPrompt.MIN_PARTIAL_MATCH_LENGTH)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseResponse - Exact Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `parseResponse returns exact match - same case`() {
        val result = VoiceCommandPrompt.parseResponse("open settings", sampleCommands)
        assertEquals("open settings", result)
    }

    @Test
    fun `parseResponse returns exact match - different case`() {
        val result = VoiceCommandPrompt.parseResponse("OPEN SETTINGS", sampleCommands)
        assertEquals("open settings", result)
    }

    @Test
    fun `parseResponse returns exact match - mixed case`() {
        val result = VoiceCommandPrompt.parseResponse("Open Settings", sampleCommands)
        assertEquals("open settings", result)
    }

    @Test
    fun `parseResponse strips Response prefix`() {
        val result = VoiceCommandPrompt.parseResponse("Response: open settings", sampleCommands)
        assertEquals("open settings", result)
    }

    @Test
    fun `parseResponse strips trailing punctuation`() {
        assertEquals("go back", VoiceCommandPrompt.parseResponse("go back.", sampleCommands))
        assertEquals("go back", VoiceCommandPrompt.parseResponse("go back!", sampleCommands))
    }

    @Test
    fun `parseResponse trims whitespace`() {
        val result = VoiceCommandPrompt.parseResponse("  scroll down  ", sampleCommands)
        assertEquals("scroll down", result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseResponse - No Match Indicators Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `parseResponse returns null for NO_MATCH`() {
        assertNull(VoiceCommandPrompt.parseResponse("NO_MATCH", sampleCommands))
    }

    @Test
    fun `parseResponse returns null for no match variants`() {
        assertNull(VoiceCommandPrompt.parseResponse("no match", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("cannot match", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("unable to match", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("no appropriate command", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("none of the commands match", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("doesn't match any command", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("does not match", sampleCommands))
        assertNull(VoiceCommandPrompt.parseResponse("no suitable command found", sampleCommands))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseResponse - Contained Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `parseResponse finds command contained in response`() {
        val result = VoiceCommandPrompt.parseResponse(
            "The best match is: open settings",
            sampleCommands
        )
        assertEquals("open settings", result)
    }

    @Test
    fun `parseResponse finds command when LLM adds explanation`() {
        val result = VoiceCommandPrompt.parseResponse(
            "Based on the user input, I believe the command should be go back",
            sampleCommands
        )
        assertEquals("go back", result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseResponse - Partial Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `parseResponse finds partial match when response is substring of command`() {
        // "scroll" is part of "scroll down"
        val result = VoiceCommandPrompt.parseResponse("scroll", sampleCommands)
        assertEquals("scroll down", result)
    }

    @Test
    fun `parseResponse rejects too short partial matches`() {
        // "go" is only 2 chars, below MIN_PARTIAL_MATCH_LENGTH
        val result = VoiceCommandPrompt.parseResponse("go", sampleCommands)
        assertNull(result)
    }

    @Test
    fun `parseResponse accepts partial match at MIN_PARTIAL_MATCH_LENGTH`() {
        // If we had a command with a 3-letter unique substring
        val commands = listOf("abc command", "def action")
        val result = VoiceCommandPrompt.parseResponse("abc", commands)
        assertEquals("abc command", result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseResponse - No Match Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `parseResponse returns null for unrecognized input`() {
        val result = VoiceCommandPrompt.parseResponse("play music", sampleCommands)
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for empty response`() {
        val result = VoiceCommandPrompt.parseResponse("", sampleCommands)
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for whitespace only`() {
        val result = VoiceCommandPrompt.parseResponse("   ", sampleCommands)
        assertNull(result)
    }

    @Test
    fun `parseResponse returns null for empty command list`() {
        val result = VoiceCommandPrompt.parseResponse("open settings", emptyList())
        assertNull(result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // create Prompt Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `create generates prompt with all commands`() {
        val prompt = VoiceCommandPrompt.create("open the settings", "", sampleCommands)

        assertTrue(prompt.contains("open settings"))
        assertTrue(prompt.contains("go back"))
        assertTrue(prompt.contains("scroll down"))
        assertTrue(prompt.contains("\"open the settings\""))
        assertTrue(prompt.contains("Response:"))
    }

    @Test
    fun `create truncates commands beyond MAX_COMMANDS_FULL_PROMPT`() {
        val manyCommands = (1..100).map { "command $it" }
        val prompt = VoiceCommandPrompt.create("test", "", manyCommands)

        assertTrue(prompt.contains("command 1"))
        assertTrue(prompt.contains("command 50"))
        assertTrue(prompt.contains("... and 50 more commands"))
    }

    @Test
    fun `create includes schema when provided`() {
        val schema = "category: navigation, media, system"
        val prompt = VoiceCommandPrompt.create("test", schema, sampleCommands)

        assertTrue(prompt.contains("## Command Schema"))
        assertTrue(prompt.contains(schema))
    }

    @Test
    fun `create omits schema section when empty`() {
        val prompt = VoiceCommandPrompt.create("test", "", sampleCommands)

        assertTrue(!prompt.contains("## Command Schema"))
    }

    @Test
    fun `create truncates long schema`() {
        val longSchema = "x".repeat(1000)
        val prompt = VoiceCommandPrompt.create("test", longSchema, sampleCommands)

        // Schema should be truncated to MAX_SCHEMA_CHARS (500)
        assertTrue(prompt.contains("x".repeat(500)))
        assertTrue(!prompt.contains("x".repeat(600)))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // createConcise Prompt Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `createConcise generates shorter prompt`() {
        val concise = VoiceCommandPrompt.createConcise("open settings", sampleCommands)
        val full = VoiceCommandPrompt.create("open settings", "", sampleCommands)

        assertTrue(concise.length < full.length)
        assertTrue(concise.contains("Commands:"))
        assertTrue(concise.contains("Match (or NO_MATCH):"))
    }

    @Test
    fun `createConcise truncates commands at MAX_COMMANDS_CONCISE_PROMPT`() {
        val manyCommands = (1..50).map { "cmd$it" }
        val prompt = VoiceCommandPrompt.createConcise("test", manyCommands)

        // Should only include first 30
        assertTrue(prompt.contains("cmd1"))
        assertTrue(prompt.contains("cmd30"))
        assertTrue(!prompt.contains("cmd31"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseResponseDebug Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `parseResponseDebug returns same result as parseResponse`() {
        // Debug version should return same results
        assertEquals(
            VoiceCommandPrompt.parseResponse("open settings", sampleCommands),
            VoiceCommandPrompt.parseResponseDebug("open settings", sampleCommands)
        )

        assertEquals(
            VoiceCommandPrompt.parseResponse("NO_MATCH", sampleCommands),
            VoiceCommandPrompt.parseResponseDebug("NO_MATCH", sampleCommands)
        )
    }
}
