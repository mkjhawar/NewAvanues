/**
 * Platform-specific utilities for JS/Web.
 */

package com.augmentalis.nlu.matching

/**
 * Unicode NFKC normalization (JS).
 */
internal actual fun normalizeUnicode(text: String): String {
    return text.asDynamic().normalize("NFKC") as String
}

/**
 * Strip diacritical marks (JS).
 * NFD decomposition + remove combining marks.
 */
internal actual fun stripDiacritics(text: String): String {
    val normalized = text.asDynamic().normalize("NFD") as String
    // Remove combining diacritical marks
    return normalized.replace(Regex("[\\u0300-\\u036F]"), "")
}

/**
 * Current time in milliseconds (JS).
 */
internal actual fun currentTimeMillis(): Long {
    return kotlin.js.Date().getTime().toLong()
}
