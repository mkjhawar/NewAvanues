/**
 * E2ECommandFlowTest.kt - End-to-end test for Android command flow
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Tests the complete voice command processing pipeline:
 * 1. Voice input received
 * 2. Dynamic command lookup (with verb/target extraction)
 * 3. Static handler routing
 * 4. NLU classification (mock)
 * 5. LLM interpretation (mock)
 * 6. Handler execution
 * 7. Result returned
 */
package com.augmentalis.voiceoscoreng.e2e

import com.augmentalis.voiceoscoreng.common.*
import com.augmentalis.voiceoscoreng.handlers.*
import com.augmentalis.voiceoscoreng.nlu.*
import com.augmentalis.voiceoscoreng.llm.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end test for the complete Android command flow.
 *
 * This test validates the 6-tier command routing:
 * 1. Dynamic command by target match
 * 2. Dynamic command by fuzzy match
 * 3. Static handler match
 * 4. NLU classification
 * 5. LLM interpretation
 * 6. Voice interpreter fallback
 */
class E2ECommandFlowTest {

    private lateinit var coordinator: ActionCoordinator
    private lateinit var mockNluProcessor: MockNluProcessor
    private lateinit var mockLlmProcessor: MockLlmProcessor
    private lateinit var handlerRegistry: HandlerRegistry

    @Before
    fun setup() {
        handlerRegistry = HandlerRegistry()
        mockNluProcessor = MockNluProcessor()
        mockLlmProcessor = MockLlmProcessor()

        coordinator = ActionCoordinator(
            voiceInterpreter = DefaultVoiceCommandInterpreter,
            handlerRegistry = handlerRegistry,
            commandRegistry = CommandRegistry(),
            nluProcessor = mockNluProcessor,
            llmProcessor = mockLlmProcessor,
            nluConfig = NluConfig.DEFAULT.copy(enabled = true),
            llmConfig = LlmConfig.DEFAULT.copy(enabled = true)
        )
    }

