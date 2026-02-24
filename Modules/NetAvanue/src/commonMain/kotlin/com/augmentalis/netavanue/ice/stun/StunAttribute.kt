package com.augmentalis.netavanue.ice.stun

/**
 * IP address family for STUN address attributes (RFC 5389, Section 15.1).
 */
enum class AddressFamily(val code: Int) {
    IPv4(0x01),
    IPv6(0x02);

    companion object {
        fun fromCode(code: Int): AddressFamily = when (code) {
            0x01 -> IPv4
            0x02 -> IPv6
            else -> throw IllegalArgumentException("Unknown address family: 0x${code.toString(16)}")
        }
    }
}

/**
 * STUN attributes (RFC 5389, Section 15).
 *
 * Each attribute is TLV-encoded in the message body:
 * ```
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |         Type                  |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Value (variable)                ....
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * ```
 *
 * Values are padded to the next 4-byte boundary. Padding bytes are not
 * included in the Length field.
 */
sealed class StunAttribute(val type: Int) {

    /**
     * MAPPED-ADDRESS (0x0001) -- RFC 5389, Section 15.1.
     *
     * Reports the reflexive transport address as seen by the server.
     * Superseded by XOR-MAPPED-ADDRESS but still included for backwards compat.
     */
    data class MappedAddress(
        val family: AddressFamily,
        val port: Int,
        val address: ByteArray,
    ) : StunAttribute(ATTR_MAPPED_ADDRESS) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MappedAddress) return false
            return family == other.family && port == other.port && address.contentEquals(other.address)
        }

        override fun hashCode(): Int {
            var result = family.hashCode()
            result = 31 * result + port
            result = 31 * result + address.contentHashCode()
            return result
        }
    }

    /**
     * XOR-MAPPED-ADDRESS (0x0020) -- RFC 5389, Section 15.2.
     *
     * Same semantics as MAPPED-ADDRESS but the port and address are XOR'd
     * with the magic cookie (and transaction ID for IPv6) to prevent NAT
     * ALGs from rewriting the embedded address.
     *
     * The [port] and [address] stored here are the DECODED (un-XOR'd) values.
     */
    data class XorMappedAddress(
        val family: AddressFamily,
        val port: Int,
        val address: ByteArray,
    ) : StunAttribute(ATTR_XOR_MAPPED_ADDRESS) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is XorMappedAddress) return false
            return family == other.family && port == other.port && address.contentEquals(other.address)
        }

        override fun hashCode(): Int {
            var result = family.hashCode()
            result = 31 * result + port
            result = 31 * result + address.contentHashCode()
            return result
        }
    }

    /**
     * USERNAME (0x0006) -- RFC 5389, Section 15.3.
     *
     * UTF-8 encoded username, used for message integrity checks in
     * ICE connectivity checks.
     */
    data class Username(val value: String) : StunAttribute(ATTR_USERNAME)

    /**
     * ERROR-CODE (0x0009) -- RFC 5389, Section 15.6.
     *
     * Carries an error response code (300-699) and a UTF-8 reason phrase.
     */
    data class ErrorCode(val code: Int, val reason: String) : StunAttribute(ATTR_ERROR_CODE)

    /**
     * SOFTWARE (0x8022) -- RFC 5389, Section 15.10.
     *
     * Textual description of the software being used (client or server).
     */
    data class Software(val value: String) : StunAttribute(ATTR_SOFTWARE)

    /**
     * Catch-all for any attribute type we don't explicitly parse.
     * Preserves the raw bytes so the attribute can be re-encoded losslessly.
     */
    data class UnknownAttribute(val attrType: Int, val value: ByteArray) : StunAttribute(attrType) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UnknownAttribute) return false
            return attrType == other.attrType && value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            var result = attrType
            result = 31 * result + value.contentHashCode()
            return result
        }
    }

    companion object {
        const val ATTR_MAPPED_ADDRESS = 0x0001
        const val ATTR_USERNAME = 0x0006
        const val ATTR_MESSAGE_INTEGRITY = 0x0008
        const val ATTR_ERROR_CODE = 0x0009
        const val ATTR_UNKNOWN_ATTRIBUTES = 0x000A
        const val ATTR_REALM = 0x0014
        const val ATTR_NONCE = 0x0015
        const val ATTR_XOR_MAPPED_ADDRESS = 0x0020
        const val ATTR_SOFTWARE = 0x8022
        const val ATTR_ALTERNATE_SERVER = 0x8023
        const val ATTR_FINGERPRINT = 0x8028
    }
}

/**
 * Codec for encoding and decoding STUN attributes to/from byte arrays.
 *
 * All multi-byte integers use big-endian (network) byte order.
 */
object StunAttributeCodec {

