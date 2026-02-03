package com.augmentalis.avaelements.assets.models

import kotlinx.serialization.Serializable

/**
 * Represents an image asset in the system
 */
@Serializable
data class ImageAsset(
    /** Unique identifier */
    val id: String,

    /** Human-readable name */
    val name: String,

    /** File path or URL */
    val path: String,

    /** Image format */
    val format: ImageFormat,

    /** Image dimensions */
    val dimensions: Dimensions,

    /** Thumbnail (JPEG, max 128x128) */
    val thumbnail: ByteArray? = null,

    /** File size in bytes */
    val fileSize: Long,

    /** Search tags */
    val tags: List<String> = emptyList(),

    /** Alt text for accessibility */
    val altText: String? = null,

    /** Copyright/attribution */
    val attribution: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageAsset

        if (id != other.id) return false
        if (name != other.name) return false
        if (path != other.path) return false
        if (format != other.format) return false
        if (dimensions != other.dimensions) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false
        if (fileSize != other.fileSize) return false
        if (tags != other.tags) return false
        if (altText != other.altText) return false
        if (attribution != other.attribution) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + dimensions.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (altText?.hashCode() ?: 0)
        result = 31 * result + (attribution?.hashCode() ?: 0)
        return result
    }
}

/**
 * Image dimensions
 */
@Serializable
data class Dimensions(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
    val isSquare: Boolean get() = width == height
    val isLandscape: Boolean get() = width > height
    val isPortrait: Boolean get() = height > width
}

/**
 * Supported image formats
 */
@Serializable
enum class ImageFormat {
    PNG,
    JPEG,
    SVG,
    WEBP,
    GIF,
    BMP;

    companion object {
        fun fromExtension(extension: String): ImageFormat? {
            return when (extension.lowercase()) {
                "png" -> PNG
                "jpg", "jpeg" -> JPEG
                "svg" -> SVG
                "webp" -> WEBP
                "gif" -> GIF
                "bmp" -> BMP
                else -> null
            }
        }
    }
}
