package com.augmentalis.voiceoscore

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for StaticCommandRegistry — in-memory static voice command store.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class StaticCommandRegistryTest {

    private fun makeCommand(
        id: String,
        phrases: List<String>,
        category: CommandCategory = CommandCategory.NAVIGATION,
        action: CommandActionType = CommandActionType.NAVIGATE,
        description: String = "Test command",
        domain: String = "app"
    ) = StaticCommand(
        id = id,
        phrases = phrases,
        actionType = action,
        category = category,
        description = description,
        domain = domain
    )

    @BeforeTest
    fun setup() {
        StaticCommandRegistry.reset()
    }

    @AfterTest
    fun teardown() {
        StaticCommandRegistry.reset()
    }

    // ── initialize / isInitialized ────────────────────────────────────────────

    @Test
    fun isInitialized_false_before_initialize() {
        assertFalse(StaticCommandRegistry.isInitialized())
    }

    @Test
    fun isInitialized_true_after_initialize() {
        StaticCommandRegistry.initialize(emptyList())
        assertTrue(StaticCommandRegistry.isInitialized())
    }

    @Test
    fun reset_clears_initialization_state() {
        StaticCommandRegistry.initialize(listOf(makeCommand("nav_back", listOf("go back"))))
        assertTrue(StaticCommandRegistry.isInitialized())
        StaticCommandRegistry.reset()
        assertFalse(StaticCommandRegistry.isInitialized())
    }

    // ── all / commandCount / phraseCount ──────────────────────────────────────

    @Test
    fun all_returns_empty_list_when_not_initialized() {
        assertTrue(StaticCommandRegistry.all().isEmpty())
    }

    @Test
    fun commandCount_and_phraseCount_reflect_initialized_data() {
        val commands = listOf(
            makeCommand("nav_back", listOf("go back", "back")),
            makeCommand("nav_home", listOf("go home"))
        )
        StaticCommandRegistry.initialize(commands)
        assertEquals(2, StaticCommandRegistry.commandCount)
        assertEquals(3, StaticCommandRegistry.phraseCount)
    }

    // ── findByPhrase ──────────────────────────────────────────────────────────

    @Test
    fun findByPhrase_exact_match_case_insensitive() {
        StaticCommandRegistry.initialize(
            listOf(makeCommand("nav_back", listOf("Go Back", "go back")))
        )
        val result = StaticCommandRegistry.findByPhrase("go back")
        assertNotNull(result)
        assertEquals("nav_back", result.id)
    }

    @Test
    fun findByPhrase_returns_null_when_no_match() {
        StaticCommandRegistry.initialize(
            listOf(makeCommand("nav_back", listOf("go back")))
        )
        assertNull(StaticCommandRegistry.findByPhrase("open settings"))
    }

    @Test
    fun findByPhrase_trims_whitespace() {
        StaticCommandRegistry.initialize(
            listOf(makeCommand("nav_back", listOf("go back")))
        )
        assertNotNull(StaticCommandRegistry.findByPhrase("  go back  "))
    }

    // ── byCategory ────────────────────────────────────────────────────────────

    @Test
    fun byCategory_returns_only_matching_category_commands() {
        StaticCommandRegistry.initialize(
            listOf(
                makeCommand("nav_back", listOf("go back"), category = CommandCategory.NAVIGATION),
                makeCommand("media_play", listOf("play"), category = CommandCategory.MEDIA,
                    action = CommandActionType.MEDIA_PLAY)
            )
        )
        val navCmds = StaticCommandRegistry.byCategory(CommandCategory.NAVIGATION)
        assertEquals(1, navCmds.size)
        assertEquals("nav_back", navCmds.first().id)
    }

    // ── byDomain ─────────────────────────────────────────────────────────────

    @Test
    fun byDomain_filters_by_domain_string() {
        StaticCommandRegistry.initialize(
            listOf(
                makeCommand("global_back", listOf("go back"), domain = "app"),
                makeCommand("web_refresh", listOf("refresh page"), domain = "web")
            )
        )
        val webCmds = StaticCommandRegistry.byDomain("web")
        assertEquals(1, webCmds.size)
        assertEquals("web_refresh", webCmds.first().id)
    }

    // ── findByPhraseInDomains ─────────────────────────────────────────────────

    @Test
    fun findByPhraseInDomains_non_app_domain_wins_over_app() {
        StaticCommandRegistry.initialize(
            listOf(
                makeCommand("global_refresh", listOf("refresh"), domain = "app"),
                makeCommand("web_refresh", listOf("refresh"), domain = "web")
            )
        )
        val result = StaticCommandRegistry.findByPhraseInDomains("refresh", setOf("app", "web"))
        assertNotNull(result)
        assertEquals("web_refresh", result.id) // specific domain wins
    }

    // ── allPhrases ────────────────────────────────────────────────────────────

    @Test
    fun allPhrases_contains_all_phrase_variants() {
        StaticCommandRegistry.initialize(
            listOf(makeCommand("nav_back", listOf("go back", "navigate back", "back")))
        )
        val phrases = StaticCommandRegistry.allPhrases()
        assertTrue("go back" in phrases)
        assertTrue("navigate back" in phrases)
        assertTrue("back" in phrases)
    }

    // ── StaticCommand.primaryPhrase ───────────────────────────────────────────

    @Test
    fun staticCommand_primaryPhrase_returns_first_phrase() {
        val cmd = makeCommand("test", listOf("primary phrase", "alt phrase"))
        assertEquals("primary phrase", cmd.primaryPhrase)
    }

    // ── toQuantizedCommands ───────────────────────────────────────────────────

    @Test
    fun toQuantizedCommands_produces_one_entry_per_phrase() {
        val cmd = makeCommand("nav_back", listOf("go back", "back", "navigate back"))
        val quantized = cmd.toQuantizedCommands()
        assertEquals(3, quantized.size)
    }

    @Test
    fun toQuantizedCommands_static_commands_have_null_targetAvid() {
        val cmd = makeCommand("nav_back", listOf("go back"))
        val quantized = cmd.toQuantizedCommands()
        assertTrue(quantized.all { it.targetAvid == null })
    }

    @Test
    fun toQuantizedCommands_static_commands_have_confidence_1() {
        val cmd = makeCommand("nav_back", listOf("go back"))
        val quantized = cmd.toQuantizedCommands()
        assertTrue(quantized.all { it.confidence == 1.0f })
    }
}
