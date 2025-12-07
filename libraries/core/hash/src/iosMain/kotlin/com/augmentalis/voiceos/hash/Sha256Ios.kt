/**
 * Sha256Ios.kt - iOS implementation of SHA-256
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-16
 *
 * Note: Uses a pure Kotlin implementation for maximum compatibility.
 * For production use, consider using platform-specific crypto libraries.
 */
package com.augmentalis.voiceos.hash

/**
 * iOS implementation of SHA-256 hashing
 *
 * Currently uses the JVM-compatible implementation via expect/actual.
 * iOS native crypto integration can be added later if needed.
 */
internal actual fun sha256Impl(input: String): String {
    // Pure Kotlin SHA-256 implementation
    // For now, we'll use a simple implementation
    // In production, this would use iOS CommonCrypto or a KMP crypto library
    return pureSha256(input)
}

/**
 * Pure Kotlin SHA-256 implementation
 *
 * This is a simplified implementation for cross-platform compatibility.
 * For production use, integrate with platform-specific crypto libraries.
 */
private fun pureSha256(input: String): String {
    val bytes = input.encodeToByteArray()
    val hash = sha256Pure(bytes)
    return hash.joinToString("") { byte ->
        val hex = (byte.toInt() and 0xFF).toString(16)
        if (hex.length == 1) "0$hex" else hex
    }
}

/**
 * Pure SHA-256 algorithm implementation
 *
 * Based on FIPS 180-4 specification.
 */
private fun sha256Pure(message: ByteArray): ByteArray {
    // SHA-256 constants
    val k = intArrayOf(
        0x428a2f98.toInt(), 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
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

    // Initial hash values
    var h0 = 0x6a09e667
    var h1 = 0xbb67ae85.toInt()
    var h2 = 0x3c6ef372
    var h3 = 0xa54ff53a.toInt()
    var h4 = 0x510e527f
    var h5 = 0x9b05688c.toInt()
    var h6 = 0x1f83d9ab
    var h7 = 0x5be0cd19

    // Pre-processing: padding
    val paddedMessage = padMessage(message)

    // Process message in 512-bit chunks
    for (chunkStart in paddedMessage.indices step 64) {
        val w = IntArray(64)

        // Copy chunk into first 16 words of message schedule
        for (i in 0 until 16) {
            w[i] = ((paddedMessage[chunkStart + i * 4].toInt() and 0xFF) shl 24) or
                    ((paddedMessage[chunkStart + i * 4 + 1].toInt() and 0xFF) shl 16) or
                    ((paddedMessage[chunkStart + i * 4 + 2].toInt() and 0xFF) shl 8) or
                    (paddedMessage[chunkStart + i * 4 + 3].toInt() and 0xFF)
        }

        // Extend the first 16 words into remaining 48 words
        for (i in 16 until 64) {
            val s0 = (w[i - 15] rotateRight 7) xor (w[i - 15] rotateRight 18) xor (w[i - 15] ushr 3)
            val s1 = (w[i - 2] rotateRight 17) xor (w[i - 2] rotateRight 19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        // Initialize working variables
        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7

        // Main loop
        for (i in 0 until 64) {
            val S1 = (e rotateRight 6) xor (e rotateRight 11) xor (e rotateRight 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + S1 + ch + k[i] + w[i]
            val S0 = (a rotateRight 2) xor (a rotateRight 13) xor (a rotateRight 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = S0 + maj

            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        // Add compressed chunk to current hash value
        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        h5 += f
        h6 += g
        h7 += h
    }

    // Produce final hash value
    return intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        .flatMap { it.toByteArray().toList() }
        .toByteArray()
}

private fun padMessage(message: ByteArray): ByteArray {
    val messageLength = message.size
    val bitLength = messageLength.toLong() * 8

    // Calculate padding length (message + 1 + zeros + 8 bytes for length)
    val paddingLength = (64 - ((messageLength + 9) % 64)) % 64
    val totalLength = messageLength + 1 + paddingLength + 8

    val padded = ByteArray(totalLength)

    // Copy original message
    message.copyInto(padded, 0, 0, messageLength)

    // Append bit '1' (0x80)
    padded[messageLength] = 0x80.toByte()

    // Append length as 64-bit big-endian
    for (i in 0 until 8) {
        padded[totalLength - 8 + i] = ((bitLength ushr (56 - i * 8)) and 0xFF).toByte()
    }

    return padded
}

private infix fun Int.rotateRight(bits: Int): Int {
    return (this ushr bits) or (this shl (32 - bits))
}

private fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        ((this ushr 24) and 0xFF).toByte(),
        ((this ushr 16) and 0xFF).toByte(),
        ((this ushr 8) and 0xFF).toByte(),
        (this and 0xFF).toByte()
    )
}
