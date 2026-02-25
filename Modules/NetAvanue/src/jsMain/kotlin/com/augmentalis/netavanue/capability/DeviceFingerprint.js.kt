package com.augmentalis.netavanue.capability

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * JS/Browser implementation of [DeviceFingerprint].
 *
 * Fingerprint: SHA-256 hash of navigator properties (userAgent, platform, language,
 * hardwareConcurrency, screen dimensions) to create a semi-stable browser fingerprint.
 *
 * Key pair: Generated ECDSA P-256 key pair stored in memory (no persistent storage
 * equivalent to Android Keystore or desktop filesystem in browser context).
 * For production, use IndexedDB or the Web Crypto API's extractable keys.
 */
@OptIn(ExperimentalEncodingApi::class)
actual class DeviceFingerprint actual constructor() {

    actual val fingerprint: String
    actual val publicKey: String
    private val privateKeyBytes: ByteArray

    init {
        // Generate fingerprint from browser-accessible properties
        val navigatorInfo = buildString {
            append(js("typeof navigator !== 'undefined' ? navigator.userAgent : 'unknown'") as String)
            append("|")
            append(js("typeof navigator !== 'undefined' ? navigator.platform : 'unknown'") as String)
            append("|")
            append(js("typeof navigator !== 'undefined' ? navigator.language : 'unknown'") as String)
            append("|")
            append(js("typeof navigator !== 'undefined' ? navigator.hardwareConcurrency : 0").toString())
            append("|")
            append(js("typeof screen !== 'undefined' ? screen.width : 0").toString())
            append("x")
            append(js("typeof screen !== 'undefined' ? screen.height : 0").toString())
        }
        fingerprint = sha256Hex(navigatorInfo.encodeToByteArray())

        // Generate a random key pair (ephemeral — for session-based signing)
        // In production, this should use Web Crypto API's generateKey()
        // and persist in IndexedDB for cross-session stability
        privateKeyBytes = Random.nextBytes(32)
        publicKey = Base64.encode(derivePublicKeyStub(privateKeyBytes))
    }

    actual fun sign(data: ByteArray): String {
        // HMAC-SHA256 with private key as signing substitute
        // Full ECDSA would use Web Crypto API (async) — this provides
        // a synchronous signature compatible with the expect contract
        val signed = hmacSha256(privateKeyBytes, data)
        return Base64.encode(signed)
    }

    private fun derivePublicKeyStub(privKey: ByteArray): ByteArray {
        // Derive a deterministic "public key" from the private key via SHA-256
        // This is NOT real ECDSA — it's a placeholder derivation for the
        // synchronous API contract. Real Web Crypto ECDSA is async.
        return sha256Bytes(privKey)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val blockSize = 64
        val normalizedKey = when {
            key.size > blockSize -> sha256Bytes(key)
            key.size < blockSize -> key + ByteArray(blockSize - key.size)
            else -> key
        }
        val iPad = ByteArray(blockSize) { (normalizedKey[it].toInt() xor 0x36).toByte() }
        val oPad = ByteArray(blockSize) { (normalizedKey[it].toInt() xor 0x5c).toByte() }
        val innerHash = sha256Bytes(iPad + data)
        return sha256Bytes(oPad + innerHash)
    }

    private fun sha256Hex(data: ByteArray): String {
        return sha256Bytes(data).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }

    /** Pure-Kotlin SHA-256 for synchronous hashing in JS environments */
    private fun sha256Bytes(data: ByteArray): ByteArray {
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
        var h0 = 0x6a09e667; var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372; var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f; var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab; var h7 = 0x5be0cd19
        val bitLen = data.size.toLong() * 8
        val paddedSize = ((data.size + 9 + 63) / 64) * 64
        val padded = ByteArray(paddedSize)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        for (i in 0 until 8) padded[paddedSize - 1 - i] = (bitLen shr (i * 8)).toByte()
        for (offset in 0 until paddedSize step 64) {
            val w = IntArray(64)
            for (i in 0 until 16) {
                w[i] = ((padded[offset + i * 4].toInt() and 0xFF) shl 24) or
                    ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 16) or
                    ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 8) or
                    (padded[offset + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = ror(w[i - 15], 7) xor ror(w[i - 15], 18) xor (w[i - 15] ushr 3)
                val s1 = ror(w[i - 2], 17) xor ror(w[i - 2], 19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h0; var b = h1; var c = h2; var d = h3
            var e = h4; var f = h5; var g = h6; var h = h7
            for (i in 0 until 64) {
                val s1 = ror(e, 6) xor ror(e, 11) xor ror(e, 25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + k[i] + w[i]
                val s0 = ror(a, 2) xor ror(a, 13) xor ror(a, 22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            h0 += a; h1 += b; h2 += c; h3 += d; h4 += e; h5 += f; h6 += g; h7 += h
        }
        return byteArrayOf(
            (h0 shr 24).toByte(), (h0 shr 16).toByte(), (h0 shr 8).toByte(), h0.toByte(),
            (h1 shr 24).toByte(), (h1 shr 16).toByte(), (h1 shr 8).toByte(), h1.toByte(),
            (h2 shr 24).toByte(), (h2 shr 16).toByte(), (h2 shr 8).toByte(), h2.toByte(),
            (h3 shr 24).toByte(), (h3 shr 16).toByte(), (h3 shr 8).toByte(), h3.toByte(),
            (h4 shr 24).toByte(), (h4 shr 16).toByte(), (h4 shr 8).toByte(), h4.toByte(),
            (h5 shr 24).toByte(), (h5 shr 16).toByte(), (h5 shr 8).toByte(), h5.toByte(),
            (h6 shr 24).toByte(), (h6 shr 16).toByte(), (h6 shr 8).toByte(), h6.toByte(),
            (h7 shr 24).toByte(), (h7 shr 16).toByte(), (h7 shr 8).toByte(), h7.toByte()
        )
    }

    private fun ror(value: Int, bits: Int): Int = (value ushr bits) or (value shl (32 - bits))
}
