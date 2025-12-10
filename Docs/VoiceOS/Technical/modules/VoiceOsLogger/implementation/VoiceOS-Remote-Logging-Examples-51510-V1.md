# VoiceOsLogger Remote Logging - Code Examples

**Last Updated:** 2025-10-09 03:23:40 PDT

## Quick Start Examples

### Example 1: Basic Firebase Setup (Stub)

```kotlin
import com.augmentalis.logger.VoiceOsLogger

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize logger
        VoiceOsLogger.initialize(this)

        // Enable Firebase logging (stub - ready for Firebase SDK)
        VoiceOsLogger.enableFirebaseLogging()

        // Set user context
        VoiceOsLogger.setUserId("user-12345")

        // Log normally - WARN and ERROR go to Firebase
        VoiceOsLogger.w("App", "App started")
        VoiceOsLogger.e("App", "Critical error", RuntimeException("Test"))
    }
}
```

### Example 2: Custom Remote Endpoint

```kotlin
import com.augmentalis.logger.VoiceOsLogger

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize logger
        VoiceOsLogger.initialize(this)

        // Enable custom remote logging
        VoiceOsLogger.enableRemoteLogging(
            endpoint = "https://logs.mycompany.com/api/logs",
            apiKey = "your-secure-api-key"
        )

        // Configure batching
        VoiceOsLogger.configureRemoteBatching(
            intervalMs = 60000,  // Send every 60 seconds
            maxBatchSize = 50     // Max 50 logs per batch
        )

        // Set minimum level (only ERROR sent remotely)
        VoiceOsLogger.setRemoteLogLevel(VoiceOsLogger.Level.ERROR)
    }
}
```

### Example 3: Debug vs Production Configuration

```kotlin
import com.augmentalis.logger.VoiceOsLogger
import android.app.Application

class VoiceOsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize logger
        VoiceOsLogger.initialize(this)

        if (BuildConfig.DEBUG) {
            // Development: All logs to logcat and file, no remote
            VoiceOsLogger.setGlobalLogLevel(VoiceOsLogger.Level.VERBOSE)
            // Remote logging disabled by default
        } else {
            // Production: Only WARN+ to logcat, ERROR to remote
            VoiceOsLogger.setGlobalLogLevel(VoiceOsLogger.Level.WARN)

            // Enable remote logging for production monitoring
            VoiceOsLogger.enableRemoteLogging(
                endpoint = BuildConfig.LOG_ENDPOINT,
                apiKey = BuildConfig.LOG_API_KEY
            )

            // Only send critical errors remotely
            VoiceOsLogger.setRemoteLogLevel(VoiceOsLogger.Level.ERROR)

            // Set user ID for tracking (use non-PII identifier)
            val userId = getUserIdFromPreferences() // e.g., hashed user ID
            VoiceOsLogger.setUserId(userId)

            // Set custom context
            VoiceOsLogger.setCustomKey("environment", "production")
            VoiceOsLogger.setCustomKey("device_type", getDeviceType())
        }
    }

    private fun getUserIdFromPreferences(): String {
        // Implementation to get user ID
        return "user-12345"
    }

    private fun getDeviceType(): String {
        return if (isTablet()) "tablet" else "phone"
    }

    private fun isTablet(): Boolean {
        // Check if device is tablet
        return false
    }
}
```

## Real-World Use Cases

### Use Case 1: Voice Command Processing with Remote Logging

