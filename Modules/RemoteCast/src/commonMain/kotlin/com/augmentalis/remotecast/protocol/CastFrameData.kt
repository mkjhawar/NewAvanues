/**
 * CastFrameData.kt — Wire protocol for MJPEG-over-TCP streaming
 *
 * Defines the binary frame format used to transmit JPEG frames across the
 * TCP transport layer. Each frame is preceded by a 20-byte fixed-length header.
 *
 * Header layout (20 bytes total):
 *   Bytes  0–3:  Magic "CAST" (0x43, 0x41, 0x53, 0x54)
 *   Bytes  4–7:  Sequence number (Int, big-endian)
 *   Bytes 8–15:  Timestamp in milliseconds (Long, big-endian)
 *   Bytes 16–19: Payload size in bytes (Int, big-endian)
 *
 * The payload immediately follows the header and contains raw JPEG bytes.
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.remotecast.protocol

/**
 * Represents a single captured screen frame ready for transport.
 *
 * @param frameBytes  Raw JPEG-compressed image data.
 * @param timestamp   Wall-clock timestamp (System.currentTimeMillis) when the frame was captured.
 * @param sequenceNumber  Monotonically increasing counter; wraps at Int.MAX_VALUE.
 * @param width       Frame width in pixels.
 * @param height      Frame height in pixels.
 */
data class CastFrameData(
    val frameBytes: ByteArray,
    val timestamp: Long,
    val sequenceNumber: Int,
    val width: Int,
    val height: Int
) {
    companion object {
        /** Magic bytes 'C','A','S','T' that start every frame header. */
        val FRAME_HEADER_MAGIC: ByteArray = byteArrayOf(0x43, 0x41, 0x53, 0x54)

        /**
         * Total size of the binary header in bytes.
         * Layout: 4 (magic) + 4 (seq) + 8 (timestamp) + 4 (payload size) = 20
         */
        const val HEADER_SIZE: Int = 20

        /**
         * Encodes a 20-byte header into [dest] starting at [offset].
         *
         * @param dest            Target byte array; must have at least [offset] + [HEADER_SIZE] bytes available.
         * @param offset          Starting position in [dest].
         * @param sequenceNumber  Frame sequence number.
         * @param timestamp       Capture timestamp in milliseconds.
         * @param payloadSize     Number of JPEG bytes that follow the header.
         */
        fun encodeHeader(
            dest: ByteArray,
            offset: Int = 0,
            sequenceNumber: Int,
            timestamp: Long,
            payloadSize: Int
        ) {
            require(dest.size >= offset + HEADER_SIZE) {
                "Destination buffer too small: need ${offset + HEADER_SIZE}, have ${dest.size}"
            }
            // Magic (4 bytes)
            dest[offset + 0] = FRAME_HEADER_MAGIC[0]
            dest[offset + 1] = FRAME_HEADER_MAGIC[1]
            dest[offset + 2] = FRAME_HEADER_MAGIC[2]
            dest[offset + 3] = FRAME_HEADER_MAGIC[3]
            // Sequence number (4 bytes, big-endian)
            dest[offset + 4] = ((sequenceNumber ushr 24) and 0xFF).toByte()
            dest[offset + 5] = ((sequenceNumber ushr 16) and 0xFF).toByte()
            dest[offset + 6] = ((sequenceNumber ushr 8) and 0xFF).toByte()
            dest[offset + 7] = (sequenceNumber and 0xFF).toByte()
            // Timestamp (8 bytes, big-endian)
            dest[offset + 8]  = ((timestamp ushr 56) and 0xFF).toByte()
            dest[offset + 9]  = ((timestamp ushr 48) and 0xFF).toByte()
            dest[offset + 10] = ((timestamp ushr 40) and 0xFF).toByte()
            dest[offset + 11] = ((timestamp ushr 32) and 0xFF).toByte()
            dest[offset + 12] = ((timestamp ushr 24) and 0xFF).toByte()
            dest[offset + 13] = ((timestamp ushr 16) and 0xFF).toByte()
            dest[offset + 14] = ((timestamp ushr 8) and 0xFF).toByte()
            dest[offset + 15] = (timestamp and 0xFF).toByte()
            // Payload size (4 bytes, big-endian)
            dest[offset + 16] = ((payloadSize ushr 24) and 0xFF).toByte()
            dest[offset + 17] = ((payloadSize ushr 16) and 0xFF).toByte()
            dest[offset + 18] = ((payloadSize ushr 8) and 0xFF).toByte()
            dest[offset + 19] = (payloadSize and 0xFF).toByte()
        }

        /**
         * Decodes a header from [src] starting at [offset].
         *
         * @return [DecodedHeader] if the magic bytes match, or null if this is not a valid CAST frame.
         */
        fun decodeHeader(src: ByteArray, offset: Int = 0): DecodedHeader? {
            if (src.size < offset + HEADER_SIZE) return null
            // Validate magic
            if (src[offset + 0] != FRAME_HEADER_MAGIC[0] ||
                src[offset + 1] != FRAME_HEADER_MAGIC[1] ||
                src[offset + 2] != FRAME_HEADER_MAGIC[2] ||
                src[offset + 3] != FRAME_HEADER_MAGIC[3]
            ) return null

            val sequenceNumber = ((src[offset + 4].toInt() and 0xFF) shl 24) or
                    ((src[offset + 5].toInt() and 0xFF) shl 16) or
                    ((src[offset + 6].toInt() and 0xFF) shl 8) or
                    (src[offset + 7].toInt() and 0xFF)

            val timestamp = ((src[offset + 8].toLong() and 0xFF) shl 56) or
                    ((src[offset + 9].toLong() and 0xFF) shl 48) or
                    ((src[offset + 10].toLong() and 0xFF) shl 40) or
                    ((src[offset + 11].toLong() and 0xFF) shl 32) or
                    ((src[offset + 12].toLong() and 0xFF) shl 24) or
                    ((src[offset + 13].toLong() and 0xFF) shl 16) or
                    ((src[offset + 14].toLong() and 0xFF) shl 8) or
                    (src[offset + 15].toLong() and 0xFF)

            val payloadSize = ((src[offset + 16].toInt() and 0xFF) shl 24) or
                    ((src[offset + 17].toInt() and 0xFF) shl 16) or
                    ((src[offset + 18].toInt() and 0xFF) shl 8) or
                    (src[offset + 19].toInt() and 0xFF)

            return DecodedHeader(
                sequenceNumber = sequenceNumber,
                timestamp = timestamp,
                payloadSize = payloadSize
            )
        }

        /**
         * Builds the complete on-wire packet (header + JPEG payload) for [frame].
         */
        fun buildPacket(frame: CastFrameData): ByteArray {
            val packet = ByteArray(HEADER_SIZE + frame.frameBytes.size)
            encodeHeader(
                dest = packet,
                offset = 0,
                sequenceNumber = frame.sequenceNumber,
                timestamp = frame.timestamp,
                payloadSize = frame.frameBytes.size
            )
            frame.frameBytes.copyInto(packet, destinationOffset = HEADER_SIZE)
            return packet
        }
    }

    /**
     * Decoded values from a 20-byte CAST frame header.
     */
    data class DecodedHeader(
        val sequenceNumber: Int,
        val timestamp: Long,
        val payloadSize: Int
    )

    // ByteArray equality requires manual override
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CastFrameData) return false
        return sequenceNumber == other.sequenceNumber &&
                timestamp == other.timestamp &&
                width == other.width &&
                height == other.height &&
                frameBytes.contentEquals(other.frameBytes)
    }

    override fun hashCode(): Int {
        var result = frameBytes.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
