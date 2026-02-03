package com.augmentalis.magicelements.core.resources

/**
 * Represents an icon resource that can be loaded and rendered
 *
 * Icons can come from different sources:
 * - Material Icons (default)
 * - Custom vector drawables
 * - Raster images (PNG/WebP)
 * - Network URLs
 *
 * @since 3.0.0-flutter-parity
 */
sealed class IconResource {
    /**
     * Material Design icon from Material Icons library
     *
     * @property name Icon name (e.g., "check", "person", "settings")
     * @property variant Icon variant (filled, outlined, rounded, sharp, two-tone)
     */
    data class MaterialIcon(
        val name: String,
        val variant: IconVariant = IconVariant.Filled
    ) : IconResource()

    /**
     * Custom vector drawable from resources
     *
     * @property resourceId Resource identifier or name
     */
    data class VectorDrawable(
        val resourceId: String
    ) : IconResource()

    /**
     * Raster image from resources (PNG/WebP)
     *
     * @property resourceId Resource identifier or name
     */
    data class RasterImage(
        val resourceId: String
    ) : IconResource()

    /**
     * Network image from URL
     *
     * @property url Image URL
     * @property placeholder Optional placeholder icon while loading
     * @property errorIcon Optional icon to show on error
     */
    data class NetworkImage(
        val url: String,
        val placeholder: IconResource? = null,
        val errorIcon: IconResource? = null
    ) : IconResource()

    /**
     * Icon from base64-encoded data
     *
     * @property data Base64-encoded image data
     * @property mimeType MIME type (e.g., "image/png", "image/svg+xml")
     */
    data class Base64Image(
        val data: String,
        val mimeType: String
    ) : IconResource()

    /**
     * Icon variant types matching Material Design icon styles
     */
    enum class IconVariant {
        /** Filled variant (default) */
        Filled,
        /** Outlined variant */
        Outlined,
        /** Rounded variant */
        Rounded,
        /** Sharp variant */
        Sharp,
        /** Two-tone variant */
        TwoTone
    }

    companion object {
        /**
         * Create a Material Icon from Flutter icon name
         *
         * Converts Flutter icon names to Material Design icon names.
         * Examples:
         * - Icons.check -> MaterialIcon("check")
         * - Icons.person -> MaterialIcon("person")
         * - Icons.settings_outlined -> MaterialIcon("settings", IconVariant.Outlined)
         */
        fun fromFlutterIcon(flutterIconName: String): MaterialIcon {
            // Parse Flutter icon name
            val parts = flutterIconName.removePrefix("Icons.").split("_")
            val variant = when (parts.lastOrNull()) {
                "outlined" -> IconVariant.Outlined
                "rounded" -> IconVariant.Rounded
                "sharp" -> IconVariant.Sharp
                else -> IconVariant.Filled
            }

            val iconName = if (variant != IconVariant.Filled) {
                parts.dropLast(1).joinToString("_")
            } else {
                parts.joinToString("_")
            }

            return MaterialIcon(iconName, variant)
        }

        /**
         * Create icon from resource name string
         *
         * Automatically detects icon type:
         * - Starts with "http://" or "https://" -> NetworkImage
         * - Starts with "data:" -> Base64Image
         * - Contains "." extension -> RasterImage
         * - Otherwise -> MaterialIcon
         */
        fun fromString(resourceName: String): IconResource {
            return when {
                resourceName.startsWith("http://") || resourceName.startsWith("https://") -> {
                    NetworkImage(resourceName)
                }
                resourceName.startsWith("data:") -> {
                    val parts = resourceName.removePrefix("data:").split(",", limit = 2)
                    val mimeType = parts[0].substringBefore(";")
                    val data = parts.getOrNull(1) ?: ""
                    Base64Image(data, mimeType)
                }
                resourceName.contains(".") -> {
                    RasterImage(resourceName)
                }
                else -> {
                    fromFlutterIcon(resourceName)
                }
            }
        }
    }
}

/**
 * Icon size presets following Material Design guidelines
 */
enum class IconSize(val dp: Float) {
    /** 18dp - Small inline icons */
    Small(18f),
    /** 24dp - Standard icons (default) */
    Standard(24f),
    /** 36dp - Large icons */
    Large(36f),
    /** 48dp - Extra large icons */
    ExtraLarge(48f);

    companion object {
        fun fromDp(dp: Float): IconSize {
            return entries.minByOrNull { kotlin.math.abs(it.dp - dp) } ?: Standard
        }
    }
}
