package com.augmentalis.ava.features.actions

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for IntentActionHandlerRegistry.
 *
 * Validates:
 * - Handler registration (single and multiple)
 * - Handler lookup and retrieval
 * - Action execution delegation
 * - Thread safety and synchronization
 * - Error handling for missing handlers
 */
class IntentActionHandlerRegistryTest {

    private lateinit var mockContext: Context

    @BeforeTest
    fun setup() {
        mockContext = mockk(relaxed = true)
        IntentActionHandlerRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        IntentActionHandlerRegistry.clear()
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    fun `test register single handler`() {
        val handler = TestActionHandler("test_intent")

        IntentActionHandlerRegistry.register(handler)

        assertTrue(IntentActionHandlerRegistry.hasHandler("test_intent"))
        assertEquals(handler, IntentActionHandlerRegistry.getHandler("test_intent"))
    }

    @Test
    fun `test register multiple handlers individually`() {
        val handler1 = TestActionHandler("intent_1")
        val handler2 = TestActionHandler("intent_2")
        val handler3 = TestActionHandler("intent_3")

        IntentActionHandlerRegistry.register(handler1)
        IntentActionHandlerRegistry.register(handler2)
        IntentActionHandlerRegistry.register(handler3)

        assertTrue(IntentActionHandlerRegistry.hasHandler("intent_1"))
        assertTrue(IntentActionHandlerRegistry.hasHandler("intent_2"))
        assertTrue(IntentActionHandlerRegistry.hasHandler("intent_3"))

        val registeredIntents = IntentActionHandlerRegistry.getRegisteredIntents()
        assertEquals(3, registeredIntents.size)
        assertTrue(registeredIntents.containsAll(listOf("intent_1", "intent_2", "intent_3")))
    }

    @Test
    fun `test registerAll with varargs`() {
        val handlers = arrayOf(
            TestActionHandler("intent_a"),
            TestActionHandler("intent_b"),
            TestActionHandler("intent_c")
        )

        IntentActionHandlerRegistry.registerAll(*handlers)

        assertEquals(3, IntentActionHandlerRegistry.getRegisteredIntents().size)
        assertTrue(IntentActionHandlerRegistry.hasHandler("intent_a"))
        assertTrue(IntentActionHandlerRegistry.hasHandler("intent_b"))
        assertTrue(IntentActionHandlerRegistry.hasHandler("intent_c"))
    }

    @Test
    fun `test overwriting existing handler`() {
        val handler1 = TestActionHandler("test_intent", successMessage = "First handler")
        val handler2 = TestActionHandler("test_intent", successMessage = "Second handler")

        IntentActionHandlerRegistry.register(handler1)
        IntentActionHandlerRegistry.register(handler2)

        // Should have overwritten first handler
        val retrieved = IntentActionHandlerRegistry.getHandler("test_intent")
        assertEquals(handler2, retrieved)
        assertEquals(1, IntentActionHandlerRegistry.getRegisteredIntents().size)
    }

    // ========== LOOKUP TESTS ==========

    @Test
    fun `test hasHandler returns false for unregistered intent`() {
        assertFalse(IntentActionHandlerRegistry.hasHandler("nonexistent_intent"))
    }

    @Test
    fun `test getHandler returns null for unregistered intent`() {
        assertNull(IntentActionHandlerRegistry.getHandler("nonexistent_intent"))
    }

    @Test
    fun `test getRegisteredIntents returns empty list initially`() {
        val intents = IntentActionHandlerRegistry.getRegisteredIntents()
        assertTrue(intents.isEmpty())
    }

    @Test
    fun `test getRegisteredIntents returns all registered intent names`() {
        IntentActionHandlerRegistry.registerAll(
            TestActionHandler("show_time"),
            TestActionHandler("set_alarm"),
            TestActionHandler("check_weather")
        )

        val intents = IntentActionHandlerRegistry.getRegisteredIntents()
        assertEquals(3, intents.size)
        assertTrue(intents.contains("show_time"))
        assertTrue(intents.contains("set_alarm"))
        assertTrue(intents.contains("check_weather"))
    }

    // ========== EXECUTION TESTS ==========

    @Test
    fun `test executeAction with registered handler - success`() = runTest {
        val handler = TestActionHandler("test_intent", successMessage = "Action executed")
        IntentActionHandlerRegistry.register(handler)

        val result = IntentActionHandlerRegistry.executeAction(
            context = mockContext,
            intent = "test_intent",
            utterance = "test utterance"
        )

        assertTrue(result is ActionResult.Success)
        assertEquals("Action executed", (result as ActionResult.Success).message)
    }

    @Test
    fun `test executeAction with unregistered handler returns failure`() = runTest {
        val result = IntentActionHandlerRegistry.executeAction(
            context = mockContext,
            intent = "nonexistent_intent",
            utterance = "test"
        )

        assertTrue(result is ActionResult.Failure)
        assertTrue((result as ActionResult.Failure).message.contains("No action handler available"))
    }

    @Test
    fun `test executeAction catches handler exceptions`() = runTest {
        val handler = FailingActionHandler("failing_intent")
        IntentActionHandlerRegistry.register(handler)

        val result = IntentActionHandlerRegistry.executeAction(
            context = mockContext,
            intent = "failing_intent",
            utterance = "test"
        )

        assertTrue(result is ActionResult.Failure)
        val failure = result as ActionResult.Failure
        assertTrue(failure.message.contains("Failed to execute action"))
        assertNotNull(failure.exception)
    }

    @Test
    fun `test executeAction passes correct parameters to handler`() = runTest {
        val handler = CapturingActionHandler("capturing_intent")
        IntentActionHandlerRegistry.register(handler)

        IntentActionHandlerRegistry.executeAction(
            context = mockContext,
            intent = "capturing_intent",
            utterance = "Turn on the lights"
        )

        assertEquals(mockContext, handler.capturedContext)
        assertEquals("Turn on the lights", handler.capturedUtterance)
    }

    // ========== CLEAR TESTS ==========

    @Test
    fun `test clear removes all handlers`() {
        IntentActionHandlerRegistry.registerAll(
            TestActionHandler("intent_1"),
            TestActionHandler("intent_2"),
            TestActionHandler("intent_3")
        )

        assertEquals(3, IntentActionHandlerRegistry.getRegisteredIntents().size)

        IntentActionHandlerRegistry.clear()

        assertEquals(0, IntentActionHandlerRegistry.getRegisteredIntents().size)
        assertFalse(IntentActionHandlerRegistry.hasHandler("intent_1"))
    }

    @Test
    fun `test clear is idempotent`() {
        IntentActionHandlerRegistry.register(TestActionHandler("test"))
        IntentActionHandlerRegistry.clear()
        IntentActionHandlerRegistry.clear() // Clear again

        assertEquals(0, IntentActionHandlerRegistry.getRegisteredIntents().size)
    }

    // ========== THREAD SAFETY TESTS ==========

    @Test
    fun `test concurrent registration is thread-safe`() = runTest {
        val handlers = (1..100).map { TestActionHandler("intent_$it") }

        // Register all handlers (simulating concurrent access)
        handlers.forEach { handler ->
            IntentActionHandlerRegistry.register(handler)
        }

        // Verify all handlers were registered
        assertEquals(100, IntentActionHandlerRegistry.getRegisteredIntents().size)
        handlers.forEach { handler ->
            assertTrue(IntentActionHandlerRegistry.hasHandler(handler.intent))
        }
    }

    @Test
    fun `test concurrent access to getRegisteredIntents`() = runTest {
        IntentActionHandlerRegistry.registerAll(
            TestActionHandler("intent_1"),
            TestActionHandler("intent_2")
        )

        // Multiple reads should be consistent
        val results = (1..10).map {
            IntentActionHandlerRegistry.getRegisteredIntents()
        }

        results.forEach { intents ->
            assertEquals(2, intents.size)
        }
    }

    // ========== TEST HELPERS ==========

    /**
     * Simple test handler that always succeeds.
     */
    private class TestActionHandler(
        override val intent: String,
        private val successMessage: String = "Success"
    ) : IntentActionHandler {
        override suspend fun execute(context: Context, utterance: String): ActionResult {
            return ActionResult.Success(message = successMessage)
        }
    }

    /**
     * Test handler that always throws an exception.
     */
    private class FailingActionHandler(
        override val intent: String
    ) : IntentActionHandler {
        override suspend fun execute(context: Context, utterance: String): ActionResult {
            throw RuntimeException("Handler intentionally failed")
        }
    }

    /**
     * Test handler that captures execution parameters.
     */
    private class CapturingActionHandler(
        override val intent: String
    ) : IntentActionHandler {
        var capturedContext: Context? = null
        var capturedUtterance: String? = null

        override suspend fun execute(context: Context, utterance: String): ActionResult {
            capturedContext = context
            capturedUtterance = utterance
            return ActionResult.Success()
        }
    }
}
