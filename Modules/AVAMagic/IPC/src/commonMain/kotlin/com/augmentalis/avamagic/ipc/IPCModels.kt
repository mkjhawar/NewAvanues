package com.augmentalis.avamagic.ipc

import kotlinx.serialization.Serializable

/**
 * IPC Connection Models
 *
 * Unified data structures for inter-process communication across all platforms.
 * Combines connection management, messaging, and monitoring capabilities.
 *
 * All data classes include `toAvuLine()` for AVU format serialization.
 *
 * @since 1.0.0
 * @author Augmentalis
 */

// =============================================================================
// CONNECTION STATE MANAGEMENT
// =============================================================================

/**
 * Connection state machine representing all possible states of an IPC connection.
 *
 * State transitions:
 * - DISCONNECTED -> CONNECTING -> CONNECTED
 * - CONNECTED -> DISCONNECTING -> DISCONNECTED
 * - CONNECTED -> RECONNECTING -> CONNECTED
 * - Any state -> FAILED
 */
enum class ConnectionState {
    /** No active connection */
    DISCONNECTED,
    /** Connection attempt in progress */
    CONNECTING,
    /** Successfully connected and ready for communication */
    CONNECTED,
    /** Connection attempt or operation failed */
    FAILED,
    /** Attempting to re-establish a lost connection */
    RECONNECTING,
    /** Gracefully closing the connection */
    DISCONNECTING;

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = "state:$name"
}

/**
 * Active connection handle representing a live IPC channel.
 *
 * @property id Unique identifier for this connection
 * @property packageName Target app package/bundle identifier
 * @property serviceId Service identifier within the target app
 * @property state Current connection state
 * @property connectedAt Timestamp when connection was established (epoch millis)
 * @property protocol IPC protocol used for this connection
 * @property handle Platform-specific handle (IBinder, XPCConnection, WebSocket, etc.)
 */
data class Connection(
    val id: String,
    val packageName: String,
    val serviceId: String,
    val state: ConnectionState,
    val connectedAt: Long = System.currentTimeMillis(),
    val protocol: IPCProtocol,
    val handle: Any
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("connection(")
        append("id=$id,")
        append("package=$packageName,")
        append("service=$serviceId,")
        append("state=${state.name},")
        append("connectedAt=$connectedAt,")
        append("protocol=${protocol.name}")
        append(")")
    }
}

/**
 * IPC protocol types supported across platforms.
 *
 * Each protocol has different characteristics:
 * - AIDL: High performance, Android only
 * - CONTENT_PROVIDER: Data sharing, Android only
 * - WEBSOCKET: Cross-platform, network-based
 * - URL_SCHEME: App launching, iOS/macOS
 * - XPC: Secure IPC, iOS/macOS
 * - NAMED_PIPE: Fast local IPC, Windows/Linux
 */
enum class IPCProtocol {
    /** Android Interface Definition Language - high performance Android IPC */
    AIDL,
    /** Android Content Provider - data sharing between apps */
    CONTENT_PROVIDER,
    /** WebSocket - cross-platform network-based communication */
    WEBSOCKET,
    /** iOS URL Scheme - app-to-app communication via URLs */
    URL_SCHEME,
    /** iOS/macOS XPC - secure inter-process communication */
    XPC,
    /** Windows/Linux Named Pipes - fast local IPC */
    NAMED_PIPE;

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = "protocol:$name"
}

/**
 * Result of a connection attempt.
 */
sealed class ConnectionResult {
    /**
     * Connection established successfully.
     * @property connection The active connection handle
     */
    data class Success(val connection: Connection) : ConnectionResult() {
        /**
         * Serialize to AVU format.
         * @return AVU line representation
         */
        fun toAvuLine(): String = "result.success(${connection.toAvuLine()})"
    }

    /**
     * Connection failed with an error.
     * @property error The error that occurred
     */
    data class Error(val error: IPCError) : ConnectionResult() {
        /**
         * Serialize to AVU format.
         * @return AVU line representation
         */
        fun toAvuLine(): String = "result.error(${error.toAvuLine()})"
    }