```kotlin
import com.augmentalis.logger.VoiceOsLogger

class VoiceCommandProcessor(private val context: Context) {

    init {
        // Tag this feature for remote filtering
        VoiceOsLogger.setCustomKey("feature", "voice-commands")
    }

    fun processCommand(command: String) {
        // Debug logs (not sent remotely by default)
        VoiceOsLogger.d(TAG, "Processing command: $command")

        try {
            // Validate command
            validateCommand(command)

            // Execute command
            val result = executeCommand(command)

            // Info logs (not sent remotely by default)
            VoiceOsLogger.i(TAG, "Command executed successfully: $result")

        } catch (e: IllegalArgumentException) {
            // Warning - may be sent remotely depending on config
            VoiceOsLogger.w(TAG, "Invalid command: $command - ${e.message}")
        } catch (e: Exception) {
            // Error with exception - sent remotely immediately
            VoiceOsLogger.e(TAG, "Command processing failed: $command", e)

            // Additional context for remote debugging
            VoiceOsLogger.setCustomKey("failed_command", command)
            VoiceOsLogger.setCustomKey("error_type", e.javaClass.simpleName)
        }
    }

    private fun validateCommand(command: String) {
        if (command.isBlank()) {
            throw IllegalArgumentException("Command cannot be blank")
        }
    }

    private fun executeCommand(command: String): String {
        // Implementation
        return "success"
    }

    companion object {
        private const val TAG = "VoiceCommandProcessor"
    }
}
```

### Use Case 2: Network Request Logging

```kotlin
import com.augmentalis.logger.VoiceOsLogger
import kotlinx.coroutines.*

class ApiClient {

    suspend fun fetchData(endpoint: String): Result<Data> = withContext(Dispatchers.IO) {
        VoiceOsLogger.d(TAG, "Fetching data from: $endpoint")
        VoiceOsLogger.startTiming("api_fetch_$endpoint")

        try {
            // Simulate API call
            delay(1000)
            val data = performNetworkRequest(endpoint)

            VoiceOsLogger.endTiming("api_fetch_$endpoint")
            VoiceOsLogger.i(TAG, "Data fetched successfully from: $endpoint")

            Result.success(data)

        } catch (e: java.net.SocketTimeoutException) {
            VoiceOsLogger.endTiming("api_fetch_$endpoint")
            // Timeout - sent to remote for monitoring
            VoiceOsLogger.e(TAG, "Network timeout for: $endpoint", e)
            Result.failure(e)

        } catch (e: Exception) {
            VoiceOsLogger.endTiming("api_fetch_$endpoint")
            // Generic error - sent to remote
            VoiceOsLogger.e(TAG, "Failed to fetch data from: $endpoint", e)
            Result.failure(e)
        }
    }

    private fun performNetworkRequest(endpoint: String): Data {
        // Implementation
        return Data()
    }

    data class Data(val value: String = "test")

    companion object {
        private const val TAG = "ApiClient"
    }
}
```

### Use Case 3: App Lifecycle Logging

```kotlin
import com.augmentalis.logger.VoiceOsLogger
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class VoiceOsActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        VoiceOsLogger.d(TAG, "Activity created: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        VoiceOsLogger.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
    }

    override fun onActivityResumed(activity: Activity) {
        VoiceOsLogger.i(TAG, "Activity resumed: ${activity.javaClass.simpleName}")
        // Track current screen for remote context
        VoiceOsLogger.setCustomKey("current_screen", activity.javaClass.simpleName)
    }

    override fun onActivityPaused(activity: Activity) {
        VoiceOsLogger.d(TAG, "Activity paused: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStopped(activity: Activity) {
        VoiceOsLogger.d(TAG, "Activity stopped: ${activity.javaClass.simpleName}")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        VoiceOsLogger.d(TAG, "Activity saving state: ${activity.javaClass.simpleName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        VoiceOsLogger.d(TAG, "Activity destroyed: ${activity.javaClass.simpleName}")
    }

    companion object {
        private const val TAG = "ActivityLifecycle"
    }
}

// Register in Application
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        VoiceOsLogger.initialize(this)

        // Register lifecycle callbacks
        registerActivityLifecycleCallbacks(VoiceOsActivityLifecycleCallbacks())
    }
}
```

### Use Case 4: Crash Recovery with Remote Logging

