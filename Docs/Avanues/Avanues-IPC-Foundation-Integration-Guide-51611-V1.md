# IPC Foundation Integration Guide

**Version:** 1.0.0
**Created:** 2025-11-10
**Last Updated:** 2025-11-10
**Status:** Complete

---

## Overview

This guide demonstrates how to integrate the **IPC Foundation** (ARGScanner, VoiceCommandRouter, IPCConnector) to enable seamless communication between VoiceOS and Avanue apps.

**IPC Foundation Modules:**
1. **ARGScanner** - Discovers available services and capabilities
2. **VoiceCommandRouter** - Routes voice commands to appropriate handlers
3. **IPCConnector** - Manages cross-process service connections

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         VoiceOS App                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Voice Command Processor                      │  │
│  │  "Open bookmark"  → VoiceCommandRouter → Browser Handler │  │
│  │  "Start recording" → VoiceCommandRouter → Notes Handler  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  IPC Connector                            │  │
│  │  - Connection Management                                  │  │
│  │  - Circuit Breaker                                        │  │
│  │  - Rate Limiting                                          │  │
│  │  - Automatic Reconnection                                │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  ARG Scanner                              │  │
│  │  - Scans AndroidManifest.xml                             │  │
│  │  - Discovers available services                           │  │
│  │  - Builds capability registry                             │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ AIDL/ContentProvider
┌─────────────────────────────────────────────────────────────────┐
│                     Avanue Apps (Browser, Notes, AI)            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Service Endpoints                            │  │
│  │  - AIDL Services (internal modules)                      │  │
│  │  - ContentProviders (external plugins)                   │  │
│  │  - Capabilities declared in manifest                     │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Step 1: Declare Services (In Avanue Apps)

### Example: BrowserAvanue Service Declaration

**File:** `android/apps/browseravanue/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.augmentalis.avanue.browser">

    <!-- Declare browser service -->
    <service
        android:name=".service.BrowserService"
        android:exported="true"
        android:permission="com.augmentalis.voiceos.VOICE_SERVICE">

        <intent-filter>
            <action android:name="com.augmentalis.avanue.BROWSER_SERVICE" />
        </intent-filter>

        <!-- ARG Scanner metadata -->
        <meta-data
            android:name="avamagic.service.type"
            android:value="browser" />
        <meta-data
            android:name="avamagic.service.version"
            android:value="1.0" />
        <meta-data
            android:name="avamagic.service.aidl"
            android:value="com.augmentalis.avanue.browser.IBrowserService" />
        <meta-data
            android:name="avamagic.service.capabilities"
            android:value="navigation,bookmarks,tabs,downloads" />
    </service>

    <!-- Declare browser content provider (for bookmarks) -->
    <provider
        android:name=".provider.BrowserContentProvider"
        android:authorities="com.augmentalis.avanue.browser"
        android:exported="true"
        android:readPermission="com.augmentalis.avanue.READ_BOOKMARKS"
        android:writePermission="com.augmentalis.avanue.WRITE_BOOKMARKS">

        <meta-data
            android:name="avamagic.provider.type"
            android:value="bookmarks" />
        <meta-data
            android:name="avamagic.provider.version"
            android:value="1.0" />
    </provider>

</manifest>
```

---

## Step 2: Scan for Services (In VoiceOS)

### Initialize ARGScanner

**File:** `VoiceOSApp.kt`

```kotlin
class VoiceOSApp : Application() {
    lateinit var argScanner: ARGScanner
    lateinit var registry: ARGRegistry

    override fun onCreate() {
        super.onCreate()

        // Initialize ARGScanner
        argScanner = ARGScanner()

        // Scan for services on all installed packages
        val scanResult = argScanner.scanAllPackages(this)

        registry = when (scanResult) {
            is ScanResult.Success -> {
                Log.i("VoiceOS", "Found ${scanResult.registry.getAllServices().size} services")
                scanResult.registry
            }
            is ScanResult.Error -> {
                Log.e("VoiceOS", "Scan failed: ${scanResult.error}")
                ARGRegistry()  // Empty registry
            }
        }
    }
}
```

### Query Available Services

```kotlin
class ServiceDiscovery(private val registry: ARGRegistry) {

    fun findBrowserService(): ServiceEndpoint? {
        // Find browser service
        val services = registry.getServicesByType("browser")
        return services.firstOrNull()
    }

    fun findAllCapabilities(): Map<String, List<String>> {
        return registry.getAllServices().associate { service ->
            service.id to service.capabilities
        }
    }

    fun supportsBookmarks(packageName: String): Boolean {
        val services = registry.getServicesByPackage(packageName)
        return services.any { "bookmarks" in it.capabilities }
    }
}
```

