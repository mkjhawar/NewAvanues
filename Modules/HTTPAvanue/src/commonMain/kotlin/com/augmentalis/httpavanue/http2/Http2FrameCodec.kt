package com.augmentalis.httpavanue.http2

import com.augmentalis.httpavanue.io.AvanueSource
import com.augmentalis.httpavanue.io.AvanueSink

/**
 * HTTP/2 frame types (RFC 7540 Section 6)
 */
enum class Http2FrameType(val value: Int) {
    DATA(0x0), HEADERS(0x1), PRIORITY(0x2), RST_STREAM(0x3),
    SETTINGS(0x4), PUSH_PROMISE(0x5), PING(0x6), GOAWAY(0x7),
    WINDOW_UPDATE(0x8), CONTINUATION(0x9);
    companion object { fun from(value: Int) = entries.firstOrNull { it.value == value } }
}

/** HTTP/2 frame flags */
object Http2Flags {
    const val END_STREAM: Int = 0x1
    const val END_HEADERS: Int = 0x4
    const val PADDED: Int = 0x8
    const val PRIORITY: Int = 0x20
    const val ACK: Int = 0x1 // For SETTINGS and PING
}

/**
 * Parsed HTTP/2 frame (9-byte header + payload)
 */
data class Http2Frame(
    val type: Http2FrameType?,
    val typeValue: Int,
    val flags: Int,
    val streamId: Int,
    val payload: ByteArray,
) {
    val length: Int get() = payload.size
    fun hasFlag(flag: Int) = (flags and flag) != 0

    override fun equals(other: Any?) = other is Http2Frame && typeValue == other.typeValue &&
        flags == other.flags && streamId == other.streamId && payload.contentEquals(other.payload)
    override fun hashCode(): Int {
        var result = typeValue
        result = 31 * result + flags; result = 31 * result + streamId; result = 31 * result + payload.contentHashCode()
        return result
    }
}

/**
 * HTTP/2 Frame Codec — reads/writes the 9-byte frame header + payload (RFC 7540 Section 4.1)
 *
 * Frame format:
 *   Length (24 bits) | Type (8) | Flags (8) | Reserved (1) | Stream ID (31) | Payload
 */
object Http2FrameCodec {
    const val FRAME_HEADER_SIZE = 9
    const val DEFAULT_MAX_FRAME_SIZE = 16384
    const val MAX_MAX_FRAME_SIZE = 16777215 // 2^24 - 1

    /** Connection preface magic bytes (RFC 7540 Section 3.5) */
    val CONNECTION_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".encodeToByteArray()

    /** Read one HTTP/2 frame from the source */
    fun readFrame(source: AvanueSource, maxFrameSize: Int = DEFAULT_MAX_FRAME_SIZE): Http2Frame {
        // Read 9-byte header
        val header = source.readByteArray(FRAME_HEADER_SIZE.toLong())
        val length = ((header[0].toInt() and 0xFF) shl 16) or
            ((header[1].toInt() and 0xFF) shl 8) or
            (header[2].toInt() and 0xFF)
        val typeValue = header[3].toInt() and 0xFF
        val flags = header[4].toInt() and 0xFF
        val streamId = ((header[5].toInt() and 0x7F) shl 24) or
            ((header[6].toInt() and 0xFF) shl 16) or
            ((header[7].toInt() and 0xFF) shl 8) or
            (header[8].toInt() and 0xFF)

        if (length > maxFrameSize) {
            throw Http2Exception(Http2ErrorCode.FRAME_SIZE_ERROR,
                "Frame size $length exceeds maximum $maxFrameSize", streamId)
        }

        val payload = if (length > 0) source.readByteArray(length.toLong()) else byteArrayOf()
        return Http2Frame(Http2FrameType.from(typeValue), typeValue, flags, streamId, payload)
    }

