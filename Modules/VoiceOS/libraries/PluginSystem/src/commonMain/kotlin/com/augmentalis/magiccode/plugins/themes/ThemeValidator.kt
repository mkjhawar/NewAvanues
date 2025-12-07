package com.augmentalis.magiccode.plugins.themes

import com.augmentalis.magiccode.plugins.core.PluginLog

/**
 * Validator for theme definitions.
 *
 * Validates YAML schema, color formats, typography, spacing, and effects.
 * Implements FR-005 (Theme definition with strict YAML schema).
 */
class ThemeValidator {
    companion object {
        private const val TAG = "ThemeValidator"

        // Color format patterns
        private val HEX_COLOR_PATTERN = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}|[A-Fa-f0-9]{8})$")
        private val RGB_PATTERN = Regex("^rgb\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)$")
        private val RGBA_PATTERN = Regex("^rgba\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*[0-9.]+\\s*\\)$")

        // Font family validation
        private val VALID_FONT_FAMILIES = setOf(
            "system", "system-ui", "sans-serif", "serif", "monospace",
            "sf pro", "roboto", "arial", "helvetica", "times new roman"
        )

        // Font weight range
        private val VALID_FONT_WEIGHT_RANGE = 100..900

        // Size constraints
        private const val MIN_FONT_SIZE = 8
        private const val MAX_FONT_SIZE = 72
        private const val MIN_SPACING = 0
        private const val MAX_SPACING = 100
    }

    /**
     * Validation result.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    }

    /**
     * Validation error.
     */
    data class ValidationError(
        val field: String,
        val message: String,
        val severity: Severity = Severity.ERROR
    )

    /**
     * Error severity.
     */
    enum class Severity {
        ERROR,   // Must fix
        WARNING  // Should fix
    }

    /**
     * Validate complete theme definition.
     *
     * @param theme Theme definition to validate
     * @return ValidationResult
     */
    fun validate(theme: ThemeDefinition): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate basic structure
        errors.addAll(validateBasicFields(theme))

        // Validate colors
        errors.addAll(validateColors(theme.colors))

        // Validate typography
        errors.addAll(validateTypography(theme.typography))

        // Validate spacing
        errors.addAll(validateSpacing(theme.spacing))

        // Validate effects (if present)
        theme.effects?.let { effects ->
            errors.addAll(validateEffects(effects))
        }

        return if (errors.isEmpty()) {
            PluginLog.i(TAG, "Theme validation passed: ${theme.name}")
            ValidationResult.Valid
        } else {
            PluginLog.w(TAG, "Theme validation failed: ${theme.name} (${errors.size} errors)")
            ValidationResult.Invalid(errors)
        }
    }

    /**
     * Validate basic theme fields.
     */
    private fun validateBasicFields(theme: ThemeDefinition): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (theme.name.isBlank()) {
            errors.add(ValidationError("name", "Theme name must not be empty"))
        }

        if (theme.version.isBlank()) {
            errors.add(ValidationError("version", "Theme version must not be empty"))
        } else if (!theme.version.matches(Regex("^\\d+\\.\\d+\\.\\d+.*$"))) {
            errors.add(ValidationError("version", "Theme version must follow semver format"))
        }

        return errors
    }

    /**
     * Validate color palette.
     *
     * Checks hex, RGB, RGBA formats.
     */
    private fun validateColors(colors: ColorPalette): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate required colors
        validateColor("colors.primary", colors.primary, errors)
        validateColor("colors.secondary", colors.secondary, errors)
        validateColor("colors.background", colors.background, errors)
        validateColor("colors.text", colors.text, errors)

        // Validate optional colors
        colors.surface?.let { validateColor("colors.surface", it, errors) }
        colors.textSecondary?.let { validateColor("colors.textSecondary", it, errors) }
        colors.accent?.let { validateColor("colors.accent", it, errors) }
        colors.error?.let { validateColor("colors.error", it, errors) }
        colors.warning?.let { validateColor("colors.warning", it, errors) }
        colors.success?.let { validateColor("colors.success", it, errors) }
        colors.info?.let { validateColor("colors.info", it, errors) }
        colors.border?.let { validateColor("colors.border", it, errors) }
        colors.divider?.let { validateColor("colors.divider", it, errors) }

        // Validate custom colors
        colors.custom.forEach { (key, value) ->
            validateColor("colors.custom.$key", value, errors)
        }

        return errors
    }

    /**
     * Validate single color value.
     */
    private fun validateColor(field: String, color: String, errors: MutableList<ValidationError>) {
        val isValid = HEX_COLOR_PATTERN.matches(color) ||
                      RGB_PATTERN.matches(color) ||
                      RGBA_PATTERN.matches(color)

        if (!isValid) {
            errors.add(
                ValidationError(
                    field,
                    "Invalid color format: $color (must be hex, rgb, or rgba)"
                )
            )
        }
    }

    /**
     * Validate typography settings.
     */
    private fun validateTypography(typography: Typography): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate font family
        if (typography.fontFamily.isBlank()) {
            errors.add(ValidationError("typography.fontFamily", "Font family must not be empty"))
        }

        // Validate font sizes
        errors.addAll(validateFontSizes(typography.fontSize))

        // Validate font weights (if present)
        typography.fontWeight?.let { weights ->
            errors.addAll(validateFontWeights(weights))
        }

        // Validate line heights (if present)
        typography.lineHeight?.let { lineHeights ->
            errors.addAll(validateLineHeights(lineHeights))
        }

        return errors
    }

    /**
     * Validate font sizes.
     */
    private fun validateFontSizes(sizes: FontSizes): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        validateFontSize("typography.fontSize.small", sizes.small, errors)
        validateFontSize("typography.fontSize.medium", sizes.medium, errors)
        validateFontSize("typography.fontSize.large", sizes.large, errors)

        sizes.xs?.let { validateFontSize("typography.fontSize.xs", it, errors) }
        sizes.xl?.let { validateFontSize("typography.fontSize.xl", it, errors) }
        sizes.xxl?.let { validateFontSize("typography.fontSize.xxl", it, errors) }

        // Validate size progression (small < medium < large)
        if (sizes.small >= sizes.medium) {
            errors.add(
                ValidationError(
                    "typography.fontSize",
                    "Font size small (${sizes.small}) must be less than medium (${sizes.medium})",
                    Severity.WARNING
                )
            )
        }

        if (sizes.medium >= sizes.large) {
            errors.add(
                ValidationError(
                    "typography.fontSize",
                    "Font size medium (${sizes.medium}) must be less than large (${sizes.large})",
                    Severity.WARNING
                )
            )
        }

        return errors
    }

    /**
     * Validate single font size.
     */
    private fun validateFontSize(field: String, size: Int, errors: MutableList<ValidationError>) {
        if (size < MIN_FONT_SIZE || size > MAX_FONT_SIZE) {
            errors.add(
                ValidationError(
                    field,
                    "Font size $size out of range ($MIN_FONT_SIZE-$MAX_FONT_SIZE)"
                )
            )
        }
    }

    /**
     * Validate font weights.
     */
    private fun validateFontWeights(weights: FontWeights): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        fun validateWeight(field: String, weight: Int) {
            if (weight !in VALID_FONT_WEIGHT_RANGE) {
                errors.add(
                    ValidationError(
                        field,
                        "Font weight $weight out of range (${VALID_FONT_WEIGHT_RANGE.first}-${VALID_FONT_WEIGHT_RANGE.last})"
                    )
                )
            }
            if (weight % 100 != 0) {
                errors.add(
                    ValidationError(
                        field,
                        "Font weight $weight should be multiple of 100",
                        Severity.WARNING
                    )
                )
            }
        }

        weights.light?.let { validateWeight("typography.fontWeight.light", it) }
        validateWeight("typography.fontWeight.regular", weights.regular)
        weights.medium?.let { validateWeight("typography.fontWeight.medium", it) }
        weights.semibold?.let { validateWeight("typography.fontWeight.semibold", it) }
        validateWeight("typography.fontWeight.bold", weights.bold)
        weights.extrabold?.let { validateWeight("typography.fontWeight.extrabold", it) }

        return errors
    }

    /**
     * Validate line heights.
     */
    private fun validateLineHeights(lineHeights: LineHeights): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        fun validateLineHeight(field: String, lineHeight: Double) {
            if (lineHeight < 0.5 || lineHeight > 3.0) {
                errors.add(
                    ValidationError(
                        field,
                        "Line height $lineHeight out of reasonable range (0.5-3.0)",
                        Severity.WARNING
                    )
                )
            }
        }

        lineHeights.tight?.let { validateLineHeight("typography.lineHeight.tight", it) }
        validateLineHeight("typography.lineHeight.normal", lineHeights.normal)
        lineHeights.relaxed?.let { validateLineHeight("typography.lineHeight.relaxed", it) }
        lineHeights.loose?.let { validateLineHeight("typography.lineHeight.loose", it) }

        return errors
    }

    /**
     * Validate spacing scale.
     */
    private fun validateSpacing(spacing: Spacing): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        fun validateSpacingValue(field: String, value: Int) {
            if (value < MIN_SPACING || value > MAX_SPACING) {
                errors.add(
                    ValidationError(
                        field,
                        "Spacing value $value out of range ($MIN_SPACING-$MAX_SPACING)"
                    )
                )
            }
        }

        validateSpacingValue("spacing.xs", spacing.xs)
        validateSpacingValue("spacing.sm", spacing.sm)
        validateSpacingValue("spacing.md", spacing.md)
        validateSpacingValue("spacing.lg", spacing.lg)
        validateSpacingValue("spacing.xl", spacing.xl)
        spacing.xxl?.let { validateSpacingValue("spacing.xxl", it) }

        // Validate progression
        if (spacing.xs >= spacing.sm || spacing.sm >= spacing.md ||
            spacing.md >= spacing.lg || spacing.lg >= spacing.xl) {
            errors.add(
                ValidationError(
                    "spacing",
                    "Spacing values should follow progression: xs < sm < md < lg < xl",
                    Severity.WARNING
                )
            )
        }

        return errors
    }

    /**
     * Validate visual effects.
     */
    private fun validateEffects(effects: Effects): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate border radius
        effects.borderRadius?.let { borderRadius ->
            fun validateRadius(field: String, value: Int) {
                if (value < 0 || value > 100) {
                    errors.add(
                        ValidationError(
                            field,
                            "Border radius $value out of range (0-100)",
                            Severity.WARNING
                        )
                    )
                }
            }

            validateRadius("effects.borderRadius.none", borderRadius.none)
            validateRadius("effects.borderRadius.small", borderRadius.small)
            validateRadius("effects.borderRadius.medium", borderRadius.medium)
            validateRadius("effects.borderRadius.large", borderRadius.large)
            borderRadius.xl?.let { validateRadius("effects.borderRadius.xl", it) }
        }

        // Validate opacity
        effects.opacity?.let { opacity ->
            fun validateOpacityValue(field: String, value: Double) {
                if (value < 0.0 || value > 1.0) {
                    errors.add(
                        ValidationError(
                            field,
                            "Opacity $value out of range (0.0-1.0)"
                        )
                    )
                }
            }

            validateOpacityValue("effects.opacity.transparent", opacity.transparent)
            validateOpacityValue("effects.opacity.semiTransparent", opacity.semiTransparent)
            validateOpacityValue("effects.opacity.opaque", opacity.opaque)
        }

        return errors
    }
}
