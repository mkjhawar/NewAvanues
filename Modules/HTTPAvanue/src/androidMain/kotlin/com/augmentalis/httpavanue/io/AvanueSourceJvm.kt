package com.augmentalis.httpavanue.io

import java.io.InputStream

/**
 * JVM implementation of [AvanueSource] — wraps a [java.io.InputStream]
 * with an internal prefetch [AvanueBuffer].
 *
 * Used on both Android and Desktop targets. The buffer is filled
 * from the input stream on demand, in 8 KB chunks.
 */
class AvanueSourceJvm(private val input: InputStream) : AvanueSource {

    override val buffer = AvanueBuffer()
    private var closed = false

    override fun readByte(): Byte {
        require(1)
        return buffer.readByte()
    }

    override fun readShort(): Short {
        require(2)
        return buffer.readShort()
    }

    override fun readInt(): Int {
        require(4)
        return buffer.readInt()
    }

    override fun readLong(): Long {
        require(8)
        return buffer.readLong()
    }

    override fun readByteArray(byteCount: Long): ByteArray {
        require(byteCount)
        return buffer.readByteArray(byteCount)
    }

    override fun readUtf8(byteCount: Long): String {
        require(byteCount)
        return buffer.readUtf8(byteCount)
    }

    override fun request(byteCount: Long): Boolean {
        while (buffer.size < byteCount) {
            if (!fillBuffer()) return false
        }
        return true
    }

    override fun require(byteCount: Long) {
        if (!request(byteCount)) {
            throw AvanueEofException("Required $byteCount bytes but only ${buffer.size} available")
        }
    }

    override fun skip(byteCount: Long) {
        require(byteCount)
        buffer.skip(byteCount)
    }

    override fun peek(): AvanueSource {
        return PeekSource(this)
    }

    override fun close() {
        if (!closed) {
            closed = true
            input.close()
        }
    }

    /** Fill the internal buffer from the input stream. Returns false on EOF. */
    private fun fillBuffer(): Boolean {
        if (closed) return false
        val chunk = ByteArray(READ_CHUNK_SIZE)
        val bytesRead = input.read(chunk)
        if (bytesRead <= 0) return false
        buffer.write(chunk, 0, bytesRead)
        return true
    }

    companion object {
        private const val READ_CHUNK_SIZE = 8192
    }
}

/**
 * A peek source that reads from the parent's buffer without consuming bytes.
 * Maintains its own read cursor into a snapshot of the parent buffer.
 */
private class PeekSource(private val parent: AvanueSource) : AvanueSource {
    override val buffer = AvanueBuffer()
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            buffer.write(parent.buffer.snapshot())
        }
    }

    override fun readByte(): Byte { ensureInitialized(); return buffer.readByte() }
    override fun readShort(): Short { ensureInitialized(); return buffer.readShort() }
    override fun readInt(): Int { ensureInitialized(); return buffer.readInt() }
    override fun readLong(): Long { ensureInitialized(); return buffer.readLong() }
    override fun readByteArray(byteCount: Long): ByteArray { ensureInitialized(); return buffer.readByteArray(byteCount) }
    override fun readUtf8(byteCount: Long): String { ensureInitialized(); return buffer.readUtf8(byteCount) }
    override fun request(byteCount: Long): Boolean { ensureInitialized(); return buffer.size >= byteCount }
    override fun require(byteCount: Long) { ensureInitialized(); if (buffer.size < byteCount) throw AvanueEofException("Peek buffer exhausted") }
    override fun skip(byteCount: Long) { ensureInitialized(); buffer.skip(byteCount) }
    override fun peek(): AvanueSource = PeekSource(this)
    override fun close() { /* peek sources don't own the underlying stream */ }
}