    /** Write one HTTP/2 frame to the sink */
    fun writeFrame(sink: AvanueSink, type: Http2FrameType, flags: Int, streamId: Int, payload: ByteArray = byteArrayOf()) {
        val length = payload.size
        require(length <= MAX_MAX_FRAME_SIZE) { "Payload too large: $length" }

        // Write 9-byte header
        sink.writeByte((length shr 16) and 0xFF)
        sink.writeByte((length shr 8) and 0xFF)
        sink.writeByte(length and 0xFF)
        sink.writeByte(type.value)
        sink.writeByte(flags)
        sink.writeInt(streamId and 0x7FFFFFFF)

        if (payload.isNotEmpty()) sink.write(payload)
        sink.flush()
    }

    /** Write SETTINGS frame */
    fun writeSettings(sink: AvanueSink, settings: Http2Settings, ack: Boolean = false) {
        val payload = if (ack) byteArrayOf() else Http2Settings.encode(settings)
        val flags = if (ack) Http2Flags.ACK else 0
        writeFrame(sink, Http2FrameType.SETTINGS, flags, 0, payload)
    }

    /** Write GOAWAY frame */
    fun writeGoaway(sink: AvanueSink, lastStreamId: Int, errorCode: Http2ErrorCode, debugData: ByteArray = byteArrayOf()) {
        val payload = ByteArray(8 + debugData.size)
        payload[0] = (lastStreamId shr 24).toByte()
        payload[1] = (lastStreamId shr 16).toByte()
        payload[2] = (lastStreamId shr 8).toByte()
        payload[3] = lastStreamId.toByte()
        val code = errorCode.code.toInt()
        payload[4] = (code shr 24).toByte()
        payload[5] = (code shr 16).toByte()
        payload[6] = (code shr 8).toByte()
        payload[7] = code.toByte()
        debugData.copyInto(payload, 8)
        writeFrame(sink, Http2FrameType.GOAWAY, 0, 0, payload)
    }

    /** Write PING frame */
    fun writePing(sink: AvanueSink, ack: Boolean = false, opaqueData: ByteArray = ByteArray(8)) {
        require(opaqueData.size == 8) { "PING payload must be 8 bytes" }
        writeFrame(sink, Http2FrameType.PING, if (ack) Http2Flags.ACK else 0, 0, opaqueData)
    }

    /** Write WINDOW_UPDATE frame */
    fun writeWindowUpdate(sink: AvanueSink, streamId: Int, windowSizeIncrement: Int) {
        require(windowSizeIncrement > 0) { "Window size increment must be positive" }
        val payload = ByteArray(4)
        payload[0] = (windowSizeIncrement shr 24).toByte()
        payload[1] = (windowSizeIncrement shr 16).toByte()
        payload[2] = (windowSizeIncrement shr 8).toByte()
        payload[3] = windowSizeIncrement.toByte()
        writeFrame(sink, Http2FrameType.WINDOW_UPDATE, 0, streamId, payload)
    }

    /** Write RST_STREAM frame */
    fun writeRstStream(sink: AvanueSink, streamId: Int, errorCode: Http2ErrorCode) {
        val code = errorCode.code.toInt()
        val payload = byteArrayOf((code shr 24).toByte(), (code shr 16).toByte(), (code shr 8).toByte(), code.toByte())
        writeFrame(sink, Http2FrameType.RST_STREAM, 0, streamId, payload)
    }

    /** Write DATA frame */
    fun writeData(sink: AvanueSink, streamId: Int, data: ByteArray, endStream: Boolean = false) {
        writeFrame(sink, Http2FrameType.DATA, if (endStream) Http2Flags.END_STREAM else 0, streamId, data)
    }

    /** Write HEADERS frame */
    fun writeHeaders(sink: AvanueSink, streamId: Int, headerBlock: ByteArray, endStream: Boolean = false, endHeaders: Boolean = true) {
        var flags = 0
        if (endStream) flags = flags or Http2Flags.END_STREAM
        if (endHeaders) flags = flags or Http2Flags.END_HEADERS
        writeFrame(sink, Http2FrameType.HEADERS, flags, streamId, headerBlock)
    }
}
