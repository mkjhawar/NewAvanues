## IPC Connector

**Inter-Process Communication (IPC) connector for VoiceOS ecosystem with automatic reconnection, circuit breaker, and rate limiting.**

## Overview

The IPC Connector provides a robust, production-ready solution for inter-app communication in the VoiceOS ecosystem:

- **AIDL Service Connector**: Connect to remote Android services via AIDL
- **Content Provider Connector**: Query and modify data via Content Providers
- **Connection Lifecycle**: Automatic connection management with reconnection
- **Circuit Breaker**: Prevent cascading failures with automatic recovery
- **Rate Limiting**: Token bucket rate limiter to prevent service abuse
- **Thread Safety**: All APIs are thread-safe and coroutine-aware
- **Monitoring**: Built-in metrics for observability

## Architecture

```
ConnectionManager (High-level API)
├── ServiceConnector (AIDL)
│   └── Platform Implementation (Android/iOS/etc.)
├── ContentProviderConnector
│   └── Platform Implementation (Android/iOS/etc.)
├── CircuitBreaker
├── RateLimiter
└── Metrics
```

## Quick Start

### 1. AIDL Service Connection

```kotlin
// Setup
val registry = ARGRegistry()
val connector = ServiceConnector()
connector.setContext(context)  // Android only

// Find service endpoint
val endpoint = registry.getService("com.app.service")

// Connect
val result = connector.connect(endpoint)
when (result) {
    is ConnectionResult.Success -> {
        val connection = result.connection
        println("Connected: ${connection.id}")

        // Invoke method
        val methodResult = connector.invoke(
            connection.id,
            MethodInvocation("getUserData", mapOf("userId" to "123"))
        )

        when (methodResult) {
            is MethodResult.Success -> println("Result: ${methodResult.value}")
            is MethodResult.Error -> println("Error: ${methodResult.error}")
        }

        // Disconnect
        connector.disconnect(connection.id)
    }
    is ConnectionResult.Error -> {
        println("Failed: ${result.error}")
    }
}
```

### 2. Content Provider Query

```kotlin
val connector = ContentProviderConnector()
connector.setContext(context)

val endpoint = registry.getContentProvider("com.app.provider")

val result = connector.query(
    endpoint,
    QueryParams(
        uri = "content://com.app.provider/notes",
        projection = listOf("id", "title", "content"),
        selection = "title LIKE ?",
        selectionArgs = listOf("%meeting%"),
        sortOrder = "created DESC",
        limit = 10
    )
)

result.onSuccess { queryResult ->
    queryResult.rows.forEach { row ->
        println("${row["title"]}: ${row["content"]}")
    }
}
```

### 3. Connection Manager (Recommended)

```kotlin
val manager = ConnectionManager(
    registry = registry,
    reconnectionPolicy = ReconnectionPolicy(
        enabled = true,
        maxRetries = 3,
        initialDelayMs = 1000,
        backoffMultiplier = 2.0f
    ),
    circuitBreakerConfig = CircuitBreakerConfig(
        failureThreshold = 5,
        successThreshold = 2,
        timeoutMs = 60000
    ),
    rateLimitConfig = RateLimitConfig(
        maxRequestsPerSecond = 10,
        burstSize = 20
    )
)

// Connect with all protections
val result = manager.connect(endpoint)

// Monitor metrics
manager.metrics.collect { metrics ->
    println("Active: ${metrics.connectionsActive}")
    println("Failed: ${metrics.connectionsFailed}")
    println("Error Rate: ${metrics.errorRate}")
}
```

## Core Components

### 1. ServiceConnector

Platform-agnostic AIDL service connector.

**Methods:**
- `connect(endpoint): ConnectionResult` - Connect to service
- `disconnect(connectionId)` - Disconnect from service
- `invoke(connectionId, invocation): MethodResult` - Call remote method
- `isConnected(connectionId): Boolean` - Check connection status
- `getConnection(connectionId): Connection?` - Get connection details

**Android Implementation:**
- Uses `Context.bindService()` with `BIND_AUTO_CREATE`
- Implements `ServiceConnection` callbacks
- Handles `DeathRecipient` for crash detection
- Properly unbinds in cleanup

### 2. ContentProviderConnector

Platform-agnostic Content Provider connector.

**Methods:**
- `query(endpoint, params): Result<QueryResult>` - Query data
- `insert(endpoint, uri, values): Result<String>` - Insert row
- `update(endpoint, uri, values, selection, args): Result<Int>` - Update rows
- `delete(endpoint, uri, selection, args): Result<Int>` - Delete rows
- `registerObserver(uri, callback)` - Watch for changes
- `unregisterObserver(uri)` - Stop watching

**Android Implementation:**
- Uses `ContentResolver` for CRUD operations
- Implements `ContentObserver` for change notifications
- Handles `Cursor` lifecycle properly
- Type-safe value conversion

