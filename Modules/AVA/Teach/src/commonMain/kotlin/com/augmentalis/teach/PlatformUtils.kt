package com.augmentalis.teach
/**
 * Platform utility functions for Teach-Ava module
 */

/**
 * Calculate MD5 hash of a string
 */
expect fun calculateMD5Hash(input: String): String

/**
 * Format timestamp as date string
 */
expect fun formatDate(timestamp: Long): String
