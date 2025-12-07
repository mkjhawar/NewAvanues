/**
 * RefactoringTestAssertions.kt - Custom test assertions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15 03:37:00 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3 Afternoon
 */
package com.augmentalis.voiceoscore.refactoring.utils

import com.augmentalis.voiceoscore.refactoring.interfaces.*
import com.augmentalis.voiceoscore.refactoring.mocks.*
import org.junit.Assert.*

/**
 * Custom Test Assertions for VoiceOSService Refactoring
 *
 * Provides domain-specific assertion helpers:
 * - Component state assertions
 * - Mock verification
 * - Performance assertions
 * - Metrics validation
 */
object RefactoringTestAssertions {

    // ========================================
    // Command Orchestrator Assertions
    // ========================================

    fun assertCommandOrchestratorReady(orchestrator: ICommandOrchestrator) {
        assertTrue("CommandOrchestrator should be ready", orchestrator.isReady)
        assertEquals(
            "CommandOrchestrator should be in READY state",
            ICommandOrchestrator.CommandOrchestratorState.READY,
            orchestrator.currentState
        )
    }

    fun assertCommandExecutedSuccessfully(
        result: ICommandOrchestrator.CommandResult,
        expectedTier: Int? = null
    ) {
        assertTrue(
            "Command should execute successfully",
            result is ICommandOrchestrator.CommandResult.Success
        )

        if (expectedTier != null && result is ICommandOrchestrator.CommandResult.Success) {
            assertEquals(
                "Command should execute on tier $expectedTier",
                expectedTier,
                result.tier
            )
        }
    }

    fun assertCommandFailed(
        result: ICommandOrchestrator.CommandResult,
        expectedReason: String? = null
    ) {
        assertTrue(
            "Command should fail",
            result is ICommandOrchestrator.CommandResult.Failure
        )

        if (expectedReason != null && result is ICommandOrchestrator.CommandResult.Failure) {
            assertTrue(
                "Failure reason should contain '$expectedReason'",
                result.reason.contains(expectedReason, ignoreCase = true)
            )
        }
    }

    fun assertFallbackModeEnabled(orchestrator: ICommandOrchestrator) {
        assertTrue(
            "Fallback mode should be enabled",
            orchestrator.isFallbackModeEnabled
        )
    }

    // ========================================
    // Event Router Assertions
    // ========================================

    fun assertEventRouterReady(router: IEventRouter) {
        assertTrue("EventRouter should be ready", router.isReady)
        assertEquals(
            "EventRouter should be in READY state",
            IEventRouter.EventRouterState.READY,
            router.currentState
        )
    }

    fun assertEventProcessed(
        metrics: IEventRouter.EventMetrics,
        minProcessedEvents: Long = 1
    ) {
        assertTrue(
            "At least $minProcessedEvents events should be processed",
            metrics.totalEventsProcessed >= minProcessedEvents
        )
    }

    fun assertEventFiltered(
        metrics: IEventRouter.EventMetrics,
        minFilteredEvents: Long = 1
    ) {
        assertTrue(
            "At least $minFilteredEvents events should be filtered",
            metrics.totalEventsFiltered >= minFilteredEvents
        )
    }

    // ========================================
    // Speech Manager Assertions
    // ========================================

    fun assertSpeechManagerReady(manager: ISpeechManager) {
        assertTrue("SpeechManager should be ready", manager.isReady)
    }

    fun assertListening(manager: ISpeechManager) {
        assertTrue("SpeechManager should be listening", manager.isListening)
        assertEquals(
            "SpeechManager should be in LISTENING state",
            ISpeechManager.RecognitionState.LISTENING,
            manager.recognitionState
        )
    }

    fun assertNotListening(manager: ISpeechManager) {
        assertFalse("SpeechManager should not be listening", manager.isListening)
    }

    fun assertEngineHealthy(
        status: ISpeechManager.EngineStatus,
        minSuccessRate: Float = 0.8f
    ) {
        assertTrue("Engine should be initialized", status.isInitialized)
        assertTrue("Engine should be available", status.isAvailable)
        assertTrue("Engine should be healthy", status.isHealthy)
        assertTrue(
            "Success rate should be >= $minSuccessRate",
            status.successRate >= minSuccessRate
        )
    }

