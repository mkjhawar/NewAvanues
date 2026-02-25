package com.augmentalis.httpavanue.io

/**
 * Buffered byte source — replaces okio.BufferedSource.
 *
 * Reads bytes from an underlying transport (socket, byte array, etc.)
 * with an internal prefetch buffer for peek/request semantics.
 */
interface AvanueSource {

    /** Internal prefetch buffer — callers can inspect buffered data without consuming. */
    val buffer: AvanueBuffer

    /** Read a single byte. Throws [AvanueEofException] if no data available. */
    fun readByte(): Byte

    /** Read a 2-byte big-endian short. */
    fun readShort(): Short

    /** Read a 4-byte big-endian int. */
    fun readInt(): Int

    /** Read an 8-byte big-endian long. */
    fun readLong(): Long

    /** Read exactly [byteCount] bytes into a new array. Throws on EOF. */
    fun readByteArray(byteCount: Long): ByteArray

    /** Read up to [byteCount] bytes as a UTF-8 string. */
    fun readUtf8(byteCount: Long): String

    /**
     * Non-blocking fill: ensures at least [byteCount] bytes are in the buffer.
     * Returns true if the buffer contains enough data, false if EOF was reached
     * before the requested amount.
     */
    fun request(byteCount: Long): Boolean

    /**
     * Blocking fill: ensures at least [byteCount] bytes are in the buffer.
     * Throws [AvanueEofException] if EOF is reached before the requested amount.
     */
    fun require(byteCount: Long)

    /** Skip [byteCount] bytes. Throws on EOF. */
    fun skip(byteCount: Long)

    /**
     * Returns a source that reads from this source's buffer without consuming.
     * Useful for protocol detection (e.g., HTTP/2 preface peek).
     */
    fun peek(): AvanueSource

    /** Release underlying resources. */
    fun close()
}

/** Thrown when a read operation encounters end-of-stream prematurely. */
class AvanueEofException(message: String = "End of stream") : Exception(message)
