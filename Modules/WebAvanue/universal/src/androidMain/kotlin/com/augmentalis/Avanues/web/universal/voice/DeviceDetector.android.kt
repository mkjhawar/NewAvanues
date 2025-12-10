package com.augmentalis.Avanues.web.universal.voice

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Android-specific device detection implementation
 */
actual object DeviceDetector {

    /**
     * Detect current device type based on manufacturer and model
     */
    actual fun detectDeviceType(): DeviceType {
        val smartGlass = detectSmartGlass()
        if (smartGlass != null) {
            return smartGlass.type
        }

        // Fallback: check if tablet
        return if (isTablet()) DeviceType.TABLET else DeviceType.PHONE
    }

    /**
     * Detect specific smart glass device
     */
    actual fun detectSmartGlass(): SmartGlassDevice? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return SmartGlassDevice.detect(manufacturer, model)
    }

    /**
     * Check if device is a tablet based on screen size
     */
    actual fun isTablet(): Boolean {
        // Method 1: Check smallest screen width (>= 600dp = tablet)
        val smallestScreenWidthDp = try {
            android.content.res.Resources.getSystem()
                .configuration.smallestScreenWidthDp
        } catch (e: Exception) {
            0
        }

        return smallestScreenWidthDp >= 600
    }

    /**
     * Get human-readable device name for logging/debugging
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return "$manufacturer $model"
    }

    /**
     * Get detailed device info for diagnostics
     */
    fun getDeviceInfo(): String {
        return buildString {
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")

            val deviceType = detectDeviceType()
            appendLine("Detected Type: $deviceType")

            val smartGlass = detectSmartGlass()
            if (smartGlass != null) {
                appendLine("Smart Glass: $smartGlass")
            }

            appendLine("Is Tablet: ${isTablet()}")
        }
    }
}