### 3. CircuitBreaker

Prevents cascading failures by opening circuit after repeated errors.

**States:**
- **CLOSED**: Normal operation, requests allowed
- **OPEN**: Failing, requests rejected
- **HALF_OPEN**: Testing if service recovered

**Configuration:**
```kotlin
CircuitBreakerConfig(
    failureThreshold = 5,        // Failures before opening
    successThreshold = 2,        // Successes to close
    timeoutMs = 60000           // 60s before testing recovery
)
```

**Usage:**
```kotlin
val circuitBreaker = CircuitBreaker(config)

val result = circuitBreaker.execute {
    // Your operation
}

if (result.isSuccess) {
    // Success - circuit remains closed
} else {
    // Failure - counts toward threshold
}
```

### 4. RateLimiter

Token bucket rate limiter to prevent service abuse.

**Algorithm:**
- Tokens refill at `maxRequestsPerSecond` rate
- Bucket holds up to `burstSize` tokens
- Each request consumes 1 token
- Rejects requests when no tokens available

**Configuration:**
```kotlin
RateLimitConfig(
    maxRequestsPerSecond = 10,   // Sustained rate
    burstSize = 20               // Initial burst allowance
)
```

**Usage:**
```kotlin
val rateLimiter = RateLimiter(config)

if (rateLimiter.tryAcquire()) {
    // Request allowed
} else {
    val waitMs = rateLimiter.timeUntilNextToken()
    // Rate limited - wait or reject
}
```

### 5. ConnectionManager

High-level orchestration with all protections enabled.

**Features:**
- Automatic reconnection with exponential backoff
- Circuit breaker per service
- Rate limiter per service
- Resource limits (max connections, timeouts)
- Connection pooling
- Metrics collection
- Weak reference callbacks

**Lifecycle:**
```kotlin
// Register callback
manager.registerCallback(object : ConnectionCallback {
    override fun onConnected(connection: Connection) {
        println("Connected: ${connection.id}")
    }

    override fun onDisconnected(connectionId: String, reason: String) {
        println("Disconnected: $reason")
    }

    override fun onError(connectionId: String, error: IPCError) {
        println("Error: $error")
    }

    override fun onStateChanged(connectionId: String, oldState: ConnectionState, newState: ConnectionState) {
        println("State: $oldState → $newState")
    }
})

// Cleanup
manager.shutdown()
```

## Error Handling

### Error Categories

**Transient (Retry Possible):**
- `IPCError.ServiceUnavailable` - Service temporarily unavailable
- `IPCError.Timeout` - Operation timed out
- `IPCError.NetworkFailure` - Network issue

**Permanent (No Retry):**
- `IPCError.PermissionDenied` - Missing required permission
- `IPCError.ServiceNotFound` - Service doesn't exist
- `IPCError.InvalidResponse` - Malformed response

**Security:**
- `IPCError.AuthenticationFailed` - Auth credentials invalid
- `IPCError.SignatureVerificationFailed` - Package signature mismatch

**Resource:**
- `IPCError.ResourceExhausted` - Resource limit reached
- `IPCError.RateLimitExceeded` - Too many requests

### Error Recovery Strategy

| Error | Retry? | User Notify? | Action |
|-------|--------|--------------|--------|
| ServiceUnavailable | ✅ (exponential backoff) | After max retries | Automatic reconnect |
| Timeout | ✅ (3 attempts) | After 2nd failure | Increase timeout |
| PermissionDenied | ❌ | ✅ Immediate | Request permission |
| ServiceNotFound | ❌ | ✅ Immediate | Check ARG registry |
| RateLimitExceeded | ⚠️ (after delay) | ❌ | Wait for token refill |

## Configuration

### Reconnection Policy

```kotlin
ReconnectionPolicy(
    enabled = true,
    maxRetries = 3,
    initialDelayMs = 1000,       // 1 second
    maxDelayMs = 30000,          // 30 seconds max
    backoffMultiplier = 2.0f     // Exponential backoff
)

// Delay calculation:
// Attempt 1: 1s
// Attempt 2: 2s
// Attempt 3: 4s
// Attempt 4: 8s (capped at maxDelay)
```

### Resource Limits

```kotlin
ResourceLimits(
    maxConnections = 32,            // Per app
    maxMessageSize = 1_048_576,     // 1MB
    connectionTimeoutMs = 5000,     // 5 seconds
    methodTimeoutMs = 10000,        // 10 seconds
    queryTimeoutMs = 5000           // 5 seconds
)
```

## Performance

### Latency Targets

| Operation | Target | Maximum |
|-----------|--------|---------|
| Connect | <100ms | 500ms |
| Method Call | <10ms | 100ms |
| Query | <50ms | 200ms |
| Disconnect | <50ms | 200ms |

### Memory

