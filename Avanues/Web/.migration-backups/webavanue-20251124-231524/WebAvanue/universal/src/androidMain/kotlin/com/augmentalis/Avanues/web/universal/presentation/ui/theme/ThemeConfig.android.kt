package com.augmentalis.Avanues.web.universal.presentation.ui.theme

import android.content.Context
import android.content.pm.PackageManager

/**
 * Android-specific theme configuration implementation
 */

// Platform-specific context holder (set by App/Activity)
private var appContext: Context? = null

/**
 * Initialize theme system with Android context
 * Call this from Application.onCreate() or MainActivity.onCreate()
 */
fun initializeThemeSystem(context: Context) {
    appContext = context.applicationContext
}

/**
 * ThemePreferences - Android implementation using SharedPreferences
 */
actual object ThemePreferences {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_THEME = "theme_type"

    private fun getPrefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?: error("ThemePreferences not initialized. Call initializeThemeSystem() first.")

    actual fun getTheme(): ThemeType? {
        val prefs = getPrefs()
        val themeName = prefs.getString(KEY_THEME, null) ?: return null
        return try {
            ThemeType.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    actual fun setTheme(theme: ThemeType) {
        getPrefs().edit().putString(KEY_THEME, theme.name).apply()
    }

    actual fun clearTheme() {
        getPrefs().edit().remove(KEY_THEME).apply()
    }
}

/**
 * ThemeDetector - Android implementation
 *
 * Detects Avanues ecosystem by checking for:
 * - com.avanues.launcher (Avanues Launcher)
 * - com.avanues.framework (Avanues Framework)
 * - com.ideahq.voiceos (VoiceOS)
 */
actual object ThemeDetector {
    private val avaPackages = listOf(
        "com.avanues.launcher",
        "com.avanues.framework",
        "com.ideahq.voiceos",
        "com.augmentalis.ava",
        "com.augmentalis.avaconnect"
    )

    actual fun isAvanuesEcosystem(): Boolean {
        val context = appContext ?: return false
        val packageManager = context.packageManager

        return avaPackages.any { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    actual fun detectTheme(): ThemeType {
        return if (isAvanuesEcosystem()) {
            ThemeType.AVAMAGIC
        } else {
            ThemeType.APP_BRANDING
        }
    }
}