```kotlin
import com.augmentalis.logger.VoiceOsLogger
import kotlinx.coroutines.*

class CrashHandler : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            // Log the crash with full context
            VoiceOsLogger.e(TAG, "UNCAUGHT EXCEPTION in thread: ${thread.name}", exception)

            // Add crash context
            VoiceOsLogger.setCustomKey("crash_thread", thread.name)
            VoiceOsLogger.setCustomKey("crash_time", System.currentTimeMillis().toString())

            // Flush logs immediately before app dies
            runBlocking {
                VoiceOsLogger.flushRemoteLogs()
                // Give it a moment to send
                delay(2000)
            }

        } catch (e: Exception) {
            // Don't crash in crash handler
            android.util.Log.e(TAG, "Failed to log crash", e)
        } finally {
            // Call default handler
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    companion object {
        private const val TAG = "CrashHandler"

        fun install() {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        }
    }
}

// Install in Application
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        VoiceOsLogger.initialize(this)
        VoiceOsLogger.enableRemoteLogging(
            endpoint = "https://logs.myapp.com/api/logs",
            apiKey = "your-api-key"
        )

        // Install crash handler
        CrashHandler.install()
    }
}
```

### Use Case 5: Performance Monitoring with Remote Alerts

```kotlin
import com.augmentalis.logger.VoiceOsLogger

class PerformanceMonitor {

    fun monitorOperation(operationName: String, block: () -> Unit) {
        val startTime = System.currentTimeMillis()
        VoiceOsLogger.startTiming(operationName)

        try {
            block()
        } finally {
            VoiceOsLogger.endTiming(operationName)
            val duration = System.currentTimeMillis() - startTime

            // Alert if operation is too slow
            if (duration > SLOW_OPERATION_THRESHOLD) {
                VoiceOsLogger.w(
                    TAG,
                    "SLOW OPERATION: $operationName took ${duration}ms"
                )
            }

            // Critical alert if extremely slow
            if (duration > CRITICAL_THRESHOLD) {
                VoiceOsLogger.e(
                    TAG,
                    "CRITICAL SLOW OPERATION: $operationName took ${duration}ms"
                )
                // This will be sent to remote immediately
            }
        }
    }

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val SLOW_OPERATION_THRESHOLD = 1000 // 1 second
        private const val CRITICAL_THRESHOLD = 5000 // 5 seconds
    }
}

// Usage
class MyService {
    private val perfMonitor = PerformanceMonitor()

    fun processLargeDataset(data: List<Data>) {
        perfMonitor.monitorOperation("process_large_dataset") {
            // Processing logic
            data.forEach { item ->
                processItem(item)
            }
        }
    }

    private fun processItem(item: Data) {
        // Implementation
    }

    data class Data(val value: String = "test")
}
```

## Testing Remote Logging

### Integration Test with MockWebServer

```kotlin
import com.augmentalis.logger.VoiceOsLogger
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RemoteLoggingIntegrationTest {

    private lateinit var mockServer: MockWebServer

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun testRemoteLoggingSendsToEndpoint() {
        // Enqueue successful response
        mockServer.enqueue(MockResponse().setResponseCode(200))

        // Initialize logger with mock endpoint
        val endpoint = mockServer.url("/logs").toString()
        VoiceOsLogger.enableRemoteLogging(endpoint, "test-api-key")

        // Log an error
        VoiceOsLogger.e("TestTag", "Test error message", RuntimeException("Test"))

        // Wait for immediate send (errors with exceptions send immediately)
        Thread.sleep(1000)

        // Verify request was made
        val request = mockServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("POST", request.method)
        assertEquals("Bearer test-api-key", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("Test error message"))
    }
}
```

### Manual Testing Checklist

