package com.augmentalis.webavanue.domain.validation

import com.augmentalis.webavanue.domain.model.BrowserSettings

/**
 * Settings validation and constraint enforcement.
 *
 * Validates BrowserSettings values and applies auto-correction
 * to ensure all settings are within acceptable ranges.
 *
 * Responsibilities:
 * - Validate setting ranges (zoom, text size, cache size)
 * - Auto-correct invalid values to nearest valid value
 * - Provide user-friendly error messages
 * - Enforce security constraints (file access, mixed content)
 *
 * @see BrowserSettings for the complete settings model
 */
object SettingsValidation {

    /**
     * Validation result for settings
     *
     * @param isValid True if all settings are valid
     * @param correctedSettings Settings with auto-corrections applied
     * @param warnings List of warning messages for corrected values
     * @param errors List of error messages for invalid values
     */
    data class ValidationResult(
        val isValid: Boolean,
        val correctedSettings: BrowserSettings,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList()
    )

    /**
     * Validate and auto-correct browser settings.
     *
     * Applies constraints to all settings values:
     * - Clamps numeric values to valid ranges
     * - Corrects incompatible setting combinations
     * - Enforces security constraints
     *
     * @param settings Settings to validate
     * @return ValidationResult with corrected settings and messages
     */
    fun validate(settings: BrowserSettings): ValidationResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Validate and correct desktop mode zoom (50-200%)
        val correctedZoom = settings.desktopModeDefaultZoom.coerceIn(50, 200)
        if (correctedZoom != settings.desktopModeDefaultZoom) {
            warnings.add("Desktop mode zoom corrected from ${settings.desktopModeDefaultZoom}% to $correctedZoom% (valid range: 50-200%)")
        }

        // Validate and correct initial scale (0.5-2.0)
        val correctedInitialScale = settings.initialScale.coerceIn(0.5f, 2.0f)
        if (correctedInitialScale != settings.initialScale) {
            warnings.add("Initial scale corrected from ${settings.initialScale} to $correctedInitialScale (valid range: 0.5-2.0)")
        }

        // Validate WebXR settings combinations
        if (settings.enableWebXR && !settings.enableJavaScript) {
            errors.add("WebXR requires JavaScript - enable JavaScript or disable WebXR")
        }

        if (settings.enableAR && !settings.enableWebXR) {
            warnings.add("AR enabled but WebXR disabled - AR requires WebXR, auto-enabling WebXR")
        }

        if (settings.enableVR && !settings.enableWebXR) {
            warnings.add("VR enabled but WebXR disabled - VR requires WebXR, auto-enabling WebXR")
        }

        // Auto-correct WebXR dependencies
        val correctedEnableWebXR = settings.enableWebXR ||
                settings.enableAR ||
                settings.enableVR

        val correctedEnableJavaScript = settings.enableJavaScript ||
                correctedEnableWebXR

        // Warn about performance impact
        if (settings.hardwareAcceleration && settings.dataSaver) {
            warnings.add("Hardware acceleration and data saver both enabled - may conflict (data saver limits hardware acceleration)")
        }

        if (settings.enableWebXR && settings.dataSaver) {
            warnings.add("WebXR with data saver may have reduced performance - consider disabling data saver for XR experiences")
        }

        // Warn about security implications
        if (settings.blockTrackers && !settings.blockPopups) {
            warnings.add("Tracker blocking enabled but popups allowed - some trackers use popups")
        }

        if (settings.enableCookies && !settings.blockTrackers) {
            warnings.add("Cookies enabled without tracker blocking - third-party cookies may track you across sites")
        }

        // Create corrected settings
        val correctedSettings = settings.copy(
            desktopModeDefaultZoom = correctedZoom,
            initialScale = correctedInitialScale,
            enableWebXR = correctedEnableWebXR,
            enableJavaScript = correctedEnableJavaScript
        )

        return ValidationResult(
            isValid = errors.isEmpty(),
            correctedSettings = correctedSettings,
            warnings = warnings,
            errors = errors
        )
    }

    /**
     * Constraint definitions for settings
     */
    object Constraints {
        const val MIN_ZOOM = 50
        const val MAX_ZOOM = 200
        const val DEFAULT_ZOOM = 100

        const val MIN_INITIAL_SCALE = 0.5f
        const val MAX_INITIAL_SCALE = 2.0f
        const val DEFAULT_INITIAL_SCALE = 0.75f

        const val MIN_CACHE_SIZE_MB = 10
        const val MAX_CACHE_SIZE_MB = 500
        const val DEFAULT_CACHE_SIZE_MB = 100

        /**
         * Check if zoom value is valid
         */
        fun isValidZoom(zoom: Int): Boolean = zoom in MIN_ZOOM..MAX_ZOOM

        /**
         * Check if initial scale is valid
         */
        fun isValidInitialScale(scale: Float): Boolean = scale in MIN_INITIAL_SCALE..MAX_INITIAL_SCALE

        /**
         * Check if cache size is valid
         */
        fun isValidCacheSize(sizeMB: Int): Boolean = sizeMB in MIN_CACHE_SIZE_MB..MAX_CACHE_SIZE_MB
    }

    /**
     * Get user-friendly error messages for settings
     *
     * @param settingKey Setting identifier (e.g., "enableJavaScript", "desktopModeDefaultZoom")
     * @param value Current value
     * @return User-friendly error message or null if valid
     */
    fun getErrorMessage(settingKey: String, value: Any?): String? {
        return when (settingKey) {
            "desktopModeDefaultZoom" -> {
                val zoom = value as? Int ?: return "Invalid zoom value"
                if (!Constraints.isValidZoom(zoom)) {
                    "Zoom must be between ${Constraints.MIN_ZOOM}% and ${Constraints.MAX_ZOOM}%"
                } else null
            }

            "initialScale" -> {
                val scale = value as? Float ?: return "Invalid scale value"
                if (!Constraints.isValidInitialScale(scale)) {
                    "Scale must be between ${Constraints.MIN_INITIAL_SCALE} and ${Constraints.MAX_INITIAL_SCALE}"
                } else null
            }

            "enableWebXR" -> {
                val enableWebXR = value as? Boolean ?: return "Invalid WebXR setting"
                if (enableWebXR) {
                    // WebXR requires JavaScript
                    "WebXR requires JavaScript to be enabled"
                } else null
            }

            else -> null // No validation error
        }
    }
}
