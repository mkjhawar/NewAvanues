package com.augmentalis.avamagic.components.themebuilder.Engine

import com.augmentalis.avamagic.core.*
import com.augmentalis.avamagic.core.types.Color
import com.augmentalis.avamagic.core.types.Shadow
import com.augmentalis.avamagic.core.types.CornerRadius
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Export format for themes
 */
enum class ExportFormat {
    DSL,        // Kotlin DSL code
    YAML,       // YAML configuration
    JSON,       // JSON configuration
    CSS,        // CSS variables (for web)
    ANDROID_XML // Android theme XML
}

/**
 * Compiler for converting Theme objects to various output formats
 */
class ThemeCompiler {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Compile theme to Kotlin DSL code
     */
    fun compileToDSL(theme: Theme): String {
        return buildString {
            appendLine("// Generated Theme: ${theme.name}")
            appendLine("// Platform: ${theme.platform}")
            appendLine()
            appendLine("val ${theme.name.toCamelCase()} = Theme(")
            appendLine("    name = \"${theme.name}\",")
            appendLine("    platform = ThemePlatform.${theme.platform},")
            appendLine("    colorScheme = ColorScheme(")

            // Color scheme
            with(theme.colorScheme) {
                appendLine("        mode = ColorMode.${mode},")
                appendLine("        primary = ${colorToDSL(primary)},")
                appendLine("        onPrimary = ${colorToDSL(onPrimary)},")
                appendLine("        primaryContainer = ${colorToDSL(primaryContainer)},")
                appendLine("        onPrimaryContainer = ${colorToDSL(onPrimaryContainer)},")
                appendLine("        secondary = ${colorToDSL(secondary)},")
                appendLine("        onSecondary = ${colorToDSL(onSecondary)},")
                appendLine("        secondaryContainer = ${colorToDSL(secondaryContainer)},")
                appendLine("        onSecondaryContainer = ${colorToDSL(onSecondaryContainer)},")
                appendLine("        tertiary = ${colorToDSL(tertiary)},")
                appendLine("        onTertiary = ${colorToDSL(onTertiary)},")
                appendLine("        tertiaryContainer = ${colorToDSL(tertiaryContainer)},")
                appendLine("        onTertiaryContainer = ${colorToDSL(onTertiaryContainer)},")
                appendLine("        error = ${colorToDSL(error)},")
                appendLine("        onError = ${colorToDSL(onError)},")
                appendLine("        errorContainer = ${colorToDSL(errorContainer)},")
                appendLine("        onErrorContainer = ${colorToDSL(onErrorContainer)},")
                appendLine("        surface = ${colorToDSL(surface)},")
                appendLine("        onSurface = ${colorToDSL(onSurface)},")
                appendLine("        surfaceVariant = ${colorToDSL(surfaceVariant)},")
                appendLine("        onSurfaceVariant = ${colorToDSL(onSurfaceVariant)},")
                appendLine("        background = ${colorToDSL(background)},")
                appendLine("        onBackground = ${colorToDSL(onBackground)},")
                appendLine("        outline = ${colorToDSL(outline)},")
                appendLine("        outlineVariant = ${colorToDSL(outlineVariant)}")
            }
            appendLine("    ),")

            // Typography
            appendLine("    typography = Typography(")
            with(theme.typography) {
                appendLine("        displayLarge = ${fontToDSL(displayLarge)},")
                appendLine("        displayMedium = ${fontToDSL(displayMedium)},")
                appendLine("        displaySmall = ${fontToDSL(displaySmall)},")
                appendLine("        headlineLarge = ${fontToDSL(headlineLarge)},")
                appendLine("        headlineMedium = ${fontToDSL(headlineMedium)},")
                appendLine("        headlineSmall = ${fontToDSL(headlineSmall)},")
                appendLine("        titleLarge = ${fontToDSL(titleLarge)},")
                appendLine("        titleMedium = ${fontToDSL(titleMedium)},")
                appendLine("        titleSmall = ${fontToDSL(titleSmall)},")
                appendLine("        bodyLarge = ${fontToDSL(bodyLarge)},")
                appendLine("        bodyMedium = ${fontToDSL(bodyMedium)},")
                appendLine("        bodySmall = ${fontToDSL(bodySmall)},")
                appendLine("        labelLarge = ${fontToDSL(labelLarge)},")
                appendLine("        labelMedium = ${fontToDSL(labelMedium)},")
                appendLine("        labelSmall = ${fontToDSL(labelSmall)}")
            }
            appendLine("    ),")

            // Shapes
            appendLine("    shapes = Shapes(")
            with(theme.shapes) {
                appendLine("        extraSmall = ${cornerRadiusToDSL(extraSmall)},")
                appendLine("        small = ${cornerRadiusToDSL(small)},")
                appendLine("        medium = ${cornerRadiusToDSL(medium)},")
                appendLine("        large = ${cornerRadiusToDSL(large)},")
                appendLine("        extraLarge = ${cornerRadiusToDSL(extraLarge)}")
            }
            appendLine("    ),")

            // Spacing
            appendLine("    spacing = SpacingScale(")
            with(theme.spacing) {
                appendLine("        xs = ${xs}f,")
                appendLine("        sm = ${sm}f,")
                appendLine("        md = ${md}f,")
                appendLine("        lg = ${lg}f,")
                appendLine("        xl = ${xl}f,")
                appendLine("        xxl = ${xxl}f")
            }
            appendLine("    ),")

            // Elevation
            appendLine("    elevation = ElevationScale(")
            with(theme.elevation) {
                appendLine("        level0 = ${shadowToDSL(level0)},")
                appendLine("        level1 = ${shadowToDSL(level1)},")
                appendLine("        level2 = ${shadowToDSL(level2)},")
                appendLine("        level3 = ${shadowToDSL(level3)},")
                appendLine("        level4 = ${shadowToDSL(level4)},")
                appendLine("        level5 = ${shadowToDSL(level5)}")
            }
            appendLine("    )")

            // Material system (if present)
            theme.material?.let { material ->
                appendLine("    material = MaterialSystem(")
                material.glassMaterial?.let {
                    appendLine("        glassMaterial = MaterialSystem.GlassMaterial(")
                    appendLine("            blurRadius = ${it.blurRadius}f,")
                    appendLine("            tintColor = ${colorToDSL(it.tintColor)},")
                    appendLine("            thickness = ${it.thickness}f,")
                    appendLine("            brightness = ${it.brightness}f")
                    appendLine("        )")
                }
                material.micaMaterial?.let {
                    appendLine("        micaMaterial = MaterialSystem.MicaMaterial(")
                    appendLine("            baseColor = ${colorToDSL(it.baseColor)},")
                    appendLine("            tintOpacity = ${it.tintOpacity}f,")
                    appendLine("            luminosity = ${it.luminosity}f")
                    appendLine("        )")
                }
                appendLine("    )")
            }

            appendLine(")")
        }
    }

