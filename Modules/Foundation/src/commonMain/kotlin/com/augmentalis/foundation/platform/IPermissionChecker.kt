/**
 * IPermissionChecker.kt - Cross-platform permission checking interface
 *
 * Abstraction for querying and requesting platform permissions.
 * Android implements via PackageManager + ActivityCompat.
 * iOS implements via Info.plist + AVFoundation / CoreLocation / etc.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.platform

/**
 * Platform-agnostic permission checking and requesting.
 *
 * Permission strings follow Android conventions (e.g., "android.permission.RECORD_AUDIO")
 * on Android, and platform-specific identifiers on iOS (e.g., "microphone", "camera").
 * Implementations map these to the appropriate platform API calls.
 */
interface IPermissionChecker {

    /**
     * Check whether a permission is currently granted.
     *
     * @param permission Platform-specific permission identifier
     * @return true if the permission is granted
     */
    suspend fun hasPermission(permission: String): Boolean

    /**
     * Request a permission from the user.
     *
     * On Android, this triggers the system permission dialog via ActivityCompat.
     * On iOS, this triggers the native permission prompt.
     * Returns the result after the user responds.
     *
     * @param permission Platform-specific permission identifier
     * @return true if the permission was granted
     */
    suspend fun requestPermission(permission: String): Boolean

    /**
     * Check whether the accessibility service is enabled.
     *
     * On Android, checks if VoiceOSAccessibilityService is enabled in system settings.
     * On iOS, checks if AssistiveTouch / Switch Control APIs are available.
     *
     * @return true if accessibility service is active
     */
    fun isAccessibilityEnabled(): Boolean

    /**
     * Check whether overlay (draw over other apps) permission is granted.
     *
     * On Android, checks Settings.canDrawOverlays().
     * On iOS, this is always true (no equivalent restriction).
     *
     * @return true if overlay drawing is permitted
     */
    fun canDrawOverlays(): Boolean
}
