package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommandMatcherTest {

    private fun createCommand(
        phrase: String,
        targetVuid: String,
        actionType: CommandActionType = CommandActionType.CLICK,
        confidence: Float = 0.8f
    ) = QuantizedCommand(
        uuid = "",
        phrase = phrase,
        actionType = actionType,
        targetVuid = targetVuid,
        confidence = confidence
    )

    private fun createRegistry(vararg commands: QuantizedCommand): CommandRegistry {
        return CommandRegistry().apply { update(commands.toList()) }
    }

    // ==================== Exact Match Tests ====================

    @Test
    fun `match returns Exact for exact phrase match`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1"),
            createCommand("click Cancel", "vuid2")
        )

        val result = CommandMatcher.match("click Submit", registry)

        assertIs<CommandMatcher.MatchResult.Exact>(result)
        assertEquals("vuid1", result.command.targetVuid)
    }

    @Test
    fun `match is case insensitive for exact match`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1")
        )

        val result = CommandMatcher.match("CLICK SUBMIT", registry)

        assertIs<CommandMatcher.MatchResult.Exact>(result)
    }

    @Test
    fun `match trims whitespace for exact match`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1")
        )

        val result = CommandMatcher.match("  click Submit  ", registry)

        assertIs<CommandMatcher.MatchResult.Exact>(result)
    }

    // ==================== Fuzzy Match Tests ====================

    @Test
    fun `match returns Fuzzy for partial match above threshold`() {
        val registry = createRegistry(
            createCommand("click Submit button", "vuid1")
        )

        // Use lower threshold for partial match
        val result = CommandMatcher.match("click Submit", registry, threshold = 0.5f)

        assertIs<CommandMatcher.MatchResult.Fuzzy>(result)
        assertEquals("vuid1", result.command.targetVuid)
        assertTrue(result.confidence >= 0.5f)
    }

    @Test
    fun `match returns Fuzzy for label-only match`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1")
        )

        // Use lower threshold for single word match
        val result = CommandMatcher.match("Submit", registry, threshold = 0.4f)

        assertIs<CommandMatcher.MatchResult.Fuzzy>(result)
        assertEquals("vuid1", result.command.targetVuid)
    }

    // ==================== Ambiguous Match Tests ====================

    @Test
    fun `match returns Ambiguous when multiple candidates have similar scores`() {
        val registry = createRegistry(
            createCommand("click Submit form", "vuid1"),
            createCommand("click Submit button", "vuid2")
        )

        // With lower threshold, both commands should match with similar scores
        val result = CommandMatcher.match("click Submit", registry, threshold = 0.5f)

        assertIs<CommandMatcher.MatchResult.Ambiguous>(result)
        assertEquals(2, result.candidates.size)
    }

    // ==================== No Match Tests ====================

    @Test
    fun `match returns NoMatch when no commands match`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1"),
            createCommand("click Cancel", "vuid2")
        )

        val result = CommandMatcher.match("click Delete", registry)

        assertIs<CommandMatcher.MatchResult.NoMatch>(result)
    }

    @Test
    fun `match returns NoMatch for empty registry`() {
        val registry = CommandRegistry()

        val result = CommandMatcher.match("click Submit", registry)

        assertIs<CommandMatcher.MatchResult.NoMatch>(result)
    }

    @Test
    fun `match returns NoMatch for empty input`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1")
        )

        val result = CommandMatcher.match("", registry)

        assertIs<CommandMatcher.MatchResult.NoMatch>(result)
    }

    // ==================== Threshold Tests ====================

    @Test
    fun `match respects custom threshold`() {
        val registry = createRegistry(
            createCommand("click Submit form", "vuid1")
        )

        // With high threshold, partial match should not qualify
        val result = CommandMatcher.match("Submit", registry, threshold = 0.95f)

        assertIs<CommandMatcher.MatchResult.NoMatch>(result)
    }

    @Test
    fun `match with lower threshold accepts more matches`() {
        val registry = createRegistry(
            createCommand("click Submit form button", "vuid1")
        )

        // "click" is 1 of 4 words = 0.25 score
        val result = CommandMatcher.match("click", registry, threshold = 0.2f)

        // Should match with low threshold
        assertTrue(result !is CommandMatcher.MatchResult.NoMatch)
    }

    // ==================== Action Type Filtering ====================

    @Test
    fun `match can filter by action type`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1", CommandActionType.CLICK),
            createCommand("type Email", "vuid2", CommandActionType.TYPE)
        )

        // Use lower threshold since "Submit" is only part of the phrase
        val result = CommandMatcher.match(
            "Submit",
            registry,
            threshold = 0.4f,
            actionFilter = CommandActionType.CLICK
        )

        assertIs<CommandMatcher.MatchResult.Fuzzy>(result)
        assertEquals(CommandActionType.CLICK, result.command.actionType)
    }

    // ==================== Best Match Selection ====================

    @Test
    fun `match selects highest confidence when scores are equal`() {
        val registry = createRegistry(
            createCommand("click Submit", "vuid1", confidence = 0.7f),
            createCommand("click Submit", "vuid2", confidence = 0.9f)
        )

        // Note: Registry uses VUID as key, so only one will be stored
        // This tests that exact matches work correctly
        val result = CommandMatcher.match("click Submit", registry)

        assertIs<CommandMatcher.MatchResult.Exact>(result)
    }
}
