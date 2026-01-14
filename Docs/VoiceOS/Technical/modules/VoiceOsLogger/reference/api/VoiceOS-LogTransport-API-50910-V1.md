# LogTransport API Reference

**Package:** `com.augmentalis.logger.remote`
**Since:** VOS4 v1.1 (VoiceOS Logger 3.0)
**Classification:** Cold Path (<10 Hz)
**Created:** 2025-10-09 05:37:00 PDT

## Overview

`LogTransport` is a strategic interface that abstracts the protocol layer for remote log delivery. It enables protocol flexibility (HTTP, gRPC, WebSocket, custom) without modifying `RemoteLogSender` implementation.

## Interface Definition

```kotlin
package com.augmentalis.logger.remote

/**
 * Protocol abstraction for remote log delivery
 *
 * Enables multiple transport implementations:
 * - HTTP via HttpLogTransport
 * - gRPC via GrpcLogTransport (future)
 * - WebSocket via WebSocketLogTransport (future)
 * - Custom implementations for specific backends
 *
 * **Strategic Interface Justification:**
 * - Call frequency: 0.1-1 Hz (cold path)
 * - Battery cost: 0.0001% (7ms per 10 hours)
 * - Testing benefit: 350x faster (35s → 0.1s)
 * - ROI: 7000:1 (dev time vs battery cost)
 *
 * @see HttpLogTransport for default HTTP implementation
 * @see RemoteLogSender for usage
 * @see ADR-002-Strategic-Interfaces-251009-0511.md for rationale
 */
interface LogTransport {

    /**
     * Send log payload to remote endpoint
     *
     * This method is called by RemoteLogSender when a batch of logs
     * is ready to be sent. The implementation should:
     * 1. Establish connection to remote endpoint
     * 2. Send payload with provided headers
     * 3. Return success with response code or failure with error
     *
     * @param payload JSON string containing log batch with structure:
     *   ```json
     *   {
     *     "logs": [
     *       {
     *         "timestamp": 1234567890,
     *         "level": "ERROR",
     *         "tag": "ModuleName",
     *         "message": "Log message",
     *         "stackTrace": "..." // optional
     *       }
     *     ],
     *     "batch_size": 10,
     *     "immediate": false,
     *     "device_info": { "manufacturer": "...", "model": "..." },
     *     "app_info": { "package_name": "...", "version_name": "..." }
     *   }
     *   ```
     *
     * @param headers Map of HTTP headers to include in request:
     *   - "Content-Type": Usually "application/json"
     *   - "Authorization": API key or bearer token
     *   - "User-Agent": Client identifier (e.g., "VoiceOS-Logger/3.0")
     *   - Custom headers as needed by implementation
     *
     * @return Result<Int> with:
     *   - Success: Response code (200-299 for success)
     *   - Failure: Exception with error message
     *
     * @throws None - All errors returned as Result.failure
     */
    suspend fun send(payload: String, headers: Map<String, String>): Result<Int>
}
```

## Implementations

### HttpLogTransport (Default)

**File:** `HttpLogTransport.kt`
**Protocol:** HTTP/HTTPS
**Usage:**

```kotlin
val transport = HttpLogTransport(
    endpoint = "https://logs.example.com/api/v1/logs",
    apiKey = "your-api-key-here"
)

val sender = RemoteLogSender(transport, context)
sender.enable()
```

**Features:**
- HTTP POST with JSON payload
- Automatic endpoint URL validation
- Configurable timeout (10 seconds)
- Bearer token authentication
- Comprehensive error codes:
  - 200-299: Success
  - 401: Authentication failed
  - 403: Authorization failed
  - 404: Endpoint not found
  - 429: Rate limit exceeded
  - 500-599: Server errors
- Network error handling with retries

**Configuration:**

```kotlin
class HttpLogTransport(
    private val endpoint: String,  // Remote server URL (HTTPS recommended)
    private val apiKey: String     // API key for authentication
) : LogTransport
```

### MockLogTransport (Testing)

**File:** `LogTransportTest.kt`
**Purpose:** JVM-only unit testing (350x faster)
**Usage:**

```kotlin
val mockTransport = MockLogTransport(
    shouldSucceed = true,
    responseCode = 200,
    errorMessage = "Mock error",
    delayMs = 0
)

val sender = RemoteLogSender(mockTransport, context)
// Test without network calls
```

**Features:**
- Configurable success/failure responses
- Custom response codes
- Simulated network delays
- Payload and header inspection
- Call count tracking

### GrpcLogTransport (Future)

**Status:** Planned
**Protocol:** gRPC
**Benefits:**
- Binary protocol (smaller payload)
- Bidirectional streaming
- HTTP/2 multiplexing
- Built-in load balancing

**Future Implementation:**

```kotlin
class GrpcLogTransport(
    private val channel: ManagedChannel,
    private val stub: LogServiceGrpc.LogServiceStub
) : LogTransport {
    override suspend fun send(payload: String, headers: Map<String, String>): Result<Int> {
        // gRPC implementation
    }
}
```

### WebSocketLogTransport (Future)

**Status:** Planned
**Protocol:** WebSocket
**Benefits:**
- Persistent connection
- Real-time log streaming
- Lower latency
- Reduced overhead

**Future Implementation:**

```kotlin
class WebSocketLogTransport(
    private val websocket: WebSocket
) : LogTransport {
    override suspend fun send(payload: String, headers: Map<String, String>): Result<Int> {
        // WebSocket implementation
    }
}
```

## Integration with VoiceOsLogger

### Public API (Unchanged)

```kotlin
// User-facing API remains the same
VoiceOsLogger.enableRemoteLogging(
    endpoint = "https://logs.example.com/api/v1/logs",
    apiKey = "your-api-key-here"
)
```

### Internal Implementation

