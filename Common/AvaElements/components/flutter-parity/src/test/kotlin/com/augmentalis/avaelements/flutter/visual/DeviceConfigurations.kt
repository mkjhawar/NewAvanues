package com.augmentalis.avaelements.flutter.visual

import android.content.res.Configuration
import app.cash.paparazzi.DeviceConfig
import com.android.resources.Density
import com.android.resources.ScreenOrientation

/**
 * Device configurations for visual testing across the device matrix.
 *
 * Provides 4 standard device configurations:
 * - Pixel 6 (Standard phone)
 * - Pixel Tablet (Tablet)
 * - Pixel Fold (Foldable/wide screen)
 * - Pixel 4a (Small phone)
 *
 * @since 1.0.0 (Visual Testing Framework)
 */
object DeviceConfigurations {

    /**
     * Pixel 6 - Standard Android phone configuration.
     *
     * Resolution: 1080x2400
     * DPI: 420
     * Use case: Most common phone size
     */
    val PIXEL_6 = DeviceConfig(
        screenHeight = 2400,
        screenWidth = 1080,
        xdpi = 420,
        ydpi = 420,
        orientation = ScreenOrientation.PORTRAIT,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS",
        fontScale = 1.0f,
        screenRound = false,
        chinSize = 0,
        density = Density.XXHIGH,
        ratio = com.android.resources.ScreenRatio.LONG
    )

    /**
     * Pixel Tablet - Large screen tablet configuration.
     *
     * Resolution: 2560x1600
     * DPI: 320
     * Use case: Tablet layouts, landscape orientation
     */
    val PIXEL_TABLET = DeviceConfig(
        screenHeight = 1600,
        screenWidth = 2560,
        xdpi = 320,
        ydpi = 320,
        orientation = ScreenOrientation.LANDSCAPE,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS",
        fontScale = 1.0f,
        screenRound = false,
        chinSize = 0,
        density = Density.XHIGH,
        ratio = com.android.resources.ScreenRatio.LONG
    )

    /**
     * Pixel Fold - Foldable phone unfolded configuration.
     *
     * Resolution: 1080x2092
     * DPI: 420
     * Use case: Wide/tall screens, multi-window
     */
    val PIXEL_FOLD_UNFOLDED = DeviceConfig(
        screenHeight = 2092,
        screenWidth = 1080,
        xdpi = 420,
        ydpi = 420,
        orientation = ScreenOrientation.PORTRAIT,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS",
        fontScale = 1.0f,
        screenRound = false,
        chinSize = 0,
        density = Density.XXHIGH,
        ratio = com.android.resources.ScreenRatio.LONG
    )

    /**
     * Pixel 4a - Smaller Android phone configuration.
     *
     * Resolution: 1080x2340
     * DPI: 440
     * Use case: Compact phones, smaller screens
     */
    val PIXEL_4A = DeviceConfig(
        screenHeight = 2340,
        screenWidth = 1080,
        xdpi = 440,
        ydpi = 440,
        orientation = ScreenOrientation.PORTRAIT,
        uiMode = Configuration.UI_MODE_NIGHT_NO,
        locale = "en-rUS",
        fontScale = 1.0f,
        screenRound = false,
        chinSize = 0,
        density = Density.XXXHIGH,
        ratio = com.android.resources.ScreenRatio.LONG
    )

    /**
     * All device configurations for matrix testing.
     */
    val ALL_DEVICES = listOf(
        PIXEL_6,
        PIXEL_TABLET,
        PIXEL_FOLD_UNFOLDED,
        PIXEL_4A
    )

    /**
     * Device names for labeling screenshots.
     */
    val DEVICE_NAMES = mapOf(
        PIXEL_6 to "Pixel6",
        PIXEL_TABLET to "PixelTablet",
        PIXEL_FOLD_UNFOLDED to "PixelFold",
        PIXEL_4A to "Pixel4a"
    )

    /**
     * Gets a friendly name for a device configuration.
     */
    fun DeviceConfig.getName(): String = DEVICE_NAMES[this] ?: "UnknownDevice"
}
