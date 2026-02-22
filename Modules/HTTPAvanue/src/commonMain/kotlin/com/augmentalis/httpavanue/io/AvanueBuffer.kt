package com.augmentalis.httpavanue.io

/**
 * Growable byte buffer — replaces okio.Buffer.
 *
 * Serves both as a read buffer (for AvanueSource prefetch) and as a
 * write accumulator (for building request/response payloads). Uses a
 * circular-style cursor over a linear array that doubles on overflow.
 *
 * Read operations consume bytes (advance [readPos]).
 * Write operations append bytes (advance [writePos]).
 * [size] is always `writePos - readPos`.
 */
class AvanueBuffer {

    private var data: ByteArray = ByteArray(INITIAL_CAPACITY)
    private var readPos: Int = 0
    private var writePos: Int = 0

    /** Number of bytes available for reading. */
    val size: Long get() = (writePos - readPos).toLong()

    // ── Write operations ──────────────────────────────────────────

    fun writeByte(value: Int): AvanueBuffer {
        ensureCapacity(1)
        data[writePos++] = value.toByte()
        return this
    }

    fun writeShort(value: Int): AvanueBuffer {
        ensureCapacity(2)
        data[writePos++] = (value shr 8).toByte()
        data[writePos++] = value.toByte()
        return this
    }

    fun writeInt(value: Int): AvanueBuffer {
        ensureCapacity(4)
        data[writePos++] = (value shr 24).toByte()
        data[writePos++] = (value shr 16).toByte()
        data[writePos++] = (value shr 8).toByte()
        data[writePos++] = value.toByte()
        return this
    }

    fun write(bytes: ByteArray): AvanueBuffer {
        return write(bytes, 0, bytes.size)
    }

    fun write(bytes: ByteArray, offset: Int, count: Int): AvanueBuffer {
        ensureCapacity(count)
        bytes.copyInto(data, writePos, offset, offset + count)
        writePos += count
        return this
    }

    fun writeUtf8(string: String): AvanueBuffer {
        val bytes = string.encodeToByteArray()
        return write(bytes)
    }

    // ── Read operations ───────────────────────────────────────────

    fun readByte(): Byte {
        if (size <= 0) throw AvanueEofException("Buffer exhausted")
        return data[readPos++]
    }

    fun readShort(): Short {
        require(2)
        val v = ((data[readPos].toInt() and 0xFF) shl 8) or
                (data[readPos + 1].toInt() and 0xFF)
        readPos += 2
        return v.toShort()
    }

    fun readInt(): Int {
        require(4)
        val v = ((data[readPos].toInt() and 0xFF) shl 24) or
                ((data[readPos + 1].toInt() and 0xFF) shl 16) or
                ((data[readPos + 2].toInt() and 0xFF) shl 8) or
                (data[readPos + 3].toInt() and 0xFF)
        readPos += 4
        return v
    }

    fun readLong(): Long {
        require(8)
        var v = 0L
        for (i in 0 until 8) {
            v = (v shl 8) or (data[readPos + i].toLong() and 0xFF)
        }
        readPos += 8
        return v
    }

    fun readByteArray(byteCount: Long): ByteArray {
        val count = byteCount.toInt()
        require(count)
        val result = data.copyOfRange(readPos, readPos + count)
        readPos += count
        return result
    }

    fun readUtf8(byteCount: Long): String {
        return readByteArray(byteCount).decodeToString()
    }

    /** Read remaining buffer content as UTF-8. */
    fun readUtf8(): String {
        return readUtf8(size)
    }

    /**
     * Read one line of UTF-8 text (up to CR LF or LF).
     * Returns null at EOF. Line terminator is consumed but not included.
     */
    fun readUtf8Line(): String? {
        if (size <= 0L) return null
        val startPos = readPos
        while (readPos < writePos) {
            val b = data[readPos]
            if (b == '\n'.code.toByte()) {
                val lineEnd = readPos
                readPos++ // consume \n
                // Strip trailing \r if present
                val effectiveEnd = if (lineEnd > startPos && data[lineEnd - 1] == '\r'.code.toByte()) {
                    lineEnd - 1
                } else {
                    lineEnd
                }
                return data.copyOfRange(startPos, effectiveEnd).decodeToString()
            }
            readPos++
        }
        // No newline found — return remaining as line (EOF case)
        return data.copyOfRange(startPos, writePos).decodeToString()
    }

    // ── Buffer management ─────────────────────────────────────────

    /**
     * Non-destructive snapshot of current buffer contents.
     * Returns a copy — mutations to the buffer do not affect the snapshot.
     */
    fun snapshot(): ByteArray {
        return data.copyOfRange(readPos, writePos)
    }

    /**
     * Convert entire buffer contents to a ByteArray (same as snapshot but
     * named for compatibility with patterns like `sink.write(buffer.toByteArray())`).
     */
    fun toByteArray(): ByteArray = snapshot()

    /** Skip [byteCount] bytes. */
    fun skip(byteCount: Long) {
        val count = byteCount.toInt()
        require(count)
        readPos += count
    }

    /** Clear all data and reset positions. */
    fun clear() {
        readPos = 0
        writePos = 0
    }

    // ── Internal ──────────────────────────────────────────────────

    private fun require(count: Int) {
        if (size < count) throw AvanueEofException("Required $count bytes but only $size available")
    }

    private fun ensureCapacity(additional: Int) {
        val needed = writePos + additional
        if (needed <= data.size) return

        // Compact first: shift unread data to position 0
        if (readPos > 0) {
            val unread = writePos - readPos
            data.copyInto(data, 0, readPos, writePos)
            readPos = 0
            writePos = unread
            if (writePos + additional <= data.size) return
        }

        // Double until large enough
        var newSize = data.size
        while (newSize < writePos + additional) {
            newSize *= 2
        }
        val newData = ByteArray(newSize)
        data.copyInto(newData, 0, 0, writePos)
        data = newData
    }

    companion object {
        private const val INITIAL_CAPACITY = 256
    }
}
