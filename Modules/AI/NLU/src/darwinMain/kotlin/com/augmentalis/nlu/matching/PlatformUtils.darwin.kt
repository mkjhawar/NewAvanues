/**
 * Platform-specific utilities for Darwin (iOS + macOS).
 *
 * Uses Foundation APIs shared across all Apple platforms:
 * - NSString for Unicode normalization
 * - NSDate for time measurement
 */

package com.augmentalis.nlu.matching

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.timeIntervalSince1970

/**
 * Unicode NFKC normalization via Foundation.
 */
internal actual fun normalizeUnicode(text: String): String {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val nsString = text as NSString
    return nsString.precomposedStringWithCompatibilityMapping
}

/**
 * Strip diacritical marks via Foundation.
 * NFD decomposition + remove combining marks (Unicode block 0300-036F).
 */
internal actual fun stripDiacritics(text: String): String {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val nsString = text as NSString
    val decomposed = nsString.decomposedStringWithCanonicalMapping
    return decomposed.replace(Regex("[\\u0300-\\u036F]"), "")
}

/**
 * Current time in milliseconds via Foundation NSDate.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
