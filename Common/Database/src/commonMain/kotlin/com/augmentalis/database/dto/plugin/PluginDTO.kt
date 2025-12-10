/**
 * PluginDTO.kt - Data Transfer Object for Plugin
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.dto.plugin

/**
 * Data Transfer Object for Plugin entity.
 * Represents plugin metadata for cross-platform use.
 */
data class PluginDTO(
    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val author: String?,
    val state: PluginState,
    val enabled: Boolean,
    val installPath: String,
    val installedAt: Long,
    val updatedAt: Long,
    val configJson: String?
)

/**
 * Plugin lifecycle state.
 */
enum class PluginState {
    PENDING,
    INSTALLING,
    INSTALLED,
    ENABLED,
    DISABLED,
    UPDATING,
    UNINSTALLING,
    FAILED
}

/**
 * Plugin source origin.
 */
enum class PluginSource {
    PRE_BUNDLED,
    APPAVENUE_STORE,
    THIRD_PARTY
}

/**
 * Developer verification level.
 */
enum class DeveloperVerificationLevel {
    VERIFIED,      // Manual review passed
    REGISTERED,    // Code signing with selective review
    UNVERIFIED     // Sandboxing only
}
