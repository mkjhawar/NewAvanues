package com.augmentalis.netavanue.ice

import com.avanues.logging.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Android (JVM) implementation of [UdpSocket] backed by [DatagramSocket].
 *
 * All blocking socket operations are wrapped in `withContext(Dispatchers.IO)`
 * to avoid blocking the caller's coroutine dispatcher.
 */
actual class UdpSocket actual constructor() {

    private val logger = LoggerFactory.getLogger("UdpSocket")
    private var socket: DatagramSocket? = null

    actual val localPort: Int
        get() = socket?.localPort ?: -1

    actual val isClosed: Boolean
        get() = socket?.isClosed ?: true

    actual suspend fun bind(port: Int) {
        withContext(Dispatchers.IO) {
            try {
                val s = DatagramSocket(port)
                // Allow address reuse for quick rebind after close
                s.reuseAddress = true
                socket = s
                logger.d { "Bound UDP socket on port ${s.localPort}" }
            } catch (e: SocketException) {
                logger.e({ "Failed to bind UDP socket on port $port: ${e.message}" }, e)
                throw e
            }
        }
    }

    actual suspend fun send(data: ByteArray, host: String, port: Int) {
        val s = socket ?: throw IllegalStateException("Socket not bound. Call bind() first.")
        withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(host)
                val packet = DatagramPacket(data, data.size, address, port)
                s.send(packet)
            } catch (e: SocketException) {
                if (!s.isClosed) {
                    logger.e({ "Send failed to $host:$port: ${e.message}" }, e)
                }
                throw e
            } catch (e: Exception) {
                logger.e({ "Send failed to $host:$port: ${e.message}" }, e)
                throw e
            }
        }
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        val s = socket ?: throw IllegalStateException("Socket not bound. Call bind() first.")
        return withContext(Dispatchers.IO) {
            try {
                // Set the socket timeout so the blocking receive() will throw
                // SocketTimeoutException if no packet arrives in time. This is
                // more efficient than withTimeoutOrNull which would cancel the
                // coroutine and leave the socket in an undefined state.
                s.soTimeout = timeoutMs.toInt().coerceAtLeast(1)
                val packet = DatagramPacket(buffer, buffer.size)
                s.receive(packet)
                UdpPacket(
                    data = buffer,
                    length = packet.length,
                    remoteHost = packet.address.hostAddress ?: packet.address.hostName,
                    remotePort = packet.port,
                )
            } catch (_: SocketTimeoutException) {
                // Normal timeout -- no data arrived within the window
                null
            } catch (_: SocketException) {
                // Socket was closed while we were blocking on receive
                if (s.isClosed) {
                    null
                } else {
                    logger.w { "SocketException during receive (socket still open)" }
                    null
                }
            }
        }
    }

    actual fun close() {
        try {
            socket?.close()
            logger.d { "UDP socket closed" }
        } catch (e: Exception) {
            logger.w { "Error closing UDP socket: ${e.message}" }
        } finally {
            socket = null
        }
    }
}
