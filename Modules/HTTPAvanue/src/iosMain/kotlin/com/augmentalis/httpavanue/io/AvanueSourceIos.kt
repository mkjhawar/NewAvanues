@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.httpavanue.io

import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS implementation of [AvanueSource] — reads from a POSIX socket
 * file descriptor using `recv()`.
 *
 * Replaces the previous Okio-based `SocketSource` inner class that
 * was defined inside `Socket.ios.kt`. The buffer is filled from the
 * socket in 8 KB chunks via POSIX `recv()`.
 */
class AvanueSourceIos(private val fd: Int) : AvanueSource {

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
        return PeekSourceIos(this)
    }

    override fun close() {
        closed = true
    }

    /** Fill the internal buffer from the socket fd. Returns false on EOF. */
    private fun fillBuffer(): Boolean {
        if (closed) return false
        val chunk = ByteArray(READ_CHUNK_SIZE)
        val bytesRead = chunk.usePinned { pinned ->
            recv(fd, pinned.addressOf(0), READ_CHUNK_SIZE.toULong(), 0)
        }
        return when {
            bytesRead > 0 -> { buffer.write(chunk, 0, bytesRead.toInt()); true }
            bytesRead == 0L -> false // EOF
            else -> throw AvanueEofException("Read failed: ${strerror(errno)?.toKString()}")
        }
    }

    companion object {
        private const val READ_CHUNK_SIZE = 8192
    }
}

/** Peek source for iOS — same logic as JVM PeekSource but in iosMain. */
private class PeekSourceIos(private val parent: AvanueSource) : AvanueSource {
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
    override fun peek(): AvanueSource = PeekSourceIos(this)
    override fun close() {}
}
