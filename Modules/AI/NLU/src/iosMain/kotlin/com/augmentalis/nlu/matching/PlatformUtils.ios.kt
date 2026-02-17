/**
 * Platform-specific utilities for iOS.
 */

package com.augmentalis.nlu.matching

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.timeIntervalSince1970

/**
 * Unicode NFKC normalization (iOS).
 */
internal actual fun normalizeUnicode(text: String): String {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val nsString = text as NSString
    return nsString.precomposedStringWithCompatibilityMapping
}

/**
 * Strip diacritical marks (iOS).
 * NFD decomposition + remove combining marks.
 */
internal actual fun stripDiacritics(text: String): String {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val nsString = text as NSString
    val decomposed = nsString.decomposedStringWithCanonicalMapping
    // Remove combining diacritical marks (Unicode block 0300-036F)
    return decomposed.replace(Regex("[\\u0300-\\u036F]"), "")
}

/**
 * Current time in milliseconds (iOS).
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
