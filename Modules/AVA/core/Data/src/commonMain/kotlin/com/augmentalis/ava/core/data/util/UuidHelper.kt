package com.augmentalis.ava.core.data.util

import kotlin.random.Random

/**
 * Cross-platform UUID generator
 *
 * Generates RFC 4122 version 4 UUIDs (random-based).
 * This is a simple implementation for KMP compatibility.
 */
object UuidHelper {

    /**
     * Generate a random UUID string
     *
     * @return UUID string in format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     */
    fun randomUUID(): String {
        val random = Random.Default

        // Generate 16 random bytes
        val bytes = ByteArray(16) { random.nextInt(256).toByte() }

        // Set version to 4 (random UUID)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()

        // Set variant to RFC 4122
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        // Convert bytes to UUID string
        return buildString {
            for (i in bytes.indices) {
                if (i == 4 || i == 6 || i == 8 || i == 10) {
                    append('-')
                }
                val hex = (bytes[i].toInt() and 0xFF).toString(16).padStart(2, '0')
                append(hex)
            }
        }
    }
}
