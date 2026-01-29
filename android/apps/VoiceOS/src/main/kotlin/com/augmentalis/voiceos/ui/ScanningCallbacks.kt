package com.augmentalis.voiceos.ui

/**
 * Callbacks for scanning operations from UI.
 *
 * Used by DeveloperSettingsScreen to trigger accessibility service actions
 * like rescan and monitoring toggles.
 */
data class ScanningCallbacks(
    /**
     * Enable or disable continuous monitoring of screen changes.
     */
    val onSetContinuousMonitoring: (Boolean) -> Unit = {},

    /**
     * Rescan the current app (clear cache for current package).
     */
    val onRescanCurrentApp: () -> Unit = {},

    /**
     * Rescan everything (clear all cached screens).
     */
    val onRescanEverything: () -> Unit = {}
)
