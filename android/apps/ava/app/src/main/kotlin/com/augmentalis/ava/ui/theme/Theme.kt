// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/Theme.kt
// created: 2025-11-02 15:34:00 -0800
// Updated: 2025-11-04 - Migrated to universal theme engine
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.augmentalis.ava.core.theme.AvaTheme as UniversalAvaTheme

/**
 * AvaTheme for ava-standalone app
 *
 * This is now a thin wrapper around the Universal AvaTheme from Core:Theme module.
 * All theme tokens (colors, spacing, shapes, etc.) are centralized in the universal module.
 *
 * The wrapper handles Android-specific UI configuration (status bar, navigation bar).
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to enable Material You dynamic colors (Android 12+)
 * @param content App content
 */
@Composable
fun AvaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Configure system UI (status bar, navigation bar)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Use universal theme
    UniversalAvaTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
