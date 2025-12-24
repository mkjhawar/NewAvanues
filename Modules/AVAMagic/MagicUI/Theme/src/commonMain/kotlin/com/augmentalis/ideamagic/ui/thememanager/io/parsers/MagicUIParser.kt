package com.augmentalis.magicui.ui.thememanager.io.parsers

import com.augmentalis.magicui.components.core.*
import com.augmentalis.magicui.ui.thememanager.io.ThemeIOError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * MagicUI JSON Parser
 *
 * Parses themes from the MagicUI extended JSON format, which includes:
 * - All W3C Design Tokens standard properties
 * - Spatial materials (depth, orientation, 3D positioning)
 * - Glass materials (blur, opacity, tint)
 * - Mica materials (Windows 11 Fluent)
 * - Platform-specific extensions
 *
 * This format provides full-fidelity import/export for all MagicUI theme capabilities.
 *
 * @since 1.0.0
 */
class MagicUIParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parse MagicUI JSON into a Theme
     *
     * @param jsonString The MagicUI theme JSON
     * @return Result containing the parsed Theme or an error
     */
    fun parse(jsonString: String): Result<Theme> = runCatching {
        val dto = json.decodeFromString<MagicUIThemeDTO>(jsonString)
        dto.toTheme()
    }.recoverCatching { error ->
        throw ThemeIOError.InvalidFormat(
            message = "Failed to parse MagicUI theme: ${error.message}",
            cause = error
        )
    }

    /**
     * Serialize a Theme to MagicUI JSON
     *
     * @param theme The theme to serialize
     * @param pretty Whether to format with indentation
     * @return JSON string
     */
    fun serialize(theme: Theme, pretty: Boolean = true): String {
        val dto = MagicUIThemeDTO.fromTheme(theme)
        return if (pretty) {
            Json { prettyPrint = true }.encodeToString(
                MagicUIThemeDTO.serializer(),
                dto
            )
        } else {
            json.encodeToString(MagicUIThemeDTO.serializer(), dto)
        }
    }
}

// ==================== Data Transfer Objects ====================

/**
 * MagicUI Theme DTO for JSON serialization
 */
@Serializable
data class MagicUIThemeDTO(
    @SerialName("\$schema")
    val schema: String = "https://magicui.dev/schemas/theme-v1.json",

    val version: String = "1.0.0",
    val name: String,
    val platform: String,
    val extends: String? = null,

    val colors: ColorSchemeDTO,
    val typography: TypographyDTO,
    val shapes: ShapesDTO,
    val spacing: SpacingDTO,
    val elevation: ElevationDTO,
    val material: MaterialSystemDTO? = null,
    val animation: AnimationConfigDTO
) {
    fun toTheme(): Theme = Theme(
        name = name,
        platform = platform.toThemePlatform(),
        colorScheme = colors.toColorScheme(),
        typography = typography.toTypography(),
        shapes = shapes.toShapes(),
        spacing = spacing.toSpacingScale(),
        elevation = elevation.toElevationScale(),
        material = material?.toMaterialSystem(),
        animation = animation.toAnimationConfig()
    )

    companion object {
        fun fromTheme(theme: Theme) = MagicUIThemeDTO(
            name = theme.name,
            platform = theme.platform.name,
            colors = ColorSchemeDTO.fromColorScheme(theme.colorScheme),
            typography = TypographyDTO.fromTypography(theme.typography),
            shapes = ShapesDTO.fromShapes(theme.shapes),
            spacing = SpacingDTO.fromSpacingScale(theme.spacing),
            elevation = ElevationDTO.fromElevationScale(theme.elevation),
            material = theme.material?.let { MaterialSystemDTO.fromMaterialSystem(it) },
            animation = AnimationConfigDTO.fromAnimationConfig(theme.animation)
        )
    }
}

