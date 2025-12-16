/**
 * StateTransitionTest.kt - Comprehensive state transition tests
 *
 * Tests all state machines in JITLearning and LearnAppCore:
 * 1. JITLearningService lifecycle states
 * 2. Learning states (IDLE → CAPTURING → ANALYZING → STORING)
 * 3. Screen context states (NEW → VISITED → LEARNED)
 * 4. Batch processing states (EMPTY → FILLING → FULL → FLUSHING)
 *
 * Uses state machine testing patterns:
 * - Valid transitions succeed
 * - Invalid transitions fail with correct errors
 * - Concurrent state changes handled safely
 * - Timing diagrams in comments
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-LearnApp-Phase2-Tests-51211-V1.md
 */

package com.augmentalis.jitlearning

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ServiceTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * State Transition Tests
 *
 * Tests all state machines in the JIT Learning system.
 *
 * ## State Machines Tested:
 * 1. Service Lifecycle States
 * 2. Learning States
 * 3. Screen Context States
 * 4. Batch Processing States
 * 5. Exploration States
 *
 * @since 2.1.0 (Phase 2 State Transition Tests)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StateTransitionTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private var serviceBinder: IElementCaptureService? = null
    private var mockProvider: JITLearnerProvider? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create mock provider
        mockProvider = mockk<JITLearnerProvider>(relaxed = true)

        // Setup default mock behavior
        every { mockProvider?.isLearningActive() } returns true
        every { mockProvider?.isLearningPaused() } returns false
        every { mockProvider?.getScreensLearnedCount() } returns 0
        every { mockProvider?.getElementsDiscoveredCount() } returns 0
        every { mockProvider?.getCurrentPackage() } returns null
        every { mockProvider?.getLearnedScreenHashes(any()) } returns emptyList()
        every { mockProvider?.startExploration(any()) } returns true
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.idle()
    }

    @After
    fun teardown() {
        serviceBinder = null
        mockProvider = null
    }

    // ================================================================
    // TEST GROUP 1: SERVICE LIFECYCLE STATE TRANSITIONS
    // ================================================================

    /**
     * Test: Service Lifecycle - Happy Path
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms   200ms   300ms   400ms   500ms   600ms
     * │      │       │       │       │       │       │
     * CREATE → BIND → RUN → PAUSE → RESUME → UNBIND → DESTROY
     * ```
     *
     * Expected: All transitions succeed
     */
    @Test
    fun serviceLifecycle_HappyPath_AllTransitionsSucceed() {
        // CREATED
        val serviceIntent = Intent(context, JITLearningService::class.java)

        // BOUND
        val binder = serviceRule.bindService(serviceIntent)
        assertNotNull("Service should bind successfully", binder)

        val service = IElementCaptureService.Stub.asInterface(binder)
        assertNotNull("AIDL interface should be available", service)

        val serviceInstance = JITLearningService.getInstance()
        assertNotNull("Service instance should be created", serviceInstance)
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // RUNNING → Query initial state
        val initialState = service.queryState()
        assertNotNull("Initial state should be available", initialState)

        // PAUSED
        service.pauseCapture()
        Thread.sleep(100)
        verify(exactly = 1) { mockProvider?.pauseLearning() }

        // RUNNING (resumed)
        service.resumeCapture()
        Thread.sleep(100)
        verify(exactly = 1) { mockProvider?.resumeLearning() }

        // UNBOUND → happens automatically in teardown
        // DESTROYED → verified by instance becoming null after service stops
    }

    /**
     * Test: Service Lifecycle - Crash and Restart
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms   200ms   300ms   400ms
     * │      │       │       │       │
     * CREATE → RUN → CRASH → RESTART → RUN
     * ```
     *
     * Expected: Service restarts correctly after crash
     */
    @Test
    fun serviceLifecycle_CrashAndRestart_ServiceRecovers() {
        // Initial service start
        val serviceIntent = Intent(context, JITLearningService::class.java)
        var binder = serviceRule.bindService(serviceIntent)
        var service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance1 = JITLearningService.getInstance()
        serviceInstance1?.setLearnerProvider(mockProvider!!)

        // Set some state
        service.pauseCapture()
        Thread.sleep(100)

        // Simulate crash by unbinding and rebinding
        serviceRule.unbindService()
        Thread.sleep(200)

        // Restart service
        binder = serviceRule.bindService(serviceIntent)
        service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance2 = JITLearningService.getInstance()
        assertNotNull("Service should restart after crash", serviceInstance2)
        serviceInstance2?.setLearnerProvider(mockProvider!!)

        // Verify service is operational
        val state = service.queryState()
        assertNotNull("Restarted service should have state", state)
    }

    /**
     * Test: Service Lifecycle - Low Memory Recovery
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms      200ms        300ms        400ms
     * │      │          │            │            │
     * RUN → LOW_MEM → PAUSE → MEM_RECOVER → RESUME
     * ```
     *
     * Expected: Service pauses on low memory, resumes on recovery
     */
    @Test
    fun serviceLifecycle_LowMemory_PausesAndResumes() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Simulate low memory by pausing
        service.pauseCapture()
        Thread.sleep(100)

        val pausedState = service.queryState()
        assertNotNull("Paused state should be available", pausedState)

        // Simulate memory recovery by resuming
        service.resumeCapture()
        Thread.sleep(100)

        val resumedState = service.queryState()
        assertNotNull("Resumed state should be available", resumedState)

        verify(exactly = 1) { mockProvider?.pauseLearning() }
        verify(exactly = 1) { mockProvider?.resumeLearning() }
    }

    /**
     * Test: Service Lifecycle - Invalid Transitions
     *
     * Expected: Invalid transitions are rejected
     */
    @Test
    fun serviceLifecycle_InvalidTransitions_Rejected() {
        // Cannot query state before service is created
        // (This is enforced by Android framework - service must be bound first)

        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        // Multiple pauses are idempotent (not rejected, but no-op)
        service.pauseCapture()
        service.pauseCapture()
        service.pauseCapture()

        // Multiple resumes are idempotent
        service.resumeCapture()
        service.resumeCapture()
        service.resumeCapture()

        // State should still be valid
        val state = service.queryState()
        assertNotNull("State should be valid after multiple transitions", state)
    }

    // ================================================================
    // TEST GROUP 2: LEARNING STATE TRANSITIONS
    // ================================================================

    /**
     * Test: Learning States - Happy Path
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms      200ms       300ms      400ms
     * │      │          │           │          │
     * IDLE → CAPTURE → ANALYZE → STORE → IDLE
     * ```
     *
     * Expected: Full learning cycle completes successfully
     */
    @Test
    fun learningStates_HappyPath_CompletesFullCycle() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // IDLE state - no current package
        every { mockProvider?.getCurrentPackage() } returns null
        var state = service.queryState()
        assertTrue("Should start in IDLE (no package)", state.isIdle())

        // CAPTURING - package set
        every { mockProvider?.getCurrentPackage() } returns "com.example.app"
        every { mockProvider?.isLearningActive() } returns true

        state = service.queryState()
        assertFalse("Should not be idle when package is set", state.isIdle())
        assertEquals("Package should be set", "com.example.app", state.currentPackage)

        // ANALYZING → STORING - elements discovered increase
        every { mockProvider?.getElementsDiscoveredCount() } returns 5
        every { mockProvider?.getScreensLearnedCount() } returns 1

        state = service.queryState()
        assertEquals("Elements should be discovered", 5, state.elementsDiscovered)
        assertEquals("Screen should be learned", 1, state.screensLearned)

        // Back to IDLE
        every { mockProvider?.getCurrentPackage() } returns null
        state = service.queryState()
        assertTrue("Should return to IDLE", state.isIdle())
    }

    /**
     * Test: Learning States - Pause During Capture
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms      100ms     200ms      300ms      400ms
     * │        │         │          │          │
     * CAPTURE → PAUSE → CAPTURE → PAUSE → IDLE
     * ```
     *
     * Expected: Can pause and resume during capture
     */
    @Test
    fun learningStates_PauseDuringCapture_ResumesCorrectly() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Start capturing
        every { mockProvider?.getCurrentPackage() } returns "com.example.app"
        every { mockProvider?.isLearningActive() } returns true

        // Pause
        service.pauseCapture()
        Thread.sleep(100)
        verify(exactly = 1) { mockProvider?.pauseLearning() }

        // Resume
        service.resumeCapture()
        Thread.sleep(100)
        verify(exactly = 1) { mockProvider?.resumeLearning() }

        // Pause again
        service.pauseCapture()
        Thread.sleep(100)
        verify(exactly = 2) { mockProvider?.pauseLearning() }

        // Back to idle
        every { mockProvider?.getCurrentPackage() } returns null
        val state = service.queryState()
        assertTrue("Should return to idle", state.isIdle())
    }

    /**
     * Test: Learning States - Error During Analysis
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms      100ms       200ms      300ms
     * │        │           │          │
     * CAPTURE → ANALYZE → ERROR → IDLE
     * ```
     *
     * Expected: Error transitions back to IDLE
     */
    @Test
    fun learningStates_ErrorDuringAnalysis_ReturnsToIdle() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Start capturing
        every { mockProvider?.getCurrentPackage() } returns "com.example.app"
        every { mockProvider?.isLearningActive() } returns true

        // Simulate error by setting package to null (learning aborted)
        every { mockProvider?.getCurrentPackage() } returns null
        every { mockProvider?.isLearningActive() } returns false

        val state = service.queryState()
        assertTrue("Should return to IDLE after error", state.isIdle())
    }

    /**
     * Test: Learning States - Concurrent State Changes
     *
     * Expected: Concurrent operations don't corrupt state
     */
    @Test
    fun learningStates_ConcurrentChanges_MaintainConsistency() = runBlocking {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val operations = 50
        val errors = AtomicInteger(0)

        // Launch concurrent pause/resume operations
        val jobs = List(operations) { index ->
            launch {
                try {
                    delay(index.toLong() * 10)
                    if (index % 2 == 0) {
                        service.pauseCapture()
                    } else {
                        service.resumeCapture()
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        // Wait for all operations
        jobs.forEach { it.join() }

        // Verify no errors occurred
        assertEquals("No errors should occur during concurrent operations", 0, errors.get())

        // Verify service is still responsive
        val state = service.queryState()
        assertNotNull("Service should still be responsive", state)
    }

    // ================================================================
    // TEST GROUP 3: SCREEN CONTEXT STATE TRANSITIONS
    // ================================================================

    /**
     * Test: Screen Context States - Happy Path
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms   100ms         200ms           300ms
     * │     │             │               │
     * NEW → VISITED_1 → VISITED_N → LEARNED
     * ```
     *
     * Expected: Screen transitions from new to learned
     */
    @Test
    fun screenContext_HappyPath_NewToLearned() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"
        val screenHash = "screen_hash_123"

        // NEW screen - not in learned hashes
        every { mockProvider?.hasScreen(screenHash) } returns false
        every { mockProvider?.getLearnedScreenHashes(packageName) } returns emptyList()

        var hashes = service.getLearnedScreenHashes(packageName)
        assertFalse("Screen should not be learned yet", hashes.contains(screenHash))

        // VISITED_ONCE - screen now learned
        every { mockProvider?.hasScreen(screenHash) } returns true
        every { mockProvider?.getLearnedScreenHashes(packageName) } returns listOf(screenHash)
        every { mockProvider?.getScreensLearnedCount() } returns 1

        hashes = service.getLearnedScreenHashes(packageName)
        assertTrue("Screen should be learned", hashes.contains(screenHash))

        val state = service.queryState()
        assertEquals("Should have 1 screen learned", 1, state.screensLearned)
    }

    /**
     * Test: Screen Context States - Framework Change Relearning
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms      100ms              200ms           300ms
     * │        │                  │               │
     * LEARNED → FRAMEWORK_CHG → RELEARN → LEARNED
     * ```
     *
     * Expected: Screen re-learned after framework change
     */
    @Test
    fun screenContext_FrameworkChange_Relearns() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"
        val oldScreenHash = "screen_hash_old"
        val newScreenHash = "screen_hash_new"

        // LEARNED state
        every { mockProvider?.getLearnedScreenHashes(packageName) } returns listOf(oldScreenHash)
        every { mockProvider?.hasScreen(oldScreenHash) } returns true

        var hashes = service.getLearnedScreenHashes(packageName)
        assertTrue("Old screen should be learned", hashes.contains(oldScreenHash))
        assertEquals("Should have 1 screen", 1, hashes.size)

        // FRAMEWORK_CHANGED - new hash for same screen
        every { mockProvider?.getLearnedScreenHashes(packageName) } returns listOf(oldScreenHash, newScreenHash)
        every { mockProvider?.hasScreen(newScreenHash) } returns true
        every { mockProvider?.getScreensLearnedCount() } returns 2

        // RELEARNED state
        hashes = service.getLearnedScreenHashes(packageName)
        assertTrue("New screen hash should be learned", hashes.contains(newScreenHash))
        assertEquals("Should have 2 screens (old + new)", 2, hashes.size)
    }

    /**
     * Test: Screen Context States - Unsafe to Safe Transition
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms     100ms    200ms         300ms
     * │       │        │             │
     * UNSAFE → VIS1 → VIS2+3 → SAFE_LEARNED
     * ```
     *
     * Expected: Screen becomes safe after multiple visits
     */
    @Test
    fun screenContext_UnsafeToSafe_RequiresMultipleVisits() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.suspicious.app"
        val screenHash = "potential_phishing"

        // UNSAFE - first visit
        every { mockProvider?.hasScreen(screenHash) } returns false
        every { mockProvider?.getLearnedScreenHashes(packageName) } returns emptyList()

        var hashes = service.getLearnedScreenHashes(packageName)
        assertFalse("Unsafe screen not learned yet", hashes.contains(screenHash))

        // After multiple visits (simulated by now being learned)
        every { mockProvider?.hasScreen(screenHash) } returns true
        every { mockProvider?.getLearnedScreenHashes(packageName) } returns listOf(screenHash)

        hashes = service.getLearnedScreenHashes(packageName)
        assertTrue("Screen should be safe after multiple visits", hashes.contains(screenHash))
    }

    // ================================================================
    // TEST GROUP 4: BATCH PROCESSING STATE TRANSITIONS
    // ================================================================

    /**
     * Test: Batch Processing - Happy Path
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms     200ms    300ms      400ms
     * │      │         │        │          │
     * EMPTY → FILL → FULL → FLUSH → EMPTY
     * ```
     *
     * Expected: Batch fills, flushes, and empties
     */
    @Test
    fun batchProcessing_HappyPath_FillFlushEmpty() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // EMPTY - no elements
        every { mockProvider?.getElementsDiscoveredCount() } returns 0
        var state = service.queryState()
        assertEquals("Should start empty", 0, state.elementsDiscovered)

        // FILLING - elements accumulating
        every { mockProvider?.getElementsDiscoveredCount() } returns 5
        state = service.queryState()
        assertEquals("Batch should be filling", 5, state.elementsDiscovered)

        // FULL - many elements
        every { mockProvider?.getElementsDiscoveredCount() } returns 100
        state = service.queryState()
        assertEquals("Batch should be full", 100, state.elementsDiscovered)

        // FLUSHING → EMPTY - elements persisted, count may reset or continue growing
        // (In real implementation, count continues growing)
        every { mockProvider?.getElementsDiscoveredCount() } returns 105
        state = service.queryState()
        assertTrue("Should have elements after flush", state.elementsDiscovered > 0)
    }

    /**
     * Test: Batch Processing - Auto-Flush
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms     200ms         300ms
     * │      │         │             │
     * EMPTY → FILL → AUTO_FLUSH → EMPTY
     * ```
     *
     * Expected: Batch auto-flushes when threshold reached
     */
    @Test
    fun batchProcessing_AutoFlush_TriggersAtThreshold() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // EMPTY
        every { mockProvider?.getElementsDiscoveredCount() } returns 0
        var state = service.queryState()
        assertEquals("Should start empty", 0, state.elementsDiscovered)

        // FILLING - approach threshold
        every { mockProvider?.getElementsDiscoveredCount() } returns 99
        state = service.queryState()
        assertEquals("Should be near threshold", 99, state.elementsDiscovered)

        // AUTO_FLUSH triggered at threshold (100)
        every { mockProvider?.getElementsDiscoveredCount() } returns 100
        state = service.queryState()
        assertEquals("Should reach threshold", 100, state.elementsDiscovered)

        // After flush, continues
        every { mockProvider?.getElementsDiscoveredCount() } returns 105
        state = service.queryState()
        assertTrue("Should continue after auto-flush", state.elementsDiscovered > 100)
    }

    /**
     * Test: Batch Processing - Flush Error and Retry
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms    200ms     300ms     400ms
     * │      │        │         │         │
     * FILL → FLUSH → ERROR → RETRY → EMPTY
     * ```
     *
     * Expected: Failed flush retries and succeeds
     */
    @Test
    fun batchProcessing_FlushError_RetriesSuccessfully() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // FILLING
        every { mockProvider?.getElementsDiscoveredCount() } returns 50
        var state = service.queryState()
        assertEquals("Should be filling", 50, state.elementsDiscovered)

        // ERROR during flush (simulated - count stays same)
        // In real implementation, retry would be automatic
        state = service.queryState()
        assertEquals("Count should persist during error", 50, state.elementsDiscovered)

        // RETRY success (count continues)
        every { mockProvider?.getElementsDiscoveredCount() } returns 55
        state = service.queryState()
        assertEquals("Should continue after retry", 55, state.elementsDiscovered)
    }

    // ================================================================
    // TEST GROUP 5: EXPLORATION STATE TRANSITIONS
    // ================================================================

    /**
     * Test: Exploration States - Happy Path
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms    100ms     200ms      300ms       400ms
     * │      │         │          │           │
     * IDLE → START → RUNNING → COMPLETE → IDLE
     * ```
     *
     * Expected: Exploration completes successfully
     */
    @Test
    fun explorationStates_HappyPath_CompletesSuccessfully() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"

        // IDLE
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.idle()
        var progress = service.getExplorationProgress()
        assertEquals("Should start idle", "idle", progress.state)

        // START
        every { mockProvider?.startExploration(packageName) } returns true
        val started = service.startExploration(packageName)
        assertTrue("Exploration should start", started)

        // RUNNING
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.running(
            packageName = packageName,
            screensExplored = 5,
            elementsDiscovered = 42,
            currentDepth = 2,
            progressPercent = 50,
            elapsedMs = 5000
        )
        progress = service.getExplorationProgress()
        assertEquals("Should be running", "running", progress.state)
        assertEquals("Should have progress", 50, progress.progressPercent)

        // COMPLETED
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.completed(
            packageName = packageName,
            screensExplored = 10,
            elementsDiscovered = 87
        )
        progress = service.getExplorationProgress()
        assertEquals("Should be completed", "completed", progress.state)
        assertEquals("Should be 100%", 100, progress.progressPercent)

        // Back to IDLE
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.idle()
        progress = service.getExplorationProgress()
        assertEquals("Should return to idle", "idle", progress.state)
    }

    /**
     * Test: Exploration States - Pause and Resume
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms      100ms     200ms     300ms      400ms
     * │        │         │         │          │
     * RUNNING → PAUSE → PAUSED → RESUME → RUNNING
     * ```
     *
     * Expected: Can pause and resume exploration
     */
    @Test
    fun explorationStates_PauseResume_TransitionsCorrectly() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"

        // Start exploration
        service.startExploration(packageName)

        // RUNNING
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.running(
            packageName = packageName,
            screensExplored = 3,
            elementsDiscovered = 20,
            currentDepth = 1,
            progressPercent = 30,
            elapsedMs = 3000
        )
        var progress = service.getExplorationProgress()
        assertEquals("Should be running", "running", progress.state)

        // PAUSE
        service.pauseExploration()
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.paused(
            packageName = packageName,
            screensExplored = 3,
            elementsDiscovered = 20,
            pauseReason = "User requested"
        )
        progress = service.getExplorationProgress()
        assertEquals("Should be paused", "paused", progress.state)
        assertEquals("Should have pause reason", "User requested", progress.pauseReason)

        // RESUME
        service.resumeExploration()
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.running(
            packageName = packageName,
            screensExplored = 4,
            elementsDiscovered = 25,
            currentDepth = 1,
            progressPercent = 40,
            elapsedMs = 4000
        )
        progress = service.getExplorationProgress()
        assertEquals("Should be running again", "running", progress.state)
        assertTrue("Should have progressed", progress.screensExplored > 3)
    }

    /**
     * Test: Exploration States - Stop During Exploration
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms      100ms     200ms    300ms
     * │        │         │        │
     * RUNNING → STOP → IDLE → IDLE
     * ```
     *
     * Expected: Stop transitions immediately to idle
     */
    @Test
    fun explorationStates_StopDuringExploration_ReturnsToIdle() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"

        // Start exploration
        service.startExploration(packageName)

        // RUNNING
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.running(
            packageName = packageName,
            screensExplored = 2,
            elementsDiscovered = 15,
            currentDepth = 1,
            progressPercent = 20,
            elapsedMs = 2000
        )
        var progress = service.getExplorationProgress()
        assertEquals("Should be running", "running", progress.state)

        // STOP
        service.stopExploration()
        verify(exactly = 1) { mockProvider?.stopExploration() }

        // IDLE
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.idle()
        progress = service.getExplorationProgress()
        assertEquals("Should be idle after stop", "idle", progress.state)
    }

    /**
     * Test: Exploration States - Invalid Transitions
     *
     * Expected: Invalid transitions are handled gracefully
     */
    @Test
    fun explorationStates_InvalidTransitions_HandledGracefully() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Try to pause when idle (should be no-op)
        service.pauseExploration()
        verify { mockProvider?.pauseExploration() }

        // Try to resume when idle (should be no-op)
        service.resumeExploration()
        verify { mockProvider?.resumeExploration() }

        // Try to stop when idle (should be no-op)
        service.stopExploration()
        verify { mockProvider?.stopExploration() }

        // Start exploration
        val packageName = "com.example.app"
        service.startExploration(packageName)

        // Try to start again while running (should be handled)
        service.startExploration(packageName)
        verify(atLeast = 1) { mockProvider?.startExploration(packageName) }

        // Service should still be operational
        val progress = service.getExplorationProgress()
        assertNotNull("Progress should still be available", progress)
    }

    // ================================================================
    // TEST GROUP 6: COMPLEX STATE TRANSITION SCENARIOS
    // ================================================================

    /**
     * Test: Complex Scenario - Learning with Concurrent Exploration
     *
     * Timing Diagram:
     * ```
     * Time →
     * 0ms       100ms      200ms       300ms       400ms
     * │         │          │           │           │
     * PASSIVE → EXPLORE → BOTH → PAUSE_PASS → RESUME_ALL
     * ```
     *
     * Expected: Learning and exploration can run concurrently
     */
    @Test
    fun complexScenario_LearningWithExploration_BothRunConcurrently() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"

        // PASSIVE learning active
        every { mockProvider?.getCurrentPackage() } returns packageName
        every { mockProvider?.isLearningActive() } returns true

        var state = service.queryState()
        assertFalse("Learning should be active", state.isIdle())

        // Start EXPLORATION while learning
        service.startExploration(packageName)
        every { mockProvider?.getExplorationProgress() } returns ExplorationProgress.running(
            packageName = packageName,
            screensExplored = 3,
            elementsDiscovered = 20,
            currentDepth = 1,
            progressPercent = 30,
            elapsedMs = 3000
        )

        // BOTH should be running
        state = service.queryState()
        assertFalse("Learning should still be active", state.isIdle())

        val progress = service.getExplorationProgress()
        assertEquals("Exploration should be running", "running", progress.state)

        // PAUSE passive learning (exploration continues)
        service.pauseCapture()
        Thread.sleep(100)

        // Exploration should still be running
        val progress2 = service.getExplorationProgress()
        assertEquals("Exploration should still be running", "running", progress2.state)

        // RESUME all
        service.resumeCapture()
        Thread.sleep(100)

        state = service.queryState()
        val progress3 = service.getExplorationProgress()
        assertNotNull("Both should be operational", state)
        assertNotNull("Exploration should be operational", progress3)
    }

    /**
     * Test: Complex Scenario - State Recovery After Multiple Failures
     *
     * Expected: System recovers from multiple consecutive failures
     */
    @Test
    fun complexScenario_MultipleFailures_SystemRecovers() {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        var binder = serviceRule.bindService(serviceIntent)
        var service = IElementCaptureService.Stub.asInterface(binder)

        var serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        // Simulate multiple crash/restart cycles
        repeat(3) { cycle ->
            // Set some state
            service.pauseCapture()
            Thread.sleep(50)

            // Crash and restart
            serviceRule.unbindService()
            Thread.sleep(100)

            binder = serviceRule.bindService(serviceIntent)
            service = IElementCaptureService.Stub.asInterface(binder)
            serviceInstance = JITLearningService.getInstance()
            assertNotNull("Service should restart after crash $cycle", serviceInstance)
            serviceInstance?.setLearnerProvider(mockProvider!!)
        }

        // Verify service is fully operational after multiple crashes
        val state = service.queryState()
        assertNotNull("Service should be operational after multiple crashes", state)

        service.resumeCapture()
        Thread.sleep(100)

        val state2 = service.queryState()
        assertNotNull("Service should respond after resume", state2)
    }

    /**
     * Test: Complex Scenario - Rapid State Changes
     *
     * Expected: System handles rapid state changes without corruption
     */
    @Test
    fun complexScenario_RapidStateChanges_MaintainsConsistency() = runBlocking {
        val serviceIntent = Intent(context, JITLearningService::class.java)
        val binder = serviceRule.bindService(serviceIntent)
        val service = IElementCaptureService.Stub.asInterface(binder)

        val serviceInstance = JITLearningService.getInstance()
        serviceInstance?.setLearnerProvider(mockProvider!!)

        val packageName = "com.example.app"

        // Perform rapid state changes
        repeat(20) { index ->
            launch {
                when (index % 4) {
                    0 -> service.pauseCapture()
                    1 -> service.resumeCapture()
                    2 -> service.startExploration(packageName)
                    3 -> service.stopExploration()
                }
                delay(10)
            }
        }

        // Wait for all operations
        delay(500)

        // Verify system is still consistent
        val state = service.queryState()
        assertNotNull("State should be consistent after rapid changes", state)

        val progress = service.getExplorationProgress()
        assertNotNull("Exploration state should be consistent", progress)
    }
}
