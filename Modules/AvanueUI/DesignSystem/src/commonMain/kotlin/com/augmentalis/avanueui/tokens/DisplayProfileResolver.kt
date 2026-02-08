package com.augmentalis.avanueui.tokens

/**
 * Resolves [DisplayProfile] from device display characteristics.
 *
 * Pure function - no platform dependencies. The calling layer
 * (e.g., DeviceManager, Activity) provides raw measurements
 * and this resolver selects the appropriate profile.
 *
 * Resolution heuristics for smart glasses:
 * - <=480px width → GLASS_MICRO (Vuzix Blade 480x480, Shield 640x360)
 * - <=854px width → GLASS_COMPACT (RealWear HMT 854x480, Vuzix M400)
 * - <=1280px width → GLASS_STANDARD (RealWear Nav520 1280x720, M4000)
 * - >1280px width → GLASS_HD (XREAL Air 1920x1080, Vuzix Z100)
 *
 * Phone/tablet heuristics:
 * - smallestWidthDp >= 600 → TABLET
 * - else → PHONE
 */
object DisplayProfileResolver {

    /**
     * Resolve the best [DisplayProfile] for the given display characteristics.
     *
     * @param widthPx Physical display width in pixels (landscape orientation for glasses)
     * @param heightPx Physical display height in pixels
     * @param densityDpi Display density in DPI (e.g., 160, 213, 320, 480)
     * @param isSmartGlass Whether the device is detected as a smart glass form factor
     * @return The [DisplayProfile] that best matches the display
     */
    fun resolve(
        widthPx: Int,
        heightPx: Int,
        densityDpi: Int,
        isSmartGlass: Boolean
    ): DisplayProfile {
        // Smart glass path: use resolution to select glass profile
        if (isSmartGlass) {
            return resolveGlassProfile(widthPx, heightPx)
        }

        // Phone/tablet path: use smallest width in dp
        val density = densityDpi / 160f
        val widthDp = widthPx / density
        val heightDp = heightPx / density
        val smallestWidthDp = minOf(widthDp, heightDp)

        return if (smallestWidthDp >= 600f) {
            DisplayProfile.TABLET
        } else {
            DisplayProfile.PHONE
        }
    }

    /**
     * Resolve glass profile from pixel resolution.
     * Uses the larger dimension (landscape width) for classification.
     */
    private fun resolveGlassProfile(widthPx: Int, heightPx: Int): DisplayProfile {
        val maxDimension = maxOf(widthPx, heightPx)

        return when {
            maxDimension <= 640 -> DisplayProfile.GLASS_MICRO
            maxDimension <= 960 -> DisplayProfile.GLASS_COMPACT
            maxDimension <= 1280 -> DisplayProfile.GLASS_STANDARD
            else -> DisplayProfile.GLASS_HD
        }
    }
}
