package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.Permission

/**
 * iOS-specific permission checking
 *
 * @since 2.0.0
 */
actual fun expect_checkPermission(pluginId: String, permission: Permission): Boolean {
    // iOS implementation using Authorization framework
    // For now, return false (deny all permissions)
    return false
}

/**
 * iOS-specific permission requesting
 *
 * @since 2.0.0
 */
actual suspend fun expect_requestPermission(pluginId: String, permission: Permission): Boolean {
    // iOS implementation using permission dialogs
    // For now, return false (deny all permissions)
    return false
}
