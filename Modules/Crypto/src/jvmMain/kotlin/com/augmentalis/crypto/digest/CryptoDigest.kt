/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto.digest

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.zip.CRC32 as JavaCRC32

/**
 * JVM implementation of CryptoDigest (shared by Android + Desktop).
 * Uses java.security.MessageDigest, javax.crypto.Mac, and java.util.zip.CRC32.
 */
actual object CryptoDigest {

    actual suspend fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    actual suspend fun sha256Truncated16(data: ByteArray): ByteArray {
        return sha256(data).copyOf(16)
    }

    actual suspend fun md5(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(data)
    }

    actual suspend fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    actual fun crc32(data: ByteArray): Int {
        val crc = JavaCRC32()
        crc.update(data)
        return crc.value.toInt()
    }

    actual fun crc32(vararg chunks: ByteArray): Int {
        val crc = JavaCRC32()
        for (chunk in chunks) {
            crc.update(chunk)
        }
        return crc.value.toInt()
    }
}
