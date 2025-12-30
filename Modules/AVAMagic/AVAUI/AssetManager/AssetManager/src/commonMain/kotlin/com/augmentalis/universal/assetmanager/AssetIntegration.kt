package com.augmentalis.universal.assetmanager

/**
 * Integration helpers for using assets with UI components
 *
 * This file provides utility functions and examples for integrating the Asset Management System
 * with UI components in the AvaUI framework.
 */

/**
 * Asset source that can be resolved to a URL or data
 */
sealed class AssetSource {
    /**
     * Reference to an asset in a registered library
     */
    data class Reference(val reference: String) : AssetSource() {
        // Format: "library:asset-id"
        val libraryId: String
            get() = reference.substringBefore(':')
        val assetId: String
            get() = reference.substringAfter(':')
    }

    /**
     * Direct URL to an asset
     */
    data class Url(val url: String) : AssetSource()

    /**
     * Inline data (for small assets or base64 encoded data)
     */
    data class Data(val data: ByteArray, val mimeType: String? = null) : AssetSource() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Data

            if (!data.contentEquals(other.data)) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + (mimeType?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * Local file path
     */
    data class FilePath(val path: String) : AssetSource()
}

/**
 * Resolves asset sources to usable URLs or data
 */
object AssetResolver {
    /**
     * CDN base URL (can be configured)
     */
    var cdnBaseUrl: String? = null

    /**
     * Local assets base path
     */
    var localAssetsPath: String = "/Assets"

    /**
     * Resolve an icon reference to a source
     *
     * @param reference Icon reference (e.g., "MaterialIcons:home")
     * @param format Preferred format (SVG or PNG)
     * @param size PNG size if format is PNG
     * @return Asset source or null if not found
     */
    suspend fun resolveIcon(
        reference: String,
        format: IconFormat = IconFormat.SVG,
        size: Int = 24
    ): AssetSource? {
        val icon = AssetManager.getIcon(reference) ?: return null
        val ref = IconReference.parse(reference) ?: return null

        return when (format) {
            IconFormat.SVG -> {
                if (icon.svg != null) {
                    AssetSource.Data(icon.svg.encodeToByteArray(), "image/svg+xml")
                } else {
                    // Fall back to PNG
                    val pngData = icon.getPngForSize(size)
                    pngData?.let { AssetSource.Data(it, "image/png") }
                }
            }
            IconFormat.PNG -> {
                val pngData = icon.getPngForSize(size)
                pngData?.let { AssetSource.Data(it, "image/png") }
            }
        }
    }

    /**
     * Get icon URL (for CDN or local file system)
     *
     * @param reference Icon reference
     * @param format Preferred format
     * @param size PNG size if format is PNG
     * @return URL string or null
     */
    suspend fun getIconUrl(
        reference: String,
        format: IconFormat = IconFormat.SVG,
        size: Int = 24
    ): String? {
        val ref = IconReference.parse(reference) ?: return null

        return if (cdnBaseUrl != null) {
            // Use CDN
            when (format) {
                IconFormat.SVG -> "$cdnBaseUrl/icons/${ref.libraryId}/${ref.iconId}.svg"
                IconFormat.PNG -> "$cdnBaseUrl/icons/${ref.libraryId}/${ref.iconId}_$size.png"
            }
        } else {
            // Use local path
            when (format) {
                IconFormat.SVG -> "$localAssetsPath/Icons/${ref.libraryId}/svg/${ref.iconId}.svg"
                IconFormat.PNG -> "$localAssetsPath/Icons/${ref.libraryId}/png/$size/${ref.iconId}.png"
            }
        }
    }

    /**
     * Resolve an image reference to a source
     *
     * @param reference Image reference (e.g., "Backgrounds:gradient-1")
     * @return Asset source or null if not found
     */
    suspend fun resolveImage(reference: String): AssetSource? {
        val image = AssetManager.getImage(reference) ?: return null
        val ref = ImageReference.parse(reference) ?: return null

        return if (cdnBaseUrl != null) {
            AssetSource.Url("$cdnBaseUrl/images/${ref.libraryId}/${image.path}")
        } else {
            AssetSource.FilePath("$localAssetsPath/Images/${ref.libraryId}/${image.path}")
        }
    }

    /**
     * Get image URL
     *
     * @param reference Image reference
     * @return URL string or null
     */
    suspend fun getImageUrl(reference: String): String? {
        val image = AssetManager.getImage(reference) ?: return null
        val ref = ImageReference.parse(reference) ?: return null

        return if (cdnBaseUrl != null) {
            "$cdnBaseUrl/images/${ref.libraryId}/${image.path}"
        } else {
            "$localAssetsPath/Images/${ref.libraryId}/${image.path}"
        }
    }

