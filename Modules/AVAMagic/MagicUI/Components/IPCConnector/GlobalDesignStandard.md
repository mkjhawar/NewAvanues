# Global Design Standard: IPC Connector

**Version:** 1.1.0
**Last Updated:** 2025-11-10
**Status:** Active
**Applies To:** VoiceOS IPC Communication Layer

---

## References to Global Standards

This module-specific design standard extends the following global standards:

1. **[IPC Architecture](../../../../GlobalDesignStandards/GlobalDesignStandard-IPC-Architecture.md)**
   - Provides foundation for all IPC mechanisms
   - Defines decision tree for choosing IPC type
   - Establishes error handling and threading patterns

2. **[Module Structure](../../../../GlobalDesignStandards/GlobalDesignStandard-Module-Structure.md)**
   - Defines canonical directory layout
   - Specifies layer responsibilities
   - Establishes expect/actual patterns

**This document** adds IPCConnector-specific design decisions not covered by global standards, including:
- Circuit breaker state machine implementation
- Rate limiter token bucket algorithm
- Connection lifecycle management
- Reconnection policy with exponential backoff

---

## 1. Architecture Principles

### 1.1 Separation of Concerns
- **Connection Management**: Separate layer for lifecycle (connect, disconnect, reconnect)
- **Protocol Handling**: Separate implementations for AIDL, Content Provider, WebSocket
- **Error Recovery**: Centralized retry and fallback logic
- **Thread Safety**: All public APIs must be thread-safe

### 1.2 Platform Abstraction
```kotlin
// Common interface
expect class ServiceConnector {
    fun connect(endpoint: ServiceEndpoint): ConnectionResult
    fun disconnect(connectionId: String)
}

// Platform-specific implementations
actual class ServiceConnector // Android AIDL
actual class ServiceConnector // iOS XPC
actual class ServiceConnector // Windows Named Pipes
```

### 1.3 Dependency Direction
```
IPCConnector
    ↓
ARGScanner (for endpoint discovery)
    ↓
Components:Core (base types)
```

**Rule**: Never depend on higher-level modules (VoiceCommandRouter, UI, Apps)

---

## 2. Connection Lifecycle

### 2.1 State Machine
```
DISCONNECTED → CONNECTING → CONNECTED → DISCONNECTING → DISCONNECTED
                    ↓             ↓
                  FAILED      RECONNECTING
```

### 2.2 State Transitions
- **DISCONNECTED**: Initial state, ready to connect
- **CONNECTING**: Binding to service/opening provider
- **CONNECTED**: Active connection, can invoke methods
- **FAILED**: Connection attempt failed, waiting for retry
- **RECONNECTING**: Automatic retry in progress
- **DISCONNECTING**: Graceful shutdown in progress

### 2.3 Automatic Reconnection
```kotlin
data class ReconnectionPolicy(
    val enabled: Boolean = true,
    val maxRetries: Int = 3,
    val initialDelay: Long = 1000,      // 1s
    val maxDelay: Long = 30000,         // 30s
    val backoffMultiplier: Float = 2.0f // Exponential backoff
)
```

**Rule**: Always use exponential backoff to prevent overwhelming services

---

## 3. Error Handling

### 3.1 Error Categories
```kotlin
sealed class IPCError {
    // Transient errors (retry possible)
    data class ServiceUnavailable(val reason: String) : IPCError()
    data class Timeout(val durationMs: Long) : IPCError()
    data class NetworkFailure(val cause: Throwable) : IPCError()

    // Permanent errors (retry not possible)
    data class PermissionDenied(val permission: String) : IPCError()
    data class ServiceNotFound(val packageName: String) : IPCError()
    data class InvalidResponse(val details: String) : IPCError()

    // Security errors
    data class AuthenticationFailed(val reason: String) : IPCError()
    data class SignatureVerification(val packageName: String) : IPCError()
}
```

