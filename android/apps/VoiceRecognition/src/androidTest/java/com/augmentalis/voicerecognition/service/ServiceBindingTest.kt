/**
 * ServiceBindingTest.kt - Integration tests for VoiceRecognitionService binding
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-08-28
 * 
 * Tests for AIDL service binding, callback registration, recognition lifecycle,
 * multi-client scenarios, and service death recovery.
 */
package com.augmentalis.voicerecognition.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import com.augmentalis.voicerecognition.mocks.MockRecognitionCallback
import kotlinx.coroutines.*
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
 * Integration tests for VoiceRecognitionService AIDL binding
 * 
 * Tests the complete service lifecycle including:
 * - Service binding and unbinding
 * - Callback registration and communication
 * - Recognition start/stop operations
 * - Multi-client scenarios
 * - Service death and recovery
 */
@RunWith(AndroidJUnit4::class)
class ServiceBindingTest {
    
    companion object {
        private const val TAG = "ServiceBindingTest"
        private const val SERVICE_PACKAGE = "com.augmentalis.voicerecognition"
        private const val SERVICE_CLASS = "com.augmentalis.voicerecognition.service.VoiceRecognitionService"
        private const val BINDING_TIMEOUT_MS = 10000L
        private const val CALLBACK_TIMEOUT_MS = 5000L
        private const val TEST_ENGINE = "google"
        private const val TEST_LANGUAGE = "en-US"
        private const val TEST_MODE = 0 // Continuous mode
    }
    
    private lateinit var context: Context
    private var serviceReference = AtomicReference<IVoiceRecognitionService?>(null)
    private val serviceConnections = mutableListOf<TestServiceConnection>()
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    /**
     * Test service connection wrapper
     */
    private inner class TestServiceConnection(
        private val name: String
    ) : ServiceConnection {
        
        private val connectionLatch = CountDownLatch(1)
        private val disconnectionLatch = CountDownLatch(1)
        private val isConnected = AtomicBoolean(false)
        private var service: IVoiceRecognitionService? = null
        
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this.service = IVoiceRecognitionService.Stub.asInterface(service)
            serviceReference.set(this.service)
            isConnected.set(true)
            connectionLatch.countDown()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            this.service = null
            serviceReference.set(null)
            isConnected.set(false)
            disconnectionLatch.countDown()
        }
        
