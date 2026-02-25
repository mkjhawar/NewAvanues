/**
 * SynonymBinaryFormat.kt - Binary format for synonym packs (.qsyn)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 *
 * Efficient binary format for production synonym packs.
 * Provides O(1) lookup via hash index.
 *
 * File Format (.qsyn):
 * ┌─────────────────────────────────────┐
 * │ Header (24 bytes)                   │
 * │ - Magic: "QSYN" (4 bytes)           │
 * │ - Version: uint16 (2 bytes)         │
 * │ - Flags: uint16 (2 bytes)           │
 * │ - Language: 3 chars + null (4 bytes)│
 * │ - Entry count: uint32 (4 bytes)     │
 * │ - Index offset: uint32 (4 bytes)    │
 * │ - Data offset: uint32 (4 bytes)     │
 * ├─────────────────────────────────────┤
 * │ Metadata Section                    │
 * │ - Language name (null-terminated)   │
 * │ - Version string (null-terminated)  │
 * │ - Script type: uint8                │
 * │ - Tokenizer type: uint8             │
 * │ - Flags: uint8 (RTL, etc.)          │
 * ├─────────────────────────────────────┤
 * │ Index Section                       │
 * │ - Sorted synonym hashes for search  │
 * │ - [hash: uint32, offset: uint32]    │
 * ├─────────────────────────────────────┤
 * │ Data Section                        │
 * │ - Canonical actions (null-term)     │
 * │ - Synonym strings (null-term)       │
 * └─────────────────────────────────────┘
 */
package com.augmentalis.voiceoscore

/**
 * Binary format constants and utilities.
 */
object SynonymBinaryFormat {
    /** Magic bytes identifying .qsyn format */
    const val MAGIC = "QSYN"

    /** Current format version */
    const val VERSION: Short = 1

    /** Header size in bytes */
    const val HEADER_SIZE = 24

    /** Flag bits */
    const val FLAG_RTL: Short = 0x0001
    const val FLAG_COMPRESSED: Short = 0x0002

    /**
     * Write a SynonymMap to binary format.
     *
     * @param map The SynonymMap to serialize
     * @return ByteArray containing the binary data
     */
    fun write(map: SynonymMap): ByteArray {
        val writer = BinaryWriter()

        // Build index entries
        val indexEntries = mutableListOf<IndexEntry>()
        val dataBuilder = StringBuilder()

        for (entry in map.getAllEntries()) {
            // Record canonical action offset
            val canonicalOffset = dataBuilder.length

            // Write canonical action
            dataBuilder.append(entry.canonical)
            dataBuilder.append('\u0000') // null terminator

            // Write synonym count
            val synonymCount = entry.synonyms.size

            // Write synonyms
            for (synonym in entry.synonyms) {
                // Add index entry for this synonym
                indexEntries.add(IndexEntry(
                    hash = synonym.hashCode(),
                    dataOffset = canonicalOffset,
                    synonymOffset = dataBuilder.length
                ))

                dataBuilder.append(synonym)
                dataBuilder.append('\u0000')
            }

            // Also index the canonical itself
            indexEntries.add(IndexEntry(
                hash = entry.canonical.hashCode(),
                dataOffset = canonicalOffset,
                synonymOffset = -1 // -1 indicates this is the canonical itself
            ))
        }

        // Sort index by hash for binary search
        indexEntries.sortBy { it.hash }

        val dataBytes = dataBuilder.toString().encodeToByteArray()

        // Calculate offsets
        val metadataSize = estimateMetadataSize(map.metadata)
        val indexOffset = HEADER_SIZE + metadataSize
        val indexSize = indexEntries.size * 12 // 4 bytes hash + 4 bytes dataOffset + 4 bytes synonymOffset
        val dataOffset = indexOffset + indexSize

        // Write header
        writer.writeString(MAGIC, 4)
        writer.writeShort(VERSION)
        writer.writeShort(buildFlags(map.metadata))
        writer.writeString(map.languageCode.take(3).padEnd(4, '\u0000'), 4)
        writer.writeInt(indexEntries.size)
        writer.writeInt(indexOffset)
        writer.writeInt(dataOffset)

        // Write metadata
        writer.writeNullTerminatedString(map.metadata.languageName)
        writer.writeNullTerminatedString(map.metadata.version)
        writer.writeByte(map.metadata.script.ordinal.toByte())
        writer.writeByte(map.metadata.tokenizer.ordinal.toByte())
        writer.writeByte(if (map.metadata.isRtl) 1 else 0)

        // Pad metadata to expected size
        while (writer.size < indexOffset) {
            writer.writeByte(0)
        }

        // Write index
        for (entry in indexEntries) {
            writer.writeInt(entry.hash)
            writer.writeInt(entry.dataOffset)
            writer.writeInt(entry.synonymOffset)
        }

        // Write data
        writer.writeBytes(dataBytes)

        return writer.toByteArray()
    }

