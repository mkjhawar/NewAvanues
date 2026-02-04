/**
 * Deprecated type aliases for backward compatibility
 *
 * These utilities have been moved to the Shared Foundation module.
 * Import from com.augmentalis.shared.util.*
 *
 * This file will be removed in a future version.
 */
package com.augmentalis.voiceoscore.util

@Deprecated(
    message = "Use com.augmentalis.shared.util.NumberSystem",
    replaceWith = ReplaceWith("com.augmentalis.shared.util.NumberSystem")
)
typealias NumberSystem = com.augmentalis.shared.util.NumberSystem

@Deprecated(
    message = "Use com.augmentalis.shared.util.NumberToWords",
    replaceWith = ReplaceWith("com.augmentalis.shared.util.NumberToWords")
)
val NumberToWords = com.augmentalis.shared.util.NumberToWords