    /**
     * Encode a single STUN attribute into bytes (type + length + value + padding).
     *
     * @param attr The attribute to encode
     * @param transactionId The 12-byte transaction ID (needed for XOR-MAPPED-ADDRESS IPv6)
     * @return The encoded attribute bytes including padding to 4-byte boundary
     */
    fun encode(attr: StunAttribute, transactionId: ByteArray): ByteArray {
        val valueBytes = encodeValue(attr, transactionId)
        val paddedLen = paddedLength(valueBytes.size)
        val result = ByteArray(4 + paddedLen)

        // Type (2 bytes, big-endian)
        result[0] = (attr.type shr 8).toByte()
        result[1] = (attr.type and 0xFF).toByte()

        // Length (2 bytes, big-endian) -- actual value length, NOT padded
        result[2] = (valueBytes.size shr 8).toByte()
        result[3] = (valueBytes.size and 0xFF).toByte()

        // Value
        valueBytes.copyInto(result, destinationOffset = 4)

        // Padding bytes remain zero (ByteArray default)
        return result
    }

    /**
     * Decode all attributes from a STUN message body.
     *
     * @param data The full STUN message bytes
     * @param offset Starting offset of the first attribute (typically 20, after the header)
     * @param length Total attribute section length in bytes
     * @param transactionId The 12-byte transaction ID (needed for XOR-MAPPED-ADDRESS)
     * @return List of decoded attributes
     */
    fun decodeAll(
        data: ByteArray,
        offset: Int,
        length: Int,
        transactionId: ByteArray,
    ): List<StunAttribute> {
        val attributes = mutableListOf<StunAttribute>()
        var pos = offset
        val end = offset + length

        while (pos + 4 <= end) {
            val attrType = readUInt16(data, pos)
            val attrLen = readUInt16(data, pos + 2)
            pos += 4

            if (pos + attrLen > end) break // malformed, stop parsing

            val attr = decodeValue(attrType, data, pos, attrLen, transactionId)
            attributes.add(attr)

            // Advance past value + padding to next 4-byte boundary
            pos += paddedLength(attrLen)
        }

        return attributes
    }

    // ─── Value Encoding ────────────────────────────────────────────

    private fun encodeValue(attr: StunAttribute, transactionId: ByteArray): ByteArray {
        return when (attr) {
            is StunAttribute.MappedAddress -> encodeMappedAddress(attr.family, attr.port, attr.address)
            is StunAttribute.XorMappedAddress -> encodeXorMappedAddress(attr.family, attr.port, attr.address, transactionId)
            is StunAttribute.Username -> attr.value.encodeToByteArray()
            is StunAttribute.ErrorCode -> encodeErrorCode(attr.code, attr.reason)
            is StunAttribute.Software -> attr.value.encodeToByteArray()
            is StunAttribute.UnknownAttribute -> attr.value
        }
    }

    private fun encodeMappedAddress(family: AddressFamily, port: Int, address: ByteArray): ByteArray {
        val addrLen = if (family == AddressFamily.IPv4) 4 else 16
        require(address.size == addrLen) {
            "Address length ${address.size} does not match family $family (expected $addrLen)"
        }
        val result = ByteArray(4 + addrLen)
        result[0] = 0 // reserved
        result[1] = family.code.toByte()
        result[2] = (port shr 8).toByte()
        result[3] = (port and 0xFF).toByte()
        address.copyInto(result, destinationOffset = 4)
        return result
    }

    private fun encodeXorMappedAddress(
        family: AddressFamily,
        port: Int,
        address: ByteArray,
        transactionId: ByteArray,
    ): ByteArray {
        val addrLen = if (family == AddressFamily.IPv4) 4 else 16
        require(address.size == addrLen) {
            "Address length ${address.size} does not match family $family (expected $addrLen)"
        }

        val magicCookieBytes = intToBytes(StunMessage.MAGIC_COOKIE)

        // XOR port with upper 16 bits of magic cookie
        val xPort = port xor ((magicCookieBytes[0].toInt() and 0xFF shl 8) or (magicCookieBytes[1].toInt() and 0xFF))

        // XOR address with magic cookie (IPv4) or magic cookie + transaction ID (IPv6)
        val xAddress = ByteArray(addrLen)
        if (family == AddressFamily.IPv4) {
            for (i in 0 until 4) {
                xAddress[i] = (address[i].toInt() xor magicCookieBytes[i].toInt()).toByte()
            }
        } else {
            // IPv6: XOR with magic cookie (4 bytes) + transaction ID (12 bytes) = 16 bytes
            val xorKey = magicCookieBytes + transactionId
            for (i in 0 until 16) {
                xAddress[i] = (address[i].toInt() xor xorKey[i].toInt()).toByte()
            }
        }

        val result = ByteArray(4 + addrLen)
        result[0] = 0 // reserved
        result[1] = family.code.toByte()
        result[2] = (xPort shr 8).toByte()
        result[3] = (xPort and 0xFF).toByte()
        xAddress.copyInto(result, destinationOffset = 4)
        return result
    }

