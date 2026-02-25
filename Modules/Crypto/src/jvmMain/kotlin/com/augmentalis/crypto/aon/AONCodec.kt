/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

import com.augmentalis.crypto.digest.CryptoDigest
import com.augmentalis.crypto.identity.PlatformIdentity
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM implementation of AONCodec (shared by Android + Desktop).
 *
 * Uses javax.crypto for HMAC-SHA256, AES-256-GCM, and java.security
 * for SHA-256/MD5. CRC32 via java.util.zip.
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
            // Parse header
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
                errors.add("File truncated: expected ${payloadEnd + AONFormat.FOOTER_SIZE}, got ${aonData.size}")
                return AONVerifyResult(false, false, false, false, false, modelId, licenseTier, errors)
            }

            val onnxData = aonData.copyOfRange(AONFormat.HEADER_SIZE, payloadEnd)
            val footerBytes = aonData.copyOfRange(payloadEnd, payloadEnd + AONFormat.FOOTER_SIZE)
            val footer = AONFooter.parse(footerBytes)

            // 3. HMAC-SHA256 verification
            hmacValid = verifyHmac(headerBytes, onnxData, header.signature)
            if (!hmacValid) errors.add("HMAC signature mismatch")

            // 4. SHA-256 integrity
            val onnxHash = CryptoDigest.sha256(onnxData)
            val truncatedHash = onnxHash.copyOf(16)

            val headerIntegrity = truncatedHash.contentEquals(header.onnxSHA256Truncated)
            val footerOnnxIntegrity = onnxHash.contentEquals(footer.onnxHash)
            val headerHashValid = CryptoDigest.sha256(headerBytes).contentEquals(footer.headerHash)
            val footerMagicValid = footer.hasValidMagic()

            // 5. CRC32 integrity
            val expectedCrc = CryptoDigest.crc32(headerBytes, onnxData)
            val crcValid = expectedCrc == footer.checksumCRC32

            integrityValid = headerIntegrity && footerOnnxIntegrity && headerHashValid && footerMagicValid && crcValid
            if (!headerIntegrity) errors.add("ONNX SHA-256 truncated hash mismatch in header")
            if (!footerOnnxIntegrity) errors.add("ONNX SHA-256 full hash mismatch in footer")
            if (!headerHashValid) errors.add("Header SHA-256 hash mismatch in footer")
            if (!footerMagicValid) errors.add("Invalid footer magic bytes")
            if (!crcValid) errors.add("CRC32 checksum mismatch")

            // 6. Expiry check
            if (header.expiryTimestamp > 0) {
                val now = System.currentTimeMillis() / 1000
                expired = now > header.expiryTimestamp
                if (expired) errors.add("AON file expired at ${header.expiryTimestamp}")
            }

            // 7. Identity check
            identityValid = verifyIdentity(header, appIdentifier)
            if (!identityValid) errors.add("Package/identity not authorized")

        } catch (e: Exception) {
            errors.add("Verification error: ${e.message}")
        }

        val valid = hmacValid && integrityValid && identityValid && !expired && errors.isEmpty()
        return AONVerifyResult(valid, hmacValid, integrityValid, identityValid, expired, modelId, licenseTier, errors)
    }

    actual suspend fun unwrap(aonData: ByteArray, appIdentifier: String?): ByteArray {
        // Quick magic check
        if (!isAON(aonData)) {
            // Not an AON file — return as-is (raw ONNX backward compatibility)
            return aonData
        }

        val result = verify(aonData, appIdentifier)
        if (!result.valid) {
            throw AONSecurityException(
                "AON verification failed: ${result.errors.joinToString("; ")}"
            )
        }

        // Extract payload (safe Long→Int conversion)
        val header = parseHeader(aonData)
        val onnxSize = AONFormat.safeOnnxDataSize(header.onnxDataSize)
        val payloadEnd = AONFormat.HEADER_SIZE + onnxSize
        val onnxData = aonData.copyOfRange(AONFormat.HEADER_SIZE, payloadEnd)

        // Decrypt if encrypted
        return if (header.encryptionScheme == AONFormat.ENCRYPTION_AES_256_GCM) {
            decryptAesGcm(onnxData, header.ivNonce)
        } else {
            onnxData
        }
    }

    actual suspend fun wrap(onnxData: ByteArray, config: AONWrapConfig): ByteArray {
        val hmacKey = getHmacKey()
        val currentTime = System.currentTimeMillis() / 1000

        val iv: ByteArray
        val payload: ByteArray

        if (config.encrypt) {
            iv = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            payload = encryptAesGcm(onnxData, iv)
        } else {
            iv = ByteArray(16)
            payload = onnxData
        }

        return AONWrapper.buildAonFile(payload, config, hmacKey, currentTime, iv)
    }

    actual fun isAON(data: ByteArray): Boolean {
        if (data.size < AONFormat.MAGIC_SIZE) return false
        for (i in AONFormat.MAGIC.indices) {
            if (data[i] != AONFormat.MAGIC[i]) return false
        }
        return true
    }

    actual fun parseHeader(data: ByteArray): AONHeader {
        return AONHeader.parse(data)
    }

    // ─── Internal ────────────────────────────────────────────

    /**
     * Verify HMAC-SHA256 signature.
     *
     * The signature covers: header (with signature field zeroed) + SHA-256(ONNX data).
     * This fixes the v1 bug where the existing Android AONFileManager couldn't
     * recompute HMAC because it didn't zero the signature field.
     */
    private suspend fun verifyHmac(
        headerBytes: ByteArray,
        onnxData: ByteArray,
        storedSignature: ByteArray
    ): Boolean {
        val hmacKey = getHmacKey()
        val onnxHash = CryptoDigest.sha256(onnxData)

        // Zero out the signature field in a copy of the header
        val headerForSigning = headerBytes.copyOf()
        for (i in AONFormat.OFF_SIGNATURE until AONFormat.OFF_SIGNATURE + AONFormat.SIGNATURE_SIZE) {
            headerForSigning[i] = 0
        }

        // Concatenate header + onnx hash for HMAC input
        val signInput = headerForSigning + onnxHash
        val computed = CryptoDigest.hmacSha256(hmacKey, signInput)

        // The stored signature is 64 bytes (HMAC-SHA256 doubled: 32 + 32)
        val expected = computed + computed
        return AONFormat.constantTimeEquals(expected, storedSignature)
    }

    /**
     * Verify package/identity authorization.
     */
    private suspend fun verifyIdentity(header: AONHeader, appIdentifier: String?): Boolean {
        if (!header.hasPackageRestrictions()) return true

        val identity = appIdentifier ?: PlatformIdentity.getAppIdentifier()
        val identityHash = CryptoDigest.md5(identity.encodeToByteArray())

        return header.allowedPackages.any { pkg ->
            AONFormat.constantTimeEquals(pkg, identityHash)
        }
    }

    /**
     * Encrypt ONNX data with AES-256-GCM.
     * Key: SHA-256(HMAC_KEY). IV: first 12 bytes of ivNonce. Auth tag: 128-bit (appended).
     */
    private fun encryptAesGcm(data: ByteArray, ivNonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keyBytes = CryptoDigest.sha256Blocking(AONFormat.getDefaultHmacKey())
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val iv = ivNonce.copyOf(12)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(data)
    }

    /**
     * Decrypt AES-256-GCM encrypted ONNX data.
     */
    private fun decryptAesGcm(encryptedData: ByteArray, ivNonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keyBytes = CryptoDigest.sha256Blocking(AONFormat.getDefaultHmacKey())
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val iv = ivNonce.copyOf(12) // GCM uses 12-byte IV
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encryptedData)
    }

    /**
     * Get the HMAC key with JVM-specific override support.
     */
    private fun getHmacKey(): ByteArray {
        // Priority 1: System property override
        System.getProperty("aon.hmac.key")?.let {
            return it.encodeToByteArray()
        }
        // Priority 2: Obfuscated default from AONFormat
        return AONFormat.getDefaultHmacKey()
    }
}

/**
 * Synchronous SHA-256 for use outside coroutine context (AES key derivation).
 */
internal fun CryptoDigest.sha256Blocking(data: ByteArray): ByteArray {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(data)
}