@Serializable
data class ColorSchemeDTO(
    val mode: String,
    val primary: String,
    val onPrimary: String,
    val primaryContainer: String,
    val onPrimaryContainer: String,
    val secondary: String,
    val onSecondary: String,
    val secondaryContainer: String,
    val onSecondaryContainer: String,
    val tertiary: String,
    val onTertiary: String,
    val tertiaryContainer: String,
    val onTertiaryContainer: String,
    val error: String,
    val onError: String,
    val errorContainer: String,
    val onErrorContainer: String,
    val surface: String,
    val onSurface: String,
    val surfaceVariant: String,
    val onSurfaceVariant: String,
    val surfaceTint: String? = null,
    val background: String,
    val onBackground: String,
    val outline: String,
    val outlineVariant: String,
    val scrim: String? = null,
    val inverseSurface: String? = null,
    val inverseOnSurface: String? = null,
    val inversePrimary: String? = null
) {
    fun toColorScheme() = ColorScheme(
        mode = when (mode.lowercase()) {
            "light" -> ColorScheme.ColorMode.Light
            "dark" -> ColorScheme.ColorMode.Dark
            "auto" -> ColorScheme.ColorMode.Auto
            else -> ColorScheme.ColorMode.Light
        },
        primary = Color.parse(primary) ?: Color.hex("#6750A4"),
        onPrimary = Color.parse(onPrimary) ?: Color.White,
        primaryContainer = Color.parse(primaryContainer) ?: Color.hex("#EADDFF"),
        onPrimaryContainer = Color.parse(onPrimaryContainer) ?: Color.hex("#21005D"),
        secondary = Color.parse(secondary) ?: Color.hex("#625B71"),
        onSecondary = Color.parse(onSecondary) ?: Color.White,
        secondaryContainer = Color.parse(secondaryContainer) ?: Color.hex("#E8DEF8"),
        onSecondaryContainer = Color.parse(onSecondaryContainer) ?: Color.hex("#1D192B"),
        tertiary = Color.parse(tertiary) ?: Color.hex("#7D5260"),
        onTertiary = Color.parse(onTertiary) ?: Color.White,
        tertiaryContainer = Color.parse(tertiaryContainer) ?: Color.hex("#FFD8E4"),
        onTertiaryContainer = Color.parse(onTertiaryContainer) ?: Color.hex("#31111D"),
        error = Color.parse(error) ?: Color.hex("#B3261E"),
        onError = Color.parse(onError) ?: Color.White,
        errorContainer = Color.parse(errorContainer) ?: Color.hex("#F9DEDC"),
        onErrorContainer = Color.parse(onErrorContainer) ?: Color.hex("#410E0B"),
        surface = Color.parse(surface) ?: Color.hex("#FEF7FF"),
        onSurface = Color.parse(onSurface) ?: Color.hex("#1D1B20"),
        surfaceVariant = Color.parse(surfaceVariant) ?: Color.hex("#E7E0EC"),
        onSurfaceVariant = Color.parse(onSurfaceVariant) ?: Color.hex("#49454F"),
        surfaceTint = surfaceTint?.let { Color.parse(it) },
        background = Color.parse(background) ?: Color.hex("#FEF7FF"),
        onBackground = Color.parse(onBackground) ?: Color.hex("#1D1B20"),
        outline = Color.parse(outline) ?: Color.hex("#79747E"),
        outlineVariant = Color.parse(outlineVariant) ?: Color.hex("#CAC4D0"),
        scrim = scrim?.let { Color.parse(it) } ?: Color(0, 0, 0, 0.32f),
        inverseSurface = inverseSurface?.let { Color.parse(it) },
        inverseOnSurface = inverseOnSurface?.let { Color.parse(it) },
        inversePrimary = inversePrimary?.let { Color.parse(it) }
    )

    companion object {
        fun fromColorScheme(scheme: ColorScheme) = ColorSchemeDTO(
            mode = scheme.mode.name,
            primary = scheme.primary.toHex(),
            onPrimary = scheme.onPrimary.toHex(),
            primaryContainer = scheme.primaryContainer.toHex(),
            onPrimaryContainer = scheme.onPrimaryContainer.toHex(),
            secondary = scheme.secondary.toHex(),
            onSecondary = scheme.onSecondary.toHex(),
            secondaryContainer = scheme.secondaryContainer.toHex(),
            onSecondaryContainer = scheme.onSecondaryContainer.toHex(),
            tertiary = scheme.tertiary.toHex(),
            onTertiary = scheme.onTertiary.toHex(),
            tertiaryContainer = scheme.tertiaryContainer.toHex(),
            onTertiaryContainer = scheme.onTertiaryContainer.toHex(),
            error = scheme.error.toHex(),
            onError = scheme.onError.toHex(),
            errorContainer = scheme.errorContainer.toHex(),
            onErrorContainer = scheme.onErrorContainer.toHex(),
            surface = scheme.surface.toHex(),
            onSurface = scheme.onSurface.toHex(),
            surfaceVariant = scheme.surfaceVariant.toHex(),
            onSurfaceVariant = scheme.onSurfaceVariant.toHex(),
            surfaceTint = scheme.surfaceTint?.toHex(),
            background = scheme.background.toHex(),
            onBackground = scheme.onBackground.toHex(),
            outline = scheme.outline.toHex(),
            outlineVariant = scheme.outlineVariant.toHex(),
            scrim = scheme.scrim.toHex(),
            inverseSurface = scheme.inverseSurface?.toHex(),
            inverseOnSurface = scheme.inverseOnSurface?.toHex(),
            inversePrimary = scheme.inversePrimary?.toHex()
        )
    }
}