    /**
     * Connection is in progress.
     * @property progress Progress from 0.0 to 1.0
     */
    data class Pending(val progress: Float) : ConnectionResult() {
        /**
         * Serialize to AVU format.
         * @return AVU line representation
         */
        fun toAvuLine(): String = "result.pending(progress=$progress)"
    }

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = when (this) {
        is Success -> (this as Success).toAvuLine()
        is Error -> (this as Error).toAvuLine()
        is Pending -> (this as Pending).toAvuLine()
    }
}

// =============================================================================
// ERROR HANDLING
// =============================================================================

// IPCError is defined in IPCErrors.kt with full Exception support.
// Use: IPCError.ServiceUnavailable, IPCError.Timeout, IPCError.PermissionDenied, etc.

// =============================================================================
// CONFIGURATION
// =============================================================================

/**
 * Reconnection policy with exponential backoff.
 *
 * @property enabled Whether automatic reconnection is enabled
 * @property maxRetries Maximum number of retry attempts
 * @property initialDelayMs Initial delay before first retry (milliseconds)
 * @property maxDelayMs Maximum delay between retries (milliseconds)
 * @property backoffMultiplier Multiplier for exponential backoff
 */
@Serializable
data class ReconnectionPolicy(
    val enabled: Boolean = true,
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffMultiplier: Float = 2.0f
) {
    /**
     * Calculate delay for a specific retry attempt using exponential backoff.
     *
     * @param attempt Retry attempt number (1-based)
     * @return Delay in milliseconds, capped at maxDelayMs
     */
    fun getDelay(attempt: Int): Long {
        if (attempt <= 0) return initialDelayMs
        val delay = (initialDelayMs * kotlin.math.pow(backoffMultiplier.toDouble(), (attempt - 1).toDouble())).toLong()
        return minOf(delay, maxDelayMs)
    }

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("reconnection_policy(")
        append("enabled=$enabled,")
        append("maxRetries=$maxRetries,")
        append("initialDelayMs=$initialDelayMs,")
        append("maxDelayMs=$maxDelayMs,")
        append("backoffMultiplier=$backoffMultiplier")
        append(")")
    }
}

/**
 * Circuit breaker configuration for fault tolerance.
 *
 * The circuit breaker pattern prevents cascading failures:
 * - CLOSED: Normal operation, tracking failures
 * - OPEN: Too many failures, rejecting all requests
 * - HALF_OPEN: Testing if service has recovered
 *
 * @property failureThreshold Number of failures before opening circuit
 * @property successThreshold Number of successes to close circuit from half-open
 * @property timeoutMs Time before transitioning from open to half-open (milliseconds)
 */
@Serializable
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val successThreshold: Int = 2,
    val timeoutMs: Long = 60000
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("circuit_breaker(")
        append("failureThreshold=$failureThreshold,")
        append("successThreshold=$successThreshold,")
        append("timeoutMs=$timeoutMs")
        append(")")
    }
}

/**
 * Circuit breaker state.
 */
enum class CircuitState {
    /** Normal operation - requests allowed, tracking failures */
    CLOSED,
    /** Failing - all requests rejected immediately */
    OPEN,
    /** Testing recovery - limited requests allowed */
    HALF_OPEN;

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = "circuit_state:$name"
}

/**
 * Rate limiter configuration using token bucket algorithm.
 *
 * @property maxRequestsPerSecond Sustained request rate limit
 * @property burstSize Maximum burst size allowed
 */
@Serializable
data class RateLimitConfig(
    val maxRequestsPerSecond: Int = 10,
    val burstSize: Int = 20
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("rate_limit(")
        append("maxRps=$maxRequestsPerSecond,")
        append("burstSize=$burstSize")
        append(")")
    }
}

/**
 * Resource limits for IPC operations.
 *
 * @property maxConnections Maximum concurrent connections
 * @property maxMessageSize Maximum message size in bytes
 * @property connectionTimeoutMs Timeout for connection establishment
 * @property methodTimeoutMs Timeout for method invocations
 * @property queryTimeoutMs Timeout for content provider queries
 */