### 3.2 Error Recovery Strategy
| Error Type | Action | Retry? | User Notification? |
|------------|--------|--------|-------------------|
| ServiceUnavailable | Wait + Retry | ✅ Yes (exponential backoff) | ❌ No (unless max retries) |
| Timeout | Retry with longer timeout | ✅ Yes (3 attempts) | ⚠️ After 2nd failure |
| PermissionDenied | Prompt user | ❌ No | ✅ Immediate |
| ServiceNotFound | Check registry | ❌ No | ✅ Immediate |
| AuthenticationFailed | Re-authenticate | ⚠️ Once | ✅ Immediate |

### 3.3 Circuit Breaker Pattern
```kotlin
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,          // Failures before open
    val successThreshold: Int = 2,          // Successes to close
    val timeout: Long = 60000,              // 60s before half-open
)

// States: CLOSED (normal) → OPEN (failing) → HALF_OPEN (testing) → CLOSED
```

**Rule**: Prevent cascading failures by opening circuit after repeated errors

---

## 4. Thread Safety

### 4.1 Concurrency Model
- **Main Thread**: UI callbacks, lifecycle events
- **Background Thread**: Connection establishment, network I/O
- **IPC Thread**: Service binder calls (Android only)

### 4.2 Thread Safety Requirements
```kotlin
class ServiceConnector {
    private val connections = ConcurrentHashMap<String, Connection>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // All public methods must be thread-safe
    @ThreadSafe
    suspend fun connect(endpoint: ServiceEndpoint): ConnectionResult {
        mutex.withLock {
            // Thread-safe implementation
        }
    }
}
```

### 4.3 Synchronization Rules
- **Mutex**: Use `Mutex` for Kotlin coroutines
- **Atomic**: Use `AtomicReference` for single values
- **ConcurrentHashMap**: Use for connection registry
- **StateFlow**: Use for observable state

**Rule**: Never block main thread for >16ms (1 frame @ 60fps)

---

## 5. Security Considerations

### 5.1 Package Signature Verification
```kotlin
fun verifyPackageSignature(packageName: String): Boolean {
    val signatures = packageManager.getPackageInfo(packageName, GET_SIGNATURES)
    val expectedHash = trustedSignatures[packageName] ?: return false
    val actualHash = signatures.signatures[0].toByteArray().sha256()
    return actualHash.contentEquals(expectedHash)
}
```

**Rule**: Always verify package signatures before connecting to services

### 5.2 Permission Checks
```kotlin
// Before connecting to AIDL service
if (!hasPermission(endpoint.requiresPermission)) {
    return ConnectionResult.Error(IPCError.PermissionDenied(endpoint.requiresPermission))
}
```

### 5.3 Data Sanitization
```kotlin
// Before sending parameters
fun sanitizeParameter(param: String): String {
    return param
        .replace(Regex("[<>\"']"), "")  // Prevent injection
        .take(MAX_PARAM_LENGTH)          // Prevent DoS
}
```

### 5.4 Rate Limiting
```kotlin
data class RateLimitConfig(
    val maxRequestsPerSecond: Int = 10,
    val burstSize: Int = 20
)
```

**Rule**: Protect services from abuse with token bucket rate limiting

---

## 6. Performance Requirements

### 6.1 Latency Targets
| Operation | Target | Maximum | Notes |
|-----------|--------|---------|-------|
| Connect | <100ms | 500ms | Including service binding |
| Method Call | <10ms | 100ms | Synchronous AIDL call |
| Query | <50ms | 200ms | Content Provider query |
| Disconnect | <50ms | 200ms | Graceful unbind |

### 6.2 Resource Limits
```kotlin
data class ResourceLimits(
    val maxConnections: Int = 32,           // Per app
    val maxMessageSize: Int = 1_048_576,    // 1MB
    val connectionTimeout: Long = 5000,     // 5s
    val methodTimeout: Long = 10000,        // 10s
    val queryTimeout: Long = 5000           // 5s
)
```

