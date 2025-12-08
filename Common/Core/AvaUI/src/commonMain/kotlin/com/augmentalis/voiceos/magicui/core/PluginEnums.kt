package com.augmentalis.avanues.avaui.core

/**
 * Plugin source enumeration.
 *
 * Indicates where the plugin originated from.
 */
enum class PluginSource {
    /** Pre-bundled with application */
    PRE_BUNDLED,

    /** Downloaded from AppAvanue store */
    APPAVENUE_STORE,

    /** Installed from third-party source */
    THIRD_PARTY
}

/**
 * Developer verification level enumeration.
 *
 * Indicates the security verification level for the plugin developer.
 */
enum class DeveloperVerificationLevel {
    /** Manual review passed - highest trust */
    VERIFIED,

    /** Code signing with selective review */
    REGISTERED,

    /** Sandboxing only - lowest trust */
    UNVERIFIED
}
