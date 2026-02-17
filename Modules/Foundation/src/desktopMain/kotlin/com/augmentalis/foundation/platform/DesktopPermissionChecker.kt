/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */

package com.augmentalis.foundation.platform

/**
 * Desktop (JVM) implementation of [IPermissionChecker].
 *
 * Desktop platforms (Windows, macOS, Linux) do not have a runtime permission model similar to Android.
 * All permission checks return true as permissions are handled at the OS file system and process level.
 */
class DesktopPermissionChecker : IPermissionChecker {

    override suspend fun hasPermission(permission: String): Boolean = true

    override suspend fun requestPermission(permission: String): Boolean = true

    override fun isAccessibilityEnabled(): Boolean = true

    override fun canDrawOverlays(): Boolean = true
}