### 6.3 Memory Management
- **Connection Pool**: Reuse connections when possible
- **Weak References**: Use for callbacks to prevent leaks
- **Auto-disconnect**: Close idle connections after 5 minutes

**Rule**: Release all resources in `disconnect()` - no leaks allowed

---

## 7. API Design Patterns

### 7.1 Result Type Pattern
```kotlin
sealed class ConnectionResult {
    data class Success(val connection: Connection) : ConnectionResult()
    data class Error(val error: IPCError) : ConnectionResult()
    data class Pending(val progress: Float) : ConnectionResult()
}
```

**Rule**: Never throw exceptions from public APIs - return Result types

### 7.2 Callback Pattern
```kotlin
interface ConnectionCallback {
    fun onConnected(connection: Connection)
    fun onDisconnected(reason: String)
    fun onError(error: IPCError)
}

// Weak reference to prevent leaks
private val callbacks = mutableListOf<WeakReference<ConnectionCallback>>()
```

### 7.3 Builder Pattern
```kotlin
val connector = ServiceConnector.Builder()
    .registry(registry)
    .reconnectionPolicy(policy)
    .circuitBreaker(config)
    .rateLimiter(limiter)
    .build()
```

### 7.4 Fluent API
```kotlin
connector.connect(endpoint)
    .onSuccess { connection ->
        connection.call("method", params)
            .onSuccess { result -> /* ... */ }
            .onError { error -> /* ... */ }
    }
    .onError { error -> /* ... */ }
```

---

## 8. Testing Requirements

### 8.1 Unit Test Coverage
- **Minimum**: 80% code coverage
- **Critical Paths**: 100% (connection, error handling, security)

### 8.2 Test Categories
```kotlin
// 1. Connection lifecycle tests
@Test fun testConnectSuccess()
@Test fun testConnectFailure()
@Test fun testReconnectAfterFailure()
@Test fun testDisconnectWhileConnecting()

// 2. Error handling tests
@Test fun testServiceUnavailableRetry()
@Test fun testPermissionDeniedNoRetry()
@Test fun testCircuitBreakerOpens()

// 3. Thread safety tests
@Test fun testConcurrentConnections()
@Test fun testConcurrentDisconnects()

// 4. Security tests
@Test fun testSignatureVerification()
@Test fun testPermissionCheck()
@Test fun testParameterSanitization()

// 5. Performance tests
@Test fun testConnectionLatency()
@Test fun testMethodCallLatency()
@Test fun testMemoryLeaks()
```

### 8.3 Integration Tests
- **Android Instrumentation**: Test with real AIDL services
- **Mock Services**: Test error conditions
- **Stress Tests**: 100 concurrent connections

---

## 9. Protocol-Specific Design

### 9.1 AIDL Service Connector (Android)
```kotlin
class AIDLServiceConnector : ServiceConnector {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            // Handle connection
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // Handle disconnection (crashed or killed)
        }

        override fun onBindingDied(name: ComponentName) {
            // Handle death recipient (process died)
        }
    }
}
```

**Design Rules:**
- Use `BIND_AUTO_CREATE` flag for automatic service start
- Implement `DeathRecipient` for crash detection
- Unbind in `onDestroy()` to prevent leaks
- Handle `RemoteException` for all binder calls

### 9.2 Content Provider Connector
```kotlin
class ContentProviderConnector {
    suspend fun query(
        uri: Uri,
        projection: Array<String>? = null,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null
    ): Cursor? {
        return withContext(Dispatchers.IO) {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        }
    }
}
```

**Design Rules:**
- Always close `Cursor` after use
- Use `ContentObserver` for data change notifications
- Handle `SecurityException` for permission denials
- Implement timeout for long queries

### 9.3 WebSocket Connector (Future)
```kotlin
class WebSocketConnector {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
}
```

**Design Rules:**
- Use OkHttp for WebSocket implementation
- Implement heartbeat/ping for keepalive
- Handle network changes gracefully
- Support compression for large messages

