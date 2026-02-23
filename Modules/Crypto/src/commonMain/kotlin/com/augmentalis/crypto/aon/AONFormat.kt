/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

/**
 * AVA-AON File Format Constants — Single Source of Truth
 *
 * Defines the binary layout for .AON (AVA ONNX Naming) model files.
 * All platforms use these constants for reading/writing AON files.
 *
 * ## File Layout:
 * ```
 * ┌──────────────────────────────────┐
 * │  AON Header (256 bytes)          │  ← Authentication + metadata
 * ├──────────────────────────────────┤
 * │  ONNX Payload (variable)         │  ← Raw or AES-256-GCM encrypted
 * ├──────────────────────────────────┤
 * │  AON Footer (128 bytes)          │  ← Integrity verification
 * └──────────────────────────────────┘
 * ```
 *
 * ## Header Layout (256 bytes, Little-Endian):
 * ```
 * Offset  Size  Field
 * 0       8     magic               "AVA-AON\x01"
 * 8       4     formatVersion       Currently 1
 * 12      64    signature           HMAC-SHA256 (doubled to 64 bytes)
 * 76      32    modelId             Null-padded ASCII string
 * 108     4     modelVersion        Integer version
 * 112     8     createdTimestamp    Unix seconds
 * 120     8     expiryTimestamp     Unix seconds (0 = no expiry)
 * 128     1     licenseTier         0=free, 1=pro, 2=enterprise
 * 129     1     platformFlags       Bitfield: b0=Android b1=iOS b2=Desktop b3=Web b4=Node
 * 130     14    reserved1
 * 144     1     encryptionScheme    0=none, 1=AES-256-GCM
 * 145     16    ivNonce             AES-GCM IV (12 bytes used, padded to 16)
 * 161     15    reserved2
 * 176     8     onnxDataOffset      Always HEADER_SIZE (256)
 * 184     8     onnxDataSize        Payload byte count
 * 192     16    onnxSHA256Trunc     First 16 bytes of SHA-256(ONNX)
 * 208     16    allowedPackage[0]   MD5 hash of package/bundle/origin
 * 224     16    allowedPackage[1]   MD5 hash
 * 240     16    allowedPackage[2]   MD5 hash
 * ```
 *
 * ## Footer Layout (128 bytes, Little-Endian):
 * ```
 * Offset  Size  Field
 * 0       32    headerHash          SHA-256 of entire header
 * 32      32    onnxHash            SHA-256 of ONNX payload
 * 64      8     footerMagic         "ENDAON\x01\x00"
 * 72      8     fileSize            Total file size in bytes
 * 80      4     checksumCRC32       CRC32(header + ONNX payload)
 * 84      12    reserved4
 * 96      4     buildNumber
 * 100     16    creatorSignature    Null-padded ASCII
 * 116     12    reserved5
 * ```
 */
object AONFormat {

    // ─── Magic Bytes ─────────────────────────────────────────

    /** Header magic: ASCII "AVA-AON\x01" */
    val MAGIC = byteArrayOf(0x41, 0x56, 0x41, 0x2D, 0x41, 0x4F, 0x4E, 0x01)

    /** Footer magic: ASCII "ENDAON\x01\x00" */
    val FOOTER_MAGIC = byteArrayOf(0x45, 0x4E, 0x44, 0x41, 0x4F, 0x4E, 0x01, 0x00)

    // ─── Format Constants ────────────────────────────────────

    const val FORMAT_VERSION = 1
    const val HEADER_SIZE = 256
    const val FOOTER_SIZE = 128
    const val MAGIC_SIZE = 8
    const val SIGNATURE_SIZE = 64
    const val MODEL_ID_SIZE = 32
    const val IV_NONCE_SIZE = 16
    const val SHA256_TRUNCATED_SIZE = 16
    const val PACKAGE_HASH_SIZE = 16  // MD5 = 16 bytes
    const val MAX_PACKAGES = 3
    const val CREATOR_SIZE = 16

    // ─── Header Field Offsets ────────────────────────────────

    const val OFF_MAGIC = 0
    const val OFF_FORMAT_VERSION = 8
    const val OFF_SIGNATURE = 12
    const val OFF_MODEL_ID = 76
    const val OFF_MODEL_VERSION = 108
    const val OFF_CREATED_TIMESTAMP = 112
    const val OFF_EXPIRY_TIMESTAMP = 120
    const val OFF_LICENSE_TIER = 128
    const val OFF_PLATFORM_FLAGS = 129
    const val OFF_RESERVED1 = 130
    const val OFF_ENCRYPTION_SCHEME = 144
    const val OFF_IV_NONCE = 145
    const val OFF_RESERVED2 = 161
    const val OFF_ONNX_DATA_OFFSET = 176
    const val OFF_ONNX_DATA_SIZE = 184
    const val OFF_ONNX_SHA256_TRUNC = 192
    const val OFF_ALLOWED_PACKAGES = 208

