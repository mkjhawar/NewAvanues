package com.augmentalis.webavanue

/**
 * iOS DownloadPermissionManager implementation
 *
 * iOS doesn't require storage permissions for downloads to app documents directory
 */
actual class DownloadPermissionManager {
    actual fun hasPermission(): Boolean {
        // iOS apps always have permission to write to their own documents directory
        return true
    }

    actual fun requestPermission(callback: (Boolean) -> Unit) {
        // No permission request needed for iOS
        callback(true)
    }

    actual fun shouldShowRationale(): Boolean {
        return false
    }

    actual fun openSettings() {
        // iOS doesn't need settings for this
    }
}