@Serializable
data class ResourceLimits(
    val maxConnections: Int = 32,
    val maxMessageSize: Int = 1_048_576,
    val connectionTimeoutMs: Long = 5000,
    val methodTimeoutMs: Long = 10000,
    val queryTimeoutMs: Long = 5000
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("resource_limits(")
        append("maxConnections=$maxConnections,")
        append("maxMessageSize=$maxMessageSize,")
        append("connectionTimeoutMs=$connectionTimeoutMs,")
        append("methodTimeoutMs=$methodTimeoutMs,")
        append("queryTimeoutMs=$queryTimeoutMs")
        append(")")
    }
}

// =============================================================================
// METHOD INVOCATION
// =============================================================================

/**
 * Method invocation request for remote procedure calls.
 *
 * @property methodName Name of the method to invoke
 * @property parameters Method parameters as key-value pairs
 * @property timeoutMs Timeout for this invocation (milliseconds)
 */
@Serializable
data class MethodInvocation(
    val methodName: String,
    val parameters: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 10000
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("invoke(")
        append("method=$methodName,")
        append("timeout=$timeoutMs")
        if (parameters.isNotEmpty()) {
            append(",params={")
            append(parameters.entries.joinToString(",") { "${it.key}=${it.value}" })
            append("}")
        }
        append(")")
    }
}

/**
 * Result of a method invocation.
 */
sealed class MethodResult {
    /**
     * Method executed successfully.
     * @property value Return value (may be null for void methods)
     */
    data class Success(val value: Any?) : MethodResult() {
        /**
         * Serialize to AVU format.
         * @return AVU line representation
         */
        fun toAvuLine(): String = "method_result.success(value=$value)"
    }

    /**
     * Method execution failed.
     * @property error The error that occurred
     */
    data class Error(val error: IPCError) : MethodResult() {
        /**
         * Serialize to AVU format.
         * @return AVU line representation
         */
        fun toAvuLine(): String = "method_result.error(${error.toAvuLine()})"
    }

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = when (this) {
        is Success -> (this as Success).toAvuLine()
        is Error -> (this as Error).toAvuLine()
    }
}

// =============================================================================
// CONTENT PROVIDER (Android)
// =============================================================================

/**
 * Query parameters for Content Provider operations.
 *
 * @property uri Content URI to query
 * @property projection Columns to return (null for all)
 * @property selection SQL WHERE clause (without WHERE keyword)
 * @property selectionArgs Values for ? placeholders in selection
 * @property sortOrder SQL ORDER BY clause (without ORDER BY keyword)
 * @property limit Maximum rows to return
 */
@Serializable
data class QueryParams(
    val uri: String,
    val projection: List<String>? = null,
    val selection: String? = null,
    val selectionArgs: List<String>? = null,
    val sortOrder: String? = null,
    val limit: Int? = null
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("query(")
        append("uri=$uri")
        projection?.let { append(",projection=[${it.joinToString(",")}]") }
        selection?.let { append(",selection=$it") }
        selectionArgs?.let { append(",args=[${it.joinToString(",")}]") }
        sortOrder?.let { append(",sortOrder=$it") }
        limit?.let { append(",limit=$it") }
        append(")")
    }
}

/**
 * Result from a Content Provider query.
 *
 * @property rows List of rows, each as a map of column name to value
 * @property count Total number of rows returned
 */
@Serializable
data class QueryResult(
    val rows: List<Map<String, String>>,
    val count: Int
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("query_result(")
        append("count=$count,")
        append("rows=[")
        append(rows.joinToString(";") { row ->
            "{" + row.entries.joinToString(",") { "${it.key}=${it.value}" } + "}"
        })
        append("])")
    }
}

// =============================================================================
// MESSAGE FILTERING (Universal IPC)
// =============================================================================

/**
 * Message type categories for filtering subscriptions.
 */
