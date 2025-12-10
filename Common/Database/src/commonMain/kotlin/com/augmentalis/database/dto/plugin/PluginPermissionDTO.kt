/**
 * PluginPermissionDTO.kt - Data Transfer Object for Plugin Permission
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.dto.plugin

/**
 * Data Transfer Object for PluginPermission entity.
 * Represents plugin permissions for cross-platform use.
 */
data class PluginPermissionDTO(
    val id: Long?,
    val pluginId: String,
    val permission: String,
    val granted: Boolean,
    val grantedAt: Long?,
    val grantedBy: String?
)

/**
 * Plugin permission types.
 */
enum class PluginPermission {
    CAMERA,
    LOCATION,
    STORAGE_READ,
    STORAGE_WRITE,
    NETWORK,
    MICROPHONE,
    CONTACTS,
    CALENDAR,
    BLUETOOTH,
    SENSORS,
    ACCESSIBILITY_SERVICES,
    PAYMENTS
}

/**
 * Permission grant status.
 */
enum class GrantStatus {
    PENDING,    // Awaiting user decision
    GRANTED,    // User granted permission
    DENIED,     // User denied permission
    REVOKED     // Previously granted, now revoked
}
