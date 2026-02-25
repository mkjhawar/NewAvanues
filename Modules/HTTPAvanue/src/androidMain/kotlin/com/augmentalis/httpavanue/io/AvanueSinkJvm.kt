package com.augmentalis.httpavanue.io

import java.io.OutputStream

/**
 * JVM implementation of [AvanueSink] — wraps a [java.io.OutputStream]
 * with an internal batch buffer to minimize syscalls.
 *
 * Used on both Android and Desktop targets. Flushes when the buffer
 * exceeds [WRITE_BUFFER_SIZE] or on explicit [flush] / [close].
 */
class AvanueSinkJvm(private val output: OutputStream) : AvanueSink {

    private val buffer = ByteArray(WRITE_BUFFER_SIZE)
    private var pos = 0
    private var closed = false

    override fun writeByte(value: Int): AvanueSink {
        if (pos >= WRITE_BUFFER_SIZE) drainBuffer()
        buffer[pos++] = value.toByte()
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
            bytes.copyInto(buffer, pos, srcOffset, srcOffset + toCopy)
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
        output.flush()
    }

    override fun close() {
        if (!closed) {
            closed = true
            flush()
            output.close()
        }
    }

    private fun drainBuffer() {
        if (pos > 0) {
            output.write(buffer, 0, pos)
            pos = 0
        }
    }

    companion object {
        private const val WRITE_BUFFER_SIZE = 8192
    }
}
