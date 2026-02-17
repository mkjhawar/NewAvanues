package com.augmentalis.webavanue

import platform.UIKit.UIDevice

/**
 * iOS DeviceDetector implementation
 */
actual object DeviceDetector {
    actual fun isTablet(): Boolean {
        return UIDevice.currentDevice.userInterfaceIdiom == platform.UIKit.UIUserInterfaceIdiomPad
    }

    actual fun isSmartGlasses(): Boolean {
        // iOS doesn't have smart glasses support
        return false
    }

    actual fun getDeviceName(): String {
        return UIDevice.currentDevice.name
    }

    actual fun getDeviceModel(): String {
        return UIDevice.currentDevice.model
    }
}
