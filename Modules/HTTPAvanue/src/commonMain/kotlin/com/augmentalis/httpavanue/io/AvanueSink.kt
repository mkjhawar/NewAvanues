package com.augmentalis.httpavanue.io

/**
 * Buffered byte sink — replaces okio.BufferedSink.
 *
 * Writes bytes to an underlying transport (socket, byte array, etc.)
 * with an internal batch buffer to reduce syscalls.
 */
interface AvanueSink {

    /** Write a single byte (only the low 8 bits of [value] are used). */
    fun writeByte(value: Int): AvanueSink

    /** Write a 2-byte big-endian short. */
    fun writeShort(value: Int): AvanueSink

    /** Write a 4-byte big-endian int. */
    fun writeInt(value: Int): AvanueSink

    /** Write all bytes from [bytes]. */
    fun write(bytes: ByteArray): AvanueSink

    /** Write [count] bytes from [bytes] starting at [offset]. */
    fun write(bytes: ByteArray, offset: Int, count: Int): AvanueSink

    /** Encode [string] as UTF-8 and write the bytes. */
    fun writeUtf8(string: String): AvanueSink

    /** Flush buffered bytes to the underlying transport. */
    fun flush()

    /** Flush and release underlying resources. */
    fun close()
}
