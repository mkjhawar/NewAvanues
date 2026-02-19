package com.augmentalis.httpavanue.http2

/**
 * HTTP/2 error codes (RFC 7540 Section 7)
 */
enum class Http2ErrorCode(val code: Long) {
    NO_ERROR(0x0),
    PROTOCOL_ERROR(0x1),
    INTERNAL_ERROR(0x2),
    FLOW_CONTROL_ERROR(0x3),
    SETTINGS_TIMEOUT(0x4),
    STREAM_CLOSED(0x5),
    FRAME_SIZE_ERROR(0x6),
    REFUSED_STREAM(0x7),
    CANCEL(0x8),
    COMPRESSION_ERROR(0x9),
    CONNECT_ERROR(0xa),
    ENHANCE_YOUR_CALM(0xb),
    INADEQUATE_SECURITY(0xc),
    HTTP_1_1_REQUIRED(0xd);

    companion object {
        fun from(code: Long) = entries.firstOrNull { it.code == code } ?: INTERNAL_ERROR
    }
}

class Http2Exception(
    val errorCode: Http2ErrorCode,
    message: String,
    val streamId: Int = 0,
) : Exception("HTTP/2 error ${errorCode.name} (stream $streamId): $message")
