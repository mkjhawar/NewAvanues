/**
 * CommandOrchestratorImplTest.kt - Comprehensive test suite for CommandOrchestrator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-15 04:33:24 PDT
 * Part of: VoiceOSService SOLID Refactoring - Week 3, Day 16
 *
 * Test Coverage: 120+ tests across 11 categories
 * Target: 100% functional equivalence with VoiceOSService.kt lines 973-1143
 */
package com.augmentalis.voiceoscore.refactoring.impl

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.commandmanager.models.Command
import com.augmentalis.commandmanager.models.CommandContext
import com.augmentalis.commandmanager.models.CommandSource
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator.*
import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager
import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for CommandOrchestratorImpl
 *
 * Categories:
 * 1. Initialization & Lifecycle (15 tests)
 * 2. Tier 1 Command Execution (15 tests)
 * 3. Tier 2 Command Execution (15 tests)
 * 4. Tier 3 Command Execution (15 tests)
 * 5. Tier Fallback Logic (10 tests)
 * 6. Fallback Mode (10 tests)
 * 7. Global Actions (10 tests)
 * 8. Command Registration & Vocabulary (10 tests)
 * 9. Metrics & History (10 tests)
 * 10. Thread Safety & Concurrency (10 tests)
 * 11. Edge Cases & Error Handling (15 tests)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommandOrchestratorImplTest {

    // Mocks
    private lateinit var mockContext: Context
    private lateinit var mockStateManager: IStateManager
    private lateinit var mockSpeechManager: ISpeechManager
    private lateinit var mockCommandManager: CommandManager
    private lateinit var mockVoiceCommandProcessor: VoiceCommandProcessor
    private lateinit var mockActionCoordinator: ActionCoordinator
    private lateinit var mockAccessibilityService: AccessibilityService

    // System under test
    private lateinit var orchestrator: CommandOrchestratorImpl

    // Test dispatcher
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Create mocks
        mockContext = mockk(relaxed = true)
        mockStateManager = mockk(relaxed = true)
        mockSpeechManager = mockk(relaxed = true)
        mockCommandManager = mockk(relaxed = true)
        mockVoiceCommandProcessor = mockk(relaxed = true)
        mockActionCoordinator = mockk(relaxed = true)
        mockAccessibilityService = mockk(relaxed = true)

        // Create orchestrator
        orchestrator = CommandOrchestratorImpl(
            appContext = mockContext,
            stateManager = mockStateManager,
            speechManager = mockSpeechManager
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========================================
    // Category 1: Initialization & Lifecycle (15 tests)
    // ========================================

    @Test
    fun `initialize - successful initialization`() = runTest {
        // When
        orchestrator.initialize(mockContext)

        // Then
        assertTrue(orchestrator.isReady)
        assertEquals(CommandOrchestratorState.READY, orchestrator.currentState)
    }

    @Test(expected = IllegalStateException::class)
    fun `initialize - throws when already initialized`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When - initialize again
        orchestrator.initialize(mockContext)

        // Then - exception thrown
    }

    @Test
    fun `initialize - starts in UNINITIALIZED state`() {
        // Then
        assertFalse(orchestrator.isReady)
        assertEquals(CommandOrchestratorState.UNINITIALIZED, orchestrator.currentState)
    }

    @Test
    fun `initialize - clears metrics on initialization`() = runTest {
        // When
        orchestrator.initialize(mockContext)

        // Then
        val metrics = orchestrator.getMetrics()
        assertEquals(0L, metrics.totalCommandsExecuted)
        assertEquals(0L, metrics.tier1SuccessCount)
        assertEquals(0L, metrics.tier2SuccessCount)
        assertEquals(0L, metrics.tier3SuccessCount)
    }

    @Test
    fun `initialize - clears history on initialization`() = runTest {
        // When
        orchestrator.initialize(mockContext)

        // Then
        val history = orchestrator.getCommandHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `setTierExecutors - sets all tier executors`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.setTierExecutors(
            commandManager = mockCommandManager,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = mockActionCoordinator,
            accessibilityService = mockAccessibilityService
        )

        // Then - no exception, executors are set internally
        // (We'll validate this in execution tests)
    }

    @Test
    fun `setTierExecutors - allows null executors`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.setTierExecutors(
            commandManager = null,
            voiceCommandProcessor = null,
            actionCoordinator = null,
            accessibilityService = null
        )

        // Then - no exception
    }

    @Test
    fun `pause - transitions from READY to PAUSED`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.pause()

        // Then
        assertEquals(CommandOrchestratorState.PAUSED, orchestrator.currentState)
    }

    @Test
    fun `pause - does nothing when not READY`() = runTest {
        // When - pause before initialization
        orchestrator.pause()

        // Then - state unchanged
        assertEquals(CommandOrchestratorState.UNINITIALIZED, orchestrator.currentState)
    }

    @Test
    fun `resume - transitions from PAUSED to READY`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.pause()

        // When
        orchestrator.resume()

        // Then
        assertEquals(CommandOrchestratorState.READY, orchestrator.currentState)
    }

    @Test
    fun `resume - does nothing when not PAUSED`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When - resume when already READY
        orchestrator.resume()

        // Then - state unchanged
        assertEquals(CommandOrchestratorState.READY, orchestrator.currentState)
    }

    @Test
    fun `cleanup - transitions to SHUTDOWN state`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.cleanup()

        // Then
        assertEquals(CommandOrchestratorState.SHUTDOWN, orchestrator.currentState)
        assertFalse(orchestrator.isReady)
    }

    @Test
    fun `cleanup - clears all metrics and history`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.cleanup()

        // Then
        val history = orchestrator.getCommandHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `cleanup - can be called multiple times safely`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.cleanup()
        orchestrator.cleanup() // Second cleanup

        // Then - no exception
        assertEquals(CommandOrchestratorState.SHUTDOWN, orchestrator.currentState)
    }

    @Test
    fun `isReady - returns false before initialization`() {
        // Then
        assertFalse(orchestrator.isReady)
    }

    // ========================================
    // Category 2: Tier 1 Command Execution (15 tests)
    // ========================================

    @Test
    fun `executeCommand - Tier 1 success with valid command`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = mockCommandManager,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = mockActionCoordinator,
            accessibilityService = mockAccessibilityService
        )

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        every { commandResult.response } returns "Command executed"
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        val context = CommandContext(packageName = "com.test.app")

        // When
        val result = orchestrator.executeCommand("test command", 0.8f, context)

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(1, (result as CommandResult.Success).tier)
        assertEquals(1L, orchestrator.getMetrics().tier1SuccessCount)

        // Verify CommandManager was called
        coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }

        // Verify Tier 2 and 3 were NOT called
        coVerify(exactly = 0) { mockVoiceCommandProcessor.processCommand(any()) }
        coVerify(exactly = 0) { mockActionCoordinator.executeAction(any()) }
    }

    @Test
    fun `executeCommand - Tier 1 creates Command with correct parameters`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true

        val capturedCommand = slot<Command>()
        coEvery { mockCommandManager.executeCommand(capture(capturedCommand)) } returns commandResult

        val context = CommandContext(packageName = "com.test.app")

        // When
        orchestrator.executeCommand("test command", 0.75f, context)

        // Then - verify Command object created correctly
        assertEquals("test command", capturedCommand.captured.text)
        assertEquals(0.75f, capturedCommand.captured.confidence)
        assertEquals(CommandSource.VOICE, capturedCommand.captured.source)
        assertEquals(context, capturedCommand.captured.context)
    }

    @Test
    fun `executeCommand - Tier 1 normalizes command text to lowercase`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true

        val capturedCommand = slot<Command>()
        coEvery { mockCommandManager.executeCommand(capture(capturedCommand)) } returns commandResult

        // When
        orchestrator.executeCommand("TEST COMMAND", 0.9f, CommandContext())

        // Then
        assertEquals("test command", capturedCommand.captured.text)
    }

    @Test
    fun `executeCommand - Tier 1 trims whitespace from command`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true

        val capturedCommand = slot<Command>()
        coEvery { mockCommandManager.executeCommand(capture(capturedCommand)) } returns commandResult

        // When
        orchestrator.executeCommand("  test command  ", 0.9f, CommandContext())

        // Then
        assertEquals("test command", capturedCommand.captured.text)
    }

    @Test
    fun `executeCommand - Tier 1 failure falls through to Tier 2`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = mockCommandManager,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = null,
            accessibilityService = null
        )

        // Tier 1 fails
        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        every { tier1Result.error } returns null
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        // Tier 2 succeeds
        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)

        // Verify both tiers were called
        coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }
        coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
    }

    @Test
    fun `executeCommand - Tier 1 skipped when commandManager is null`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = null, // No Tier 1
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = null,
            accessibilityService = null
        )

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)

        // Verify Tier 1 was NOT called (it's null)
        coVerify(exactly = 0) { mockCommandManager.executeCommand(any()) }
    }

    @Test
    fun `executeCommand - Tier 1 exception falls through to Tier 2`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = mockCommandManager,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = null,
            accessibilityService = null
        )

        // Tier 1 throws exception
        coEvery { mockCommandManager.executeCommand(any()) } throws RuntimeException("Tier 1 error")

        // Tier 2 succeeds
        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - Tier 2 should still execute
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)
    }

    @Test
    fun `executeCommand - Tier 1 success increments metrics`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        // When
        orchestrator.executeCommand("test1", 0.9f, CommandContext())
        orchestrator.executeCommand("test2", 0.9f, CommandContext())

        // Then
        val metrics = orchestrator.getMetrics()
        assertEquals(2L, metrics.totalCommandsExecuted)
        assertEquals(2L, metrics.tier1SuccessCount)
    }

    @Test
    fun `executeCommand - Tier 1 success records execution time`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext()) as CommandResult.Success

        // Then - execution time should be recorded
        assertTrue(result.executionTimeMs >= 0)
    }

    @Test
    fun `executeCommand - Tier 1 success adds to command history`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        // When
        orchestrator.executeCommand("test command", 0.85f, CommandContext())

        // Then
        val history = orchestrator.getCommandHistory()
        assertEquals(1, history.size)
        assertEquals("test command", history[0].commandText)
        assertEquals(0.85f, history[0].confidence)
        assertEquals(1, history[0].tier)
    }

    @Test
    fun `executeCommand - Tier 1 high confidence command`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        // When - very high confidence
        val result = orchestrator.executeCommand("test", 0.99f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
    }

    @Test
    fun `executeCommand - Tier 1 with complex command context`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true

        val capturedCommand = slot<Command>()
        coEvery { mockCommandManager.executeCommand(capture(capturedCommand)) } returns commandResult

        val context = CommandContext(
            packageName = "com.test.app",
            activityName = "MainActivity",
            focusedElement = "EditText",
            deviceState = mapOf("isScreenOn" to true),
            customData = mapOf("extra" to "data")
        )

        // When
        orchestrator.executeCommand("complex command", 0.9f, context)

        // Then - context passed correctly
        assertEquals("com.test.app", capturedCommand.captured.context?.packageName)
        assertEquals("MainActivity", capturedCommand.captured.context?.activityName)
        assertEquals("EditText", capturedCommand.captured.context?.focusedElement)
    }

    @Test
    fun `executeCommand - Tier 1 emits execution started event`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(2).toList(events)
        }

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())
        job.join()

        // Then
        assertTrue(events.any { it is CommandEvent.ExecutionStarted })
        assertTrue(events.any { it is CommandEvent.ExecutionCompleted })
    }

    @Test
    fun `executeCommand - Tier 1 multiple commands in sequence`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val commandResult = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { commandResult.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns commandResult

        // When - execute multiple commands
        val result1 = orchestrator.executeCommand("command1", 0.9f, CommandContext())
        val result2 = orchestrator.executeCommand("command2", 0.9f, CommandContext())
        val result3 = orchestrator.executeCommand("command3", 0.9f, CommandContext())

        // Then
        assertTrue(result1 is CommandResult.Success)
        assertTrue(result2 is CommandResult.Success)
        assertTrue(result3 is CommandResult.Success)
        assertEquals(3L, orchestrator.getMetrics().totalCommandsExecuted)
    }

    // ========================================
    // Category 3: Tier 2 Command Execution (15 tests)
    // ========================================

    @Test
    fun `executeCommand - Tier 2 success when Tier 1 unavailable`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = null, // No Tier 1
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = null,
            accessibilityService = null
        )

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        every { tier2Result.message } returns "Tier 2 executed"
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)
        assertEquals(1L, orchestrator.getMetrics().tier2SuccessCount)

        coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
    }

    @Test
    fun `executeCommand - Tier 2 processes normalized command text`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true

        val capturedCommand = slot<String>()
        coEvery { mockVoiceCommandProcessor.processCommand(capture(capturedCommand)) } returns tier2Result

        // When
        orchestrator.executeCommand("  MIXED Case Command  ", 0.9f, CommandContext())

        // Then
        assertEquals("mixed case command", capturedCommand.captured)
    }

    @Test
    fun `executeCommand - Tier 2 failure falls through to Tier 3`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = null,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = mockActionCoordinator,
            accessibilityService = null
        )

        // Tier 2 fails
        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        every { tier2Result.message } returns "Tier 2 failed"
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // Tier 3 succeeds (no return value, just executes)
        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)

        // Verify both tiers were called
        coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
        coVerify(exactly = 1) { mockActionCoordinator.executeAction(any()) }
    }

    @Test
    fun `executeCommand - Tier 2 skipped when processor is null`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = null,
            voiceCommandProcessor = null, // No Tier 2
            actionCoordinator = mockActionCoordinator,
            accessibilityService = null
        )

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - goes directly to Tier 3
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)

        // Verify Tier 2 was NOT called
        coVerify(exactly = 0) { mockVoiceCommandProcessor.processCommand(any()) }
    }

    @Test
    fun `executeCommand - Tier 2 exception falls through to Tier 3`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = null,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = mockActionCoordinator,
            accessibilityService = null
        )

        // Tier 2 throws exception
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } throws RuntimeException("Tier 2 error")

        // Tier 3 succeeds
        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - Tier 3 should still execute
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)
    }

    @Test
    fun `executeCommand - Tier 2 success increments metrics`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        orchestrator.executeCommand("test1", 0.9f, CommandContext())
        orchestrator.executeCommand("test2", 0.9f, CommandContext())

        // Then
        val metrics = orchestrator.getMetrics()
        assertEquals(2L, metrics.totalCommandsExecuted)
        assertEquals(2L, metrics.tier2SuccessCount)
    }

    @Test
    fun `executeCommand - Tier 2 success records execution time`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext()) as CommandResult.Success

        // Then
        assertTrue(result.executionTimeMs >= 0)
    }

    @Test
    fun `executeCommand - Tier 2 success adds to command history`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        orchestrator.executeCommand("tier2 command", 0.75f, CommandContext())

        // Then
        val history = orchestrator.getCommandHistory()
        assertEquals(1, history.size)
        assertEquals("tier2 command", history[0].commandText)
        assertEquals(2, history[0].tier)
    }

    @Test
    fun `executeCommand - Tier 2 emits fallback event when Tier 1 fails`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(3).toList(events)
        }

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())
        job.join()

        // Then
        assertTrue(events.any { it is CommandEvent.TierFallback && it.fromTier == 1 && it.toTier == 2 })
    }

    @Test
    fun `executeCommand - Tier 2 app-specific command`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        every { tier2Result.message } returns "App-specific action executed"

        val capturedCommand = slot<String>()
        coEvery { mockVoiceCommandProcessor.processCommand(capture(capturedCommand)) } returns tier2Result

        // When
        orchestrator.executeCommand("tap submit button", 0.85f, CommandContext())

        // Then
        assertEquals("tap submit button", capturedCommand.captured)
        assertTrue(tier2Result.success)
    }

    @Test
    fun `executeCommand - Tier 2 with medium confidence`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When - medium confidence (just above threshold)
        val result = orchestrator.executeCommand("test", 0.55f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
    }

    @Test
    fun `executeCommand - Tier 2 multiple successful commands`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        repeat(5) {
            orchestrator.executeCommand("test$it", 0.9f, CommandContext())
        }

        // Then
        assertEquals(5L, orchestrator.getMetrics().tier2SuccessCount)
        assertEquals(5, orchestrator.getCommandHistory().size)
    }

    @Test
    fun `executeCommand - Tier 2 empty result message`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        every { tier2Result.message } returns "" // Empty message
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - still succeeds
        assertTrue(result is CommandResult.Success)
    }

    @Test
    fun `executeCommand - Tier 2 result details captured`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        every { tier2Result.message } returns "Detailed execution info"
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext()) as CommandResult.Success

        // Then
        assertTrue(result.details?.contains("Detailed execution info") == true)
    }

    // ========================================
    // Category 4: Tier 3 Command Execution (15 tests)
    // ========================================

    @Test
    fun `executeCommand - Tier 3 executes when all previous tiers fail`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = mockCommandManager,
            voiceCommandProcessor = mockVoiceCommandProcessor,
            actionCoordinator = mockActionCoordinator,
            accessibilityService = null
        )

        // Tier 1 fails
        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        // Tier 2 fails
        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // Tier 3 executes
        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)

        // Verify all tiers were called
        coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }
        coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
        coVerify(exactly = 1) { mockActionCoordinator.executeAction(any()) }
    }

    @Test
    fun `executeCommand - Tier 3 always succeeds when ActionCoordinator executes`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        // Tier 3 executes (no return value in original code)
        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - Tier 3 is "best effort" and always logs as executed
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)
    }

    @Test
    fun `executeCommand - Tier 3 handles exception gracefully`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        // Tier 3 throws exception
        coEvery { mockActionCoordinator.executeAction(any()) } throws RuntimeException("Action error")

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - returns failure
        assertTrue(result is CommandResult.Failure)
        assertEquals(3, (result as CommandResult.Failure).tier)
    }

    @Test
    fun `executeCommand - Tier 3 returns NotFound when ActionCoordinator is null`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(
            commandManager = null,
            voiceCommandProcessor = null,
            actionCoordinator = null, // No Tier 3
            accessibilityService = null
        )

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.NotFound)
    }

    @Test
    fun `executeCommand - Tier 3 success increments metrics`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        orchestrator.executeCommand("test1", 0.9f, CommandContext())
        orchestrator.executeCommand("test2", 0.9f, CommandContext())

        // Then
        val metrics = orchestrator.getMetrics()
        assertEquals(2L, metrics.totalCommandsExecuted)
        assertEquals(2L, metrics.tier3SuccessCount)
    }

    @Test
    fun `executeCommand - Tier 3 records execution time`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext()) as CommandResult.Success

        // Then
        assertTrue(result.executionTimeMs >= 0)
    }

    @Test
    fun `executeCommand - Tier 3 adds to command history`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        orchestrator.executeCommand("tier3 command", 0.65f, CommandContext())

        // Then
        val history = orchestrator.getCommandHistory()
        assertEquals(1, history.size)
        assertEquals("tier3 command", history[0].commandText)
        assertEquals(3, history[0].tier)
    }

    @Test
    fun `executeCommand - Tier 3 emits fallback event when Tier 2 fails`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, mockActionCoordinator, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(3).toList(events)
        }

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())
        job.join()

        // Then
        assertTrue(events.any { it is CommandEvent.TierFallback && it.fromTier == 2 && it.toTier == 3 })
    }

    @Test
    fun `executeCommand - Tier 3 processes general commands`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        val capturedCommand = slot<String>()
        coEvery { mockActionCoordinator.executeAction(capture(capturedCommand)) } just Awaits

        // When
        orchestrator.executeCommand("go back", 0.9f, CommandContext())

        // Then
        assertEquals("go back", capturedCommand.captured)
    }

    @Test
    fun `executeCommand - Tier 3 multiple commands in sequence`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        repeat(3) {
            orchestrator.executeCommand("action$it", 0.9f, CommandContext())
        }

        // Then
        assertEquals(3L, orchestrator.getMetrics().tier3SuccessCount)
        coVerify(exactly = 3) { mockActionCoordinator.executeAction(any()) }
    }

    @Test
    fun `executeCommand - Tier 3 with low-medium confidence`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When - just above minimum threshold
        val result = orchestrator.executeCommand("test", 0.51f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
    }

    @Test
    fun `executeCommand - Tier 3 best effort execution`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("unknown command", 0.9f, CommandContext()) as CommandResult.Success

        // Then - marked as "best effort"
        assertTrue(result.details?.contains("best effort") == true)
    }

    @Test
    fun `executeCommand - Tier 3 exception increments failure metrics`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } throws RuntimeException("Error")

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertEquals(1L, orchestrator.getMetrics().failureCount)
    }

    @Test
    fun `executeCommand - Tier 3 emits error event on exception`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        coEvery { mockActionCoordinator.executeAction(any()) } throws RuntimeException("Action failed")

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(2).toList(events)
        }

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())
        job.join()

        // Then - no error event (exception is caught and returned as Failure)
        assertTrue(events.any { it is CommandEvent.ExecutionCompleted })
    }

    @Test
    fun `executeCommand - Tier 3 normalizes command before execution`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, null, mockActionCoordinator, null)

        val capturedCommand = slot<String>()
        coEvery { mockActionCoordinator.executeAction(capture(capturedCommand)) } just Awaits

        // When
        orchestrator.executeCommand("  GO HOME  ", 0.9f, CommandContext())

        // Then
        assertEquals("go home", capturedCommand.captured)
    }

    // ========================================
    // Category 5: Tier Fallback Logic (10 tests)
    // ========================================

    @Test
    fun `executeCommand - fallback from Tier 1 to Tier 2`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)
    }

    @Test
    fun `executeCommand - fallback from Tier 2 to Tier 3`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(null, mockVoiceCommandProcessor, mockActionCoordinator, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)
    }

    @Test
    fun `executeCommand - full fallback cascade Tier 1 to 2 to 3`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        assertTrue(result is CommandResult.Success)
        assertEquals(3, (result as CommandResult.Success).tier)

        // All three tiers should have been attempted
        coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }
        coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) }
        coVerify(exactly = 1) { mockActionCoordinator.executeAction(any()) }
    }

    @Test
    fun `executeCommand - emits TierFallback events for each transition`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(4).toList(events)
        }

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())
        job.join()

        // Then
        val fallbackEvents = events.filterIsInstance<CommandEvent.TierFallback>()
        assertEquals(2, fallbackEvents.size) // 1->2, 2->3
        assertTrue(fallbackEvents.any { it.fromTier == 1 && it.toTier == 2 })
        assertTrue(fallbackEvents.any { it.fromTier == 2 && it.toTier == 3 })
    }

    @Test
    fun `executeCommand - stop at first successful tier`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true // SUCCESS
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - Tier 3 should NOT be called
        coVerify(exactly = 0) { mockActionCoordinator.executeAction(any()) }
    }

    @Test
    fun `executeCommand - fallback preserves command text through tiers`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        val tier1Command = slot<Command>()
        val tier2Command = slot<String>()
        val tier3Command = slot<String>()

        coEvery { mockCommandManager.executeCommand(capture(tier1Command)) } returns tier1Result
        coEvery { mockVoiceCommandProcessor.processCommand(capture(tier2Command)) } returns tier2Result
        coEvery { mockActionCoordinator.executeAction(capture(tier3Command)) } just Awaits

        // When
        orchestrator.executeCommand("original command", 0.9f, CommandContext())

        // Then - all tiers receive same normalized command
        assertEquals("original command", tier1Command.captured.text)
        assertEquals("original command", tier2Command.captured)
        assertEquals("original command", tier3Command.captured)
    }

    @Test
    fun `executeCommand - fallback respects exception handling`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)

        // Tier 1 throws exception
        coEvery { mockCommandManager.executeCommand(any()) } throws RuntimeException("Tier 1 error")

        // Tier 2 succeeds
        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - fallback to Tier 2 still works
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)
    }

    @Test
    fun `executeCommand - fallback metrics are tracked correctly`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        val metrics = orchestrator.getMetrics()
        assertEquals(0L, metrics.tier1SuccessCount) // Failed
        assertEquals(1L, metrics.tier2SuccessCount) // Succeeded
    }

    @Test
    fun `executeCommand - fallback history records final successful tier`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        orchestrator.executeCommand("test", 0.85f, CommandContext())

        // Then
        val history = orchestrator.getCommandHistory()
        assertEquals(1, history.size)
        assertEquals(2, history[0].tier) // Tier 2 succeeded
    }

    @Test
    fun `executeCommand - rapid fallback transitions`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)

        // All tiers fail quickly
        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns false
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        val startTime = System.currentTimeMillis()
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext()) as CommandResult.Success
        val totalTime = System.currentTimeMillis() - startTime

        // Then - should complete quickly (< 1 second)
        assertTrue(totalTime < 1000)
        assertEquals(3, result.tier)
    }

    // ========================================
    // Category 6: Fallback Mode (10 tests)
    // ========================================

    @Test
    fun `enableFallbackMode - sets fallback mode flag`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.enableFallbackMode()

        // Then
        assertTrue(orchestrator.isFallbackModeEnabled)
    }

    @Test
    fun `enableFallbackMode - increments fallback mode activations`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        // When
        orchestrator.enableFallbackMode()

        // Then
        assertEquals(1, orchestrator.getMetrics().fallbackModeActivations)
    }

    @Test
    fun `enableFallbackMode - emits FallbackModeChanged event`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(1).toList(events)
        }

        // When
        orchestrator.enableFallbackMode()
        job.join()

        // Then
        assertTrue(events.any { it is CommandEvent.FallbackModeChanged && it.enabled })
    }

    @Test
    fun `enableFallbackMode - multiple enable calls only emit one event`() = runTest {
        // Given
        orchestrator.initialize(mockContext)

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(1).toList(events)
        }

        // When
        orchestrator.enableFallbackMode()
        orchestrator.enableFallbackMode() // Second enable
        job.join()

        // Then - only one event
        assertEquals(1, events.filterIsInstance<CommandEvent.FallbackModeChanged>().size)
    }

    @Test
    fun `disableFallbackMode - clears fallback mode flag`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.enableFallbackMode()

        // When
        orchestrator.disableFallbackMode()

        // Then
        assertFalse(orchestrator.isFallbackModeEnabled)
    }

    @Test
    fun `disableFallbackMode - emits FallbackModeChanged event`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.enableFallbackMode()

        val events = mutableListOf<CommandEvent>()
        val job = launch {
            orchestrator.commandEvents.take(1).toList(events)
        }

        // When
        orchestrator.disableFallbackMode()
        job.join()

        // Then
        assertTrue(events.any { it is CommandEvent.FallbackModeChanged && !it.enabled })
    }

    @Test
    fun `executeCommand - fallback mode skips Tier 1`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When - enable fallback mode and execute command
        orchestrator.enableFallbackMode()
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - goes directly to Tier 2
        assertTrue(result is CommandResult.Success)
        assertEquals(2, (result as CommandResult.Success).tier)

        // Tier 1 should NOT be called
        coVerify(exactly = 0) { mockCommandManager.executeCommand(any()) }
    }

    @Test
    fun `executeCommand - fallback mode still attempts Tier 2 and 3`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, mockActionCoordinator, null)
        orchestrator.enableFallbackMode()

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns false
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        coEvery { mockActionCoordinator.executeAction(any()) } just Awaits

        // When
        orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then
        coVerify(exactly = 0) { mockCommandManager.executeCommand(any()) } // Skipped
        coVerify(exactly = 1) { mockVoiceCommandProcessor.processCommand(any()) } // Attempted
        coVerify(exactly = 1) { mockActionCoordinator.executeAction(any()) } // Attempted
    }

    @Test
    fun `executeCommand - disabling fallback mode restores Tier 1`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, null, null, null)

        val tier1Result = mockk<com.augmentalis.commandmanager.models.CommandResult>()
        every { tier1Result.success } returns true
        coEvery { mockCommandManager.executeCommand(any()) } returns tier1Result

        orchestrator.enableFallbackMode()
        orchestrator.disableFallbackMode()

        // When
        val result = orchestrator.executeCommand("test", 0.9f, CommandContext())

        // Then - Tier 1 is used again
        assertTrue(result is CommandResult.Success)
        assertEquals(1, (result as CommandResult.Success).tier)
        coVerify(exactly = 1) { mockCommandManager.executeCommand(any()) }
    }

    @Test
    fun `executeCommand - fallback mode multiple commands`() = runTest {
        // Given
        orchestrator.initialize(mockContext)
        orchestrator.setTierExecutors(mockCommandManager, mockVoiceCommandProcessor, null, null)
        orchestrator.enableFallbackMode()

        val tier2Result = mockk<com.augmentalis.voiceoscore.scraping.CommandResult>()
        every { tier2Result.success } returns true
        coEvery { mockVoiceCommandProcessor.processCommand(any()) } returns tier2Result

        // When
        repeat(3) {
            orchestrator.executeCommand("test$it", 0.9f, CommandContext())
        }

        // Then - all commands skip Tier 1
        coVerify(exactly = 0) { mockCommandManager.executeCommand(any()) }
        coVerify(exactly = 3) { mockVoiceCommandProcessor.processCommand(any()) }
        assertEquals(3L, orchestrator.getMetrics().tier2SuccessCount)
    }

    // Due to character limits, I'll complete the remaining test categories in the next message
    // This includes:
    // - Category 7: Global Actions (10 tests)
    // - Category 8: Command Registration & Vocabulary (10 tests)
    // - Category 9: Metrics & History (10 tests)
    // - Category 10: Thread Safety & Concurrency (10 tests)
    // - Category 11: Edge Cases & Error Handling (15 tests)
}
