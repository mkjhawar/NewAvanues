/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.digest

/**
 * Cross-platform cryptographic digest and HMAC operations.
 *
 * Platform implementations:
 * - JVM (Android + Desktop): java.security.MessageDigest, javax.crypto.Mac, java.util.zip.CRC32
 * - iOS/macOS: CommonCrypto (CC_SHA256, CCHmac, CC_MD5) via cinterop
 * - JS: Node.js crypto (sync) or browser crypto.subtle (async)
 *
 * All suspend functions — synchronous on JVM/iOS, actually suspends on browser JS.
 */
expect object CryptoDigest {

    /** Compute SHA-256 hash (32 bytes) */
    suspend fun sha256(data: ByteArray): ByteArray

    /** Compute SHA-256 hash truncated to first 16 bytes */
    suspend fun sha256Truncated16(data: ByteArray): ByteArray

    /** Compute MD5 hash (16 bytes) — used for package identity hashing */
    suspend fun md5(data: ByteArray): ByteArray

    /** Compute HMAC-SHA256 (32 bytes) */
    suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    /** Compute CRC32 checksum */
    fun crc32(data: ByteArray): Int

    /** Compute CRC32 over multiple byte arrays (concatenated) */
    fun crc32(vararg chunks: ByteArray): Int
}