    /**
     * Compile theme to YAML
     */
    fun compileToYAML(theme: Theme): String {
        return buildString {
            appendLine("# Generated Theme: ${theme.name}")
            appendLine("# Platform: ${theme.platform}")
            appendLine()
            appendLine("Theme:")
            appendLine("  name: \"${theme.name}\"")
            appendLine("  platform: ${theme.platform}")
            appendLine()

            // Color scheme
            appendLine("  colorScheme:")
            appendLine("    mode: ${theme.colorScheme.mode}")
            with(theme.colorScheme) {
                appendLine("    primary: \"${colorToHex(primary)}\"")
                appendLine("    onPrimary: \"${colorToHex(onPrimary)}\"")
                appendLine("    primaryContainer: \"${colorToHex(primaryContainer)}\"")
                appendLine("    onPrimaryContainer: \"${colorToHex(onPrimaryContainer)}\"")
                appendLine("    secondary: \"${colorToHex(secondary)}\"")
                appendLine("    onSecondary: \"${colorToHex(onSecondary)}\"")
                appendLine("    secondaryContainer: \"${colorToHex(secondaryContainer)}\"")
                appendLine("    onSecondaryContainer: \"${colorToHex(onSecondaryContainer)}\"")
                appendLine("    tertiary: \"${colorToHex(tertiary)}\"")
                appendLine("    onTertiary: \"${colorToHex(onTertiary)}\"")
                appendLine("    tertiaryContainer: \"${colorToHex(tertiaryContainer)}\"")
                appendLine("    onTertiaryContainer: \"${colorToHex(onTertiaryContainer)}\"")
                appendLine("    error: \"${colorToHex(error)}\"")
                appendLine("    onError: \"${colorToHex(onError)}\"")
                appendLine("    errorContainer: \"${colorToHex(errorContainer)}\"")
                appendLine("    onErrorContainer: \"${colorToHex(onErrorContainer)}\"")
                appendLine("    surface: \"${colorToHex(surface)}\"")
                appendLine("    onSurface: \"${colorToHex(onSurface)}\"")
                appendLine("    surfaceVariant: \"${colorToHex(surfaceVariant)}\"")
                appendLine("    onSurfaceVariant: \"${colorToHex(onSurfaceVariant)}\"")
                appendLine("    background: \"${colorToHex(background)}\"")
                appendLine("    onBackground: \"${colorToHex(onBackground)}\"")
                appendLine("    outline: \"${colorToHex(outline)}\"")
                appendLine("    outlineVariant: \"${colorToHex(outlineVariant)}\"")
            }
            appendLine()

            // Typography
            appendLine("  typography:")
            with(theme.typography) {
                appendLine("    displayLarge: { family: \"${displayLarge.family}\", size: ${displayLarge.size}, weight: ${displayLarge.weight} }")
                appendLine("    bodyLarge: { family: \"${bodyLarge.family}\", size: ${bodyLarge.size}, weight: ${bodyLarge.weight} }")
                appendLine("    labelMedium: { family: \"${labelMedium.family}\", size: ${labelMedium.size}, weight: ${labelMedium.weight} }")
            }
            appendLine()

            // Spacing
            appendLine("  spacing:")
            with(theme.spacing) {
                appendLine("    xs: $xs")
                appendLine("    sm: $sm")
                appendLine("    md: $md")
                appendLine("    lg: $lg")
                appendLine("    xl: $xl")
                appendLine("    xxl: $xxl")
            }
        }
    }

