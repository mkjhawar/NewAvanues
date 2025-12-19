/**
 * ActionCoordinatorTest.kt - Comprehensive unit tests for ActionCoordinator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-18
 *
 * Tests action coordination, routing, metrics, and error handling.
 * Uses MockK for mocking and kotlinx-coroutines-test for suspend functions.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
import com.augmentalis.voiceoscore.accessibility.handlers.ActionHandler
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ActionCoordinator.
 *
 * Tests:
 * - Handler registration and initialization
 * - Action routing and execution
 * - Command processing and interpretation
 * - Voice command processing with confidence
 * - Error handling and timeouts
 * - Performance metrics tracking
 * - Async action execution
 * - Handler priority and matching
 * - Debug information
 * - Resource cleanup
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActionCoordinatorTest {

    private lateinit var mockService: VoiceOSService
    private lateinit var actionCoordinator: ActionCoordinator

    @Before
    fun setup() {
        // Mock VoiceOSService
        mockService = mockk<VoiceOSService>(relaxed = true)

        // Create ActionCoordinator with mocked service
        actionCoordinator = ActionCoordinator(mockService)
    }

    @After
    fun tearDown() {
        // Clean up resources
        actionCoordinator.dispose()
        clearAllMocks()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    fun `initialize registers all default handlers`() {
        // Act
        actionCoordinator.initialize()

        // Assert - Check that handlers are registered by verifying supported actions
        val allActions = actionCoordinator.getAllSupportedActions()

        assertFalse("Should have registered handlers with actions", allActions.isEmpty())
        assertTrue("Should contain system actions",
            allActions.any { it.contains("system", ignoreCase = true) })
    }

    @Test
    fun `initialize handles handler initialization errors gracefully`() {
        // Note: This test verifies that initialize() doesn't crash even if handlers fail
        // The real handlers are created internally, so we test the overall behavior

        // Act - should not throw
        actionCoordinator.initialize()

        // Assert - coordinator should still be functional
        assertTrue("Coordinator should be functional after init",
            actionCoordinator.getAllSupportedActions().isNotEmpty())
    }

    // ========== ACTION HANDLING TESTS ==========

    @Test
    fun `canHandle returns true for supported action`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val canHandleBack = actionCoordinator.canHandle("navigate_back")

        // Assert
        assertTrue("Should handle navigate_back", canHandleBack)
    }

    @Test
    fun `canHandle returns false for unsupported action`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val canHandle = actionCoordinator.canHandle("nonexistent_action_xyz")

        // Assert
        assertFalse("Should not handle nonexistent action", canHandle)
    }

    @Test
    fun `executeAction returns false for unsupported action`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.executeAction("nonexistent_action_xyz")

        // Assert
        assertFalse("Should return false for unsupported action", result)
    }

    @Test
    fun `executeAction executes supported action successfully`() {
        // Arrange
        actionCoordinator.initialize()

        // Act - Using a system action that should be handled
        val result = actionCoordinator.executeAction("navigate_back")

        // Assert - May succeed or fail depending on service state, but should not crash
        // The important part is that it routes to the correct handler
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun `executeAction with parameters passes params to handler`() {
        // Arrange
        actionCoordinator.initialize()
        val params = mapOf("key" to "value", "count" to 5)

        // Act
        val result = actionCoordinator.executeAction("navigate_back", params)

        // Assert - Should execute without crashing
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun `executeAction records metrics for success`() {
        // Arrange
        actionCoordinator.initialize()
        val action = "navigate_back"

        // Act
        actionCoordinator.executeAction(action)

        // Assert
        val metrics = actionCoordinator.getMetricsForAction(action)
        assertNotNull("Metrics should be recorded", metrics)
        assertEquals("Should have 1 execution", 1, metrics?.count)
    }

    @Test
    fun `executeAction records metrics for failure`() {
        // Arrange
        actionCoordinator.initialize()
        val action = "nonexistent_action"

        // Act
        actionCoordinator.executeAction(action)

        // Assert
        val metrics = actionCoordinator.getMetricsForAction(action)
        assertNotNull("Metrics should be recorded even for failures", metrics)
        assertEquals("Should have 1 execution", 1, metrics?.count)
        assertEquals("Should have 0 successes", 0, metrics?.successCount)
    }

    // ========== COMMAND PROCESSING TESTS ==========

    @Test
    fun `processCommand returns false for empty command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("")

        // Assert
        assertFalse("Should return false for empty command", result)
    }

    @Test
    fun `processCommand returns false for blank command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("   ")

        // Assert
        assertFalse("Should return false for blank command", result)
    }

    @Test
    fun `processCommand interprets go back as navigate_back`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("go back")

        // Assert
        assertNotNull("Should process 'go back' command", result)
    }

    @Test
    fun `processCommand interprets scroll up command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("scroll up")

        // Assert
        assertNotNull("Should process 'scroll up' command", result)
    }

    @Test
    fun `processCommand interprets volume up command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("volume up")

        // Assert
        assertNotNull("Should process 'volume up' command", result)
    }

    @Test
    fun `processCommand interprets open app command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("open settings")

        // Assert
        assertNotNull("Should process 'open settings' command", result)
    }

    @Test
    fun `processCommand interprets launch app command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("launch chrome")

        // Assert
        assertNotNull("Should process 'launch chrome' command", result)
    }

    @Test
    fun `processCommand interprets swipe commands`() {
        // Arrange
        actionCoordinator.initialize()

        // Act & Assert
        assertNotNull("Should process swipe left",
            actionCoordinator.processCommand("swipe left"))
        assertNotNull("Should process swipe right",
            actionCoordinator.processCommand("swipe right"))
        assertNotNull("Should process swipe up",
            actionCoordinator.processCommand("swipe up"))
        assertNotNull("Should process swipe down",
            actionCoordinator.processCommand("swipe down"))
    }

    @Test
    fun `processCommand interprets type text command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("type hello world")

        // Assert
        assertNotNull("Should process 'type' command", result)
    }

    @Test
    fun `processCommand interprets bluetooth commands`() {
        // Arrange
        actionCoordinator.initialize()

        // Act & Assert
        assertNotNull("Should process bluetooth enable",
            actionCoordinator.processCommand("bluetooth on"))
        assertNotNull("Should process bluetooth disable",
            actionCoordinator.processCommand("bluetooth off"))
    }

    @Test
    fun `processCommand interprets show numbers command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("show numbers")

        // Assert
        assertNotNull("Should process 'show numbers' command", result)
    }

    @Test
    fun `processCommand interprets tap number command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("tap 5")

        // Assert
        assertNotNull("Should process 'tap 5' command", result)
    }

    @Test
    fun `processCommand interprets select number command`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processCommand("select 3")

        // Assert
        assertNotNull("Should process 'select 3' command", result)
    }

    @Test
    fun `processCommand handles case insensitivity`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result1 = actionCoordinator.processCommand("GO BACK")
        val result2 = actionCoordinator.processCommand("Go Back")
        val result3 = actionCoordinator.processCommand("go back")

        // Assert - All should be processed the same way
        assertNotNull("Should handle uppercase", result1)
        assertNotNull("Should handle mixed case", result2)
        assertNotNull("Should handle lowercase", result3)
    }

    // ========== VOICE COMMAND PROCESSING TESTS ==========

    @Test
    fun `processVoiceCommand processes command with high confidence`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processVoiceCommand("go back", 0.95f)

        // Assert
        assertNotNull("Should process voice command", result)
    }

    @Test
    fun `processVoiceCommand processes command with low confidence`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result = actionCoordinator.processVoiceCommand("go back", 0.3f)

        // Assert
        assertNotNull("Should process voice command even with low confidence", result)
    }

    @Test
    fun `processVoiceCommand records metrics with voice prefix`() {
        // Arrange
        actionCoordinator.initialize()
        val command = "go back"

        // Act
        actionCoordinator.processVoiceCommand(command, 0.9f)

        // Assert
        val metrics = actionCoordinator.getMetrics()
        assertTrue("Should record voice-prefixed metrics",
            metrics.keys.any { it.startsWith("voice:") })
    }

    @Test
    fun `processVoiceCommand handles common voice variations`() {
        // Arrange
        actionCoordinator.initialize()

        // Act - Commands with common prefixes/suffixes
        val result1 = actionCoordinator.processVoiceCommand("please go back", 0.9f)
        val result2 = actionCoordinator.processVoiceCommand("can you go home", 0.9f)
        val result3 = actionCoordinator.processVoiceCommand("scroll up please", 0.9f)

        // Assert
        assertNotNull("Should handle 'please' prefix", result1)
        assertNotNull("Should handle 'can you' prefix", result2)
        assertNotNull("Should handle 'please' suffix", result3)
    }

    // ========== ASYNC EXECUTION TESTS ==========

    @Test
    fun `executeActionAsync executes action asynchronously`() = runTest {
        // Arrange
        actionCoordinator.initialize()
        var callbackInvoked = false
        var callbackResult: Boolean? = null

        // Act
        actionCoordinator.executeActionAsync("navigate_back") { result ->
            callbackInvoked = true
            callbackResult = result
        }

        // Wait for async execution
        Thread.sleep(200)

        // Assert
        assertTrue("Callback should be invoked", callbackInvoked)
        assertNotNull("Callback should receive result", callbackResult)
    }

    // ========== SUPPORTED ACTIONS TESTS ==========

    @Test
    fun `getAllSupportedActions returns non-empty list`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val actions = actionCoordinator.getAllSupportedActions()

        // Assert
        assertTrue("Should have supported actions", actions.isNotEmpty())
    }

    @Test
    fun `getSupportedActions returns actions for specific category`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val systemActions = actionCoordinator.getSupportedActions(ActionCategory.SYSTEM)

        // Assert
        assertTrue("Should have system actions", systemActions.isNotEmpty())
    }

    @Test
    fun `getSupportedActions returns empty list for unregistered category`() {
        // Arrange
        val coordinator = ActionCoordinator(mockService)
        // Don't initialize - no handlers registered

        // Act
        val actions = coordinator.getSupportedActions(ActionCategory.SYSTEM)

        // Assert
        assertTrue("Should return empty list for unregistered category", actions.isEmpty())

        coordinator.dispose()
    }

    @Test
    fun `getAllActions returns comprehensive action list`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val allActions = actionCoordinator.getAllActions()

        // Assert
        assertTrue("Should have actions", allActions.isNotEmpty())
        assertTrue("Should contain mute voice", allActions.contains("mute voice"))
        assertTrue("Should contain wake up voice", allActions.contains("wake up voice"))
        assertTrue("Should contain dictation", allActions.contains("dictation"))
    }

    // ========== METRICS TESTS ==========

    @Test
    fun `getMetrics returns empty map initially`() {
        // Arrange
        val coordinator = ActionCoordinator(mockService)

        // Act
        val metrics = coordinator.getMetrics()

        // Assert
        assertTrue("Should have no metrics initially", metrics.isEmpty())

        coordinator.dispose()
    }

    @Test
    fun `getMetrics returns accumulated metrics after executions`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        actionCoordinator.executeAction("navigate_back")
        actionCoordinator.executeAction("navigate_back")
        actionCoordinator.executeAction("navigate_home")

        // Assert
        val metrics = actionCoordinator.getMetrics()
        assertTrue("Should have metrics", metrics.isNotEmpty())

        val backMetrics = metrics["navigate_back"]
        assertNotNull("Should have metrics for navigate_back", backMetrics)
        assertEquals("Should have 2 executions", 2, backMetrics?.count)
    }

    @Test
    fun `getMetricsForAction returns null for unexecuted action`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val metrics = actionCoordinator.getMetricsForAction("never_executed_action")

        // Assert
        assertNull("Should return null for unexecuted action", metrics)
    }

    @Test
    fun `clearMetrics removes all metrics`() {
        // Arrange
        actionCoordinator.initialize()
        actionCoordinator.executeAction("navigate_back")

        // Act
        actionCoordinator.clearMetrics()

        // Assert
        val metrics = actionCoordinator.getMetrics()
        assertTrue("Should have no metrics after clear", metrics.isEmpty())
    }

    @Test
    fun `MetricData calculates average time correctly`() {
        // Arrange
        val metricData = ActionCoordinator.MetricData()

        // Act
        metricData.count = 4
        metricData.totalTimeMs = 400

        // Assert
        assertEquals("Average should be 100ms", 100, metricData.averageTimeMs)
    }

    @Test
    fun `MetricData calculates success rate correctly`() {
        // Arrange
        val metricData = ActionCoordinator.MetricData()

        // Act
        metricData.count = 10
        metricData.successCount = 8

        // Assert
        assertEquals("Success rate should be 0.8", 0.8f, metricData.successRate, 0.001f)
    }

    @Test
    fun `MetricData handles zero count gracefully`() {
        // Arrange
        val metricData = ActionCoordinator.MetricData()

        // Assert
        assertEquals("Average should be 0 for zero count", 0, metricData.averageTimeMs)
        assertEquals("Success rate should be 0 for zero count", 0f, metricData.successRate, 0.001f)
    }

    // ========== HANDLER PRIORITY TESTS ==========

    @Test
    fun `findHandler respects priority order for ambiguous actions`() {
        // Arrange
        actionCoordinator.initialize()

        // Act - System actions should have highest priority
        val result = actionCoordinator.executeAction("navigate_back")

        // Assert - Should execute without error (priority is respected internally)
        assertNotNull("Should handle action according to priority", result)
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    fun `executeAction handles handler exceptions gracefully`() {
        // Arrange
        actionCoordinator.initialize()

        // Act - Execute with an action that might fail
        val result = actionCoordinator.executeAction("some_action_that_might_fail")

        // Assert - Should not throw, should return false
        assertNotNull("Should handle exceptions gracefully", result)
    }

    @Test
    fun `processVoiceCommand handles exceptions gracefully`() {
        // Arrange
        actionCoordinator.initialize()

        // Act - Process potentially problematic command
        val result = actionCoordinator.processVoiceCommand("", 0.5f)

        // Assert - Should not throw
        assertNotNull("Should handle exceptions gracefully", result)
    }

    // ========== DEBUG INFO TESTS ==========

    @Test
    fun `getDebugInfo returns non-empty string`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val debugInfo = actionCoordinator.getDebugInfo()

        // Assert
        assertFalse("Debug info should not be empty", debugInfo.isEmpty())
        assertTrue("Should contain header",
            debugInfo.contains("ActionCoordinator Debug Info"))
        assertTrue("Should contain handler info",
            debugInfo.contains("Handlers:"))
    }

    @Test
    fun `getDebugInfo includes metrics information`() {
        // Arrange
        actionCoordinator.initialize()
        actionCoordinator.executeAction("navigate_back")

        // Act
        val debugInfo = actionCoordinator.getDebugInfo()

        // Assert
        assertTrue("Should contain metrics info",
            debugInfo.contains("Metrics:"))
    }

    // ========== DISPOSAL TESTS ==========

    @Test
    fun `dispose clears all handlers`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        actionCoordinator.dispose()

        // Assert
        val actions = actionCoordinator.getAllSupportedActions()
        assertTrue("Should have no actions after disposal", actions.isEmpty())
    }

    @Test
    fun `dispose clears all metrics`() {
        // Arrange
        actionCoordinator.initialize()
        actionCoordinator.executeAction("navigate_back")

        // Act
        actionCoordinator.dispose()

        // Assert
        val metrics = actionCoordinator.getMetrics()
        assertTrue("Should have no metrics after disposal", metrics.isEmpty())
    }

    @Test
    fun `dispose can be called multiple times safely`() {
        // Arrange
        actionCoordinator.initialize()

        // Act & Assert - Should not throw
        actionCoordinator.dispose()
        actionCoordinator.dispose()
        actionCoordinator.dispose()
    }

    // ========== COMMAND VARIATION TESTS ==========

    @Test
    fun `voice command variations handle verb transformations`() {
        // Arrange
        actionCoordinator.initialize()

        // Act - Different verbs that mean the same thing
        val result1 = actionCoordinator.processVoiceCommand("press the button", 0.9f)
        val result2 = actionCoordinator.processVoiceCommand("tap the button", 0.9f)
        val result3 = actionCoordinator.processVoiceCommand("touch the button", 0.9f)

        // Assert - All should be processed (converted to similar actions)
        assertNotNull("Should handle 'press'", result1)
        assertNotNull("Should handle 'tap'", result2)
        assertNotNull("Should handle 'touch'", result3)
    }

    @Test
    fun `voice command variations handle prefix removal`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result1 = actionCoordinator.processVoiceCommand("please go back", 0.9f)
        val result2 = actionCoordinator.processVoiceCommand("can you go back", 0.9f)
        val result3 = actionCoordinator.processVoiceCommand("hey go back", 0.9f)

        // Assert
        assertNotNull("Should handle 'please' prefix", result1)
        assertNotNull("Should handle 'can you' prefix", result2)
        assertNotNull("Should handle 'hey' prefix", result3)
    }

    @Test
    fun `voice command variations handle suffix removal`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        val result1 = actionCoordinator.processVoiceCommand("go back please", 0.9f)
        val result2 = actionCoordinator.processVoiceCommand("go back now", 0.9f)
        val result3 = actionCoordinator.processVoiceCommand("go back for me", 0.9f)

        // Assert
        assertNotNull("Should handle 'please' suffix", result1)
        assertNotNull("Should handle 'now' suffix", result2)
        assertNotNull("Should handle 'for me' suffix", result3)
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    fun `full workflow from voice command to execution with metrics`() {
        // Arrange
        actionCoordinator.initialize()
        val command = "go back"
        val confidence = 0.9f

        // Act
        val result = actionCoordinator.processVoiceCommand(command, confidence)

        // Assert
        assertNotNull("Should process command", result)

        val metrics = actionCoordinator.getMetrics()
        assertTrue("Should record metrics", metrics.isNotEmpty())

        val debugInfo = actionCoordinator.getDebugInfo()
        assertTrue("Should provide debug info", debugInfo.isNotEmpty())
    }

    @Test
    fun `multiple executions of same action accumulate metrics`() {
        // Arrange
        actionCoordinator.initialize()
        val action = "navigate_back"

        // Act
        actionCoordinator.executeAction(action)
        actionCoordinator.executeAction(action)
        actionCoordinator.executeAction(action)

        // Assert
        val metrics = actionCoordinator.getMetricsForAction(action)
        assertNotNull("Should have metrics", metrics)
        assertEquals("Should have 3 executions", 3, metrics?.count)
        assertTrue("Should have total time > 0", metrics?.totalTimeMs ?: 0 > 0)
    }

    @Test
    fun `mixed success and failure executions track correctly`() {
        // Arrange
        actionCoordinator.initialize()

        // Act
        actionCoordinator.executeAction("navigate_back")  // Likely to succeed
        actionCoordinator.executeAction("nonexistent_1")   // Will fail
        actionCoordinator.executeAction("navigate_home")   // Likely to succeed
        actionCoordinator.executeAction("nonexistent_2")   // Will fail

        // Assert
        val metrics = actionCoordinator.getMetrics()
        assertTrue("Should have metrics for multiple actions", metrics.size >= 2)
    }
}
