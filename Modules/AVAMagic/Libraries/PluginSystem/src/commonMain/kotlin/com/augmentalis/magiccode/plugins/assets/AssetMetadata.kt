package com.augmentalis.avacode.plugins.assets

/**
 * Metadata for a resolved asset with file information and integrity verification.
 *
 * AssetMetadata contains extracted information about a resolved asset file,
 * including content type, size, integrity checksum, and fallback status.
 *
 * ## Metadata Fields
 *
 * ### MIME Type
 * Standard MIME type identifying the asset content type for proper handling:
 * - Images: "image/png", "image/jpeg", "image/svg+xml"
 * - Fonts: "font/ttf", "font/otf", "font/woff", "font/woff2"
 * - Themes: "application/x-yaml", "application/json"
 * - Unknown: "application/octet-stream"
 *
 * ### Size
 * File size in bytes for memory management and progress indication.
 * Use [getFormattedSize] for human-readable format (KB, MB, GB).
 *
 * ### Checksum
 * Optional integrity checksum (MD5 or SHA256) for:
 * - Verifying asset integrity
 * - Detecting corruption or tampering
 * - Cache validation
 * - Plugin update detection
 *
 * ### Fallback Status
 * Indicates whether this is a fallback asset provided when the requested
 * asset could not be found. Fallback assets ensure graceful degradation.
 *
 * ### Custom Metadata
 * Extensible key-value map for plugin-specific metadata like:
 * - Image dimensions
 * - Font family/weight
 * - Theme variant
 * - Author/license information
 *
 * ## Example Usage
 * ```kotlin
 * val metadata = AssetMetadata(
 *     mimeType = "image/png",
 *     sizeBytes = 45678,
 *     checksum = "a1b2c3d4...",
 *     custom = mapOf("width" to "512", "height" to "512")
 * )
 *
 * println(metadata.getFormattedSize()) // "44.6 KB"
 * println(metadata.isImage()) // true
 * println(metadata.custom["width"]) // "512"
 * ```
 *
 * @property mimeType MIME type of the asset (e.g., "image/png", "font/ttf")
 * @property sizeBytes File size in bytes
 * @property checksum Optional integrity checksum (MD5 or SHA256), null if not calculated
 * @property isFallback Whether this is a fallback asset (default false)
 * @property custom Additional custom metadata as key-value pairs
 * @since 1.0.0
 * @see AssetHandle
 * @see AssetResolver
 */
data class AssetMetadata(
    /**
     * MIME type of the asset.
     * Example: "image/png", "font/ttf", "application/x-yaml"
     */
    val mimeType: String,

    /**
     * File size in bytes.
     */
    val sizeBytes: Long,

    /**
     * Checksum for integrity verification (MD5, SHA256, etc.)
     * Null if not calculated.
     */
    val checksum: String? = null,

    /**
     * Whether this is a fallback asset.
     */
    val isFallback: Boolean = false,

    /**
     * Additional custom metadata.
     */
    val custom: Map<String, String> = emptyMap()
) {
    /**
     * Get human-readable file size.
     *
     * Formats the file size in bytes to a human-readable string with
     * appropriate unit (B, KB, MB, GB).
     *
     * ## Examples
     * ```kotlin
     * AssetMetadata(mimeType = "...", sizeBytes = 512).getFormattedSize()      // "512 B"
     * AssetMetadata(mimeType = "...", sizeBytes = 1536).getFormattedSize()     // "1.5 KB"
     * AssetMetadata(mimeType = "...", sizeBytes = 2621440).getFormattedSize()  // "2.5 MB"
     * AssetMetadata(mimeType = "...", sizeBytes = 3221225472).getFormattedSize() // "3.0 GB"
     * ```
     *
     * @return Formatted size string (e.g., "1.5 KB", "2.3 MB")
     * @since 1.0.0
     */
    fun getFormattedSize(): String {
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "%.1f KB".format(sizeBytes / 1024.0)
            sizeBytes < 1024 * 1024 * 1024 -> "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
            else -> "%.1f GB".format(sizeBytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Check if asset is an image.
     *
     * Determines if the asset is an image type based on MIME type prefix.
     * Covers all standard image formats (PNG, JPEG, SVG, GIF, WebP, etc.).
     *
     * @return true if MIME type starts with "image/", false otherwise
     * @since 1.0.0
     */
    fun isImage(): Boolean {
        return mimeType.startsWith("image/")
    }

    /**
     * Check if asset is a font.
     *
     * Determines if the asset is a font file based on MIME type prefix.
     * Covers all standard font formats (TTF, OTF, WOFF, WOFF2).
     *
     * @return true if MIME type starts with "font/", false otherwise
     * @since 1.0.0
     */
    fun isFont(): Boolean {
        return mimeType.startsWith("font/")
    }

    /**
     * Check if asset is a theme/config file.
     *
     * Determines if the asset is a theme or configuration file based on
     * MIME type. Covers YAML and JSON formats commonly used for themes.
     *
     * @return true if MIME type contains "yaml" or "json", false otherwise
     * @since 1.0.0
     */
    fun isTheme(): Boolean {
        return mimeType.contains("yaml") || mimeType.contains("json")
    }
}