    /**
     * Compile theme to JSON
     */
    fun compileToJSON(theme: Theme): String {
        return json.encodeToString(theme)
    }

    /**
     * Compile theme to CSS variables (for web)
     */
    fun compileToCSS(theme: Theme): String {
        return buildString {
            appendLine("/* Generated Theme: ${theme.name} */")
            appendLine(":root {")

            // Color scheme
            with(theme.colorScheme) {
                appendLine("  /* Primary Colors */")
                appendLine("  --color-primary: ${colorToRGB(primary)};")
                appendLine("  --color-on-primary: ${colorToRGB(onPrimary)};")
                appendLine("  --color-primary-container: ${colorToRGB(primaryContainer)};")
                appendLine("  --color-on-primary-container: ${colorToRGB(onPrimaryContainer)};")
                appendLine()
                appendLine("  /* Secondary Colors */")
                appendLine("  --color-secondary: ${colorToRGB(secondary)};")
                appendLine("  --color-on-secondary: ${colorToRGB(onSecondary)};")
                appendLine("  --color-secondary-container: ${colorToRGB(secondaryContainer)};")
                appendLine("  --color-on-secondary-container: ${colorToRGB(onSecondaryContainer)};")
                appendLine()
                appendLine("  /* Surface Colors */")
                appendLine("  --color-surface: ${colorToRGB(surface)};")
                appendLine("  --color-on-surface: ${colorToRGB(onSurface)};")
                appendLine("  --color-background: ${colorToRGB(background)};")
                appendLine("  --color-on-background: ${colorToRGB(onBackground)};")
            }
            appendLine()

            // Typography
            with(theme.typography) {
                appendLine("  /* Typography */")
                appendLine("  --font-display-large: ${displayLarge.size}px/${displayLarge.lineHeight} ${displayLarge.family};")
                appendLine("  --font-body-large: ${bodyLarge.size}px/${bodyLarge.lineHeight} ${bodyLarge.family};")
                appendLine("  --font-label-medium: ${labelMedium.size}px/${labelMedium.lineHeight} ${labelMedium.family};")
            }
            appendLine()

            // Spacing
            with(theme.spacing) {
                appendLine("  /* Spacing */")
                appendLine("  --spacing-xs: ${xs}px;")
                appendLine("  --spacing-sm: ${sm}px;")
                appendLine("  --spacing-md: ${md}px;")
                appendLine("  --spacing-lg: ${lg}px;")
                appendLine("  --spacing-xl: ${xl}px;")
                appendLine("  --spacing-xxl: ${xxl}px;")
            }
            appendLine()

            // Border radius
            with(theme.shapes) {
                appendLine("  /* Border Radius */")
                appendLine("  --radius-xs: ${extraSmall.topLeft}px;")
                appendLine("  --radius-sm: ${small.topLeft}px;")
                appendLine("  --radius-md: ${medium.topLeft}px;")
                appendLine("  --radius-lg: ${large.topLeft}px;")
                appendLine("  --radius-xl: ${extraLarge.topLeft}px;")
            }

            appendLine("}")
        }
    }