@Serializable
data class TypographyDTO(
    val displayLarge: FontDTO,
    val displayMedium: FontDTO,
    val displaySmall: FontDTO,
    val headlineLarge: FontDTO,
    val headlineMedium: FontDTO,
    val headlineSmall: FontDTO,
    val titleLarge: FontDTO,
    val titleMedium: FontDTO,
    val titleSmall: FontDTO,
    val bodyLarge: FontDTO,
    val bodyMedium: FontDTO,
    val bodySmall: FontDTO,
    val labelLarge: FontDTO,
    val labelMedium: FontDTO,
    val labelSmall: FontDTO
) {
    fun toTypography() = Typography(
        displayLarge = displayLarge.toFont(),
        displayMedium = displayMedium.toFont(),
        displaySmall = displaySmall.toFont(),
        headlineLarge = headlineLarge.toFont(),
        headlineMedium = headlineMedium.toFont(),
        headlineSmall = headlineSmall.toFont(),
        titleLarge = titleLarge.toFont(),
        titleMedium = titleMedium.toFont(),
        titleSmall = titleSmall.toFont(),
        bodyLarge = bodyLarge.toFont(),
        bodyMedium = bodyMedium.toFont(),
        bodySmall = bodySmall.toFont(),
        labelLarge = labelLarge.toFont(),
        labelMedium = labelMedium.toFont(),
        labelSmall = labelSmall.toFont()
    )

    companion object {
        fun fromTypography(typo: Typography) = TypographyDTO(
            displayLarge = FontDTO.fromFont(typo.displayLarge),
            displayMedium = FontDTO.fromFont(typo.displayMedium),
            displaySmall = FontDTO.fromFont(typo.displaySmall),
            headlineLarge = FontDTO.fromFont(typo.headlineLarge),
            headlineMedium = FontDTO.fromFont(typo.headlineMedium),
            headlineSmall = FontDTO.fromFont(typo.headlineSmall),
            titleLarge = FontDTO.fromFont(typo.titleLarge),
            titleMedium = FontDTO.fromFont(typo.titleMedium),
            titleSmall = FontDTO.fromFont(typo.titleSmall),
            bodyLarge = FontDTO.fromFont(typo.bodyLarge),
            bodyMedium = FontDTO.fromFont(typo.bodyMedium),
            bodySmall = FontDTO.fromFont(typo.bodySmall),
            labelLarge = FontDTO.fromFont(typo.labelLarge),
            labelMedium = FontDTO.fromFont(typo.labelMedium),
            labelSmall = FontDTO.fromFont(typo.labelSmall)
        )
    }
}

@Serializable
data class FontDTO(
    val family: String? = null,
    val size: Float,
    val weight: Int,
    val lineHeight: Float? = null,
    val letterSpacing: Float? = null
) {
    fun toFont() = Font(
        family = family,
        size = size,
        weight = Font.Weight.fromValue(weight),
        lineHeight = lineHeight,
        letterSpacing = letterSpacing
    )

    companion object {
        fun fromFont(font: Font) = FontDTO(
            family = font.family,
            size = font.size,
            weight = font.weight.value,
            lineHeight = font.lineHeight,
            letterSpacing = font.letterSpacing
        )
    }
}

