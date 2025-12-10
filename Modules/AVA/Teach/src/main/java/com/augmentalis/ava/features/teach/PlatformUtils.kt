package com.augmentalis.ava.features.teach

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Platform utility functions for Teach-Ava module
 * (Converted from KMP expect/actual to Android-only)
 */

/**
 * Calculate MD5 hash of a string
 */
fun calculateMD5Hash(input: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

/**
 * Format timestamp as date string
 */
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
