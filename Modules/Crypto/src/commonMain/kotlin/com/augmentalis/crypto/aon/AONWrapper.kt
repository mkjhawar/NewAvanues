/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

import com.augmentalis.crypto.digest.CryptoDigest

/**
 * Shared AON file builder — constructs the complete binary from a
 * (possibly encrypted) payload and metadata.
 *
 * This helper lives in commonMain so all platform [AONCodec] actuals
 * delegate here after handling platform-specific concerns (encryption,
 * IV generation, timestamp).
 *
 * The caller is responsible for:
 * 1. Encrypting the payload if [AONWrapConfig.encrypt] is true
 * 2. Generating a cryptographically secure IV for encryption
 * 3. Providing the current Unix timestamp in seconds
 *
 * This object handles:
 * - 256-byte header construction with all metadata fields
 * - HMAC-SHA256 signature computation (using zeroed-sig header + SHA-256(payload))
 * - 128-byte footer construction with integrity hashes and CRC32
 * - Concatenation into the final AON byte array
 */
internal object AONWrapper {

    /**
     * Build a complete AON file.
     *
     * @param payload ONNX bytes (raw or already AES-256-GCM encrypted)
     * @param config Wrapping configuration (model ID, version, etc.)
     * @param hmacKey HMAC signing key (from platform-specific key store or default)
     * @param currentTimeSec Current time in Unix seconds (for created timestamp)
     * @param iv AES-GCM IV/nonce (16 bytes, only first 12 used). Zero-filled if not encrypting.
     * @return Complete AON file bytes: header (256) + payload + footer (128)
     */
    suspend fun buildAonFile(
        payload: ByteArray,
        config: AONWrapConfig,
        hmacKey: ByteArray,
        currentTimeSec: Long,
        iv: ByteArray = ByteArray(16)
    ): ByteArray {

        // ── 1. Build header (256 bytes, signature zeroed) ───────────

        val header = ByteArray(AONFormat.HEADER_SIZE)

        // Magic: "AVA-AON\x01"
        AONFormat.MAGIC.copyInto(header, AONFormat.OFF_MAGIC)

        // Format version
        AONFormat.putIntLE(header, AONFormat.OFF_FORMAT_VERSION, AONFormat.FORMAT_VERSION)

        // Signature field — stays zeroed until HMAC is computed (step 3)

        // Model ID (null-padded to 32 bytes)
        AONFormat.putString(header, AONFormat.OFF_MODEL_ID, config.modelId, AONFormat.MODEL_ID_SIZE)

        // Model version
        AONFormat.putIntLE(header, AONFormat.OFF_MODEL_VERSION, config.modelVersion)

        // Timestamps
        val timestamp = if (config.createdTimestamp > 0) config.createdTimestamp else currentTimeSec
        AONFormat.putLongLE(header, AONFormat.OFF_CREATED_TIMESTAMP, timestamp)
        AONFormat.putLongLE(header, AONFormat.OFF_EXPIRY_TIMESTAMP, config.expiryTimestamp)

        // License tier + platform flags
        header[AONFormat.OFF_LICENSE_TIER] = config.licenseTier
        header[AONFormat.OFF_PLATFORM_FLAGS] = config.platformFlags

        // Encryption scheme + IV nonce
        header[AONFormat.OFF_ENCRYPTION_SCHEME] =
            if (config.encrypt) AONFormat.ENCRYPTION_AES_256_GCM else AONFormat.ENCRYPTION_NONE
        iv.copyInto(header, AONFormat.OFF_IV_NONCE, 0, minOf(iv.size, AONFormat.IV_NONCE_SIZE))

        // ONNX data offset (always HEADER_SIZE) and payload size
        AONFormat.putLongLE(header, AONFormat.OFF_ONNX_DATA_OFFSET, AONFormat.HEADER_SIZE.toLong())
        AONFormat.putLongLE(header, AONFormat.OFF_ONNX_DATA_SIZE, payload.size.toLong())

        // Truncated SHA-256 of payload (first 16 bytes)
        val payloadSha256 = CryptoDigest.sha256(payload)
        payloadSha256.copyOf(AONFormat.SHA256_TRUNCATED_SIZE)
            .copyInto(header, AONFormat.OFF_ONNX_SHA256_TRUNC)

        // Allowed packages (MD5 hashes, up to 3 slots)
        for (i in 0 until AONFormat.MAX_PACKAGES) {
            val offset = AONFormat.OFF_ALLOWED_PACKAGES + (i * AONFormat.PACKAGE_HASH_SIZE)
            if (i < config.allowedPackages.size) {
                val pkgHash = CryptoDigest.md5(config.allowedPackages[i].encodeToByteArray())
                pkgHash.copyInto(header, offset, 0, minOf(pkgHash.size, AONFormat.PACKAGE_HASH_SIZE))
            }
            // Remaining slots stay zeroed (no restriction)
        }

        // ── 2. Compute HMAC-SHA256 signature ────────────────────────
        // Input: header (with zeroed sig field) + SHA-256(payload)
        // Output: 64 bytes (HMAC doubled: 32 + 32)

        val signInput = header + payloadSha256
        val hmac = CryptoDigest.hmacSha256(hmacKey, signInput)
        val doubledHmac = hmac + hmac  // 64 bytes to prevent shortening attacks
        doubledHmac.copyInto(header, AONFormat.OFF_SIGNATURE)

        // ── 3. Build footer (128 bytes) ─────────────────────────────

        val footer = ByteArray(AONFormat.FOOTER_SIZE)

        // SHA-256 of the final header (WITH signature now filled in)
        CryptoDigest.sha256(header).copyInto(footer, AONFormat.FOFF_HEADER_HASH)

        // SHA-256 of payload
        payloadSha256.copyInto(footer, AONFormat.FOFF_ONNX_HASH)

        // Footer magic: "ENDAON\x01\x00"
        AONFormat.FOOTER_MAGIC.copyInto(footer, AONFormat.FOFF_FOOTER_MAGIC)

        // Total file size
        val totalSize = (AONFormat.HEADER_SIZE + payload.size + AONFormat.FOOTER_SIZE).toLong()
        AONFormat.putLongLE(footer, AONFormat.FOFF_FILE_SIZE, totalSize)

        // CRC32 of header + payload
        AONFormat.putIntLE(footer, AONFormat.FOFF_CHECKSUM_CRC32, CryptoDigest.crc32(header, payload))

        // Build number
        AONFormat.putIntLE(footer, AONFormat.FOFF_BUILD_NUMBER, config.buildNumber)

        // Creator signature (null-padded to 16 bytes)
        AONFormat.putString(footer, AONFormat.FOFF_CREATOR_SIGNATURE, config.creatorSignature, AONFormat.CREATOR_SIZE)

        // ── 4. Concatenate ──────────────────────────────────────────

        return header + payload + footer
    }
}
