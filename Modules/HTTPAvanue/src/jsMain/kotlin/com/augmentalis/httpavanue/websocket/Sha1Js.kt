package com.augmentalis.httpavanue.websocket

/**
 * JS implementation of SHA-1 using pure Kotlin.
 *
 * Provides a synchronous SHA-1 hash that works in both browser and Node.js.
 * The WebCrypto API (SubtleCrypto) only offers async SHA-1, so we use a
 * pure-Kotlin implementation to match the synchronous expect signature.
 *
 * Used by [WebSocketHandshake] to compute the Sec-WebSocket-Accept header.
 */
actual fun sha1(data: ByteArray): ByteArray {
    // Pre-processing: pad to 512-bit blocks
    val bitLen = data.size.toLong() * 8
    val paddedSize = ((data.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedSize)
    data.copyInto(padded)
    padded[data.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedSize - 1 - i] = (bitLen shr (i * 8)).toByte()
    }

    // Initialize hash values
    var h0 = 0x67452301
    var h1 = 0xEFCDAB89.toInt()
    var h2 = 0x98BADCFE.toInt()
    var h3 = 0x10325476
    var h4 = 0xC3D2E1F0.toInt()

    // Process each 512-bit block
    for (offset in 0 until paddedSize step 64) {
        val w = IntArray(80)
        for (i in 0 until 16) {
            w[i] = ((padded[offset + i * 4].toInt() and 0xFF) shl 24) or
                ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 16) or
                ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 8) or
                (padded[offset + i * 4 + 3].toInt() and 0xFF)
        }
        for (i in 16 until 80) {
            w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
        }

        var a = h0; var b = h1; var c = h2; var d = h3; var e = h4

        for (i in 0 until 80) {
            val (f, k) = when (i) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }
            val temp = a.rotateLeft(5) + f + e + k + w[i]
            e = d; d = c; c = b.rotateLeft(30); b = a; a = temp
        }

        h0 += a; h1 += b; h2 += c; h3 += d; h4 += e
    }

    return byteArrayOf(
        (h0 shr 24).toByte(), (h0 shr 16).toByte(), (h0 shr 8).toByte(), h0.toByte(),
        (h1 shr 24).toByte(), (h1 shr 16).toByte(), (h1 shr 8).toByte(), h1.toByte(),
        (h2 shr 24).toByte(), (h2 shr 16).toByte(), (h2 shr 8).toByte(), h2.toByte(),
        (h3 shr 24).toByte(), (h3 shr 16).toByte(), (h3 shr 8).toByte(), h3.toByte(),
        (h4 shr 24).toByte(), (h4 shr 16).toByte(), (h4 shr 8).toByte(), h4.toByte()
    )
}

private fun Int.rotateLeft(bits: Int): Int = (this shl bits) or (this ushr (32 - bits))
