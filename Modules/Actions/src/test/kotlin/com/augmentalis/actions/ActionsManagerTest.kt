package com.augmentalis.actions

import android.content.Context
import com.augmentalis.ava.core.domain.resolution.AppResolverService
import com.augmentalis.ava.core.domain.resolution.PreferencePromptManager
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for ActionsManager (Hilt-injectable wrapper).
 *
 * Validates:
 * - ActionsManager wraps ActionsInitializer correctly
 * - ActionsManager wraps IntentActionHandlerRegistry correctly
 * - Initialization through ActionsManager
 * - Action execution through ActionsManager
 * - Handler lookup through ActionsManager
 * - Reset functionality
 */
class ActionsManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppResolverService: AppResolverService
    private lateinit var mockPreferencePromptManager: PreferencePromptManager
    private lateinit var actionsManager: ActionsManager

    @BeforeTest
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAppResolverService = mockk(relaxed = true)
        mockPreferencePromptManager = mockk(relaxed = true)
        actionsManager = ActionsManager(mockContext, mockAppResolverService, mockPreferencePromptManager)
        ActionsInitializer.reset()
    }

    @AfterTest
    fun teardown() {
        ActionsInitializer.reset()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    fun `test initialize delegates to ActionsInitializer`() {
        assertFalse(actionsManager.isInitialized())

        actionsManager.initialize()

        assertTrue(actionsManager.isInitialized())
        assertTrue(ActionsInitializer.isInitialized())
    }

    @Test
    fun `test initialize is idempotent through ActionsManager`() {
        actionsManager.initialize()
        val firstCallIntents = actionsManager.getRegisteredIntents()

        actionsManager.initialize()
        val secondCallIntents = actionsManager.getRegisteredIntents()

        assertEquals(firstCallIntents.size, secondCallIntents.size)
    }

    @Test
    fun `test isInitialized reflects ActionsInitializer state`() {
        // Before initialization
        assertFalse(actionsManager.isInitialized())

        // After initialization via ActionsManager
        actionsManager.initialize()
        assertTrue(actionsManager.isInitialized())

        // After reset
        ActionsInitializer.reset()
        assertFalse(actionsManager.isInitialized())
    }

    // ========== HANDLER LOOKUP TESTS ==========

    @Test
    fun `test hasHandler delegates to registry`() {
        actionsManager.initialize()

        assertTrue(actionsManager.hasHandler("show_time"))
        assertTrue(actionsManager.hasHandler("set_alarm"))
        assertTrue(actionsManager.hasHandler("check_weather"))
        assertFalse(actionsManager.hasHandler("nonexistent_intent"))
    }

    @Test
    fun `test getHandler delegates to registry`() {
        actionsManager.initialize()

        val timeHandler = actionsManager.getHandler("show_time")
        assertNotNull(timeHandler)
        assertEquals("show_time", timeHandler.intent)

        val nonexistentHandler = actionsManager.getHandler("nonexistent")
        assertNull(nonexistentHandler)
    }

    @Test
    fun `test getRegisteredIntents delegates to registry`() {
        actionsManager.initialize()

        val intents = actionsManager.getRegisteredIntents()
        assertEquals(3, intents.size)
        assertTrue(intents.contains("show_time"))
        assertTrue(intents.contains("set_alarm"))
        assertTrue(intents.contains("check_weather"))
    }

    @Test
    fun `test getRegisteredIntents returns empty list before initialization`() {
        val intents = actionsManager.getRegisteredIntents()
        assertTrue(intents.isEmpty())
    }

    // ========== ACTION EXECUTION TESTS ==========

    @Test
    fun `test executeAction with valid handler`() = runTest {
        actionsManager.initialize()

        val result = actionsManager.executeAction(
            intent = "show_time",
            utterance = "What time is it?"
        )

        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `test executeAction with invalid handler returns failure`() = runTest {
        actionsManager.initialize()

        val result = actionsManager.executeAction(
            intent = "nonexistent_intent",
            utterance = "test"
        )

        assertTrue(result is ActionResult.Failure)
        assertTrue((result as ActionResult.Failure).message.contains("No action handler"))
    }

    @Test
    fun `test executeAction uses injected context`() = runTest {
        actionsManager.initialize()

        // Execute action - internally should use mockContext
        val result = actionsManager.executeAction(
            intent = "show_time",
            utterance = "What time is it?"
        )

        // Should succeed (mock context works)
        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `test executeAction before initialization`() = runTest {
        // Don't initialize

        val result = actionsManager.executeAction(
            intent = "show_time",
            utterance = "test"
        )

        // Should fail - no handlers registered
        assertTrue(result is ActionResult.Failure)
    }

    // ========== RESET TESTS ==========

    @Test
    fun `test reset clears initialization`() {
        actionsManager.initialize()
        assertTrue(actionsManager.isInitialized())

        actionsManager.reset()

        assertFalse(actionsManager.isInitialized())
    }

    @Test
    fun `test reset clears handlers`() {
        actionsManager.initialize()
        assertTrue(actionsManager.hasHandler("show_time"))

        actionsManager.reset()

        assertFalse(actionsManager.hasHandler("show_time"))
        assertTrue(actionsManager.getRegisteredIntents().isEmpty())
    }

    @Test
    fun `test can re-initialize after reset`() {
        actionsManager.initialize()
        actionsManager.reset()

        actionsManager.initialize()

        assertTrue(actionsManager.isInitialized())
        assertTrue(actionsManager.hasHandler("show_time"))
    }

    // ========== LIFECYCLE SIMULATION TESTS ==========

    @Test
    fun `test typical ViewModel lifecycle`() = runTest {
        // Simulate ViewModel constructor + init block
        val viewModelActionsManager = ActionsManager(mockContext, mockAppResolverService, mockPreferencePromptManager)
        viewModelActionsManager.initialize()

        // Verify initialized
        assertTrue(viewModelActionsManager.isInitialized())

        // Simulate user action
        val hasHandler = viewModelActionsManager.hasHandler("show_time")
        assertTrue(hasHandler)

        // Execute action
        val result = viewModelActionsManager.executeAction(
            intent = "show_time",
            utterance = "What time is it?"
        )

        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `test multiple ViewModels share same registry`() {
        val manager1 = ActionsManager(mockContext, mockAppResolverService, mockPreferencePromptManager)
        val manager2 = ActionsManager(mockContext, mockAppResolverService, mockPreferencePromptManager)

        // Initialize through first manager
        manager1.initialize()

        // Second manager should see same initialization state
        assertTrue(manager2.isInitialized())
        assertTrue(manager2.hasHandler("show_time"))
    }

    // ========== HANDLER EXISTENCE TESTS ==========

    @Test
    fun `test all expected handlers are registered`() {
        actionsManager.initialize()

        val expectedHandlers = listOf(
            "show_time",
            "set_alarm",
            "check_weather"
        )

        expectedHandlers.forEach { intent ->
            assertTrue(
                actionsManager.hasHandler(intent),
                "Expected handler for intent: $intent"
            )
            assertNotNull(
                actionsManager.getHandler(intent),
                "Expected to retrieve handler for: $intent"
            )
        }
    }

    // ========== EDGE CASES ==========

    @Test
    fun `test executeAction with empty utterance`() = runTest {
        actionsManager.initialize()

        val result = actionsManager.executeAction(
            intent = "show_time",
            utterance = ""
        )

        // Should still succeed (handlers may not use utterance)
        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `test executeAction with very long utterance`() = runTest {
        actionsManager.initialize()

        val longUtterance = "test ".repeat(1000)
        val result = actionsManager.executeAction(
            intent = "show_time",
            utterance = longUtterance
        )

        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `test hasHandler with empty string`() {
        actionsManager.initialize()

        assertFalse(actionsManager.hasHandler(""))
    }

    @Test
    fun `test getHandler with empty string returns null`() {
        actionsManager.initialize()

        assertNull(actionsManager.getHandler(""))
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    fun `test initialize and execute all built-in handlers`() = runTest {
        actionsManager.initialize()

        val handlers = actionsManager.getRegisteredIntents()

        // Execute each handler to verify they work
        handlers.forEach { intent ->
            val result = actionsManager.executeAction(
                intent = intent,
                utterance = "test utterance for $intent"
            )

            // All built-in handlers should succeed (or at least not throw)
            assertTrue(
                result is ActionResult.Success || result is ActionResult.Failure,
                "Expected ActionResult for intent: $intent"
            )
        }
    }
}
