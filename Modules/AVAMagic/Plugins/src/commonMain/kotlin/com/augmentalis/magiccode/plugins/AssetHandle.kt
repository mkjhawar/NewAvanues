package com.augmentalis.avacode.plugins

/**
 * Platform-specific handle to a resolved plugin asset.
 *
 * AssetHandle provides a unified interface for accessing plugin assets across
 * different platforms (Android, iOS, JVM) while encapsulating platform-specific
 * file access mechanisms.
 *
 * ## Asset Access
 * AssetHandle offers multiple access patterns:
 * - **Binary Access**: [readBytes] for images, fonts, and binary data
 * - **Text Access**: [readText] for YAML, JSON, and text files
 * - **Platform Handle**: [getPlatformHandle] for native API integration
 *
 * ## Platform Implementations
 * AssetHandle uses Kotlin Multiplatform's expect/actual mechanism:
 * - **Android**: Returns `java.io.File` from [getPlatformHandle]
 * - **iOS**: Returns `NSURL` or `NSData` from [getPlatformHandle]
 * - **JVM**: Returns `java.io.File` from [getPlatformHandle]
 *
 * ## Validity Checking
 * Use [isValid] to verify the asset file still exists before accessing.
 * Handles may become invalid if:
 * - Plugin is uninstalled
 * - Asset file is deleted
 * - File permissions change
 *
 * ## Example Usage
 * ```kotlin
 * val result = resolver.resolveAsset("plugin://my.plugin/icons/app.png")
 * when (result) {
 *     is Success -> {
 *         val handle = result.assetHandle
 *         if (handle.isValid()) {
 *             val imageData = handle.readBytes()
 *             // Process image data
 *         }
 *     }
 *     is Failure -> println("Failed: ${result.reason}")
 * }
 * ```
 *
 * ## Thread Safety
 * AssetHandle instances are immutable and thread-safe. However, the underlying
 * file system operations ([readBytes], [readText]) are subject to platform-specific
 * I/O thread safety guarantees.
 *
 * @property reference Asset reference with resolved path information
 * @property absolutePath Absolute filesystem path to the asset file
 * @property metadata Asset metadata including MIME type, size, and checksum
 * @since 1.0.0
 * @see AssetResolver
 * @see AssetReference
 * @see AssetMetadata
 */
data class AssetHandle(
    /**
     * Asset reference with resolved path.
     */
    val reference: AssetReference,

    /**
     * Absolute path to asset file.
     */
    val absolutePath: String,

    /**
     * Asset metadata.
     */
    val metadata: AssetMetadata
) {
    /**
     * Get asset URI in plugin:// format.
     *
     * @return Plugin asset URI (e.g., "plugin://my.plugin/icons/app.png")
     * @since 1.0.0
     */
    fun getUri(): String {
        return reference.toUri()
    }

    companion object {
        /**
         * Create AssetHandle from resolved asset reference.
         *
         * Factory method for creating AssetHandle from an AssetReference that
         * has already been resolved to an absolute filesystem path.
         *
         * @param reference Asset reference with resolved path (must have resolvedPath set)
         * @param metadata Asset metadata extracted during resolution
         * @return AssetHandle for asset access
         * @throws IllegalArgumentException if reference is not resolved
         * @throws IllegalStateException if reference has no resolved path
         * @since 1.0.0
         */
        fun create(reference: AssetReference, metadata: AssetMetadata): AssetHandle {
            require(reference.isResolved()) {
                "AssetReference must be resolved before creating AssetHandle"
            }

            val resolvedPath = reference.resolvedPath
                ?: throw IllegalStateException("Asset reference must have resolved path")

            return AssetHandle(
                reference = reference,
                absolutePath = resolvedPath,
                metadata = metadata
            )
        }
    }
}

/**
 * Check if handle is valid (file still exists).
 *
 * Verifies that the asset file still exists and is accessible.
 * Call this before accessing the asset to handle cases where the file
 * may have been deleted or the plugin uninstalled.
 *
 * @return true if the asset file exists and is accessible, false otherwise
 * @since 1.0.0
 */
expect fun AssetHandle.isValid(): Boolean

/**
 * Read asset as byte array.
 *
 * Reads the entire asset file into memory as a byte array. Suitable for
 * binary assets like images, fonts, and icon files.
 *
 * ## Example
 * ```kotlin
 * val imageData = handle.readBytes()
 * val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
 * ```
 *
 * @return Asset contents as bytes
 * @throws IOException if read fails or file doesn't exist
 * @since 1.0.0
 */
expect fun AssetHandle.readBytes(): ByteArray

/**
 * Read asset as string (UTF-8).
 *
 * Reads the entire asset file as a UTF-8 encoded string. Suitable for
 * text assets like YAML themes, JSON configs, and documentation.
 *
 * ## Example
 * ```kotlin
 * val themeYaml = handle.readText()
 * val theme = yamlParser.parse(themeYaml)
 * ```
 *
 * @return Asset contents as string
 * @throws IOException if read fails or file doesn't exist
 * @since 1.0.0
 */
expect fun AssetHandle.readText(): String

/**
 * Get platform-specific file handle.
 *
 * Returns platform-appropriate type for native API integration:
 * - **Android**: `java.io.File`
 * - **iOS**: `NSURL` or `NSData`
 * - **JVM**: `java.io.File`
 *
 * Use this when you need to pass the asset to platform-specific APIs
 * that require native file handles.
 *
 * ## Example
 * ```kotlin
 * // Android
 * val file = handle.getPlatformHandle() as File
 * val uri = Uri.fromFile(file)
 *
 * // iOS
 * val url = handle.getPlatformHandle() as NSURL
 * val image = UIImage.imageWithContentsOfURL(url)
 * ```
 *
 * @return Platform-specific file object
 * @since 1.0.0
 */
expect fun AssetHandle.getPlatformHandle(): Any
