/**
 * MissingTypes.kt - Type aliases for backwards compatibility
 *
 * Note: Core command types including CommandCategory are now in CommandModels.kt.
 * This file contains only supplementary type aliases.
 */
package com.augmentalis.voiceoscore

// Type aliases for backwards compatibility with androidMain code
typealias UnifiedCommandContext = CommandContext
typealias ModelsCommandContext = CommandContext

/**
 * Additional error codes for specific use cases.
 */
object AdditionalErrorCodes {
    const val NONE = 0
    const val ELEMENT_NOT_FOUND = 1
    const val INVALID_STATE = 2
    const val NOT_SUPPORTED = 3
}
