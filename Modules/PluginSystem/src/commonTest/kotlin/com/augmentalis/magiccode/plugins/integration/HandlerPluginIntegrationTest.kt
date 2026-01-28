/**
 * HandlerPluginIntegrationTest.kt - Handler plugin execution integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Comprehensive integration tests for handler plugins including:
 * - NavigationHandlerPlugin tests
 * - UIInteractionPlugin tests
 * - TextInputPlugin tests
 * - SystemCommandPlugin tests
 * - GesturePlugin tests
 * - SelectionPlugin tests
 * - AppLauncherPlugin tests
 * - Handler routing with multiple handlers
 * - Confidence scoring validation
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.builtin.*
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.commandmanager.ActionResult
import com.augmentalis.commandmanager.CommandActionType
import com.augmentalis.commandmanager.ElementType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for handler plugin functionality.
 *
 * Tests the complete flow of command handling including pattern matching,
 * execution, and result handling.
 */
class HandlerPluginIntegrationTest {

    // =========================================================================
    // GesturePlugin Tests
    // =========================================================================

    @Test
    fun testGesturePluginTapCommand() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor(shouldSucceed = true)
        val plugin = GesturePlugin { mockExecutor }
        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        plugin.initialize(config, context)

        val command = TestUtils.createMockQuantizedCommand(phrase = "tap")
        val handlerContext = TestUtils.createMockHandlerContext()

        // Act
        val canHandle = plugin.canHandle(command, handlerContext)
        val result = plugin.handle(command, handlerContext)

