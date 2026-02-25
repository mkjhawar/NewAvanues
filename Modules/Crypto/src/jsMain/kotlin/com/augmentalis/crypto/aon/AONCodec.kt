/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.aon

import com.augmentalis.crypto.JsBufferUtils
import com.augmentalis.crypto.digest.CryptoDigest
import com.augmentalis.crypto.identity.PlatformIdentity
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import kotlin.js.Date
import kotlin.js.Promise

/**
 * JS implementation of AONCodec (browser + Node.js).
 *
 * Performs the same 8-step verification pipeline as JVM/Darwin:
 * 1. Magic bytes → 2. Format version → 3. HMAC-SHA256 →
 * 4. SHA-256 integrity → 5. CRC32 → 6. Expiry → 7. Identity → 8. Decrypt
 *
 * AES-256-GCM decryption uses:
 * - Browser: crypto.subtle.decrypt (async, non-extractable key)
 * - Node.js: crypto.createDecipheriv (synchronous)
 */
actual object AONCodec {

    private val isNodeJs: Boolean get() = JsBufferUtils.isNodeJs

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
                val now = (Date().getTime() / 1000).toLong()
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
        val currentTime = (Date().getTime() / 1000).toLong()

        val iv: ByteArray
        val payload: ByteArray

        if (config.encrypt) {
            iv = generateSecureIv()
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
     * Same algorithm as JVM: zeros signature field in header copy,
     * then computes HMAC-SHA256(key, headerWithZeroedSig + SHA-256(onnxData)).
     * The stored signature is 64 bytes (HMAC doubled: 32 + 32).
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
     * Decrypt AES-256-GCM encrypted ONNX data.
     *
     * Browser: Uses crypto.subtle with non-extractable key.
     * Node.js: Uses crypto.createDecipheriv (synchronous).
     */
    private suspend fun decryptAesGcm(encryptedData: ByteArray, ivNonce: ByteArray): ByteArray {
        return if (isNodeJs) {
            decryptAesGcmNode(encryptedData, ivNonce)
        } else {
            decryptAesGcmBrowser(encryptedData, ivNonce)
        }
    }

    private fun decryptAesGcmNode(encryptedData: ByteArray, ivNonce: ByteArray): ByteArray {
        val crypto = js("require('crypto')")
        val keyBytes = sha256SyncNode(AONFormat.getDefaultHmacKey())
        val iv = ivNonce.copyOf(12)

        // GCM auth tag is the last 16 bytes of ciphertext
        val tagLength = 16
        if (encryptedData.size < tagLength) {
            throw AONSecurityException("Encrypted data too small for GCM auth tag")
        }
        val ciphertext = encryptedData.copyOfRange(0, encryptedData.size - tagLength)
        val authTag = encryptedData.copyOfRange(encryptedData.size - tagLength, encryptedData.size)

        val decipher = crypto.createDecipheriv(
            "aes-256-gcm",
            JsBufferUtils.toNodeBuffer(keyBytes),
            JsBufferUtils.toNodeBuffer(iv)
        )
        decipher.setAuthTag(JsBufferUtils.toNodeBuffer(authTag))

        val decrypted1 = decipher.update(JsBufferUtils.toNodeBuffer(ciphertext))
        val decrypted2 = decipher.final()

        // Concatenate result buffers
        val buffer = js("Buffer")
        val result = buffer.concat(js("[ decrypted1, decrypted2 ]"))
        return JsBufferUtils.fromNodeBuffer(result)
    }

    private suspend fun decryptAesGcmBrowser(encryptedData: ByteArray, ivNonce: ByteArray): ByteArray {
        val subtle = js("crypto.subtle")
        val keyData = sha256SyncPure(AONFormat.getDefaultHmacKey())
        val iv = ivNonce.copyOf(12)

        // Import AES-GCM key (non-extractable)
        val keyBuffer = JsBufferUtils.toArrayBuffer(keyData)
        val cryptoKey: dynamic = (subtle.importKey(
            "raw",
            keyBuffer,
            js("({name: 'AES-GCM'})"),
            false,
            js("['decrypt']")
        ) as Promise<dynamic>).await()

        val dataBuffer = JsBufferUtils.toArrayBuffer(encryptedData)
        val ivBuffer = JsBufferUtils.toArrayBuffer(iv)

        val result: ArrayBuffer = (subtle.decrypt(
            js("({name: 'AES-GCM', iv: ivBuffer})"),
            cryptoKey,
            dataBuffer
        ) as Promise<ArrayBuffer>).await()

        return JsBufferUtils.fromArrayBuffer(result)
    }

    // ─── Encryption ───────────────────────────────────────────

    /**
     * Generate a 16-byte cryptographically secure IV.
     * Node.js: crypto.randomBytes. Browser: crypto.getRandomValues.
     */
    private fun generateSecureIv(): ByteArray {
        return if (isNodeJs) {
            val crypto = js("require('crypto')")
            val buf = crypto.randomBytes(16)
            JsBufferUtils.fromNodeBuffer(buf)
        } else {
            val arr = js("new Uint8Array(16)")
            js("crypto.getRandomValues(arr)")
            ByteArray(16) { i -> (arr[i] as Number).toByte() }
        }
    }

    /**
     * Encrypt ONNX data with AES-256-GCM.
     */
    private suspend fun encryptAesGcm(data: ByteArray, ivNonce: ByteArray): ByteArray {
        return if (isNodeJs) {
            encryptAesGcmNode(data, ivNonce)
        } else {
            encryptAesGcmBrowser(data, ivNonce)
        }
    }

    private fun encryptAesGcmNode(data: ByteArray, ivNonce: ByteArray): ByteArray {
        val crypto = js("require('crypto')")
        val keyBytes = sha256SyncNode(AONFormat.getDefaultHmacKey())
        val iv = ivNonce.copyOf(12)

        val cipher = crypto.createCipheriv(
            "aes-256-gcm",
            JsBufferUtils.toNodeBuffer(keyBytes),
            JsBufferUtils.toNodeBuffer(iv)
        )

        val encrypted1 = cipher.update(JsBufferUtils.toNodeBuffer(data))
        val encrypted2 = cipher.final()
        val authTag = cipher.getAuthTag()

        // Concatenate: ciphertext + auth tag (matches JVM GCM output)
        val buffer = js("Buffer")
        val result = buffer.concat(js("[ encrypted1, encrypted2, authTag ]"))
        return JsBufferUtils.fromNodeBuffer(result)
    }

    private suspend fun encryptAesGcmBrowser(data: ByteArray, ivNonce: ByteArray): ByteArray {
        val subtle = js("crypto.subtle")
        val keyData = sha256SyncPure(AONFormat.getDefaultHmacKey())
        val iv = ivNonce.copyOf(12)

        // Import AES-GCM key (non-extractable)
        val keyBuffer = JsBufferUtils.toArrayBuffer(keyData)
        val cryptoKey: dynamic = (subtle.importKey(
            "raw",
            keyBuffer,
            js("({name: 'AES-GCM'})"),
            false,
            js("['encrypt']")
        ) as Promise<dynamic>).await()

        val dataBuffer = JsBufferUtils.toArrayBuffer(data)
        val ivBuffer = JsBufferUtils.toArrayBuffer(iv)

        val result: ArrayBuffer = (subtle.encrypt(
            js("({name: 'AES-GCM', iv: ivBuffer})"),
            cryptoKey,
            dataBuffer
        ) as Promise<ArrayBuffer>).await()

        return JsBufferUtils.fromArrayBuffer(result)
    }

    // ─── Key Management ───────────────────────────────────────

    /**
     * Get the HMAC key with JS-specific override support.
     */
    private fun getHmacKey(): ByteArray {
        if (isNodeJs) {
            // Node.js: check environment variable
            val envKey: dynamic = js("typeof process !== 'undefined' && process.env ? process.env.AON_HMAC_KEY : undefined")
            if (envKey != null && envKey != undefined) {
                return (envKey as String).encodeToByteArray()
            }
        }
        // Fallback: obfuscated default from AONFormat
        return AONFormat.getDefaultHmacKey()
    }

    // ─── Sync SHA-256 (for AES key derivation) ────────────────

    /**
     * Synchronous SHA-256 for Node.js (used only for AES key derivation).
     */
    private fun sha256SyncNode(data: ByteArray): ByteArray {
        val crypto = js("require('crypto')")
        val hash = crypto.createHash("sha256")
        hash.update(JsBufferUtils.toNodeBuffer(data))
        val result = hash.digest()
        return JsBufferUtils.fromNodeBuffer(result)
    }

    /**
     * Pure Kotlin SHA-256 for browser sync use (AES key derivation).
     * This avoids needing to make decryptAesGcmBrowser doubly-async.
     *
     * Actually, crypto.subtle.digest IS async, so for browser we derive
     * the key via crypto.subtle in the suspend decrypt function.
     * This function is only used as fallback.
     */
    private fun sha256SyncPure(data: ByteArray): ByteArray {
        // For browser, we compute SHA-256 inline using crypto.subtle
        // This is called from a suspend context, so we can await
        // But since we need it synchronously for importKey, we use
        // a pure implementation. Fortunately the key is small (48 bytes).
        return sha256Pure(data)
    }

    /**
     * Pure Kotlin SHA-256 implementation.
     * Used only for AES key derivation from the HMAC key (48 bytes).
     */
    private fun sha256Pure(data: ByteArray): ByteArray {
        val k = intArrayOf(
            0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
            0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
            0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
            0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
            0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
            0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
            0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
            0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
        )

        // Padding
        val messageLenBits = data.size.toLong() * 8
        val paddingLen = ((56 - (data.size + 1) % 64 + 64) % 64) + 1
        val padded = ByteArray(data.size + paddingLen + 8)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        // Big-endian length (SHA-256 uses big-endian, unlike MD5)
        for (i in 0 until 8) {
            padded[padded.size - 8 + i] = (messageLenBits ushr ((7 - i) * 8)).toByte()
        }

        var h0 = 0x6a09e667
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19

        for (offset in padded.indices step 64) {
            val w = IntArray(64)
            for (i in 0 until 16) {
                w[i] = ((padded[offset + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[offset + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0; var b = h1; var c = h2; var d = h3
            var e = h4; var f = h5; var g = h6; var h = h7

            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + k[i] + w[i]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }

            h0 += a; h1 += b; h2 += c; h3 += d
            h4 += e; h5 += f; h6 += g; h7 += h
        }

        val result = ByteArray(32)
        intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7).forEachIndexed { idx, v ->
            result[idx * 4] = (v shr 24).toByte()
            result[idx * 4 + 1] = (v shr 16).toByte()
            result[idx * 4 + 2] = (v shr 8).toByte()
            result[idx * 4 + 3] = v.toByte()
        }
        return result
    }

    // Kotlin/JS doesn't have Int.rotateRight, so we implement it
    private fun Int.rotateRight(n: Int): Int = (this ushr n) or (this shl (32 - n))

}
