/**
 * CastWebSocketServer.kt — WebSocket-based CAST frame server
 *
 * Wraps HTTPAvanue's HttpServer + WebSocket handler to stream CAST-framed
 * JPEG data over WebSocket binary messages. Replaces the Android-only
 * MjpegTcpServer with a cross-platform transport.
 *
 * Endpoints:
 *   WS  /cast/stream   — Binary WebSocket stream of CAST frames
 *   GET /cast/status    — JSON status payload
 *   GET /cast/health    — Simple health check
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.transport

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.platform.Socket
import com.augmentalis.httpavanue.platform.currentTimeMillis
import com.augmentalis.httpavanue.server.HttpServer
import com.augmentalis.httpavanue.server.ServerConfig
import com.augmentalis.httpavanue.websocket.WebSocket
import com.augmentalis.httpavanue.websocket.WebSocketMessage
import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cross-platform WebSocket server for streaming CAST frames.
 *
 * Usage:
 * ```
 * val server = CastWebSocketServer(port = 54321, scope = viewModelScope)
 * server.start()
 * // ... in capture loop:
 * server.sendFrame(frameData)
 * // ... on cleanup:
 * server.stop()
 * ```
 *
 * @param port  TCP port to bind (default 54321).
 * @param scope CoroutineScope for managing WebSocket connections.
 */
class CastWebSocketServer(
    private val port: Int = DEFAULT_PORT,
    private val scope: CoroutineScope,
) {
    private val logger = LoggerFactory.getLogger("CastWSServer")

    private var httpServer: HttpServer? = null
    private val clientMutex = Mutex()
    private val connectedClients = mutableListOf<WebSocket>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _clientConnected = MutableStateFlow(false)
    val clientConnected: StateFlow<Boolean> = _clientConnected.asStateFlow()

    private var frameCount = 0L
    private var startTimeMs = 0L

    /**
     * Starts the HTTP + WebSocket server. Safe to call multiple times.
     */
    fun start() {
        if (_isRunning.value) return

        val config = ServerConfig(port = port, http2Enabled = false)
        val server = HttpServer(config)

        // REST endpoints
        server.routes {
            get("/cast/status") { _ ->
                val status = CastStatus(
                    isRunning = _isRunning.value,
                    clientCount = connectedClients.size,
                    framesSent = frameCount,
                    uptimeMs = if (startTimeMs > 0) currentTimeMillis() - startTimeMs else 0,
                )
                HttpResponse.json(Json.encodeToString(CastStatus.serializer(), status))
            }
            get("/cast/health") { _ ->
                HttpResponse.ok("ok")
            }
        }

        // WebSocket handler for frame streaming
        server.websocket("/cast/stream") { socket ->
            handleWebSocketClient(socket)
        }

        server.start()
        httpServer = server
        _isRunning.value = true
        startTimeMs = currentTimeMillis()
        frameCount = 0L
        logger.i { "Cast WebSocket server started on port $port" }
    }

    /**
     * Sends a CAST-framed JPEG to all connected WebSocket clients.
     * Uses the existing CAST wire protocol (20-byte header + JPEG payload)
     * for backward compatibility. Clients that don't understand the header
     * can skip the first 20 bytes.
     *
     * If no clients are connected, the frame is silently dropped.
     */
    suspend fun sendFrame(frameData: CastFrameData) {
        val clients = clientMutex.withLock { connectedClients.toList() }
        if (clients.isEmpty()) return

        val packet = CastFrameData.buildPacket(frameData)
        frameCount++

        val deadClients = mutableListOf<WebSocket>()
        for (ws in clients) {
            try {
                if (ws.isOpen()) {
                    ws.sendBinary(packet)
                } else {
                    deadClients.add(ws)
                }
            } catch (e: Exception) {
                logger.w { "Failed to send frame to client: ${e.message}" }
                deadClients.add(ws)
            }
        }

        if (deadClients.isNotEmpty()) {
            clientMutex.withLock {
                connectedClients.removeAll(deadClients.toSet())
                _clientConnected.value = connectedClients.isNotEmpty()
            }
        }
    }

    /**
     * Stops the server and disconnects all clients.
     */
    suspend fun stop() {
        clientMutex.withLock {
            connectedClients.forEach { ws ->
                try { ws.close() } catch (_: Exception) {}
            }
            connectedClients.clear()
            _clientConnected.value = false
        }
        httpServer?.stop()
        httpServer = null
        _isRunning.value = false
        logger.i { "Cast WebSocket server stopped" }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private suspend fun handleWebSocketClient(socket: Socket) {
        val ws = WebSocket(socket, isServer = true, maxMessageSize = 1024)
        ws.start(scope)

        clientMutex.withLock {
            connectedClients.add(ws)
            _clientConnected.value = true
        }
        logger.i { "Cast client connected (total: ${connectedClients.size})" }

        try {
            // Keep the connection open — this is a server-push stream.
            // Listen for close/ping from the client.
            ws.messages.collect { message ->
                when (message) {
                    is WebSocketMessage.Close -> {
                        logger.d { "Cast client sent close" }
                    }
                    is WebSocketMessage.Text -> {
                        // Clients can send text commands (future: quality change, pause)
                        logger.d { "Cast client text: ${message.data}" }
                    }
                    is WebSocketMessage.Binary -> {
                        // Not expected on a server-push stream, but harmless
                    }
                }
            }
        } catch (_: Exception) {
            // Flow completed — client disconnected
        } finally {
            clientMutex.withLock {
                connectedClients.remove(ws)
                _clientConnected.value = connectedClients.isNotEmpty()
            }
            logger.i { "Cast client disconnected (remaining: ${connectedClients.size})" }
        }
    }

    companion object {
        const val DEFAULT_PORT = 54321
    }
}

/** Simple status model for the /cast/status endpoint. */
@Serializable
private data class CastStatus(
    val isRunning: Boolean,
    val clientCount: Int,
    val framesSent: Long,
    val uptimeMs: Long,
)
