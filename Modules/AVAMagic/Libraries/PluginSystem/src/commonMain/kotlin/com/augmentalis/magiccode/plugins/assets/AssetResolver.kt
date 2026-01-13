package com.augmentalis.avacode.plugins.assets

import com.augmentalis.avacode.plugins.core.*
import com.augmentalis.avacode.plugins.platform.FileIO

/**
 * Resolves plugin assets from URI references to accessible handles.
 *
 * AssetResolver maps plugin asset URIs (e.g., `plugin://com.example.plugin/icons/app.png`)
 * to platform-specific file handles, managing caching, namespace validation, and access logging.
 *
 * ## URI Format
 * Asset URIs follow the pattern:
 * ```
 * plugin://<plugin-id>/<category>/<path>
 * ```
 *
 * Where:
 * - `plugin-id`: Reverse-domain plugin identifier (e.g., "com.augmentalis.theme-pack")
 * - `category`: Asset category (images, fonts, icons, themes, data)
 * - `path`: Relative path within the category (e.g., "dark-theme.yaml", "logo.png")
 *
 * ## Resolution Process
 * 1. Check cache for previously resolved asset
 * 2. Parse URI into AssetReference components
 * 3. Verify plugin exists in registry
 * 4. Validate asset path within plugin namespace (security)
 * 5. Verify file exists on filesystem
 * 6. Extract metadata (MIME type, size, checksum)
 * 7. Create AssetHandle for platform access
 * 8. Cache result for future requests
 * 9. Log access for auditing
 *
 * ## Caching
 * Frequently accessed assets are cached in memory for performance.
 * Cache entries are invalidated when:
 * - Plugin is uninstalled
 * - Cache size limit is reached (LRU eviction)
 * - Manual cache clear is triggered
 *
 * ## Fallback Support
 * When asset resolution fails, the resolver can provide fallback assets
 * to ensure graceful degradation (e.g., default icons, placeholder images).
 *
 * ## Security
 * All asset paths are validated against plugin namespace boundaries to prevent
 * unauthorized filesystem access (FR-038).
 *
 * ## Thread Safety
 * AssetResolver is thread-safe. Multiple coroutines can safely resolve assets
 * concurrently. The underlying cache uses mutex-based synchronization.
 *
 * @param registry Plugin registry for validating plugin IDs and retrieving namespaces
 * @param cache Asset cache implementation for performance optimization
 * @param fallbackProvider Provides fallback assets when resolution fails
 * @param fileIO File system access abstraction for platform independence
 * @param checksumCalculator Calculates checksums for integrity verification
 * @param checksumAlgorithm Algorithm to use for checksum calculation (MD5 or SHA256)
 * @param accessLogger Logs all asset access attempts for security auditing
 * @since 1.0.0
 * @see AssetHandle
 * @see AssetReference
 * @see AssetCache
 */
