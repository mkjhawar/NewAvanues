package com.augmentalis.httpavanue.hpack

/**
 * HPACK encoder (RFC 7541) — encodes headers into compressed header blocks.
 *
 * Uses Huffman encoding for string values when it produces smaller output
 * (RFC 7541 Section 5.2), and leverages both static and dynamic table
 * indexing for header name/value compression.
 */
class HpackEncoder(maxDynamicTableSize: Int = 4096) {
    private val dynamicTable = HpackDynamicTable(maxDynamicTableSize)

    /** Encode a list of header name-value pairs into an HPACK header block */
    fun encode(headers: List<Pair<String, String>>): ByteArray {
        // Pre-allocate with rough estimate: ~4 bytes overhead per header + string lengths
        val estimated = headers.sumOf { it.first.length + it.second.length + 4 }
        val buffer = ArrayList<Byte>(estimated)
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

        /**
         * Encode HPACK string (RFC 7541 Section 5.2).
         * Uses Huffman encoding when it produces a smaller output (H=1),
         * otherwise sends raw bytes (H=0).
         */
        fun encodeString(buffer: MutableList<Byte>, value: String) {
            val bytes = value.encodeToByteArray()
            val huffmanEncoded = HpackHuffman.encodeIfSmaller(bytes)
            if (huffmanEncoded != null) {
                // Huffman-encoded: H=1 (0x80 prefix)
                encodeInteger(buffer, huffmanEncoded.size, 7, 0x80)
                for (b in huffmanEncoded) buffer.add(b)
            } else {
                // Raw: H=0 (0x00 prefix)
                encodeInteger(buffer, bytes.size, 7, 0x00)
                for (b in bytes) buffer.add(b)
            }
        }
    }
}