@Serializable
data class ShapesDTO(
    val extraSmall: Float,
    val small: Float,
    val medium: Float,
    val large: Float,
    val extraLarge: Float
) {
    fun toShapes() = Shapes(
        extraSmall = CornerRadius.all(extraSmall),
        small = CornerRadius.all(small),
        medium = CornerRadius.all(medium),
        large = CornerRadius.all(large),
        extraLarge = CornerRadius.all(extraLarge)
    )

    companion object {
        fun fromShapes(shapes: Shapes) = ShapesDTO(
            extraSmall = shapes.extraSmall.topLeft,
            small = shapes.small.topLeft,
            medium = shapes.medium.topLeft,
            large = shapes.large.topLeft,
            extraLarge = shapes.extraLarge.topLeft
        )
    }
}

@Serializable
data class SpacingDTO(
    val xs: Float,
    val sm: Float,
    val md: Float,
    val lg: Float,
    val xl: Float,
    val xxl: Float
) {
    fun toSpacingScale() = SpacingScale(xs, sm, md, lg, xl, xxl)

    companion object {
        fun fromSpacingScale(spacing: SpacingScale) = SpacingDTO(
            xs = spacing.xs,
            sm = spacing.sm,
            md = spacing.md,
            lg = spacing.lg,
            xl = spacing.xl,
            xxl = spacing.xxl
        )
    }
}

@Serializable
data class ElevationDTO(
    val level0: ShadowDTO,
    val level1: ShadowDTO,
    val level2: ShadowDTO,
    val level3: ShadowDTO,
    val level4: ShadowDTO,
    val level5: ShadowDTO
) {
    fun toElevationScale() = ElevationScale(
        level0 = level0.toShadow(),
        level1 = level1.toShadow(),
        level2 = level2.toShadow(),
        level3 = level3.toShadow(),
        level4 = level4.toShadow(),
        level5 = level5.toShadow()
    )

    companion object {
        fun fromElevationScale(elevation: ElevationScale) = ElevationDTO(
            level0 = ShadowDTO.fromShadow(elevation.level0),
            level1 = ShadowDTO.fromShadow(elevation.level1),
            level2 = ShadowDTO.fromShadow(elevation.level2),
            level3 = ShadowDTO.fromShadow(elevation.level3),
            level4 = ShadowDTO.fromShadow(elevation.level4),
            level5 = ShadowDTO.fromShadow(elevation.level5)
        )
    }
}

@Serializable
data class ShadowDTO(
    val offsetX: Float = 0f,
    val offsetY: Float,
    val blurRadius: Float,
    val spreadRadius: Float = 0f,
    val color: String? = null
) {
    fun toShadow() = Shadow(
        offsetX = offsetX,
        offsetY = offsetY,
        blurRadius = blurRadius,
        spreadRadius = spreadRadius,
        color = color?.let { Color.parse(it) } ?: Color(0, 0, 0, 0.2f)
    )

    companion object {
        fun fromShadow(shadow: Shadow) = ShadowDTO(
            offsetX = shadow.offsetX,
            offsetY = shadow.offsetY,
            blurRadius = shadow.blurRadius,
            spreadRadius = shadow.spreadRadius,
            color = shadow.color.toHex()
        )
    }
}

@Serializable
data class MaterialSystemDTO(
    val glass: GlassMaterialDTO? = null,
    val mica: MicaMaterialDTO? = null,
    val spatial: SpatialMaterialDTO? = null
) {
    fun toMaterialSystem() = MaterialSystem(
        glassMaterial = glass?.toGlassMaterial(),
        micaMaterial = mica?.toMicaMaterial(),
        spatialMaterial = spatial?.toSpatialMaterial()
    )

    companion object {
        fun fromMaterialSystem(material: MaterialSystem) = MaterialSystemDTO(
            glass = material.glassMaterial?.let { GlassMaterialDTO.fromGlassMaterial(it) },
            mica = material.micaMaterial?.let { MicaMaterialDTO.fromMicaMaterial(it) },
            spatial = material.spatialMaterial?.let { SpatialMaterialDTO.fromSpatialMaterial(it) }
        )
    }
}

