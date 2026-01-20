package com.augmentalis.avamagic.components.core

import kotlinx.serialization.Serializable

/**
 * Platform theme system supporting 7 major design systems:
 * - iOS 26 Liquid Glass
 * - macOS 26 Tahoe
 * - visionOS 2 Spatial Glass
 * - Windows 11 Fluent 2
 * - Android XR Spatial Material
 * - Material Design 3 Expressive
 * - Samsung One UI 7
 */
@Serializable
data class Theme(
    val name: String,
    val platform: ThemePlatform,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val shapes: Shapes,
    val spacing: SpacingScale,
    val elevation: ElevationScale,
    val material: MaterialSystem? = null,
    val animation: AnimationConfig = AnimationConfig()
)

@Serializable
enum class ThemePlatform {
    iOS26_LiquidGlass,
    macOS26_Tahoe,
    visionOS2_SpatialGlass,
    Windows11_Fluent2,
    AndroidXR_SpatialMaterial,
    Material3_Expressive,
    SamsungOneUI7_ColoredGlass,
    Custom
}

// ==================== Color System ====================

@Serializable
data class ColorScheme(
    val mode: ColorMode = ColorMode.Light,

    // Primary colors
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Secondary colors
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    // Tertiary colors
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    // Error colors
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    // Surface colors
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color? = null,

    // Background colors
    val background: Color,
    val onBackground: Color,

    // Outline colors
    val outline: Color,
    val outlineVariant: Color,

    // Special colors
    val scrim: Color = Color(0, 0, 0, 0.32f),
    val inverseSurface: Color? = null,
    val inverseOnSurface: Color? = null,
    val inversePrimary: Color? = null
) {
    enum class ColorMode {
        Light,
        Dark,
        Auto  // System-based
    }

    companion object {
        /**
         * Material Design 3 default light scheme
         */
        val Material3Light = ColorScheme(
            mode = ColorMode.Light,
            primary = Color.hex("#6750A4"),
            onPrimary = Color.White,
            primaryContainer = Color.hex("#EADDFF"),
            onPrimaryContainer = Color.hex("#21005D"),
            secondary = Color.hex("#625B71"),
            onSecondary = Color.White,
            secondaryContainer = Color.hex("#E8DEF8"),
            onSecondaryContainer = Color.hex("#1D192B"),
            tertiary = Color.hex("#7D5260"),
            onTertiary = Color.White,
            tertiaryContainer = Color.hex("#FFD8E4"),
            onTertiaryContainer = Color.hex("#31111D"),
            error = Color.hex("#B3261E"),
            onError = Color.White,
            errorContainer = Color.hex("#F9DEDC"),
            onErrorContainer = Color.hex("#410E0B"),
            surface = Color.hex("#FEF7FF"),
            onSurface = Color.hex("#1D1B20"),
            surfaceVariant = Color.hex("#E7E0EC"),
            onSurfaceVariant = Color.hex("#49454F"),
            background = Color.hex("#FEF7FF"),
            onBackground = Color.hex("#1D1B20"),
            outline = Color.hex("#79747E"),
            outlineVariant = Color.hex("#CAC4D0")
        )

        /**
         * iOS 26 Liquid Glass light scheme
         */
        val iOS26Light = ColorScheme(
            mode = ColorMode.Light,
            primary = Color.hex("#007AFF"),
            onPrimary = Color.White,
            primaryContainer = Color.hex("#E5F2FF"),
            onPrimaryContainer = Color.hex("#001D35"),
            secondary = Color.hex("#5E5CE6"),
            onSecondary = Color.White,
            secondaryContainer = Color.hex("#EFEDFF"),
            onSecondaryContainer = Color.hex("#1A1A4D"),
            tertiary = Color.hex("#FF9500"),
            onTertiary = Color.White,
            tertiaryContainer = Color.hex("#FFE8CC"),
            onTertiaryContainer = Color.hex("#4D2C00"),
            error = Color.hex("#FF3B30"),
            onError = Color.White,
            errorContainer = Color.hex("#FFE5E5"),
            onErrorContainer = Color.hex("#4D0000"),
            surface = Color.hex("#FFFFFF").copy(alpha = 0.7f),  // Translucent
            onSurface = Color.hex("#000000"),
            surfaceVariant = Color.hex("#F2F2F7"),
            onSurfaceVariant = Color.hex("#3C3C43"),
            background = Color.hex("#F2F2F7"),
            onBackground = Color.hex("#000000"),
            outline = Color.hex("#C6C6C8"),
            outlineVariant = Color.hex("#E5E5EA"),
            surfaceTint = Color.hex("#007AFF").copy(alpha = 0.1f)
        )

        /**
         * Windows 11 Fluent 2 light scheme
         */
        val Windows11Light = ColorScheme(
            mode = ColorMode.Light,
            primary = Color.hex("#0078D4"),
            onPrimary = Color.White,
            primaryContainer = Color.hex("#E1F0FF"),
            onPrimaryContainer = Color.hex("#002952"),
            secondary = Color.hex("#8764B8"),
            onSecondary = Color.White,
            secondaryContainer = Color.hex("#F3EEFF"),
            onSecondaryContainer = Color.hex("#2B1A47"),
            tertiary = Color.hex("#00B7C3"),
            onTertiary = Color.White,
            tertiaryContainer = Color.hex("#D6F5F7"),
            onTertiaryContainer = Color.hex("#003942"),
            error = Color.hex("#D13438"),
            onError = Color.White,
            errorContainer = Color.hex("#FFE5E5"),
            onErrorContainer = Color.hex("#4D0000"),
            surface = Color.hex("#F3F3F3"),
            onSurface = Color.hex("#1C1C1C"),
            surfaceVariant = Color.hex("#E6E6E6"),
            onSurfaceVariant = Color.hex("#3B3B3B"),
            background = Color.hex("#F9F9F9"),
            onBackground = Color.hex("#1C1C1C"),
            outline = Color.hex("#D1D1D1"),
            outlineVariant = Color.hex("#E6E6E6")
        )
    }
}

