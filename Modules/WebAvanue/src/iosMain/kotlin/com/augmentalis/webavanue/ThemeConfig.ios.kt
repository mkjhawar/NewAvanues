package com.augmentalis.webavanue

import platform.Foundation.*

/**
 * iOS ThemePreferences implementation using UserDefaults
 */
actual object ThemePreferences {
    private const val THEME_KEY = "webavanue_theme_type"
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getTheme(): ThemeType? {
        val themeString = defaults.stringForKey(THEME_KEY) ?: return null
        return try {
            ThemeType.valueOf(themeString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    actual fun setTheme(theme: ThemeType) {
        defaults.setObject(theme.name, THEME_KEY)
        defaults.synchronize()
    }

    actual fun clearTheme() {
        defaults.removeObjectForKey(THEME_KEY)
        defaults.synchronize()
    }
}

/**
 * iOS ThemeDetector implementation
 *
 * Detects if running in Avanues ecosystem by checking for VoiceOS framework
 */
actual object ThemeDetector {
    actual fun isAvanuesEcosystem(): Boolean {
        // Check if VoiceOS framework is available
        // This would check for the presence of VoiceOS Swift/ObjC classes
        // For now, return false (standalone mode)
        return false
    }

    actual fun detectTheme(): ThemeType {
        return if (isAvanuesEcosystem()) {
            ThemeType.AVAMAGIC
        } else {
            ThemeType.APP_BRANDING
        }
    }
}