---

## Step 3: Route Voice Commands (VoiceCommandRouter)

### Setup Command Router

```kotlin
class VoiceOSCommandManager(
    private val registry: ARGRegistry,
    private val ipcConnector: ConnectionManager
) {
    private val router = VoiceCommandRouter(registry)

    init {
        setupRoutes()
    }

    private fun setupRoutes() {
        // Register browser commands
        router.registerCommand(
            pattern = "open (.+)",
            targetCapability = "navigation",
            handler = ::handleOpenUrl
        )

        router.registerCommand(
            pattern = "bookmark this",
            targetCapability = "bookmarks",
            handler = ::handleAddBookmark
        )

        // Register notes commands
        router.registerCommand(
            pattern = "create note (.+)",
            targetCapability = "notes",
            handler = ::handleCreateNote
        )

        // Register AI commands
        router.registerCommand(
            pattern = "ask ai (.+)",
            targetCapability = "ai",
            handler = ::handleAIQuery
        )
    }

    suspend fun processCommand(command: String): CommandResult {
        return router.route(command)
    }

    private suspend fun handleOpenUrl(params: Map<String, Any>): Boolean {
        val url = params["1"] as? String ?: return false

        // Find browser service
        val browserService = registry.getServicesByType("browser").firstOrNull()
            ?: return false

        // Connect and invoke
        val connection = ipcConnector.connect(browserService)
        if (connection is ConnectionResult.Success) {
            val invocation = MethodInvocation(
                methodName = "navigateToUrl",
                parameters = mapOf("url" to url)
            )
            val result = ipcConnector.invoke(connection.connection.id, invocation)
            return result is MethodResult.Success
        }

        return false
    }

    private suspend fun handleAddBookmark(params: Map<String, Any>): Boolean {
        // Implementation for adding bookmark
        return true
    }

    private suspend fun handleCreateNote(params: Map<String, Any>): Boolean {
        // Implementation for creating note
        return true
    }

    private suspend fun handleAIQuery(params: Map<String, Any>): Boolean {
        // Implementation for AI query
        return true
    }
}
```

---

## Step 4: Connect to Services (IPCConnector)

### Initialize IPC Connector

```kotlin
class VoiceOSIPCManager(
    private val context: Context,
    private val registry: ARGRegistry
) {
    private val connectionManager = ConnectionManager(
        registry = registry,
        reconnectionPolicy = ReconnectionPolicy(
            enabled = true,
            maxRetries = 3,
            initialDelayMs = 1000,
            maxDelayMs = 30000,
            backoffMultiplier = 2.0f
        ),
        circuitBreakerConfig = CircuitBreakerConfig(
            failureThreshold = 5,
            successThreshold = 2,
            timeoutMs = 30000
        ),
        rateLimitConfig = RateLimitConfig(
            maxRequestsPerSecond = 10,
            burstSize = 20
        )
    )

    suspend fun connectToBrowser(): ConnectionResult {
        val browserService = registry.getServicesByType("browser").firstOrNull()
            ?: return ConnectionResult.Error(
                IPCError.ServiceNotFound("browser")
            )

        return connectionManager.connect(browserService)
    }

    suspend fun invokeBrowserMethod(
        methodName: String,
        params: Map<String, Any>
    ): MethodResult {
        val connection = connectToBrowser()

        return when (connection) {
            is ConnectionResult.Success -> {
                val invocation = MethodInvocation(
                    methodName = methodName,
                    parameters = params,
                    timeoutMs = 5000
                )
                connectionManager.invoke(connection.connection.id, invocation)
            }
            is ConnectionResult.Error -> {
                MethodResult.Error(connection.error)
            }
            else -> MethodResult.Error(IPCError.ServiceUnavailable("Connection pending"))
        }
    }

    fun cleanup() {
        connectionManager.shutdown()
    }
}
```

---

## Step 5: Complete Integration Example