// ==================== Typography System ====================

@Serializable
data class Typography(
    val displayLarge: Font = Font(size = 57f, weight = Font.Weight.Regular),
    val displayMedium: Font = Font(size = 45f, weight = Font.Weight.Regular),
    val displaySmall: Font = Font(size = 36f, weight = Font.Weight.Regular),
    val headlineLarge: Font = Font(size = 32f, weight = Font.Weight.Regular),
    val headlineMedium: Font = Font(size = 28f, weight = Font.Weight.Regular),
    val headlineSmall: Font = Font(size = 24f, weight = Font.Weight.Regular),
    val titleLarge: Font = Font(size = 22f, weight = Font.Weight.Regular),
    val titleMedium: Font = Font(size = 16f, weight = Font.Weight.Medium),
    val titleSmall: Font = Font(size = 14f, weight = Font.Weight.Medium),
    val bodyLarge: Font = Font(size = 16f, weight = Font.Weight.Regular),
    val bodyMedium: Font = Font(size = 14f, weight = Font.Weight.Regular),
    val bodySmall: Font = Font(size = 12f, weight = Font.Weight.Regular),
    val labelLarge: Font = Font(size = 14f, weight = Font.Weight.Medium),
    val labelMedium: Font = Font(size = 12f, weight = Font.Weight.Medium),
    val labelSmall: Font = Font(size = 11f, weight = Font.Weight.Medium)
) {
    companion object {
        val Material3 = Typography()

        val iOS26 = Typography(
            displayLarge = Font(family = "SF Pro Display", size = 34f, weight = Font.Weight.Bold),
            headlineLarge = Font(family = "SF Pro Display", size = 28f, weight = Font.Weight.Bold),
            titleLarge = Font(family = "SF Pro Text", size = 20f, weight = Font.Weight.SemiBold),
            bodyLarge = Font(family = "SF Pro Text", size = 17f, weight = Font.Weight.Regular),
            labelLarge = Font(family = "SF Pro Text", size = 13f, weight = Font.Weight.Regular)
        )

        val Windows11 = Typography(
            displayLarge = Font(family = "Segoe UI Variable Display", size = 40f, weight = Font.Weight.SemiBold),
            headlineLarge = Font(family = "Segoe UI Variable Display", size = 28f, weight = Font.Weight.SemiBold),
            titleLarge = Font(family = "Segoe UI Variable Text", size = 20f, weight = Font.Weight.SemiBold),
            bodyLarge = Font(family = "Segoe UI Variable Text", size = 14f, weight = Font.Weight.Regular),
            labelLarge = Font(family = "Segoe UI Variable Small", size = 12f, weight = Font.Weight.Regular)
        )
    }
}

// ==================== Shape System ====================

@Serializable
data class Shapes(
    val extraSmall: CornerRadius = CornerRadius.all(4f),
    val small: CornerRadius = CornerRadius.all(8f),
    val medium: CornerRadius = CornerRadius.all(12f),
    val large: CornerRadius = CornerRadius.all(16f),
    val extraLarge: CornerRadius = CornerRadius.all(28f)
) {
    companion object {
        val Material3 = Shapes()
        val iOS26 = Shapes(
            extraSmall = CornerRadius.all(6f),
            small = CornerRadius.all(10f),
            medium = CornerRadius.all(14f),
            large = CornerRadius.all(20f),
            extraLarge = CornerRadius.all(30f)
        )
        val Windows11 = Shapes(
            extraSmall = CornerRadius.all(4f),
            small = CornerRadius.all(6f),
            medium = CornerRadius.all(8f),
            large = CornerRadius.all(12f),
            extraLarge = CornerRadius.all(16f)
        )
    }
}

