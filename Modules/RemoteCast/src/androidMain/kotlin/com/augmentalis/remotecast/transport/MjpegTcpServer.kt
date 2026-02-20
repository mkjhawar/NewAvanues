/**
 * MjpegTcpServer.kt — TCP server that streams MJPEG frames to a single connected client
 *
 * Binds to the given port, accepts exactly one client at a time, and streams
 * CAST-protocol frames (20-byte header + JPEG payload) over the connection.
 * When a client disconnects the server immediately waits for the next connection.
 *
 * Uses Java NIO via standard java.net.ServerSocket wrapped in Kotlin coroutines
 * (Dispatchers.IO) so the blocking accept/write calls do not block the caller.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.transport

import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Single-client MJPEG TCP server.
 *
 * @param port  TCP port to bind on (default 54321).
 * @param scope CoroutineScope that owns the accept loop. Cancelling the scope stops the server.
 */
@Deprecated(
    message = "Use CastWebSocketServer from commonMain transport package instead",
    replaceWith = ReplaceWith(
        "CastWebSocketServer(port, scope)",
        "com.augmentalis.remotecast.transport.CastWebSocketServer"
    )
)
class MjpegTcpServer(
    private val port: Int = 54321,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientOutputStream: OutputStream? = null
    private var acceptJob: Job? = null

    private val writeMutex = Mutex()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _clientConnected = MutableStateFlow(false)
    val clientConnected: StateFlow<Boolean> = _clientConnected.asStateFlow()

    /**
     * Starts the server by binding to [port] and launching an accept loop.
     * Suspends until the server socket is bound and ready.
     * Safe to call multiple times — if already running, this is a no-op.
     */
    suspend fun start() {
        if (_isRunning.value) return

        withContext(Dispatchers.IO) {
            val ss = ServerSocket(port).also {
                it.reuseAddress = true
                it.soTimeout = 0  // Block indefinitely on accept()
            }
            serverSocket = ss
            _isRunning.value = true

            acceptJob = scope.launch(Dispatchers.IO) {
                acceptLoop(ss)
            }
        }
    }

    /**
     * Sends a single encoded frame to the connected client.
     * If no client is connected, the frame is silently dropped.
     * Thread-safe — concurrent calls are serialised via [writeMutex].
     */
    suspend fun sendFrame(frameData: CastFrameData) {
        if (!_clientConnected.value) return

        writeMutex.withLock {
            val out = clientOutputStream ?: return
            try {
                val packet = CastFrameData.buildPacket(frameData)
                withContext(Dispatchers.IO) {
                    out.write(packet)
                    out.flush()
                }
            } catch (e: SocketException) {
                handleClientDisconnect()
            } catch (e: Exception) {
                handleClientDisconnect()
            }
        }
    }

    /**
     * Stops the server, closes all sockets, and resets state.
     * Safe to call on any thread.
     */
    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        closeClientSocket()
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        _isRunning.value = false
        _clientConnected.value = false
    }

    // ── Private ──────────────────────────────────────────────────────────────

    /** Runs on an IO dispatcher; loops accepting clients until the scope is cancelled. */
    private suspend fun acceptLoop(ss: ServerSocket) {
        while (scope.isActive && !ss.isClosed) {
            try {
                val client = ss.accept()  // Blocks until a client connects
                // Only one client at a time: close any previous
                closeClientSocket()
                clientSocket = client
                clientOutputStream = client.getOutputStream()
                _clientConnected.value = true
            } catch (e: SocketException) {
                // ServerSocket was closed — stop the loop
                break
            } catch (e: Exception) {
                // Transient accept error; continue accepting
                _clientConnected.value = false
            }
        }
    }

    private fun handleClientDisconnect() {
        closeClientSocket()
        _clientConnected.value = false
    }

    private fun closeClientSocket() {
        try { clientOutputStream?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        clientOutputStream = null
        clientSocket = null
    }
}
