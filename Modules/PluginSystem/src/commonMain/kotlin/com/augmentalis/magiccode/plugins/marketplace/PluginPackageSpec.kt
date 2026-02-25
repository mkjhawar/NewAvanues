package com.augmentalis.magiccode.plugins.marketplace

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Plugin package specification for marketplace distribution.
 *
 * Represents a complete plugin package as distributed through the marketplace,
 * including the manifest, security signatures, checksums, and associated assets.
 * This is the primary unit of distribution for plugins.
 *
 * ## Package Contents
 * A plugin package contains:
 * - **Manifest**: Complete plugin metadata and configuration
 * - **Signature**: Cryptographic signature for authenticity verification
 * - **Checksum**: Hash for integrity verification
 * - **Assets**: References to bundled resources
 *
 * ## Security Model
 * All packages must be signed by the publisher's private key. The marketplace
 * verifies signatures before accepting submissions, and clients verify
 * signatures before installation.
 *
 * ## Example
 * ```kotlin
 * val package = PluginPackage(
 *     manifest = PluginManifestData(
 *         pluginId = "com.example.awesome-plugin",
 *         name = "Awesome Plugin",
 *         version = "1.0.0",
 *         description = "An awesome plugin for voice commands",
 *         author = "Example Corp",
 *         capabilities = listOf("voice.commands", "ui.overlay")
 *     ),
 *     signature = "base64-encoded-signature",
 *     checksum = "sha256:abc123...",
 *     publishedAt = Clock.System.now().toEpochMilliseconds(),
 *     size = 1024000L
 * )
 * ```
 *
 * @property manifest Plugin metadata and configuration
 * @property signature Base64-encoded cryptographic signature
 * @property checksum Content hash with algorithm prefix (e.g., "sha256:...")
 * @property publishedAt Unix timestamp of publication (milliseconds)
 * @property size Package size in bytes
 * @property assets List of asset references included in the package
 * @since 1.0.0
 * @see PluginManifestData
 * @see AssetReference
 */
@Serializable
data class PluginPackage(
    val manifest: PluginManifestData,
    val signature: String,
    val checksum: String,
    val publishedAt: Long,
    val size: Long,
    val assets: List<AssetReference> = emptyList()
) {
    /**
     * Validate that the package has all required fields populated.
     *
     * @return true if package is complete and valid
     */
    fun isComplete(): Boolean {
        return signature.isNotBlank() &&
            checksum.isNotBlank() &&
            publishedAt > 0 &&
            size > 0
    }

    /**
     * Get total size including all assets.
     *
     * @return Total package size in bytes
     */
    fun getTotalSize(): Long {
        return size + assets.sumOf { it.size }
    }
}

/**
 * Plugin manifest data for marketplace distribution.
 *
 * Contains essential plugin metadata required for marketplace listing and
 * installation. This is a simplified version of the full PluginManifest
 * optimized for marketplace operations.
 *
 * ## Required Fields
 * All fields except permissions and minSdkVersion are required:
 * - **pluginId**: Unique identifier in reverse-domain notation
 * - **name**: Human-readable display name
 * - **version**: Semantic version string (X.Y.Z)
 * - **description**: Brief description of plugin functionality
 * - **author**: Publisher name or organization
 * - **capabilities**: List of capability identifiers
 *
 * ## Plugin ID Format
 * Plugin IDs must follow reverse-domain notation:
 * - Valid: `com.example.my-plugin`, `org.acme.voice.commands`
 * - Invalid: `my-plugin`, `MyPlugin`, `com.Example.plugin`
 *
 * ## Version Format
 * Versions must follow semantic versioning (semver):
 * - Valid: `1.0.0`, `2.1.3`, `1.0.0-beta.1`
 * - Invalid: `1.0`, `v1.0.0`, `1.0.0.0`
 *
 * @property pluginId Unique plugin identifier in reverse-domain notation
 * @property name Human-readable plugin name
 * @property version Semantic version string
 * @property description Brief description of functionality
 * @property author Publisher name or organization
 * @property capabilities List of capability identifiers (e.g., "voice.commands")
 * @property permissions Required permissions (e.g., "NETWORK", "STORAGE_READ")
 * @property minSdkVersion Minimum Android SDK version required
 * @since 1.0.0
 * @see PluginPackage
 */
@Serializable
data class PluginManifestData(
    val pluginId: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val capabilities: List<String>,
    val permissions: List<String> = emptyList(),
    val minSdkVersion: Int = 29
) {
    /**
     * Check if this manifest data is valid.
     *
     * Performs basic validation without full regex checks.
     * Use [com.augmentalis.magiccode.plugins.marketplace.SubmissionValidator]
     * for comprehensive validation.
     *
     * @return true if all required fields are non-blank
     */
    fun isValid(): Boolean {
        return pluginId.isNotBlank() &&
            name.isNotBlank() &&
            version.isNotBlank() &&
            description.isNotBlank() &&
            author.isNotBlank()
    }

    /**
     * Get the major version number.
     *
     * @return Major version or 0 if parsing fails
     */
    fun getMajorVersion(): Int {
        return version.split(".").firstOrNull()?.toIntOrNull() ?: 0
    }

    /**
     * Check if plugin has a specific capability.
     *
     * @param capability Capability identifier to check
     * @return true if plugin declares this capability
     */
    fun hasCapability(capability: String): Boolean {
        return capabilities.contains(capability)
    }
}

/**
 * Reference to an asset bundled with a plugin package.
 *
 * Asset references provide metadata about resources included in a plugin
 * package, enabling integrity verification and size estimation before
 * download.
 *
 * ## Asset Paths
 * Asset paths are relative to the plugin root directory:
 * - `assets/images/icon.png`
 * - `assets/fonts/custom.ttf`
 * - `themes/dark-theme.yaml`
 *
 * ## Checksum Format
 * Checksums include the algorithm prefix:
 * - `sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
 * - `md5:d41d8cd98f00b204e9800998ecf8427e`
 *
 * @property path Relative path to the asset within the plugin package
 * @property checksum Content hash with algorithm prefix
 * @property size Asset size in bytes
 * @since 1.0.0
 * @see PluginPackage
 */
@Serializable
data class AssetReference(
    val path: String,
    val checksum: String,
    val size: Long
) {
    /**
     * Get the file extension of this asset.
     *
     * @return File extension without dot, or empty string if none
     */
    fun getExtension(): String {
        return path.substringAfterLast(".", "")
    }

    /**
     * Get the filename without directory path.
     *
     * @return Just the filename portion
     */
    fun getFilename(): String {
        return path.substringAfterLast("/")
    }

    /**
     * Check if the asset is an image file.
     *
     * @return true if extension indicates an image
     */
    fun isImage(): Boolean {
        val ext = getExtension().lowercase()
        return ext in listOf("png", "jpg", "jpeg", "svg", "gif", "webp")
    }

    /**
     * Check if the asset is a font file.
     *
     * @return true if extension indicates a font
     */
    fun isFont(): Boolean {
        val ext = getExtension().lowercase()
        return ext in listOf("ttf", "otf", "woff", "woff2")
    }

    /**
     * Get the checksum algorithm used.
     *
     * @return Algorithm name (e.g., "sha256") or null if not prefixed
     */
    fun getChecksumAlgorithm(): String? {
        return if (checksum.contains(":")) {
            checksum.substringBefore(":")
        } else {
            null
        }
    }

    /**
     * Get the raw checksum value without algorithm prefix.
     *
     * @return Checksum hash value
     */
    fun getChecksumValue(): String {
        return if (checksum.contains(":")) {
            checksum.substringAfter(":")
        } else {
            checksum
        }
    }
}
