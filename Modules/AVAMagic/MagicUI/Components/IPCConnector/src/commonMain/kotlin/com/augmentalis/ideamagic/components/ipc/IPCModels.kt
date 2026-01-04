package com.augmentalis.magicui.components.ipc

import kotlinx.serialization.Serializable

/**
 * IPC Connection Models
 *
 * Core data structures for inter-process communication.
 *
 * @since 1.0.0
 */

/**
 * Connection state machine
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
    RECONNECTING,
    DISCONNECTING
}

/**
 * Active connection handle
 */
data class Connection(
    val id: String,
    val packageName: String,
    val serviceId: String,
    val state: ConnectionState,
    val connectedAt: Long = System.currentTimeMillis(),
    val protocol: IPCProtocol,
    val handle: Any  // Platform-specific: IBinder (Android), XPCConnection (iOS), etc.
)

/**
 * IPC protocol type
 */
enum class IPCProtocol {
    AIDL,              // Android Inter-Process Communication
    CONTENT_PROVIDER,  // Android Content Provider
    WEBSOCKET,         // Cross-platform WebSocket
    URL_SCHEME,        // iOS URL Scheme
    XPC,               // iOS/macOS XPC
    NAMED_PIPE         // Windows Named Pipes
}

/**
 * Connection result
 */
sealed class ConnectionResult {
    data class Success(val connection: Connection) : ConnectionResult()
    data class Error(val error: IPCError) : ConnectionResult()
    data class Pending(val progress: Float) : ConnectionResult()
}

/**
 * IPC error categories
 */
sealed class IPCError {
    // Transient errors (retry possible)
    data class ServiceUnavailable(val reason: String) : IPCError()
    data class Timeout(val durationMs: Long) : IPCError()
    data class NetworkFailure(val cause: Throwable? = null) : IPCError()

    // Permanent errors (no retry)
    data class PermissionDenied(val permission: String) : IPCError()
    data class ServiceNotFound(val packageName: String) : IPCError()
    data class InvalidResponse(val details: String) : IPCError()

    // Security errors
    data class AuthenticationFailed(val reason: String) : IPCError()
    data class SignatureVerificationFailed(val packageName: String) : IPCError()

    // Resource errors
    data class ResourceExhausted(val resource: String) : IPCError()
    data class RateLimitExceeded(val retryAfterMs: Long) : IPCError()
}

/**
 * Reconnection policy
 */
@Serializable
data class ReconnectionPolicy(
    val enabled: Boolean = true,
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,      // 1s
    val maxDelayMs: Long = 30000,         // 30s
    val backoffMultiplier: Float = 2.0f   // Exponential backoff
) {
    /**
     * Calculate delay for retry attempt
     */
    fun getDelay(attempt: Int): Long {
        if (attempt <= 0) return initialDelayMs
        val delay = (initialDelayMs * Math.pow(backoffMultiplier.toDouble(), (attempt - 1).toDouble())).toLong()
        return minOf(delay, maxDelayMs)
    }
}

/**
 * Circuit breaker configuration
 */
@Serializable
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,        // Failures before open
    val successThreshold: Int = 2,        // Successes to close
    val timeoutMs: Long = 60000           // 60s before half-open
)

/**
 * Circuit breaker state
 */
enum class CircuitState {
    CLOSED,      // Normal operation
    OPEN,        // Failing, rejecting requests
    HALF_OPEN    // Testing if service recovered
}

/**
 * Rate limiter configuration
 */
@Serializable
data class RateLimitConfig(
    val maxRequestsPerSecond: Int = 10,
    val burstSize: Int = 20
)

/**
 * Resource limits
 */
@Serializable
data class ResourceLimits(
    val maxConnections: Int = 32,
    val maxMessageSize: Int = 1_048_576,    // 1MB
    val connectionTimeoutMs: Long = 5000,   // 5s
    val methodTimeoutMs: Long = 10000,      // 10s
    val queryTimeoutMs: Long = 5000         // 5s
)

/**
 * IPC metrics for monitoring
 */
data class IPCMetrics(
    val connectionsActive: Int = 0,
    val connectionsTotal: Long = 0,
    val connectionsFailed: Long = 0,
    val averageLatencyMs: Double = 0.0,
    val p95LatencyMs: Long = 0,
    val p99LatencyMs: Long = 0,
    val errorRate: Float = 0f,
    val circuitBreakerState: CircuitState = CircuitState.CLOSED
)

/**
 * Method invocation request
 */
data class MethodInvocation(
    val methodName: String,
    val parameters: Map<String, Any> = emptyMap(),
    val timeoutMs: Long = 10000
)

/**
 * Method invocation result
 */
sealed class MethodResult {
    data class Success(val value: Any?) : MethodResult()
    data class Error(val error: IPCError) : MethodResult()
}

/**
 * Connection callback interface
 */
interface ConnectionCallback {
    fun onConnected(connection: Connection)
    fun onDisconnected(connectionId: String, reason: String)
    fun onError(connectionId: String, error: IPCError)
    fun onStateChanged(connectionId: String, oldState: ConnectionState, newState: ConnectionState)
}

/**
 * Query parameters for Content Provider
 */
data class QueryParams(
    val uri: String,
    val projection: List<String>? = null,
    val selection: String? = null,
    val selectionArgs: List<String>? = null,
    val sortOrder: String? = null,
    val limit: Int? = null
)

/**
 * Query result from Content Provider
 */
data class QueryResult(
    val rows: List<Map<String, Any>>,
    val count: Int
)