    private fun encodeErrorCode(code: Int, reason: String): ByteArray {
        val reasonBytes = reason.encodeToByteArray()
        val result = ByteArray(4 + reasonBytes.size)
        // First 2 bytes reserved (zero)
        val errorClass = code / 100
        val errorNumber = code % 100
        result[2] = (errorClass and 0x07).toByte()
        result[3] = (errorNumber and 0xFF).toByte()
        reasonBytes.copyInto(result, destinationOffset = 4)
        return result
    }

    // ─── Value Decoding ────────────────────────────────────────────

    private fun decodeValue(
        attrType: Int,
        data: ByteArray,
        offset: Int,
        length: Int,
        transactionId: ByteArray,
    ): StunAttribute {
        return when (attrType) {
            StunAttribute.ATTR_MAPPED_ADDRESS -> decodeMappedAddress(data, offset, length)
            StunAttribute.ATTR_XOR_MAPPED_ADDRESS -> decodeXorMappedAddress(data, offset, length, transactionId)
            StunAttribute.ATTR_USERNAME -> StunAttribute.Username(data.decodeToString(offset, offset + length))
            StunAttribute.ATTR_ERROR_CODE -> decodeErrorCode(data, offset, length)
            StunAttribute.ATTR_SOFTWARE -> StunAttribute.Software(data.decodeToString(offset, offset + length))
            else -> StunAttribute.UnknownAttribute(attrType, data.copyOfRange(offset, offset + length))
        }
    }

    private fun decodeMappedAddress(data: ByteArray, offset: Int, length: Int): StunAttribute.MappedAddress {
        // byte 0: reserved, byte 1: family, bytes 2-3: port, bytes 4+: address
        val family = AddressFamily.fromCode(data[offset + 1].toInt() and 0xFF)
        val port = readUInt16(data, offset + 2)
        val addrLen = if (family == AddressFamily.IPv4) 4 else 16
        require(length >= 4 + addrLen) { "MAPPED-ADDRESS too short: $length bytes" }
        val address = data.copyOfRange(offset + 4, offset + 4 + addrLen)
        return StunAttribute.MappedAddress(family, port, address)
    }

    private fun decodeXorMappedAddress(
        data: ByteArray,
        offset: Int,
        length: Int,
        transactionId: ByteArray,
    ): StunAttribute.XorMappedAddress {
        val family = AddressFamily.fromCode(data[offset + 1].toInt() and 0xFF)
        val xPort = readUInt16(data, offset + 2)
        val addrLen = if (family == AddressFamily.IPv4) 4 else 16
        require(length >= 4 + addrLen) { "XOR-MAPPED-ADDRESS too short: $length bytes" }

        val magicCookieBytes = intToBytes(StunMessage.MAGIC_COOKIE)

        // Un-XOR port
        val port = xPort xor ((magicCookieBytes[0].toInt() and 0xFF shl 8) or (magicCookieBytes[1].toInt() and 0xFF))

        // Un-XOR address
        val xAddress = data.copyOfRange(offset + 4, offset + 4 + addrLen)
        val address = ByteArray(addrLen)
        if (family == AddressFamily.IPv4) {
            for (i in 0 until 4) {
                address[i] = (xAddress[i].toInt() xor magicCookieBytes[i].toInt()).toByte()
            }
        } else {
            val xorKey = magicCookieBytes + transactionId
            for (i in 0 until 16) {
                address[i] = (xAddress[i].toInt() xor xorKey[i].toInt()).toByte()
            }
        }

        return StunAttribute.XorMappedAddress(family, port, address)
    }

    private fun decodeErrorCode(data: ByteArray, offset: Int, length: Int): StunAttribute.ErrorCode {
        require(length >= 4) { "ERROR-CODE too short: $length bytes" }
        val errorClass = data[offset + 2].toInt() and 0x07
        val errorNumber = data[offset + 3].toInt() and 0xFF
        val code = errorClass * 100 + errorNumber
        val reason = if (length > 4) data.decodeToString(offset + 4, offset + length) else ""
        return StunAttribute.ErrorCode(code, reason)
    }

    // ─── Utilities ─────────────────────────────────────────────────

    /** Read an unsigned 16-bit integer in big-endian byte order. */
    private fun readUInt16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF shl 8) or
            (data[offset + 1].toInt() and 0xFF)
    }

    /** Convert a 32-bit integer to 4 bytes in big-endian byte order. */
    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            (value and 0xFF).toByte(),
        )
    }

    /** Round up to the next multiple of 4. */
    private fun paddedLength(length: Int): Int {
        return (length + 3) and 0x7FFFFFFC.toInt() // clear lower 2 bits after rounding
    }
}
