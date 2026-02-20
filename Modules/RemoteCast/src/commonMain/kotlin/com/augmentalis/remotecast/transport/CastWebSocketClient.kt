/**
 * CastWebSocketClient.kt — WebSocket-based CAST frame receiver
 *
 * Wraps HTTPAvanue's WebSocketClient to connect to a CastWebSocketServer and
 * receive CAST-framed JPEG binary messages. Replaces the Android-only
 * MjpegTcpClient with a cross-platform transport.
 *
 * The returned Flow<ByteArray> emits raw JPEG data (payload only, header stripped),
 * matching the same API shape as the legacy MjpegTcpClient.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.transport

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.websocket.WebSocketClient
import com.augmentalis.httpavanue.websocket.WebSocketClientConfig
import com.augmentalis.httpavanue.websocket.WebSocketConnectionState
import com.augmentalis.httpavanue.websocket.WebSocketMessage
import com.augmentalis.httpavanue.websocket.WebSocketReconnectConfig
import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * Cross-platform WebSocket client for receiving CAST frames.
 *
 * Usage:
 * ```
 * val client = CastWebSocketClient(scope = viewModelScope)
 * val frames: Flow<ByteArray> = client.connect(host = "192.168.1.50", port = 54321)
 * frames.collect { jpegBytes -> renderFrame(jpegBytes) }
 * // ... on cleanup:
 * client.disconnect()
 * ```
 *
 * @param scope CoroutineScope for managing the WebSocket connection lifecycle.
 */
class CastWebSocketClient(
    private val scope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger("CastWSClient")
    private var wsClient: WebSocketClient? = null

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    /**
     * Connects to a CastWebSocketServer and returns a flow of raw JPEG frames.
     *
     * Each emission is one complete JPEG image (the CAST header is decoded and stripped).
     * The flow completes when the connection is closed by either side.
     *
     * @param host IP address or hostname of the server.
     * @param port TCP port (default 54321).
     * @return Flow of raw JPEG byte arrays.
     */
    suspend fun connect(host: String, port: Int = CastWebSocketServer.DEFAULT_PORT): Flow<ByteArray> {
        val config = WebSocketClientConfig(
            url = "ws://$host:$port/cast/stream",
            connectTimeout = 10_000,
            handshakeTimeout = 10_000,
            maxMessageSize = 10 * 1024 * 1024, // 10 MB max frame
            reconnectConfig = WebSocketReconnectConfig(
                maxRetries = 5,
                baseDelayMs = 2_000,
                maxDelayMs = 30_000,
            ),
        )

        val client = WebSocketClient(config)
        wsClient = client

        client.connect(enableAutoReconnect = true)
        client.start(scope)
        logger.i { "Connected to cast server at $host:$port" }

        // Mirror connection state
        client.connectionState
            .onEach { state -> _connectionState.value = state }
            .launchIn(scope)

        // Transform binary WebSocket messages into raw JPEG byte arrays
        return client.messages.mapNotNull { message ->
            when (message) {
                is WebSocketMessage.Binary -> decodeCastFrame(message.data)
                is WebSocketMessage.Close -> {
                    logger.d { "Server closed connection: ${message.code} ${message.reason}" }
                    null
                }
                is WebSocketMessage.Text -> {
                    logger.d { "Server text: ${message.data}" }
                    null
                }
            }
        }
    }

    /**
     * Disconnects from the server. Safe to call from any thread.
     */
    suspend fun disconnect() {
        wsClient?.close()
        wsClient = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
        logger.i { "Disconnected from cast server" }
    }

    /**
     * Decodes a CAST wire-format message (20-byte header + JPEG payload)
     * and returns just the JPEG bytes, or null if the header is invalid.
     */
    private fun decodeCastFrame(data: ByteArray): ByteArray? {
        if (data.size < CastFrameData.HEADER_SIZE) {
            logger.w { "Received undersized binary message: ${data.size} bytes" }
            return null
        }

        val header = CastFrameData.decodeHeader(data)
        if (header == null) {
            logger.w { "Invalid CAST magic in binary message" }
            return null
        }

        val expectedSize = CastFrameData.HEADER_SIZE + header.payloadSize
        if (data.size < expectedSize) {
            logger.w { "Truncated CAST frame: expected $expectedSize, got ${data.size}" }
            return null
        }

        return data.copyOfRange(CastFrameData.HEADER_SIZE, CastFrameData.HEADER_SIZE + header.payloadSize)
    }
}