### VoiceOS Main Activity

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var argScanner: ARGScanner
    private lateinit var registry: ARGRegistry
    private lateinit var commandManager: VoiceOSCommandManager
    private lateinit var ipcManager: VoiceOSIPCManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize IPC Foundation
        setupIPCFoundation()

        setContent {
            VoiceOSTheme {
                VoiceOSScreen(
                    onCommand = ::handleVoiceCommand
                )
            }
        }
    }

    private fun setupIPCFoundation() {
        // Step 1: Scan for services
        argScanner = ARGScanner()
        val scanResult = argScanner.scanAllPackages(this)

        registry = when (scanResult) {
            is ScanResult.Success -> scanResult.registry
            is ScanResult.Error -> ARGRegistry()
        }

        // Step 2: Initialize IPC manager
        ipcManager = VoiceOSIPCManager(this, registry)

        // Step 3: Initialize command manager
        val connectionManager = ConnectionManager(
            registry = registry,
            reconnectionPolicy = ReconnectionPolicy(),
            circuitBreakerConfig = CircuitBreakerConfig(),
            rateLimitConfig = RateLimitConfig()
        )
        commandManager = VoiceOSCommandManager(registry, connectionManager)

        // Log discovered services
        val services = registry.getAllServices()
        Log.i("VoiceOS", "Discovered ${services.size} services:")
        services.forEach { service ->
            Log.i("VoiceOS", "  - ${service.id}: ${service.capabilities.joinToString()}")
        }
    }

    private fun handleVoiceCommand(command: String) {
        lifecycleScope.launch {
            val result = commandManager.processCommand(command)

            when (result) {
                is CommandResult.Success -> {
                    Log.i("VoiceOS", "Command executed: $command")
                    showToast("Done!")
                }
                is CommandResult.NoMatch -> {
                    Log.w("VoiceOS", "No handler for: $command")
                    showToast("I don't understand that command")
                }
                is CommandResult.Error -> {
                    Log.e("VoiceOS", "Command failed: ${result.error}")
                    showToast("Sorry, that didn't work")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ipcManager.cleanup()
    }
}
```

---

## Common Integration Patterns

### Pattern 1: Lazy Service Connection

```kotlin
class LazyBrowserClient(
    private val connectionManager: ConnectionManager,
    private val browserEndpoint: ServiceEndpoint
) {
    private var connectionId: String? = null

    private suspend fun ensureConnected(): String? {
        if (connectionId != null && connectionManager.isConnected(connectionId!!)) {
            return connectionId
        }

        val result = connectionManager.connect(browserEndpoint)
        return when (result) {
            is ConnectionResult.Success -> {
                connectionId = result.connection.id
                connectionId
            }
            else -> null
        }
    }

    suspend fun openUrl(url: String): Boolean {
        val connId = ensureConnected() ?: return false

        val invocation = MethodInvocation("navigateToUrl", mapOf("url" to url))
        val result = connectionManager.invoke(connId, invocation)

        return result is MethodResult.Success
    }
}
```

---

### Pattern 2: Connection Pool

```kotlin
class ConnectionPool(
    private val connectionManager: ConnectionManager,
    private val registry: ARGRegistry
) {
    private val connections = ConcurrentHashMap<String, String>()

    suspend fun getOrConnect(serviceType: String): String? {
        // Check if already connected
        connections[serviceType]?.let { connId ->
            if (connectionManager.isConnected(connId)) {
                return connId
            } else {
                connections.remove(serviceType)
            }
        }

        // Find and connect to service
        val endpoint = registry.getServicesByType(serviceType).firstOrNull()
            ?: return null

        val result = connectionManager.connect(endpoint)
        return when (result) {
            is ConnectionResult.Success -> {
                connections[serviceType] = result.connection.id
                result.connection.id
            }
            else -> null
        }
    }

    suspend fun invoke(
        serviceType: String,
        methodName: String,
        params: Map<String, Any>
    ): MethodResult {
        val connId = getOrConnect(serviceType)
            ?: return MethodResult.Error(IPCError.ServiceNotFound(serviceType))

        val invocation = MethodInvocation(methodName, params)
        return connectionManager.invoke(connId, invocation)
    }

    fun cleanup() {
        connections.values.forEach { connId ->
            runBlocking {
                connectionManager.disconnect(connId)
            }
        }
        connections.clear()
    }
}
```

---

### Pattern 3: Command Pipeline

```kotlin
class CommandPipeline(
    private val router: VoiceCommandRouter,
    private val connectionPool: ConnectionPool
) {
    suspend fun execute(command: String): PipelineResult {
        // Step 1: Route command
        val routeResult = router.route(command)

        if (routeResult !is CommandResult.Success) {
            return PipelineResult.Failed("Routing failed")
        }

        // Step 2: Extract target and method
        val serviceType = routeResult.targetService.type
        val methodName = extractMethodName(command)
        val params = extractParams(command)

        // Step 3: Invoke via connection pool
        val invokeResult = connectionPool.invoke(serviceType, methodName, params)

        return when (invokeResult) {
            is MethodResult.Success -> {
                PipelineResult.Success(invokeResult.result)
            }
            is MethodResult.Error -> {
                PipelineResult.Failed(invokeResult.error.toString())
            }
        }
    }

    private fun extractMethodName(command: String): String {
        // Parse command to extract method name
        return when {
            command.startsWith("open") -> "navigateToUrl"
            command.startsWith("bookmark") -> "addBookmark"
            command.startsWith("create note") -> "createNote"
            else -> "unknown"
        }
    }

    private fun extractParams(command: String): Map<String, Any> {
        // Parse command to extract parameters
        return emptyMap()
    }
}

sealed class PipelineResult {
    data class Success(val result: Any) : PipelineResult()
    data class Failed(val reason: String) : PipelineResult()
}
```

---

## Error Handling

### Comprehensive Error Handling

```kotlin
class RobustIPCClient(
    private val connectionManager: ConnectionManager,
    private val endpoint: ServiceEndpoint
) {
    suspend fun invokeWithRetry(
        methodName: String,
        params: Map<String, Any>,
        maxRetries: Int = 3
    ): Result<Any> {
        repeat(maxRetries) { attempt ->
            // Connect
            val connectionResult = connectionManager.connect(endpoint)

            when (connectionResult) {
                is ConnectionResult.Success -> {
                    // Invoke
                    val invocation = MethodInvocation(methodName, params)
                    val methodResult = connectionManager.invoke(
                        connectionResult.connection.id,
                        invocation
                    )

                    when (methodResult) {
                        is MethodResult.Success -> {
                            return Result.success(methodResult.result)
                        }
                        is MethodResult.Error -> {
                            when (methodResult.error) {
                                is IPCError.ServiceUnavailable,
                                is IPCError.Timeout,
                                is IPCError.NetworkFailure -> {
                                    // Transient error - retry
                                    delay(1000L * (attempt + 1))
                                    continue
                                }
                                is IPCError.PermissionDenied,
                                is IPCError.ServiceNotFound -> {
                                    // Permanent error - don't retry
                                    return Result.failure(Exception(methodResult.error.toString()))
                                }
                                else -> {
                                    return Result.failure(Exception(methodResult.error.toString()))
                                }
                            }
                        }
                    }
                }
                is ConnectionResult.Error -> {
                    return Result.failure(Exception(connectionResult.error.toString()))
                }
                else -> {
                    delay(1000L * (attempt + 1))
                }
            }
        }

        return Result.failure(Exception("Max retries exceeded"))
    }
}
```

---

## Testing

### Unit Test Example

```kotlin
class IPCIntegrationTest {
    private lateinit var registry: ARGRegistry
    private lateinit var connectionManager: ConnectionManager
    private lateinit var router: VoiceCommandRouter

    @Before
    fun setup() {
        // Create mock registry
        registry = ARGRegistry()

        // Add mock services
        val browserService = ServiceEndpoint(
            id = "com.augmentalis.avanue.browser",
            type = "browser",
            version = "1.0",
            aidlInterface = "com.augmentalis.avanue.browser.IBrowserService",
            capabilities = listOf("navigation", "bookmarks")
        )
        registry.registerService(browserService)

        // Initialize connection manager
        connectionManager = ConnectionManager(registry)

        // Initialize router
        router = VoiceCommandRouter(registry)
        router.registerCommand(
            pattern = "open (.+)",
            targetCapability = "navigation",
            handler = { true }
        )
    }

    @Test
    fun testCommandRouting() = runBlocking {
        val result = router.route("open google.com")

        assertTrue(result is CommandResult.Success)
        assertEquals("browser", (result as CommandResult.Success).targetService.type)
    }

    @Test
    fun testServiceConnection() = runBlocking {
        val service = registry.getServicesByType("browser").first()
        val result = connectionManager.connect(service)

        assertTrue(result is ConnectionResult.Success)
    }
}
```

---

## Performance Considerations

### Optimization Tips

1. **Reuse Connections**: Don't connect/disconnect for every command
2. **Connection Pooling**: Maintain pool of active connections
3. **Async Invocation**: Use coroutines for non-blocking calls
4. **Circuit Breaker**: Prevents wasting resources on failing services
5. **Rate Limiting**: Protects services from abuse
6. **Caching**: Cache ARGRegistry scan results (re-scan on package install/update)

---

## Version History

- **v1.0.0** (2025-11-10): Initial integration guide

---

**Created by Manoj Jhawar, manoj@ideahq.net**