    // ========================================
    // UI Scraping Service Assertions
    // ========================================

    fun assertUIScrapingReady(service: IUIScrapingService) {
        assertTrue("UIScrapingService should be ready", service.isReady)
        assertEquals(
            "UIScrapingService should be in READY state",
            IUIScrapingService.ScrapingState.READY,
            service.currentState
        )
    }

    fun assertElementsExtracted(
        elements: List<IUIScrapingService.UIElement>,
        minCount: Int = 1
    ) {
        assertTrue(
            "At least $minCount elements should be extracted",
            elements.size >= minCount
        )
    }

    fun assertElementCached(
        service: IUIScrapingService,
        packageName: String
    ) {
        assertTrue(
            "Elements should be cached for $packageName",
            service.isCached(packageName)
        )
    }

    fun assertCacheWithinLimit(service: IUIScrapingService) {
        assertTrue(
            "Cache size should not exceed max",
            service.cacheSize <= service.maxCacheSize
        )
    }

    // ========================================
    // Service Monitor Assertions
    // ========================================

    fun assertMonitoringActive(monitor: IServiceMonitor) {
        assertTrue("Monitoring should be active", monitor.isMonitoring)
    }

    fun assertHealthy(status: IServiceMonitor.HealthStatus) {
        assertEquals(
            "Health status should be HEALTHY",
            IServiceMonitor.HealthStatus.HEALTHY,
            status
        )
    }

    fun assertComponentHealthy(
        health: IServiceMonitor.ComponentHealth,
        component: IServiceMonitor.MonitoredComponent
    ) {
        assertEquals("Component should match", component, health.component)
        assertEquals(
            "Component should be HEALTHY",
            IServiceMonitor.HealthStatus.HEALTHY,
            health.status
        )
        assertTrue("Component should be responsive", health.isResponsive)
        assertEquals("Error count should be 0", 0, health.errorCount)
    }

    fun assertPerformanceAcceptable(
        snapshot: IServiceMonitor.PerformanceSnapshot,
        maxCpu: Float = 5f,
        maxMemoryMb: Long = 20,
        maxResponseMs: Long = 100
    ) {
        assertTrue(
            "CPU usage should be <= $maxCpu%",
            snapshot.cpuUsagePercent <= maxCpu
        )
        assertTrue(
            "Memory usage should be <= ${maxMemoryMb}MB",
            snapshot.memoryUsageMb <= maxMemoryMb
        )
        assertTrue(
            "Response time should be <= ${maxResponseMs}ms",
            snapshot.averageResponseTimeMs <= maxResponseMs
        )
    }

    // ========================================
    // Database Manager Assertions
    // ========================================

    fun assertDatabaseReady(manager: IDatabaseManager) {
        assertTrue("DatabaseManager should be ready", manager.isReady)
        assertTrue("Database should be healthy", manager.isDatabaseHealthy)
        assertEquals(
            "DatabaseManager should be in READY state",
            IDatabaseManager.DatabaseState.READY,
            manager.currentState
        )
    }

    fun assertCommandsLoaded(
        commands: List<IDatabaseManager.VoiceCommand>,
        minCount: Int = 1
    ) {
        assertTrue(
            "At least $minCount commands should be loaded",
            commands.size >= minCount
        )
    }

    fun assertCacheEnabled(manager: IDatabaseManager) {
        assertTrue("Cache should be enabled", manager.isCacheEnabled())
    }

    fun assertCacheEffective(
        stats: IDatabaseManager.CacheStats,
        minHitRate: Float = 0.5f
    ) {
        assertTrue("Cache should be enabled", stats.isEnabled)
        assertTrue(
            "Cache hit rate should be >= $minHitRate",
            stats.hitRate >= minHitRate
        )
    }

    // ========================================
    // State Manager Assertions
    // ========================================

