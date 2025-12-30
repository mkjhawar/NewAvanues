/**
 * AIContextProviderTest.kt - Unit Tests for AI Context Provider
 *
 * Tests for NLU/LLM integration and command resolution logic.
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System - Test Suite
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlin.test.*

class AIContextProviderTest {

    private fun createTestHierarchy(
        screenId: String = "test-screen",
        screenType: ScreenType = ScreenType.FORM,
        commandableElements: List<CommandableElement> = emptyList(),
        formFields: List<FormField> = emptyList()
    ): ScreenHierarchy {
        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            isInteractive = false,
            depth = 0
        )

        return ScreenHierarchy(
            screenId = screenId,
            screenHash = "hash-$screenId",
            screenType = screenType,
            appContext = AppContext("test.app", "Test App", "test.app"),
            navigationContext = NavigationContext(screenId, null, listOf(screenId)),
            root = root,
            commandableElements = commandableElements,
            formFields = formFields,
            actions = emptyList(),
            dataDisplay = emptyList(),
            complexity = ComplexityScore(10, 5, 3, 2, 1)
        )
    }

    @Test
    fun testUpdateScreen() {
        val provider = AIContextProvider()
        val hierarchy = createTestHierarchy()

        provider.updateScreen(hierarchy)

        assertNotNull(provider.currentHierarchy.value, "Current hierarchy should be set")
        assertEquals("test-screen", provider.currentHierarchy.value?.screenId)
    }

    @Test
    fun testUpdateScreenAddsToHistory() {
        val provider = AIContextProvider()
        val hierarchy1 = createTestHierarchy(screenId = "screen1")
        val hierarchy2 = createTestHierarchy(screenId = "screen2")

        provider.updateScreen(hierarchy1)
        provider.updateScreen(hierarchy2)

        val history = provider.getHistory()
        assertTrue(history.isNotEmpty(), "History should not be empty")
        assertEquals("screen1", history.first().screenId, "First screen should be in history")
    }

    @Test
    fun testUpdateScreenSameHashDoesNotAddToHistory() {
        val provider = AIContextProvider()
        val hierarchy1 = createTestHierarchy(screenId = "screen1")
        val hierarchy2 = hierarchy1.copy() // Same hash

        provider.updateScreen(hierarchy1)
        provider.updateScreen(hierarchy2)

        val history = provider.getHistory()
        assertTrue(history.isEmpty(), "History should be empty for same hash updates")
    }

    @Test
    fun testGetContextForNLU() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn1", "submit button", "Button", "click", listOf("tap"), priority = 10)
        )

        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field")
        )

        val hierarchy = createTestHierarchy(
            commandableElements = commandables,
            formFields = formFields
        )

        provider.updateScreen(hierarchy)
        val nluContext = provider.getContextForNLU()

        assertNotNull(nluContext.screen, "Screen info should be present")
        assertTrue(nluContext.commands.isNotEmpty(), "Commands should be present")
        assertTrue(nluContext.entities.isNotEmpty(), "Entities should be present")
        assertTrue(nluContext.formMode, "Should indicate form mode")
        assertTrue(nluContext.intents.isNotEmpty(), "Intents should be present")
    }

    @Test
    fun testGetContextForNLUEmpty() {
        val provider = AIContextProvider()
        val nluContext = provider.getContextForNLU()

        assertEquals("unknown", nluContext.screen.screenType, "Should have unknown screen type")
        assertTrue(nluContext.commands.isEmpty(), "Should have no commands")
        assertTrue(nluContext.entities.isEmpty(), "Should have no entities")
    }

    @Test
    fun testGetContextForLLM() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn1", "submit button", "Button", "click", emptyList(), priority = 10)
        )

        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field")
        )

        val hierarchy = createTestHierarchy(
            screenType = ScreenType.LOGIN,
            commandableElements = commandables,
            formFields = formFields
        )

        provider.updateScreen(hierarchy)
        val llmContext = provider.getContextForLLM(maxTokens = 200)

        assertNotNull(llmContext, "LLM context should not be null")
        assertTrue(llmContext.contains("Login") || llmContext.contains("login"), "Should mention screen type")
        assertTrue(llmContext.isNotEmpty(), "Context should not be empty")
    }

    @Test
    fun testGetContextForLLMEmpty() {
        val provider = AIContextProvider()
        val llmContext = provider.getContextForLLM()

        assertEquals("No screen currently loaded.", llmContext, "Should return default message")
    }

    @Test
    fun testGetCompactContext() {
        val provider = AIContextProvider()

        val hierarchy = createTestHierarchy(screenType = ScreenType.LOGIN)
        provider.updateScreen(hierarchy)

        val compactContext = provider.getCompactContext()

        assertNotNull(compactContext, "Compact context should not be null")
        assertTrue(compactContext.length < 100, "Compact context should be brief")
    }

    @Test
    fun testResolveCommandSuccess() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-submit", "submit button", "Button", "click", listOf("tap"), priority = 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        val resolution = provider.resolveCommand("click", mapOf("target" to "submit button"))

        assertTrue(resolution is CommandResolution.Success, "Should resolve successfully")
        val success = resolution as CommandResolution.Success
        assertEquals("btn-submit", success.elementId, "Should resolve to correct element")
        assertEquals(1.0f, success.confidence, "Should have full confidence for exact match")
    }

    @Test
    fun testResolveCommandPartialMatch() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-submit", "submit button", "Button", "click", listOf("tap"), priority = 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        val resolution = provider.resolveCommand("click", mapOf("target" to "submit"))

        assertTrue(
            resolution is CommandResolution.Success || resolution is CommandResolution.Ambiguous,
            "Should resolve with partial match"
        )
    }

    @Test
    fun testResolveCommandAmbiguous() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-submit1", "submit button", "Button", "click", emptyList(), priority = 10),
            CommandableElement("btn-submit2", "submit form", "Button", "click", emptyList(), priority = 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        val resolution = provider.resolveCommand("click", mapOf("target" to "submit"))

        assertTrue(resolution is CommandResolution.Ambiguous, "Should be ambiguous with multiple matches")
        val ambiguous = resolution as CommandResolution.Ambiguous
        assertTrue(ambiguous.suggestions.size >= 2, "Should have at least 2 suggestions")
        assertNotNull(ambiguous.bestMatch, "Should have a best match")
    }

    @Test
    fun testResolveCommandFailed() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-submit", "submit button", "Button", "click", emptyList(), priority = 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        val resolution = provider.resolveCommand("click", mapOf("target" to "nonexistent"))

        assertTrue(resolution is CommandResolution.Failed, "Should fail for nonexistent target")
        val failed = resolution as CommandResolution.Failed
        assertTrue(failed.reason.contains("No matching"), "Should indicate no matches")
    }

    @Test
    fun testResolveCommandNoScreen() {
        val provider = AIContextProvider()

        val resolution = provider.resolveCommand("click", mapOf("target" to "submit"))

        assertTrue(resolution is CommandResolution.Failed, "Should fail without screen context")
        val failed = resolution as CommandResolution.Failed
        assertTrue(failed.reason.contains("No screen context"), "Should indicate no screen")
    }

    @Test
    fun testResolveCommandWithPriority() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-low", "button", "Button", "click", emptyList(), priority = 1),
            CommandableElement("btn-high", "button", "Button", "click", emptyList(), priority = 100)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        val resolution = provider.resolveCommand("click", mapOf("target" to "button"))

        when (resolution) {
            is CommandResolution.Success -> {
                assertEquals("btn-high", resolution.elementId, "Should prefer higher priority")
            }
            is CommandResolution.Ambiguous -> {
                assertEquals("btn-high", resolution.bestMatch, "Best match should be higher priority")
            }
            else -> fail("Should resolve successfully or ambiguously")
        }
    }

    @Test
    fun testGetHistory() {
        val provider = AIContextProvider()

        val hierarchy1 = createTestHierarchy(screenId = "screen1")
        val hierarchy2 = createTestHierarchy(screenId = "screen2")
        val hierarchy3 = createTestHierarchy(screenId = "screen3")

        provider.updateScreen(hierarchy1)
        provider.updateScreen(hierarchy2)
        provider.updateScreen(hierarchy3)

        val history = provider.getHistory()

        assertEquals(2, history.size, "History should have 2 entries (max history - 1)")
        assertEquals("screen2", history[0].screenId, "Most recent in history should be screen2")
        assertEquals("screen1", history[1].screenId, "Oldest in history should be screen1")
    }

    @Test
    fun testGetHistoryLimit() {
        val provider = AIContextProvider()

        // Add more screens than history limit (10)
        repeat(15) { i ->
            val hierarchy = createTestHierarchy(screenId = "screen$i")
            provider.updateScreen(hierarchy)
        }

        val history = provider.getHistory()

        assertTrue(history.size <= 10, "History should be limited to 10 entries")
    }

    @Test
    fun testGetStatistics() {
        val provider = AIContextProvider()

        val hierarchy = createTestHierarchy()
        provider.updateScreen(hierarchy)

        val stats = provider.getStatistics()

        assertEquals(1, stats.totalScreensProcessed, "Should count 1 screen processed")
        assertEquals(1, stats.uniqueScreens, "Should count 1 unique screen")
        assertEquals(0, stats.commandResolutions, "Should have 0 resolutions initially")
        assertTrue(stats.averageComplexity > 0.0, "Should have average complexity")
    }

    @Test
    fun testGetStatisticsMultipleScreens() {
        val provider = AIContextProvider()

        val hierarchy1 = createTestHierarchy(screenId = "screen1")
        val hierarchy2 = createTestHierarchy(screenId = "screen2")

        provider.updateScreen(hierarchy1)
        provider.updateScreen(hierarchy2)

        val stats = provider.getStatistics()

        assertEquals(2, stats.totalScreensProcessed, "Should count 2 screens processed")
        assertEquals(2, stats.uniqueScreens, "Should count 2 unique screens")
    }

    @Test
    fun testClear() {
        val provider = AIContextProvider()

        val hierarchy = createTestHierarchy()
        provider.updateScreen(hierarchy)

        provider.clear()

        assertNull(provider.currentHierarchy.value, "Current hierarchy should be null after clear")
        assertTrue(provider.getHistory().isEmpty(), "History should be empty after clear")
    }

    @Test
    fun testCommandResolutionUpdatesStatistics() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-submit", "submit button", "Button", "click", emptyList(), priority = 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        provider.resolveCommand("click", mapOf("target" to "submit button"))

        val stats = provider.getStatistics()
        assertEquals(1, stats.commandResolutions, "Should count 1 command resolution")
    }

    @Test
    fun testResolveCommandWithThreshold() {
        val provider = AIContextProvider()

        val commandables = listOf(
            CommandableElement("btn-submit", "submit button", "Button", "click", emptyList(), priority = 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        provider.updateScreen(hierarchy)

        // Low threshold should accept partial matches
        val resolution1 = provider.resolveCommand("click", mapOf("target" to "sub"), threshold = 0.3f)
        assertTrue(resolution1 is CommandResolution.Success || resolution1 is CommandResolution.Ambiguous,
            "Should accept partial match with low threshold")

        // High threshold should be stricter
        val resolution2 = provider.resolveCommand("click", mapOf("target" to "sub"), threshold = 0.9f)
        assertTrue(resolution2 is CommandResolution.Failed || resolution2 is CommandResolution.Ambiguous,
            "Should be stricter with high threshold")
    }
}
