/**
 * VSMFormat.kt - VoiceOS Speech Model file format specification
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Defines the .vlm (VoiceOS Language Model) encrypted container format.
 * Uses AES-256-CTR + XOR scramble + Fisher-Yates byte shuffle to protect
 * Whisper ggml model files from identification and extraction.
 *
 * File layout:
 *   [64-byte header] [4-byte metadata length] [metadata JSON] [encrypted blocks...]
 *
 * Block encryption (per 64 KB block):
 *   1. XOR scramble with SHA-512 derived pattern
 *   2. Fisher-Yates byte shuffle with seeded RNG
 *   3. AES-256-CTR encryption with per-block nonce
 */
package com.augmentalis.speechrecognition.whisper.vsm

/**
 * VSM file format constants and configuration.
 *
 * Magic bytes "VSM1" distinguish these files from AVA3 (.amm/.amg/.amr)
 * which use "AVA3". The master seed is unique to VSM — NOT shared with
 * the AI/ALC encryption pipeline.
 */
object VSMFormat {
    /** Magic bytes: 0x56='V', 0x53='S', 0x4D='M', 0x31='1' */
    const val MAGIC: Int = 0x56534D31

    /** Format version 1.0 */
    const val VERSION: Short = 0x0100

    /** Fixed header size in bytes */
    const val HEADER_SIZE: Int = 64

    /** Block size for chunked encryption (64 KB) */
    const val BLOCK_SIZE: Int = 65536

    /** Header flag: file is encrypted */
    const val FLAG_ENCRYPTED: Short = 0x0001

    /** Header flag: file is compressed before encryption */
    const val FLAG_COMPRESSED: Short = 0x0002

    /** Standard file extension — .vlm (VoiceOS Language Model) */
    const val VSM_EXTENSION: String = ".vlm"

    /** Partial download suffix */
    const val PARTIAL_SUFFIX: String = ".vlm.partial"

    /**
     * Master seed for key derivation — unique to VSM.
     * ASCII: "VSM-SPEECH-1.0-VOICEOS-2026-IDL\0"
     * NOT shared with AVA3's "AVA-AI-3.0-MANOJ-JHAWAR-2025-IDL" seed.
     */
    val MASTER_SEED: ByteArray = byteArrayOf(
        0x56, 0x53, 0x4D, 0x2D, 0x53, 0x50, 0x45, 0x45,
        0x43, 0x48, 0x2D, 0x31, 0x2E, 0x30, 0x2D, 0x56,
        0x4F, 0x49, 0x43, 0x45, 0x4F, 0x53, 0x2D, 0x32,
        0x30, 0x32, 0x36, 0x2D, 0x49, 0x44, 0x4C, 0x00
    )

    /** PBKDF2 iteration count for key derivation */
    const val PBKDF2_ITERATIONS: Int = 10000

    /** Salt for PBKDF2 key derivation */
    const val SALT: String = "VSM-1.0-SALT-2026"

    /** Shared model storage subdirectory name (under ava-ai-models/) */
    const val SHARED_STORAGE_DIR: String = "ava-ai-models/vlm"
}

/**
 * Parsed VSM file header (64 bytes).
 *
 * All multi-byte integers are little-endian.
 */
data class VSMHeader(
    val magic: Int,
    val version: Short,
    val flags: Short,
    val originalSize: Long,
    val encodedSize: Long,
    val blockSize: Int,
    val blockCount: Int,
    /** First 16 bytes of SHA-256 hash of the original (unencrypted) data */
    val fileHash: ByteArray,
    /** Timestamp (ms since epoch) used in key derivation */
    val timestamp: Long,
    val contentType: Int,
    val reserved: Int
) {
    /** Check if the header has valid magic bytes */
    fun isValid(): Boolean = magic == VSMFormat.MAGIC

    /** Check if the file is encrypted */
    fun isEncrypted(): Boolean = (flags.toInt() and VSMFormat.FLAG_ENCRYPTED.toInt()) != 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VSMHeader) return false
        return magic == other.magic && version == other.version &&
            flags == other.flags && originalSize == other.originalSize &&
            timestamp == other.timestamp
    }

    override fun hashCode(): Int = magic xor timestamp.toInt()
}

/**
 * Convert a ggml model filename (e.g. "ggml-base.en.bin") to clean VSM filename.
 * Looks up the enum entry for a clean name with no whisper/ggml traces.
 * Fallback: strips "ggml-" prefix and replaces extension if unknown.
 */
fun vsmFileName(ggmlFileName: String): String {
    // Exact match against known models — returns clean names like "VoiceOS-Bas-EN.vlm"
    val knownModel = com.augmentalis.speechrecognition.whisper.WhisperModelSize.entries
        .firstOrNull { it.ggmlFileName == ggmlFileName }
    if (knownModel != null) return knownModel.vsmName

    // Fallback for unknown filenames: strip "ggml-" prefix, replace extension
    return ggmlFileName
        .removePrefix("ggml-")
        .replace(".bin", VSMFormat.VSM_EXTENSION)
}

// --- Byte conversion helpers (little-endian) ---

fun intToLEBytes(value: Int): ByteArray = ByteArray(4).also {
    it[0] = (value and 0xFF).toByte()
    it[1] = ((value shr 8) and 0xFF).toByte()
    it[2] = ((value shr 16) and 0xFF).toByte()
    it[3] = ((value shr 24) and 0xFF).toByte()
}

fun longToLEBytes(value: Long): ByteArray = ByteArray(8).also {
    for (i in 0..7) it[i] = ((value shr (i * 8)) and 0xFF).toByte()
}

fun shortToLEBytes(value: Short): ByteArray = ByteArray(2).also {
    it[0] = (value.toInt() and 0xFF).toByte()
    it[1] = ((value.toInt() shr 8) and 0xFF).toByte()
}

fun leBytesToInt(bytes: ByteArray, offset: Int = 0): Int =
    (bytes[offset].toInt() and 0xFF) or
    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
    ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
    ((bytes[offset + 3].toInt() and 0xFF) shl 24)

fun leBytesToLong(bytes: ByteArray, offset: Int = 0): Long {
    var result = 0L
    for (i in 0..7) result = result or ((bytes[offset + i].toLong() and 0xFF) shl (i * 8))
    return result
}

fun leBytesToShort(bytes: ByteArray, offset: Int = 0): Short =
    ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort()
