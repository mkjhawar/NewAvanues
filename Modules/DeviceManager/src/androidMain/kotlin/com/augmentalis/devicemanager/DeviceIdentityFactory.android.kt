// Author: Manoj Jhawar
// Purpose: Android actual implementation of DeviceIdentityFactory

package com.augmentalis.devicemanager

/**
 * Android actual implementation of DeviceIdentityFactory.
 *
 * Uses the DeviceCapabilityFactory context for initialization.
 */
actual object DeviceIdentityFactory {
    /**
     * Create an Android device identity provider.
     * Requires DeviceCapabilityFactory.initialize(context) to be called first.
     */
    actual fun create(): DeviceIdentityProvider {
        // Leverage the capability provider for identity
        val capabilityProvider = DeviceCapabilityFactory.create()
        return AndroidDeviceIdentityProvider(capabilityProvider)
    }
}
