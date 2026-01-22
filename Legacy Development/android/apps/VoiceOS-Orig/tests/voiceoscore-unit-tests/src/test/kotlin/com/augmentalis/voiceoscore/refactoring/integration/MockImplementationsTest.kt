/**
 * MockImplementationsTest.kt - Comprehensive mock behavior tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15 03:40:00 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3 Afternoon
 *
 * Tests all 7 mock implementations:
 * - Basic functionality
 * - Call tracking
 * - Configurable behavior
 * - Error simulation
 * - Thread safety
 */
package com.augmentalis.voiceoscore.refactoring.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.refactoring.interfaces.*
import com.augmentalis.voiceoscore.refactoring.mocks.*
import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestAssertions
import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestFixtures
import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mock Implementations Integration Test
 *
 * Validates all mock implementations provide:
 * 1. Complete interface contract fulfillment
 * 2. Configurable behavior (success/failure/delays)
 * 3. Call tracking and verification
 * 4. Thread safety
 * 5. Reset functionality
 */
class MockImplementationsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ========================================
    // MockCommandOrchestrator Tests
    // ========================================

    @Test
    fun testMockCommandOrchestrator_BasicFunctionality() = runBlocking {
        val mock = MockCommandOrchestrator(context)

        // Test initialization
        mock.initialize(context)
        RefactoringTestAssertions.assertCommandOrchestratorReady(mock)

        // Test command execution
        val commandContext = RefactoringTestFixtures.CommandOrchestrator
        val result = mock.executeCommand("test command", 0.9f,
            RefactoringTestUtils.createMockCommandContext())

        RefactoringTestAssertions.assertCommandExecutedSuccessfully(result)
        RefactoringTestAssertions.assertMockMethodCalled(mock, "executeCommand")

        // Test metrics
        val metrics = mock.getMetrics()
        assertTrue(metrics.totalCommandsExecuted > 0, "Commands executed")
    }

    @Test
    fun testMockCommandOrchestrator_ConfigurableFailure() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)

        // Configure mock to fail
        mock.defaultCommandResult = ICommandOrchestrator.CommandResult.Failure(
            tier = 1,
            reason = "Configured test failure"
        )

        val result = mock.executeCommand("test", 0.9f,
            RefactoringTestUtils.createMockCommandContext())

        RefactoringTestAssertions.assertCommandFailed(result, "test failure")
    }

    @Test
    fun testMockCommandOrchestrator_FallbackMode() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)

        assertFalse(mock.isFallbackModeEnabled)

        mock.enableFallbackMode()
        RefactoringTestAssertions.assertFallbackModeEnabled(mock)

        mock.disableFallbackMode()
        assertFalse(mock.isFallbackModeEnabled)
    }

    @Test
    fun testMockCommandOrchestrator_Reset() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)
        mock.executeCommand("test", 0.9f, RefactoringTestUtils.createMockCommandContext())

        assertTrue(mock.isReady)

        mock.reset()
        RefactoringTestAssertions.assertMockReset(mock)
    }

    // ========================================
    // MockEventRouter Tests
    // ========================================

    @Test
    fun testMockEventRouter_BasicFunctionality() = runBlocking {
        val mock = MockEventRouter(context)

        mock.initialize(context, RefactoringTestFixtures.EventRouter.createEventConfig())
        RefactoringTestAssertions.assertEventRouterReady(mock)

        // Test event routing
        val event = RefactoringTestUtils.createMockAccessibilityEvent()
        mock.routeEvent(event)

        val metrics = mock.getMetrics()
        assertTrue(metrics.totalEventsReceived > 0, "Events received")
    }

    @Test
    fun testMockEventRouter_PackageFiltering() = runBlocking {
        val mock = MockEventRouter(context)
        mock.initialize(context, IEventRouter.EventRouterConfig())

        mock.addPackageFilter("com.test.filtered")
        assertTrue(mock.getPackageFilters().contains("com.test.filtered"))

        mock.removePackageFilter("com.test.filtered")
        assertFalse(mock.getPackageFilters().contains("com.test.filtered"))
    }

    // ========================================
    // MockSpeechManager Tests
    // ========================================

    @Test
    fun testMockSpeechManager_BasicFunctionality() = runBlocking {
        val mock = MockSpeechManager(context)

        mock.initialize(context, ISpeechManager.SpeechConfig())
        RefactoringTestAssertions.assertSpeechManagerReady(mock)

        // Test listening
        val started = mock.startListening()
        assertTrue(started)
        RefactoringTestAssertions.assertListening(mock)

        mock.stopListening()
        RefactoringTestAssertions.assertNotListening(mock)
    }

    @Test
    fun testMockSpeechManager_EngineStatus() {
        val mock = MockSpeechManager(context)

        val status = mock.getEngineStatus(ISpeechManager.SpeechEngine.VIVOKA)
        RefactoringTestAssertions.assertEngineHealthy(status)
    }

    @Test
    fun testMockSpeechManager_VocabularyManagement() = runBlocking {
        val mock = MockSpeechManager(context)
        mock.initialize(context, ISpeechManager.SpeechConfig())

        assertEquals(0, mock.getVocabularySize())

        mock.updateVocabulary(setOf("command1", "command2", "command3"))
        assertEquals(3, mock.getVocabularySize())

        mock.addVocabulary(setOf("command4"))
        assertEquals(4, mock.getVocabularySize())

        mock.clearVocabulary()
        assertEquals(0, mock.getVocabularySize())
    }

    // ========================================
    // MockUIScrapingService Tests
    // ========================================

    @Test
    fun testMockUIScrapingService_BasicFunctionality() = runBlocking {
        val mock = MockUIScrapingService(context)

        mock.initialize(context, RefactoringTestFixtures.UIScrapingService.createScrapingConfig())
        RefactoringTestAssertions.assertUIScrapingReady(mock)

        // Configure mock to return elements
        mock.mockElements = RefactoringTestFixtures.UIScrapingService.sampleElements

        val elements = mock.extractCurrentScreen()
        RefactoringTestAssertions.assertElementsExtracted(elements, 1)
    }

    @Test
    fun testMockUIScrapingService_Caching() = runBlocking {
        val mock = MockUIScrapingService(context)
        mock.initialize(context, IUIScrapingService.ScrapingConfig())

        mock.mockElements = RefactoringTestFixtures.UIScrapingService.sampleElements

        val elements = mock.extractCurrentScreen()
        mock.updateCache(elements)

        RefactoringTestAssertions.assertElementCached(mock, "com.test.app")

        mock.clearCache()
        assertFalse(mock.isCached("com.test.app"))
    }

    // ========================================
    // MockServiceMonitor Tests
    // ========================================

    @Test
    fun testMockServiceMonitor_BasicFunctionality() = runBlocking {
        val mock = MockServiceMonitor(context)

        mock.initialize(context, RefactoringTestFixtures.ServiceMonitor.createMonitorConfig())

        mock.startMonitoring()
        RefactoringTestAssertions.assertMonitoringActive(mock)

        mock.stopMonitoring()
        assertFalse(mock.isMonitoring)
    }

    @Test
    fun testMockServiceMonitor_HealthChecks() = runBlocking {
        val mock = MockServiceMonitor(context)
        mock.initialize(context, IServiceMonitor.MonitorConfig())
        mock.mockHealthStatus = IServiceMonitor.HealthStatus.HEALTHY

        val status = mock.performHealthCheck()
        RefactoringTestAssertions.assertHealthy(status)
    }

    @Test
    fun testMockServiceMonitor_PerformanceMetrics() {
        val mock = MockServiceMonitor(context)

        val snapshot = mock.getCurrentMetrics()
        RefactoringTestAssertions.assertPerformanceAcceptable(snapshot)
    }

    // ========================================
    // MockDatabaseManager Tests
    // ========================================

    @Test
    fun testMockDatabaseManager_BasicFunctionality() = runBlocking {
        val mock = MockDatabaseManager(context)

        mock.initialize(context, RefactoringTestFixtures.DatabaseManager.createDatabaseConfig())
        RefactoringTestAssertions.assertDatabaseReady(mock)
    }

    @Test
    fun testMockDatabaseManager_VoiceCommands() = runBlocking {
        val mock = MockDatabaseManager(context)
        mock.initialize(context, IDatabaseManager.DatabaseConfig())

        mock.mockVoiceCommands = RefactoringTestFixtures.DatabaseManager.sampleVoiceCommands

        val commands = mock.getAllVoiceCommands()
        RefactoringTestAssertions.assertCommandsLoaded(commands)
    }

    @Test
    fun testMockDatabaseManager_Caching() = runBlocking {
        val mock = MockDatabaseManager(context)
        mock.initialize(context, IDatabaseManager.DatabaseConfig())

        RefactoringTestAssertions.assertCacheEnabled(mock)

        mock.disableCache()
        assertFalse(mock.isCacheEnabled())

        mock.enableCache()
        assertTrue(mock.isCacheEnabled())
    }

    // ========================================
    // MockStateManager Tests
    // ========================================

    @Test
    fun testMockStateManager_BasicFunctionality() = runBlocking {
        val mock = MockStateManager(context)

        mock.initialize(context, RefactoringTestFixtures.StateManager.createStateConfig())
        RefactoringTestAssertions.assertStateManagerReady(mock)
    }

    @Test
    fun testMockStateManager_StateTransitions() = runBlocking {
        val mock = MockStateManager(context)
        mock.initialize(context, IStateManager.StateConfig())

        assertFalse(mock.isServiceReady.value)

        mock.setServiceReady(true)
        assertTrue(mock.isServiceReady.value)

        mock.setVoiceInitialized(true)
        assertTrue(mock.isVoiceInitialized.value)

        mock.setCommandProcessing(true)
        assertTrue(mock.isCommandProcessing.value)
    }

    @Test
    fun testMockStateManager_Snapshots() = runBlocking {
        val mock = MockStateManager(context)
        mock.initialize(context, IStateManager.StateConfig())

        mock.setServiceReady(true)
        mock.setVoiceInitialized(true)

        val snapshot = mock.getStateSnapshot()
        RefactoringTestAssertions.assertStateSnapshot(
            snapshot,
            expectedServiceReady = true,
            expectedVoiceInitialized = true
        )
    }

    @Test
    fun testMockStateManager_Checkpoints() = runBlocking {
        val mock = MockStateManager(context)
        mock.initialize(context, IStateManager.StateConfig())

        mock.setServiceReady(true)
        mock.createCheckpoint("test_checkpoint")

        mock.setServiceReady(false)
        assertFalse(mock.isServiceReady.value)

        val restored = mock.restoreCheckpoint("test_checkpoint")
        assertTrue(restored)
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    fun testMocks_ThreadSafety() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)

        // Execute commands concurrently
        RefactoringTestUtils.runConcurrently(10) { index ->
            mock.executeCommand(
                "command_$index",
                0.9f,
                RefactoringTestUtils.createMockCommandContext()
            )
        }

        val metrics = mock.getMetrics()
        assertEquals(10L, metrics.totalCommandsExecuted)
    }
}
