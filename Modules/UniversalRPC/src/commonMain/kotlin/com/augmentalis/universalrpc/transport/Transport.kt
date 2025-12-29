/**
 * Transport.kt - Common transport interface for UniversalRPC
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Defines the transport abstraction layer for cross-platform IPC.
 * Implementations include Unix Domain Sockets (Android/Desktop),
 * TCP sockets, and platform-specific optimizations.
 */
package com.augmentalis.universalrpc.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport state enumeration
 */
enum class TransportState {
    /** Transport is not connected */
    DISCONNECTED,
    /** Transport is attempting to connect */
    CONNECTING,
    /** Transport is connected and ready */
    CONNECTED,
    /** Transport is attempting to reconnect after failure */
    RECONNECTING,
    /** Transport has failed and cannot recover */
    FAILED,
    /** Transport is shutting down */
    CLOSING
}

/**
 * Transport configuration
 */
data class TransportConfig(
    /** Connection timeout in milliseconds */
    val connectionTimeoutMs: Long = 5000L,
    /** Read timeout in milliseconds */
    val readTimeoutMs: Long = 30000L,
    /** Write timeout in milliseconds */
    val writeTimeoutMs: Long = 10000L,
    /** Maximum retry attempts for reconnection */
    val maxRetryAttempts: Int = 3,
    /** Base delay between retry attempts in milliseconds */
    val retryDelayMs: Long = 1000L,
    /** Buffer size for read/write operations */
    val bufferSize: Int = 8192,
    /** Enable automatic reconnection */
    val autoReconnect: Boolean = true,
    /** Keep-alive interval in milliseconds (0 to disable) */
    val keepAliveIntervalMs: Long = 30000L
)

/**
 * Transport event for monitoring
 */
sealed class TransportEvent {
    data class StateChanged(val oldState: TransportState, val newState: TransportState) : TransportEvent()
    data class DataReceived(val bytes: Int) : TransportEvent()
    data class DataSent(val bytes: Int) : TransportEvent()
    data class Error(val throwable: Throwable) : TransportEvent()
    data class ReconnectAttempt(val attempt: Int, val maxAttempts: Int) : TransportEvent()
    object KeepAlive : TransportEvent()
}

/**
 * Transport listener interface
 */
interface TransportListener {
    fun onStateChanged(oldState: TransportState, newState: TransportState)
    fun onDataReceived(data: ByteArray)
    fun onError(error: Throwable)
}

/**
 * Common transport interface for all platforms
 *
 * Implementations must be thread-safe and handle concurrent
 * read/write operations properly.
 */
interface Transport {
    companion object

    /** Current transport state */
    val state: StateFlow<TransportState>

    /** Transport events flow */
    val events: Flow<TransportEvent>

    /** Transport configuration */
    val config: TransportConfig

    /** Whether the transport is currently connected */
    val isConnected: Boolean
        get() = state.value == TransportState.CONNECTED

    /**
     * Connect the transport
     *
     * @throws TransportException if connection fails
     */
    suspend fun connect()

    /**
     * Disconnect the transport
     *
     * @param graceful If true, waits for pending operations to complete
     */
    suspend fun disconnect(graceful: Boolean = true)

    /**
     * Send data through the transport
     *
     * @param data The data to send
     * @throws TransportException if send fails
     */
    suspend fun send(data: ByteArray)

    /**
     * Receive data from the transport
     *
     * @return The received data
     * @throws TransportException if receive fails
     */
    suspend fun receive(): ByteArray

    /**
     * Add a transport listener
     */
    fun addListener(listener: TransportListener)

    /**
     * Remove a transport listener
     */
    fun removeListener(listener: TransportListener)

    /**
     * Close the transport and release all resources
     */
    suspend fun close()
}

/**
 * Server-side transport for accepting connections
 */
interface ServerTransport {
    /** Current server state */
    val state: StateFlow<TransportState>

    /** Server events flow */
    val events: Flow<TransportEvent>

    /** Whether the server is currently listening */
    val isListening: Boolean
        get() = state.value == TransportState.CONNECTED

    /**
     * Start listening for connections
     *
     * @throws TransportException if server cannot start
     */
    suspend fun start()

    /**
     * Stop the server
     *
     * @param graceful If true, waits for existing connections to complete
     */
    suspend fun stop(graceful: Boolean = true)

    /**
     * Accept a new client connection
     *
     * @return A Transport instance for the new connection
     * @throws TransportException if accept fails
     */
    suspend fun accept(): Transport

    /**
     * Get all active client connections
     */
    fun getConnections(): List<Transport>

    /**
     * Close the server and all connections
     */
    suspend fun close()
}

/**
 * Transport exception wrapper
 */
class TransportException(
    message: String,
    cause: Throwable? = null,
    val isRecoverable: Boolean = true
) : Exception(message, cause)

/**
 * Transport type enumeration
 */
enum class TransportType {
    /** Unix Domain Socket - fastest for same-device IPC */
    UNIX_DOMAIN_SOCKET,
    /** TCP Socket - for cross-device communication */
    TCP,
    /** In-memory transport for testing */
    IN_MEMORY,
    /** Platform-specific optimized transport */
    PLATFORM_NATIVE
}

/**
 * Transport address abstraction
 */
sealed class TransportAddress {
    /**
     * Unix Domain Socket address
     * @param path Socket file path or abstract namespace name
     * @param abstract If true, uses abstract namespace (no filesystem path)
     */
    data class UnixSocket(
        val path: String,
        val abstract: Boolean = true
    ) : TransportAddress() {
        override fun toString(): String = if (abstract) "@$path" else path
    }

    /**
     * TCP socket address
     * @param host Host address
     * @param port Port number
     */
    data class TcpSocket(
        val host: String,
        val port: Int
    ) : TransportAddress() {
        override fun toString(): String = "$host:$port"
    }

    /**
     * In-memory address for testing
     */
    data class InMemory(val name: String) : TransportAddress() {
        override fun toString(): String = "memory://$name"
    }
}
