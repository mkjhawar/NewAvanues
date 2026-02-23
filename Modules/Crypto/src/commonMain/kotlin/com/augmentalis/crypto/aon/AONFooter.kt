/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

/**
 * Parsed AON file footer (128 bytes)
 *
 * Contains integrity verification data: hashes, CRC32, and metadata.
 * Construct from raw bytes via [AONFooter.parse].
 */
data class AONFooter(
    val headerHash: ByteArray,       // 32 bytes SHA-256 of header
    val onnxHash: ByteArray,         // 32 bytes SHA-256 of ONNX payload
    val footerMagic: ByteArray,      // 8 bytes "ENDAON\x01\x00"
    val fileSize: Long,
    val checksumCRC32: Int,
    val buildNumber: Int,
    val creatorSignature: String,    // Up to 16 chars
) {
    companion object {
        /**
         * Parse a 128-byte footer from raw bytes
         */
        fun parse(bytes: ByteArray): AONFooter {
            require(bytes.size >= AONFormat.FOOTER_SIZE) {
                "Footer must be at least ${AONFormat.FOOTER_SIZE} bytes, got ${bytes.size}"
            }

            return AONFooter(
                headerHash = bytes.copyOfRange(
                    AONFormat.FOFF_HEADER_HASH,
                    AONFormat.FOFF_HEADER_HASH + 32
                ),
                onnxHash = bytes.copyOfRange(
                    AONFormat.FOFF_ONNX_HASH,
                    AONFormat.FOFF_ONNX_HASH + 32
                ),
                footerMagic = bytes.copyOfRange(
                    AONFormat.FOFF_FOOTER_MAGIC,
                    AONFormat.FOFF_FOOTER_MAGIC + AONFormat.MAGIC_SIZE
                ),
                fileSize = AONFormat.getLongLE(bytes, AONFormat.FOFF_FILE_SIZE),
                checksumCRC32 = AONFormat.getIntLE(bytes, AONFormat.FOFF_CHECKSUM_CRC32),
                buildNumber = AONFormat.getIntLE(bytes, AONFormat.FOFF_BUILD_NUMBER),
                creatorSignature = AONFormat.extractString(
                    bytes,
                    AONFormat.FOFF_CREATOR_SIGNATURE,
                    AONFormat.CREATOR_SIZE
                )
            )
        }
    }

    /** Check if this footer has valid magic bytes */
    fun hasValidMagic(): Boolean = footerMagic.contentEquals(AONFormat.FOOTER_MAGIC)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AONFooter) return false
        return headerHash.contentEquals(other.headerHash) &&
                onnxHash.contentEquals(other.onnxHash) &&
                footerMagic.contentEquals(other.footerMagic) &&
                fileSize == other.fileSize &&
                checksumCRC32 == other.checksumCRC32 &&
                buildNumber == other.buildNumber &&
                creatorSignature == other.creatorSignature
    }

    override fun hashCode(): Int {
        var result = headerHash.contentHashCode()
        result = 31 * result + onnxHash.contentHashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + checksumCRC32
        result = 31 * result + buildNumber
        return result
    }
}
