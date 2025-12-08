package com.augmentalis.avaelements.flutter.visual

import android.content.res.Configuration
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams

/**
 * Shared Paparazzi configuration for all visual tests.
 *
 * Provides centralized configuration for:
 * - Device matrix (4 devices)
 * - Theme modes (light/dark)
 * - Rendering settings
 * - Screenshot output settings
 *
 * @since 1.0.0 (Visual Testing Framework)
 */
object PaparazziConfig {

    /**
     * Default Paparazzi instance for standard phone testing.
     * Uses Pixel 6 configuration with light theme.
     */
    fun createDefault(): Paparazzi = Paparazzi(
        deviceConfig = DeviceConfigurations.PIXEL_6,
        theme = "android:Theme.Material3.Light.NoActionBar",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
        maxPercentDifference = 0.1 // Allow 0.1% pixel difference for anti-aliasing
    )

    /**
     * Dark theme Paparazzi instance.
     */
    fun createDark(): Paparazzi = Paparazzi(
        deviceConfig = DeviceConfigurations.PIXEL_6.copy(
            uiMode = Configuration.UI_MODE_NIGHT_YES
        ),
        theme = "android:Theme.Material3.Dark.NoActionBar",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    /**
     * Tablet configuration for larger screen testing.
     */
    fun createTablet(): Paparazzi = Paparazzi(
        deviceConfig = DeviceConfigurations.PIXEL_TABLET,
        theme = "android:Theme.Material3.Light.NoActionBar",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    /**
     * Accessibility configuration with large text (200% scale).
     */
    fun createAccessibility(): Paparazzi = Paparazzi(
        deviceConfig = DeviceConfigurations.PIXEL_6.copy(
            fontScale = 2.0f
        ),
        theme = "android:Theme.Material3.Light.NoActionBar",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = true, // Show accessibility UI elements
        maxPercentDifference = 0.1
    )

    /**
     * RTL (Right-to-Left) configuration for Arabic/Hebrew testing.
     */
    fun createRTL(): Paparazzi = Paparazzi(
        deviceConfig = DeviceConfigurations.PIXEL_6.copy(
            locale = "ar" // Arabic locale
        ),
        theme = "android:Theme.Material3.Light.NoActionBar",
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    /**
     * Creates a Paparazzi instance for a specific device and theme.
     *
     * @param device Device configuration from DeviceConfigurations
     * @param isDark Whether to use dark theme
     * @return Configured Paparazzi instance
     */
    fun create(device: DeviceConfig, isDark: Boolean = false): Paparazzi = Paparazzi(
        deviceConfig = if (isDark) {
            device.copy(uiMode = Configuration.UI_MODE_NIGHT_YES)
        } else {
            device
        },
        theme = if (isDark) {
            "android:Theme.Material3.Dark.NoActionBar"
        } else {
            "android:Theme.Material3.Light.NoActionBar"
        },
        renderingMode = SessionParams.RenderingMode.SHRINK,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )
}
