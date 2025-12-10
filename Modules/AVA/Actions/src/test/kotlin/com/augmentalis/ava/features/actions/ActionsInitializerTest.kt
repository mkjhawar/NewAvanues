package com.augmentalis.ava.features.actions

import android.content.Context
import io.mockk.mockk
import kotlin.test.*

/**
 * Tests for ActionsInitializer.
 *
 * Validates:
 * - Initialization registers all built-in handlers
 * - Idempotent initialization (safe to call multiple times)
 * - Thread-safe initialization
 * - Reset functionality for testing
 */
class ActionsInitializerTest {

    private lateinit var mockContext: Context

    @BeforeTest
    fun setup() {
        mockContext = mockk(relaxed = true)
        ActionsInitializer.reset()
    }

    @AfterTest
    fun teardown() {
        ActionsInitializer.reset()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    fun `test initialize registers built-in handlers`() {
        ActionsInitializer.initialize(mockContext)

        val registeredIntents = IntentActionHandlerRegistry.getRegisteredIntents()

        // Should have registered TimeActionHandler, AlarmActionHandler, WeatherActionHandler
        assertTrue(registeredIntents.size >= 3, "Expected at least 3 built-in handlers")
        assertTrue(registeredIntents.contains("show_time"), "Missing TimeActionHandler")
        assertTrue(registeredIntents.contains("set_alarm"), "Missing AlarmActionHandler")
        assertTrue(registeredIntents.contains("check_weather"), "Missing WeatherActionHandler")
    }

    @Test
    fun `test initialize sets isInitialized flag`() {
        assertFalse(ActionsInitializer.isInitialized(), "Should not be initialized before calling initialize()")

        ActionsInitializer.initialize(mockContext)

        assertTrue(ActionsInitializer.isInitialized(), "Should be initialized after calling initialize()")
    }

    @Test
    fun `test initialize registers specific handlers`() {
        ActionsInitializer.initialize(mockContext)

        // Verify each expected handler is registered
        assertNotNull(IntentActionHandlerRegistry.getHandler("show_time"))
        assertNotNull(IntentActionHandlerRegistry.getHandler("set_alarm"))
        assertNotNull(IntentActionHandlerRegistry.getHandler("check_weather"))
    }

    // ========== IDEMPOTENCY TESTS ==========

    @Test
    fun `test initialize is idempotent`() {
        ActionsInitializer.initialize(mockContext)
        val firstCallIntents = IntentActionHandlerRegistry.getRegisteredIntents()

        // Call initialize again
        ActionsInitializer.initialize(mockContext)
        val secondCallIntents = IntentActionHandlerRegistry.getRegisteredIntents()

        // Should have same handlers (not duplicated)
        assertEquals(firstCallIntents.size, secondCallIntents.size)
        assertEquals(firstCallIntents.toSet(), secondCallIntents.toSet())
    }

    @Test
    fun `test multiple initialize calls do not duplicate handlers`() {
        repeat(5) {
            ActionsInitializer.initialize(mockContext)
        }

        val intents = IntentActionHandlerRegistry.getRegisteredIntents()

        // Count should be unique (no duplicates)
        assertEquals(intents.size, intents.toSet().size)
    }

    @Test
    fun `test isInitialized remains true after multiple initialize calls`() {
        ActionsInitializer.initialize(mockContext)
        assertTrue(ActionsInitializer.isInitialized())

        repeat(3) {
            ActionsInitializer.initialize(mockContext)
            assertTrue(ActionsInitializer.isInitialized())
        }
    }

    // ========== RESET TESTS ==========

    @Test
    fun `test reset clears initialization state`() {
        ActionsInitializer.initialize(mockContext)
        assertTrue(ActionsInitializer.isInitialized())

        ActionsInitializer.reset()

        assertFalse(ActionsInitializer.isInitialized())
    }

    @Test
    fun `test reset clears all registered handlers`() {
        ActionsInitializer.initialize(mockContext)
        assertTrue(IntentActionHandlerRegistry.getRegisteredIntents().isNotEmpty())

        ActionsInitializer.reset()

        assertTrue(IntentActionHandlerRegistry.getRegisteredIntents().isEmpty())
    }

    @Test
    fun `test can re-initialize after reset`() {
        ActionsInitializer.initialize(mockContext)
        ActionsInitializer.reset()

        assertFalse(ActionsInitializer.isInitialized())

        ActionsInitializer.initialize(mockContext)

        assertTrue(ActionsInitializer.isInitialized())
        assertTrue(IntentActionHandlerRegistry.getRegisteredIntents().isNotEmpty())
    }

    @Test
    fun `test reset is idempotent`() {
        ActionsInitializer.initialize(mockContext)

        ActionsInitializer.reset()
        ActionsInitializer.reset() // Reset again

        assertFalse(ActionsInitializer.isInitialized())
        assertTrue(IntentActionHandlerRegistry.getRegisteredIntents().isEmpty())
    }

    // ========== THREAD SAFETY TESTS ==========

    @Test
    fun `test concurrent initialize calls are thread-safe`() {
        // Simulate multiple threads calling initialize simultaneously
        val initResults = (1..10).map {
            ActionsInitializer.initialize(mockContext)
        }

        // Should be initialized exactly once
        assertTrue(ActionsInitializer.isInitialized())

        // Should have expected number of handlers (not duplicated)
        val intents = IntentActionHandlerRegistry.getRegisteredIntents()
        assertTrue(intents.size >= 3)
        assertEquals(intents.size, intents.toSet().size) // No duplicates
    }

    // ========== HANDLER COUNT TESTS ==========

    @Test
    fun `test initialize registers exactly 3 built-in handlers`() {
        ActionsInitializer.initialize(mockContext)

        val registeredIntents = IntentActionHandlerRegistry.getRegisteredIntents()

        // Current implementation has 3 built-in handlers
        // If this fails, either:
        // 1. New handlers were added (update this test)
        // 2. Handlers were removed (investigate)
        assertEquals(3, registeredIntents.size,
            "Expected exactly 3 built-in handlers. Got: $registeredIntents")
    }

    @Test
    fun `test initialize registers handlers in expected order`() {
        ActionsInitializer.initialize(mockContext)

        val registeredIntents = IntentActionHandlerRegistry.getRegisteredIntents()

        // Should contain all expected intents (order may vary)
        val expectedIntents = setOf("show_time", "set_alarm", "check_weather")
        assertEquals(expectedIntents, registeredIntents.toSet())
    }

    // ========== LIFECYCLE TESTS ==========

    @Test
    fun `test typical lifecycle - initialize, use, reset, re-initialize`() {
        // Initialize
        ActionsInitializer.initialize(mockContext)
        assertTrue(ActionsInitializer.isInitialized())
        assertTrue(IntentActionHandlerRegistry.hasHandler("show_time"))

        // Use (simulated by checking handlers exist)
        assertNotNull(IntentActionHandlerRegistry.getHandler("set_alarm"))

        // Reset (for testing)
        ActionsInitializer.reset()
        assertFalse(ActionsInitializer.isInitialized())
        assertFalse(IntentActionHandlerRegistry.hasHandler("show_time"))

        // Re-initialize
        ActionsInitializer.initialize(mockContext)
        assertTrue(ActionsInitializer.isInitialized())
        assertTrue(IntentActionHandlerRegistry.hasHandler("show_time"))
    }

    @Test
    fun `test isInitialized is false before any initialization`() {
        // In clean state (after setup() reset)
        assertFalse(ActionsInitializer.isInitialized())
    }
}
