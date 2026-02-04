/**
 * Deprecated type aliases for backward compatibility
 *
 * These utilities have been moved to the Shared Foundation module.
 * Import from com.augmentalis.foundation.util.*
 *
 * This file will be removed in a future version.
 */
package com.augmentalis.voiceoscore.util

@Deprecated(
    message = "Use com.augmentalis.foundation.util.NumberSystem",
    replaceWith = ReplaceWith("com.augmentalis.foundation.util.NumberSystem")
)
typealias NumberSystem = com.augmentalis.foundation.util.NumberSystem

@Deprecated(
    message = "Use com.augmentalis.foundation.util.NumberToWords",
    replaceWith = ReplaceWith("com.augmentalis.foundation.util.NumberToWords")
)
val NumberToWords = com.augmentalis.foundation.util.NumberToWords
