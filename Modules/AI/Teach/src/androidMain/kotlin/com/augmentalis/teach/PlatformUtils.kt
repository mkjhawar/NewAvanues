package com.augmentalis.teach
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Platform utility functions for Teach-Ava module (Android implementation)
 */

/**
 * Calculate MD5 hash of a string
 */
actual fun calculateMD5Hash(input: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

/**
 * Format timestamp as date string
 */
actual fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