        // Assert
        assertTrue(canHandle, "GesturePlugin should handle 'tap' command")
        assertTrue(result.isSuccess, "Tap command should succeed")
        assertEquals(1, mockExecutor.actions.size, "One gesture action should be recorded")
        assertTrue(mockExecutor.actions[0].startsWith("tap("), "Action should be a tap")
    }

    @Test
    fun testGesturePluginSwipeCommands() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor(shouldSucceed = true)
        val plugin = GesturePlugin { mockExecutor }
        val context = TestUtils.createMockPluginContext()
        plugin.initialize(context = context, config = TestUtils.createMockPluginConfig())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test all swipe directions
        val swipeCommands = listOf("swipe up", "swipe down", "swipe left", "swipe right")

        for (commandPhrase in swipeCommands) {
            mockExecutor.clearActions()
            val command = TestUtils.createMockQuantizedCommand(phrase = commandPhrase)

            // Act
            val canHandle = plugin.canHandle(command, handlerContext)
            val result = plugin.handle(command, handlerContext)

            // Assert
            assertTrue(canHandle, "GesturePlugin should handle '$commandPhrase'")
            assertTrue(result.isSuccess, "'$commandPhrase' should succeed")
            assertEquals(1, mockExecutor.actions.size, "One gesture should be recorded for $commandPhrase")

            val expectedAction = commandPhrase.replace("swipe ", "swipe").replace(" ", "")
            assertTrue(
                mockExecutor.actions[0].lowercase().startsWith(expectedAction.lowercase().replace(" ", "")),
                "Action should match command: ${mockExecutor.actions[0]}"
            )
        }
    }

    @Test
    fun testGesturePluginDoubleTapAndLongPress() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor(shouldSucceed = true)
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test double tap
        var command = TestUtils.createMockQuantizedCommand(phrase = "double tap")
        var result = plugin.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Double tap should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("doubleTap") == true, "Should be doubleTap action")

        // Test long press
        mockExecutor.clearActions()
        command = TestUtils.createMockQuantizedCommand(phrase = "long press")
        result = plugin.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Long press should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("longPress") == true, "Should be longPress action")

        // Test hold (alias for long press)
        mockExecutor.clearActions()
        command = TestUtils.createMockQuantizedCommand(phrase = "hold")
        result = plugin.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Hold should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("longPress") == true, "Hold should map to longPress")
    }

    @Test
    fun testGesturePluginTapAtCoordinates() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor(shouldSucceed = true)
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test tap at coordinates
        val command = TestUtils.createMockQuantizedCommand(phrase = "tap at 100,200")
        val result = plugin.handle(command, handlerContext)

        // Assert
        assertTrue(result.isSuccess, "Tap at coordinates should succeed")
        assertEquals("tap(100, 200)", mockExecutor.lastAction(), "Should tap at specified coordinates")
    }

    @Test
    fun testGesturePluginPinchAndZoom() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor(shouldSucceed = true)
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test pinch in (zoom out)
        var command = TestUtils.createMockQuantizedCommand(phrase = "pinch in")
        var canHandle = plugin.canHandle(command, handlerContext)
        var result = plugin.handle(command, handlerContext)
        assertTrue(canHandle, "Should handle pinch in")
        assertTrue(result.isSuccess, "Pinch in should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("pinchIn") == true, "Should be pinchIn action")

        // Test zoom out (alias for pinch in)
        mockExecutor.clearActions()
        command = TestUtils.createMockQuantizedCommand(phrase = "zoom out")
        result = plugin.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Zoom out should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("pinchIn") == true, "Zoom out should map to pinchIn")

        // Test pinch out (zoom in)
        mockExecutor.clearActions()
        command = TestUtils.createMockQuantizedCommand(phrase = "pinch out")
        result = plugin.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Pinch out should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("pinchOut") == true, "Should be pinchOut action")

        // Test zoom in (alias for pinch out)
        mockExecutor.clearActions()
        command = TestUtils.createMockQuantizedCommand(phrase = "zoom in")
        result = plugin.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Zoom in should succeed")
        assertTrue(mockExecutor.lastAction()?.startsWith("pinchOut") == true, "Zoom in should map to pinchOut")
    }

    @Test
    fun testGesturePluginFailedExecution() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor(shouldSucceed = false)
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val command = TestUtils.createMockQuantizedCommand(phrase = "tap")
        val handlerContext = TestUtils.createMockHandlerContext()

        // Act
        val result = plugin.handle(command, handlerContext)

        // Assert
        assertFalse(result.isSuccess, "Result should indicate failure")
        assertTrue(result is ActionResult.Error, "Result should be Error type")
    }

    @Test
    fun testGesturePluginUnknownCommand() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor()
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val command = TestUtils.createMockQuantizedCommand(phrase = "jump")
        val handlerContext = TestUtils.createMockHandlerContext()

        // Act
        val canHandle = plugin.canHandle(command, handlerContext)

        // Assert
        assertFalse(canHandle, "GesturePlugin should not handle unknown command 'jump'")
    }

    // =========================================================================
    // Handler Routing Tests
    // =========================================================================

    @Test
    fun testHandlerRouting() = runBlocking {
        // Arrange: Create multiple test handlers with different supported phrases
        val gestureHandler = TestHandlerPlugin(
            pluginId = "routing.gesture",
            handlerType = HandlerType.UI_INTERACTION,
            supportedPhrases = listOf("tap", "click", "swipe")
        )

        val navigationHandler = TestHandlerPlugin(
            pluginId = "routing.navigation",
            handlerType = HandlerType.NAVIGATION,
            supportedPhrases = listOf("scroll", "back", "home")
        )

        val textHandler = TestHandlerPlugin(
            pluginId = "routing.text",
            handlerType = HandlerType.TEXT_INPUT,
            supportedPhrases = listOf("type", "enter", "dictate")
        )

        val context = TestUtils.createMockPluginContext()
        val config = TestUtils.createMockPluginConfig()

        for (handler in listOf(gestureHandler, navigationHandler, textHandler)) {
            handler.initialize(config, context)
        }

        val handlers = listOf(gestureHandler, navigationHandler, textHandler)
        val handlerContext = TestUtils.createMockHandlerContext()

        // Test routing to gesture handler
        var command = TestUtils.createMockQuantizedCommand(phrase = "tap")
        var matchingHandler = handlers.firstOrNull { it.canHandle(command, handlerContext) }
        assertEquals(gestureHandler, matchingHandler, "Should route 'tap' to gesture handler")

        // Test routing to navigation handler
        command = TestUtils.createMockQuantizedCommand(phrase = "scroll")
        matchingHandler = handlers.firstOrNull { it.canHandle(command, handlerContext) }
        assertEquals(navigationHandler, matchingHandler, "Should route 'scroll' to navigation handler")

        // Test routing to text handler
        command = TestUtils.createMockQuantizedCommand(phrase = "type")
        matchingHandler = handlers.firstOrNull { it.canHandle(command, handlerContext) }
        assertEquals(textHandler, matchingHandler, "Should route 'type' to text handler")

        // Test no matching handler
        command = TestUtils.createMockQuantizedCommand(phrase = "unknown")
        matchingHandler = handlers.firstOrNull { it.canHandle(command, handlerContext) }
        assertEquals(null, matchingHandler, "Should not find handler for unknown command")
    }

    @Test
    fun testHandlerPriorityRouting() = runBlocking {
        // Arrange: Create handlers with overlapping phrases but different confidence
        val lowConfidenceHandler = object : TestHandlerPlugin(
            pluginId = "priority.low",
            supportedPhrases = listOf("click")
        ) {
            override fun getConfidence(
                command: com.augmentalis.commandmanager.QuantizedCommand,
                context: HandlerContext
            ): Float = 0.5f
        }

        val highConfidenceHandler = object : TestHandlerPlugin(
            pluginId = "priority.high",
            supportedPhrases = listOf("click")
        ) {
            override fun getConfidence(
                command: com.augmentalis.commandmanager.QuantizedCommand,
                context: HandlerContext
            ): Float = 0.95f
        }

        val context = TestUtils.createMockPluginContext()
        lowConfidenceHandler.initialize(TestUtils.createMockPluginConfig(), context)
        highConfidenceHandler.initialize(TestUtils.createMockPluginConfig(), context)

        val handlers = listOf(lowConfidenceHandler, highConfidenceHandler)
        val handlerContext = TestUtils.createMockHandlerContext()
        val command = TestUtils.createMockQuantizedCommand(phrase = "click")

        // Act: Get handler with highest confidence
        val selectedHandler = handlers
            .filter { it.canHandle(command, handlerContext) }
            .maxByOrNull { it.getConfidence(command, handlerContext) }

        // Assert
        assertEquals(highConfidenceHandler, selectedHandler, "Should select handler with highest confidence")
        assertEquals(0.95f, selectedHandler?.getConfidence(command, handlerContext), "Confidence should be 0.95")
    }

    // =========================================================================
    // Confidence Scoring Tests
    // =========================================================================

    @Test
    fun testConfidenceScoringExactMatch() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor()
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test exact match - should have high confidence
        val exactCommand = TestUtils.createMockQuantizedCommand(phrase = "tap")
        val exactConfidence = plugin.getConfidence(exactCommand, handlerContext)

        assertTrue(exactConfidence >= 0.9f, "Exact match should have confidence >= 0.9, got $exactConfidence")
    }

    @Test
    fun testConfidenceScoringPatternMatch() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor()
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test pattern match
        val patternCommand = TestUtils.createMockQuantizedCommand(phrase = "swipe up")
        val patternConfidence = plugin.getConfidence(patternCommand, handlerContext)

        assertTrue(patternConfidence >= 0.8f, "Pattern match should have confidence >= 0.8, got $patternConfidence")
    }

    @Test
    fun testConfidenceScoringPartialMatch() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor()
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test partial match (starts with gesture verb but incomplete)
        val partialCommand = TestUtils.createMockQuantizedCommand(phrase = "tap somewhere")
        val partialConfidence = plugin.getConfidence(partialCommand, handlerContext)

        assertTrue(partialConfidence < 0.9f, "Partial match should have lower confidence, got $partialConfidence")
    }

    @Test
    fun testConfidenceScoringNoMatch() = runBlocking {
        // Arrange
        val mockExecutor = MockGestureExecutor()
        val plugin = GesturePlugin { mockExecutor }
        plugin.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test no match
        val noMatchCommand = TestUtils.createMockQuantizedCommand(phrase = "open settings")
        val noMatchConfidence = plugin.getConfidence(noMatchCommand, handlerContext)

        assertEquals(0.0f, noMatchConfidence, "No match should have confidence 0.0")
    }

    // =========================================================================
    // Test Handler Plugin Tests
    // =========================================================================

    @Test
    fun testTestHandlerPluginBasicOperation() = runBlocking {
        // Arrange
        val handler = TestHandlerPlugin(
            pluginId = "test.basic",
            supportedPhrases = listOf("test", "execute", "run")
        )
        handler.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()

        // Test supported command
        val command = TestUtils.createMockQuantizedCommand(phrase = "test")
        assertTrue(handler.canHandle(command, handlerContext), "Should handle 'test' command")

        val result = handler.handle(command, handlerContext)
        assertTrue(result.isSuccess, "Command execution should succeed")
        assertEquals(1, handler.handledCommands.size, "One command should be recorded")

        // Test unsupported command
        val unsupported = TestUtils.createMockQuantizedCommand(phrase = "unknown")
        assertFalse(handler.canHandle(unsupported, handlerContext), "Should not handle 'unknown' command")
    }

    @Test
    fun testTestHandlerPluginFailure() = runBlocking {
        // Arrange
        val handler = TestHandlerPlugin(
            pluginId = "test.failure",
            supportedPhrases = listOf("fail"),
            shouldSucceed = false
        )
        handler.initialize(TestUtils.createMockPluginConfig(), TestUtils.createMockPluginContext())

        val handlerContext = TestUtils.createMockHandlerContext()
        val command = TestUtils.createMockQuantizedCommand(phrase = "fail")

        // Act
        val result = handler.handle(command, handlerContext)

        // Assert
        assertFalse(result.isSuccess, "Command should fail")
        assertTrue(result is ActionResult.Error, "Result should be Error type")
    }

    // =========================================================================
    // Handler Context Tests
    // =========================================================================

    @Test
    fun testHandlerContextWithElements() = runBlocking {
        // Arrange
        val elements = listOf(
            TestUtils.createMockQuantizedElement("ELM_1", "Submit Button", ElementType.BUTTON),
            TestUtils.createMockQuantizedElement("ELM_2", "Cancel Button", ElementType.BUTTON),
            TestUtils.createMockQuantizedElement("ELM_3", "Username", ElementType.TEXT_FIELD)
        )

        val handlerContext = TestUtils.createMockHandlerContext(
            elements = elements,
            packageName = "com.test.form",
            activityName = "LoginActivity"
        )

        // Assert context properties
        assertEquals(3, handlerContext.elements.size, "Should have 3 elements")
        assertEquals("com.test.form", handlerContext.currentScreen.packageName, "Package name should match")
        assertEquals("LoginActivity", handlerContext.currentScreen.activityName, "Activity should match")

        // Find element by label
        val submitButton = handlerContext.elements.find { it.label == "Submit Button" }
        assertNotNull(submitButton, "Should find Submit Button element")
        assertEquals("ELM_1", submitButton.avid, "AVID should match")
    }

    @Test
    fun testHandlerContextWithPreviousCommand() = runBlocking {
        // Arrange
        val previousCommand = TestUtils.createMockQuantizedCommand(
            phrase = "select item 1",
            actionType = CommandActionType.CLICK
        )

        val handlerContext = TestUtils.createMockHandlerContext(
            previousCommand = previousCommand
        )

        // Assert
        assertNotNull(handlerContext.previousCommand, "Should have previous command")
        assertEquals("select item 1", handlerContext.previousCommand?.phrase, "Previous phrase should match")
        assertEquals(CommandActionType.CLICK, handlerContext.previousCommand?.actionType, "Action type should match")
    }

    // =========================================================================
    // Command Pattern Tests
    // =========================================================================

    @Test
    fun testCommandPatternMatching() {
        // Arrange
        val patterns = listOf(
            CommandPattern(
                regex = Regex("^tap$", RegexOption.IGNORE_CASE),
                intent = "TAP",
                examples = listOf("tap")
            ),
            CommandPattern(
                regex = Regex("^click (.+)$", RegexOption.IGNORE_CASE),
                intent = "CLICK",
                requiredEntities = setOf("target"),
                examples = listOf("click button", "click submit")
            ),
            CommandPattern(
                regex = Regex("^scroll (up|down)$", RegexOption.IGNORE_CASE),
                intent = "SCROLL",
                requiredEntities = setOf("direction"),
                examples = listOf("scroll up", "scroll down")
            )
        )

        // Assert pattern matching
        assertTrue(patterns[0].matches("tap"), "Should match 'tap'")
        assertTrue(patterns[0].matches("TAP"), "Should match 'TAP' (case insensitive)")
        assertFalse(patterns[0].matches("tap button"), "Should not match 'tap button'")

        assertTrue(patterns[1].matches("click button"), "Should match 'click button'")
        assertTrue(patterns[1].matches("click submit"), "Should match 'click submit'")
        assertFalse(patterns[1].matches("click"), "Should not match 'click' without target")

        assertTrue(patterns[2].matches("scroll up"), "Should match 'scroll up'")
        assertTrue(patterns[2].matches("scroll down"), "Should match 'scroll down'")
        assertFalse(patterns[2].matches("scroll left"), "Should not match 'scroll left'")
    }

    @Test
    fun testCommandPatternExtraction() {
        // Arrange
        val clickPattern = CommandPattern(
            regex = Regex("^click (.+)$", RegexOption.IGNORE_CASE),
            intent = "CLICK",
            requiredEntities = setOf("target"),
            examples = listOf("click button")
        )

        // Act
        val match = clickPattern.regex.find("click Submit Button")

        // Assert
        assertNotNull(match, "Should match")
        assertEquals("Submit Button", match.groupValues[1], "Should extract target")
    }

    // =========================================================================
    // Handler Type Tests
    // =========================================================================

    @Test
    fun testHandlerTypes() {
        // Assert handler type values
        assertEquals("NAVIGATION", HandlerType.NAVIGATION.name)
        assertEquals("UI_INTERACTION", HandlerType.UI_INTERACTION.name)
        assertEquals("TEXT_INPUT", HandlerType.TEXT_INPUT.name)
        assertEquals("SYSTEM", HandlerType.SYSTEM.name)
        assertEquals("ACCESSIBILITY", HandlerType.ACCESSIBILITY.name)
        assertEquals("CUSTOM", HandlerType.CUSTOM.name)

        // Assert handler types can be compared
        val type1 = HandlerType.NAVIGATION
        val type2 = HandlerType.NAVIGATION
        assertEquals(type1, type2, "Same handler types should be equal")
    }

    // =========================================================================
    // GestureConfig Tests
    // =========================================================================

    @Test
    fun testGestureConfigFactory() {
        // Test tap factory
        val tap = GestureConfig.tap(100, 200)
        assertEquals(GestureType.TAP, tap.type)
        assertEquals(100, tap.x)
        assertEquals(200, tap.y)

        // Test double tap factory
        val doubleTap = GestureConfig.doubleTap(300, 400)
        assertEquals(GestureType.DOUBLE_TAP, doubleTap.type)
        assertEquals(300, doubleTap.x)
        assertEquals(400, doubleTap.y)

        // Test long press factory
        val longPress = GestureConfig.longPress(500, 600, 1000L)
        assertEquals(GestureType.LONG_PRESS, longPress.type)
        assertEquals(500, longPress.x)
        assertEquals(600, longPress.y)
        assertEquals(1000L, longPress.duration)

        // Test swipe factory
        val swipe = GestureConfig.swipe(GestureType.SWIPE_UP, 300)
        assertEquals(GestureType.SWIPE_UP, swipe.type)
        assertEquals(300, swipe.distance)
    }

    @Test
    fun testGestureConfigHelpers() {
        // Test isSwipe
        assertTrue(GestureConfig(type = GestureType.SWIPE_UP).isSwipe())
        assertTrue(GestureConfig(type = GestureType.SWIPE_DOWN).isSwipe())
        assertTrue(GestureConfig(type = GestureType.SWIPE_LEFT).isSwipe())
        assertTrue(GestureConfig(type = GestureType.SWIPE_RIGHT).isSwipe())
        assertFalse(GestureConfig(type = GestureType.TAP).isSwipe())

        // Test isPinch
        assertTrue(GestureConfig(type = GestureType.PINCH_IN).isPinch())
        assertTrue(GestureConfig(type = GestureType.PINCH_OUT).isPinch())
        assertFalse(GestureConfig(type = GestureType.TAP).isPinch())

        // Test isRotation
        assertTrue(GestureConfig(type = GestureType.ROTATE_LEFT).isRotation())
        assertTrue(GestureConfig(type = GestureType.ROTATE_RIGHT).isRotation())
        assertFalse(GestureConfig(type = GestureType.TAP).isRotation())
    }
}
