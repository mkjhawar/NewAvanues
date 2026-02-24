package com.augmentalis.netavanue.ice.stun

import kotlin.random.Random

/**
 * Complete STUN message (RFC 5389, Section 6).
 *
 * Wire format:
 * ```
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |0 0|     STUN Message Type     |         Message Length        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Magic Cookie                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * |                     Transaction ID (96 bits)                  |
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * ```
 *
 * The header is 20 bytes total. Message Length counts only the bytes
 * after the header (i.e. total attribute bytes including padding).
 *
 * The top 2 bits of the first byte MUST be zero per RFC 5389, which
 * distinguishes STUN from other multiplexed protocols (DTLS, RTP).
 *
 * @param type The decoded message type (method + class)
 * @param transactionId 12-byte transaction identifier
 * @param attributes Ordered list of STUN attributes in the message body
 */
data class StunMessage(
    val type: StunMessageType,
    val transactionId: ByteArray,
    val attributes: List<StunAttribute>,
) {

    init {
        require(transactionId.size == TRANSACTION_ID_LENGTH) {
            "Transaction ID must be $TRANSACTION_ID_LENGTH bytes, got ${transactionId.size}"
        }
    }

    /**
     * Encode this message into a complete STUN packet (header + attributes).
     *
     * @return The wire-format byte array ready for transmission
     */
    fun encode(): ByteArray {
        // Encode all attributes first to determine total body length
        val encodedAttrs = attributes.map { StunAttributeCodec.encode(it, transactionId) }
        val bodyLength = encodedAttrs.sumOf { it.size }

        val result = ByteArray(HEADER_SIZE + bodyLength)

        // Type (2 bytes, big-endian) -- top 2 bits are zero
        val typeValue = type.encode()
        result[0] = (typeValue shr 8).toByte()
        result[1] = (typeValue and 0xFF).toByte()

        // Message Length (2 bytes, big-endian) -- excludes the 20-byte header
        result[2] = (bodyLength shr 8).toByte()
        result[3] = (bodyLength and 0xFF).toByte()

        // Magic Cookie (4 bytes, big-endian)
        result[4] = (MAGIC_COOKIE ushr 24).toByte()
        result[5] = (MAGIC_COOKIE ushr 16).toByte()
        result[6] = (MAGIC_COOKIE ushr 8).toByte()
        result[7] = (MAGIC_COOKIE and 0xFF).toByte()

        // Transaction ID (12 bytes)
        transactionId.copyInto(result, destinationOffset = 8)

        // Attributes
        var offset = HEADER_SIZE
        for (encodedAttr in encodedAttrs) {
            encodedAttr.copyInto(result, destinationOffset = offset)
            offset += encodedAttr.size
        }

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StunMessage) return false
        return type == other.type &&
            transactionId.contentEquals(other.transactionId) &&
            attributes == other.attributes
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + transactionId.contentHashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }

    override fun toString(): String {
        val txIdHex = transactionId.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return "StunMessage(type=$type, txId=$txIdHex, attributes=${attributes.size})"
    }

    companion object {
        /** STUN magic cookie value (RFC 5389, Section 6). */
        val MAGIC_COOKIE: Int = 0x2112A442.toInt()

        /** STUN header size in bytes. */
        const val HEADER_SIZE = 20

        /** Transaction ID length in bytes. */
        const val TRANSACTION_ID_LENGTH = 12

        /**
         * Generate a cryptographically random 12-byte transaction ID.
         *
         * Per RFC 5389 Section 6, the transaction ID should be uniformly
         * and randomly chosen. We use kotlin.random.Random which delegates
         * to the platform's secure random source on most KMP targets.
         */
        fun generateTransactionId(): ByteArray {
            return Random.nextBytes(TRANSACTION_ID_LENGTH)
        }

        /**
         * Create a STUN Binding Request with no attributes.
         *
         * This is the simplest and most common STUN message, used for
         * server-reflexive address discovery and ICE connectivity checks.
         */
        fun bindingRequest(transactionId: ByteArray = generateTransactionId()): StunMessage {
            return StunMessage(
                type = StunMessageType(StunMethod.BINDING, StunClass.REQUEST),
                transactionId = transactionId,
                attributes = emptyList(),
            )
        }

        /**
         * Decode a complete STUN message from wire-format bytes.
         *
         * Validates the magic cookie and parses the header and all attributes.
         *
         * @param data Raw bytes received from the network
         * @return The decoded [StunMessage]
         * @throws IllegalArgumentException if the data is too short, has an invalid
         *         magic cookie, or the declared length exceeds available data
         */
        fun decode(data: ByteArray): StunMessage {
            require(data.size >= HEADER_SIZE) {
                "STUN message too short: ${data.size} bytes (minimum $HEADER_SIZE)"
            }

            // Verify top 2 bits are zero (STUN multiplexing indicator)
            require(data[0].toInt() and 0xC0 == 0) {
                "First two bits must be zero for STUN (got 0x${(data[0].toInt() and 0xFF).toString(16)})"
            }

            // Type (2 bytes)
            val typeValue = readUInt16(data, 0)
            val type = StunMessageType.decode(typeValue)

            // Message Length (2 bytes) -- body length excluding 20-byte header
            val messageLength = readUInt16(data, 2)
            require(data.size >= HEADER_SIZE + messageLength) {
                "STUN message truncated: declared body length $messageLength, " +
                    "but only ${data.size - HEADER_SIZE} bytes available after header"
            }

            // Magic Cookie (4 bytes)
            val cookie = readInt32(data, 4)
            require(cookie == MAGIC_COOKIE) {
                "Invalid magic cookie: 0x${cookie.toUInt().toString(16)} " +
                    "(expected 0x${MAGIC_COOKIE.toUInt().toString(16)})"
            }

            // Transaction ID (12 bytes)
            val transactionId = data.copyOfRange(8, 8 + TRANSACTION_ID_LENGTH)

            // Decode attributes from the body
            val attributes = StunAttributeCodec.decodeAll(
                data = data,
                offset = HEADER_SIZE,
                length = messageLength,
                transactionId = transactionId,
            )

            return StunMessage(type = type, transactionId = transactionId, attributes = attributes)
        }

        // ─── Private Helpers ───────────────────────────────────────

        /** Read an unsigned 16-bit integer in big-endian byte order. */
        private fun readUInt16(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF shl 8) or
                (data[offset + 1].toInt() and 0xFF)
        }

        /** Read a signed 32-bit integer in big-endian byte order. */
        private fun readInt32(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF shl 24) or
                (data[offset + 1].toInt() and 0xFF shl 16) or
                (data[offset + 2].toInt() and 0xFF shl 8) or
                (data[offset + 3].toInt() and 0xFF)
        }
    }
}
