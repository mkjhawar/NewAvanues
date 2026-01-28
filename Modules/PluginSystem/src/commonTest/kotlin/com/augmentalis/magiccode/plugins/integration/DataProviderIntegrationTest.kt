/**
 * DataProviderIntegrationTest.kt - Data provider integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Comprehensive integration tests for data providers including:
 * - AccessibilityDataProviderImpl tests
 * - CachedAccessibilityData tests
 * - StateFlow reactivity tests
 * - Thread safety tests
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.magiccode.plugins.universal.data.*
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.element.ElementType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for accessibility data providers.
 *
 * Tests data retrieval, caching, flow reactivity, and thread safety.
 */
class DataProviderIntegrationTest {

    // =========================================================================
    // Mock Data Provider Tests
    // =========================================================================

    @Test
    fun testGetScreenElements() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement("ELM_1", "Submit Button"),
            TestUtils.createMockQuantizedElement("ELM_2", "Cancel Button"),
            TestUtils.createMockQuantizedElement("ELM_3", "Username Field", ElementType.TEXT_FIELD)
        )
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act
        val result = provider.getCurrentScreenElements()

        // Assert
        assertEquals(3, result.size, "Should return 3 elements")
        assertEquals(1, provider.getElementsCallCount, "Should count one call")
        assertTrue(result.any { it.label == "Submit Button" }, "Should contain Submit Button")
        assertTrue(result.any { it.label == "Cancel Button" }, "Should contain Cancel Button")
        assertTrue(result.any { it.label == "Username Field" }, "Should contain Username Field")
    }

    @Test
    fun testGetElementById() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement("ELM_TARGET", "Target Element"),
            TestUtils.createMockQuantizedElement("ELM_OTHER", "Other Element")
        )
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act: Get existing element
        val found = provider.getElement("ELM_TARGET")

        // Assert
        assertNotNull(found, "Should find element by AVID")
        assertEquals("Target Element", found.label, "Should have correct label")
        assertEquals(1, provider.getElementCallCount, "Should count one call")

        // Act: Get non-existent element
        val notFound = provider.getElement("ELM_NONEXISTENT")

        // Assert
        assertNull(notFound, "Should return null for non-existent AVID")
        assertEquals(2, provider.getElementCallCount, "Should count two calls")
    }

    @Test
    fun testGetLearnedCommands() = runBlocking {
        // Arrange
        val commands = listOf(
            TestUtils.createMockQuantizedCommand("submit form"),
            TestUtils.createMockQuantizedCommand("cancel", actionType = CommandActionType.BACK),
            TestUtils.createMockQuantizedCommand("scroll down", actionType = CommandActionType.SCROLL)
        )
        val provider = MockAccessibilityDataProvider(commands = commands)

        // Act
        val result = provider.getScreenCommands()

        // Assert
        assertEquals(3, result.size, "Should return 3 commands")
        assertEquals(1, provider.getCommandsCallCount, "Should count one call")
        assertTrue(result.any { it.phrase == "submit form" }, "Should contain submit form command")
    }

    @Test
    fun testGetScreenContext() = runBlocking {
        // Arrange
        val screenContext = ScreenContext(
            packageName = "com.test.app",
            activityName = "LoginActivity",
            screenTitle = "Login Screen",
            elementCount = 10,
            primaryAction = "login"
        )
        val provider = MockAccessibilityDataProvider(screenContext = screenContext)

        // Act
        val result = provider.getScreenContext()

        // Assert
        assertEquals("com.test.app", result.packageName)
        assertEquals("LoginActivity", result.activityName)
        assertEquals("Login Screen", result.screenTitle)
        assertEquals(10, result.elementCount)
    }

    @Test
    fun testUpdateMockElements() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider(elements = emptyList())

        // Initial state
        var elements = provider.getCurrentScreenElements()
        assertEquals(0, elements.size, "Should start empty")

        // Act: Update elements
        val newElements = listOf(
            TestUtils.createMockQuantizedElement("NEW_1", "New Element 1"),
            TestUtils.createMockQuantizedElement("NEW_2", "New Element 2")
        )
        provider.setElements(newElements)

        // Assert
        elements = provider.getCurrentScreenElements()
        assertEquals(2, elements.size, "Should have 2 new elements")
        assertTrue(elements.any { it.label == "New Element 1" }, "Should contain new element 1")
    }

    // =========================================================================
    // StateFlow Reactivity Tests
    // =========================================================================

    @Test
    fun testScreenElementsFlow() = runBlocking {
        // Arrange
        val initialElements = listOf(
            TestUtils.createMockQuantizedElement("ELM_1", "Initial Element")
        )
        val provider = MockAccessibilityDataProvider(elements = initialElements)
        val collectedElements = mutableListOf<List<QuantizedElement>>()

        // Start collecting flow
        val collectJob = launch {
            provider.screenElementsFlow.collect { elements ->
                collectedElements.add(elements)
            }
        }

        // Wait for initial value
        delay(50)

        // Act: Update elements
        val updatedElements = listOf(
            TestUtils.createMockQuantizedElement("ELM_2", "Updated Element 1"),
            TestUtils.createMockQuantizedElement("ELM_3", "Updated Element 2")
        )
        provider.setElements(updatedElements)

        // Wait for flow to emit
        delay(50)

        collectJob.cancel()

        // Assert
        assertTrue(collectedElements.size >= 2, "Should collect at least 2 emissions")

        // First emission should be initial
        assertEquals(1, collectedElements[0].size, "First emission should have 1 element")

        // Last emission should be updated
        val lastEmission = collectedElements.last()
        assertEquals(2, lastEmission.size, "Last emission should have 2 elements")
    }

    @Test
    fun testScreenContextFlow() = runBlocking {
        // Arrange
        val initialContext = ScreenContext(
            packageName = "com.initial.app",
            activityName = "InitialActivity",
            screenTitle = "Initial",
            elementCount = 10,
            primaryAction = null
        )
        val provider = MockAccessibilityDataProvider(screenContext = initialContext)
        val collectedContexts = mutableListOf<ScreenContext?>()

        // Start collecting flow
        val collectJob = launch {
            provider.screenContextFlow.collect { context ->
                collectedContexts.add(context)
            }
        }

        delay(50)

        // Act: Update context
        val updatedContext = ScreenContext(
            packageName = "com.updated.app",
            activityName = "UpdatedActivity",
            screenTitle = "Updated",
            elementCount = 15,
            primaryAction = "submit"
        )
        provider.setScreenContext(updatedContext)

        delay(50)

        collectJob.cancel()

        // Assert
        assertTrue(collectedContexts.size >= 2, "Should collect at least 2 emissions")
        assertEquals("com.updated.app", collectedContexts.last()?.packageName)
    }

    @Test
    fun testFlowFirstValue() = runBlocking {
        // Arrange
        val elements = TestUtils.createMockScreenElements(5)
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act: Get first value from flow
        val firstValue = withTimeout(1000) {
            provider.screenElementsFlow.first()
        }

        // Assert
        assertEquals(5, firstValue.size, "Should get 5 elements from first value")
    }

    // =========================================================================
    // Concurrent Access Tests
    // =========================================================================

    @Test
    fun testConcurrentAccess() = runBlocking {
        // Arrange
        val elements = TestUtils.createMockScreenElements(10)
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act: Concurrent reads
        val results = coroutineScope {
            (1..20).map {
                async {
                    provider.getCurrentScreenElements()
                }
            }.awaitAll()
        }

        // Assert: All reads should succeed and return consistent data
        assertTrue(results.all { it.size == 10 }, "All concurrent reads should return 10 elements")
        assertEquals(20, provider.getElementsCallCount, "Should count 20 calls")
    }

    @Test
    fun testConcurrentReadAndWrite() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider(
            elements = TestUtils.createMockScreenElements(5)
        )

        // Act: Concurrent reads and writes
        coroutineScope {
            // Writer coroutine
            launch {
                repeat(10) { i ->
                    provider.setElements(TestUtils.createMockScreenElements(i + 1))
                    delay(10)
                }
            }

            // Reader coroutines
            (1..5).map {
                async {
                    val results = mutableListOf<Int>()
                    repeat(10) {
                        results.add(provider.getCurrentScreenElements().size)
                        delay(5)
                    }
                    results
                }
            }.awaitAll()
        }

        // Assert: No exceptions should have been thrown
        // Final state should be the last written value
        val finalElements = provider.getCurrentScreenElements()
        assertEquals(10, finalElements.size, "Final state should have 10 elements")
    }

    // =========================================================================
    // Screen Context Tests
    // =========================================================================

    @Test
    fun testScreenContextProperties() {
        // Test default/unknown context
        val unknown = ScreenContext(
            packageName = "unknown",
            activityName = "unknown",
            screenTitle = null,
            elementCount = 0,
            primaryAction = null
        )
        assertEquals("unknown", unknown.packageName)
        assertEquals("unknown", unknown.activityName)
        assertNull(unknown.screenTitle)

        // Test custom context
        val custom = ScreenContext(
            packageName = "com.test.app",
            activityName = "TestActivity",
            screenTitle = "Test Screen",
            elementCount = 15,
            primaryAction = "submit"
        )

        assertEquals("com.test.app", custom.packageName)
        assertEquals("TestActivity", custom.activityName)
        assertEquals("Test Screen", custom.screenTitle)
        assertEquals(15, custom.elementCount)
        assertEquals("submit", custom.primaryAction)
    }

    // =========================================================================
    // Handler Context Data Access Tests
    // =========================================================================

    @Test
    fun testHandlerContextElementAccess() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement("BTN_1", "Save", ElementType.BUTTON, "click"),
            TestUtils.createMockQuantizedElement("BTN_2", "Delete", ElementType.BUTTON, "click"),
            TestUtils.createMockQuantizedElement("TXT_1", "Title", ElementType.TEXT, "")
        )

        val context = TestUtils.createMockHandlerContext(elements = elements)

        // Act: Access elements from context
        val buttons = context.elements.filter { it.type == ElementType.BUTTON }
        val clickable = context.elements.filter { it.actions.contains("click") }

        // Assert
        assertEquals(2, buttons.size, "Should have 2 buttons")
        assertEquals(2, clickable.size, "Should have 2 clickable elements")
    }

    @Test
    fun testHandlerContextWithUserPreferences() = runBlocking {
        // Arrange
        val preferences = mapOf<String, Any>(
            "voice_speed" to 1.5,
            "confirm_actions" to true,
            "language" to "en-US"
        )

        val context = TestUtils.createMockHandlerContext(userPreferences = preferences)

        // Assert
        assertEquals(1.5, context.userPreferences["voice_speed"])
        assertEquals(true, context.userPreferences["confirm_actions"])
        assertEquals("en-US", context.userPreferences["language"])
    }

    // =========================================================================
    // Empty Data Tests
    // =========================================================================

    @Test
    fun testEmptyElementsList() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider(elements = emptyList())

        // Act
        val elements = provider.getCurrentScreenElements()
        val element = provider.getElement("ANY_ID")

        // Assert
        assertTrue(elements.isEmpty(), "Should return empty list")
        assertNull(element, "Should return null for any ID")
    }

    @Test
    fun testEmptyCommandsList() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider(commands = emptyList())

        // Act
        val commands = provider.getScreenCommands()

        // Assert
        assertTrue(commands.isEmpty(), "Should return empty list")
    }

    // =========================================================================
    // Data Transformation Tests
    // =========================================================================

    @Test
    fun testElementToElmLine() {
        // Arrange
        val element = TestUtils.createMockQuantizedElement(
            avid = "ELM_TEST_123",
            label = "Test Button",
            type = ElementType.BUTTON,
            actions = "click"
        )

        // Act
        val elmLine = element.toElmLine()

        // Assert
        assertTrue(elmLine.startsWith("ELM:"), "Should start with ELM:")
        assertTrue(elmLine.contains("ELM_TEST_123"), "Should contain AVID")
        assertTrue(elmLine.contains("Test Button"), "Should contain label")
        assertTrue(elmLine.contains("BUTTON"), "Should contain type")
    }

    @Test
    fun testCommandToCmdLine() {
        // Arrange
        val command = QuantizedCommand(
            avid = "CMD_TEST_123",
            phrase = "click button",
            actionType = CommandActionType.CLICK,
            targetAvid = "ELM_TARGET",
            confidence = 0.95f
        )

        // Act
        val cmdLine = command.toCmdLine()

        // Assert
        assertTrue(cmdLine.startsWith("CMD:"), "Should start with CMD:")
        assertTrue(cmdLine.contains("CMD_TEST_123"), "Should contain AVID")
        assertTrue(cmdLine.contains("click button"), "Should contain phrase")
        assertTrue(cmdLine.contains("CLICK"), "Should contain action type")
        assertTrue(cmdLine.contains("ELM_TARGET"), "Should contain target AVID")
    }

    // =========================================================================
    // Navigation Graph Tests
    // =========================================================================

    @Test
    fun testEmptyNavigationGraph() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider()

        // Act
        val navGraph = provider.getNavigationGraph("com.test.app")

        // Assert
        assertEquals("com.test.app", navGraph.packageName)
        assertTrue(navGraph.isEmpty(), "Navigation graph should be empty")
    }

    // =========================================================================
    // Command History Tests
    // =========================================================================

    @Test
    fun testEmptyCommandHistory() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider()

        // Act
        val history = provider.getCommandHistory(limit = 10)

        // Assert
        assertTrue(history.isEmpty(), "Command history should be empty")
    }

    @Test
    fun testEmptyTopCommands() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider()

        // Act
        val topCommands = provider.getTopCommands(limit = 5)

        // Assert
        assertTrue(topCommands.isEmpty(), "Top commands should be empty")
    }

    // =========================================================================
    // Provider Reset Tests
    // =========================================================================

    @Test
    fun testProviderCounterReset() = runBlocking {
        // Arrange
        val provider = MockAccessibilityDataProvider(
            elements = TestUtils.createMockScreenElements(5)
        )

        // Make some calls
        provider.getCurrentScreenElements()
        provider.getCurrentScreenElements()
        provider.getElement("ANY")
        provider.getScreenCommands()

        // Assert initial counts
        assertEquals(2, provider.getElementsCallCount)
        assertEquals(1, provider.getElementCallCount)
        assertEquals(1, provider.getCommandsCallCount)

        // Act: Reset counters
        provider.resetCounters()

        // Assert: Counters are reset
        assertEquals(0, provider.getElementsCallCount)
        assertEquals(0, provider.getElementCallCount)
        assertEquals(0, provider.getCommandsCallCount)
    }

    // =========================================================================
    // Element Search Tests
    // =========================================================================

    @Test
    fun testFindElementByLabel() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement("ELM_1", "Submit"),
            TestUtils.createMockQuantizedElement("ELM_2", "Cancel"),
            TestUtils.createMockQuantizedElement("ELM_3", "Help")
        )
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act
        val allElements = provider.getCurrentScreenElements()
        val submitElement = allElements.find { it.label == "Submit" }
        val unknownElement = allElements.find { it.label == "Unknown" }

        // Assert
        assertNotNull(submitElement, "Should find element by label")
        assertEquals("ELM_1", submitElement.avid)
        assertNull(unknownElement, "Should not find non-existent label")
    }

    @Test
    fun testFindElementByType() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement("BTN_1", "Button 1", ElementType.BUTTON),
            TestUtils.createMockQuantizedElement("TXT_1", "Text 1", ElementType.TEXT),
            TestUtils.createMockQuantizedElement("BTN_2", "Button 2", ElementType.BUTTON),
            TestUtils.createMockQuantizedElement("INPUT_1", "Input 1", ElementType.TEXT_FIELD)
        )
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act
        val allElements = provider.getCurrentScreenElements()
        val buttons = allElements.filter { it.type == ElementType.BUTTON }
        val textFields = allElements.filter { it.type == ElementType.TEXT_FIELD }

        // Assert
        assertEquals(2, buttons.size, "Should find 2 buttons")
        assertEquals(1, textFields.size, "Should find 1 text field")
    }

    @Test
    fun testFindElementByAlias() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement(
                avid = "ELM_1",
                label = "Submit",
                aliases = listOf("send", "confirm", "ok")
            )
        )
        val provider = MockAccessibilityDataProvider(elements = elements)

        // Act
        val allElements = provider.getCurrentScreenElements()
        val elementWithAlias = allElements.find {
            it.label == "Submit" || it.aliases.contains("confirm")
        }

        // Assert
        assertNotNull(elementWithAlias, "Should find element by label or alias")
        assertTrue(elementWithAlias.aliases.contains("send"))
        assertTrue(elementWithAlias.aliases.contains("confirm"))
        assertTrue(elementWithAlias.aliases.contains("ok"))
    }
}
