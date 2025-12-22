/**
 * HashUtils.kt - VoiceOS utility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Utility for generating consistent hashes for apps and UI elements
 */
package com.augmentalis.voiceoscore.utils

import java.security.MessageDigest

/**
 * Utility object for hash calculations
 */
object HashUtils {
    /**
     * Calculate a hash for an app based on package name and version
     *
     * @param packageName The app's package name
     * @param versionCode The app's version code
     * @return A hash string representing this app version
     */
    fun calculateAppHash(packageName: String, versionCode: Long): String {
        val input = "$packageName:$versionCode"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.substring(0, 16)
    }

    /**
     * Calculate a hash for a UI element based on its properties
     *
     * @param viewId The view ID resource name
     * @param contentDescription The content description
     * @param text The text content
     * @return A hash string representing this element
     */
    fun calculateElementHash(
        viewId: String?,
        contentDescription: String?,
        text: String?
    ): String {
        val input = "${viewId ?: ""}:${contentDescription ?: ""}:${text ?: ""}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.substring(0, 16)
    }
}
