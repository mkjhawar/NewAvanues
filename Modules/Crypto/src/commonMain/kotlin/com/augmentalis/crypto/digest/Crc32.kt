/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package com.augmentalis.crypto.digest

/**
 * Pure Kotlin CRC32 (IEEE 802.3 polynomial).
 *
 * Shared implementation used by darwinMain and jsMain CryptoDigest.
 * JVM uses java.util.zip.CRC32 directly (native, faster).
 */
internal object Crc32 {

    private val TABLE = UIntArray(256) { i ->
        var crc = i.toUInt()
        repeat(8) {
            crc = if (crc and 1u != 0u) (crc shr 1) xor 0xEDB88320u else crc shr 1
        }
        crc
    }

    fun compute(data: ByteArray): Int {
        var crc = 0xFFFFFFFF.toUInt()
        crc = update(crc, data)
        return (crc xor 0xFFFFFFFF.toUInt()).toInt()
    }

    fun compute(vararg chunks: ByteArray): Int {
        var crc = 0xFFFFFFFF.toUInt()
        for (chunk in chunks) {
            crc = update(crc, chunk)
        }
        return (crc xor 0xFFFFFFFF.toUInt()).toInt()
    }

    private fun update(initial: UInt, data: ByteArray): UInt {
        var crc = initial
        for (byte in data) {
            val index = ((crc xor byte.toUInt()) and 0xFFu).toInt()
            crc = (crc shr 8) xor TABLE[index]
        }
        return crc
    }
}