- **Connection Pool**: Reuses connections when possible
- **Weak References**: Callbacks use weak refs to prevent leaks
- **Auto-cleanup**: Idle connections closed after 5 minutes

### Thread Safety

All public APIs are thread-safe:
- `Mutex` for coroutine synchronization
- `ConcurrentHashMap` for connection registry
- `StateFlow` for observable state

## Testing

### Unit Tests

Run tests:
```bash
./gradlew :modules:MagicIdea:Components:IPCConnector:test
```

**Test Coverage:**
- ✅ Circuit breaker state machine
- ✅ Rate limiter token bucket
- ✅ Reconnection policy delays
- ✅ Connection lifecycle
- ✅ Error handling
- ✅ Configuration validation

### Integration Tests

For Android instrumentation tests:
```bash
./gradlew :modules:MagicIdea:Components:IPCConnector:connectedAndroidTest
```

## Security

### Package Signature Verification

```kotlin
// Verify package signature before connecting
fun verifyPackage(packageName: String, context: Context): Boolean {
    val pm = context.packageManager
    val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    val actualSignature = packageInfo.signatures[0].toByteArray()

    // Compare with trusted signature
    val expectedSignature = getTrustedSignature(packageName)
    return actualSignature.contentEquals(expectedSignature)
}
```

### Permission Checks

Always check permissions before connecting:
```kotlin
if (endpoint.requiresPermission != null) {
    if (!hasPermission(context, endpoint.requiresPermission)) {
        return ConnectionResult.Error(
            IPCError.PermissionDenied(endpoint.requiresPermission)
        )
    }
}
```

### Parameter Sanitization

```kotlin
fun sanitize(param: String): String {
    return param
        .replace(Regex("[<>\"']"), "")  // Prevent injection
        .take(MAX_PARAM_LENGTH)          // Prevent DoS
}
```

## Monitoring

### Metrics

```kotlin
manager.metrics.collect { metrics ->
    // Connection metrics
    println("Active: ${metrics.connectionsActive}")
    println("Total: ${metrics.connectionsTotal}")
    println("Failed: ${metrics.connectionsFailed}")

    // Performance metrics
    println("Avg Latency: ${metrics.averageLatencyMs}ms")
    println("P95 Latency: ${metrics.p95LatencyMs}ms")
    println("P99 Latency: ${metrics.p99LatencyMs}ms")

    // Health metrics
    println("Error Rate: ${metrics.errorRate}")
    println("Circuit: ${metrics.circuitBreakerState}")
}
```

### Logging

Structured logging for all events:
```kotlin
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

## Emergency Procedures

### Circuit Breaker Override

```kotlin
// Manually open circuit (service misbehaving)
manager.openCircuit(serviceId, "Manual override - DoS detected")

// Manually close circuit (service recovered)
manager.closeCircuit(serviceId)
```

### Connection Drain

```kotlin
// Gracefully close all connections before shutdown
manager.drainConnections(timeout = 30.seconds)
```

### Kill Switch

```kotlin
// Emergency shutdown of all IPC
manager.disableAllConnections()
```

## Platform Support

**Current:**
- ✅ Android (AIDL, Content Provider)

**Planned:**
- ⏳ iOS (XPC, URL Schemes)
- ⏳ macOS (XPC)
- ⏳ Windows (Named Pipes)

## Dependencies

```kotlin
dependencies {
    // ARG Scanner for endpoint discovery
    implementation(project(":modules:MagicIdea:Components:ARGScanner"))

    // Components Core for base types
    implementation(project(":modules:MagicIdea:Components:Core"))

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

## API Stability

- ✅ **Stable**: ConnectionManager, CircuitBreaker, RateLimiter
- ⚠️ **Beta**: ServiceConnector expect/actual (KMP limitation)
- 🔬 **Experimental**: WebSocket connector (not yet implemented)

## Known Limitations

1. **AIDL**: Requires code generation for each service interface
2. **Content Provider**: Limited to Android-style URIs
3. **Single Process**: Connection pooling not shared across processes
4. **No TLS**: AIDL doesn't support encryption (use Content Provider with HTTPS)

## Future Enhancements

- [ ] WebSocket connector for real-time communication
- [ ] Persistent connection pooling across app restarts
- [ ] Adaptive rate limiting based on service load
- [ ] Circuit breaker dashboard UI
- [ ] Connection analytics and insights

## Version

**1.0.0** - Initial release

## License

Copyright © 2025 Augmentalis. All rights reserved.

## Related

- [ARGScanner](../ARGScanner/README.md) - Service/provider discovery
- [VoiceCommandRouter](../VoiceCommandRouter/README.md) - Voice command routing
- [GlobalDesignStandard](./GlobalDesignStandard.md) - Complete design specification

---

**Created by Manoj Jhawar, manoj@ideahq.net**
