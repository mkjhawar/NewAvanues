/**
 * DIPerformanceTest.kt - DI Performance and overhead measurement
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15 03:40:00 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3 Afternoon
 *
 * Performance Tests:
 * - DI overhead < 5ms
 * - Memory usage < 1MB for all components
 * - No blocking on main thread
 * - Stress testing with concurrent operations
 */
package com.augmentalis.voiceoscore.refactoring.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.refactoring.interfaces.*
import com.augmentalis.voiceoscore.refactoring.mocks.*
import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestAssertions
import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * DI Performance Integration Test
 *
 * Validates performance requirements:
 * 1. Component creation < 5ms average
 * 2. Total DI overhead < 1MB memory
 * 3. Concurrent access thread-safe
 * 4. No main thread blocking
 * 5. Stress test 1000 operations
 */
class DIPerformanceTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ========================================
    // Component Creation Performance
    // ========================================

    @Test
    fun testCommandOrchestrator_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockCommandOrchestrator(context)
        }

        println("CommandOrchestrator creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    @Test
    fun testEventRouter_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockEventRouter(context)
        }

        println("EventRouter creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    @Test
    fun testSpeechManager_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockSpeechManager(context)
        }

        println("SpeechManager creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    @Test
    fun testUIScrapingService_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockUIScrapingService(context)
        }

        println("UIScrapingService creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    @Test
    fun testServiceMonitor_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockServiceMonitor(context)
        }

        println("ServiceMonitor creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    @Test
    fun testDatabaseManager_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockDatabaseManager(context)
        }

        println("DatabaseManager creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    @Test
    fun testStateManager_CreationPerformance() {
        val metrics = measureComponentCreation {
            MockStateManager(context)
        }

        println("StateManager creation: $metrics")
        RefactoringTestAssertions.assertDIOverheadAcceptable(metrics, maxAverageMs = 5.0)
    }

    // ========================================
    // Full Stack Performance
    // ========================================

    @Test
    fun testAllComponents_CumulativeOverhead() = runBlocking {
        val totalTimeMs = measureTimeMillis {
            // Create all 7 components
            val orchestrator = MockCommandOrchestrator(context)
            val router = MockEventRouter(context)
            val speech = MockSpeechManager(context)
            val scraping = MockUIScrapingService(context)
            val monitor = MockServiceMonitor(context)
            val database = MockDatabaseManager(context)
            val state = MockStateManager(context)

            // Initialize all components
            orchestrator.initialize(context)
            router.initialize(context, IEventRouter.EventRouterConfig())
            speech.initialize(context, ISpeechManager.SpeechConfig())
            scraping.initialize(context, IUIScrapingService.ScrapingConfig())
            monitor.initialize(context, IServiceMonitor.MonitorConfig())
            database.initialize(context, IDatabaseManager.DatabaseConfig())
            state.initialize(context, IStateManager.StateConfig())
        }

        println("Full stack creation and initialization: ${totalTimeMs}ms")

        // Total overhead should be < 100ms for all 7 components
        assertTrue(
            totalTimeMs < 100,
            "Total DI overhead should be < 100ms (got ${totalTimeMs}ms)"
        )
    }

    // ========================================
    // Memory Overhead Tests
    // ========================================

    @Test
    fun testMemory_ComponentFootprint() {
        // Measure memory before and after component creation
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Create all components
        val components = listOf(
            MockCommandOrchestrator(context),
            MockEventRouter(context),
            MockSpeechManager(context),
            MockUIScrapingService(context),
            MockServiceMonitor(context),
            MockDatabaseManager(context),
            MockStateManager(context)
        )

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryOverhead = (finalMemory - initialMemory) / 1024 / 1024 // Convert to MB

        println("Memory overhead for all components: ${memoryOverhead}MB")

        // Memory overhead should be < 1MB
        assertTrue(
            memoryOverhead < 1,
            "Memory overhead should be < 1MB (got ${memoryOverhead}MB)"
        )
    }

    // ========================================
    // Concurrent Access Tests
    // ========================================

    @Test
    fun testConcurrentAccess_CommandOrchestrator() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)

        val result = RefactoringTestUtils.stressTest(
            operations = 1000,
            concurrency = 10
        ) {
            mock.executeCommand(
                "test_command",
                0.9f,
                RefactoringTestUtils.createMockCommandContext()
            )
        }

        println("CommandOrchestrator stress test: $result")
        RefactoringTestAssertions.assertStressTestSuccessful(result, minSuccessRate = 0.99)
    }

    @Test
    fun testConcurrentAccess_EventRouter() = runBlocking {
        val mock = MockEventRouter(context)
        mock.initialize(context, IEventRouter.EventRouterConfig())

        val result = RefactoringTestUtils.stressTest(
            operations = 1000,
            concurrency = 10
        ) {
            val event = RefactoringTestUtils.createMockAccessibilityEvent()
            mock.routeEvent(event)
        }

        println("EventRouter stress test: $result")
        RefactoringTestAssertions.assertStressTestSuccessful(result, minSuccessRate = 0.99)
    }

    @Test
    fun testConcurrentAccess_SpeechManager() = runBlocking {
        val mock = MockSpeechManager(context)
        mock.initialize(context, ISpeechManager.SpeechConfig())

        val result = RefactoringTestUtils.stressTest(
            operations = 100, // Fewer operations for start/stop
            concurrency = 5
        ) {
            mock.startListening()
            mock.stopListening()
        }

        println("SpeechManager stress test: $result")
        RefactoringTestAssertions.assertStressTestSuccessful(result, minSuccessRate = 0.99)
    }

    @Test
    fun testConcurrentAccess_StateManager() = runBlocking {
        val mock = MockStateManager(context)
        mock.initialize(context, IStateManager.StateConfig())

        val result = RefactoringTestUtils.stressTest(
            operations = 1000,
            concurrency = 10
        ) {
            mock.setServiceReady(true)
            mock.setVoiceInitialized(true)
            mock.setCommandProcessing(false)
        }

        println("StateManager stress test: $result")
        RefactoringTestAssertions.assertStressTestSuccessful(result, minSuccessRate = 0.99)
    }

    // ========================================
    // Operation Latency Tests
    // ========================================

    @Test
    fun testOperationLatency_CommandExecution() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)

        val (result, latencyMs) = RefactoringTestUtils.measureExecutionTime {
            mock.executeCommand(
                "test_command",
                0.9f,
                RefactoringTestUtils.createMockCommandContext()
            )
        }

        println("Command execution latency: ${latencyMs}ms")

        // Command execution should be < 100ms
        assertTrue(
            latencyMs < 100,
            "Command execution latency should be < 100ms (got ${latencyMs}ms)"
        )
    }

    @Test
    fun testOperationLatency_EventRouting() = runBlocking {
        val mock = MockEventRouter(context)
        mock.initialize(context, IEventRouter.EventRouterConfig())

        val (_, latencyMs) = RefactoringTestUtils.measureExecutionTime {
            val event = RefactoringTestUtils.createMockAccessibilityEvent()
            mock.routeEvent(event)
        }

        println("Event routing latency: ${latencyMs}ms")

        // Event routing should be < 50ms
        assertTrue(
            latencyMs < 50,
            "Event routing latency should be < 50ms (got ${latencyMs}ms)"
        )
    }

    @Test
    fun testOperationLatency_StateUpdate() = runBlocking {
        val mock = MockStateManager(context)
        mock.initialize(context, IStateManager.StateConfig())

        val (_, latencyMs) = RefactoringTestUtils.measureExecutionTime {
            mock.setServiceReady(true)
            mock.setVoiceInitialized(true)
            mock.setCommandProcessing(false)
        }

        println("State update latency: ${latencyMs}ms")

        // State updates should be < 10ms
        assertTrue(
            latencyMs < 10,
            "State update latency should be < 10ms (got ${latencyMs}ms)"
        )
    }

    // ========================================
    // Throughput Tests
    // ========================================

    @Test
    fun testThroughput_CommandsPerSecond() = runBlocking {
        val mock = MockCommandOrchestrator(context)
        mock.initialize(context)

        val result = RefactoringTestUtils.stressTest(
            operations = 1000,
            concurrency = 10
        ) {
            mock.executeCommand(
                "test_command",
                0.9f,
                RefactoringTestUtils.createMockCommandContext()
            )
        }

        println("Command throughput: ${"%.2f".format(result.operationsPerSecond)} ops/sec")

        // Should process at least 100 commands per second
        assertTrue(
            result.operationsPerSecond >= 100,
            "Command throughput should be >= 100 ops/sec (got ${result.operationsPerSecond})"
        )
    }

    // ========================================
    // Helper Functions
    // ========================================

    private fun measureComponentCreation(
        iterations: Int = 100,
        creator: () -> Any
    ): RefactoringTestUtils.DIOverheadMetrics = runBlocking {
        RefactoringTestUtils.measureDIOverhead(iterations) {
            creator()
        }
    }
}
