package com.augmentalis.httpavanue.websocket

enum class WebSocketOpcode(val value: Int) {
    CONTINUATION(0x0), TEXT(0x1), BINARY(0x2), CLOSE(0x8), PING(0x9), PONG(0xA);
    companion object { fun from(value: Int) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid opcode: $value") }
    fun isControl() = value >= 0x8
    fun isData() = value < 0x8
}

enum class WebSocketCloseCode(val code: Int) {
    NORMAL(1000), GOING_AWAY(1001), PROTOCOL_ERROR(1002), UNSUPPORTED_DATA(1003),
    INVALID_FRAME_PAYLOAD(1007), POLICY_VIOLATION(1008), MESSAGE_TOO_BIG(1009), INTERNAL_ERROR(1011);
    companion object { fun from(code: Int) = entries.firstOrNull { it.code == code } }
}

data class WebSocketFrame(
    val opcode: WebSocketOpcode,
    val payload: ByteArray,
    val isFinal: Boolean = true,
    val isMasked: Boolean = false,
    val maskingKey: ByteArray? = null,
) {
    fun payloadAsText(): String = payload.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSocketFrame) return false
        return opcode == other.opcode && payload.contentEquals(other.payload) && isFinal == other.isFinal &&
            isMasked == other.isMasked && (maskingKey?.contentEquals(other.maskingKey) ?: (other.maskingKey == null))
    }
    override fun hashCode(): Int {
        var result = opcode.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + isFinal.hashCode()
        result = 31 * result + isMasked.hashCode()
        result = 31 * result + (maskingKey?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        fun text(message: String, isFinal: Boolean = true) = WebSocketFrame(WebSocketOpcode.TEXT, message.encodeToByteArray(), isFinal)
        fun binary(data: ByteArray, isFinal: Boolean = true) = WebSocketFrame(WebSocketOpcode.BINARY, data, isFinal)
        fun ping(payload: ByteArray = byteArrayOf()) = WebSocketFrame(WebSocketOpcode.PING, payload, true)
        fun pong(payload: ByteArray = byteArrayOf()) = WebSocketFrame(WebSocketOpcode.PONG, payload, true)
        fun close(code: WebSocketCloseCode = WebSocketCloseCode.NORMAL, reason: String = "") =
            WebSocketFrame(WebSocketOpcode.CLOSE, buildClosePayload(code, reason), true)
        private fun buildClosePayload(code: WebSocketCloseCode, reason: String): ByteArray {
            val reasonBytes = reason.encodeToByteArray()
            return ByteArray(2 + reasonBytes.size).apply {
                this[0] = (code.code shr 8).toByte(); this[1] = code.code.toByte()
                reasonBytes.copyInto(this, 2)
            }
        }
    }
}
