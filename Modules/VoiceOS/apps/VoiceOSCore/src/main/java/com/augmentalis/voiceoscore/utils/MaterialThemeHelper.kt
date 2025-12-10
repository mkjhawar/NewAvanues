/**
 * MaterialThemeHelper.kt - Theme wrapper for Material3 view inflation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Provides themed context for inflating Material3 views in AccessibilityService context.
 *
 * ## Problem
 * AccessibilityService uses the app's default theme (Theme.AppCompat), but Material3
 * views (MaterialCardView, MaterialButton, CircularProgressIndicator) require
 * Theme.MaterialComponents or Theme.Material3 as theme parent.
 *
 * ## Solution
 * Use ContextThemeWrapper to wrap the service context with Material3 theme
 * specifically for view inflation, without affecting the rest of the app.
 *
 * ## Usage
 * ```kotlin
 * // Instead of:
 * val view = LayoutInflater.from(context).inflate(R.layout.my_layout, null)
 *
 * // Use:
 * val view = MaterialThemeHelper.inflate(context, R.layout.my_layout)
 *
 * // Or get themed context for multiple operations:
 * val themedContext = MaterialThemeHelper.getThemedContext(context)
 * ```
 */
package com.augmentalis.voiceoscore.utils

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * Helper object for inflating Material3 views in non-Material themed contexts.
 *
 * This is necessary when:
 * - Inflating views from AccessibilityService
 * - Creating overlay windows with Material components
 * - Any context that doesn't inherit from Theme.MaterialComponents
 */
object MaterialThemeHelper {

    /**
     * Wrap context with Material3 theme for view inflation.
     *
     * @param context Base context (e.g., AccessibilityService)
     * @return Context wrapped with Material3 theme
     */
    fun getThemedContext(context: Context): Context {
        return ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
    }

    /**
     * Get LayoutInflater with Material3 theme applied.
     *
     * @param context Base context
     * @return LayoutInflater that can inflate Material3 views
     */
    fun getInflater(context: Context): LayoutInflater {
        return LayoutInflater.from(getThemedContext(context))
    }

    /**
     * Inflate a layout with Material3 theme applied.
     *
     * @param context Base context
     * @param layoutRes Layout resource ID
     * @param root Optional root ViewGroup
     * @param attachToRoot Whether to attach inflated view to root
     * @return Inflated view
     */
    fun inflate(
        context: Context,
        @LayoutRes layoutRes: Int,
        root: ViewGroup? = null,
        attachToRoot: Boolean = false
    ): View {
        return getInflater(context).inflate(layoutRes, root, attachToRoot)
    }

    /**
     * Inflate a layout without attaching to root.
     *
     * Convenience method for the common case of inflating overlay views.
     *
     * @param context Base context
     * @param layoutRes Layout resource ID
     * @return Inflated view
     */
    fun inflateOverlay(context: Context, @LayoutRes layoutRes: Int): View {
        return getInflater(context).inflate(layoutRes, null)
    }
}
