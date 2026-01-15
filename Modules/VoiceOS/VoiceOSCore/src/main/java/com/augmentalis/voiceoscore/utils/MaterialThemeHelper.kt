/**
 * MaterialThemeHelper.kt - Helper for Material theme in AccessibilityService context
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-17
 *
 * Provides Material theme-wrapped context and inflation helpers for overlays
 * used in AccessibilityService context where theming is not automatically available.
 */
package com.augmentalis.voiceoscore.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper

/**
 * Material Theme Helper
 *
 * Provides utility functions for inflating views with Material theme in
 * non-Activity contexts like AccessibilityService.
 */
object MaterialThemeHelper {

    /**
     * Create a Material-themed context wrapper
     *
     * @param context Base context
     * @return ContextThemeWrapper with Material3 theme applied
     */
    fun createThemedContext(context: Context): Context {
        return ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_Material3_DayNight
        )
    }

    /**
     * Inflate a layout with Material theme applied
     *
     * @param context Base context
     * @param layoutRes Layout resource ID
     * @param parent Optional parent ViewGroup
     * @param attachToParent Whether to attach to parent
     * @return Inflated View with Material theme
     */
    fun inflateOverlay(
        context: Context,
        layoutRes: Int,
        parent: ViewGroup? = null,
        attachToParent: Boolean = false
    ): View {
        val themedContext = createThemedContext(context)
        val inflater = LayoutInflater.from(themedContext)
        return inflater.inflate(layoutRes, parent, attachToParent)
    }

    /**
     * Get a themed LayoutInflater
     *
     * @param context Base context
     * @return LayoutInflater with Material theme
     */
    fun getThemedInflater(context: Context): LayoutInflater {
        val themedContext = createThemedContext(context)
        return LayoutInflater.from(themedContext)
    }
}