    // ─── Footer Field Offsets ────────────────────────────────

    const val FOFF_HEADER_HASH = 0
    const val FOFF_ONNX_HASH = 32
    const val FOFF_FOOTER_MAGIC = 64
    const val FOFF_FILE_SIZE = 72
    const val FOFF_CHECKSUM_CRC32 = 80
    const val FOFF_RESERVED4 = 84
    const val FOFF_BUILD_NUMBER = 96
    const val FOFF_CREATOR_SIGNATURE = 100
    const val FOFF_RESERVED5 = 116

    // ─── Platform Flags ──────────────────────────────────────

    const val PLATFORM_ANDROID: Byte = 0x01
    const val PLATFORM_IOS: Byte = 0x02
    const val PLATFORM_DESKTOP: Byte = 0x04
    const val PLATFORM_WEB: Byte = 0x08
    const val PLATFORM_NODEJS: Byte = 0x10

    // ─── Encryption Schemes ──────────────────────────────────

    const val ENCRYPTION_NONE: Byte = 0x00
    const val ENCRYPTION_AES_256_GCM: Byte = 0x01

    // ─── Byte Helpers (Little-Endian) ────────────────────────

    fun putIntLE(dst: ByteArray, offset: Int, value: Int) {
        dst[offset] = (value and 0xFF).toByte()
        dst[offset + 1] = (value shr 8 and 0xFF).toByte()
        dst[offset + 2] = (value shr 16 and 0xFF).toByte()
        dst[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    fun getIntLE(src: ByteArray, offset: Int): Int {
        return (src[offset].toInt() and 0xFF) or
                ((src[offset + 1].toInt() and 0xFF) shl 8) or
                ((src[offset + 2].toInt() and 0xFF) shl 16) or
                ((src[offset + 3].toInt() and 0xFF) shl 24)
    }

    fun putLongLE(dst: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) {
            dst[offset + i] = (value shr (i * 8) and 0xFF).toByte()
        }
    }

    fun getLongLE(src: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((src[offset + i].toLong() and 0xFF) shl (i * 8))
        }
        return result
    }

    /** Copy a fixed number of bytes from src at srcOffset into dst at dstOffset */
    fun copyBytes(src: ByteArray, srcOffset: Int, dst: ByteArray, dstOffset: Int, length: Int) {
        src.copyInto(dst, dstOffset, srcOffset, srcOffset + length)
    }

    /** Extract a null-terminated ASCII string from a byte array */
    fun extractString(src: ByteArray, offset: Int, maxLen: Int): String {
        val end = (offset until (offset + maxLen)).firstOrNull { src[it] == 0.toByte() }
            ?: (offset + maxLen)
        return src.decodeToString(offset, end)
    }

    /** Write a null-padded ASCII string into a byte array */
    fun putString(dst: ByteArray, offset: Int, value: String, maxLen: Int) {
        val bytes = value.encodeToByteArray()
        val copyLen = minOf(bytes.size, maxLen)
        bytes.copyInto(dst, offset, 0, copyLen)
        // Remaining bytes are already 0 in a fresh array
    }

    // ─── HMAC Key (Obfuscated) ───────────────────────────────

    /**
     * Retrieve the HMAC signing key.
     *
     * The key is stored as XOR-masked fragments to prevent trivial
     * string extraction from compiled binaries. This is the same key
     * across all platforms to ensure cross-platform AON compatibility.
     *
     * Platform-specific overrides (Android Keystore, iOS Keychain,
     * env vars) are handled in each AONCodec actual implementation.
     */
    internal fun getDefaultHmacKey(): ByteArray {
        // "AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION" XOR-masked
        // Mask: 0x17, 0x3A, 0x82, 0x4F, 0xC1, 0x5D, 0x96
        val masked = intArrayOf(
            0x56, 0x6C, 0xC3, 0x62, 0x80, 0x12, 0xD8, // AVA-AON
            0x3A, 0x72, 0xCF, 0x0E, 0x82, 0x70, 0xC5, // -HMAC-S
            0x52, 0x79, 0xD0, 0x0A, 0x95, 0x70, 0xDD, // ECRET-K
            0x52, 0x63, 0xAF, 0x19, 0xF0, 0x70, 0xD5, // EY-V1-C
            0x5F, 0x7B, 0xCC, 0x08, 0x84, 0x70, 0xDF, // HANGE-I
            0x59, 0x17, 0xD2, 0x1D, 0x8E, 0x19, 0xC3, // N-PRODU
            0x54, 0x6E, 0xCB, 0x00, 0x8F                // CTION
        )
        val mask = intArrayOf(0x17, 0x3A, 0x82, 0x4F, 0xC1, 0x5D, 0x96)
        return ByteArray(masked.size) { i ->
            (masked[i] xor mask[i % mask.size]).toByte()
        }
    }
}
