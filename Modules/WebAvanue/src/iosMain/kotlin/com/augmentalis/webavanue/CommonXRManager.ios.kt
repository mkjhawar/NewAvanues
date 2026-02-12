package com.augmentalis.webavanue

/**
 * iOS XR Manager stub implementation
 *
 * iOS doesn't have smart glasses XR support
 * Returns a no-op implementation
 */
actual fun createXRManager(platformContext: Any): CommonXRManager {
    return object : CommonXRManager {
        override fun isXRDeviceConnected(): Boolean = false
        override fun getDeviceInfo(): String = "No XR device (iOS)"
        override suspend fun showWebPage(url: String): Boolean = false
        override suspend fun closeWebPage(): Boolean = false
        override fun dispose() {}
    }
}