enum class MessageType {
    /** Request messages expecting a response */
    REQUEST,
    /** Response messages to a previous request */
    RESPONSE,
    /** Event notifications (fire-and-forget) */
    EVENT,
    /** State change notifications */
    STATE,
    /** Content/data payloads */
    CONTENT;

    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = "message_type:$name"
}

/**
 * Message filter for subscription filtering.
 *
 * All non-null fields must match for a message to pass the filter.
 *
 * @property sourceApp Filter by source app ID (null = any app)
 * @property messageCode Filter by message code (null = any code)
 * @property messageType Filter by message type category (null = any type)
 */
@Serializable
data class MessageFilter(
    val sourceApp: String? = null,
    val messageCode: String? = null,
    val messageType: MessageType? = null
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("filter(")
        val parts = mutableListOf<String>()
        sourceApp?.let { parts.add("source=$it") }
        messageCode?.let { parts.add("code=$it") }
        messageType?.let { parts.add("type=${it.name}") }
        append(parts.joinToString(","))
        append(")")
    }
}

// =============================================================================
// CALLBACKS
// =============================================================================

/**
 * Connection callback interface for receiving connection events.
 */
interface ConnectionCallback {
    /**
     * Called when a connection is successfully established.
     * @param connection The new connection
     */
    fun onConnected(connection: Connection)

    /**
     * Called when a connection is closed.
     * @param connectionId ID of the closed connection
     * @param reason Human-readable reason for disconnection
     */
    fun onDisconnected(connectionId: String, reason: String)

    /**
     * Called when an error occurs on a connection.
     * @param connectionId ID of the affected connection
     * @param error The error that occurred
     */
    fun onError(connectionId: String, error: IPCError)

    /**
     * Called when connection state changes.
     * @param connectionId ID of the connection
     * @param oldState Previous state
     * @param newState New state
     */
    fun onStateChanged(connectionId: String, oldState: ConnectionState, newState: ConnectionState)
}

// =============================================================================
// METRICS & MONITORING
// =============================================================================

/**
 * Comprehensive IPC metrics for monitoring and observability.
 *
 * Combines connection metrics, message metrics, and latency percentiles.
 *
 * @property connectionsActive Current number of active connections
 * @property connectionsTotal Total connections established since startup
 * @property connectionsFailed Total connection attempts that failed
 * @property messagesSent Total messages sent
 * @property messagesReceived Total messages received
 * @property messagesFailed Total messages that failed to send/receive
 * @property averageLatencyMs Average round-trip latency in milliseconds
 * @property p95LatencyMs 95th percentile latency in milliseconds
 * @property p99LatencyMs 99th percentile latency in milliseconds
 * @property errorRate Error rate as a fraction (0.0 to 1.0)
 * @property uptime Time since IPC system started (milliseconds)
 * @property circuitBreakerState Current state of the circuit breaker
 */
@Serializable
data class IPCMetrics(
    val connectionsActive: Int = 0,
    val connectionsTotal: Long = 0,
    val connectionsFailed: Long = 0,
    val messagesSent: Long = 0,
    val messagesReceived: Long = 0,
    val messagesFailed: Long = 0,
    val averageLatencyMs: Double = 0.0,
    val p95LatencyMs: Long = 0,
    val p99LatencyMs: Long = 0,
    val errorRate: Float = 0f,
    val uptime: Long = 0,
    val circuitBreakerState: CircuitState = CircuitState.CLOSED
) {
    /**
     * Serialize to AVU format.
     * @return AVU line representation
     */
    fun toAvuLine(): String = buildString {
        append("metrics(")
        append("active=$connectionsActive,")
        append("total=$connectionsTotal,")
        append("failed=$connectionsFailed,")
        append("sent=$messagesSent,")
        append("received=$messagesReceived,")
        append("msgFailed=$messagesFailed,")
        append("avgLatency=$averageLatencyMs,")
        append("p95=$p95LatencyMs,")
        append("p99=$p99LatencyMs,")
        append("errorRate=$errorRate,")
        append("uptime=$uptime,")
        append("circuit=${circuitBreakerState.name}")
        append(")")
    }
}
