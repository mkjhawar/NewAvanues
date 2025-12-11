package com.avanues.cockpit.core.display

/**
 * Display information provider interface
 *
 * Platform-agnostic interface for retrieving display characteristics.
 * Implementations use platform-specific APIs:
 * - Android: DisplayManager, WindowManager
 * - iOS: UIScreen, UIDevice
 * - Desktop: GraphicsEnvironment, GraphicsDevice
 *
 * **Public API for developers:**
 * - Inject this interface into your spatial rendering components
 * - Platform provides concrete implementation
 * - Mockable for testing
 *
 * @see AndroidDisplayInfoProvider for Android implementation
 */
interface DisplayInfoProvider {

    /**
     * Get current display metrics
     *
     * Returns real-time display information including size, density, refresh rate.
     * Updates automatically on configuration changes (rotation, external display).
     *
     * @return Current display metrics
     */
    fun getDisplayMetrics(): DisplayMetrics

    /**
     * Detect display type (AR vs LCD)
     *
     * Uses platform-specific heuristics:
     * - Android: Check for AR capability (ARCore support)
     * - iOS: Check for AR capability (ARKit support)
     * - Desktop: Always LCD
     *
     * @return Detected display type
     */
    fun detectDisplayType(): DisplayType

    /**
     * Get optimal display configuration for current device
     *
     * Combines display metrics with type detection to provide
     * optimized configuration for spatial rendering.
     *
     * @return Optimal display configuration
     */
    fun getOptimalDisplayConfig(): DisplayConfig {
        val metrics = getDisplayMetrics()
        val displayType = detectDisplayType()

        return DisplayConfig.autoDetect(
            screenWidthPx = metrics.widthPx,
            screenHeightPx = metrics.heightPx,
            hasARCapability = displayType == DisplayType.AR_GLASSES
        )
    }

    /**
     * Register listener for display configuration changes
     *
     * Notifies when display characteristics change:
     * - Rotation (portrait ↔ landscape)
     * - External display connected/disconnected
     * - Display mode change (AR ↔ LCD)
     *
     * @param listener Callback for configuration changes
     */
    fun addDisplayConfigListener(listener: DisplayConfigListener)

    /**
     * Unregister display configuration listener
     *
     * @param listener Listener to remove
     */
    fun removeDisplayConfigListener(listener: DisplayConfigListener)
}

/**
 * Display metrics
 *
 * Platform-agnostic display characteristics.
 * Populated from platform-specific sources:
 * - Android: DisplayMetrics, Display
 * - iOS: UIScreen.main
 * - Desktop: GraphicsDevice
 */
data class DisplayMetrics(
    /**
     * Screen width in pixels
     */
    val widthPx: Int,

    /**
     * Screen height in pixels
     */
    val heightPx: Int,

    /**
     * Screen density in DPI
     * - Android: densityDpi
     * - iOS: scale * 160 (Retina = 320, standard = 160)
     * - Desktop: Screen DPI from GraphicsConfiguration
     */
    val densityDpi: Float,

    /**
     * Screen density scale factor
     * - Android: density (1.0 = mdpi, 2.0 = xhdpi, etc.)
     * - iOS: scale (1.0 = standard, 2.0 = Retina, 3.0 = Retina HD)
     */
    val densityScale: Float,

    /**
     * Physical screen size in inches (diagonal)
     * Calculated from width/height pixels and DPI
     */
    val physicalSizeInches: Float,

    /**
     * Refresh rate in Hz (60, 90, 120, etc.)
     */
    val refreshRateHz: Float,

    /**
     * Is this an external display?
     * - Android: Display.FLAG_PRESENTATION
     * - iOS: UIScreen != UIScreen.main
     * - Desktop: Multiple monitors
     */
    val isExternalDisplay: Boolean,

    /**
     * Display orientation
     */
    val orientation: DisplayOrientation,

    /**
     * Has AR capability (ARCore/ARKit)
     */
    val hasARCapability: Boolean,

    /**
     * Display category hint
     * Auto-detected from physical size and density
     */
    val category: DisplayCategory
) {
    /**
     * Aspect ratio (width / height)
     */
    val aspectRatio: Float
        get() = widthPx.toFloat() / heightPx.toFloat()

    /**
     * Is portrait orientation?
     */
    val isPortrait: Boolean
        get() = heightPx > widthPx

    /**
     * Is landscape orientation?
     */
    val isLandscape: Boolean
        get() = widthPx > heightPx
}

/**
 * Display orientation
 */
enum class DisplayOrientation {
    PORTRAIT,
    LANDSCAPE,
    PORTRAIT_REVERSE,
    LANDSCAPE_REVERSE
}

/**
 * Display category (auto-detected)
 */
enum class DisplayCategory {
    /**
     * Phone: < 7" diagonal
     */
    PHONE,

    /**
     * Tablet: 7" - 13" diagonal
     */
    TABLET,

    /**
     * Monitor: 13" - 32" diagonal
     */
    MONITOR,

    /**
     * TV: > 32" diagonal
     */
    TV,

    /**
     * AR Glasses: Transparent display
     */
    AR_GLASSES
}

/**
 * Display configuration change listener
 */
fun interface DisplayConfigListener {
    /**
     * Called when display configuration changes
     *
     * @param newMetrics Updated display metrics
     * @param newConfig Updated display configuration
     */
    fun onDisplayConfigChanged(newMetrics: DisplayMetrics, newConfig: DisplayConfig)
}
