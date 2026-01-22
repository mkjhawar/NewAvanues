/**
 * Platform-specific utilities for Desktop (JVM).
 */

package com.augmentalis.nlu.matching

import java.text.Normalizer

/**
 * Unicode NFKC normalization (Desktop/JVM).
 */
internal actual fun normalizeUnicode(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFKC)
}

/**
 * Strip diacritical marks (Desktop/JVM).
 */
internal actual fun stripDiacritics(text: String): String {
    val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
    return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

/**
 * Current time in milliseconds (Desktop/JVM).
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
