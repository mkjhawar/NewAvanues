/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

import com.augmentalis.crypto.digest.CryptoDigest
import com.augmentalis.crypto.identity.PlatformIdentity

/**
 * iOS/macOS implementation of AONCodec.
 * Uses CommonCrypto via CryptoDigest for all crypto operations.
 *
 * Note: AES-256-GCM decryption requires CommonCrypto's CCCrypt with
 * kCCAlgorithmAES + kCCOptionPKCS7Padding for GCM mode. For initial
 * release, encrypted models are not supported on iOS — only signed
 * and integrity-checked models.
 */
actual object AONCodec {

    actual suspend fun verify(aonData: ByteArray, appIdentifier: String?): AONVerifyResult {
        val errors = mutableListOf<String>()
        var hmacValid = false
        var integrityValid = false
        var identityValid = false
        var expired = false
        var modelId = ""
        var licenseTier = 0

        try {
            if (aonData.size < AONFormat.HEADER_SIZE + AONFormat.FOOTER_SIZE) {
                return AONVerifyResult(false, false, false, false, false, "", 0,
                    listOf("File too small: ${aonData.size} bytes"))
            }

            val headerBytes = aonData.copyOfRange(0, AONFormat.HEADER_SIZE)
            val header = AONHeader.parse(headerBytes)
            modelId = header.modelId
            licenseTier = header.licenseTier.toInt()

            // 1. Magic bytes
            if (!header.hasValidMagic()) {
                errors.add("Invalid magic bytes")
                return AONVerifyResult(false, false, false, false, false, modelId, licenseTier, errors)
            }

            // 2. Format version
            if (header.formatVersion != AONFormat.FORMAT_VERSION) {
                errors.add("Unsupported format version: ${header.formatVersion}")
                return AONVerifyResult(false, false, false, false, false, modelId, licenseTier, errors)
            }

            // Extract ONNX payload (safe conversion from Long to Int)
            val onnxSize = AONFormat.safeOnnxDataSize(header.onnxDataSize)
            val payloadEnd = AONFormat.HEADER_SIZE + onnxSize
            if (aonData.size < payloadEnd + AONFormat.FOOTER_SIZE) {
                errors.add("File truncated")
                return AONVerifyResult(false, false, false, false, false, modelId, licenseTier, errors)
            }

            val onnxData = aonData.copyOfRange(AONFormat.HEADER_SIZE, payloadEnd)
            val footerBytes = aonData.copyOfRange(payloadEnd, payloadEnd + AONFormat.FOOTER_SIZE)
            val footer = AONFooter.parse(footerBytes)

            // 3. HMAC-SHA256
            hmacValid = verifyHmac(headerBytes, onnxData, header.signature)
            if (!hmacValid) errors.add("HMAC signature mismatch")

            // 4. SHA-256 integrity
            val onnxHash = CryptoDigest.sha256(onnxData)
            val truncatedHash = onnxHash.copyOf(16)

            val headerIntegrity = truncatedHash.contentEquals(header.onnxSHA256Truncated)
            val footerOnnxIntegrity = onnxHash.contentEquals(footer.onnxHash)
            val headerHashValid = CryptoDigest.sha256(headerBytes).contentEquals(footer.headerHash)
            val footerMagicValid = footer.hasValidMagic()

            // 5. CRC32
            val expectedCrc = CryptoDigest.crc32(headerBytes, onnxData)
            val crcValid = expectedCrc == footer.checksumCRC32

            integrityValid = headerIntegrity && footerOnnxIntegrity && headerHashValid && footerMagicValid && crcValid
            if (!headerIntegrity) errors.add("ONNX SHA-256 truncated mismatch")
            if (!footerOnnxIntegrity) errors.add("ONNX SHA-256 full mismatch")
            if (!headerHashValid) errors.add("Header hash mismatch")
            if (!footerMagicValid) errors.add("Invalid footer magic")
            if (!crcValid) errors.add("CRC32 mismatch")

            // 6. Expiry
            if (header.expiryTimestamp > 0) {
                val now = platform.Foundation.NSDate().timeIntervalSince1970.toLong()
                expired = now > header.expiryTimestamp
                if (expired) errors.add("AON file expired")
            }

            // 7. Identity check
            identityValid = verifyIdentity(header, appIdentifier)
            if (!identityValid) errors.add("Bundle not authorized")

        } catch (e: Exception) {
            errors.add("Verification error: ${e.message}")
        }

        val valid = hmacValid && integrityValid && identityValid && !expired && errors.isEmpty()
        return AONVerifyResult(valid, hmacValid, integrityValid, identityValid, expired, modelId, licenseTier, errors)
    }

    actual suspend fun unwrap(aonData: ByteArray, appIdentifier: String?): ByteArray {
        if (!isAON(aonData)) return aonData

        val result = verify(aonData, appIdentifier)
        if (!result.valid) {
            throw AONSecurityException("AON verification failed: ${result.errors.joinToString("; ")}")
        }

        val header = parseHeader(aonData)
        val onnxSize = AONFormat.safeOnnxDataSize(header.onnxDataSize)
        val payloadEnd = AONFormat.HEADER_SIZE + onnxSize
        val onnxData = aonData.copyOfRange(AONFormat.HEADER_SIZE, payloadEnd)

        if (header.encryptionScheme == AONFormat.ENCRYPTION_AES_256_GCM) {
            throw AONSecurityException("AES-256-GCM decryption not yet supported on iOS. Use unencrypted AON files.")
        }

        return onnxData
    }

    actual fun isAON(data: ByteArray): Boolean {
        if (data.size < AONFormat.MAGIC_SIZE) return false
        for (i in AONFormat.MAGIC.indices) {
            if (data[i] != AONFormat.MAGIC[i]) return false
        }
        return true
    }

    actual fun parseHeader(data: ByteArray): AONHeader = AONHeader.parse(data)

    // ─── Internal ────────────────────────────────────────────

    private suspend fun verifyHmac(
        headerBytes: ByteArray,
        onnxData: ByteArray,
        storedSignature: ByteArray
    ): Boolean {
        val hmacKey = AONFormat.getDefaultHmacKey()
        val onnxHash = CryptoDigest.sha256(onnxData)

        val headerForSigning = headerBytes.copyOf()
        for (i in AONFormat.OFF_SIGNATURE until AONFormat.OFF_SIGNATURE + AONFormat.SIGNATURE_SIZE) {
            headerForSigning[i] = 0
        }

        val signInput = headerForSigning + onnxHash
        val computed = CryptoDigest.hmacSha256(hmacKey, signInput)
        val expected = computed + computed // 64 bytes
        return AONFormat.constantTimeEquals(expected, storedSignature)
    }

    private suspend fun verifyIdentity(header: AONHeader, appIdentifier: String?): Boolean {
        if (!header.hasPackageRestrictions()) return true

        val identity = appIdentifier ?: PlatformIdentity.getAppIdentifier()
        val identityHash = CryptoDigest.md5(identity.encodeToByteArray())

        return header.allowedPackages.any { AONFormat.constantTimeEquals(it, identityHash) }
    }
}