// ==================== Spacing System ====================

@Serializable
data class SpacingScale(
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f,
    val xxl: Float = 48f
)

// ==================== Elevation System ====================

@Serializable
data class ElevationScale(
    val level0: Shadow = Shadow(offsetY = 0f, blurRadius = 0f),
    val level1: Shadow = Shadow(offsetY = 1f, blurRadius = 3f),
    val level2: Shadow = Shadow(offsetY = 2f, blurRadius = 6f),
    val level3: Shadow = Shadow(offsetY = 4f, blurRadius = 8f),
    val level4: Shadow = Shadow(offsetY = 6f, blurRadius = 12f),
    val level5: Shadow = Shadow(offsetY = 8f, blurRadius = 16f)
)

// ==================== Material System (Glass Effects) ====================

@Serializable
data class MaterialSystem(
    val glassMaterial: GlassMaterial? = null,
    val micaMaterial: MicaMaterial? = null,
    val spatialMaterial: SpatialMaterial? = null
) {
    @Serializable
    data class GlassMaterial(
        val blurRadius: Float = 30f,
        val tintColor: Color = Color.White.copy(alpha = 0.15f),
        val thickness: Float = 2f,
        val brightness: Float = 1.0f
    )

    @Serializable
    data class MicaMaterial(
        val baseColor: Color = Color.hex("#F3F3F3"),
        val tintOpacity: Float = 0.5f,
        val luminosity: Float = 1.0f
    )

    @Serializable
    data class SpatialMaterial(
        val depth: Float = 100f,  // Z-axis depth in dp
        val orientation: Orientation = Orientation.Flat,
        val glassEffect: GlassMaterial? = null
    ) {
        enum class Orientation {
            Flat,
            Tilted,
            Billboard  // Always faces user
        }
    }
}

// ==================== Animation Configuration ====================

@Serializable
data class AnimationConfig(
    val defaultDuration: Long = 300,
    val defaultEasing: Animation.Easing = Animation.Easing.EaseInOut,
    val enableMotion: Boolean = true,
    val reduceMotion: Boolean = false  // Accessibility setting
)

// ==================== Predefined Themes ====================

object Themes {
    val Material3Light = Theme(
        name = "Material Design 3 Light",
        platform = ThemePlatform.Material3_Expressive,
        colorScheme = ColorScheme.Material3Light,
        typography = Typography.Material3,
        shapes = Shapes.Material3,
        spacing = SpacingScale(),
        elevation = ElevationScale()
    )

    val iOS26LiquidGlass = Theme(
        name = "iOS 26 Liquid Glass",
        platform = ThemePlatform.iOS26_LiquidGlass,
        colorScheme = ColorScheme.iOS26Light,
        typography = Typography.iOS26,
        shapes = Shapes.iOS26,
        spacing = SpacingScale(),
        elevation = ElevationScale(),
        material = MaterialSystem(
            glassMaterial = MaterialSystem.GlassMaterial(
                blurRadius = 30f,
                tintColor = Color.White.copy(alpha = 0.15f),
                thickness = 2f
            )
        )
    )

    val Windows11Fluent2 = Theme(
        name = "Windows 11 Fluent 2",
        platform = ThemePlatform.Windows11_Fluent2,
        colorScheme = ColorScheme.Windows11Light,
        typography = Typography.Windows11,
        shapes = Shapes.Windows11,
        spacing = SpacingScale(),
        elevation = ElevationScale(),
        material = MaterialSystem(
            micaMaterial = MaterialSystem.MicaMaterial()
        )
    )

    val visionOS2SpatialGlass = Theme(
        name = "visionOS 2 Spatial Glass",
        platform = ThemePlatform.visionOS2_SpatialGlass,
        colorScheme = ColorScheme.iOS26Light.copy(
            surface = Color.White.copy(alpha = 0.5f)
        ),
        typography = Typography.iOS26,
        shapes = Shapes.iOS26,
        spacing = SpacingScale(),
        elevation = ElevationScale(),
        material = MaterialSystem(
            spatialMaterial = MaterialSystem.SpatialMaterial(
                depth = 100f,
                glassEffect = MaterialSystem.GlassMaterial(
                    blurRadius = 40f,
                    tintColor = Color.White.copy(alpha = 0.1f)
                )
            )
        )
    )
}
