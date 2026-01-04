package com.augmentalis.magicui.components.ipc

import com.augmentalis.magicui.components.argscanner.ARGRegistry
import com.augmentalis.magicui.components.argscanner.ServiceEndpoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Connection Manager
 *
 * High-level API for managing IPC connections with automatic reconnection,
 * circuit breaker, and rate limiting.
 *
 * ## Usage
 * ```kotlin
 * val manager = ConnectionManager(registry)
 *
 * // Connect to service
 * val result = manager.connect(endpoint)
 * when (result) {
 *     is ConnectionResult.Success -> {
 *         println("Connected: ${result.connection.id}")
 *     }
 *     is ConnectionResult.Error -> {
 *         println("Failed: ${result.error}")
 *     }
 * }
 *
 * // Invoke method
 * manager.invoke(connectionId, MethodInvocation("doSomething", params))
 *
 * // Disconnect
 * manager.disconnect(connectionId)
 * ```
 *
 * @since 1.0.0
 */
class ConnectionManager(
    private val registry: ARGRegistry,
    private val reconnectionPolicy: ReconnectionPolicy = ReconnectionPolicy(),
    private val circuitBreakerConfig: CircuitBreakerConfig = CircuitBreakerConfig(),
    private val rateLimitConfig: RateLimitConfig = RateLimitConfig(),
    private val resourceLimits: ResourceLimits = ResourceLimits()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private val connections = mutableMapOf<String, Connection>()
    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()
    private val rateLimiters = mutableMapOf<String, RateLimiter>()
    private val reconnectJobs = mutableMapOf<String, Job>()
    private val callbacks = mutableListOf<ConnectionCallback>()

    private val _metrics = MutableStateFlow(IPCMetrics())
    val metrics: StateFlow<IPCMetrics> = _metrics.asStateFlow()

    /**
     * Connect to a service endpoint
     *
     * @param endpoint Service endpoint from ARG registry
     * @return ConnectionResult indicating success or failure
     */
    suspend fun connect(endpoint: ServiceEndpoint): ConnectionResult {
        // Check resource limits
        if (connections.size >= resourceLimits.maxConnections) {
            return ConnectionResult.Error(
                IPCError.ResourceExhausted("Max connections (${resourceLimits.maxConnections}) reached")
            )
        }

        // Get or create circuit breaker for this service
        val circuitBreaker = circuitBreakers.getOrPut(endpoint.id) {
            CircuitBreaker(circuitBreakerConfig)
        }

        // Get or create rate limiter for this service
        val rateLimiter = rateLimiters.getOrPut(endpoint.id) {
            RateLimiter(rateLimitConfig)
        }

        // Check rate limit
        if (!rateLimiter.tryAcquire()) {
            val retryAfter = rateLimiter.timeUntilNextToken()
            return ConnectionResult.Error(
                IPCError.RateLimitExceeded(retryAfter)
            )
        }

        // Execute with circuit breaker protection
        val result = circuitBreaker.execute {
            connectInternal(endpoint)
        }

        return if (result.isSuccess) {
            val connection = result.getOrThrow()
            ConnectionResult.Success(connection)
        } else {
            ConnectionResult.Error(
                IPCError.ServiceUnavailable(result.exceptionOrNull()?.message ?: "Unknown error")
            )
        }
    }

    /**
     * Internal connection logic
     */
    private suspend fun connectInternal(endpoint: ServiceEndpoint): Connection {
        val connectionId = generateConnectionId(endpoint)

        // Update state
        val connection = Connection(
            id = connectionId,
            packageName = endpoint.id.substringBefore("."),
            serviceId = endpoint.id,
            state = ConnectionState.CONNECTING,
            protocol = IPCProtocol.AIDL,
            handle = Any()  // Will be replaced by actual implementation
        )

        mutex.withLock {
            connections[connectionId] = connection
        }

        notifyStateChange(connectionId, ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)

        // Simulate connection (actual implementation in platform-specific code)
        delay(100)

        // Update to connected
        val connectedConnection = connection.copy(state = ConnectionState.CONNECTED)
        mutex.withLock {
            connections[connectionId] = connectedConnection
        }

        notifyStateChange(connectionId, ConnectionState.CONNECTING, ConnectionState.CONNECTED)
        notifyConnected(connectedConnection)

        updateMetrics()

        return connectedConnection
    }

    /**
     * Disconnect from a service
     *
     * @param connectionId Connection ID to disconnect
     */
    suspend fun disconnect(connectionId: String) {
        val connection = connections[connectionId] ?: return

        // Cancel any reconnect jobs
        reconnectJobs[connectionId]?.cancel()
        reconnectJobs.remove(connectionId)

        // Update state
        val disconnecting = connection.copy(state = ConnectionState.DISCONNECTING)
        mutex.withLock {
            connections[connectionId] = disconnecting
        }

        notifyStateChange(connectionId, connection.state, ConnectionState.DISCONNECTING)

        // Simulate disconnect
        delay(50)

        // Remove connection
        mutex.withLock {
            connections.remove(connectionId)
        }

        notifyDisconnected(connectionId, "Manual disconnect")
        updateMetrics()
    }

    /**
     * Invoke method on connected service
     *
     * @param connectionId Connection ID
     * @param invocation Method to invoke
     * @return MethodResult with return value or error
     */
    suspend fun invoke(connectionId: String, invocation: MethodInvocation): MethodResult {
        val connection = connections[connectionId]
            ?: return MethodResult.Error(IPCError.ServiceUnavailable("Connection not found"))

        if (connection.state != ConnectionState.CONNECTED) {
            return MethodResult.Error(IPCError.ServiceUnavailable("Connection not in CONNECTED state"))
        }

        // Get rate limiter for this service
        val rateLimiter = rateLimiters[connection.serviceId]
        if (rateLimiter != null && !rateLimiter.tryAcquire()) {
            val retryAfter = rateLimiter.timeUntilNextToken()
            return MethodResult.Error(IPCError.RateLimitExceeded(retryAfter))
        }

        // Simulate method invocation
        return try {
            withTimeout(invocation.timeoutMs) {
                delay(10)  // Simulate work
                MethodResult.Success("Result from ${invocation.methodName}")
            }
        } catch (e: TimeoutCancellationException) {
            MethodResult.Error(IPCError.Timeout(invocation.timeoutMs))
        } catch (e: Exception) {
            MethodResult.Error(IPCError.InvalidResponse(e.message ?: "Unknown error"))
        }
    }

    /**
     * Reconnect to a service with exponential backoff
     *
     * @param endpoint Service endpoint
     * @param attempt Retry attempt number
     */
    private fun scheduleReconnect(endpoint: ServiceEndpoint, attempt: Int = 1) {
        if (!reconnectionPolicy.enabled || attempt > reconnectionPolicy.maxRetries) {
            return
        }

        val delay = reconnectionPolicy.getDelay(attempt)
        val connectionId = generateConnectionId(endpoint)

        val job = scope.launch {
            delay(delay)

            val result = connect(endpoint)
            if (result is ConnectionResult.Error) {
                // Schedule next retry
                scheduleReconnect(endpoint, attempt + 1)
            } else {
                // Success - clear retry job
                reconnectJobs.remove(connectionId)
            }
        }

        reconnectJobs[connectionId] = job
    }

    /**
     * Check if connection is active
     *
     * @param connectionId Connection ID
     * @return true if connected
     */
    fun isConnected(connectionId: String): Boolean {
        return connections[connectionId]?.state == ConnectionState.CONNECTED
    }

    /**
     * Get connection by ID
     *
     * @param connectionId Connection ID
     * @return Connection or null
     */
    fun getConnection(connectionId: String): Connection? {
        return connections[connectionId]
    }

    /**
     * Get all active connections
     *
     * @return List of active connections
     */
    fun getAllConnections(): List<Connection> {
        return connections.values.toList()
    }

    /**
     * Register connection callback
     *
     * @param callback Callback for connection events
     */
    fun registerCallback(callback: ConnectionCallback) {
        callbacks.add(callback)
    }

    /**
     * Unregister connection callback
     *
     * @param callback Callback to remove
     */
    fun unregisterCallback(callback: ConnectionCallback) {
        callbacks.remove(callback)
    }

    /**
     * Manually open circuit breaker for a service
     *
     * @param serviceId Service ID
     * @param reason Reason for opening circuit
     */
    suspend fun openCircuit(serviceId: String, reason: String) {
        circuitBreakers[serviceId]?.open()
    }

    /**
     * Manually close circuit breaker for a service
     *
     * @param serviceId Service ID
     */
    suspend fun closeCircuit(serviceId: String) {
        circuitBreakers[serviceId]?.close()
    }

    /**
     * Drain all connections gracefully
     *
     * @param timeoutMs Maximum time to wait
     */
    suspend fun drainConnections(timeoutMs: Long = 30000) {
        val connectionIds = connections.keys.toList()

        withTimeout(timeoutMs) {
            connectionIds.forEach { connectionId ->
                disconnect(connectionId)
            }
        }
    }

    /**
     * Disable all connections (emergency kill switch)
     */
    suspend fun disableAllConnections() {
        reconnectJobs.values.forEach { it.cancel() }
        reconnectJobs.clear()

        connections.keys.toList().forEach { connectionId ->
            disconnect(connectionId)
        }

        circuitBreakers.values.forEach { it.open() }
    }

    /**
     * Shutdown connection manager
     */
    fun shutdown() {
        scope.cancel()
        reconnectJobs.clear()
        connections.clear()
        circuitBreakers.clear()
        rateLimiters.clear()
        callbacks.clear()
    }

    // Helper methods

    private fun generateConnectionId(endpoint: ServiceEndpoint): String {
        return "${endpoint.id}-${System.currentTimeMillis()}"
    }

    private fun notifyConnected(connection: Connection) {
        callbacks.forEach { callback ->
            callback.onConnected(connection)
        }
    }

    private fun notifyDisconnected(connectionId: String, reason: String) {
        callbacks.forEach { callback ->
            callback.onDisconnected(connectionId, reason)
        }
    }

    private fun notifyError(connectionId: String, error: IPCError) {
        callbacks.forEach { callback ->
            callback.onError(connectionId, error)
        }
    }

    private fun notifyStateChange(connectionId: String, oldState: ConnectionState, newState: ConnectionState) {
        callbacks.forEach { callback ->
            callback.onStateChanged(connectionId, oldState, newState)
        }
    }

    private fun updateMetrics() {
        val activeConnections = connections.values.count { it.state == ConnectionState.CONNECTED }
        val totalConnections = connections.size.toLong()

        _metrics.value = _metrics.value.copy(
            connectionsActive = activeConnections,
            connectionsTotal = totalConnections
        )
    }
}
