// Author: Manoj Jhawar
// Purpose: Android actual implementation of DeviceCapabilityFactory

package com.augmentalis.devicemanager

import android.content.Context

/**
 * Android actual implementation of DeviceCapabilityFactory.
 */
actual object DeviceCapabilityFactory {
    private var context: Context? = null

    /**
     * Initialize with Android context. Must be called before create().
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Create an Android device capability provider.
     */
    actual fun create(): DeviceCapabilityProvider {
        val ctx = context ?: throw IllegalStateException(
            "DeviceCapabilityFactory.initialize(context) must be called before create()"
        )
        return AndroidDeviceCapabilityProvider(ctx)
    }
}
