package com.avanues.cockpit.core.display

/**
 * Display type configuration for spatial rendering
 *
 * Optimizes spatial projection for different display technologies:
 * - AR_GLASSES: Transparent AR displays (Rokid, Xreal, Vision Pro)
 * - LCD_SCREEN: Regular 2D displays (phones, tablets, monitors, TVs)
 *
 * Each type has different optimal viewing parameters for spatial experience.
 */
enum class DisplayType {
    /**
     * AR glasses (transparent displays)
     *
     * Characteristics:
     * - Viewing distance: 3-5m (AR windows float in space)
     * - FOV: Wide (90-110°) to match natural peripheral vision
     * - Curve: Strong cylindrical wrap for immersion
     * - Window size: Physical meters at distance
     */
    AR_GLASSES,

    /**
     * Regular LCD/OLED screens (phones, tablets, monitors, TVs)
     *
     * Characteristics:
     * - Viewing distance: Arm's length (0.5-1m for phones, 0.6-1.5m for monitors)
     * - FOV: Narrower (60-75°) to match screen bounds
     * - Curve: Subtle perspective for "pseudo-spatial" effect
     * - Window size: Screen-relative percentages
     */
    LCD_SCREEN;

    companion object {
        /**
         * Auto-detect display type based on device characteristics
         *
         * Heuristics:
         * - Has AR capability (ARCore/ARKit) → AR_GLASSES
         * - No AR capability → LCD_SCREEN
         * - Can be overridden by user preference
         */
        fun autoDetect(hasARCapability: Boolean): DisplayType {
            return if (hasARCapability) AR_GLASSES else LCD_SCREEN
        }
    }
}

/**
 * Display-specific projection parameters
 *
 * Optimized for each display type to provide best spatial experience
 */
data class DisplayConfig(
    val displayType: DisplayType,

    /**
     * Viewing distance in meters
     * - AR: 3-5m (windows float in space)
     * - LCD: 0.5-1m (screen at arm's length)
     */
    val viewingDistance: Float,

    /**
     * Field of view in degrees
     * - AR: 90-110° (wide for immersion)
     * - LCD: 60-75° (matches screen bounds)
     */
    val fovHorizontal: Float,
    val fovVertical: Float,

    /**
     * Curve strength (0.0 = flat, 1.0 = full cylindrical)
     * - AR: 0.8-1.0 (strong wrap)
     * - LCD: 0.2-0.4 (subtle perspective)
     */
    val curveStrength: Float,

    /**
     * Window size mode
     * - AR: PHYSICAL_METERS (0.7m × 0.55m at distance)
     * - LCD: SCREEN_PERCENTAGE (30% × 40% of screen)
     */
    val windowSizeMode: WindowSizeMode
) {
    companion object {
        /**
         * Default config for AR glasses
         */
        fun forARGlasses() = DisplayConfig(
            displayType = DisplayType.AR_GLASSES,
            viewingDistance = 3.0f,
            fovHorizontal = 90f,
            fovVertical = 70f,
            curveStrength = 0.9f,
            windowSizeMode = WindowSizeMode.PHYSICAL_METERS
        )

        /**
         * Default config for LCD screens (phones, tablets, monitors)
         *
         * Optimized for "pseudo-spatial" experience on 2D screens
         */
        fun forLCDScreen(screenType: ScreenType = ScreenType.PHONE) = DisplayConfig(
            displayType = DisplayType.LCD_SCREEN,
            viewingDistance = when (screenType) {
                ScreenType.PHONE -> 0.5f      // 50cm (arm's length for phone)
                ScreenType.TABLET -> 0.7f     // 70cm (comfortable tablet distance)
                ScreenType.MONITOR -> 0.8f    // 80cm (typical monitor distance)
                ScreenType.TV -> 2.0f         // 2m (TV viewing distance)
            },
            fovHorizontal = 65f,  // Narrower for LCD (screen bounds)
            fovVertical = 55f,
            curveStrength = 0.3f,  // Subtle curve for depth perception
            windowSizeMode = WindowSizeMode.SCREEN_PERCENTAGE
        )

        /**
         * Auto-detect optimal config based on screen size
         */
        fun autoDetect(
            screenWidthPx: Int,
            screenHeightPx: Int,
            hasARCapability: Boolean
        ): DisplayConfig {
            if (hasARCapability) {
                return forARGlasses()
            }

            // Detect screen type from dimensions
            val screenType = when {
                screenWidthPx < 800 -> ScreenType.PHONE
                screenWidthPx < 1400 -> ScreenType.TABLET
                screenWidthPx < 2400 -> ScreenType.MONITOR
                else -> ScreenType.TV
            }

            return forLCDScreen(screenType)
        }
    }
}

/**
 * Screen type for LCD displays
 */
enum class ScreenType {
    PHONE,      // < 800px width
    TABLET,     // 800-1400px
    MONITOR,    // 1400-2400px
    TV          // > 2400px
}

/**
 * Window sizing mode
 */
enum class WindowSizeMode {
    /**
     * Physical meters at viewing distance (AR mode)
     * Example: 0.7m × 0.55m at 3m distance
     */
    PHYSICAL_METERS,

    /**
     * Percentage of screen dimensions (LCD mode)
     * Example: 30% width × 40% height
     */
    SCREEN_PERCENTAGE
}
