package com.augmentalis.httpavanue.websocket

import com.augmentalis.httpavanue.io.AvanueBuffer
import com.augmentalis.httpavanue.io.AvanueEofException
import com.augmentalis.httpavanue.io.AvanueSource
import com.augmentalis.httpavanue.platform.Socket
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Binary protocol for high-performance frame-based communication.
 *
 * Wire format:
 * ```
 * [4-byte magic: 0x4156_4E45 "AVNE"]
 * [2-byte type]
 * [4-byte payload length]
 * [payload bytes]
 * ```
 *
 * Designed for use after WebSocket negotiation — the handler receives
 * a raw Socket and switches to binary framing for maximum throughput.
 */
object BinaryProtocol {
    /** Magic bytes: "AVNE" (0x41 0x56 0x4E 0x45) */
    const val MAGIC: Int = 0x4156_4E45.toInt()

    /** Frame header size: 4 (magic) + 2 (type) + 4 (length) = 10 bytes */
    const val HEADER_SIZE = 10

    /** Maximum payload size: 16 MB */
    const val MAX_PAYLOAD_SIZE = 16 * 1024 * 1024

    // Pre-defined type constants for the Avanues ecosystem
    const val TYPE_CAST: UShort = 0x01u    // Screen cast frame (JPEG)
    const val TYPE_VOCAB: UShort = 0x02u   // Vocabulary sync
    const val TYPE_CMD: UShort = 0x03u     // Command dispatch
    const val TYPE_ACK: UShort = 0x04u     // Acknowledgement
    const val TYPE_IMU: UShort = 0x05u     // IMU sensor data
    const val TYPE_TTS: UShort = 0x06u     // Text-to-speech

    /** Encode a binary frame to bytes. */
    fun encode(type: UShort, payload: ByteArray): ByteArray {
        val buffer = AvanueBuffer()
        buffer.writeInt(MAGIC)
        buffer.writeShort(type.toInt())
        buffer.writeInt(payload.size)
        buffer.write(payload)
        return buffer.toByteArray()
    }

    /** Decode a binary frame from a source. Returns null on EOF. */
    fun decode(source: AvanueSource): BinaryFrame? {
        // Read and validate magic
        if (!source.request(HEADER_SIZE.toLong())) return null
        val magic = source.readInt()
        if (magic != MAGIC) throw BinaryProtocolException("Invalid magic: 0x${magic.toUInt().toString(16)}, expected 0x${MAGIC.toUInt().toString(16)}")

        val type = source.readShort().toUShort()
        val length = source.readInt()

        if (length < 0 || length > MAX_PAYLOAD_SIZE) {
            throw BinaryProtocolException("Invalid payload length: $length (max $MAX_PAYLOAD_SIZE)")
        }

        val payload = if (length > 0) source.readByteArray(length.toLong()) else byteArrayOf()
        return BinaryFrame(type, payload)
    }
}

/** A decoded binary protocol frame. */
data class BinaryFrame(
    val type: UShort,
    val payload: ByteArray,
) {
    override fun equals(other: Any?) = other is BinaryFrame && type == other.type && payload.contentEquals(other.payload)
    override fun hashCode() = 31 * type.hashCode() + payload.contentHashCode()
}

/** Exception for binary protocol errors. */
class BinaryProtocolException(message: String) : Exception(message)

/**
 * Binary protocol session — runs a frame dispatch loop on a socket.
 *
 * Reads binary frames continuously and dispatches them to the [onFrame]
 * callback. Runs until the socket is closed or an error occurs.
 */
class BinaryProtocolSession(
    private val socket: Socket,
    private val onFrame: suspend (BinaryFrame) -> Unit,
) {
    /** Run the binary protocol dispatch loop. */
    suspend fun run() {
        val source = socket.source()
        try {
            while (coroutineContext.isActive) {
                val frame = BinaryProtocol.decode(source) ?: break
                onFrame(frame)
            }
        } catch (_: AvanueEofException) {
            // Connection closed — normal termination
        }
    }

    /** Send a binary frame to the peer. */
    suspend fun send(type: UShort, payload: ByteArray) {
        val bytes = BinaryProtocol.encode(type, payload)
        socket.sink().apply { write(bytes); flush() }
    }
}
