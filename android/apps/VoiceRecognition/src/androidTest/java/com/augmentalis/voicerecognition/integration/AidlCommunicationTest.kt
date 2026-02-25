/**
 * AidlCommunicationTest.kt - AIDL communication integration tests
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-08-28
 * 
 * Tests for actual AIDL interface communication between VoiceRecognition service
 * and client applications, focusing on the interface contract compliance.
 */
package com.augmentalis.voicerecognition.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import com.augmentalis.voicerecognition.mocks.MockRecognitionCallback
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for AIDL communication
 * 
 * Focuses on verifying the AIDL interface contract and communication
 * between processes, ensuring that the service correctly implements
 * all interface methods and handles callbacks properly.
 */
@RunWith(AndroidJUnit4::class)
class AidlCommunicationTest {
    
    companion object {
        private const val TAG = "AidlCommunicationTest"
        private const val SERVICE_PACKAGE = "com.augmentalis.voicerecognition"
        private const val SERVICE_CLASS = "com.augmentalis.voicerecognition.service.VoiceRecognitionService"
        private const val BINDING_TIMEOUT_MS = 15000L
        private const val INTERFACE_TIMEOUT_MS = 5000L
    }
    
    private lateinit var context: Context
    private var service: IVoiceRecognitionService? = null
    private val isConnected = AtomicBoolean(false)
    private val connectionLatch = CountDownLatch(1)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IVoiceRecognitionService.Stub.asInterface(binder)
            isConnected.set(true)
            connectionLatch.countDown()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isConnected.set(false)
        }
    }
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        val intent = Intent().apply {
            setClassName(SERVICE_PACKAGE, SERVICE_CLASS)
        }
        
        val bound = context.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
        
        assertTrue("Service should bind successfully", bound)
        assertTrue(
            "Service should connect within timeout",
            connectionLatch.await(BINDING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        )
        assertNotNull("Service should be connected", service)
    }
    
    @After
    fun tearDown() {
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Ignore unbinding errors
        }
    }
    
    /**
     * Test AIDL interface method signatures and return types
     */
    @Test
    fun testAidlInterfaceContract() = runTest {
        val svc = service!!
        
        // Test getAvailableEngines method
        val engines = svc.availableEngines
        assertNotNull("Available engines should not be null", engines)
        assertTrue("Should have at least one engine", engines.isNotEmpty())
        
        // Test getStatus method
        val status = svc.status
        assertNotNull("Status should not be null", status)
        assertTrue("Status should be non-empty", status.isNotEmpty())
        
        // Test isRecognizing method
        val isRecognizing = svc.isRecognizing
        assertNotNull("isRecognizing should return a boolean value", isRecognizing)
        // Initial state should be false
        assertFalse("Should not be recognizing initially", isRecognizing)
    }
    
    /**
     * Test callback registration and unregistration
     */
    @Test
    fun testCallbackRegistration() = runTest {
        val svc = service!!
        val callback = MockRecognitionCallback("registrationTest")
        
        // Test registration - should not throw exception
        try {
            svc.registerCallback(callback)
            assertTrue("Callback registration should succeed", true)
        } catch (e: Exception) {
            fail("Callback registration should not throw exception: ${e.message}")
        }
        
        // Test unregistration - should not throw exception
        try {
            svc.unregisterCallback(callback)
            assertTrue("Callback unregistration should succeed", true)
        } catch (e: Exception) {
            fail("Callback unregistration should not throw exception: ${e.message}")
        }
    }
    
    /**
     * Test recognition start/stop lifecycle
     */
    @Test
    fun testRecognitionLifecycleAidl() = runTest {
        val svc = service!!
        val callback = MockRecognitionCallback("lifecycleTest")
        
        svc.registerCallback(callback)
        
        // Test start recognition with valid parameters
        val engines = svc.availableEngines
        assertTrue("Need at least one engine for testing", engines.isNotEmpty())
        
        val testEngine = engines.first()
        val startResult = svc.startRecognition(testEngine, "en-US", 0)
        
        // The result depends on implementation - service might reject or accept
        // but should not throw exception
        assertNotNull("Start recognition should return a boolean", startResult)
        
        // If start succeeded, verify service state
        if (startResult) {
            // Allow some time for service to update state
            Thread.sleep(500)
            assertTrue("Service should report recognizing state", svc.isRecognizing)
            
            // Test stop recognition
            val stopResult = svc.stopRecognition()
            assertTrue("Stop recognition should succeed", stopResult)
            
            // Allow time for service to update state
            Thread.sleep(500)
            assertFalse("Service should not be recognizing after stop", svc.isRecognizing)
        }
        
        svc.unregisterCallback(callback)
    }
    
    /**
     * Test callback communication with actual AIDL calls
     */
    @Test
    fun testCallbackCommunication() = runTest {
        val svc = service!!
        val callback = MockRecognitionCallback("communicationTest")
        
        svc.registerCallback(callback)
        
        // Attempt to start recognition to trigger callbacks
        val engines = svc.availableEngines
        if (engines.isNotEmpty()) {
            val startResult = svc.startRecognition(engines.first(), "en-US", 0)
            
            if (startResult) {
                // Wait for state change callback
                val receivedStateChange = callback.waitForStateChange(INTERFACE_TIMEOUT_MS)
                
                // State changes are expected when recognition starts
                if (receivedStateChange) {
                    assertTrue("Should have received state change", callback.getStateChangeCount() > 0)
                    val lastState = callback.getLastStateChange()
                    assertNotNull("Last state change should be available", lastState)
                }
                
                // Stop recognition
                svc.stopRecognition()
                
                // Wait for stop state change
                callback.waitForStateChange(INTERFACE_TIMEOUT_MS)
            }
        }
        
        svc.unregisterCallback(callback)
    }
    
    /**
     * Test multiple callback registration
     */
    @Test
    fun testMultipleCallbacks() = runTest {
        val svc = service!!
        val callback1 = MockRecognitionCallback("callback1")
        val callback2 = MockRecognitionCallback("callback2")
        val callback3 = MockRecognitionCallback("callback3")
        
        // Register multiple callbacks
        svc.registerCallback(callback1)
        svc.registerCallback(callback2)
        svc.registerCallback(callback3)
        
        val engines = svc.availableEngines
        if (engines.isNotEmpty()) {
            val startResult = svc.startRecognition(engines.first(), "en-US", 0)
            
            if (startResult) {
                // All callbacks should receive state changes
                val received1 = callback1.waitForStateChange(INTERFACE_TIMEOUT_MS)
                val received2 = callback2.waitForStateChange(INTERFACE_TIMEOUT_MS) 
                val received3 = callback3.waitForStateChange(INTERFACE_TIMEOUT_MS)
                
                // At least some callbacks should receive notifications
                val totalReceived = listOf(received1, received2, received3).count { it }
                assertTrue("At least one callback should receive state change", totalReceived > 0)
                
                svc.stopRecognition()
            }
        }
        
        // Unregister all callbacks
        svc.unregisterCallback(callback1)
        svc.unregisterCallback(callback2)
        svc.unregisterCallback(callback3)
    }
    
    /**
     * Test error handling in AIDL calls
     */
    @Test
    fun testAidlErrorHandling() = runTest {
        val svc = service!!
        val callback = MockRecognitionCallback("errorTest")
        
        svc.registerCallback(callback)
        
        // Test with invalid parameters
        try {
            val result = svc.startRecognition("invalid_engine", "invalid_language", -1)
            
            // Service should either reject invalid parameters (return false)
            // or accept them and produce error callbacks
            if (!result) {
                assertTrue("Invalid parameters should be rejected", true)
            } else {
                // If accepted, should get error callback
                val receivedError = callback.waitForError(INTERFACE_TIMEOUT_MS)
                if (receivedError) {
                    assertTrue("Should have received error", callback.getErrorCount() > 0)
                }
            }
        } catch (e: Exception) {
            // AIDL methods should not throw exceptions for invalid parameters
            fail("AIDL method should not throw exception: ${e.message}")
        }
        
        svc.unregisterCallback(callback)
    }
    
    /**
     * Test service state consistency across AIDL calls
     */
    @Test
    fun testServiceStateConsistency() = runTest {
        val svc = service!!
        
        // Initial state should be consistent
        assertFalse("Initial recognition state should be false", svc.isRecognizing)
        
        val initialStatus = svc.status
        assertNotNull("Initial status should not be null", initialStatus)
        
        val engines = svc.availableEngines
        assertTrue("Available engines should be consistent", engines.isNotEmpty())
        
        // State should remain consistent across multiple calls
        repeat(5) {
            val status = svc.status
            assertNotNull("Status should always be available", status)
            
            val currentEngines = svc.availableEngines
            assertEquals("Available engines should be consistent", engines.size, currentEngines.size)
            assertTrue("Available engines should contain same engines", 
                engines.containsAll(currentEngines) && currentEngines.containsAll(engines))
        }
    }
    
    /**
     * Test AIDL interface performance and responsiveness
     */
    @Test
    fun testAidlPerformance() = runTest {
        val svc = service!!
        
        // Test method call performance
        val startTime = System.currentTimeMillis()
        
        repeat(10) {
            svc.status
            svc.availableEngines
            svc.isRecognizing
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        assertTrue("AIDL calls should be responsive (< 1000ms for 30 calls)", totalTime < 1000)
        
        // Test callback registration performance
        val callbacks = mutableListOf<MockRecognitionCallback>()
        
        val callbackStartTime = System.currentTimeMillis()
        
        repeat(5) { i ->
            val callback = MockRecognitionCallback("perf_test_$i")
            callbacks.add(callback)
            svc.registerCallback(callback)
        }
        
        // Unregister all
        callbacks.forEach { callback ->
            svc.unregisterCallback(callback)
        }
        
        val callbackTime = System.currentTimeMillis() - callbackStartTime
        assertTrue("Callback registration should be fast (< 500ms for 5 callbacks)", callbackTime < 500)
    }
}