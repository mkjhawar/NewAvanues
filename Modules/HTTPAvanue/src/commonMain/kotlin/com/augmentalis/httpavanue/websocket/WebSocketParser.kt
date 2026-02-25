package com.augmentalis.httpavanue.websocket

import com.augmentalis.httpavanue.io.AvanueSource

/**
 * WebSocket frame parser (RFC 6455)
 */
internal object WebSocketParser {
    suspend fun parseFrame(source: AvanueSource, maxMessageSize: Long = 10 * 1024 * 1024): WebSocketFrame {
        val byte1 = source.readByte().toInt() and 0xFF
        val isFinal = (byte1 and 0x80) != 0
        val opcode = WebSocketOpcode.from(byte1 and 0x0F)

        val byte2 = source.readByte().toInt() and 0xFF
        val isMasked = (byte2 and 0x80) != 0
        var payloadLength = (byte2 and 0x7F).toLong()

        when (payloadLength) {
            126L -> payloadLength = source.readShort().toLong() and 0xFFFF
            127L -> { payloadLength = source.readLong(); if (payloadLength < 0) throw WebSocketException("Payload length too large") }
        }

        if (opcode.isData() && payloadLength > maxMessageSize) {
            throw WebSocketMessageTooLargeException("Frame payload size ($payloadLength bytes) exceeds maximum ($maxMessageSize bytes)")
        }

        val maskingKey = if (isMasked) source.readByteArray(4) else null
        val payload = if (payloadLength > 0) {
            val data = source.readByteArray(payloadLength)
            if (isMasked && maskingKey != null) unmask(data, maskingKey) else data
        } else byteArrayOf()

        return WebSocketFrame(opcode, payload, isFinal, isMasked, maskingKey)
    }

    fun frameToBytes(frame: WebSocketFrame): ByteArray {
        val payloadLength = frame.payload.size
        val headerSize = 2 + when { payloadLength < 126 -> 0; payloadLength < 65536 -> 2; else -> 8 } + if (frame.isMasked) 4 else 0
        val bytes = ByteArray(headerSize + payloadLength)
        var offset = 0
        bytes[offset++] = ((if (frame.isFinal) 0x80 else 0x00) or frame.opcode.value).toByte()
        val maskBit = if (frame.isMasked) 0x80 else 0x00
        when {
            payloadLength < 126 -> bytes[offset++] = (maskBit or payloadLength).toByte()
            payloadLength < 65536 -> {
                bytes[offset++] = (maskBit or 126).toByte()
                bytes[offset++] = (payloadLength shr 8).toByte()
                bytes[offset++] = payloadLength.toByte()
            }
            else -> {
                bytes[offset++] = (maskBit or 127).toByte()
                val length = payloadLength.toLong()
                for (i in 7 downTo 0) bytes[offset++] = (length shr (i * 8)).toByte()
            }
        }
        frame.maskingKey?.let { it.copyInto(bytes, offset); offset += 4 }
        if (frame.isMasked && frame.maskingKey != null) unmask(frame.payload, frame.maskingKey).copyInto(bytes, offset)
        else frame.payload.copyInto(bytes, offset)
        return bytes
    }

    private fun unmask(data: ByteArray, maskingKey: ByteArray): ByteArray {
        val unmasked = ByteArray(data.size)
        for (i in data.indices) unmasked[i] = (data[i].toInt() xor maskingKey[i % 4].toInt()).toByte()
        return unmasked
    }
}

open class WebSocketException(message: String, cause: Throwable? = null) : Exception(message, cause)
class WebSocketMessageTooLargeException(message: String) : WebSocketException(message)
