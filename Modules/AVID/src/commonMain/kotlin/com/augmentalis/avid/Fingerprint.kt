/**
 * Fingerprint.kt - Deterministic hash generation for UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-13
 */
package com.augmentalis.avid

import kotlin.random.Random

/**
 * Deterministic fingerprint generation for UI elements
 *
 * Generates stable hashes so the same UI element always gets the same identifier.
 */
object Fingerprint {

    private const val HEX_CHARS = "0123456789abcdef"

    /**
     * Generate deterministic hash for an app
     *
     * @param packageName App package name
     * @param appName App display name
     * @return 12-character hex hash
     */
    fun forApp(packageName: String, appName: String): String {
        val input = "$packageName|$appName"
        return deterministicHash(input, 12)
    }

    /**
     * Generate deterministic hash for a UI element
     *
     * @param type Element type (button, input, etc.)
     * @param resourceId Android resource ID (if available)
     * @param name Element name or text
     * @param contentDesc Accessibility content description
     * @return 8-character hex hash
     */
    fun forElement(
        type: String,
        resourceId: String? = null,
        name: String? = null,
        contentDesc: String? = null
    ): String {
        val input = buildString {
            append(type)
            resourceId?.let { append("|$it") }
            name?.let { append("|$it") }
            contentDesc?.let { append("|$it") }
        }
        return deterministicHash(input, 8)
    }

    /**
     * Generate deterministic hash for a screen
     *
     * @param packageName App package name
     * @param activityName Activity class name
     * @return 8-character hex hash
     */
    fun forScreen(packageName: String, activityName: String): String {
        val input = "$packageName|$activityName"
        return deterministicHash(input, 8)
    }

    /**
     * Generate deterministic hash from any input string
     *
     * Uses a simple but consistent hashing algorithm for KMP compatibility.
     *
     * @param input String to hash
     * @param length Desired hash length
     * @return Hex hash string
     */
    fun deterministicHash(input: String, length: Int): String {
        var hash = 0L
        for (char in input) {
            hash = hash * 31 + char.code
        }

        // Ensure positive value and convert to hex
        val positiveHash = hash and 0x7FFFFFFFFFFFFFFFL
        val hexString = positiveHash.toString(16).padStart(16, '0')

        return hexString.take(length).lowercase()
    }

    /**
     * Generate random 8-character hex hash
     */
    fun randomHash8(): String {
        return buildString {
            repeat(8) {
                append(HEX_CHARS[Random.nextInt(16)])
            }
        }
    }

    /**
     * Generate random hash of specified length
     */
    fun randomHash(length: Int): String {
        return buildString {
            repeat(length) {
                append(HEX_CHARS[Random.nextInt(16)])
            }
        }
    }
}
