package com.augmentalis.httpavanue.http2

/**
 * HTTP/2 SETTINGS parameters (RFC 7540 Section 6.5)
 */
data class Http2Settings(
    val headerTableSize: Int = 4096,
    val enablePush: Boolean = true,
    val maxConcurrentStreams: Int = 100,
    val initialWindowSize: Int = 65535,
    val maxFrameSize: Int = 16384,
    val maxHeaderListSize: Int = Int.MAX_VALUE,
) {
    companion object {
        const val HEADER_TABLE_SIZE: Int = 0x1
        const val ENABLE_PUSH: Int = 0x2
        const val MAX_CONCURRENT_STREAMS: Int = 0x3
        const val INITIAL_WINDOW_SIZE: Int = 0x4
        const val MAX_FRAME_SIZE: Int = 0x5
        const val MAX_HEADER_LIST_SIZE: Int = 0x6

        /** Encode settings to wire format: sequence of (id: UShort, value: UInt) pairs */
        fun encode(settings: Http2Settings): ByteArray {
            val pairs = mutableListOf<Pair<Int, Int>>()
            pairs.add(HEADER_TABLE_SIZE to settings.headerTableSize)
            pairs.add(ENABLE_PUSH to if (settings.enablePush) 1 else 0)
            pairs.add(MAX_CONCURRENT_STREAMS to settings.maxConcurrentStreams)
            pairs.add(INITIAL_WINDOW_SIZE to settings.initialWindowSize)
            pairs.add(MAX_FRAME_SIZE to settings.maxFrameSize)
            if (settings.maxHeaderListSize != Int.MAX_VALUE) {
                pairs.add(MAX_HEADER_LIST_SIZE to settings.maxHeaderListSize)
            }
            val result = ByteArray(pairs.size * 6)
            var offset = 0
            for ((id, value) in pairs) {
                result[offset++] = (id shr 8).toByte()
                result[offset++] = id.toByte()
                result[offset++] = (value shr 24).toByte()
                result[offset++] = (value shr 16).toByte()
                result[offset++] = (value shr 8).toByte()
                result[offset++] = value.toByte()
            }
            return result
        }

        /** Decode settings from wire format */
        fun decode(data: ByteArray): Http2Settings {
            var settings = Http2Settings()
            var offset = 0
            while (offset + 6 <= data.size) {
                val id = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
                val value = ((data[offset + 2].toInt() and 0xFF) shl 24) or
                    ((data[offset + 3].toInt() and 0xFF) shl 16) or
                    ((data[offset + 4].toInt() and 0xFF) shl 8) or
                    (data[offset + 5].toInt() and 0xFF)
                offset += 6
                settings = when (id) {
                    HEADER_TABLE_SIZE -> settings.copy(headerTableSize = value)
                    ENABLE_PUSH -> settings.copy(enablePush = value != 0)
                    MAX_CONCURRENT_STREAMS -> settings.copy(maxConcurrentStreams = value)
                    INITIAL_WINDOW_SIZE -> settings.copy(initialWindowSize = value)
                    MAX_FRAME_SIZE -> settings.copy(maxFrameSize = value)
                    MAX_HEADER_LIST_SIZE -> settings.copy(maxHeaderListSize = value)
                    else -> settings // Unknown settings are ignored per spec
                }
            }
            return settings
        }
    }
}
