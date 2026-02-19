package com.augmentalis.httpavanue.hpack

/**
 * HPACK encoder (RFC 7541) — encodes headers into compressed header blocks
 */
class HpackEncoder(maxDynamicTableSize: Int = 4096) {
    private val dynamicTable = HpackDynamicTable(maxDynamicTableSize)

    /** Encode a list of header name-value pairs into an HPACK header block */
    fun encode(headers: List<Pair<String, String>>): ByteArray {
        val buffer = mutableListOf<Byte>()
        for ((name, value) in headers) {
            encodeHeader(buffer, name, value)
        }
        return buffer.toByteArray()
    }

    private fun encodeHeader(buffer: MutableList<Byte>, name: String, value: String) {
        // Try full match in static table
        val staticIndex = HpackStaticTable.findIndex(name, value)
        if (staticIndex > 0) {
            // Indexed Header Field (Section 6.1)
            encodeInteger(buffer, staticIndex, 7, 0x80)
            return
        }

        // Try full match in dynamic table
        val dynamicIndex = dynamicTable.findIndex(name, value)
        if (dynamicIndex > 0) {
            encodeInteger(buffer, dynamicIndex, 7, 0x80)
            return
        }

        // Try name-only match
        val nameIndex = HpackStaticTable.findNameIndex(name).takeIf { it > 0 }
            ?: dynamicTable.findNameIndex(name).takeIf { it > 0 }

        if (nameIndex != null) {
            // Literal Header Field with Incremental Indexing, name reference
            encodeInteger(buffer, nameIndex, 6, 0x40)
        } else {
            // Literal Header Field with Incremental Indexing, new name
            buffer.add(0x40.toByte())
            encodeString(buffer, name)
        }
        encodeString(buffer, value)
        dynamicTable.add(name, value)
    }

    companion object {
        /** Encode HPACK integer (RFC 7541 Section 5.1) */
        fun encodeInteger(buffer: MutableList<Byte>, value: Int, prefixBits: Int, prefixPattern: Int) {
            val mask = (1 shl prefixBits) - 1
            if (value < mask) {
                buffer.add((prefixPattern or value).toByte())
            } else {
                buffer.add((prefixPattern or mask).toByte())
                var remaining = value - mask
                while (remaining >= 128) {
                    buffer.add(((remaining and 0x7F) or 0x80).toByte())
                    remaining = remaining shr 7
                }
                buffer.add(remaining.toByte())
            }
        }

        /** Encode HPACK string without Huffman encoding (RFC 7541 Section 5.2) */
        fun encodeString(buffer: MutableList<Byte>, value: String) {
            val bytes = value.encodeToByteArray()
            encodeInteger(buffer, bytes.size, 7, 0x00) // H=0 (no Huffman)
            bytes.forEach { buffer.add(it) }
        }
    }
}