@Serializable
data class GlassMaterialDTO(
    val blurRadius: Float,
    val tintColor: String,
    val thickness: Float,
    val brightness: Float
) {
    fun toGlassMaterial() = MaterialSystem.GlassMaterial(
        blurRadius = blurRadius,
        tintColor = Color.parse(tintColor) ?: Color.White.copy(alpha = 0.15f),
        thickness = thickness,
        brightness = brightness
    )

    companion object {
        fun fromGlassMaterial(glass: MaterialSystem.GlassMaterial) = GlassMaterialDTO(
            blurRadius = glass.blurRadius,
            tintColor = glass.tintColor.toHex(),
            thickness = glass.thickness,
            brightness = glass.brightness
        )
    }
}

@Serializable
data class MicaMaterialDTO(
    val baseColor: String,
    val tintOpacity: Float,
    val luminosity: Float
) {
    fun toMicaMaterial() = MaterialSystem.MicaMaterial(
        baseColor = Color.parse(baseColor) ?: Color.hex("#F3F3F3"),
        tintOpacity = tintOpacity,
        luminosity = luminosity
    )

    companion object {
        fun fromMicaMaterial(mica: MaterialSystem.MicaMaterial) = MicaMaterialDTO(
            baseColor = mica.baseColor.toHex(),
            tintOpacity = mica.tintOpacity,
            luminosity = mica.luminosity
        )
    }
}

@Serializable
data class SpatialMaterialDTO(
    val depth: Float,
    val orientation: String,
    val glass: GlassMaterialDTO? = null
) {
    fun toSpatialMaterial() = MaterialSystem.SpatialMaterial(
        depth = depth,
        orientation = when (orientation.lowercase()) {
            "flat" -> MaterialSystem.SpatialMaterial.Orientation.Flat
            "tilted" -> MaterialSystem.SpatialMaterial.Orientation.Tilted
            "billboard" -> MaterialSystem.SpatialMaterial.Orientation.Billboard
            else -> MaterialSystem.SpatialMaterial.Orientation.Flat
        },
        glassEffect = glass?.toGlassMaterial()
    )

    companion object {
        fun fromSpatialMaterial(spatial: MaterialSystem.SpatialMaterial) = SpatialMaterialDTO(
            depth = spatial.depth,
            orientation = spatial.orientation.name,
            glass = spatial.glassEffect?.let { GlassMaterialDTO.fromGlassMaterial(it) }
        )
    }
}

@Serializable
data class AnimationConfigDTO(
    val defaultDuration: Long,
    val defaultEasing: String,
    val enableMotion: Boolean,
    val reduceMotion: Boolean
) {
    fun toAnimationConfig() = AnimationConfig(
        defaultDuration = defaultDuration,
        defaultEasing = when (defaultEasing.lowercase()) {
            "linear" -> Animation.Easing.Linear
            "easein" -> Animation.Easing.EaseIn
            "easeout" -> Animation.Easing.EaseOut
            "easeinout" -> Animation.Easing.EaseInOut
            else -> Animation.Easing.EaseInOut
        },
        enableMotion = enableMotion,
        reduceMotion = reduceMotion
    )

    companion object {
        fun fromAnimationConfig(config: AnimationConfig) = AnimationConfigDTO(
            defaultDuration = config.defaultDuration,
            defaultEasing = config.defaultEasing.name,
            enableMotion = config.enableMotion,
            reduceMotion = config.reduceMotion
        )
    }
}

// ==================== Helper Extensions ====================

private fun String.toThemePlatform(): ThemePlatform = when (this) {
    "iOS26_LiquidGlass" -> ThemePlatform.iOS26_LiquidGlass
    "macOS26_Tahoe" -> ThemePlatform.macOS26_Tahoe
    "visionOS2_SpatialGlass" -> ThemePlatform.visionOS2_SpatialGlass
    "Windows11_Fluent2" -> ThemePlatform.Windows11_Fluent2
    "AndroidXR_SpatialMaterial" -> ThemePlatform.AndroidXR_SpatialMaterial
    "Material3_Expressive" -> ThemePlatform.Material3_Expressive
    "SamsungOneUI7_ColoredGlass" -> ThemePlatform.SamsungOneUI7_ColoredGlass
    else -> ThemePlatform.Custom
}

private fun Color.toHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    val a = (alpha * 255).toInt().coerceIn(0, 255)

    return if (alpha < 1.0f) {
        "rgba($r, $g, $b, ${String.format("%.2f", alpha)})"
    } else {
        "#%02X%02X%02X".format(r, g, b)
    }
}