    /**
     * Get thumbnail URL for an image
     *
     * @param reference Image reference
     * @return URL string or null
     */
    suspend fun getThumbnailUrl(reference: String): String? {
        val ref = ImageReference.parse(reference) ?: return null

        return if (cdnBaseUrl != null) {
            "$cdnBaseUrl/images/${ref.libraryId}/thumbnails/${ref.imageId}.jpg"
        } else {
            "$localAssetsPath/Images/${ref.libraryId}/thumbnails/${ref.imageId}.jpg"
        }
    }

    /**
     * Resolve thumbnail data
     *
     * @param reference Image reference
     * @return Asset source or null if not found
     */
    suspend fun resolveThumbnail(reference: String): AssetSource? {
        val image = AssetManager.getImage(reference) ?: return null

        return if (image.thumbnail != null) {
            AssetSource.Data(image.thumbnail, "image/jpeg")
        } else {
            // Fall back to thumbnail URL
            val url = getThumbnailUrl(reference)
            url?.let { AssetSource.Url(it) }
        }
    }
}

/**
 * DSL helper for creating icon references in components
 *
 * Example usage in AvaUI:
 * ```
 * Icon {
 *     source = iconSource("MaterialIcons:home")
 *     size = 24.dp
 * }
 * ```
 */
suspend fun iconSource(
    reference: String,
    format: IconFormat = IconFormat.SVG,
    size: Int = 24
): AssetSource? {
    return AssetResolver.resolveIcon(reference, format, size)
}

/**
 * DSL helper for creating image references in components
 *
 * Example usage in AvaUI:
 * ```
 * Image {
 *     source = imageSource("Backgrounds:gradient-blue")
 *     contentScale = ContentScale.Crop
 * }
 * ```
 */
suspend fun imageSource(reference: String): AssetSource? {
    return AssetResolver.resolveImage(reference)
}

/**
 * Asset preloader for improving performance
 */
object AssetPreloader {
    private val preloadedIcons = mutableMapOf<String, Icon>()
    private val preloadedImages = mutableMapOf<String, ImageAsset>()

    /**
     * Preload icons for faster access
     *
     * @param references List of icon references to preload
     */
    suspend fun preloadIcons(references: List<String>) {
        references.forEach { reference ->
            AssetManager.getIcon(reference)?.let { icon ->
                preloadedIcons[reference] = icon
            }
        }
    }

    /**
     * Preload images for faster access
     *
     * @param references List of image references to preload
     */
    suspend fun preloadImages(references: List<String>) {
        references.forEach { reference ->
            AssetManager.getImage(reference)?.let { image ->
                preloadedImages[reference] = image
            }
        }
    }

    /**
     * Clear preloaded assets
     */
    fun clear() {
        preloadedIcons.clear()
        preloadedImages.clear()
    }

    /**
     * Get preloaded icon
     */
    fun getPreloadedIcon(reference: String): Icon? {
        return preloadedIcons[reference]
    }

    /**
     * Get preloaded image
     */
    fun getPreloadedImage(reference: String): ImageAsset? {
        return preloadedImages[reference]
    }
}

/**
 * Configuration for CDN integration
 */
data class CdnConfig(
    val baseUrl: String,
    val iconPath: String = "icons",
    val imagePath: String = "images",
    val cachingEnabled: Boolean = true,
    val cacheControlMaxAge: Int = 86400, // 24 hours in seconds
    val compressionEnabled: Boolean = true
) {
    /**
     * Apply this configuration to the asset resolver
     */
    fun apply() {
        AssetResolver.cdnBaseUrl = baseUrl
    }
}

/**
 * Asset loading strategies
 */
enum class LoadingStrategy {
    /**
     * Load asset immediately
     */
    IMMEDIATE,

    /**
     * Load asset lazily when needed
     */
    LAZY,

    /**
     * Preload asset for better performance
     */
    PRELOAD
}

/**
 * Asset cache for improving performance
 */
interface AssetCache {
    suspend fun getIcon(reference: String): Icon?
    suspend fun putIcon(reference: String, icon: Icon)
    suspend fun getImage(reference: String): ImageAsset?
    suspend fun putImage(reference: String, image: ImageAsset)
    suspend fun clear()
}

/**
 * Simple in-memory asset cache implementation
 */
class InMemoryAssetCache : AssetCache {
    private val icons = mutableMapOf<String, Icon>()
    private val images = mutableMapOf<String, ImageAsset>()

    override suspend fun getIcon(reference: String): Icon? = icons[reference]

    override suspend fun putIcon(reference: String, icon: Icon) {
        icons[reference] = icon
    }

    override suspend fun getImage(reference: String): ImageAsset? = images[reference]

    override suspend fun putImage(reference: String, image: ImageAsset) {
        images[reference] = image
    }

    override suspend fun clear() {
        icons.clear()
        images.clear()
    }
}
