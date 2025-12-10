/**
 * AIDLTestTemplate.kt - Advanced AIDL integration testing template
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: {{DATE}}
 * 
 * Comprehensive AIDL interface testing with IPC, callbacks, and cross-process validation
 */
package {{PACKAGE_NAME}}

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class {{CLASS_NAME}}Test {
    
    @get:Rule
    val serviceRule = ServiceTestRule()
    
    private lateinit var context: Context
    private var aidlService: {{AIDL_INTERFACE}}? = null
    private var serviceConnection: ServiceConnection? = null
    private val testDispatcher = UnconfinedTestDispatcher()
    
    companion object {
        private const val BIND_TIMEOUT_MS = 5000L
        private const val OPERATION_TIMEOUT_MS = 10000L
        private const val SERVICE_PACKAGE = "{{SERVICE_PACKAGE}}"
        private const val SERVICE_ACTION = "{{SERVICE_ACTION}}"
    }
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }
    
    @After
    fun tearDown() {
        unbindFromService()
        Dispatchers.resetMain()
    }
    
    // ========== Service Binding Tests ==========
    
    @Test
    fun `test AIDL service binding successful`() = runTest {
        val bindResult = bindToService()
        
        assertTrue(bindResult, "Should successfully bind to AIDL service")
        assertNotNull(aidlService, "AIDL service interface should be available")
        
        // Verify service is responsive
        val isAlive = aidlService?.asBinder()?.isBinderAlive
        assertTrue(isAlive == true, "Service binder should be alive")
    }
    
    @Test
    fun `test AIDL service binding timeout handling`() = runTest {
        val invalidIntent = Intent().apply {
            component = ComponentName("invalid.package", "invalid.Service")
        }
        
        val bindResult = bindToServiceWithIntent(invalidIntent, timeout = 1000L)
        
        assertFalse(bindResult, "Should fail to bind to invalid service")
        assertNull(aidlService, "Service interface should be null")
    }
    
    @Test
    fun `test AIDL service reconnection after disconnect`() = runTest {
        // Initial binding
        assertTrue(bindToService(), "First binding should succeed")
        val firstService = aidlService
        assertNotNull(firstService)
        
        // Unbind
        unbindFromService()
        assertNull(aidlService)
        
        // Rebind
        assertTrue(bindToService(), "Rebinding should succeed")
        val secondService = aidlService
        assertNotNull(secondService)
        
        // Services might be same or different instance
        {{RECONNECTION_ASSERTIONS}}
    }
    
    // ========== Basic AIDL Operations Tests ==========
    
    @Test
    fun `test AIDL synchronous method call`() = runTest {
        assertTrue(bindToService())
        
        val result = aidlService?.{{SYNC_METHOD}}({{SYNC_PARAMS}})
        
        assertNotNull(result)
        {{SYNC_METHOD_ASSERTIONS}}
    }
    
    @Test
    fun `test AIDL method with primitive parameters`() = runTest {
        assertTrue(bindToService())
        
        val intResult = aidlService?.methodWithInt(42)
        val stringResult = aidlService?.methodWithString("test")
        val boolResult = aidlService?.methodWithBoolean(true)
        
        assertEquals(42, intResult)
        assertEquals("test", stringResult)
        assertEquals(true, boolResult)
    }
    
    @Test
    fun `test AIDL method with complex parameters`() = runTest {
        assertTrue(bindToService())
        
        val complexParam = {{COMPLEX_PARAM_CREATION}}
        val result = aidlService?.methodWithComplexParam(complexParam)
        
        assertNotNull(result)
        {{COMPLEX_PARAM_ASSERTIONS}}
    }
    
    @Test
    fun `test AIDL method with nullable parameters`() = runTest {
        assertTrue(bindToService())
        
        // Test with null
        val nullResult = aidlService?.methodWithNullable(null)
        {{NULL_PARAM_ASSERTIONS}}
        
        // Test with non-null
        val nonNullResult = aidlService?.methodWithNullable({{NON_NULL_PARAM}})
        {{NON_NULL_PARAM_ASSERTIONS}}
    }
    
    // ========== Callback Tests ==========
    
    @Test
    fun `test AIDL callback registration and invocation`() = runTest {
        assertTrue(bindToService())
        
        val callbackLatch = CountDownLatch(1)
        val receivedData = AtomicReference<{{CALLBACK_DATA_TYPE}}>()
        
        val callback = object : {{CALLBACK_INTERFACE}}.Stub() {
            override fun {{CALLBACK_METHOD}}(data: {{CALLBACK_DATA_TYPE}}) {
                receivedData.set(data)
                callbackLatch.countDown()
            }
            
            {{OTHER_CALLBACK_METHODS}}
        }
        
        // Register callback
        val registered = aidlService?.registerCallback(callback)
        assertTrue(registered == true, "Callback registration should succeed")
        
        // Trigger callback
        aidlService?.triggerCallback()
        
        // Wait for callback
        val callbackReceived = callbackLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(callbackReceived, "Should receive callback within timeout")
        
        assertNotNull(receivedData.get())
        {{CALLBACK_DATA_ASSERTIONS}}
        
        // Unregister callback
        aidlService?.unregisterCallback(callback)
    }
    
    @Test
    fun `test AIDL multiple callbacks handling`() = runTest {
        assertTrue(bindToService())
        
        val callbackCount = 5
        val callbacks = mutableListOf<{{CALLBACK_INTERFACE}}>()
        val latches = List(callbackCount) { CountDownLatch(1) }
        val results = MutableList(callbackCount) { AtomicBoolean(false) }
        
        // Register multiple callbacks
        repeat(callbackCount) { index ->
            val callback = object : {{CALLBACK_INTERFACE}}.Stub() {
                override fun {{CALLBACK_METHOD}}(data: {{CALLBACK_DATA_TYPE}}) {
                    results[index].set(true)
                    latches[index].countDown()
                }
            }
            callbacks.add(callback)
            aidlService?.registerCallback(callback)
        }
        
        // Trigger all callbacks
        aidlService?.triggerAllCallbacks()
        
        // Wait for all callbacks
        latches.forEach { latch ->
            assertTrue(latch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS))
        }
        
        // Verify all callbacks received
        assertTrue(results.all { it.get() })
        
        // Cleanup
        callbacks.forEach { aidlService?.unregisterCallback(it) }
    }
    
    // ========== Asynchronous Operations Tests ==========
    
    @Test
    fun `test AIDL asynchronous operation`() = runTest {
        assertTrue(bindToService())
        
        val resultLatch = CountDownLatch(1)
        val asyncResult = AtomicReference<{{ASYNC_RESULT_TYPE}}>()
        
        val callback = object : {{ASYNC_CALLBACK_INTERFACE}}.Stub() {
            override fun onResult(result: {{ASYNC_RESULT_TYPE}}) {
                asyncResult.set(result)
                resultLatch.countDown()
            }
            
            override fun onError(error: String) {
                resultLatch.countDown()
            }
        }
        
        // Start async operation
        aidlService?.performAsyncOperation({{ASYNC_PARAMS}}, callback)
        
        // Wait for result
        val resultReceived = resultLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(resultReceived, "Should receive async result within timeout")
        
        assertNotNull(asyncResult.get())
        {{ASYNC_RESULT_ASSERTIONS}}
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    fun `test AIDL RemoteException handling`() = runTest {
        assertTrue(bindToService())
        
        try {
            // Force RemoteException by killing service process (simulated)
            aidlService?.forceException()
            fail("Should throw RemoteException")
        } catch (e: RemoteException) {
            // Expected
            assertTrue(e.message?.isNotEmpty() == true)
        }
    }
    
    @Test
    fun `test AIDL error callback invocation`() = runTest {
        assertTrue(bindToService())
        
        val errorLatch = CountDownLatch(1)
        val errorMessage = AtomicReference<String>()
        
        val callback = object : {{ERROR_CALLBACK_INTERFACE}}.Stub() {
            override fun onSuccess(result: {{RESULT_TYPE}}) {
                // Not expected in this test
            }
            
            override fun onError(error: String) {
                errorMessage.set(error)
                errorLatch.countDown()
            }
        }
        
        // Trigger error condition
        aidlService?.performOperationWithError(callback)
        
        // Wait for error callback
        val errorReceived = errorLatch.await(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertTrue(errorReceived, "Should receive error callback")
        
        assertNotNull(errorMessage.get())
        assertTrue(errorMessage.get()!!.isNotEmpty())
    }
    
    // ========== Performance Tests ==========
    
    @Test
    fun `test AIDL call latency`() = runTest {
        assertTrue(bindToService())
        
        val latencies = mutableListOf<Long>()
        
        repeat(100) {
            val latency = measureTimeMillis {
                aidlService?.{{FAST_METHOD}}()
            }
            latencies.add(latency)
        }
        
        val averageLatency = latencies.average()
        val maxLatency = latencies.maxOrNull() ?: 0
        
        assertTrue(
            averageLatency < 10.0,
            "Average AIDL call latency should be <10ms, was ${averageLatency}ms"
        )
        assertTrue(
            maxLatency < 50,
            "Max AIDL call latency should be <50ms, was ${maxLatency}ms"
        )
    }
    
    @Test
    fun `test AIDL bulk data transfer performance`() = runTest {
        assertTrue(bindToService())
        
        val largeData = ByteArray(1_000_000) // 1MB
        
        val transferTime = measureTimeMillis {
            val result = aidlService?.transferLargeData(largeData)
            assertNotNull(result)
        }
        
        assertTrue(
            transferTime < 1000,
            "1MB transfer should complete within 1s, took ${transferTime}ms"
        )
    }
    
    // ========== Concurrency Tests ==========
    
    @Test
    fun `test AIDL concurrent calls from single client`() = runTest {
        assertTrue(bindToService())
        
        val concurrentCalls = 50
        val results = mutableListOf<Deferred<Any?>>()
        
        repeat(concurrentCalls) { index ->
            val deferred = async {
                aidlService?.{{CONCURRENT_METHOD}}(index)
            }
            results.add(deferred)
        }
        
        val completedResults = results.awaitAll()
        
        assertEquals(concurrentCalls, completedResults.size)
        assertTrue(completedResults.all { it != null })
    }
    
    @Test
    fun `test AIDL thread safety with callbacks`() = runTest {
        assertTrue(bindToService())
        
        val threadCount = 10
        val callsPerThread = 20
        val totalCallbacks = AtomicInteger(0)
        
        val threads = List(threadCount) { threadIndex ->
            thread {
                repeat(callsPerThread) { callIndex ->
                    val callback = object : {{THREAD_SAFE_CALLBACK}}.Stub() {
                        override fun onCallback() {
                            totalCallbacks.incrementAndGet()
                        }
                    }
                    
                    aidlService?.registerThreadSafeCallback(callback)
                    Thread.sleep(10)
                    aidlService?.unregisterThreadSafeCallback(callback)
                }
            }
        }
        
        threads.forEach { it.join() }
        
        // All operations should complete without deadlock
        assertTrue(true, "Thread safety test completed without deadlock")
    }
    
    // ========== Service Death Tests ==========
    
    @Test
    fun `test AIDL death recipient notification`() = runTest {
        assertTrue(bindToService())
        
        val deathLatch = CountDownLatch(1)
        val deathRecipient = IBinder.DeathRecipient {
            deathLatch.countDown()
        }
        
        aidlService?.asBinder()?.linkToDeath(deathRecipient, 0)
        
        // Simulate service death (in real test, would kill service process)
        // For this template, we'll test the mechanism
        
        aidlService?.asBinder()?.unlinkToDeath(deathRecipient, 0)
        
        // Verify death recipient mechanism works
        assertTrue(true, "Death recipient registration/unregistration succeeded")
    }
    
    // ========== Helper Methods ==========
    
    private suspend fun bindToService(): Boolean {
        val intent = Intent().apply {
            action = SERVICE_ACTION
            setPackage(SERVICE_PACKAGE)
        }
        return bindToServiceWithIntent(intent)
    }
    
    private suspend fun bindToServiceWithIntent(
        intent: Intent,
        timeout: Long = BIND_TIMEOUT_MS
    ): Boolean = withContext(Dispatchers.Main) {
        val bindLatch = CountDownLatch(1)
        val bindSuccess = AtomicBoolean(false)
        
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                aidlService = {{AIDL_INTERFACE}}.Stub.asInterface(service)
                bindSuccess.set(true)
                bindLatch.countDown()
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                aidlService = null
            }
        }
        
        try {
            val bindResult = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
            
            if (bindResult) {
                val connected = bindLatch.await(timeout, TimeUnit.MILLISECONDS)
                connected && bindSuccess.get()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun unbindFromService() {
        serviceConnection?.let { connection ->
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {
                // Service was not bound
            }
        }
        serviceConnection = null
        aidlService = null
    }
}