    // ==================== Helper Functions ====================

    private fun colorToDSL(color: Color): String {
        return if (color.alpha < 1.0f) {
            "Color(${color.red}, ${color.green}, ${color.blue}, ${color.alpha}f)"
        } else {
            "Color.hex(\"${colorToHex(color)}\")"
        }
    }

    private fun colorToHex(color: Color): String {
        val r = color.red.toString(16).padStart(2, '0')
        val g = color.green.toString(16).padStart(2, '0')
        val b = color.blue.toString(16).padStart(2, '0')
        val hex = "#$r$g$b".uppercase()
        return if (color.alpha < 1.0f) {
            val a = (color.alpha * 255).toInt().toString(16).padStart(2, '0')
            "$hex$a".uppercase()
        } else {
            hex
        }
    }

    private fun colorToRGB(color: Color): String {
        val r = color.red
        val g = color.green
        val b = color.blue
        return if (color.alpha < 1.0f) {
            "rgba($r, $g, $b, ${color.alpha})"
        } else {
            "rgb($r, $g, $b)"
        }
    }

    private fun fontToDSL(font: Font): String {
        return buildString {
            append("Font(")
            if (font.family != "System") {
                append("family = \"${font.family}\", ")
            }
            append("size = ${font.size}f, ")
            append("weight = Font.Weight.${font.weight}")
            append(")")
        }
    }

    private fun cornerRadiusToDSL(radius: CornerRadius): String {
        return if (radius.topLeft == radius.topRight &&
                   radius.topLeft == radius.bottomLeft &&
                   radius.topLeft == radius.bottomRight) {
            "CornerRadius.all(${radius.topLeft}f)"
        } else {
            "CornerRadius(${radius.topLeft}f, ${radius.topRight}f, ${radius.bottomLeft}f, ${radius.bottomRight}f)"
        }
    }

    private fun shadowToDSL(shadow: Shadow): String {
        return "Shadow(offsetX = ${shadow.offsetX}f, offsetY = ${shadow.offsetY}f, blurRadius = ${shadow.blurRadius}f)"
    }

    private fun String.toCamelCase(): String {
        return this.split(" ", "-", "_")
            .mapIndexed { index, s ->
                if (index == 0) s.lowercase()
                else s.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")
            .replace(Regex("[^a-zA-Z0-9]"), "")
    }
}

/**
 * Theme validator to check for common issues
 */
class ThemeValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    /**
     * Validate a theme for common issues
     */
    fun validate(theme: Theme): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check color contrast (WCAG AA requires 4.5:1 for normal text)
        with(theme.colorScheme) {
            if (calculateContrast(primary, onPrimary) < 4.5) {
                warnings.add("Primary/OnPrimary contrast ratio is below WCAG AA (4.5:1)")
            }
            if (calculateContrast(surface, onSurface) < 4.5) {
                warnings.add("Surface/OnSurface contrast ratio is below WCAG AA (4.5:1)")
            }
            if (calculateContrast(background, onBackground) < 4.5) {
                warnings.add("Background/OnBackground contrast ratio is below WCAG AA (4.5:1)")
            }
        }

        // Check typography sizes
        with(theme.typography) {
            if (bodyMedium.size < 12f) {
                warnings.add("Body medium font size is very small (< 12px)")
            }
            if (displayLarge.size > 72f) {
                warnings.add("Display large font size is very large (> 72px)")
            }
        }

        // Check spacing scale progression
        with(theme.spacing) {
            if (xs >= sm || sm >= md || md >= lg || lg >= xl || xl >= xxl) {
                errors.add("Spacing scale is not properly ordered (xs < sm < md < lg < xl < xxl)")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun calculateContrast(c1: Color, c2: Color): Double {
        val l1 = calculateRelativeLuminance(c1)
        val l2 = calculateRelativeLuminance(c2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun calculateRelativeLuminance(color: Color): Double {
        val r = color.red / 255.0
        val g = color.green / 255.0
        val b = color.blue / 255.0

        val rLinear = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }
}
