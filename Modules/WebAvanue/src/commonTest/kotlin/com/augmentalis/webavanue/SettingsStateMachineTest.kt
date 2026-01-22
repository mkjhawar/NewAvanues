package com.augmentalis.webavanue

import com.augmentalis.webavanue.BrowserSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Test suite for SettingsStateMachine
 *
 * Tests thread safety, state transitions, and concurrency handling.
 * These tests verify the L3 Runtime concurrency fixes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStateMachineTest {

    private lateinit var testScope: TestScope
    private lateinit var stateMachine: SettingsStateMachine

    @BeforeTest
    fun setup() {
        testScope = TestScope()
        stateMachine = SettingsStateMachine(testScope)
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
    }

    // ==================== Basic State Transitions ====================

    @Test
    fun `initial state is Idle`() = runTest {
        val state = stateMachine.state.first()
        assertTrue(state is SettingsState.Idle)
    }

    @Test
    fun `single update transitions Idle to Applying to Idle`() = runTest {
        val settings = BrowserSettings()
        var applyCount = 0

        // Request update
        stateMachine.requestUpdate(settings) { s ->
            applyCount++
            assertEquals(settings, s)
            delay(100) // Simulate async operation
            Result.success(Unit)
        }

        // Should be in Applying state
        advanceTimeBy(50)
        val applyingState = stateMachine.state.value
        assertTrue(applyingState is SettingsState.Applying)
        assertEquals(settings, (applyingState as SettingsState.Applying).settings)

        // Wait for completion
        advanceTimeBy(100)
        advanceUntilIdle()

        // Should return to Idle
        val finalState = stateMachine.state.value
        assertTrue(finalState is SettingsState.Idle)
        assertEquals(1, applyCount)
    }

    @Test
    fun `failed update transitions to Error state`() = runTest {
        val settings = BrowserSettings()
        val error = Exception("Apply failed")

        stateMachine.requestUpdate(settings) { s ->
            delay(50)
            Result.failure(error)
        }

        advanceUntilIdle()

        val errorState = stateMachine.state.value
        assertTrue(errorState is SettingsState.Error)
        assertEquals(settings, (errorState as SettingsState.Error).settings)
        assertEquals(error, errorState.error)
        assertEquals(0, errorState.retryCount)
    }

    // ==================== Queuing Behavior ====================

    @Test
    fun `rapid updates queue correctly`() = runTest {
        val settings1 = BrowserSettings(enableJavaScript = true)
        val settings2 = BrowserSettings(enableJavaScript = false)
        val appliedSettings = mutableListOf<BrowserSettings>()

        // Start first update (slow)
        stateMachine.requestUpdate(settings1) { s ->
            appliedSettings.add(s)
            delay(200)
            Result.success(Unit)
        }

        advanceTimeBy(50)

        // Request second update while first is applying
        stateMachine.requestUpdate(settings2) { s ->
            appliedSettings.add(s)
            delay(100)
            Result.success(Unit)
        }

        // Should be in Queued state
        val queuedState = stateMachine.state.value
        assertTrue(queuedState is SettingsState.Queued)
        assertEquals(settings1, (queuedState as SettingsState.Queued).current)
        assertEquals(settings2, queuedState.queued)

        // Wait for both to complete
        advanceUntilIdle()

        // Both settings should have been applied
        assertEquals(2, appliedSettings.size)
        assertEquals(settings1, appliedSettings[0])
        assertEquals(settings2, appliedSettings[1])

        // Should be back to Idle
        assertTrue(stateMachine.state.value is SettingsState.Idle)
    }

    @Test
    fun `queued update replaces previous queue`() = runTest {
        val settings1 = BrowserSettings(enableJavaScript = true)
        val settings2 = BrowserSettings(blockAds = true)
        val settings3 = BrowserSettings(blockTrackers = true)
        val appliedSettings = mutableListOf<BrowserSettings>()

        // Start first update
        stateMachine.requestUpdate(settings1) { s ->
            appliedSettings.add(s)
            delay(200)
            Result.success(Unit)
        }

        advanceTimeBy(50)

        // Queue second update
        stateMachine.requestUpdate(settings2) { s ->
            appliedSettings.add(s)
            delay(100)
            Result.success(Unit)
        }

        advanceTimeBy(50)

        // Queue third update (should replace second)
        stateMachine.requestUpdate(settings3) { s ->
            appliedSettings.add(s)
            delay(100)
            Result.success(Unit)
        }

        // Wait for completion
        advanceUntilIdle()

        // Should have applied settings1 and settings3 (settings2 was replaced)
        assertEquals(2, appliedSettings.size)
        assertEquals(settings1, appliedSettings[0])
        assertEquals(settings3, appliedSettings[1])

        assertTrue(stateMachine.state.value is SettingsState.Idle)
    }

    // ==================== Error Handling & Retry ====================

    @Test
    fun `retry error with exponential backoff`() = runTest {
        val settings = BrowserSettings()
        var attemptCount = 0
        val errors = mutableListOf<Exception>()

        // First attempt fails
        stateMachine.requestUpdate(settings) { s ->
            attemptCount++
            val error = Exception("Attempt $attemptCount failed")
            errors.add(error)
            Result.failure(error)
        }

        advanceUntilIdle()

        // Should be in Error state
        assertTrue(stateMachine.state.value is SettingsState.Error)
        assertEquals(1, attemptCount)

        // Retry (should succeed after delay)
        val retried = stateMachine.retryError({ s ->
            attemptCount++
            delay(50)
            Result.success(Unit)
        })

        assertTrue(retried)

        // Wait for exponential backoff (1 second) + apply time
        advanceTimeBy(1100)
        advanceUntilIdle()

        // Should have retried and succeeded
        assertEquals(2, attemptCount)
        assertTrue(stateMachine.state.value is SettingsState.Idle)
    }

    @Test
    fun `retry respects maxRetries limit`() = runTest {
        val settings = BrowserSettings()
        var attemptCount = 0

        // Initial attempt fails
        stateMachine.requestUpdate(settings) { s ->
            attemptCount++
            Result.failure(Exception("Failed"))
        }

        advanceUntilIdle()
        assertEquals(1, attemptCount)

        // Retry 3 times (max)
        repeat(3) {
            val retried = stateMachine.retryError({ s ->
                attemptCount++
                Result.failure(Exception("Failed"))
            }, maxRetries = 3)

            assertTrue(retried)
            advanceTimeBy(10000) // Skip exponential backoff delays
            advanceUntilIdle()
        }

        assertEquals(4, attemptCount) // Initial + 3 retries

        // Next retry should fail (max reached)
        val retried = stateMachine.retryError({ s ->
            attemptCount++
            Result.success(Unit)
        }, maxRetries = 3)

        assertFalse(retried)
        assertEquals(4, attemptCount) // No additional attempt
    }

    // ==================== Reset Behavior ====================

    @Test
    fun `reset clears queue and returns to Idle`() = runTest {
        val settings1 = BrowserSettings(enableJavaScript = true)
        val settings2 = BrowserSettings(blockAds = true)
        val appliedSettings = mutableListOf<BrowserSettings>()

        // Start first update
        stateMachine.requestUpdate(settings1) { s ->
            appliedSettings.add(s)
            delay(200)
            Result.success(Unit)
        }

        advanceTimeBy(50)

        // Queue second update
        stateMachine.requestUpdate(settings2) { s ->
            appliedSettings.add(s)
            delay(100)
            Result.success(Unit)
        }

        // Verify in Queued state
        assertTrue(stateMachine.state.value is SettingsState.Queued)

        // Reset
        stateMachine.reset()

        // Should be in Idle state
        assertTrue(stateMachine.state.value is SettingsState.Idle)

        // Wait for first update to complete (shouldn't trigger second)
        advanceUntilIdle()

        // Only first update should have been applied
        assertEquals(1, appliedSettings.size)
        assertEquals(settings1, appliedSettings[0])
    }

    // ==================== Concurrency Tests ====================

    @Test
    fun `concurrent requests are serialized`() = runTest {
        val settings1 = BrowserSettings(enableJavaScript = true)
        val settings2 = BrowserSettings(blockAds = true)
        val settings3 = BrowserSettings(blockTrackers = true)
        val appliedSettings = mutableListOf<BrowserSettings>()

        // Launch 3 concurrent requests
        val jobs = listOf(settings1, settings2, settings3).map { settings ->
            testScope.launch {
                stateMachine.requestUpdate(settings) { s ->
                    appliedSettings.add(s)
                    delay(100)
                    Result.success(Unit)
                }
            }
        }

        // Wait for all to complete
        jobs.forEach { it.join() }
        advanceUntilIdle()

        // All should have been applied (but only 2 due to queue replacement)
        // First one applies immediately, second and third get queued (third replaces second)
        assertTrue(appliedSettings.size >= 2)
        assertEquals(settings1, appliedSettings[0])

        assertTrue(stateMachine.state.value is SettingsState.Idle)
    }

    @Test
    fun `state machine prevents race conditions`() = runTest {
        val settings = BrowserSettings()
        var concurrentAccessCount = 0
        var maxConcurrent = 0
        var currentConcurrent = 0

        // Launch 10 rapid updates
        repeat(10) {
            testScope.launch {
                stateMachine.requestUpdate(settings) { s ->
                    // Track concurrent access
                    currentConcurrent++
                    if (currentConcurrent > maxConcurrent) {
                        maxConcurrent = currentConcurrent
                    }

                    delay(50)

                    currentConcurrent--
                    concurrentAccessCount++

                    Result.success(Unit)
                }
            }
        }

        advanceUntilIdle()

        // Mutex should ensure only 1 apply at a time (queue semantics)
        // Due to queue replacement, not all 10 will apply
        assertTrue(concurrentAccessCount < 10)
        assertTrue(maxConcurrent <= 1, "Expected max 1 concurrent, got $maxConcurrent")
    }

    // ==================== Utility Methods ====================

    @Test
    fun `isApplying returns correct state`() = runTest {
        assertFalse(stateMachine.isApplying())

        val settings = BrowserSettings()
        stateMachine.requestUpdate(settings) { s ->
            delay(100)
            Result.success(Unit)
        }

        advanceTimeBy(50)
        assertTrue(stateMachine.isApplying())

        advanceUntilIdle()
        assertFalse(stateMachine.isApplying())
    }

    @Test
    fun `getCurrentError returns error when in Error state`() = runTest {
        assertNull(stateMachine.getCurrentError())

        val settings = BrowserSettings()
        val error = Exception("Test error")

        stateMachine.requestUpdate(settings) { s ->
            Result.failure(error)
        }

        advanceUntilIdle()

        val currentError = stateMachine.getCurrentError()
        assertNotNull(currentError)
        assertEquals(error, currentError)
    }

    // ==================== Real-World Scenario ====================

    @Test
    fun `realistic settings update scenario`() = runTest {
        // User rapidly toggles settings
        val settings1 = BrowserSettings(enableJavaScript = true)
        val settings2 = BrowserSettings(enableJavaScript = false)
        val settings3 = BrowserSettings(enableJavaScript = true, blockAds = true)
        val appliedSettings = mutableListOf<BrowserSettings>()

        // User toggles JavaScript on
        stateMachine.requestUpdate(settings1) { s ->
            appliedSettings.add(s)
            delay(150) // WebView apply time
            Result.success(Unit)
        }

        // User immediately toggles it off (queues)
        advanceTimeBy(20)
        stateMachine.requestUpdate(settings2) { s ->
            appliedSettings.add(s)
            delay(150)
            Result.success(Unit)
        }

        // User changes mind again (replaces queue)
        advanceTimeBy(20)
        stateMachine.requestUpdate(settings3) { s ->
            appliedSettings.add(s)
            delay(150)
            Result.success(Unit)
        }

        // Wait for all updates
        advanceUntilIdle()

        // Should apply settings1, then settings3 (settings2 was replaced)
        assertEquals(2, appliedSettings.size)
        assertEquals(settings1, appliedSettings[0])
        assertEquals(settings3, appliedSettings[1])

        assertTrue(stateMachine.state.value is SettingsState.Idle)
    }
}
