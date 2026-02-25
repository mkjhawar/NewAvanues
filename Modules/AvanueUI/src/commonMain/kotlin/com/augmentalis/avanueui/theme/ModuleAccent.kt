/**
 * ModuleAccent.kt - Per-module accent color system
 *
 * Allows modules to inherit theme-derived accent colors or use custom overrides.
 * Dual-access: Compose consumers use AvanueTheme.moduleAccent(id),
 * non-Compose consumers (Services, Canvas) use AvanueModuleAccents.get(id).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.avanueui.theme

import androidx.compose.ui.graphics.Color

/**
 * Per-module accent color set.
 * Modules can inherit from AvanueTheme or use custom overrides.
 */
data class ModuleAccent(
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val isCustom: Boolean = false
)

/**
 * Static registry for module accent colors.
 *
 * Compose consumers: Use AvanueTheme.moduleAccent(id)
 * Non-Compose consumers (Services, Canvas): Use AvanueModuleAccents.get(id)
 *
 * Global colors are synced from AvanueThemeProvider via SideEffect so that
 * non-Compose code always gets the current theme colors.
 */
object AvanueModuleAccents {
    private val overrides = mutableMapOf<String, ModuleAccent>()

    @kotlin.concurrent.Volatile private var globalAccent: Color = Color(0xFF007AFF)
    @kotlin.concurrent.Volatile private var globalOnAccent: Color = Color.White
    @kotlin.concurrent.Volatile private var globalAccentMuted: Color = Color(0xFF007AFF).copy(alpha = 0.6f)

    /** Called from AvanueThemeProvider SideEffect on each recomposition */
    fun setGlobalColors(accent: Color, onAccent: Color, accentMuted: Color) {
        this.globalAccent = accent
        this.globalOnAccent = onAccent
        this.globalAccentMuted = accentMuted
    }

    /** Set a custom accent for a module */
    fun setOverride(moduleId: String, accent: ModuleAccent) {
        overrides[moduleId] = accent
    }

    /** Clear custom accent (reverts to theme) */
    fun clearOverride(moduleId: String) {
        overrides.remove(moduleId)
    }

    /** Resolved accent for a module (custom if set, else global theme) */
    fun get(moduleId: String): ModuleAccent {
        return overrides[moduleId] ?: ModuleAccent(
            accent = globalAccent,
            onAccent = globalOnAccent,
            accentMuted = globalAccentMuted,
            isCustom = false
        )
    }

    /** Get accent color as Android ARGB int (for Canvas/Paint consumers) */
    fun getAccentArgb(moduleId: String): Int = get(moduleId).accent.toArgbInt()

    /** Get onAccent color as Android ARGB int (for Canvas/Paint consumers) */
    fun getOnAccentArgb(moduleId: String): Int = get(moduleId).onAccent.toArgbInt()

    /** Get accentMuted color as Android ARGB int (for Canvas/Paint consumers) */
    fun getAccentMutedArgb(moduleId: String): Int = get(moduleId).accentMuted.toArgbInt()
}

/** Convert Compose Color to Android ARGB int (compatible with android.graphics.Color) */
private fun Color.toArgbInt(): Int {
    val a = (alpha * 255 + 0.5f).toInt() and 0xFF
    val r = (red * 255 + 0.5f).toInt() and 0xFF
    val g = (green * 255 + 0.5f).toInt() and 0xFF
    val b = (blue * 255 + 0.5f).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