    fun assertStateManagerReady(manager: IStateManager) {
        assertTrue("StateManager should be ready", manager.isReady)
        assertEquals(
            "StateManager should be in READY state",
            IStateManager.StateManagerState.READY,
            manager.currentState
        )
    }

    fun assertStateValid(result: IStateManager.ValidationResult) {
        assertTrue(
            "State should be valid",
            result is IStateManager.ValidationResult.Valid
        )
    }

    fun assertStateSnapshot(
        snapshot: IStateManager.StateSnapshot,
        expectedServiceReady: Boolean? = null,
        expectedVoiceInitialized: Boolean? = null
    ) {
        if (expectedServiceReady != null) {
            assertEquals(
                "Service ready state mismatch",
                expectedServiceReady,
                snapshot.isServiceReady
            )
        }
        if (expectedVoiceInitialized != null) {
            assertEquals(
                "Voice initialized state mismatch",
                expectedVoiceInitialized,
                snapshot.isVoiceInitialized
            )
        }
        assertStateValid(snapshot.validationResult)
    }

    // ========================================
    // Mock Verification Assertions
    // ========================================

    fun assertMockMethodCalled(
        mock: Any,
        methodName: String,
        expectedCallCount: Int? = null
    ) {
        val wasCalled = when (mock) {
            is MockCommandOrchestrator -> mock.wasMethodCalled(methodName)
            is MockEventRouter -> mock.wasMethodCalled(methodName)
            is MockSpeechManager -> mock.wasMethodCalled(methodName)
            is MockUIScrapingService -> mock.wasMethodCalled(methodName)
            is MockServiceMonitor -> mock.wasMethodCalled(methodName)
            is MockDatabaseManager -> mock.wasMethodCalled(methodName)
            is MockStateManager -> mock.wasMethodCalled(methodName)
            else -> false
        }

        assertTrue("Method $methodName should have been called", wasCalled)

        if (expectedCallCount != null && mock is MockCommandOrchestrator) {
            val actualCount = mock.getMethodCallCount(methodName)
            assertEquals(
                "Method $methodName call count mismatch",
                expectedCallCount,
                actualCount
            )
        }
    }

    fun assertMockReset(mock: Any) {
        when (mock) {
            is MockCommandOrchestrator -> assertFalse(
                "Mock should be reset",
                mock.isReady
            )
            is MockEventRouter -> assertFalse(
                "Mock should be reset",
                mock.isReady
            )
            is MockSpeechManager -> assertFalse(
                "Mock should be reset",
                mock.isReady
            )
            is MockUIScrapingService -> assertFalse(
                "Mock should be reset",
                mock.isReady
            )
            is MockServiceMonitor -> assertFalse(
                "Mock should be reset",
                mock.isMonitoring
            )
            is MockDatabaseManager -> assertFalse(
                "Mock should be reset",
                mock.isReady
            )
            is MockStateManager -> assertFalse(
                "Mock should be reset",
                mock.isReady
            )
        }
    }

    // ========================================
    // Performance Assertions
    // ========================================

    fun assertDIOverheadAcceptable(
        metrics: RefactoringTestUtils.DIOverheadMetrics,
        maxAverageMs: Double = 5.0,
        maxP95Ms: Double = 10.0
    ) {
        assertTrue(
            "DI average overhead should be <= ${maxAverageMs}ms (got ${metrics.averageMs}ms)",
            metrics.averageMs <= maxAverageMs
        )
        assertTrue(
            "DI P95 overhead should be <= ${maxP95Ms}ms (got ${metrics.p95Ms}ms)",
            metrics.p95Ms <= maxP95Ms
        )
    }

    fun assertStressTestSuccessful(
        result: RefactoringTestUtils.StressTestResult,
        minSuccessRate: Double = 0.95
    ) {
        assertTrue(
            "Stress test success rate should be >= $minSuccessRate (got ${result.successRate})",
            result.successRate >= minSuccessRate
        )
        assertTrue(
            "Stress test should have minimal failures (got ${result.failureCount})",
            result.failureCount < result.totalOperations * 0.05
        )
    }
}