class AssetResolver(
    private val registry: PluginRegistry,
    private val cache: AssetCache = AssetCache(),
    private val fallbackProvider: FallbackAssetProvider = FallbackAssetProvider(),
    private val fileIO: FileIO = FileIO(),
    private val checksumCalculator: ChecksumCalculator = ChecksumCalculator(),
    private val checksumAlgorithm: ChecksumAlgorithm = ChecksumAlgorithm.SHA256,
    private val accessLogger: AssetAccessLogger = AssetAccessLogger()
) {
    companion object {
        private const val TAG = "AssetResolver"
    }

    /**
     * Resolution result encapsulating success or failure outcomes.
     *
     * @since 1.0.0
     */
    sealed class ResolutionResult {
        /**
         * Successful asset resolution with handle.
         *
         * @property assetHandle Resolved asset handle for platform access
         */
        data class Success(val assetHandle: AssetHandle) : ResolutionResult()

        /**
         * Failed asset resolution with error details.
         *
         * @property reason Human-readable failure reason
         * @property exception Optional exception that caused the failure
         */
        data class Failure(val reason: String, val exception: Throwable? = null) : ResolutionResult()
    }

    /**
     * Resolve a plugin asset from URI to an accessible handle.
     *
     * Performs full resolution pipeline including cache lookup, namespace validation,
     * file existence verification, metadata extraction, and access logging.
     *
     * ## URI Format
     * Format: `plugin://plugin-id/category/filename`
     *
     * ## Examples
     * ```kotlin
     * // Resolve a theme asset
     * val result = resolver.resolveAsset("plugin://com.augmentalis.theme-pack/themes/dark-theme.yaml")
     * when (result) {
     *     is Success -> println("Resolved: ${result.assetHandle.absolutePath}")
     *     is Failure -> println("Failed: ${result.reason}")
     * }
     *
     * // Resolve without fallback
     * val result = resolver.resolveAsset("plugin://my.plugin/icons/app.png", useFallback = false)
     * ```
     *
     * ## Cache Behavior
     * If the asset was previously resolved, returns cached handle immediately
     * without filesystem access or validation.
     *
     * ## Thread Safety
     * This method is thread-safe and can be called from multiple coroutines
     * concurrently.
     *
     * @param uri Plugin asset URI in format `plugin://plugin-id/category/filename`
     * @param useFallback Whether to use fallback assets if resolution fails (default true)
     * @return ResolutionResult.Success with AssetHandle, or ResolutionResult.Failure with error details
     * @since 1.0.0
     * @see ResolutionResult
     * @see AssetHandle
     */
    suspend fun resolveAsset(uri: String, useFallback: Boolean = true): ResolutionResult {
        PluginLog.d(TAG, "Resolving asset: $uri")

        // Step 1: Check cache
        cache.get(uri)?.let { cachedHandle ->
            PluginLog.d(TAG, "Cache hit for: $uri")
            logAssetAccess(uri, "CACHE_HIT")
            return ResolutionResult.Success(cachedHandle)
        }

        // Step 2: Parse URI into AssetReference
        val assetRef = AssetReference.fromUri(uri)
        if (assetRef == null) {
            val error = "Invalid plugin asset URI format: $uri"
            PluginLog.w(TAG, error)
            logAssetAccess(uri, "INVALID_URI")
            return if (useFallback) {
                resolveFallback(uri)
            } else {
                ResolutionResult.Failure(error)
            }
        }

        // Step 3: Verify plugin exists and is loaded
        val pluginInfo = registry.getPlugin(assetRef.pluginId)
        if (pluginInfo == null) {
            val error = "Plugin not found: ${assetRef.pluginId}"
            PluginLog.w(TAG, error)
            logAssetAccess(uri, "PLUGIN_NOT_FOUND")
            return if (useFallback) {
                resolveFallback(uri)
            } else {
                ResolutionResult.Failure(error)
            }
        }

        // Step 4: Validate asset belongs to plugin namespace (FR-038)
        val namespace = pluginInfo.namespace
        val categoryPath = assetRef.category.getSubdirectoryPath()
        val assetPath = "${namespace.baseDir}/$categoryPath/${assetRef.filename}"

        // Verify path is within namespace
        try {
            namespace.validateAccess(assetPath)
        } catch (e: SecurityException) {
            val error = "Asset access denied - outside plugin namespace: $uri"
            PluginLog.e(TAG, error, e)
            logAssetAccess(uri, "SECURITY_VIOLATION")
            return ResolutionResult.Failure(error, e)
        }

        // Step 5: Verify file exists
        if (!fileIO.fileExists(assetPath)) {
            val error = "Asset file not found: $assetPath"
            PluginLog.w(TAG, error)
            logAssetAccess(uri, "FILE_NOT_FOUND")
            return if (useFallback) {
                resolveFallback(uri)
            } else {
                ResolutionResult.Failure(error)
            }
        }

        // Step 6: Validate file extension for category
        if (!assetRef.category.isValidExtension(assetRef.filename)) {
            PluginLog.w(TAG, "File extension not typical for category ${assetRef.category}: ${assetRef.filename}")
        }

        // Step 7: Extract metadata (FR-040)
        val metadata = extractAssetMetadata(assetPath, assetRef)

        // Step 8: Create AssetHandle
        val handle = AssetHandle(
            reference = assetRef.withResolvedPath(assetPath),
            absolutePath = assetPath,
            metadata = metadata
        )

        // Step 9: Cache the result
        cache.put(uri, handle)

        // Step 10: Log access
        logAssetAccess(uri, "SUCCESS")

        PluginLog.i(TAG, "Resolved asset: $uri -> $assetPath")
        return ResolutionResult.Success(handle)
    }

    /**
     * Resolve multiple assets in batch for performance optimization.
     *
     * Resolves multiple asset URIs efficiently. Currently processes sequentially
     * but maintains consistent error handling and caching behavior.
     *
     * ## Example
     * ```kotlin
     * val uris = listOf(
     *     "plugin://my.plugin/icons/home.png",
     *     "plugin://my.plugin/icons/settings.png",
     *     "plugin://my.plugin/themes/dark.yaml"
     * )
     * val results = resolver.resolveAssetsBatch(uris)
     * results.forEach { (uri, result) ->
     *     when (result) {
     *         is Success -> println("$uri resolved")
     *         is Failure -> println("$uri failed: ${result.reason}")
     *     }
     * }
     * ```
     *
     * ## Thread Safety
     * This method is thread-safe and can be called from multiple coroutines
     * concurrently.
     *
     * @param uris List of plugin asset URIs to resolve
     * @param useFallback Whether to use fallback assets for failures (default true)
     * @return Map of URI to ResolutionResult for each input URI
     * @since 1.0.0
     * @see resolveAsset
     */
    suspend fun resolveAssetsBatch(
        uris: List<String>,
        useFallback: Boolean = true
    ): Map<String, ResolutionResult> {
        PluginLog.d(TAG, "Batch resolving ${uris.size} assets")

        return uris.associateWith { uri ->
            resolveAsset(uri, useFallback)
        }
    }

    /**
     * Resolve fallback asset when primary resolution fails.
     *
     * Attempts to provide a default asset (e.g., placeholder image, default icon)
     * when the requested asset cannot be found. Ensures graceful degradation.
     *
     * @param uri Original URI that failed to resolve
     * @return ResolutionResult with fallback asset or failure if no fallback available
     */
    private suspend fun resolveFallback(uri: String): ResolutionResult {
        PluginLog.d(TAG, "Attempting fallback resolution for: $uri")

        val assetRef = AssetReference.fromUri(uri) ?: run {
            return ResolutionResult.Failure("Cannot resolve fallback for invalid URI: $uri")
        }

        val fallbackPath = fallbackProvider.getFallbackAsset(assetRef.category, assetRef.filename)
        if (fallbackPath == null) {
            val error = "No fallback available for: $uri"
            PluginLog.w(TAG, error)
            logAssetAccess(uri, "NO_FALLBACK")
            return ResolutionResult.Failure(error)
        }

        if (!fileIO.fileExists(fallbackPath)) {
            val error = "Fallback asset not found: $fallbackPath"
            PluginLog.w(TAG, error)
            logAssetAccess(uri, "FALLBACK_MISSING")
            return ResolutionResult.Failure(error)
        }

        val metadata = extractAssetMetadata(fallbackPath, assetRef)
        val handle = AssetHandle(
            reference = assetRef.withResolvedPath(fallbackPath),
            absolutePath = fallbackPath,
            metadata = metadata.copy(isFallback = true)
        )

        logAssetAccess(uri, "FALLBACK_SUCCESS")
        PluginLog.i(TAG, "Resolved fallback asset: $uri -> $fallbackPath")
        return ResolutionResult.Success(handle)
    }

    /**
     * Extract asset metadata from file.
     *
     * Analyzes asset file to extract metadata including MIME type, size,
     * and integrity checksum for verification.
     *
     * @param path Absolute file path to asset
     * @param reference Asset reference for context
     * @return AssetMetadata with extracted information
     */
    private fun extractAssetMetadata(path: String, reference: AssetReference): AssetMetadata {
        val fileSize = fileIO.getFileSize(path)
        val mimeType = inferMimeType(reference.filename)

        // Calculate checksum for integrity verification (FR-040)
        val checksum = try {
            val fileData = fileIO.readFileAsBytes(path)
            when (checksumAlgorithm) {
                ChecksumAlgorithm.MD5 -> checksumCalculator.calculateMD5(fileData)
                ChecksumAlgorithm.SHA256 -> checksumCalculator.calculateSHA256(fileData)
            }
        } catch (e: Exception) {
            PluginLog.w(TAG, "Failed to calculate checksum for $path: ${e.message}")
            null
        }

        return AssetMetadata(
            mimeType = mimeType,
            sizeBytes = fileSize,
            checksum = checksum,
            isFallback = false
        )
    }

    /**
     * Infer MIME type from filename extension.
     *
     * Maps common file extensions to standard MIME types for asset categorization
     * and content handling.
     *
     * @param filename Filename with extension (e.g., "logo.png", "theme.yaml")
     * @return MIME type string (e.g., "image/png", "application/x-yaml")
     */
    private fun inferMimeType(filename: String): String {
        val extension = filename.substringAfterLast(".", "").lowercase()
        return when (extension) {
            // Images
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            // Fonts
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            // Themes/Config
            "yaml", "yml" -> "application/x-yaml"
            "json" -> "application/json"
            // Icons
            "ico" -> "image/x-icon"
            // Default
            else -> "application/octet-stream"
        }
    }

    /**
     * Log asset access for debugging and security auditing.
     *
     * Records all asset access attempts with status for security monitoring
     * and performance analysis.
     *
     * @param uri Asset URI that was accessed
     * @param status Resolution status (SUCCESS, CACHE_HIT, FAILURE, etc.)
     */
    private suspend fun logAssetAccess(uri: String, status: String) {
        val accessStatus = when (status) {
            "SUCCESS" -> AssetAccessLogger.AccessStatus.SUCCESS
            "CACHE_HIT" -> AssetAccessLogger.AccessStatus.CACHE_HIT
            "INVALID_URI" -> AssetAccessLogger.AccessStatus.INVALID_URI
            "PLUGIN_NOT_FOUND" -> AssetAccessLogger.AccessStatus.PLUGIN_NOT_FOUND
            "FILE_NOT_FOUND" -> AssetAccessLogger.AccessStatus.FILE_NOT_FOUND
            "SECURITY_VIOLATION" -> AssetAccessLogger.AccessStatus.SECURITY_VIOLATION
            "NO_FALLBACK" -> AssetAccessLogger.AccessStatus.NO_FALLBACK
            "FALLBACK_MISSING" -> AssetAccessLogger.AccessStatus.FALLBACK_MISSING
            "FALLBACK_SUCCESS" -> AssetAccessLogger.AccessStatus.FALLBACK_SUCCESS
            else -> AssetAccessLogger.AccessStatus.ERROR
        }
        accessLogger.log(uri, accessStatus)
    }

    /**
     * Clear asset cache, removing all cached entries.
     *
     * Forces all subsequent asset resolutions to perform full validation
     * and filesystem access. Useful after plugin updates or installation.
     *
     * ## Thread Safety
     * This method is thread-safe.
     *
     * @since 1.0.0
     */
    suspend fun clearCache() {
        cache.clear()
        PluginLog.i(TAG, "Asset cache cleared")
    }

    /**
     * Get cache statistics for monitoring performance.
     *
     * Returns current cache size and capacity for performance analysis
     * and memory management.
     *
     * @return Map of cache metrics including "size" and "capacity"
     * @since 1.0.0
     */
    suspend fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "size" to cache.size(),
            "capacity" to cache.capacity()
        )
    }

    /**
     * Get the access logger for querying access logs.
     *
     * Provides access to asset access audit logs for security monitoring
     * and debugging.
     *
     * @return AssetAccessLogger instance
     * @since 1.0.0
     */
    fun getAccessLogger(): AssetAccessLogger {
        return accessLogger
    }
}
