package com.augmentalis.avaelements.assets.models

import kotlinx.serialization.Serializable

/**
 * Represents an icon asset in the system
 *
 * Icons can be either SVG (vector) or PNG (raster) format.
 * For PNG icons, multiple sizes can be provided for different densities.
 */
@Serializable
data class Icon(
    /** Unique identifier for the icon */
    val id: String,

    /** Human-readable name */
    val name: String,

    /** SVG content (if vector icon) */
    val svg: String? = null,

    /** PNG data for different sizes (if raster icon) */
    val png: Map<IconSize, ByteArray>? = null,

    /** Search tags for discovery */
    val tags: List<String> = emptyList(),

    /** Icon library (e.g., "material", "fontawesome") */
    val library: String? = null,

    /** Category within library */
    val category: String? = null,

    /** Alternative names/aliases */
    val aliases: List<String> = emptyList()
) {
    /** Check if this is a vector icon */
    val isVector: Boolean get() = svg != null

    /** Check if this is a raster icon */
    val isRaster: Boolean get() = png != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Icon

        if (id != other.id) return false
        if (name != other.name) return false
        if (svg != other.svg) return false
        if (tags != other.tags) return false
        if (library != other.library) return false
        if (category != other.category) return false
        if (aliases != other.aliases) return false

        // Compare PNG maps
        if (png != null) {
            if (other.png == null) return false
            if (png.keys != other.png.keys) return false
            for (size in png.keys) {
                if (!png[size].contentEquals(other.png[size])) return false
            }
        } else if (other.png != null) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (svg?.hashCode() ?: 0)
        result = 31 * result + (png?.keys?.hashCode() ?: 0)
        result = 31 * result + tags.hashCode()
        result = 31 * result + (library?.hashCode() ?: 0)
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + aliases.hashCode()
        return result
    }
}

/**
 * Standard icon sizes for raster icons
 */
@Serializable
enum class IconSize(val pixels: Int) {
    SMALL(16),
    MEDIUM(24),
    LARGE(32),
    XLARGE(48),
    XXLARGE(64),
    XXXLARGE(128);

    companion object {
        fun fromPixels(pixels: Int): IconSize? = values().find { it.pixels == pixels }
    }
}
