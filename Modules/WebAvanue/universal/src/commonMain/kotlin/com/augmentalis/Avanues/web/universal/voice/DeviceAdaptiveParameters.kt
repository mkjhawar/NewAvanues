package com.augmentalis.Avanues.web.universal.voice

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Device type categories for smart glasses and displays
 */
enum class DeviceType {
    /** See-through AR displays (Vuzix non-M400, most AR glasses) */
    SEE_THROUGH_AR,

    /** Opaque displays (RealWear, Vuzix M400, ARC3) */
    OPAQUE_DISPLAY,

    /** Standard mobile phone */
    PHONE,

    /** Tablet device */
    TABLET,

    /** Unknown or not detected */
    UNKNOWN
}

/**
 * Specific smart glass manufacturers and models
 */
enum class SmartGlassDevice(
    val manufacturer: String,
    val model: String?,
    val type: DeviceType
) {
    // RealWear devices
    REALWEAR_HMT1("realwear", "hmt-1", DeviceType.OPAQUE_DISPLAY),
    REALWEAR_HMT1Z1("realwear", "hmt-1z1", DeviceType.OPAQUE_DISPLAY),
    REALWEAR_NAVIGATOR520("realwear", "navigator 520", DeviceType.OPAQUE_DISPLAY),
    REALWEAR_NAVIGATOR500("realwear", "navigator 500", DeviceType.OPAQUE_DISPLAY),

    // Vuzix devices
    VUZIX_M400("vuzix", "m400", DeviceType.OPAQUE_DISPLAY),
    VUZIX_M4000("vuzix", "m4000", DeviceType.OPAQUE_DISPLAY),
    VUZIX_BLADE2("vuzix", "blade 2", DeviceType.SEE_THROUGH_AR),
    VUZIX_SHIELD("vuzix", "shield", DeviceType.SEE_THROUGH_AR),

    // Rokid devices
    ROKID_AIR("rokid", "air", DeviceType.SEE_THROUGH_AR),
    ROKID_AIR2("rokid", "air 2", DeviceType.SEE_THROUGH_AR),
    ROKID_MAX("rokid", "max", DeviceType.SEE_THROUGH_AR),

    // Xreal (formerly Nreal)
    XREAL_AIR("xreal", "air", DeviceType.SEE_THROUGH_AR),
    XREAL_AIR2("xreal", "air 2", DeviceType.SEE_THROUGH_AR),
    XREAL_AIR2_PRO("xreal", "air 2 pro", DeviceType.SEE_THROUGH_AR),
    XREAL_LIGHT("xreal", "light", DeviceType.SEE_THROUGH_AR),

    // TCL devices
    TCL_NXTWEAR("tcl", "nxtwear", DeviceType.SEE_THROUGH_AR),
    TCL_NXTWEAR_AIR("tcl", "nxtwear air", DeviceType.SEE_THROUGH_AR),
    TCL_NXTWEAR_S("tcl", "nxtwear s", DeviceType.SEE_THROUGH_AR),

    // Lenovo devices
    LENOVO_THINKPLUS("lenovo", "thinkplus", DeviceType.SEE_THROUGH_AR),
    LENOVO_GLASSES_T1("lenovo", "glasses t1", DeviceType.SEE_THROUGH_AR),

    // ARC3 (could be various manufacturers)
    ARC3_GENERIC("arc", "arc3", DeviceType.OPAQUE_DISPLAY);

    companion object {
        /**
         * Detect device from manufacturer and model strings
         */
        fun detect(manufacturer: String, model: String): SmartGlassDevice? {
            val mfg = manufacturer.lowercase()
            val mdl = model.lowercase()

            return values().firstOrNull { device ->
                mfg.contains(device.manufacturer) &&
                (device.model == null || mdl.contains(device.model))
            }
        }
    }
}

/**
 * Adaptive UI parameters based on device type
 * All values optimized for readability and usability on specific device categories
 */