```kotlin
// Manual test script for remote logging
fun testRemoteLogging(context: Context) {
    println("=== VoiceOsLogger Remote Logging Test ===")

    // 1. Initialize
    VoiceOsLogger.initialize(context)
    println("✓ Logger initialized")

    // 2. Enable remote logging
    VoiceOsLogger.enableRemoteLogging(
        endpoint = "https://your-test-endpoint.com/logs",
        apiKey = "test-key"
    )
    println("✓ Remote logging enabled")

    // 3. Check status
    val status = VoiceOsLogger.getRemoteLoggingStatus()
    println("Status: $status")

    // 4. Test different log levels
    VoiceOsLogger.v("Test", "Verbose - should NOT be sent")
    VoiceOsLogger.d("Test", "Debug - should NOT be sent")
    VoiceOsLogger.i("Test", "Info - should NOT be sent")
    VoiceOsLogger.w("Test", "Warning - should be sent")
    VoiceOsLogger.e("Test", "Error - should be sent", RuntimeException("Test"))
    println("✓ Logged at all levels")

    // 5. Check queue
    val queueSize = VoiceOsLogger.getRemoteLoggingStatus()["pending_logs"]
    println("Queue size: $queueSize")

    // 6. Test flush
    runBlocking {
        VoiceOsLogger.flushRemoteLogs()
    }
    println("✓ Logs flushed")

    // 7. Disable
    VoiceOsLogger.disableRemoteLogging()
    println("✓ Remote logging disabled")

    println("=== Test Complete ===")
}
```

## Server-Side Implementation Example

### Node.js Express Endpoint

```javascript
// Example server endpoint for receiving logs
const express = require('express');
const app = express();

app.use(express.json({ limit: '10mb' }));

// Middleware to verify API key
function verifyApiKey(req, res, next) {
    const apiKey = req.headers.authorization?.replace('Bearer ', '');

    if (apiKey !== process.env.LOG_API_KEY) {
        return res.status(401).json({ error: 'Invalid API key' });
    }

    next();
}

// Log endpoint
app.post('/api/logs', verifyApiKey, (req, res) => {
    const { logs, batch_size, immediate, device_info, app_info } = req.body;

    console.log(`Received ${batch_size} logs from ${device_info.model}`);

    // Process each log entry
    logs.forEach(log => {
        const { timestamp, level, tag, message, stackTrace } = log;

        // Store in database
        storeLog({
            timestamp: new Date(timestamp),
            level,
            tag,
            message,
            stackTrace,
            deviceInfo: device_info,
            appInfo: app_info,
            immediate
        });

        // Alert on critical errors
        if (level === 'ERROR' && stackTrace) {
            alertOnCriticalError(log, device_info);
        }
    });

    res.json({ success: true, received: batch_size });
});

function storeLog(log) {
    // Store in your database (MongoDB, PostgreSQL, etc.)
    console.log('Storing log:', log);
}

function alertOnCriticalError(log, deviceInfo) {
    // Send alert to monitoring system (PagerDuty, Slack, etc.)
    console.log('ALERT: Critical error:', log);
}

app.listen(3000, () => {
    console.log('Log server running on port 3000');
});
```

## Best Practices

### 1. Don't Log Sensitive Data

```kotlin
// BAD - Don't log sensitive data
VoiceOsLogger.e("Auth", "Login failed for user: ${email}, password: ${password}")

// GOOD - Log without sensitive data
VoiceOsLogger.e("Auth", "Login failed for user ID: ${hashedUserId}")
```

### 2. Use Appropriate Log Levels

```kotlin
// BAD - Using ERROR for non-errors
VoiceOsLogger.e("UI", "Button clicked")

// GOOD - Use appropriate levels
VoiceOsLogger.d("UI", "Button clicked") // Debug
VoiceOsLogger.i("UI", "Screen loaded") // Info
VoiceOsLogger.w("UI", "Deprecated API used") // Warning
VoiceOsLogger.e("UI", "Failed to load data", exception) // Error
```

### 3. Add Context Before Errors

```kotlin
// GOOD - Add context before logging critical errors
try {
    processPayment(amount)
} catch (e: Exception) {
    VoiceOsLogger.setCustomKey("payment_amount", amount.toString())
    VoiceOsLogger.setCustomKey("user_id", userId)
    VoiceOsLogger.e("Payment", "Payment processing failed", e)
}
```

### 4. Flush on App Shutdown

```kotlin
class MyApplication : Application() {
    override fun onTerminate() {
        super.onTerminate()

        // Ensure all logs are sent before shutdown
        runBlocking {
            VoiceOsLogger.flushRemoteLogs()
        }
    }
}
```

---

**Version:** 2.0.0
**Last Updated:** 2025-10-09 03:23:40 PDT
