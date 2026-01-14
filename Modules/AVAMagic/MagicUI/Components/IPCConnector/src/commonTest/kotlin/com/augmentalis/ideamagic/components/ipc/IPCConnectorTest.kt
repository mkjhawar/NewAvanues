package com.augmentalis.magicui.components.ipc

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class IPCConnectorTest {

    // ==================== Circuit Breaker Tests ====================

    @Test
    fun testCircuitBreakerClosed(): Unit = runBlocking {
        val circuitBreaker = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 3))

        // Should allow operations when closed
        val result = circuitBreaker.execute { "success" }
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState())
    }

    @Test
    fun testCircuitBreakerOpensAfterFailures(): Unit = runBlocking {
        val circuitBreaker = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 3))

        // Cause 3 failures
        repeat(3) {
            circuitBreaker.execute { error("failure") }
        }

        // Circuit should be open
        assertEquals(CircuitState.OPEN, circuitBreaker.getState())

        // Should reject requests when open
        val result = circuitBreaker.execute { "attempt" }
        assertTrue(result.isFailure)
    }

    @Test
    fun testCircuitBreakerHalfOpen(): Unit = runBlocking {
        val circuitBreaker = CircuitBreaker(CircuitBreakerConfig(
            failureThreshold = 2,
            timeoutMs = 100
        ))

        // Open the circuit
        repeat(2) {
            circuitBreaker.execute { error("failure") }
        }
        assertEquals(CircuitState.OPEN, circuitBreaker.getState())

        // Wait for timeout
        delay(150)

        // Next request should transition to HALF_OPEN
        circuitBreaker.execute { "test" }
        // Circuit allows testing in HALF_OPEN state
    }

    @Test
    fun testCircuitBreakerReset(): Unit = runBlocking {
        val circuitBreaker = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 2))

        // Open circuit
        repeat(2) {
            circuitBreaker.execute { error("failure") }
        }
        assertEquals(CircuitState.OPEN, circuitBreaker.getState())

        // Reset
        circuitBreaker.reset()
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState())
        assertEquals(0, circuitBreaker.getFailureCount())
    }

    // ==================== Rate Limiter Tests ====================

    @Test
    fun testRateLimiterAllowsRequests(): Unit = runBlocking {
        val rateLimiter = RateLimiter(RateLimitConfig(
            maxRequestsPerSecond = 10,
            burstSize = 20
        ))

        // Should allow up to burst size immediately
        repeat(20) {
            assertTrue(rateLimiter.tryAcquire(), "Request $it should be allowed")
        }
    }

    @Test
    fun testRateLimiterRejectsExcess(): Unit = runBlocking {
        val rateLimiter = RateLimiter(RateLimitConfig(
            maxRequestsPerSecond = 10,
            burstSize = 5
        ))

        // Exhaust burst
        repeat(5) {
            assertTrue(rateLimiter.tryAcquire())
        }

        // Next request should be rejected
        assertFalse(rateLimiter.tryAcquire())
    }

    @Test
    fun testRateLimiterRefillsTokens(): Unit = runBlocking {
        val rateLimiter = RateLimiter(RateLimitConfig(
            maxRequestsPerSecond = 10,
            burstSize = 5
        ))

        // Exhaust tokens
        repeat(5) {
            rateLimiter.tryAcquire()
        }

        // Wait for refill (100ms = 1 token at 10/sec)
        delay(150)

        // Should allow 1 more request
        assertTrue(rateLimiter.tryAcquire())
    }

    @Test
    fun testRateLimiterTimeUntilNext(): Unit = runBlocking {
        val rateLimiter = RateLimiter(RateLimitConfig(
            maxRequestsPerSecond = 10,
            burstSize = 2
        ))

        // Exhaust tokens
        repeat(2) {
            rateLimiter.tryAcquire()
        }

        val timeUntil = rateLimiter.timeUntilNextToken()
        assertTrue(timeUntil > 0, "Should have wait time")
        assertTrue(timeUntil <= 200, "Wait time should be ~100ms for 1 token at 10/sec")
    }

    @Test
    fun testRateLimiterReset(): Unit = runBlocking {
        val rateLimiter = RateLimiter(RateLimitConfig(burstSize = 3))

        // Exhaust tokens
        repeat(3) {
            rateLimiter.tryAcquire()
        }

        // Reset
        rateLimiter.reset()

        // Should allow requests again
        assertTrue(rateLimiter.tryAcquire())
    }

    // ==================== Connection Manager Tests ====================

    @Test
    fun testConnectionManagerResourceLimit(): Unit = runBlocking {
        val registry = com.augmentalis.avanues.avamagic.components.argscanner.ARGRegistry()
        val manager = ConnectionManager(
            registry = registry,
            resourceLimits = ResourceLimits(maxConnections = 2)
        )

        // Note: Would need mock endpoints to test fully
        // This tests the basic structure
        assertTrue(manager.getAllConnections().isEmpty())
    }

    @Test
    fun testConnectionManagerCallbacks(): Unit = runBlocking {
        val registry = com.augmentalis.avanues.avamagic.components.argscanner.ARGRegistry()
        val manager = ConnectionManager(registry)

        var connectedCalled = false
        var disconnectedCalled = false

        val callback = object : ConnectionCallback {
            override fun onConnected(connection: Connection) {
                connectedCalled = true
            }

            override fun onDisconnected(connectionId: String, reason: String) {
                disconnectedCalled = true
            }

            override fun onError(connectionId: String, error: IPCError) {}
            override fun onStateChanged(connectionId: String, oldState: ConnectionState, newState: ConnectionState) {}
        }

        manager.registerCallback(callback)

        // Note: Would need to trigger actual connection to test callbacks
        manager.unregisterCallback(callback)
    }

    // ==================== Reconnection Policy Tests ====================

    @Test
    fun testReconnectionPolicyDelay() {
        val policy = ReconnectionPolicy(
            initialDelayMs = 1000,
            backoffMultiplier = 2.0f,
            maxDelayMs = 10000
        )

        assertEquals(1000, policy.getDelay(1))
        assertEquals(2000, policy.getDelay(2))
        assertEquals(4000, policy.getDelay(3))
        assertEquals(8000, policy.getDelay(4))
        assertEquals(10000, policy.getDelay(5))  // Capped at max
    }

    // ==================== IPC Models Tests ====================

    @Test
    fun testConnectionState() {
        val states = ConnectionState.values()
        assertTrue(states.contains(ConnectionState.DISCONNECTED))
        assertTrue(states.contains(ConnectionState.CONNECTING))
        assertTrue(states.contains(ConnectionState.CONNECTED))
        assertTrue(states.contains(ConnectionState.FAILED))
        assertTrue(states.contains(ConnectionState.RECONNECTING))
        assertTrue(states.contains(ConnectionState.DISCONNECTING))
    }

    @Test
    fun testIPCProtocol() {
        val protocols = IPCProtocol.values()
        assertTrue(protocols.contains(IPCProtocol.AIDL))
        assertTrue(protocols.contains(IPCProtocol.CONTENT_PROVIDER))
        assertTrue(protocols.contains(IPCProtocol.WEBSOCKET))
    }

    @Test
    fun testCircuitBreakerConfig() {
        val config = CircuitBreakerConfig(
            failureThreshold = 10,
            successThreshold = 3,
            timeoutMs = 5000
        )

        assertEquals(10, config.failureThreshold)
        assertEquals(3, config.successThreshold)
        assertEquals(5000, config.timeoutMs)
    }

    @Test
    fun testRateLimitConfig() {
        val config = RateLimitConfig(
            maxRequestsPerSecond = 100,
            burstSize = 200
        )

        assertEquals(100, config.maxRequestsPerSecond)
        assertEquals(200, config.burstSize)
    }

    @Test
    fun testResourceLimits() {
        val limits = ResourceLimits(
            maxConnections = 50,
            maxMessageSize = 2_000_000,
            connectionTimeoutMs = 10000
        )

        assertEquals(50, limits.maxConnections)
        assertEquals(2_000_000, limits.maxMessageSize)
        assertEquals(10000, limits.connectionTimeoutMs)
    }

    @Test
    fun testMethodInvocation() {
        val invocation = MethodInvocation(
            methodName = "getUserData",
            parameters = mapOf("userId" to "123"),
            timeoutMs = 5000
        )

        assertEquals("getUserData", invocation.methodName)
        assertEquals("123", invocation.parameters["userId"])
        assertEquals(5000, invocation.timeoutMs)
    }

    @Test
    fun testQueryParams() {
        val params = QueryParams(
            uri = "content://com.app.provider/users",
            projection = listOf("id", "name", "email"),
            selection = "age > ?",
            selectionArgs = listOf("18"),
            sortOrder = "name ASC",
            limit = 10
        )

        assertEquals("content://com.app.provider/users", params.uri)
        assertEquals(3, params.projection?.size)
        assertEquals("age > ?", params.selection)
        assertEquals("18", params.selectionArgs?.get(0))
        assertEquals("name ASC", params.sortOrder)
        assertEquals(10, params.limit)
    }

    @Test
    fun testQueryResult() {
        val rows = listOf(
            mapOf("id" to 1, "name" to "Alice"),
            mapOf("id" to 2, "name" to "Bob")
        )

        val result = QueryResult(rows, rows.size)

        assertEquals(2, result.count)
        assertEquals("Alice", result.rows[0]["name"])
        assertEquals("Bob", result.rows[1]["name"])
    }

    @Test
    fun testIPCMetrics() {
        val metrics = IPCMetrics(
            connectionsActive = 5,
            connectionsTotal = 100,
            connectionsFailed = 10,
            averageLatencyMs = 15.5,
            errorRate = 0.1f
        )

        assertEquals(5, metrics.connectionsActive)
        assertEquals(100, metrics.connectionsTotal)
        assertEquals(10, metrics.connectionsFailed)
        assertEquals(15.5, metrics.averageLatencyMs)
        assertEquals(0.1f, metrics.errorRate)
    }
}
