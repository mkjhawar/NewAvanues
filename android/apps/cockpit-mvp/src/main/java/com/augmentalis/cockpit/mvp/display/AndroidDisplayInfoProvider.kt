package com.augmentalis.cockpit.mvp.display

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics as AndroidDisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.avanues.cockpit.core.display.*
import com.google.ar.core.ArCoreApk
import kotlin.math.sqrt

/**
 * Android implementation of DisplayInfoProvider
 *
 * Uses Android platform APIs:
 * - DisplayManager: Primary display information source
 * - WindowManager: Legacy fallback for older APIs
 * - ArCoreApk: AR capability detection
 * - Configuration: Orientation and UI mode
 *
 * **Public API for Android developers:**
 * ```kotlin
 * val displayProvider = AndroidDisplayInfoProvider(context)
 * val metrics = displayProvider.getDisplayMetrics()
 * val config = displayProvider.getOptimalDisplayConfig()
 *
 * // Listen for changes
 * displayProvider.addDisplayConfigListener { metrics, config ->
 *     // Update spatial rendering
 * }
 * ```
 *
 * **Thread-safe:** All methods can be called from any thread
 * **Lifecycle:** Register/unregister listeners in onResume/onPause
 */
class AndroidDisplayInfoProvider(
    private val context: Context
) : DisplayInfoProvider {

    private val displayManager: DisplayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val listeners = mutableSetOf<DisplayConfigListener>()

    // Display listener for configuration changes
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            notifyListeners()
        }

        override fun onDisplayRemoved(displayId: Int) {
            notifyListeners()
        }

        override fun onDisplayChanged(displayId: Int) {
            notifyListeners()
        }
    }

    init {
        // Register for display changes
        displayManager.registerDisplayListener(displayListener, null)
    }

    override fun getDisplayMetrics(): com.avanues.cockpit.core.display.DisplayMetrics {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }

        val androidMetrics = AndroidDisplayMetrics()
        display.getRealMetrics(androidMetrics)

        // Calculate physical size
        val widthInches = androidMetrics.widthPixels / androidMetrics.xdpi
        val heightInches = androidMetrics.heightPixels / androidMetrics.ydpi
        val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)

        // Detect orientation
        val orientation = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> DisplayOrientation.PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE -> DisplayOrientation.LANDSCAPE
            else -> if (androidMetrics.heightPixels > androidMetrics.widthPixels) {
                DisplayOrientation.PORTRAIT
            } else {
                DisplayOrientation.LANDSCAPE
            }
        }

        // Detect AR capability
        val hasAR = try {
            ArCoreApk.getInstance().checkAvailability(context) == ArCoreApk.Availability.SUPPORTED_INSTALLED
        } catch (e: Exception) {
            false
        }

        // Auto-detect category from physical size
        val category = when {
            hasAR -> DisplayCategory.AR_GLASSES
            diagonalInches < 7f -> DisplayCategory.PHONE
            diagonalInches < 13f -> DisplayCategory.TABLET
            diagonalInches < 32f -> DisplayCategory.MONITOR
            else -> DisplayCategory.TV
        }

        // Get refresh rate
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.mode.refreshRate
        } else {
            @Suppress("DEPRECATION")
            display.refreshRate
        }

        return com.avanues.cockpit.core.display.DisplayMetrics(
            widthPx = androidMetrics.widthPixels,
            heightPx = androidMetrics.heightPixels,
            densityDpi = androidMetrics.densityDpi.toFloat(),
            densityScale = androidMetrics.density,
            physicalSizeInches = diagonalInches,
            refreshRateHz = refreshRate,
            isExternalDisplay = display.displayId != Display.DEFAULT_DISPLAY,
            orientation = orientation,
            hasARCapability = hasAR,
            category = category
        )
    }

    override fun detectDisplayType(): DisplayType {
        val metrics = getDisplayMetrics()
        return if (metrics.hasARCapability) {
            DisplayType.AR_GLASSES
        } else {
            DisplayType.LCD_SCREEN
        }
    }

    override fun addDisplayConfigListener(listener: DisplayConfigListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    override fun removeDisplayConfigListener(listener: DisplayConfigListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun notifyListeners() {
        val metrics = getDisplayMetrics()
        val config = getOptimalDisplayConfig()

        synchronized(listeners) {
            listeners.forEach { it.onDisplayConfigChanged(metrics, config) }
        }
    }

    /**
     * Clean up resources
     * Call in Activity.onDestroy()
     */
    fun dispose() {
        displayManager.unregisterDisplayListener(displayListener)
        synchronized(listeners) {
            listeners.clear()
        }
    }
}