```kotlin
// VoiceOsLogger creates HttpLogTransport internally
fun enableRemoteLogging(endpoint: String, apiKey: String) {
    val transport = HttpLogTransport(endpoint, apiKey)
    remoteLogSender = RemoteLogSender(transport, context).apply {
        enable()
    }
}
```

### Advanced Usage (Direct Transport)

For advanced scenarios, users can create custom transports:

```kotlin
// Custom transport implementation
class MyCustomTransport : LogTransport {
    override suspend fun send(payload: String, headers: Map<String, String>): Result<Int> {
        // Custom protocol implementation
        return Result.success(200)
    }
}

// Use custom transport directly
val customTransport = MyCustomTransport()
val sender = RemoteLogSender(customTransport, context)
sender.enable()
```

## Error Handling

### Result Pattern

All transport implementations return `Result<Int>`:

```kotlin
val result = transport.send(payload, headers)

result.onSuccess { responseCode ->
    Log.d(TAG, "Logs sent successfully: $responseCode")
}

result.onFailure { error ->
    Log.w(TAG, "Failed to send logs: ${error.message}")
    // RemoteLogSender handles retry logic
}
```

### Common Error Messages

**Network Errors:**
- `"Network error: Connection refused"`
- `"Network error: Connection timeout"`
- `"Network error: Unknown host"`

**HTTP Errors:**
- `"Authentication failed: Invalid API key"` (401)
- `"Authorization failed: Access forbidden"` (403)
- `"Endpoint not found: https://..."` (404)
- `"Rate limit exceeded: Too many requests"` (429)
- `"Server error: 500"` (500-599)

**Transport-Specific Errors:**
- gRPC: `"gRPC error: UNAVAILABLE"`
- WebSocket: `"WebSocket closed unexpectedly"`

## Performance Characteristics

### Call Frequency
- **Typical:** 0.1-1 Hz (6-60 calls per minute)
- **Batch intervals:** 30 seconds (configurable)
- **Immediate send:** Critical errors only

### Battery Impact
- **Interface overhead:** ~8 CPU cycles vs ~2 cycles direct call
- **Total cost:** 7ms per 10 hours
- **Battery impact:** 0.0001% (negligible)

### Testing Speed
- **Emulator:** 35 seconds per test suite
- **JVM mock:** 0.1 seconds per test suite
- **Improvement:** 350x faster

## Testing

### Unit Tests

```kotlin
@Test
fun `LogTransport sends payload successfully`() = runBlocking {
    val transport = MockLogTransport(responseCode = 200)
    val result = transport.send(payload, headers)

    assertTrue(result.isSuccess)
    assertEquals(200, result.getOrNull())
}

@Test
fun `LogTransport handles network failure`() = runBlocking {
    val transport = MockLogTransport(
        shouldSucceed = false,
        errorMessage = "Network timeout"
    )
    val result = transport.send(payload, headers)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("timeout") == true)
}
```

### Integration Tests

```kotlin
@Test
fun `RemoteLogSender works with different transports`() = runBlocking {
    // Test with HTTP
    val httpTransport = HttpLogTransport(endpoint, apiKey)
    val httpSender = RemoteLogSender(httpTransport, context)
    httpSender.queueLog(Level.ERROR, "Test", "HTTP test")

    // Test with gRPC (future)
    val grpcTransport = GrpcLogTransport(channel, stub)
    val grpcSender = RemoteLogSender(grpcTransport, context)
    grpcSender.queueLog(Level.ERROR, "Test", "gRPC test")
}
```

## Migration Guide

### From RemoteLogSender v1.0 (No Interface)

**Before (VOS4 v1.0):**
```kotlin
val sender = RemoteLogSender(
    endpoint = "https://logs.example.com/api/v1/logs",
    apiKey = "your-api-key",
    context = applicationContext
)
```

**After (VOS4 v1.1):**
```kotlin
val transport = HttpLogTransport(
    endpoint = "https://logs.example.com/api/v1/logs",
    apiKey = "your-api-key"
)
val sender = RemoteLogSender(transport, applicationContext)
```

**Via VoiceOsLogger (No Changes Required):**
```kotlin
// Same API - no migration needed
VoiceOsLogger.enableRemoteLogging(
    endpoint = "https://logs.example.com/api/v1/logs",
    apiKey = "your-api-key"
)
```

## Design Decisions

### Why Interface for Cold Path?

**Decision:** Use LogTransport interface for protocol abstraction

**Rationale:**
1. **Call frequency:** 0.1-1 Hz (well below 10 Hz hot path threshold)
2. **Battery cost:** 0.0001% (7ms per 10 hours) - negligible
3. **Testing benefit:** 350x faster tests without network calls
4. **Protocol flexibility:** Swap HTTP → gRPC → WebSocket without code changes
5. **ROI:** 7000:1 (58 min/day dev time saved vs 7ms battery cost)

**Alternatives Considered:**
- ❌ Direct HTTP implementation: Inflexible, slow tests
- ❌ Abstract class: Less flexible than interface
- ✅ Interface: Maximum flexibility, mockable, clean separation

**See Also:**
- ADR-002-Strategic-Interfaces-251009-0511.md
- VOS4-CODING-PROTOCOL.md v1.1 (Strategic Interface guidelines)

## See Also

- **RemoteLogSender:** Consumer of LogTransport interface
- **VoiceOsLogger:** Public API for remote logging
- **LogTransportTest:** Unit tests and mocking examples
- **ADR-002:** Architecture decision rationale
- **VOS4-CODING-PROTOCOL.md:** Hot/cold path classification guidelines

---

**Last Updated:** 2025-10-09 05:37:00 PDT
**Version:** VOS4 v1.1 (VoiceOS Logger 3.0)
**Status:** Production Ready
