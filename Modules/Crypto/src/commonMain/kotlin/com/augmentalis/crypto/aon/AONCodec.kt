/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

/**
 * Cross-platform AON codec for wrapping and unwrapping AVA model files.
 *
 * Every platform implementation performs the same verification pipeline:
 * 1. Magic bytes check
 * 2. Format version check
 * 3. HMAC-SHA256 signature verification
 * 4. SHA-256 integrity check (header truncated + footer full)
 * 5. CRC32 checksum verification
 * 6. Expiry timestamp check
 * 7. Identity/package whitelisting check
 * 8. AES-256-GCM decryption (if encrypted)
 */
expect object AONCodec {

    /**
     * Verify AON file integrity without extracting the payload.
     *
     * @param aonData Raw AON file bytes (header + payload + footer)
     * @param appIdentifier Optional app identity for package check.
     *        If null, uses [PlatformIdentity.getAppIdentifier].
     * @return Detailed verification result
     */
    suspend fun verify(aonData: ByteArray, appIdentifier: String? = null): AONVerifyResult

    /**
     * Unwrap AON file to extract raw ONNX model bytes.
     *
     * Performs full verification pipeline before returning the payload.
     * Throws [AONSecurityException] if any check fails.
     *
     * @param aonData Raw AON file bytes (header + payload + footer)
     * @param appIdentifier Optional app identity override.
     * @return Raw ONNX model bytes ready for inference
     */
    suspend fun unwrap(aonData: ByteArray, appIdentifier: String? = null): ByteArray

    /**
     * Quick check: does this data have AON magic bytes?
     * Does NOT perform any verification — just checks the first 8 bytes.
     */
    fun isAON(data: ByteArray): Boolean

    /**
     * Parse the header without performing verification.
     * Useful for reading metadata (model ID, version, etc.) before full unwrap.
     */
    fun parseHeader(data: ByteArray): AONHeader
}

/**
 * Result of AON file verification
 */
data class AONVerifyResult(
    val valid: Boolean,
    val hmacValid: Boolean,
    val integrityValid: Boolean,
    val identityValid: Boolean,
    val expired: Boolean,
    val modelId: String,
    val licenseTier: Int,
    val errors: List<String>
)

/**
 * Thrown when AON security verification fails
 */
class AONSecurityException(message: String) : Exception(message)