---

## 10. Documentation Requirements

### 10.1 KDoc Standards
```kotlin
/**
 * Connects to a remote service via AIDL.
 *
 * This method establishes a connection to the service specified by [endpoint].
 * The connection is asynchronous and may take several seconds to complete.
 *
 * @param endpoint Service endpoint from ARG registry
 * @return ConnectionResult indicating success or failure
 * @throws SecurityException if caller lacks required permission
 *
 * @see ServiceEndpoint
 * @see ConnectionResult
 *
 * @sample
 * ```kotlin
 * val result = connector.connect(endpoint)
 * when (result) {
 *     is ConnectionResult.Success -> println("Connected!")
 *     is ConnectionResult.Error -> println("Failed: ${result.error}")
 * }
 * ```
 *
 * @since 1.0.0
 */
suspend fun connect(endpoint: ServiceEndpoint): ConnectionResult
```

### 10.2 README Requirements
- Quick Start example
- Architecture diagram
- API reference
- Common use cases
- Troubleshooting guide
- Performance tuning

---

## 11. Monitoring & Observability

### 11.1 Metrics to Track
```kotlin
data class IPCMetrics(
    val connectionsActive: Int,
    val connectionsTotal: Long,
    val connectionsFailed: Long,
    val averageLatencyMs: Double,
    val p95LatencyMs: Long,
    val p99LatencyMs: Long,
    val errorRate: Float,
    val circuitBreakerState: CircuitState
)
```

### 11.2 Logging Standards
```kotlin
// Use structured logging
logger.info(
    "Connected to service",
    mapOf(
        "packageName" to endpoint.packageName,
        "serviceId" to endpoint.id,
        "latencyMs" to duration,
        "attempt" to retryCount
    )
)
```

**Rule**: Log all connections, disconnections, errors, and retries

---

## 12. Compliance & Validation

### 12.1 Design Review Checklist
- [ ] Thread safety verified for all public APIs
- [ ] Error handling covers all failure modes
- [ ] Security checks implemented (signature, permission)
- [ ] Performance targets met (<100ms connect, <10ms call)
- [ ] Memory leaks prevented (weak refs, proper cleanup)
- [ ] Unit tests achieve 80%+ coverage
- [ ] KDoc present for all public APIs
- [ ] README includes quick start example

### 12.2 Code Review Requirements
- **Security Review**: Required for signature verification code
- **Performance Review**: Required if latency >10% above target
- **Architecture Review**: Required for new protocol implementations

---

## 13. Version Compatibility

### 13.1 API Versioning
```kotlin
interface ServiceConnector {
    val version: String  // Semantic versioning: MAJOR.MINOR.PATCH

    // Breaking changes increment MAJOR
    // New features increment MINOR
    // Bug fixes increment PATCH
}
```

### 13.2 Backward Compatibility
- **Rule**: Never break existing APIs without major version bump
- **Deprecation**: Mark old APIs with `@Deprecated` for 2 versions before removal
- **Migration Guide**: Provide upgrade path for breaking changes

---

## 14. Emergency Procedures

### 14.1 Circuit Breaker Manual Override
```kotlin
// In case of service misbehavior
connector.openCircuit(serviceId, reason = "Manual override due to DoS")
```

### 14.2 Connection Drain
```kotlin
// Gracefully close all connections before shutdown
connector.drainConnections(timeout = 30.seconds)
```

### 14.3 Kill Switch
```kotlin
// Emergency shutdown of all IPC
IPCConnector.GLOBAL.disableAllConnections()
```

---

## 15. Change Log

### Version 1.0.0 (2025-11-10)
- Initial design standard
- AIDL and Content Provider protocols
- Security, performance, and threading requirements
- Circuit breaker and rate limiting patterns

---

**Approved by:** Manoj Jhawar, manoj@ideahq.net
**Next Review:** 2026-02-10 (Quarterly)

**Created by Manoj Jhawar, manoj@ideahq.net**
