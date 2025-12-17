/**
 * MaterialThemeHelper.kt - Material Design theme utilities for LearnApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Provides Material Design 3 theme utilities for LearnApp overlays and dialogs.
 * Ensures consistent theming across all UI components.
 */
package com.augmentalis.voiceoscore.learnapp.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.augmentalis.voiceoscore.R

/**
 * Material Theme Helper
 *
 * Utility class for applying Material Design 3 theming to LearnApp components.
 * Handles dynamic colors, dark mode, and consistent styling.
 */
object MaterialThemeHelper {
    private const val TAG = "MaterialThemeHelper"

    /**
     * Create a themed context wrapper for dialogs and overlays.
     *
     * @param context Base context
     * @param themeResId Optional theme resource ID override
     * @return Themed context wrapper
     */
    fun createThemedContext(
        context: Context,
        @StyleRes themeResId: Int = com.google.android.material.R.style.Theme_Material3_DayNight
    ): Context {
        return ContextThemeWrapper(context, themeResId)
    }

    /**
     * Check if dark mode is currently active.
     *
     * @param context Context to check
     * @return true if dark mode is enabled
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Get a color from theme attribute.
     *
     * @param context Themed context
     * @param attrResId Attribute resource ID (e.g., R.attr.colorPrimary)
     * @param defaultColor Default color if attribute not found
     * @return Resolved color
     */
    @ColorInt
    fun getThemeColor(
        context: Context,
        @AttrRes attrResId: Int,
        @ColorInt defaultColor: Int = Color.BLACK
    ): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attrResId, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                context.getColor(typedValue.resourceId)
            } else {
                typedValue.data
            }
        } else {
            defaultColor
        }
    }

    /**
     * Get primary color from theme.
     */
    @ColorInt
    fun getPrimaryColor(context: Context): Int {
        return getThemeColor(
            context,
            com.google.android.material.R.attr.colorPrimary,
            Color.parseColor("#6200EE")
        )
    }

    /**
     * Get secondary color from theme.
     */
    @ColorInt
    fun getSecondaryColor(context: Context): Int {
        return getThemeColor(
            context,
            com.google.android.material.R.attr.colorSecondary,
            Color.parseColor("#03DAC6")
        )
    }

    /**
     * Get surface color from theme.
     */
    @ColorInt
    fun getSurfaceColor(context: Context): Int {
        return getThemeColor(
            context,
            com.google.android.material.R.attr.colorSurface,
            if (isDarkMode(context)) Color.parseColor("#121212") else Color.WHITE
        )
    }

    /**
     * Get on-surface color (text on surface) from theme.
     */
    @ColorInt
    fun getOnSurfaceColor(context: Context): Int {
        return getThemeColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            if (isDarkMode(context)) Color.WHITE else Color.BLACK
        )
    }

    /**
     * Get error color from theme.
     */
    @ColorInt
    fun getErrorColor(context: Context): Int {
        return getThemeColor(
            context,
            com.google.android.material.R.attr.colorError,
            Color.parseColor("#B00020")
        )
    }

    /**
     * Get background color from theme.
     */
    @ColorInt
    fun getBackgroundColor(context: Context): Int {
        return getThemeColor(
            context,
            android.R.attr.colorBackground,
            if (isDarkMode(context)) Color.parseColor("#121212") else Color.WHITE
        )
    }

    /**
     * Apply elevation overlay color for dark mode surfaces.
     *
     * @param context Themed context
     * @param surfaceColor Base surface color
     * @param elevation Elevation in dp
     * @return Color with elevation overlay applied (dark mode only)
     */
    @ColorInt
    fun applyElevationOverlay(
        context: Context,
        @ColorInt surfaceColor: Int,
        elevation: Float
    ): Int {
        if (!isDarkMode(context)) {
            return surfaceColor
        }

        // Calculate overlay alpha based on elevation
        // Material Design formula: 4.5% at 1dp, increasing logarithmically
        val alpha = ((4.5f * Math.log(elevation.toDouble() + 1) + 2f) / 100f).toFloat()
            .coerceIn(0f, 0.16f)

        // Apply white overlay
        val overlayColor = Color.argb(
            (alpha * 255).toInt(),
            255, 255, 255
        )

        return blendColors(surfaceColor, overlayColor)
    }

    /**
     * Blend two colors together.
     */
    @ColorInt
    private fun blendColors(@ColorInt color1: Int, @ColorInt color2: Int): Int {
        val alpha2 = Color.alpha(color2) / 255f
        val alpha1 = 1f - alpha2

        val red = (Color.red(color1) * alpha1 + Color.red(color2) * alpha2).toInt()
        val green = (Color.green(color1) * alpha1 + Color.green(color2) * alpha2).toInt()
        val blue = (Color.blue(color1) * alpha1 + Color.blue(color2) * alpha2).toInt()

        return Color.rgb(red, green, blue)
    }

    /**
     * Apply Material shape styling to a view.
     *
     * @param view View to style
     * @param cornerRadius Corner radius in dp
     * @param elevation Elevation in dp
     */
    fun applyMaterialStyling(
        view: View,
        cornerRadius: Float = 8f,
        elevation: Float = 4f
    ) {
        val context = view.context
        val density = context.resources.displayMetrics.density

        view.elevation = elevation * density
        view.clipToOutline = true

        // Apply rounded corners via background shape
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadii = FloatArray(8) { cornerRadius * density }
            setColor(getSurfaceColor(context))
        }
        view.background = drawable
    }

    /**
     * Get text appearance style resource.
     *
     * @param style Text style (headline, body, label, etc.)
     * @return Style resource ID
     */
    @StyleRes
    fun getTextAppearance(style: TextStyle): Int {
        return when (style) {
            TextStyle.HEADLINE_LARGE -> com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
            TextStyle.HEADLINE_MEDIUM -> com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
            TextStyle.HEADLINE_SMALL -> com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
            TextStyle.TITLE_LARGE -> com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
            TextStyle.TITLE_MEDIUM -> com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
            TextStyle.TITLE_SMALL -> com.google.android.material.R.style.TextAppearance_Material3_TitleSmall
            TextStyle.BODY_LARGE -> com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            TextStyle.BODY_MEDIUM -> com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            TextStyle.BODY_SMALL -> com.google.android.material.R.style.TextAppearance_Material3_BodySmall
            TextStyle.LABEL_LARGE -> com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
            TextStyle.LABEL_MEDIUM -> com.google.android.material.R.style.TextAppearance_Material3_LabelMedium
            TextStyle.LABEL_SMALL -> com.google.android.material.R.style.TextAppearance_Material3_LabelSmall
        }
    }

    /**
     * Material Design 3 text styles.
     */
    enum class TextStyle {
        HEADLINE_LARGE,
        HEADLINE_MEDIUM,
        HEADLINE_SMALL,
        TITLE_LARGE,
        TITLE_MEDIUM,
        TITLE_SMALL,
        BODY_LARGE,
        BODY_MEDIUM,
        BODY_SMALL,
        LABEL_LARGE,
        LABEL_MEDIUM,
        LABEL_SMALL
    }
}
