/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package com.augmentalis.crypto.digest

import com.augmentalis.crypto.native.cc.CC_MD5
import com.augmentalis.crypto.native.cc.CC_MD5_DIGEST_LENGTH
import com.augmentalis.crypto.native.cc.CC_SHA256
import com.augmentalis.crypto.native.cc.CC_SHA256_DIGEST_LENGTH
import com.augmentalis.crypto.native.cc.CCHmac
import com.augmentalis.crypto.native.cc.kCCHmacAlgSHA256
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.UByteVar
import platform.posix.uint8_tVar

/**
 * iOS/macOS implementation of CryptoDigest.
 * Uses CommonCrypto framework via cinterop.
 */
actual object CryptoDigest {

    actual suspend fun sha256(data: ByteArray): ByteArray = memScoped {
        val output = allocArray<uint8_tVar>(CC_SHA256_DIGEST_LENGTH.toInt())
        data.usePinned { pinned ->
            CC_SHA256(pinned.addressOf(0), data.size.toUInt(), output)
        }
        output.readBytes(CC_SHA256_DIGEST_LENGTH.toInt())
    }

    actual suspend fun sha256Truncated16(data: ByteArray): ByteArray {
        return sha256(data).copyOf(16)
    }

    actual suspend fun md5(data: ByteArray): ByteArray = memScoped {
        val output = allocArray<uint8_tVar>(CC_MD5_DIGEST_LENGTH.toInt())
        data.usePinned { pinned ->
            CC_MD5(pinned.addressOf(0), data.size.toUInt(), output)
        }
        output.readBytes(CC_MD5_DIGEST_LENGTH.toInt())
    }

    actual suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = memScoped {
        val output = allocArray<uint8_tVar>(CC_SHA256_DIGEST_LENGTH.toInt())
        key.usePinned { keyPinned ->
            data.usePinned { dataPinned ->
                CCHmac(
                    kCCHmacAlgSHA256,
                    keyPinned.addressOf(0),
                    key.size.toULong(),
                    dataPinned.addressOf(0),
                    data.size.toULong(),
                    output
                )
            }
        }
        output.readBytes(CC_SHA256_DIGEST_LENGTH.toInt())
    }

    actual fun crc32(data: ByteArray): Int {
        return computeCrc32(data)
    }

    actual fun crc32(vararg chunks: ByteArray): Int {
        var crc = 0xFFFFFFFF.toUInt()
        for (chunk in chunks) {
            crc = updateCrc32(crc, chunk)
        }
        return (crc xor 0xFFFFFFFF.toUInt()).toInt()
    }

    // CRC32 lookup table (IEEE 802.3 polynomial)
    private val CRC32_TABLE = UIntArray(256) { i ->
        var crc = i.toUInt()
        repeat(8) {
            crc = if (crc and 1u != 0u) {
                (crc shr 1) xor 0xEDB88320u
            } else {
                crc shr 1
            }
        }
        crc
    }

    private fun computeCrc32(data: ByteArray): Int {
        var crc = 0xFFFFFFFF.toUInt()
        crc = updateCrc32(crc, data)
        return (crc xor 0xFFFFFFFF.toUInt()).toInt()
    }

    private fun updateCrc32(initial: UInt, data: ByteArray): UInt {
        var crc = initial
        for (byte in data) {
            val index = ((crc xor byte.toUInt()) and 0xFFu).toInt()
            crc = (crc shr 8) xor CRC32_TABLE[index]
        }
        return crc
    }
}
