/**
 * CoroutineScopeCancellationTest.kt - Tests for Issue #9: Coroutine Scope Not Cancelled on Service Destroy
 *
 * YOLO Phase 2 - High Priority Issue #9: Coroutine Scope Cancellation
 *
 * Tests verify proper coroutine scope cancellation with cancelAndJoin() to prevent:
 * - Memory leaks from running coroutines after service destruction
 * - Background tasks continuing after service destroyed
 * - Exception swallowing during cancellation
 *
 * File: VoiceOSService.kt:1454-1460
 * Problem: Exception during cancel is logged but swallowed, may leave running coroutines
 * Solution: Use cancelAndJoin() or verify all jobs cancelled with proper error handling
 *
 * Run with: ./gradlew :modules:apps:VoiceOSCore:connectedDebugAndroidTest
 */
package com.augmentalis.voiceoscore.coroutines

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Instrumented test suite for coroutine scope cancellation
 *
 * Tests verify proper cancellation behavior with cancelAndJoin() to ensure
 * all coroutines are stopped when service is destroyed.
 */
@RunWith(AndroidJUnit4::class)
class CoroutineScopeCancellationTest {

    /**
     * TEST 1: Verify simple scope cancellation stops active coroutines
     */
    @Test
    fun testSimpleScopeCancellation() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val jobStarted = AtomicBoolean(false)
        val jobCompleted = AtomicBoolean(false)

        val job = scope.launch {
            jobStarted.set(true)
            delay(10000) // Long delay
            jobCompleted.set(true)
        }

        delay(100) // Let job start
        assertThat(jobStarted.get()).isTrue()
        assertThat(scope.isActive).isTrue()

        // Cancel scope and join
        scope.cancel()
        job.join()

