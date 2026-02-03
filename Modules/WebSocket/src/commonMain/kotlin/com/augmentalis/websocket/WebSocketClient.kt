package com.augmentalis.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * WebSocket connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val sessionId: String) : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
    data class Error(val message: String, val throwable: Throwable? = null) : ConnectionState()
}

/**
 * Sync state for tracking synchronization
 */
@Serializable
enum class SyncState {
    IDLE,
    CONNECTING,
    CONNECTED,
    SYNCING,
    DISCONNECTED,
    ERROR,
    OFFLINE
}

/**
 * Sync status for monitoring
 */
@Serializable
data class SyncStatus(
    val lastSyncTimestamp: Instant?,
    val pendingEventCount: Int,
    val syncState: SyncState,
    val errorMessage: String? = null
)

/**
 * Core WebSocket client interface
 *
 * Platform-agnostic WebSocket client for real-time communication.
 * Implementations provided for Android (OkHttp) and iOS (NSURLSession).
 */
interface WebSocketClient {

    /** Current connection state */
    val connectionState: StateFlow<ConnectionState>

    /** Flow of incoming text messages */
    val incomingMessages: Flow<String>

    /** Flow of incoming binary messages */
    val incomingBinaryMessages: Flow<ByteArray>

    /**
     * Connect to WebSocket server
     * @param url WebSocket URL (ws:// or wss://)
     * @param headers Optional HTTP headers for connection
     */
    suspend fun connect(url: String, headers: Map<String, String> = emptyMap()): Result<Unit>

    /**
     * Disconnect from server
     * @param code Close code (default: 1000 = normal closure)
     * @param reason Close reason message
     */
    suspend fun disconnect(code: Int = 1000, reason: String = "Client disconnect")

    /**
     * Send text message
     */
    suspend fun send(message: String): Result<Unit>

    /**
     * Send binary message
     */
    suspend fun sendBinary(data: ByteArray): Result<Unit>

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean
}

/**
 * WebSocket client configuration
 */
@Serializable
data class WebSocketClientConfig(
    /** Connection timeout in milliseconds */
    val connectionTimeoutMs: Long = 30_000,

    /** Read timeout in milliseconds */
    val readTimeoutMs: Long = 30_000,

    /** Write timeout in milliseconds */
    val writeTimeoutMs: Long = 30_000,

    /** Ping interval for keep-alive (0 = disabled) */
    val pingIntervalMs: Long = 30_000,

    /** Maximum message size in bytes */
    val maxMessageSize: Long = 1_048_576, // 1MB

    /** Enable automatic reconnection */
    val autoReconnect: Boolean = true,

    /** Maximum reconnection attempts */
    val maxReconnectAttempts: Int = 5,

    /** Initial reconnection delay in milliseconds */
    val initialReconnectDelayMs: Long = 1_000,

    /** Maximum reconnection delay in milliseconds */
    val maxReconnectDelayMs: Long = 60_000,

    /** Reconnection delay multiplier (exponential backoff) */
    val reconnectDelayMultiplier: Double = 2.0,

    /** Enable debug logging */
    val debugLogging: Boolean = false
) {
    companion object {
        val DEFAULT = WebSocketClientConfig()

        fun development() = WebSocketClientConfig(
            debugLogging = true,
            maxReconnectAttempts = 3
        )

        fun production() = WebSocketClientConfig(
            debugLogging = false,
            autoReconnect = true
        )
    }
}

/**
 * Factory for creating platform-specific WebSocket clients
 */
expect fun createWebSocketClient(config: WebSocketClientConfig = WebSocketClientConfig.DEFAULT): WebSocketClient

/**
 * Get platform name
 */
expect fun getPlatformName(): String
