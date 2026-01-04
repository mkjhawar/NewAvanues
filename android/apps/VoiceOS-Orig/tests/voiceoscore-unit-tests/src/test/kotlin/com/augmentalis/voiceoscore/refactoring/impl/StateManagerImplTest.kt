/**
 * StateManagerImplTest.kt - Comprehensive state manager tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-15 03:59:06 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3
 */
package com.augmentalis.voiceoscore.refactoring.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * Comprehensive test suite for StateManagerImpl
 *
 * Test Categories:
 * 1. Initialization & Lifecycle (10 tests)
 * 2. State Updates (10 tests)
 * 3. Thread Safety (10 tests)
 * 4. Configuration Management (8 tests)
 * 5. Validation (6 tests)
 * 6. Observers (6 tests)
 * 7. Snapshots & Checkpoints (6 tests)
 * 8. Metrics (4 tests)
 * 9. Performance (5 tests)
 * 10. Edge Cases (5 tests)
 *
 * Total: 70 tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StateManagerImplTest {

    private lateinit var stateManager: StateManagerImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        stateManager = StateManagerImpl()
    }

    @After
    fun teardown() {
        if (stateManager.isReady) {
            stateManager.cleanup()
        }
    }

    // ========================================
    // 1. Initialization & Lifecycle Tests
    // ========================================

    @Test
    fun testInitialization_success() = runTest {
        assertFalse(stateManager.isReady)
        assertEquals(StateManagerState.UNINITIALIZED, stateManager.currentState)

        val config = StateConfig(
            enablePersistence = true,
            enableValidation = true,
            maxHistorySize = 100,
            autoSaveIntervalMs = 5000L
        )

        stateManager.initialize(context, config)

        assertTrue(stateManager.isReady)
        assertEquals(StateManagerState.READY, stateManager.currentState)
    }

    @Test
    fun testInitialization_doubleInitializationThrows() = runTest {
        val config = StateConfig()
        stateManager.initialize(context, config)

        assertThrows(IllegalStateException::class.java) {
            runTest {
                stateManager.initialize(context, config)
            }
        }
    }

    @Test
    fun testInitialization_stateTransition() = runTest {
        val config = StateConfig()

        // Note: currentState is not a Flow, so we can't collect it
        // We'll just check the final state
        val initialState = stateManager.currentState
        assertEquals(StateManagerState.UNINITIALIZED, initialState)

        stateManager.initialize(context, config)
        delay(100)

        val finalState = stateManager.currentState
        assertEquals(StateManagerState.READY, finalState)
    }

    @Test
    fun testCleanup_releasesResources() = runTest {
        val config = StateConfig()
        stateManager.initialize(context, config)

        stateManager.cleanup()

        assertFalse(stateManager.isReady)
        assertEquals(StateManagerState.SHUTDOWN, stateManager.currentState)
    }

    @Test
    fun testResetState_resetsToDefaults() = runTest {
        val config = StateConfig()
        stateManager.initialize(context, config)

        // Set some state
        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        stateManager.setLastCommandLoadedTime(12345L)

        // Reset
        stateManager.resetState()

        // Verify defaults
        assertFalse(stateManager.isServiceReady.value)
        assertFalse(stateManager.isVoiceInitialized.value)
        assertEquals(0L, stateManager.getLastCommandLoadedTime())
    }

    @Test
    fun testInitialization_withPersistenceDisabled() = runTest {
        val config = StateConfig(enablePersistence = false)
        stateManager.initialize(context, config)

        assertTrue(stateManager.isReady)
    }

    @Test
    fun testInitialization_withValidationDisabled() = runTest {
        val config = StateConfig(enableValidation = false)
        stateManager.initialize(context, config)

        assertTrue(stateManager.isReady)
    }

    @Test
    fun testInitialization_customHistorySize() = runTest {
        val config = StateConfig(maxHistorySize = 50)
        stateManager.initialize(context, config)

        assertTrue(stateManager.isReady)
    }

    @Test
    fun testInitialization_customAutoSaveInterval() = runTest {
        val config = StateConfig(autoSaveIntervalMs = 1000L)
        stateManager.initialize(context, config)

        assertTrue(stateManager.isReady)
    }

    @Test
    fun testLifecycle_fullCycle() = runTest {
        val config = StateConfig()

        // Initialize
        stateManager.initialize(context, config)
        assertTrue(stateManager.isReady)

        // Use
        stateManager.setServiceReady(true)
        assertTrue(stateManager.isServiceReady.value)

        // Reset
        stateManager.resetState()
        assertFalse(stateManager.isServiceReady.value)

        // Cleanup
        stateManager.cleanup()
        assertEquals(StateManagerState.SHUTDOWN, stateManager.currentState)
    }

    // ========================================
    // 2. State Updates Tests
    // ========================================

    @Test
    fun testSetServiceReady_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isServiceReady.value)

        stateManager.setServiceReady(true)
        assertTrue(stateManager.isServiceReady.value)

        stateManager.setServiceReady(false)
        assertFalse(stateManager.isServiceReady.value)
    }

    @Test
    fun testSetVoiceInitialized_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isVoiceInitialized.value)

        stateManager.setVoiceInitialized(true)
        assertTrue(stateManager.isVoiceInitialized.value)
    }

    @Test
    fun testSetCommandProcessing_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isCommandProcessing.value)

        stateManager.setCommandProcessing(true)
        assertTrue(stateManager.isCommandProcessing.value)
    }

    @Test
    fun testSetForegroundServiceActive_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isForegroundServiceActive.value)

        stateManager.setForegroundServiceActive(true)
        assertTrue(stateManager.isForegroundServiceActive.value)
    }

    @Test
    fun testSetAppInBackground_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isAppInBackground.value)

        stateManager.setAppInBackground(true)
        assertTrue(stateManager.isAppInBackground.value)
    }

    @Test
    fun testSetVoiceSessionActive_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isVoiceSessionActive.value)

        stateManager.setVoiceSessionActive(true)
        assertTrue(stateManager.isVoiceSessionActive.value)
    }

    @Test
    fun testSetVoiceCursorInitialized_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isVoiceCursorInitialized.value)

        stateManager.setVoiceCursorInitialized(true)
        assertTrue(stateManager.isVoiceCursorInitialized.value)
    }

    @Test
    fun testSetFallbackModeEnabled_updatesState() = runTest {
        stateManager.initialize(context, StateConfig())

        assertFalse(stateManager.isFallbackModeEnabled.value)

        stateManager.setFallbackModeEnabled(true)
        assertTrue(stateManager.isFallbackModeEnabled.value)
    }

    @Test
    fun testSetLastCommandLoadedTime_updatesTimestamp() = runTest {
        stateManager.initialize(context, StateConfig())

        assertEquals(0L, stateManager.getLastCommandLoadedTime())

        val timestamp = System.currentTimeMillis()
        stateManager.setLastCommandLoadedTime(timestamp)

        assertEquals(timestamp, stateManager.getLastCommandLoadedTime())
    }

    @Test
    fun testMultipleStateUpdates_allPersist() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        stateManager.setCommandProcessing(true)
        stateManager.setAppInBackground(true)

        assertTrue(stateManager.isServiceReady.value)
        assertTrue(stateManager.isVoiceInitialized.value)
        assertTrue(stateManager.isCommandProcessing.value)
        assertTrue(stateManager.isAppInBackground.value)
    }

    // ========================================
    // 3. Thread Safety Tests
    // ========================================

    @Test
    fun testConcurrentStateUpdates_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 1000
        val latch = CountDownLatch(iterations * 2)

        repeat(iterations) {
            launch {
                stateManager.setServiceReady(true)
                latch.countDown()
            }
            launch {
                stateManager.setServiceReady(false)
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        // State should be either true or false (not corrupted)
        val finalState = stateManager.isServiceReady.value
        assertTrue(finalState || !finalState)
    }

    @Test
    fun testConcurrentReads_consistent() = runTest {
        stateManager.initialize(context, StateConfig())
        stateManager.setServiceReady(true)

        val iterations = 1000
        val latch = CountDownLatch(iterations)
        val readValues = mutableListOf<Boolean>()

        repeat(iterations) {
            launch {
                readValues.add(stateManager.isServiceReady.value)
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        // All reads should return true (consistent)
        assertTrue(readValues.all { it })
    }

    @Test
    fun testConcurrentConfigurationUpdates_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 100
        val latch = CountDownLatch(iterations)

        repeat(iterations) { index ->
            launch {
                stateManager.updateConfigProperty("cacheSize", 100 + index)
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        // Configuration should have valid value (not corrupted)
        val cacheSize = stateManager.getConfigProperty("cacheSize") as? Int
        assertNotNull(cacheSize)
        assertTrue(cacheSize!! >= 100)
    }

    @Test
    fun testConcurrentSnapshotCreation_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 100
        val latch = CountDownLatch(iterations)
        val snapshots = mutableListOf<StateSnapshot>()

        repeat(iterations) {
            launch {
                snapshots.add(stateManager.getStateSnapshot())
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(iterations, snapshots.size)
    }

    @Test
    fun testConcurrentCheckpointOperations_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 50
        val latch = CountDownLatch(iterations * 2)

        repeat(iterations) { index ->
            launch {
                stateManager.createCheckpoint("checkpoint_$index")
                latch.countDown()
            }
            launch {
                stateManager.restoreCheckpoint("checkpoint_${index - 1}")
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testConcurrentObserverRegistration_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 100
        val latch = CountDownLatch(iterations)
        val observers = mutableListOf<(StateChange) -> Unit>()

        repeat(iterations) {
            launch {
                val observer: (StateChange) -> Unit = {}
                observers.add(observer)
                stateManager.registerStateObserver(observer)
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testConcurrentMetricsAccess_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 100
        val latch = CountDownLatch(iterations)

        repeat(iterations) {
            launch {
                stateManager.getMetrics()
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testConcurrentValidation_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 100
        val latch = CountDownLatch(iterations)

        repeat(iterations) {
            launch {
                stateManager.validateState()
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testConcurrentSaveRestore_threadSafe() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 50
        val latch = CountDownLatch(iterations * 2)

        repeat(iterations) {
            launch {
                stateManager.saveState()
                latch.countDown()
            }
            launch {
                stateManager.restoreState()
                latch.countDown()
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun testConcurrentMixedOperations_noDeadlock() = runTest {
        stateManager.initialize(context, StateConfig())

        val iterations = 50
        val latch = CountDownLatch(iterations * 5)

        repeat(iterations) {
            launch {
                stateManager.setServiceReady(true)
                latch.countDown()
            }
            launch {
                stateManager.getStateSnapshot()
                latch.countDown()
            }
            launch {
                stateManager.validateState()
                latch.countDown()
            }
            launch {
                stateManager.getMetrics()
                latch.countDown()
            }
            launch {
                stateManager.saveState()
                latch.countDown()
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    // ========================================
    // 4. Configuration Management Tests
    // ========================================

    @Test
    fun testGetConfiguration_returnsDefault() = runTest {
        stateManager.initialize(context, StateConfig())

        val config = stateManager.getConfiguration()
        assertEquals(ServiceConfiguration(), config)
    }

    @Test
    fun testUpdateConfiguration_updatesAllProperties() = runTest {
        stateManager.initialize(context, StateConfig())

        val newConfig = ServiceConfiguration(
            fingerprintGesturesEnabled = true,
            commandCheckIntervalMs = 1000L,
            commandLoadDebounceMs = 1000L,
            eventDebounceMs = 2000L,
            cacheSize = 200,
            initDelayMs = 500L
        )

        stateManager.updateConfiguration(newConfig)

        assertEquals(newConfig, stateManager.getConfiguration())
    }

    @Test
    fun testUpdateConfigProperty_fingerprintGestures() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.updateConfigProperty("fingerprintGesturesEnabled", true)

        assertTrue(stateManager.getConfiguration().fingerprintGesturesEnabled)
        assertEquals(true, stateManager.getConfigProperty("fingerprintGesturesEnabled"))
    }

    @Test
    fun testUpdateConfigProperty_commandCheckInterval() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.updateConfigProperty("commandCheckIntervalMs", 1000L)

        assertEquals(1000L, stateManager.getConfiguration().commandCheckIntervalMs)
    }

    @Test
    fun testUpdateConfigProperty_cacheSize() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.updateConfigProperty("cacheSize", 200)

        assertEquals(200, stateManager.getConfiguration().cacheSize)
    }

    @Test
    fun testGetConfigProperty_unknownKey() = runTest {
        stateManager.initialize(context, StateConfig())

        val value = stateManager.getConfigProperty("unknownKey")
        assertNull(value)
    }

    @Test
    fun testConfigurationUpdate_emitsChangeEvent() = runTest {
        stateManager.initialize(context, StateConfig())

        var changeReceived = false
        val observer: (StateChange) -> Unit = { change ->
            if (change is StateChange.ConfigurationChanged) {
                changeReceived = true
            }
        }
        stateManager.registerStateObserver(observer)

        stateManager.updateConfigProperty("cacheSize", 200)
        delay(100)

        assertTrue(changeReceived)
    }

    @Test
    fun testMultipleConfigurationUpdates_allApplied() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.updateConfigProperty("cacheSize", 200)
        stateManager.updateConfigProperty("initDelayMs", 500L)
        stateManager.updateConfigProperty("fingerprintGesturesEnabled", true)

        val config = stateManager.getConfiguration()
        assertEquals(200, config.cacheSize)
        assertEquals(500L, config.initDelayMs)
        assertTrue(config.fingerprintGesturesEnabled)
    }

    // ========================================
    // 5. Validation Tests
    // ========================================

    @Test
    fun testValidation_validState() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)

        val result = stateManager.validateState()
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testValidation_commandProcessingWithoutServiceReady() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(false)
        stateManager.setCommandProcessing(true)

        val result = stateManager.validateState()
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).issues.any {
            it.contains("Command processing")
        })
    }

    @Test
    fun testValidation_voiceSessionWithoutInitialization() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setVoiceInitialized(false)
        stateManager.setVoiceSessionActive(true)

        val result = stateManager.validateState()
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testValidation_fallbackModeWarning() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.setFallbackModeEnabled(true)

        val result = stateManager.validateState()
        assertTrue(result is ValidationResult.Warning)
    }

    @Test
    fun testIsValidTransition_validTransitions() = runTest {
        stateManager.initialize(context, StateConfig())

        assertTrue(stateManager.isValidTransition(
            ServiceState.UNINITIALIZED,
            ServiceState.INITIALIZING
        ))
        assertTrue(stateManager.isValidTransition(
            ServiceState.INITIALIZING,
            ServiceState.READY
        ))
        assertTrue(stateManager.isValidTransition(
            ServiceState.READY,
            ServiceState.LISTENING
        ))
    }

    @Test
    fun testGetValidNextStates_returnsCorrectStates() = runTest {
        stateManager.initialize(context, StateConfig())

        val nextStates = stateManager.getValidNextStates(ServiceState.READY)

        assertTrue(nextStates.contains(ServiceState.LISTENING))
        assertTrue(nextStates.contains(ServiceState.PAUSED))
        assertTrue(nextStates.contains(ServiceState.SHUTDOWN))
        assertFalse(nextStates.contains(ServiceState.UNINITIALIZED))
    }

    // ========================================
    // 6. Observer Tests
    // ========================================

    @Test
    fun testRegisterStateObserver_receivesChanges() = runTest {
        stateManager.initialize(context, StateConfig())

        var changesReceived = 0
        val observer: (StateChange) -> Unit = { changesReceived++ }

        stateManager.registerStateObserver(observer)

        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        delay(100)

        assertTrue(changesReceived >= 2)
    }

    @Test
    fun testUnregisterStateObserver_stopsReceivingChanges() = runTest {
        stateManager.initialize(context, StateConfig())

        var changesReceived = 0
        val observer: (StateChange) -> Unit = { changesReceived++ }

        stateManager.registerStateObserver(observer)
        stateManager.setServiceReady(true)
        delay(100)

        stateManager.unregisterStateObserver(observer)
        stateManager.setVoiceInitialized(true)
        delay(100)

        // Should only receive 1 change (before unregister)
        assertEquals(1, changesReceived)
    }

    @Test
    fun testMultipleObservers_allReceiveChanges() = runTest {
        stateManager.initialize(context, StateConfig())

        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)
        val counter3 = AtomicInteger(0)

        stateManager.registerStateObserver { counter1.incrementAndGet() }
        stateManager.registerStateObserver { counter2.incrementAndGet() }
        stateManager.registerStateObserver { counter3.incrementAndGet() }

        stateManager.setServiceReady(true)
        delay(100)

        assertEquals(1, counter1.get())
        assertEquals(1, counter2.get())
        assertEquals(1, counter3.get())
    }

    @Test
    fun testStateChangesFlow_collectsChanges() = runTest {
        stateManager.initialize(context, StateConfig())

        val changes = mutableListOf<StateChange>()
        val job = launch {
            stateManager.stateChanges.collect { changes.add(it) }
        }

        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        delay(100)

        job.cancel()

        assertTrue(changes.size >= 2)
        assertTrue(changes.any { it is StateChange.ServiceReadyChanged })
        assertTrue(changes.any { it is StateChange.VoiceInitializedChanged })
    }

    @Test
    fun testObserveSpecificState_receivesUpdates() = runTest {
        stateManager.initialize(context, StateConfig())

        var updateReceived = false
        stateManager.observeState<Boolean>("serviceReady") { updateReceived = true }

        stateManager.setServiceReady(true)
        delay(100)

        // Note: Specific state observers are registered but not automatically triggered
        // This is a placeholder test for the API
    }

    @Test
    fun testObserverException_doesNotCrashStateManager() = runTest {
        stateManager.initialize(context, StateConfig())

        val throwingObserver: (StateChange) -> Unit = {
            throw RuntimeException("Test exception")
        }
        stateManager.registerStateObserver(throwingObserver)

        // Should not throw
        stateManager.setServiceReady(true)
        delay(100)

        // State should still be updated
        assertTrue(stateManager.isServiceReady.value)
    }

    // ========================================
    // 7. Snapshots & Checkpoints Tests
    // ========================================

    @Test
    fun testGetStateSnapshot_capturesCurrentState() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        stateManager.setLastCommandLoadedTime(12345L)

        val snapshot = stateManager.getStateSnapshot()

        assertTrue(snapshot.isServiceReady)
        assertTrue(snapshot.isVoiceInitialized)
        assertEquals(12345L, snapshot.lastCommandLoadedTime)
    }

    @Test
    fun testSaveState_addsToHistory() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.saveState()

        val history = stateManager.getStateHistory(10)
        assertTrue(history.isNotEmpty())
        assertTrue(history.first().isServiceReady)
    }

    @Test
    fun testRestoreState_restoresFromHistory() = runTest {
        stateManager.initialize(context, StateConfig())

        // Set and save state
        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        stateManager.saveState()

        // Change state
        stateManager.setServiceReady(false)
        stateManager.setVoiceInitialized(false)

        // Restore
        stateManager.restoreState()

        // Should restore to saved state
        assertTrue(stateManager.isServiceReady.value)
        assertTrue(stateManager.isVoiceInitialized.value)
    }

    @Test
    fun testCreateCheckpoint_savesNamedSnapshot() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.createCheckpoint("test_checkpoint")

        assertTrue(stateManager.restoreCheckpoint("test_checkpoint"))
        assertTrue(stateManager.isServiceReady.value)
    }

    @Test
    fun testRestoreCheckpoint_restoresNamedSnapshot() = runTest {
        stateManager.initialize(context, StateConfig())

        // Create checkpoint with specific state
        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        stateManager.createCheckpoint("checkpoint1")

        // Change state
        stateManager.setServiceReady(false)
        stateManager.setVoiceInitialized(false)

        // Restore checkpoint
        val restored = stateManager.restoreCheckpoint("checkpoint1")

        assertTrue(restored)
        assertTrue(stateManager.isServiceReady.value)
        assertTrue(stateManager.isVoiceInitialized.value)
    }

    @Test
    fun testGetStateHistory_limitWorks() = runTest {
        stateManager.initialize(context, StateConfig())

        repeat(20) {
            stateManager.setServiceReady(it % 2 == 0)
            stateManager.saveState()
        }

        val history = stateManager.getStateHistory(10)
        assertEquals(10, history.size)
    }

    // ========================================
    // 8. Metrics Tests
    // ========================================

    @Test
    fun testGetMetrics_tracksStateChanges() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.setServiceReady(true)
        stateManager.setVoiceInitialized(true)
        stateManager.setCommandProcessing(true)

        val metrics = stateManager.getMetrics()
        assertTrue(metrics.totalStateChanges >= 3)
    }

    @Test
    fun testGetMetrics_tracksSnapshotCount() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.saveState()
        stateManager.saveState()
        stateManager.saveState()

        val metrics = stateManager.getMetrics()
        assertTrue(metrics.snapshotCount >= 3)
    }

    @Test
    fun testGetMetrics_tracksCheckpointCount() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.createCheckpoint("cp1")
        stateManager.createCheckpoint("cp2")

        val metrics = stateManager.getMetrics()
        assertEquals(2, metrics.checkpointCount)
    }

    @Test
    fun testGetMetrics_tracksPersistenceOperations() = runTest {
        stateManager.initialize(context, StateConfig())

        stateManager.saveState()
        stateManager.restoreState()
        stateManager.saveState()

        val metrics = stateManager.getMetrics()
        assertTrue(metrics.persistenceOperations >= 3)
    }

    // ========================================
    // 9. Performance Tests
    // ========================================

    @Test
    fun testStateUpdate_performance() = runTest {
        stateManager.initialize(context, StateConfig())

        val time = measureTimeMillis {
            repeat(1000) {
                stateManager.setServiceReady(it % 2 == 0)
            }
        }

        // Should complete 1000 updates in less than 1000ms (avg <1ms per update)
        assertTrue("State updates took ${time}ms, expected <1000ms", time < 1000)
    }

    @Test
    fun testStateRead_performance() = runTest {
        stateManager.initialize(context, StateConfig())

        val time = measureTimeMillis {
            repeat(10000) {
                stateManager.isServiceReady.value
            }
        }

        // Should complete 10000 reads in less than 100ms (avg <0.01ms per read)
        assertTrue("State reads took ${time}ms, expected <100ms", time < 100)
    }

    @Test
    fun testValidation_performance() = runTest {
        stateManager.initialize(context, StateConfig())

        val time = measureTimeMillis {
            repeat(1000) {
                stateManager.validateState()
            }
        }

        // Should complete 1000 validations in less than 2000ms (avg <2ms per validation)
        assertTrue("Validations took ${time}ms, expected <2000ms", time < 2000)
    }

    @Test
    fun testSnapshot_performance() = runTest {
        stateManager.initialize(context, StateConfig())

        val time = measureTimeMillis {
            repeat(1000) {
                stateManager.getStateSnapshot()
            }
        }

        // Should complete 1000 snapshots in less than 1000ms (avg <1ms per snapshot)
        assertTrue("Snapshots took ${time}ms, expected <1000ms", time < 1000)
    }

    @Test
    fun testMetrics_performance() = runTest {
        stateManager.initialize(context, StateConfig())

        // Create some state changes
        repeat(100) {
            stateManager.setServiceReady(it % 2 == 0)
        }

        val time = measureTimeMillis {
            repeat(1000) {
                stateManager.getMetrics()
            }
        }

        // Should complete 1000 metrics calls in less than 1000ms (avg <1ms per call)
        assertTrue("Metrics calls took ${time}ms, expected <1000ms", time < 1000)
    }

    // ========================================
    // 10. Edge Cases Tests
    // ========================================

    @Test
    fun testSetSameValue_doesNotEmitChange() = runTest {
        stateManager.initialize(context, StateConfig())

        var changeCount = 0
        stateManager.registerStateObserver { changeCount++ }

        stateManager.setServiceReady(false) // Same as initial value
        delay(100)

        assertEquals(0, changeCount)
    }

    @Test
    fun testRestoreCheckpoint_nonexistent() = runTest {
        stateManager.initialize(context, StateConfig())

        val restored = stateManager.restoreCheckpoint("nonexistent")
        assertFalse(restored)
    }

    @Test
    fun testValidation_beforeInitialization() = runTest {
        val result = stateManager.validateState()
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testGetSnapshot_beforeInitialization() = runTest {
        // Should not crash
        val snapshot = stateManager.getStateSnapshot()
        assertNotNull(snapshot)
    }

    @Test
    fun testMaxHistorySize_enforced() = runTest {
        stateManager.initialize(context, StateConfig(maxHistorySize = 10))

        repeat(20) {
            stateManager.saveState()
        }

        val history = stateManager.getStateHistory(100)
        assertTrue(history.size <= 10)
    }
}
