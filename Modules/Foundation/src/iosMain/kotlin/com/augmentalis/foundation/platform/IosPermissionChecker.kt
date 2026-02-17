/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */
package com.augmentalis.foundation.platform

import com.augmentalis.foundation.IPermissionChecker

/**
 * iOS permission checker. Real permission requests are handled by the Swift
 * app layer. This serves as a KMP bridge for shared code that needs to
 * query permission status.
 *
 * iOS permissions are typically requested declaratively via Info.plist and
 * user approvals are handled natively by the Swift app layer, not through
 * Kotlin/Native code. This implementation bridges shared code that may need
 * to check permission status or simulate requests for cross-platform compatibility.
 */
class IosPermissionChecker : IPermissionChecker {
    override suspend fun hasPermission(permission: String): Boolean = true

    override suspend fun requestPermission(permission: String): Boolean = true

    override fun isAccessibilityEnabled(): Boolean = false

    override fun canDrawOverlays(): Boolean = true
}
