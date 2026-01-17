/**
 * SdkInitializationManagerTest.kt - Unit tests for SDK initialization framework
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 * 
 * Tests for thread-safe initialization, retry mechanisms, and state management
 */
package com.augmentalis.voiceos.speech.engines.common

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SdkInitializationManagerTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @After
    fun cleanup() = runTest {
        SdkInitializationManager.cleanup()
    }
    
    @Test
    fun testSuccessfulInitialization() = runTest {
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = "TestSDK",
            configPath = "/test/config",
            context = context,
            maxRetries = 1
        )
        
        val result = SdkInitializationManager.initializeSDK(initContext) { _ ->
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        assertTrue("Initialization should succeed", result.success)
        assertEquals(
            "State should be INITIALIZED",
            SdkInitializationManager.InitializationState.INITIALIZED,
            result.state
        )
        assertTrue("Initialization time should be tracked", result.initializationTime > 0)
    }
    
    @Test
    fun testFailureWithRetries() = runTest {
        val attemptCount = AtomicInteger(0)
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = "FailingSDK",
            configPath = "/test/config",
            context = context,
            maxRetries = 3,
            baseDelayMs = 10 // Fast retries for testing
        )
        
        val result = SdkInitializationManager.initializeSDK(initContext) { _ ->
            val count = attemptCount.incrementAndGet()
            if (count < 3) {
                throw Exception("Simulated failure attempt $count")
            }
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        assertTrue("Should succeed after retries", result.success)
        assertEquals("Should have retried 2 times", 2, result.retryCount)
        assertEquals("Should have made 3 attempts total", 3, attemptCount.get())
    }
    
    @Test
    fun testConcurrentInitializationAttempts() = runTest {
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = "ConcurrentSDK", 
            configPath = "/test/config",
            context = context,
            maxRetries = 1,
            initializationTimeout = 2000L
        )
        
        val initializationCount = AtomicInteger(0)
        val completionLatch = CountDownLatch(5)
        val results = Array<SdkInitializationManager.InitializationResult?>(5) { null }
        
        // Start 5 concurrent initialization attempts
        repeat(5) { index ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = SdkInitializationManager.initializeSDK(initContext) { _ ->
                        val count = initializationCount.incrementAndGet()
                        delay(500) // Simulate slow initialization
                        SdkInitializationManager.InitializationResult(
                            success = true,
                            state = SdkInitializationManager.InitializationState.INITIALIZED,
                            metadata = mapOf("init_count" to count)
                        )
                    }
                    results[index] = result
                } finally {
                    completionLatch.countDown()
                }
            }
        }
        
        // Wait for all attempts to complete
        assertTrue(
            "All initialization attempts should complete within 5 seconds",
            completionLatch.await(5, TimeUnit.SECONDS)
        )
        
        // Verify only one actual initialization occurred
        assertEquals(
            "Only one initialization should have occurred",
            1,
            initializationCount.get()
        )
        
        // Verify all attempts succeeded
        results.forEach { result ->
            assertNotNull("Result should not be null", result)
            assertTrue("All attempts should succeed", result!!.success)
            assertEquals(
                "All results should show INITIALIZED state",
                SdkInitializationManager.InitializationState.INITIALIZED,
                result.state
            )
        }
    }
    
    @Test
    fun testInitializationTimeout() = runTest {
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = "TimeoutSDK",
            configPath = "/test/config", 
            context = context,
            maxRetries = 1,
            initializationTimeout = 100L // Very short timeout
        )
        
        val result = SdkInitializationManager.initializeSDK(initContext) { _ ->
            delay(500) // Longer than timeout
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        assertFalse("Initialization should fail due to timeout", result.success)
        assertEquals(
            "State should be FAILED",
            SdkInitializationManager.InitializationState.FAILED,
            result.state
        )
        assertTrue("Error should mention timeout", result.error?.contains("timeout") == true)
    }
    
    @Test
    fun testExponentialBackoff() = runTest {
        val attemptTimes = mutableListOf<Long>()
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = "BackoffSDK",
            configPath = "/test/config",
            context = context,
            maxRetries = 3,
            baseDelayMs = 100L,
            backoffMultiplier = 2.0
        )
        
        val result = SdkInitializationManager.initializeSDK(initContext) { _ ->
            attemptTimes.add(System.currentTimeMillis())
            if (attemptTimes.size < 3) {
                throw Exception("Simulated failure")
            }
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        assertTrue("Should succeed after retries", result.success)
        assertEquals("Should have 3 attempts", 3, attemptTimes.size)
        
        // Verify exponential backoff delays
        if (attemptTimes.size >= 3) {
            val delay1 = attemptTimes[1] - attemptTimes[0]
            val delay2 = attemptTimes[2] - attemptTimes[1]
            
            // Allow some tolerance for timing variations
            assertTrue("Second delay should be roughly double first delay", 
                       delay2 >= delay1 * 1.5)
        }
    }
    
    @Test
    fun testStateTracking() = runTest {
        val sdkName = "StateTrackingSDK"
        
        // Initially should be NOT_INITIALIZED
        assertEquals(
            "Initial state should be NOT_INITIALIZED",
            SdkInitializationManager.InitializationState.NOT_INITIALIZED,
            SdkInitializationManager.getInitializationState(sdkName)
        )
        
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = sdkName,
            configPath = "/test/config",
            context = context,
            maxRetries = 1
        )
        
        // Start async initialization to test INITIALIZING state
        val job = CoroutineScope(Dispatchers.IO).async {
            SdkInitializationManager.initializeSDK(initContext) { _ ->
                // Check state during initialization
                val stateAtStartTime = SdkInitializationManager.getInitializationState(sdkName)
                delay(100) // Simulate initialization work
                SdkInitializationManager.InitializationResult(
                    success = true,
                    state = SdkInitializationManager.InitializationState.INITIALIZED
                )
            }
        }
        
        // Give initialization time to start
        delay(50)
        
        val duringInitState = SdkInitializationManager.getInitializationState(sdkName)
        assertTrue(
            "State during initialization should be INITIALIZING", 
            duringInitState == SdkInitializationManager.InitializationState.INITIALIZING
        )
        
        // Wait for completion
        val result = job.await()
        
        assertTrue("Initialization should succeed", result.success)
        assertEquals(
            "Final state should be INITIALIZED",
            SdkInitializationManager.InitializationState.INITIALIZED,
            SdkInitializationManager.getInitializationState(sdkName)
        )
    }
    
    @Test 
    fun testStatistics() = runTest {
        // Initialize a few different SDKs
        val contexts = listOf(
            SdkInitializationManager.InitializationContext(
                sdkName = "SDK1",
                configPath = "/test/config1",
                context = context
            ),
            SdkInitializationManager.InitializationContext(
                sdkName = "SDK2", 
                configPath = "/test/config2",
                context = context
            )
        )
        
        // Initialize first SDK successfully
        SdkInitializationManager.initializeSDK(contexts[0]) { _ ->
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        // Make second SDK fail
        SdkInitializationManager.initializeSDK(contexts[1]) { _ ->
            SdkInitializationManager.InitializationResult(
                success = false,
                state = SdkInitializationManager.InitializationState.FAILED,
                error = "Test failure"
            )
        }
        
        val stats = SdkInitializationManager.getStatistics()
        
        @Suppress("UNCHECKED_CAST")
        val initializedSdks = stats["initialized_sdks"] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val failedSdks = stats["failed_sdks"] as Set<String>
        
        assertTrue("SDK1 should be in initialized list", initializedSdks.contains("SDK1"))
        assertTrue("SDK2 should be in failed list", failedSdks.contains("SDK2"))
    }
    
    @Test
    fun testForceReset() = runTest {
        val sdkName = "ResetSDK"
        val initContext = SdkInitializationManager.InitializationContext(
            sdkName = sdkName,
            configPath = "/test/config",
            context = context
        )
        
        // Initialize successfully
        val result1 = SdkInitializationManager.initializeSDK(initContext) { _ ->
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        assertTrue("Initial initialization should succeed", result1.success)
        assertEquals(
            "State should be INITIALIZED", 
            SdkInitializationManager.InitializationState.INITIALIZED,
            SdkInitializationManager.getInitializationState(sdkName)
        )
        
        // Force reset
        SdkInitializationManager.resetInitializationState(sdkName)
        
        assertEquals(
            "State should be reset to NOT_INITIALIZED",
            SdkInitializationManager.InitializationState.NOT_INITIALIZED,
            SdkInitializationManager.getInitializationState(sdkName)
        )
        
        // Should be able to initialize again
        val result2 = SdkInitializationManager.initializeSDK(initContext) { _ ->
            SdkInitializationManager.InitializationResult(
                success = true,
                state = SdkInitializationManager.InitializationState.INITIALIZED
            )
        }
        
        assertTrue("Re-initialization should succeed after reset", result2.success)
    }
}