data class DeviceAdaptiveParameters(
    // Dialog sizing
    val dialogWidthPortrait: Float,
    val dialogHeightPortrait: Float,
    val dialogWidthLandscape: Float,
    val dialogHeightLandscape: Float,

    // Content padding
    val contentPaddingPortrait: Dp,
    val contentPaddingLandscape: Dp,

    // Header spacing
    val headerSpacingPortrait: Dp,
    val headerSpacingLandscape: Dp,

    // Grid columns
    val categoriesColumnsPortrait: Int,
    val commandsColumnsPortrait: Int,
    val categoriesColumnsLandscape: Int,
    val commandsColumnsLandscape: Int,

    // Grid spacing
    val gridHorizontalSpacing: Dp,
    val gridVerticalSpacing: Dp,

    // Button sizing
    val categoryButtonHeight: Dp,
    val iconSize: Dp,
    val backButtonIconSize: Dp,

    // Typography sizes
    val headerTextSize: TextUnit,
    val categoryTextSize: TextUnit,
    val commandTextSize: TextUnit,
    val descriptionTextSize: TextUnit,

    // Touch targets (Material Design: min 48dp)
    val minTouchTarget: Dp,

    // Card/item sizing
    val cardPadding: Dp,
    val itemSpacing: Dp,
    val badgeMinWidth: Dp
) {
    companion object {
        /**
         * Standard phone parameters (baseline)
         */
        fun phone() = DeviceAdaptiveParameters(
            dialogWidthPortrait = 0.90f,
            dialogHeightPortrait = 0.85f,
            dialogWidthLandscape = 0.80f,
            dialogHeightLandscape = 0.80f,
            contentPaddingPortrait = 24.dp,
            contentPaddingLandscape = 16.dp,
            headerSpacingPortrait = 24.dp,
            headerSpacingLandscape = 16.dp,
            categoriesColumnsPortrait = 2,
            commandsColumnsPortrait = 2,
            categoriesColumnsLandscape = 3,
            commandsColumnsLandscape = 2,
            gridHorizontalSpacing = 6.dp,
            gridVerticalSpacing = 8.dp,
            categoryButtonHeight = 56.dp,
            iconSize = 24.dp,
            backButtonIconSize = 18.dp,
            headerTextSize = 20.sp,
            categoryTextSize = 16.sp,
            commandTextSize = 13.sp,
            descriptionTextSize = 11.sp,
            minTouchTarget = 48.dp,
            cardPadding = 12.dp,
            itemSpacing = 8.dp,
            badgeMinWidth = 90.dp
        )

        /**
         * Tablet parameters (larger screen, more spacing)
         */
        fun tablet() = DeviceAdaptiveParameters(
            dialogWidthPortrait = 0.85f,
            dialogHeightPortrait = 0.80f,
            dialogWidthLandscape = 0.75f,
            dialogHeightLandscape = 0.75f,
            contentPaddingPortrait = 32.dp,
            contentPaddingLandscape = 24.dp,
            headerSpacingPortrait = 32.dp,
            headerSpacingLandscape = 24.dp,
            categoriesColumnsPortrait = 2,
            commandsColumnsPortrait = 2,
            categoriesColumnsLandscape = 3,
            commandsColumnsLandscape = 3, // 3 columns on tablet
            gridHorizontalSpacing = 8.dp,
            gridVerticalSpacing = 12.dp,
            categoryButtonHeight = 64.dp,
            iconSize = 28.dp,
            backButtonIconSize = 20.dp,
            headerTextSize = 24.sp,
            categoryTextSize = 18.sp,
            commandTextSize = 14.sp,
            descriptionTextSize = 12.sp,
            minTouchTarget = 48.dp,
            cardPadding = 16.dp,
            itemSpacing = 12.dp,
            badgeMinWidth = 100.dp
        )

        /**
         * See-through AR glasses (Rokid, Xreal, most Vuzix, TCL, Lenovo)
         *
         * Optimizations:
         * - Larger text for distance viewing
         * - More contrast/spacing for see-through display
         * - Compact layout to fit limited FOV
         * - Higher touch targets for gaze interaction
         */
        fun seeThroughAR() = DeviceAdaptiveParameters(
            dialogWidthPortrait = 0.85f,
            dialogHeightPortrait = 0.80f,
            dialogWidthLandscape = 0.75f, // Smaller to fit FOV
            dialogHeightLandscape = 0.75f,
            contentPaddingPortrait = 20.dp,
            contentPaddingLandscape = 14.dp, // Reduced for compact layout
            headerSpacingPortrait = 20.dp,
            headerSpacingLandscape = 14.dp,
            categoriesColumnsPortrait = 2,
            commandsColumnsPortrait = 2,
            categoriesColumnsLandscape = 3, // Keep 3 columns
            commandsColumnsLandscape = 2,
            gridHorizontalSpacing = 8.dp, // More spacing for clarity
            gridVerticalSpacing = 10.dp,
            categoryButtonHeight = 60.dp, // Slightly larger for gaze
            iconSize = 26.dp, // Larger icons for visibility
            backButtonIconSize = 20.dp,
            headerTextSize = 22.sp, // Larger for distance reading
            categoryTextSize = 17.sp,
            commandTextSize = 14.sp, // Larger for AR viewing
            descriptionTextSize = 12.sp,
            minTouchTarget = 48.dp, // Keep standard (gaze + gesture)
            cardPadding = 14.dp,
            itemSpacing = 10.dp,
            badgeMinWidth = 95.dp
        )

        /**
         * Opaque display glasses (RealWear, Vuzix M400, ARC3)
         *
         * Optimizations:
         * - Very large text (worn close to eye)
         * - Maximum contrast
         * - Fewer columns (limited resolution)
         * - Larger touch targets (voice + gesture primary)
         * - Compact but readable
         */
        fun opaqueDisplay() = DeviceAdaptiveParameters(
            dialogWidthPortrait = 0.92f, // Maximize screen use
            dialogHeightPortrait = 0.88f,
            dialogWidthLandscape = 0.85f, // Larger for limited res
            dialogHeightLandscape = 0.85f,
            contentPaddingPortrait = 16.dp, // Minimal padding
            contentPaddingLandscape = 12.dp,
            headerSpacingPortrait = 16.dp,
            headerSpacingLandscape = 12.dp,
            categoriesColumnsPortrait = 1, // Single column for readability
            commandsColumnsPortrait = 1,
            categoriesColumnsLandscape = 2, // Reduce to 2 for readability
            commandsColumnsLandscape = 2,
            gridHorizontalSpacing = 10.dp, // More spacing
            gridVerticalSpacing = 12.dp,
            categoryButtonHeight = 68.dp, // Larger buttons
            iconSize = 30.dp, // Very large icons
            backButtonIconSize = 24.dp,
            headerTextSize = 24.sp, // Very large text
            categoryTextSize = 19.sp,
            commandTextSize = 16.sp, // Much larger
            descriptionTextSize = 14.sp,
            minTouchTarget = 48.dp, // Keep standard
            cardPadding = 16.dp,
            itemSpacing = 12.dp,
            badgeMinWidth = 110.dp // Wider badges
        )

        /**
         * Get appropriate parameters for device type
         */
        fun forDeviceType(deviceType: DeviceType, isTablet: Boolean = false): DeviceAdaptiveParameters {
            return when {
                isTablet -> tablet()
                deviceType == DeviceType.SEE_THROUGH_AR -> seeThroughAR()
                deviceType == DeviceType.OPAQUE_DISPLAY -> opaqueDisplay()
                deviceType == DeviceType.TABLET -> tablet()
                else -> phone() // Default to phone
            }
        }
    }
}

/**
 * Platform-specific device detection
 * Implement in platform-specific source sets (androidMain, etc.)
 */
expect object DeviceDetector {
    /**
     * Detect current device type
     */
    fun detectDeviceType(): DeviceType

    /**
     * Detect specific smart glass device if applicable
     */
    fun detectSmartGlass(): SmartGlassDevice?

    /**
     * Check if device is a tablet
     */
    fun isTablet(): Boolean
}
