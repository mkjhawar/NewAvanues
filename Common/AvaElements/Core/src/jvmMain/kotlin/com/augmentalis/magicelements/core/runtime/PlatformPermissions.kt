package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.Permission

/**
 * JVM (Desktop) specific permission checking
 *
 * @since 2.0.0
 */
actual fun expect_checkPermission(pluginId: String, permission: Permission): Boolean {
    // Desktop platforms are typically more permissive
    // But we still enforce security policies
    // For now, return false (deny all permissions)
    return false
}

/**
 * JVM (Desktop) specific permission requesting
 *
 * @since 2.0.0
 */
actual suspend fun expect_requestPermission(pluginId: String, permission: Permission): Boolean {
    // Desktop implementation using native dialogs
    // For now, return false (deny all permissions)
    return false
}
