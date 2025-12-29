/**
 * ClientConfig.kt - Client connection configuration
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 */
package com.augmentalis.universalrpc.client

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for UniversalRPC client connections.
 * Supports multiple transport protocols and connection strategies.
 */
data class ClientConfig(
    val host: String = "localhost",
    val port: Int = 50051,
    val protocol: Protocol = Protocol.GRPC,
    val useTls: Boolean = false,
    val connectionTimeout: Duration = 30.seconds,
    val requestTimeout: Duration = 60.seconds,
    val keepAliveInterval: Duration = 30.seconds,
    val maxRetryAttempts: Int = 3,
    val retryDelayBase: Duration = 1.seconds,
    val retryDelayMax: Duration = 30.seconds,
    val autoReconnect: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Supported transport protocols
     */
    enum class Protocol {
        /** gRPC over HTTP/2 */
        GRPC,
        /** Unix Domain Socket (Android/iOS/Desktop) */
        UDS,
        /** WebSocket (Web, fallback) */
        WEBSOCKET,
        /** HTTP/2 with JSON (fallback) */
        HTTP2_JSON
    }

    /**
     * Returns the connection address based on protocol
     */
    val address: String
        get() = when (protocol) {
            Protocol.UDS -> host
            Protocol.WEBSOCKET -> {
                val scheme = if (useTls) "wss" else "ws"
                "$scheme://$host:$port"
            }
            Protocol.HTTP2_JSON -> {
                val scheme = if (useTls) "https" else "http"
                "$scheme://$host:$port"
            }
            Protocol.GRPC -> "$host:$port"
        }

    /**
     * Builder for creating ClientConfig with fluent API
     */
    class Builder {
        private var host: String = "localhost"
        private var port: Int = 50051
        private var protocol: Protocol = Protocol.GRPC
        private var useTls: Boolean = false
        private var connectionTimeout: Duration = 30.seconds
        private var requestTimeout: Duration = 60.seconds
        private var keepAliveInterval: Duration = 30.seconds
        private var maxRetryAttempts: Int = 3
        private var retryDelayBase: Duration = 1.seconds
        private var retryDelayMax: Duration = 30.seconds
        private var autoReconnect: Boolean = true
        private var metadata: MutableMap<String, String> = mutableMapOf()

        fun host(host: String) = apply { this.host = host }
        fun port(port: Int) = apply { this.port = port }
        fun protocol(protocol: Protocol) = apply { this.protocol = protocol }
        fun useTls(useTls: Boolean) = apply { this.useTls = useTls }
        fun connectionTimeout(timeout: Duration) = apply { this.connectionTimeout = timeout }
        fun requestTimeout(timeout: Duration) = apply { this.requestTimeout = timeout }
        fun keepAliveInterval(interval: Duration) = apply { this.keepAliveInterval = interval }
        fun maxRetryAttempts(attempts: Int) = apply { this.maxRetryAttempts = attempts }
        fun retryDelayBase(delay: Duration) = apply { this.retryDelayBase = delay }
        fun retryDelayMax(delay: Duration) = apply { this.retryDelayMax = delay }
        fun autoReconnect(autoReconnect: Boolean) = apply { this.autoReconnect = autoReconnect }
        fun addMetadata(key: String, value: String) = apply { this.metadata[key] = value }
        fun metadata(metadata: Map<String, String>) = apply { this.metadata.putAll(metadata) }

        fun build(): ClientConfig = ClientConfig(
            host = host,
            port = port,
            protocol = protocol,
            useTls = useTls,
            connectionTimeout = connectionTimeout,
            requestTimeout = requestTimeout,
            keepAliveInterval = keepAliveInterval,
            maxRetryAttempts = maxRetryAttempts,
            retryDelayBase = retryDelayBase,
            retryDelayMax = retryDelayMax,
            autoReconnect = autoReconnect,
            metadata = metadata.toMap()
        )
    }

    companion object {
        /** Default configuration for local development */
        val DEFAULT = ClientConfig()

        /** Configuration for VoiceOS service */
        fun forVoiceOS(host: String = "localhost"): ClientConfig = ClientConfig(
            host = host,
            port = 50051,
            protocol = Protocol.GRPC
        )

        /** Configuration for AVA service */
        fun forAVA(host: String = "localhost"): ClientConfig = ClientConfig(
            host = host,
            port = 50052,
            protocol = Protocol.GRPC
        )

        /** Configuration for Cockpit service */
        fun forCockpit(host: String = "localhost"): ClientConfig = ClientConfig(
            host = host,
            port = 50053,
            protocol = Protocol.GRPC
        )

        /** Configuration for NLU service */
        fun forNLU(host: String = "localhost"): ClientConfig = ClientConfig(
            host = host,
            port = 50054,
            protocol = Protocol.GRPC
        )

        /** Configuration for WebAvanue service */
        fun forWebAvanue(host: String = "localhost"): ClientConfig = ClientConfig(
            host = host,
            port = 50055,
            protocol = Protocol.WEBSOCKET,
            useTls = true
        )

        /** Configuration for Unix Domain Socket (Android/iOS/Desktop) */
        fun forUDS(socketPath: String): ClientConfig = ClientConfig(
            host = socketPath,
            port = 0,
            protocol = Protocol.UDS
        )

        /** Create a builder for custom configuration */
        fun builder(): Builder = Builder()
    }
}

/**
 * Retry strategy for connection attempts
 */
data class RetryStrategy(
    val maxAttempts: Int = 3,
    val baseDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds,
    val multiplier: Double = 2.0,
    val jitterFactor: Double = 0.1
) {
    /**
     * Calculate delay for a given attempt number (1-indexed)
     */
    fun delayForAttempt(attempt: Int): Duration {
        require(attempt > 0) { "Attempt must be positive" }
        if (attempt > maxAttempts) return maxDelay

        val exponentialDelay = baseDelay * multiplier.pow(attempt - 1)
        val cappedDelay = minOf(exponentialDelay, maxDelay)

        // Add jitter
        val jitterRange = cappedDelay.inWholeMilliseconds * jitterFactor
        val jitter = ((-jitterRange).toLong()..(jitterRange.toLong())).random()

        return Duration.parse("${cappedDelay.inWholeMilliseconds + jitter}ms")
    }

    private fun Double.pow(n: Int): Double {
        var result = 1.0
        repeat(n) { result *= this }
        return result
    }

    companion object {
        val DEFAULT = RetryStrategy()
        val AGGRESSIVE = RetryStrategy(maxAttempts = 10, baseDelay = 500.milliseconds, maxDelay = 5.seconds)
        val CONSERVATIVE = RetryStrategy(maxAttempts = 3, baseDelay = 2.seconds, maxDelay = 60.seconds)

        private val Int.milliseconds: Duration get() = Duration.parse("${this}ms")
    }
}
