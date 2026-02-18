/**
 * MjpegTcpClient.kt — TCP client that receives MJPEG frames from a MjpegTcpServer
 *
 * Connects to the given host:port, reads 20-byte CAST headers, validates the
 * magic bytes, then reads the declared payload bytes and emits them as a
 * Flow<ByteArray> of raw JPEG data.
 *
 * The flow completes normally when the remote end closes the connection.
 * Call [disconnect] to forcibly close the socket from outside the flow.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.transport

import com.augmentalis.remotecast.protocol.CastFrameData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import java.io.InputStream
import java.net.Socket
import java.net.SocketException

/**
 * Receives MJPEG-over-TCP frames from a [MjpegTcpServer].
 *
 * @param scope CoroutineScope owning connection coroutines. Cancelling the scope
 *              is equivalent to calling [disconnect].
 */
class MjpegTcpClient(
    private val scope: CoroutineScope
) {
    @Volatile private var socket: Socket? = null
    private var connectJob: Job? = null

    /**
     * Connects to [host]:[port] and returns a [Flow] of raw JPEG frame bytes.
     *
     * Each emission is one complete JPEG image (the payload of a single CAST frame).
     * The flow completes when the connection is closed by either side.
     * If the magic bytes in a header are invalid the frame is skipped and reading continues.
     *
     * The returned flow is cold: a new connection is established on each collection.
     *
     * @param host  IP address or hostname of the server.
     * @param port  TCP port (default 54321).
     */
    fun connect(host: String, port: Int = 54321): Flow<ByteArray> = callbackFlow {
        val sock = Socket(host, port)
        socket = sock
        val input: InputStream = sock.getInputStream()
        val headerBuf = ByteArray(CastFrameData.HEADER_SIZE)

        try {
            while (isActive && !sock.isClosed) {
                // Read exactly HEADER_SIZE bytes
                val headerRead = input.readNBytes(headerBuf, 0, CastFrameData.HEADER_SIZE)
                if (headerRead < CastFrameData.HEADER_SIZE) break  // Connection closed mid-header

                val header = CastFrameData.decodeHeader(headerBuf)
                if (header == null) {
                    // Invalid magic — out-of-sync. Close and let the flow complete.
                    break
                }

                val payloadSize = header.payloadSize
                if (payloadSize <= 0 || payloadSize > MAX_FRAME_BYTES) {
                    // Reject unreasonably large or zero-size frames to guard against corruption.
                    break
                }

                val payload = ByteArray(payloadSize)
                val payloadRead = input.readNBytes(payload, 0, payloadSize)
                if (payloadRead < payloadSize) break  // Partial payload — stream ended

                trySend(payload)
            }
        } catch (_: SocketException) {
            // Normal close from disconnect() or remote side
        } catch (_: Exception) {
            // Any other IO error — let the flow complete
        } finally {
            closeSocket()
        }

        awaitClose { closeSocket() }
    }.flowOn(Dispatchers.IO)

    /**
     * Closes the socket, causing the active [connect] flow to complete.
     * Safe to call from any thread or coroutine.
     */
    fun disconnect() {
        closeSocket()
        connectJob?.cancel()
        connectJob = null
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun closeSocket() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    /**
     * Reads exactly [len] bytes from the stream into [buf] starting at [off].
     * Returns the number of bytes actually read; less than [len] means the stream ended.
     */
    private fun InputStream.readNBytes(buf: ByteArray, off: Int, len: Int): Int {
        var totalRead = 0
        while (totalRead < len) {
            val n = read(buf, off + totalRead, len - totalRead)
            if (n == -1) break
            totalRead += n
        }
        return totalRead
    }

    companion object {
        /** Maximum accepted payload size (10 MB) — guards against malformed headers. */
        private const val MAX_FRAME_BYTES = 10 * 1024 * 1024
    }
}
