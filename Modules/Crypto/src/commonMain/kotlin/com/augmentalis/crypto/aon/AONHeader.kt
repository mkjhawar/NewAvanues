/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

/**
 * Parsed AON file header (256 bytes)
 *
 * All fields correspond to the binary layout defined in [AONFormat].
 * Construct from raw bytes via [AONHeader.parse].
 */
data class AONHeader(
    val magic: ByteArray,
    val formatVersion: Int,
    val signature: ByteArray,           // 64 bytes HMAC-SHA256 (doubled)
    val modelId: String,                // Up to 32 chars
    val modelVersion: Int,
    val createdTimestamp: Long,          // Unix seconds
    val expiryTimestamp: Long,          // Unix seconds (0 = no expiry)
    val licenseTier: Byte,              // 0=free, 1=pro, 2=enterprise
    val platformFlags: Byte,            // Bitfield for allowed platforms
    val encryptionScheme: Byte,         // 0=none, 1=AES-256-GCM
    val ivNonce: ByteArray,             // 16 bytes (12 used for GCM)
    val onnxDataOffset: Long,
    val onnxDataSize: Long,
    val onnxSHA256Truncated: ByteArray, // First 16 bytes of SHA-256
    val allowedPackages: List<ByteArray>, // Up to 3 MD5 hashes (16 bytes each)
) {
    companion object {
        /**
         * Parse a 256-byte header from raw bytes
         */
        fun parse(bytes: ByteArray): AONHeader {
            require(bytes.size >= AONFormat.HEADER_SIZE) {
                "Header must be at least ${AONFormat.HEADER_SIZE} bytes, got ${bytes.size}"
            }

            return AONHeader(
                magic = bytes.copyOfRange(AONFormat.OFF_MAGIC, AONFormat.OFF_MAGIC + AONFormat.MAGIC_SIZE),
                formatVersion = AONFormat.getIntLE(bytes, AONFormat.OFF_FORMAT_VERSION),
                signature = bytes.copyOfRange(AONFormat.OFF_SIGNATURE, AONFormat.OFF_SIGNATURE + AONFormat.SIGNATURE_SIZE),
                modelId = AONFormat.extractString(bytes, AONFormat.OFF_MODEL_ID, AONFormat.MODEL_ID_SIZE),
                modelVersion = AONFormat.getIntLE(bytes, AONFormat.OFF_MODEL_VERSION),
                createdTimestamp = AONFormat.getLongLE(bytes, AONFormat.OFF_CREATED_TIMESTAMP),
                expiryTimestamp = AONFormat.getLongLE(bytes, AONFormat.OFF_EXPIRY_TIMESTAMP),
                licenseTier = bytes[AONFormat.OFF_LICENSE_TIER],
                platformFlags = bytes[AONFormat.OFF_PLATFORM_FLAGS],
                encryptionScheme = bytes[AONFormat.OFF_ENCRYPTION_SCHEME],
                ivNonce = bytes.copyOfRange(AONFormat.OFF_IV_NONCE, AONFormat.OFF_IV_NONCE + AONFormat.IV_NONCE_SIZE),
                onnxDataOffset = AONFormat.getLongLE(bytes, AONFormat.OFF_ONNX_DATA_OFFSET),
                onnxDataSize = AONFormat.getLongLE(bytes, AONFormat.OFF_ONNX_DATA_SIZE),
                onnxSHA256Truncated = bytes.copyOfRange(
                    AONFormat.OFF_ONNX_SHA256_TRUNC,
                    AONFormat.OFF_ONNX_SHA256_TRUNC + AONFormat.SHA256_TRUNCATED_SIZE
                ),
                allowedPackages = List(AONFormat.MAX_PACKAGES) { i ->
                    val offset = AONFormat.OFF_ALLOWED_PACKAGES + (i * AONFormat.PACKAGE_HASH_SIZE)
                    bytes.copyOfRange(offset, offset + AONFormat.PACKAGE_HASH_SIZE)
                }
            )
        }
    }

    /** Check if this header has valid AON magic bytes */
    fun hasValidMagic(): Boolean = magic.contentEquals(AONFormat.MAGIC)

    /** Check if any allowed package slots are non-zero */
    fun hasPackageRestrictions(): Boolean = allowedPackages.any { pkg ->
        pkg.any { it != 0.toByte() }
    }

    /** Check if a specific platform flag is set */
    fun isPlatformAllowed(flag: Byte): Boolean {
        // 0x00 = all platforms allowed (backward compat)
        if (platformFlags == 0.toByte()) return true
        return (platformFlags.toInt() and flag.toInt()) != 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AONHeader) return false
        return modelId == other.modelId &&
                formatVersion == other.formatVersion &&
                modelVersion == other.modelVersion
    }

    override fun hashCode(): Int {
        var result = modelId.hashCode()
        result = 31 * result + formatVersion
        result = 31 * result + modelVersion
        return result
    }
}
