/**
 * ServiceTestTemplate.kt - Advanced Android Service testing template
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: {{DATE}}
 * 
 * Comprehensive Service/Manager testing with lifecycle, binding, and background operations
 */
package {{PACKAGE_NAME}}

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class {{CLASS_NAME}}Test {
    
    @get:Rule
    val serviceRule = ServiceTestRule()
    
    private lateinit var context: Context
    private lateinit var service: {{CLASS_NAME}}
    private var serviceBinder: IBinder? = null
    private var serviceConnection: ServiceConnection? = null
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    {{MOCK_DECLARATIONS}}
    
    companion object {
        private const val BIND_TIMEOUT_MS = 5000L
        private const val OPERATION_TIMEOUT_MS = 10000L
    }
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        context = ApplicationProvider.getApplicationContext()
        {{MOCK_SETUP}}
    }
    
    @After
    fun tearDown() {
        // Unbind and stop service
        unbindService()
        stopService()
        
        Dispatchers.resetMain()
    }
    
    // ========== Service Lifecycle Tests ==========
    
    @Test
    fun `test service starts successfully`() {
        val intent = Intent(context, {{CLASS_NAME}}::class.java)
        
        val startTime = measureTimeMillis {
            service = serviceRule.startService(intent) as {{CLASS_NAME}}
        }
        
        assertNotNull(service)
        assertTrue(startTime < 1000, "Service should start within 1s, took ${startTime}ms")
        {{SERVICE_START_ASSERTIONS}}
    }
    
    @Test
    fun `test service onCreate lifecycle`() {
        val intent = Intent(context, {{CLASS_NAME}}::class.java)
        service = serviceRule.startService(intent) as {{CLASS_NAME}}
        
        // Verify onCreate was called
        assertTrue(service.isInitialized())
        {{ON_CREATE_ASSERTIONS}}
    }
    
    @Test
    fun `test service onDestroy cleanup`() {
        val intent = Intent(context, {{CLASS_NAME}}::class.java)
        service = serviceRule.startService(intent) as {{CLASS_NAME}}
        
        // Track resources
        val initialResources = service.getActiveResources()
        
        // Stop service
        service.stopSelf()
        
        // Verify cleanup
        assertTrue(service.getActiveResources().isEmpty())
        {{ON_DESTROY_ASSERTIONS}}
    }
    
    // ========== Service Binding Tests ==========
    
    @Test
    fun `test service binding successful`() {
        val bindLatch = CountDownLatch(1)
        var boundService: {{CLASS_NAME}}? = null
        
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                boundService = (binder as {{CLASS_NAME}}.LocalBinder).getService()
                bindLatch.countDown()
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                boundService = null
            }
        }
        
        val intent = Intent(context, {{CLASS_NAME}}::class.java)
        val bindResult = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        
        assertTrue(bindResult, "Service binding should succeed")
        assertTrue(bindLatch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS), "Service should bind within timeout")
        assertNotNull(boundService)
        
        context.unbindService(connection)
    }
    
    @Test
    fun `test service binding with multiple clients`() {
        val clients = 5
        val connections = mutableListOf<ServiceConnection>()
        val bindLatches = List(clients) { CountDownLatch(1) }
        val boundServices = mutableListOf<{{CLASS_NAME}}?>()
        
        repeat(clients) { index ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    boundServices.add((binder as {{CLASS_NAME}}.LocalBinder).getService())
                    bindLatches[index].countDown()
                }
                
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            connections.add(connection)
            
            val intent = Intent(context, {{CLASS_NAME}}::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        
        // Wait for all bindings
        bindLatches.forEach { latch ->
            assertTrue(latch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        }
        
        // Verify all clients bound to same service instance
        assertEquals(clients, boundServices.size)
        assertTrue(boundServices.all { it == boundServices[0] })
        
        // Cleanup
        connections.forEach { context.unbindService(it) }
    }
    
    @Test
    fun `test service unbinding lifecycle`() {
        val bindLatch = CountDownLatch(1)
        val unbindLatch = CountDownLatch(1)
        var serviceDisconnected = false
        
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                bindLatch.countDown()
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                serviceDisconnected = true
                unbindLatch.countDown()
            }
        }
        
        val intent = Intent(context, {{CLASS_NAME}}::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        
        assertTrue(bindLatch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        
        // Unbind
        context.unbindService(connection)
        
        // Service should handle unbinding gracefully
        {{UNBIND_ASSERTIONS}}
    }
    
    // ========== Service Operations Tests ==========
    
    @Test
    fun `test service performs operation successfully`() = runTest {
        startAndBindService()
        
        val result = service.performOperation({{OPERATION_PARAMS}})
        
        assertNotNull(result)
        {{OPERATION_SUCCESS_ASSERTIONS}}
    }
    
    @Test
    fun `test service handles concurrent operations`() = runTest {
        startAndBindService()
        
        val operations = 10
        val results = mutableListOf<Deferred<Any>>()
        
        repeat(operations) { index ->
            val deferred = async {
                service.performOperation({{OPERATION_PARAMS_WITH_INDEX}})
            }
            results.add(deferred)
        }
        
        val completedResults = results.awaitAll()
        
        assertEquals(operations, completedResults.size)
        assertTrue(completedResults.all { it != null })
        {{CONCURRENT_OPERATIONS_ASSERTIONS}}
    }
    
    @Test
    fun `test service background task execution`() = runTest {
        startAndBindService()
        
        val taskLatch = CountDownLatch(1)
        var taskResult: Any? = null
        
        service.executeBackgroundTask { result ->
            taskResult = result
            taskLatch.countDown()
        }
        
        assertTrue(taskLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        assertNotNull(taskResult)
        {{BACKGROUND_TASK_ASSERTIONS}}
    }
    
    // ========== Service Communication Tests ==========
    
    @Test
    fun `test service IPC communication`() {
        startAndBindService()
        
        val message = {{TEST_MESSAGE}}
        val response = service.sendMessage(message)
        
        assertNotNull(response)
        {{IPC_COMMUNICATION_ASSERTIONS}}
    }
    
    @Test
    fun `test service broadcast receiver`() {
        startAndBindService()
        
        val receivedBroadcasts = mutableListOf<Intent>()
        service.setBroadcastListener { intent ->
            receivedBroadcasts.add(intent)
        }
        
        // Send test broadcast
        val testIntent = Intent("{{TEST_ACTION}}")
        context.sendBroadcast(testIntent)
        
        // Wait for broadcast to be received
        Thread.sleep(100)
        
        assertTrue(receivedBroadcasts.isNotEmpty())
        {{BROADCAST_RECEIVER_ASSERTIONS}}
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    fun `test service handles errors gracefully`() = runTest {
        startAndBindService()
        
        // Force an error condition
        `when`({{MOCK_DEPENDENCY}}.method()).thenThrow(RuntimeException("Test error"))
        
        val result = service.performOperationWithErrorHandling()
        
        // Service should handle error without crashing
        assertNotNull(result)
        {{ERROR_HANDLING_ASSERTIONS}}
    }
    
    @Test
    fun `test service recovery after failure`() = runTest {
        startAndBindService()
        
        // Simulate failure
        service.simulateFailure()
        
        // Wait for recovery
        delay(1000)
        
        // Service should recover
        assertTrue(service.isOperational())
        {{RECOVERY_ASSERTIONS}}
    }
    
    // ========== Performance Tests ==========
    
    @Test
    fun `test service memory usage`() = runTest {
        startAndBindService()
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Perform memory-intensive operations
        repeat(100) {
            service.performOperation({{OPERATION_PARAMS}})
        }
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / 1_000_000 // Convert to MB
        
        assertTrue(
            memoryIncrease < 50,
            "Memory usage should increase by less than 50MB, increased by ${memoryIncrease}MB"
        )
    }
    
    @Test
    fun `test service response time`() = runTest {
        startAndBindService()
        
        val responseTimes = mutableListOf<Long>()
        
        repeat(50) {
            val responseTime = measureTimeMillis {
                service.performOperation({{OPERATION_PARAMS}})
            }
            responseTimes.add(responseTime)
        }
        
        val averageResponseTime = responseTimes.average()
        val maxResponseTime = responseTimes.maxOrNull() ?: 0
        
        assertTrue(
            averageResponseTime < 100,
            "Average response time should be <100ms, was ${averageResponseTime}ms"
        )
        assertTrue(
            maxResponseTime < 500,
            "Max response time should be <500ms, was ${maxResponseTime}ms"
        )
    }
    
    // ========== Resource Management Tests ==========
    
    @Test
    fun `test service releases resources on stop`() {
        startAndBindService()
        
        // Track resources
        val resourcesBefore = service.getActiveResources()
        assertTrue(resourcesBefore.isNotEmpty())
        
        // Stop service
        stopService()
        
        // Verify resources released
        val resourcesAfter = service.getActiveResources()
        assertTrue(resourcesAfter.isEmpty())
    }
    
    @Test
    fun `test service handles low memory conditions`() {
        startAndBindService()
        
        // Simulate low memory
        service.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        
        // Service should free non-critical resources
        {{LOW_MEMORY_ASSERTIONS}}
    }
    
    // ========== Integration Tests ==========
    
    @Test
    fun `test service integration with other components`() = runTest {
        startAndBindService()
        
        // Test integration with other VOS4 components
        val result = service.integrateWith{{OTHER_COMPONENT}}()
        
        assertNotNull(result)
        {{INTEGRATION_ASSERTIONS}}
    }
    
    @Test
    fun `test complete service workflow`() = runTest {
        // Start service
        startAndBindService()
        
        // Perform workflow steps
        service.initialize()
        service.authenticate("test_user")
        val data = service.fetchData()
        service.processData(data)
        val result = service.getResult()
        
        // Verify workflow completed
        assertNotNull(result)
        {{WORKFLOW_ASSERTIONS}}
        
        // Cleanup
        service.cleanup()
        stopService()
    }
    
    // ========== Helper Methods ==========
    
    private fun startAndBindService() {
        val intent = Intent(context, {{CLASS_NAME}}::class.java)
        service = serviceRule.startService(intent) as {{CLASS_NAME}}
        
        serviceBinder = serviceRule.bindService(intent)
        assertNotNull(serviceBinder)
    }
    
    private fun unbindService() {
        serviceConnection?.let {
            try {
                context.unbindService(it)
            } catch (e: IllegalArgumentException) {
                // Service not bound
            }
        }
        serviceConnection = null
        serviceBinder = null
    }
    
    private fun stopService() {
        service?.stopSelf()
    }
}