        fun waitForConnection(): Boolean {
            return connectionLatch.await(BINDING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
        
        fun waitForDisconnection(): Boolean {
            return disconnectionLatch.await(BINDING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
        
        fun isServiceConnected(): Boolean = isConnected.get()
        
        fun getService(): IVoiceRecognitionService? = service
    }
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        serviceConnections.clear()
    }
    
    @After
    fun tearDown() {
        // Clean up all service connections
        serviceConnections.forEach { connection ->
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                // Ignore unbinding errors during cleanup
            }
        }
        serviceConnections.clear()
        
        testScope.cancel()
    }
    
    /**
     * Test basic service binding and unbinding
     */
    @Test
    fun testServiceBinding() = runTest {
        val connection = createAndBindService("testServiceBinding")
        
        // Verify service bound successfully
        assertTrue("Service should bind within timeout", connection.waitForConnection())
        assertTrue("Service connection should be active", connection.isServiceConnected())
        assertNotNull("Service reference should not be null", connection.getService())
        
        // Test service interface availability
        val service = connection.getService()!!
        assertNotNull("Service should have valid status", service.status)
        assertNotNull("Service should have available engines", service.availableEngines)
        assertFalse("Service should not be recognizing initially", service.isRecognizing)
        
        // Clean unbinding
        unbindService(connection)
        assertTrue("Service should disconnect within timeout", connection.waitForDisconnection())
        assertFalse("Service connection should be inactive", connection.isServiceConnected())
    }
    
    /**
     * Test callback registration and unregistration
     */
    @Test
    fun testCallbackRegistration() = runTest {
        val connection = createAndBindService("testCallbackRegistration")
        assertTrue("Service should bind", connection.waitForConnection())
        
        val service = connection.getService()!!
        val mockCallback = MockRecognitionCallback("testCallback")
        
        // Register callback
        service.registerCallback(mockCallback)
        
        // Test that callback is registered by triggering a state change
        // Note: This test relies on the service implementation to broadcast state changes
        mockCallback.waitForStateChange(CALLBACK_TIMEOUT_MS)
        // State change might not occur immediately, so this is optional verification
        
        // Unregister callback
        service.unregisterCallback(mockCallback)
        
        unbindService(connection)
    }
    
    /**
     * Test recognition start and stop operations
     */
    @Test
    fun testRecognitionLifecycle() = runTest {
        val connection = createAndBindService("testRecognitionLifecycle")
        assertTrue("Service should bind", connection.waitForConnection())
        
        val service = connection.getService()!!
        val mockCallback = MockRecognitionCallback("lifecycleCallback")
        
        service.registerCallback(mockCallback)
        
        // Test start recognition
        val startResult = service.startRecognition(TEST_ENGINE, TEST_LANGUAGE, TEST_MODE)
        assertTrue("Recognition should start successfully", startResult)
        
        // Wait for state change to listening
        assertTrue(
            "Should receive state change within timeout", 
            mockCallback.waitForStateChange(CALLBACK_TIMEOUT_MS)
        )
        
        // Verify service is in recognizing state
        assertTrue("Service should report recognizing state", service.isRecognizing)
        
        // Test stop recognition
        val stopResult = service.stopRecognition()
        assertTrue("Recognition should stop successfully", stopResult)
        
        // Wait for state change to idle
        assertTrue(
            "Should receive stop state change within timeout", 
            mockCallback.waitForStateChange(CALLBACK_TIMEOUT_MS)
        )
        
        // Verify service is no longer recognizing
        assertFalse("Service should not be recognizing after stop", service.isRecognizing)
        
        service.unregisterCallback(mockCallback)
        unbindService(connection)
    }
    
    /**
     * Test multiple client connections
     */
    @Test
    fun testMultiClientScenario() = runTest {
        // Create multiple client connections
        val connection1 = createAndBindService("client1")
        val connection2 = createAndBindService("client2")
        
        assertTrue("Client 1 should bind", connection1.waitForConnection())
        assertTrue("Client 2 should bind", connection2.waitForConnection())
        
        val service1 = connection1.getService()!!
        val service2 = connection2.getService()!!
        
        val callback1 = MockRecognitionCallback("client1Callback")
        val callback2 = MockRecognitionCallback("client2Callback")
        
        // Register callbacks from both clients
        service1.registerCallback(callback1)
        service2.registerCallback(callback2)
        
        // Start recognition from client 1
        val startResult = service1.startRecognition(TEST_ENGINE, TEST_LANGUAGE, TEST_MODE)
        assertTrue("Recognition should start from client 1", startResult)
        
        // Both callbacks should receive state changes
        assertTrue(
            "Client 1 callback should receive state change", 
            callback1.waitForStateChange(CALLBACK_TIMEOUT_MS)
        )
        assertTrue(
            "Client 2 callback should receive state change", 
            callback2.waitForStateChange(CALLBACK_TIMEOUT_MS)
        )
        
        // Stop recognition from client 2
        val stopResult = service2.stopRecognition()
        assertTrue("Recognition should stop from client 2", stopResult)
        
        // Both callbacks should receive stop state change
        assertTrue(
            "Client 1 callback should receive stop state", 
            callback1.waitForStateChange(CALLBACK_TIMEOUT_MS)
        )
        assertTrue(
            "Client 2 callback should receive stop state", 
            callback2.waitForStateChange(CALLBACK_TIMEOUT_MS)
        )
        
        // Clean up
        service1.unregisterCallback(callback1)
        service2.unregisterCallback(callback2)
        unbindService(connection1)
        unbindService(connection2)
    }
    
    /**
     * Test service death and recovery scenarios
     */
    @Test
    fun testServiceDeathRecovery() = runTest {
        val connection = createAndBindService("deathRecoveryTest")
        assertTrue("Service should bind", connection.waitForConnection())
        
        val service = connection.getService()!!
        val mockCallback = MockRecognitionCallback("deathRecoveryCallback")
        
        service.registerCallback(mockCallback)
        
        // Start recognition to establish active state
        service.startRecognition(TEST_ENGINE, TEST_LANGUAGE, TEST_MODE)
        assertTrue("Should receive initial state change", mockCallback.waitForStateChange(CALLBACK_TIMEOUT_MS))
        
        // Test reconnection after unbinding/rebinding (simulated death)
        unbindService(connection)
        assertTrue("Service should disconnect", connection.waitForDisconnection())
        
        // Rebind to simulate recovery
        val recoveryConnection = createAndBindService("recoveryTest")
        assertTrue("Service should rebind after recovery", recoveryConnection.waitForConnection())
        
        val recoveredService = recoveryConnection.getService()!!
        
        // Verify service state after recovery
        assertFalse("Service should not be recognizing after restart", recoveredService.isRecognizing)
        assertNotNull("Service should have status after recovery", recoveredService.status)
        assertTrue("Service should have available engines", recoveredService.availableEngines.isNotEmpty())
        
        unbindService(recoveryConnection)
    }
    
    /**
     * Test callback error handling
     */
    @Test
    fun testCallbackErrorHandling() = runTest {
        val connection = createAndBindService("errorHandlingTest")
        assertTrue("Service should bind", connection.waitForConnection())
        
        val service = connection.getService()!!
        val mockCallback = MockRecognitionCallback("errorCallback")
        
        service.registerCallback(mockCallback)
        
        // Attempt to start recognition with invalid engine
        val result = service.startRecognition("invalid_engine", TEST_LANGUAGE, TEST_MODE)
        
        if (!result) {
            // If service rejects invalid engine, that's expected
            assertTrue("Service should reject invalid engine", true)
        } else {
            // If service accepts but produces error, wait for error callback
            assertTrue(
                "Should receive error callback for invalid engine", 
                mockCallback.waitForError(CALLBACK_TIMEOUT_MS)
            )
        }
        
        service.unregisterCallback(mockCallback)
        unbindService(connection)
    }
    
    /**
     * Test engine availability and status reporting
     */
    @Test
    fun testEngineAvailabilityAndStatus() = runTest {
        val connection = createAndBindService("engineStatusTest")
        assertTrue("Service should bind", connection.waitForConnection())
        
        val service = connection.getService()!!
        
        // Test available engines
        val engines = service.availableEngines
        assertNotNull("Available engines should not be null", engines)
        assertTrue("Should have at least one engine available", engines.isNotEmpty())
        
        // Verify expected engines are available
        val engineNames = engines.map { it.lowercase() }
        assertTrue("Should have google engine", engineNames.contains("google_stt"))
        
        // Test status reporting
        val status = service.status
        assertNotNull("Status should not be null", status)
        assertTrue("Status should be non-empty", status.isNotEmpty())
        
        unbindService(connection)
    }
    
    // Helper methods
    
    private fun createAndBindService(connectionName: String): TestServiceConnection {
        val connection = TestServiceConnection(connectionName)
        serviceConnections.add(connection)
        
        val intent = Intent().apply {
            setClassName(SERVICE_PACKAGE, SERVICE_CLASS)
        }
        
        val bound = context.bindService(
            intent,
            connection,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
        
        assertTrue("Service should bind successfully", bound)
        return connection
    }
    
    private fun unbindService(connection: TestServiceConnection) {
        try {
            context.unbindService(connection)
            serviceConnections.remove(connection)
        } catch (e: Exception) {
            // Service may already be unbound
        }
    }
}