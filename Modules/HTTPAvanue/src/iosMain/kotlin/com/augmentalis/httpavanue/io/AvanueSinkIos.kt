@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.httpavanue.io

import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS implementation of [AvanueSink] — writes to a POSIX socket
 * file descriptor using `send()`.
 *
 * Replaces the previous Okio-based `SocketSink` inner class that
 * was defined inside `Socket.ios.kt`. Buffers writes internally
 * and flushes via POSIX `send()`.
 */
class AvanueSinkIos(private val fd: Int) : AvanueSink {

    private val writeBuffer = ByteArray(WRITE_BUFFER_SIZE)
    private var pos = 0
    private var closed = false

    override fun writeByte(value: Int): AvanueSink {
        if (pos >= WRITE_BUFFER_SIZE) drainBuffer()
        writeBuffer[pos++] = value.toByte()
        return this
    }

    override fun writeShort(value: Int): AvanueSink {
        writeByte(value shr 8)
        writeByte(value)
        return this
    }

    override fun writeInt(value: Int): AvanueSink {
        writeByte(value shr 24)
        writeByte(value shr 16)
        writeByte(value shr 8)
        writeByte(value)
        return this
    }

    override fun write(bytes: ByteArray): AvanueSink {
        return write(bytes, 0, bytes.size)
    }

    override fun write(bytes: ByteArray, offset: Int, count: Int): AvanueSink {
        var remaining = count
        var srcOffset = offset
        while (remaining > 0) {
            val space = WRITE_BUFFER_SIZE - pos
            if (space <= 0) {
                drainBuffer()
                continue
            }
            val toCopy = minOf(remaining, space)
            bytes.copyInto(writeBuffer, pos, srcOffset, srcOffset + toCopy)
            pos += toCopy
            srcOffset += toCopy
            remaining -= toCopy
        }
        return this
    }

    override fun writeUtf8(string: String): AvanueSink {
        return write(string.encodeToByteArray())
    }

    override fun flush() {
        drainBuffer()
    }

    override fun close() {
        if (!closed) {
            closed = true
            drainBuffer()
        }
    }

    private fun drainBuffer() {
        if (pos <= 0) return
        var offset = 0
        while (offset < pos) {
            val sent = writeBuffer.usePinned { pinned ->
                send(fd, pinned.addressOf(offset), (pos - offset).toULong(), 0)
            }
            if (sent < 0) throw AvanueEofException("Write failed: ${strerror(errno)?.toKString()}")
            offset += sent.toInt()
        }
        pos = 0
    }

    companion object {
        private const val WRITE_BUFFER_SIZE = 8192
    }
}
