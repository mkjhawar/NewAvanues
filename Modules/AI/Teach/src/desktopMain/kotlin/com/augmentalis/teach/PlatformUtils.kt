/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.teach

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Platform utility functions for Teach-Ava module (Desktop JVM implementation)
 *
 * Uses standard Java APIs which work on all JVM desktop platforms
 * (macOS, Windows, Linux).
 */

/**
 * Calculate MD5 hash of a string
 *
 * Uses Java's MessageDigest for MD5 hashing.
 *
 * @param input The string to hash
 * @return Hexadecimal MD5 hash string
 */
actual fun calculateMD5Hash(input: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

/**
 * Format timestamp as date string
 *
 * Uses Java's SimpleDateFormat for date formatting.
 *
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted date string (e.g., "Jan 15, 2025")
 */
actual fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