    /**
     * Read a SynonymMap from binary format.
     *
     * @param data The binary data to read
     * @return Parsed SynonymMap
     * @throws SynonymBinaryException if parsing fails
     */
    fun read(data: ByteArray): SynonymMap {
        val reader = BinaryReader(data)

        // Read and verify header
        val magic = reader.readString(4)
        if (magic != MAGIC) {
            throw SynonymBinaryException("Invalid magic bytes: $magic")
        }

        val version = reader.readShort()
        if (version > VERSION) {
            throw SynonymBinaryException("Unsupported version: $version")
        }

        val flags = reader.readShort()
        val languageCode = reader.readString(4).trimEnd('\u0000')
        val entryCount = reader.readInt()
        val indexOffset = reader.readInt()
        val dataOffset = reader.readInt()

        // Read metadata
        val languageName = reader.readNullTerminatedString()
        val versionStr = reader.readNullTerminatedString()
        val scriptOrdinal = reader.readByte().toInt()
        val tokenizerOrdinal = reader.readByte().toInt()
        val isRtl = reader.readByte() != 0.toByte()

        val metadata = LanguageMetadata(
            languageCode = languageCode,
            languageName = languageName,
            script = ScriptType.entries.getOrElse(scriptOrdinal) { ScriptType.LATIN },
            tokenizer = TokenizerType.entries.getOrElse(tokenizerOrdinal) { TokenizerType.WHITESPACE },
            isRtl = isRtl || (flags.toInt() and FLAG_RTL.toInt()) != 0,
            version = versionStr
        )

        // Read index
        reader.position = indexOffset
        val indexEntries = mutableListOf<IndexEntry>()
        repeat(entryCount) {
            indexEntries.add(IndexEntry(
                hash = reader.readInt(),
                dataOffset = reader.readInt(),
                synonymOffset = reader.readInt()
            ))
        }

        // Read data and build synonym map
        val dataString = data.decodeToString(dataOffset, data.size)
        val canonicalToSynonyms = mutableMapOf<String, MutableList<String>>()

        for (entry in indexEntries) {
            // Read canonical at dataOffset
            val canonicalEnd = dataString.indexOf('\u0000', entry.dataOffset)
            val canonical = dataString.substring(entry.dataOffset, canonicalEnd)

            if (entry.synonymOffset >= 0) {
                // This is a synonym entry
                val synonymEnd = dataString.indexOf('\u0000', entry.synonymOffset)
                val synonym = dataString.substring(entry.synonymOffset, synonymEnd)

                canonicalToSynonyms.getOrPut(canonical) { mutableListOf() }.add(synonym)
            }
        }

        // Build SynonymMap
        val builder = SynonymMap.Builder(languageCode).metadata(metadata)
        for ((canonical, synonyms) in canonicalToSynonyms) {
            builder.add(canonical, synonyms.distinct())
        }

        return builder.build()
    }

    /**
     * Validate binary data without fully parsing.
     */
    fun validate(data: ByteArray): Boolean {
        if (data.size < HEADER_SIZE) return false

        val magic = data.decodeToString(0, 4)
        if (magic != MAGIC) return false

        // Check version
        val version = (data[4].toInt() and 0xFF) or ((data[5].toInt() and 0xFF) shl 8)
        if (version > VERSION) return false

        return true
    }

    private fun estimateMetadataSize(metadata: LanguageMetadata): Int {
        return metadata.languageName.length + 1 +
               metadata.version.length + 1 +
               3 // script, tokenizer, flags bytes
    }

    private fun buildFlags(metadata: LanguageMetadata): Short {
        var flags: Short = 0
        if (metadata.isRtl) flags = (flags.toInt() or FLAG_RTL.toInt()).toShort()
        return flags
    }
}

/**
 * Index entry for binary search.
 */
private data class IndexEntry(
    val hash: Int,
    val dataOffset: Int,
    val synonymOffset: Int
)

/**
 * Simple binary writer.
 */
private class BinaryWriter {
    private val buffer = mutableListOf<Byte>()

    val size: Int get() = buffer.size

    fun writeByte(b: Byte) {
        buffer.add(b)
    }

    fun writeByte(b: Int) {
        buffer.add(b.toByte())
    }

    fun writeShort(s: Short) {
        buffer.add((s.toInt() and 0xFF).toByte())
        buffer.add(((s.toInt() shr 8) and 0xFF).toByte())
    }

    fun writeInt(i: Int) {
        buffer.add((i and 0xFF).toByte())
        buffer.add(((i shr 8) and 0xFF).toByte())
        buffer.add(((i shr 16) and 0xFF).toByte())
        buffer.add(((i shr 24) and 0xFF).toByte())
    }

    fun writeString(s: String, length: Int) {
        val bytes = s.encodeToByteArray()
        for (i in 0 until length) {
            buffer.add(if (i < bytes.size) bytes[i] else 0)
        }
    }

    fun writeNullTerminatedString(s: String) {
        buffer.addAll(s.encodeToByteArray().toList())
        buffer.add(0)
    }

    fun writeBytes(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}

/**
 * Simple binary reader.
 */
private class BinaryReader(private val data: ByteArray) {
    var position: Int = 0

    fun readByte(): Byte {
        return data[position++]
    }

    fun readShort(): Short {
        val low = data[position++].toInt() and 0xFF
        val high = data[position++].toInt() and 0xFF
        return (low or (high shl 8)).toShort()
    }

    fun readInt(): Int {
        val b0 = data[position++].toInt() and 0xFF
        val b1 = data[position++].toInt() and 0xFF
        val b2 = data[position++].toInt() and 0xFF
        val b3 = data[position++].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readString(length: Int): String {
        val bytes = data.sliceArray(position until position + length)
        position += length
        return bytes.decodeToString()
    }

    fun readNullTerminatedString(): String {
        val start = position
        while (position < data.size && data[position] != 0.toByte()) {
            position++
        }
        val result = data.sliceArray(start until position).decodeToString()
        position++ // Skip null terminator
        return result
    }
}

/**
 * Exception for binary format errors.
 */
class SynonymBinaryException(message: String, cause: Throwable? = null) : Exception(message, cause)