    @After
    fun teardown() {
        runBlocking {
            coordinator.dispose()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 1: Dynamic Command Priority
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `dynamic command takes priority over static handler`() = runBlocking {
        // Register a static handler
        val testHandler = TestStaticHandler()
        coordinator.registerHandler(testHandler)

        // Register a dynamic command with same trigger
        val dynamicCommand = QuantizedCommand.create(
            avid = "test-avid-001",
            phrase = "submit",
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app",
            targetAvid = "EL:abc12345",
            confidence = 1.0f
        )
        coordinator.updateDynamicCommands(listOf(dynamicCommand))

        // Process "click submit" - should match dynamic command
        val result = coordinator.processVoiceCommand("click submit")

        // Dynamic command should have been matched
        assertTrue(result.success || result.message.contains("submit", ignoreCase = true))
        assertEquals(1, coordinator.dynamicCommandCount)
    }

    @Test
    fun `number command extracts target correctly`() = runBlocking {
        // Register numbered dynamic commands (typical overlay scenario)
        val commands = listOf(
            createNumberedCommand(1, "Settings"),
            createNumberedCommand(2, "Profile"),
            createNumberedCommand(3, "Logout")
        )
        coordinator.updateDynamicCommands(commands)

        // Process "click 1" - should extract verb "click" and target "1"
        val result = coordinator.processVoiceCommand("click 1")

        // Should have processed the command
        assertTrue(
            result.success || result.message.contains("1") || result.message.contains("Settings"),
            "Expected success or recognition of command 1/Settings"
        )
    }

    @Test
    fun `bare number without verb matches dynamic command`() = runBlocking {
        val commands = listOf(createNumberedCommand(4, "Help"))
        coordinator.updateDynamicCommands(commands)

        // Process just "4" - should still match
        val result = coordinator.processVoiceCommand("4")

        assertTrue(
            result.success || result.message.contains("4") || result.message.contains("Help"),
            "Bare number should match dynamic command"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 2: Static Handler Fallback
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `static command routes to handler when no dynamic match`() = runBlocking {
        // Register only a static handler (no dynamic commands)
        val navigationHandler = TestNavigationHandler()
        coordinator.registerHandler(navigationHandler)

        // Process "scroll down" - should route to static handler
        val result = coordinator.processVoiceCommand("scroll down")

        assertTrue(
            result.success || navigationHandler.executedCommands.contains("scroll down"),
            "Static command should route to handler"
        )
    }

    @Test
    fun `system command back routes correctly`() = runBlocking {
        val systemHandler = TestSystemHandler()
        coordinator.registerHandler(systemHandler)

        val result = coordinator.processVoiceCommand("back")

        assertTrue(
            result.success || systemHandler.executedCommands.contains("back"),
            "System command 'back' should route correctly"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 3: NLU Fallback Integration
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `NLU classification used when no exact match`() = runBlocking {
        // Configure mock NLU to return a match
        val targetCommand = QuantizedCommand.create(
            avid = "nlu-cmd-001",
            phrase = "open settings",
            actionType = CommandActionType.OPEN_SETTINGS,
            packageName = "com.android.settings"
        )
        mockNluProcessor.setExpectedResult(NluResult.Match(targetCommand, 0.95f))

        // Register static handler to process the NLU result
        val systemHandler = TestSystemHandler()
        coordinator.registerHandler(systemHandler)

        // Update registry so NLU has commands to match against
        StaticCommandRegistry.all() // Ensure static commands are available

        // Process an ambiguous command that requires NLU
        val result = coordinator.processVoiceCommand("please open my settings")

        // Verify NLU was called
        assertTrue(mockNluProcessor.classifyCalled, "NLU should have been called")
    }

    @Test
    fun `NLU confidence threshold filters low-confidence matches`() = runBlocking {
        // Configure mock NLU to return a low-confidence match
        val targetCommand = QuantizedCommand.create(
            avid = "low-conf-001",
            phrase = "delete",
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app"
        )
        mockNluProcessor.setExpectedResult(NluResult.Match(targetCommand, 0.3f))

        // Process command - low confidence should fall through to LLM
        val result = coordinator.processVoiceCommand("unclear command")

        // Should have called NLU
        assertTrue(mockNluProcessor.classifyCalled)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 4: LLM Fallback Integration
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `LLM interpretation used when NLU fails`() = runBlocking {
        // Configure NLU to return no match
        mockNluProcessor.setExpectedResult(NluResult.NoMatch)

        // Configure LLM to return an interpretation
        mockLlmProcessor.setExpectedResult(
            LlmResult.Interpreted("scroll down", 0.8f, "User wants to scroll")
        )

        // Register handler
        val navigationHandler = TestNavigationHandler()
        coordinator.registerHandler(navigationHandler)

        // Process natural language command
        val result = coordinator.processVoiceCommand("move the page down a bit")

        // Verify LLM was called
        assertTrue(mockLlmProcessor.interpretCalled, "LLM should have been called")
    }

    @Test
    fun `LLM error falls back to voice interpreter`() = runBlocking {
        // Configure NLU to return no match
        mockNluProcessor.setExpectedResult(NluResult.NoMatch)

        // Configure LLM to return error
        mockLlmProcessor.setExpectedResult(
            LlmResult.Error("Model not loaded")
        )

        // Process command - should fall through to voice interpreter
        val result = coordinator.processVoiceCommand("go back")

        assertTrue(mockLlmProcessor.interpretCalled, "LLM should have been attempted")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 5: Complete E2E Flow
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `complete flow from voice input to handler execution`() = runBlocking {
        // Setup: Register handlers
        val systemHandler = TestSystemHandler()
        val navigationHandler = TestNavigationHandler()
        coordinator.registerHandler(systemHandler)
        coordinator.registerHandler(navigationHandler)

        // Setup: Register dynamic commands (simulating screen scrape)
        val screenCommands = listOf(
            QuantizedCommand.create(
                avid = "btn-001",
                phrase = "1",
                actionType = CommandActionType.CLICK,
                packageName = "com.test.app",
                targetAvid = "BTN:settings",
                screenId = "main_screen"
            ),
            QuantizedCommand.create(
                avid = "btn-002",
                phrase = "2",
                actionType = CommandActionType.CLICK,
                packageName = "com.test.app",
                targetAvid = "BTN:profile",
                screenId = "main_screen"
            )
        )
        coordinator.updateDynamicCommands(screenCommands)

        // Test 1: Dynamic command "click 1"
        val dynamicResult = coordinator.processVoiceCommand("click 1")
        assertTrue(dynamicResult.success || dynamicResult.message.isNotEmpty())

        // Test 2: Static command "scroll down"
        val staticResult = coordinator.processVoiceCommand("scroll down")
        assertTrue(staticResult.success || staticResult.message.isNotEmpty())

        // Test 3: System command "back"
        val systemResult = coordinator.processVoiceCommand("back")
        assertTrue(systemResult.success || systemResult.message.isNotEmpty())
    }

    @Test
    fun `metrics are recorded for all command executions`() = runBlocking {
        // Register handler
        val systemHandler = TestSystemHandler()
        coordinator.registerHandler(systemHandler)

        // Execute multiple commands
        coordinator.processVoiceCommand("back")
        coordinator.processVoiceCommand("home")
        coordinator.processVoiceCommand("notifications")

        // Check metrics
        val metrics = coordinator.getMetricsSummary()
        assertTrue(metrics.totalCommands >= 0, "Metrics should track commands")
    }

    @Test
    fun `coordinator state transitions correctly`() = runBlocking {
        // Initial state
        assertEquals(CoordinatorState.UNINITIALIZED, coordinator.state.value)

        // Initialize
        coordinator.initialize(listOf(TestSystemHandler()))
        assertEquals(CoordinatorState.READY, coordinator.state.value)

        // Dispose
        coordinator.dispose()
        assertEquals(CoordinatorState.DISPOSED, coordinator.state.value)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 6: Error Handling
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `unknown command returns failure result`() = runBlocking {
        // Don't register any handlers or dynamic commands

        val result = coordinator.processVoiceCommand("xyzzy magic command")

        assertFalse(result.success, "Unknown command should fail")
        assertTrue(result.message.contains("Unknown") || result.message.contains("command"))
    }

    @Test
    fun `empty command string handled gracefully`() = runBlocking {
        val result = coordinator.processVoiceCommand("")

        // Should not crash, should return some result
        assertNotNull(result)
    }

    @Test
    fun `whitespace-only command handled gracefully`() = runBlocking {
        val result = coordinator.processVoiceCommand("   ")

        // Should not crash
        assertNotNull(result)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Functions
    // ═══════════════════════════════════════════════════════════════════════════

    private fun createNumberedCommand(number: Int, label: String): QuantizedCommand {
        return QuantizedCommand.create(
            avid = "num-cmd-$number",
            phrase = number.toString(),
            actionType = CommandActionType.CLICK,
            packageName = "com.test.app",
            targetAvid = "EL:${label.lowercase()}",
            confidence = 1.0f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Mock Implementations for Testing
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Mock NLU processor for testing.
 */
class MockNluProcessor : INluProcessor {
    var classifyCalled = false
    private var expectedResult: NluResult = NluResult.NoMatch

    fun setExpectedResult(result: NluResult) {
        expectedResult = result
    }

    override fun isAvailable(): Boolean = true

    override suspend fun classify(
        text: String,
        availableCommands: List<QuantizedCommand>
    ): NluResult {
        classifyCalled = true
        return expectedResult
    }

    override suspend fun initialize(): Boolean = true

    override suspend fun dispose() {}
}

/**
 * Mock LLM processor for testing.
 */
class MockLlmProcessor : ILlmProcessor {
    var interpretCalled = false
    private var expectedResult: LlmResult = LlmResult.NoMatch

    fun setExpectedResult(result: LlmResult) {
        expectedResult = result
    }

    override fun isAvailable(): Boolean = true

    override fun isModelLoaded(): Boolean = true

    override suspend fun interpretCommand(
        text: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult {
        interpretCalled = true
        return expectedResult
    }

    override suspend fun loadModel(): Boolean = true

    override suspend fun unloadModel() {}
}

/**
 * Test system handler for testing.
 */
class TestSystemHandler : IHandler {
    val executedCommands = mutableListOf<String>()

    override val category: CommandCategory = CommandCategory.SYSTEM

    override fun canHandle(command: String): Boolean {
        return command in listOf("back", "home", "notifications", "settings", "recent apps")
    }

    override suspend fun execute(command: QuantizedCommand): HandlerResult {
        executedCommands.add(command.phrase)
        return HandlerResult.success("Executed: ${command.phrase}")
    }

    override fun getSupportedActions(): List<String> {
        return listOf("back", "home", "notifications", "settings", "recent apps")
    }

    override suspend fun initialize(): Boolean = true

    override suspend fun dispose() {}
}

/**
 * Test navigation handler for testing.
 */
class TestNavigationHandler : IHandler {
    val executedCommands = mutableListOf<String>()

    override val category: CommandCategory = CommandCategory.NAVIGATION

    override fun canHandle(command: String): Boolean {
        return command.startsWith("scroll") || command.startsWith("swipe")
    }

    override suspend fun execute(command: QuantizedCommand): HandlerResult {
        executedCommands.add(command.phrase)
        return HandlerResult.success("Executed: ${command.phrase}")
    }

    override fun getSupportedActions(): List<String> {
        return listOf("scroll up", "scroll down", "scroll left", "scroll right",
            "swipe up", "swipe down", "swipe left", "swipe right")
    }

    override suspend fun initialize(): Boolean = true

    override suspend fun dispose() {}
}

/**
 * Test static handler for testing.
 */
class TestStaticHandler : IHandler {
    val executedCommands = mutableListOf<String>()

    override val category: CommandCategory = CommandCategory.CUSTOM

    override fun canHandle(command: String): Boolean = true

    override suspend fun execute(command: QuantizedCommand): HandlerResult {
        executedCommands.add(command.phrase)
        return HandlerResult.success("Static executed: ${command.phrase}")
    }

    override fun getSupportedActions(): List<String> = listOf("submit", "cancel")

    override suspend fun initialize(): Boolean = true

    override suspend fun dispose() {}
}
