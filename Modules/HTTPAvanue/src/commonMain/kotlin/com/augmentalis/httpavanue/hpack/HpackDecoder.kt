package com.augmentalis.httpavanue.hpack

import com.augmentalis.httpavanue.http2.Http2ErrorCode
import com.augmentalis.httpavanue.http2.Http2Exception

/**
 * HPACK decoder (RFC 7541) — decodes compressed header blocks
 */
class HpackDecoder(maxDynamicTableSize: Int = 4096) {
    private val dynamicTable = HpackDynamicTable(maxDynamicTableSize)

    /** Decode a complete header block into a list of name-value pairs */
    fun decode(data: ByteArray): List<Pair<String, String>> {
        val headers = mutableListOf<Pair<String, String>>()
        var offset = 0

        while (offset < data.size) {
            val byte = data[offset].toInt() and 0xFF

            when {
                // Indexed Header Field (Section 6.1): 1xxxxxxx
                byte and 0x80 != 0 -> {
                    val (index, newOffset) = decodeInteger(data, offset, 7)
                    offset = newOffset
                    val entry = lookupIndex(index)
                    headers.add(entry.name to entry.value)
                }
                // Literal Header Field with Incremental Indexing (Section 6.2.1): 01xxxxxx
                byte and 0xC0 == 0x40 -> {
                    val (name, value, newOffset) = decodeLiteral(data, offset, 6, addToTable = true)
                    offset = newOffset
                    headers.add(name to value)
                }
                // Dynamic Table Size Update (Section 6.3): 001xxxxx
                byte and 0xE0 == 0x20 -> {
                    val (newSize, newOffset) = decodeInteger(data, offset, 5)
                    offset = newOffset
                    dynamicTable.setMaxSize(newSize)
                }
                // Literal Header Field without Indexing (Section 6.2.2): 0000xxxx
                byte and 0xF0 == 0x00 -> {
                    val (name, value, newOffset) = decodeLiteral(data, offset, 4, addToTable = false)
                    offset = newOffset
                    headers.add(name to value)
                }
                // Literal Header Field Never Indexed (Section 6.2.3): 0001xxxx
                byte and 0xF0 == 0x10 -> {
                    val (name, value, newOffset) = decodeLiteral(data, offset, 4, addToTable = false)
                    offset = newOffset
                    headers.add(name to value)
                }
                else -> throw Http2Exception(Http2ErrorCode.COMPRESSION_ERROR, "Invalid HPACK byte: $byte")
            }
        }
        return headers
    }

    private fun lookupIndex(index: Int): HpackStaticTable.Entry {
        if (index <= 0) throw Http2Exception(Http2ErrorCode.COMPRESSION_ERROR, "Invalid index: $index")
        if (index <= HpackStaticTable.size) return HpackStaticTable.get(index)!!
        return dynamicTable.getByAbsoluteIndex(index)
            ?: throw Http2Exception(Http2ErrorCode.COMPRESSION_ERROR, "Index $index out of range")
    }

    private data class LiteralResult(val name: String, val value: String, val offset: Int)

    private fun decodeLiteral(data: ByteArray, startOffset: Int, prefixBits: Int, addToTable: Boolean): LiteralResult {
        val (nameIndex, offset1) = decodeInteger(data, startOffset, prefixBits)
        val name: String
        var offset = offset1
        if (nameIndex == 0) {
            // Name is a literal string
            val (nameStr, newOffset) = decodeString(data, offset)
            name = nameStr; offset = newOffset
        } else {
            name = lookupIndex(nameIndex).name
        }
        val (value, offset2) = decodeString(data, offset)
        if (addToTable) dynamicTable.add(name, value)
        return LiteralResult(name, value, offset2)
    }

    companion object {
        /** Decode HPACK integer (RFC 7541 Section 5.1) */
        fun decodeInteger(data: ByteArray, startOffset: Int, prefixBits: Int): Pair<Int, Int> {
            val mask = (1 shl prefixBits) - 1
            var value = data[startOffset].toInt() and mask
            var offset = startOffset + 1
            if (value < mask) return value to offset
            var m = 0
            do {
                if (offset >= data.size) throw Http2Exception(Http2ErrorCode.COMPRESSION_ERROR, "Truncated integer")
                val b = data[offset].toInt() and 0xFF
                offset++
                value += (b and 0x7F) shl m
                m += 7
            } while (b and 0x80 != 0)
            return value to offset
        }

        /** Decode HPACK string (RFC 7541 Section 5.2) */
        fun decodeString(data: ByteArray, startOffset: Int): Pair<String, Int> {
            val byte = data[startOffset].toInt() and 0xFF
            val isHuffman = byte and 0x80 != 0
            val (length, offset) = decodeInteger(data, startOffset, 7)
            if (offset + length > data.size) throw Http2Exception(Http2ErrorCode.COMPRESSION_ERROR, "Truncated string")
            val stringBytes = data.copyOfRange(offset, offset + length)
            val result = if (isHuffman) {
                HpackHuffman.decode(stringBytes).decodeToString()
            } else {
                stringBytes.decodeToString()
            }
            return result to (offset + length)
        }
    }
}