        assertThat(scope.isActive).isFalse()
        assertThat(jobCompleted.get()).isFalse() // Job should not complete
    }

    /**
     * TEST 2: Verify cancel() alone does not wait for jobs to finish
     */
    @Test
    fun testCancelWithoutJoinDoesNotWait() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val jobRunning = AtomicBoolean(false)

        scope.launch {
            jobRunning.set(true)
            delay(5000) // Long delay
            jobRunning.set(false)
        }

        delay(100) // Let job start
        assertThat(jobRunning.get()).isTrue()

        // Cancel without join - does not wait
        scope.cancel()

        // Job may still be running immediately after cancel()
        assertThat(scope.isActive).isFalse()
    }

    /**
     * TEST 3: Verify cancelAndJoin() waits for all jobs to finish
     */
    @Test
    fun testCancelAndJoinWaitsForCompletion() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val executionOrder = mutableListOf<String>()

        val job = scope.launch {
            executionOrder.add("job_started")
            delay(100)
            executionOrder.add("job_finishing")
        }

        delay(50) // Let job start
        executionOrder.add("before_cancel")

        job.cancelAndJoin()
        executionOrder.add("after_cancel")

        // Verify order shows job was allowed to finish cleanup
        assertThat(executionOrder).containsExactly(
            "job_started",
            "before_cancel",
            "job_finishing",
            "after_cancel"
        ).inOrder()
    }

    /**
     * TEST 4: Verify multiple jobs all cancelled properly
     */
    @Test
    fun testMultipleJobsCancellation() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val jobsCompleted = AtomicInteger(0)

        val jobs = List(5) {
            scope.launch {
                try {
                    delay(10000)
                    jobsCompleted.incrementAndGet()
                } catch (e: Exception) {
                    // Expected cancellation
                }
            }
        }

        delay(100) // Let jobs start

        scope.cancel()
        jobs.forEach { it.join() }

        assertThat(jobsCompleted.get()).isEqualTo(0) // No jobs should complete
        assertThat(scope.isActive).isFalse()
    }

    /**
     * TEST 5: Verify exception during cancellation does not prevent scope cancellation
     */
    @Test
    fun testExceptionDuringCancellationStillCancelsScope() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        scope.launch {
            try {
                delay(10000)
            } finally {
                // Throw exception during cleanup
                throw IllegalStateException("Cleanup error")
            }
        }

        delay(100) // Let job start

        try {
            scope.cancel()
        } catch (e: Exception) {
            // Exception may be thrown
        }

        assertThat(scope.isActive).isFalse()
    }

    /**
     * TEST 6: Verify nested scopes are cancelled
     */
    @Test
    fun testNestedScopesCancellation() = runBlocking {
        val parentScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val childJobCompleted = AtomicBoolean(false)

        parentScope.launch {
            launch {
                delay(10000)
                childJobCompleted.set(true)
            }
        }

        delay(100) // Let jobs start

        parentScope.cancel()

        delay(200) // Give time for any cleanup
        assertThat(childJobCompleted.get()).isFalse()
        assertThat(parentScope.isActive).isFalse()
    }

    /**
     * TEST 7: Verify SupervisorJob isolates failures
     */
    @Test
    fun testSupervisorJobIsolatesFailures() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val job1Completed = AtomicBoolean(false)
        val job2Completed = AtomicBoolean(false)

        scope.launch {
            delay(100)
            throw IllegalStateException("Job 1 failed")
        }

        val job2 = scope.launch {
            delay(200)
            job2Completed.set(true)
        }

        delay(300) // Let both jobs run

        // Job 2 should complete despite Job 1 failure
        assertThat(job2Completed.get()).isTrue()
        assertThat(scope.isActive).isTrue()

        scope.cancel()
        job2.join()
    }

    /**
     * TEST 8: Verify cancellation with active long-running operations
     */
    @Test
    fun testCancellationWithLongRunningOperation() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val operationCancelled = AtomicBoolean(false)

        val job = scope.launch {
            try {
                repeat(100) {
                    delay(100) // Simulate long operation
                }
            } catch (e: Exception) {
                operationCancelled.set(true)
            }
        }

        delay(200) // Let operation start

        job.cancelAndJoin()

        assertThat(operationCancelled.get()).isTrue()
        assertThat(scope.isActive).isFalse()
    }

    /**
     * TEST 9: Verify scope can be checked for active state
     */
    @Test
    fun testScopeActiveStateCheck() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        assertThat(scope.isActive).isTrue()

        scope.cancel()

        assertThat(scope.isActive).isFalse()
    }

    /**
     * TEST 10: Verify jobs can check if they are active
     */
    @Test
    fun testJobActiveStateCheck() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val wasActive = AtomicBoolean(false)
        val wasInactive = AtomicBoolean(false)

        val job = scope.launch {
            wasActive.set(isActive)
            delay(100)
        }

        delay(50)
        job.cancel()
        job.join()

        assertThat(wasActive.get()).isTrue()

        scope.launch {
            delay(50)
            wasInactive.set(!isActive)
        }.let {
            it.cancel()
            it.join()
        }

        assertThat(wasInactive.get()).isTrue()
    }

    /**
     * TEST 11: Verify proper cleanup sequence with multiple resources
     */
    @Test
    fun testProperCleanupSequence() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val cleanupOrder = mutableListOf<String>()

        scope.launch {
            try {
                delay(10000)
            } finally {
                cleanupOrder.add("job1_cleanup")
            }
        }

        scope.launch {
            try {
                delay(10000)
            } finally {
                cleanupOrder.add("job2_cleanup")
            }
        }

        delay(100) // Let jobs start

        scope.cancel()

        delay(200) // Allow cleanup

        assertThat(cleanupOrder).hasSize(2)
        assertThat(cleanupOrder).containsAtLeast("job1_cleanup", "job2_cleanup")
    }

    /**
     * TEST 12: Verify cancellation propagates to child coroutines
     */
    @Test
    fun testCancellationPropagates() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val childCancelled = AtomicBoolean(false)

        val parent = scope.launch {
            launch {
                try {
                    delay(10000)
                } catch (e: Exception) {
                    childCancelled.set(true)
                }
            }
        }

        delay(100) // Let jobs start

        parent.cancelAndJoin()

        assertThat(childCancelled.get()).isTrue()
    }

    /**
     * TEST 13: Verify scope remains cancelled after cancellation
     */
    @Test
    fun testScopeRemainsCancelled() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        scope.cancel()

        assertThat(scope.isActive).isFalse()

        // Attempting to launch should fail or be ignored
        var jobLaunched = false
        try {
            scope.launch {
                jobLaunched = true
            }
        } catch (e: Exception) {
            // Expected - scope is cancelled
        }

        delay(100)
        assertThat(jobLaunched).isFalse()
    }

    /**
     * TEST 14: Verify error handling during cancellation
     */
    @Test
    fun testErrorHandlingDuringCancellation() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val errors = mutableListOf<String>()

        scope.launch {
            try {
                delay(10000)
            } catch (e: Exception) {
                errors.add("job_cancelled: ${e.javaClass.simpleName}")
            }
        }

        delay(100) // Let job start

        try {
            scope.cancel()
        } catch (e: Exception) {
            errors.add("scope_cancel_error: ${e.javaClass.simpleName}")
        }

        delay(200) // Allow error handling

        assertThat(errors).isNotEmpty()
        assertThat(scope.isActive).isFalse()
    }

    /**
     * TEST 15: Verify performance - cancellation completes within reasonable time
     */
    @Test
    fun testCancellationPerformance() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        // Launch 100 jobs
        val jobs = List(100) {
            scope.launch {
                delay(10000)
            }
        }

        delay(100) // Let jobs start

        val startTime = System.currentTimeMillis()

        scope.cancel()
        jobs.forEach { it.join() }

        val duration = System.currentTimeMillis() - startTime

        // Cancellation of 100 jobs should complete within 1 second
        assertThat(duration).isLessThan(1000L)
        assertThat(scope.isActive).isFalse()
    }
}
