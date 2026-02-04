/**
 * MissingTypes.kt - Type aliases for backwards compatibility
 *
 * Note: Core command types are now in CommandModels.kt at the package root.
 * This file contains only supplementary type aliases not in CommandModels.kt.
 */
package com.augmentalis.voiceoscore

// Type aliases for backwards compatibility with androidMain code
// that may use different package imports

/**
 * Alias for unified context (backwards compatibility).
 */
typealias UnifiedCommandContext = CommandContext
typealias ModelsCommandContext = CommandContext

/**
 * Additional error codes not in CommandModels.kt
 * These can be used alongside the ErrorCode enum from CommandModels.kt
 */
object AdditionalErrorCodes {
    const val NONE = 0
    const val ELEMENT_NOT_FOUND = 1
    const val INVALID_STATE = 2
    const val NOT_SUPPORTED = 3
}

/**
 * Additional command category values
 * Complements the categories in StaticCommandRegistry.kt
 */
enum class CommandCategory {
    NAVIGATION,
    TEXT,
    MEDIA,
    SYSTEM,
    APP,
    ACCESSIBILITY,
    VOICE,
    GESTURE,
    CUSTOM,
    INPUT,
    APP_CONTROL
}
