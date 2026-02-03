package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.Permission

/**
 * Android-specific permission checking
 *
 * @since 2.0.0
 */
actual fun expect_checkPermission(pluginId: String, permission: Permission): Boolean {
    // Android implementation using PackageManager
    // For now, return false (deny all permissions)
    return false
}

/**
 * Android-specific permission requesting
 *
 * @since 2.0.0
 */
actual suspend fun expect_requestPermission(pluginId: String, permission: Permission): Boolean {
    // Android implementation using ActivityCompat.requestPermissions
    // For now, return false (deny all permissions)
    return false
}
