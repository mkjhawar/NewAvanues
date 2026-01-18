// Author: Manoj Jhawar
// Purpose: Deprecation shims for migration to KMP APIs

package com.augmentalis.devicemanager

import android.content.Context

/**
 * DEPRECATED: Legacy device info accessors.
 *
 * This object provides backward-compatible access to device information
 * while directing consumers to the new KMP-compatible APIs.
 *
 * Migration Guide:
 * ```kotlin
 * // Old way (deprecated)
 * val manufacturer = LegacyDeviceInfo.getManufacturer(context)
 *
 * // New way (KMP-compatible)
 * DeviceCapabilityFactory.initialize(context)
 * val provider = DeviceCapabilityFactory.create()
 * val manufacturer = provider.getKmpDeviceInfo().manufacturer
 * ```
 */
@Deprecated(
    message = "Use DeviceCapabilityFactory.create() instead for KMP-compatible device info access",
    replaceWith = ReplaceWith(
        "DeviceCapabilityFactory.create()",
        "com.augmentalis.devicemanager.DeviceCapabilityFactory"
    ),
    level = DeprecationLevel.WARNING
)
object LegacyDeviceInfo {

    /**
     * Get device manufacturer.
     *
     * @param context Android context
     * @return Manufacturer name (e.g., "Google", "Samsung")
     * @deprecated Use DeviceCapabilityFactory.create().getKmpDeviceInfo().manufacturer
     */
    @Deprecated(
        message = "Use DeviceCapabilityFactory.create().getKmpDeviceInfo().manufacturer",
        replaceWith = ReplaceWith(
            "DeviceCapabilityFactory.create().getKmpDeviceInfo().manufacturer",
            "com.augmentalis.devicemanager.DeviceCapabilityFactory"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getManufacturer(context: Context): String {
        DeviceCapabilityFactory.initialize(context)
        return DeviceCapabilityFactory.create().getKmpDeviceInfo().manufacturer
    }

    /**
     * Get device model.
     *
     * @param context Android context
     * @return Model name (e.g., "Pixel 7 Pro", "Galaxy S23")
     * @deprecated Use DeviceCapabilityFactory.create().getKmpDeviceInfo().model
     */
    @Deprecated(
        message = "Use DeviceCapabilityFactory.create().getKmpDeviceInfo().model",
        replaceWith = ReplaceWith(
            "DeviceCapabilityFactory.create().getKmpDeviceInfo().model",
            "com.augmentalis.devicemanager.DeviceCapabilityFactory"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getModel(context: Context): String {
        DeviceCapabilityFactory.initialize(context)
        return DeviceCapabilityFactory.create().getKmpDeviceInfo().model
    }

    /**
     * Get device fingerprint for identification.
     *
     * @param context Android context
     * @return Device fingerprint string
     * @deprecated Use DeviceCapabilityFactory.create().getDeviceFingerprint().value
     */
    @Deprecated(
        message = "Use DeviceCapabilityFactory.create().getDeviceFingerprint().value",
        replaceWith = ReplaceWith(
            "DeviceCapabilityFactory.create().getDeviceFingerprint().value",
            "com.augmentalis.devicemanager.DeviceCapabilityFactory"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getFingerprint(context: Context): String {
        DeviceCapabilityFactory.initialize(context)
        return DeviceCapabilityFactory.create().getDeviceFingerprint().value
    }

    /**
     * Get device brand.
     *
     * @param context Android context
     * @return Brand name (e.g., "google", "samsung")
     * @deprecated Use DeviceCapabilityFactory.create().getKmpDeviceInfo().brand
     */
    @Deprecated(
        message = "Use DeviceCapabilityFactory.create().getKmpDeviceInfo().brand",
        replaceWith = ReplaceWith(
            "DeviceCapabilityFactory.create().getKmpDeviceInfo().brand",
            "com.augmentalis.devicemanager.DeviceCapabilityFactory"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getBrand(context: Context): String {
        DeviceCapabilityFactory.initialize(context)
        return DeviceCapabilityFactory.create().getKmpDeviceInfo().brand
    }

    /**
     * Get OS version string.
     *
     * @param context Android context
     * @return OS version (e.g., "14", "13")
     * @deprecated Use DeviceCapabilityFactory.create().getKmpDeviceInfo().osVersion
     */
    @Deprecated(
        message = "Use DeviceCapabilityFactory.create().getKmpDeviceInfo().osVersion",
        replaceWith = ReplaceWith(
            "DeviceCapabilityFactory.create().getKmpDeviceInfo().osVersion",
            "com.augmentalis.devicemanager.DeviceCapabilityFactory"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getOsVersion(context: Context): String {
        DeviceCapabilityFactory.initialize(context)
        return DeviceCapabilityFactory.create().getKmpDeviceInfo().osVersion
    }
}
