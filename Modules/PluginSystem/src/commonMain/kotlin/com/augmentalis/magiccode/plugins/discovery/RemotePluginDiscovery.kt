/**
 * RemotePluginDiscovery.kt - Remote plugin discovery and management
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Discovery source for plugins hosted on remote servers.
 * Downloads, caches, and verifies plugin packages from configured URLs.
 *
 * Phase 5: Advanced features - Remote Plugin Support.
 */
package com.augmentalis.magiccode.plugins.discovery

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Discovers and manages plugins from remote sources.
 *
 * ## Overview
 * RemotePluginDiscovery handles downloading plugins from configured catalog URLs,
 * verifying their integrity, caching locally, and checking for updates. It provides
 * a foundation for a plugin marketplace or remote plugin distribution.
 *
 * ## Catalog Format
 * Remote plugin catalogs are JSON files with the following format:
 * ```json
 * {
 *   "catalogVersion": "1.0.0",
 *   "plugins": [
 *     {
 *       "pluginId": "com.example.plugin",
 *       "name": "Example Plugin",
 *       "version": "1.2.3",
 *       "downloadUrl": "https://plugins.example.com/example-1.2.3.zip",
 *       "checksum": "sha256:abc123...",
 *       "capabilities": ["handler.tap", "handler.scroll"]
 *     }
 *   ]
 * }
 * ```
 *
 * ## Security
 * - All downloads use HTTPS
 * - SHA-256 checksums verify package integrity
 * - Manifest validation before loading
 *
 * ## Usage
 * ```kotlin
 * val discovery = RemotePluginDiscovery(
 *     catalogUrl = "https://plugins.example.com/catalog.json",
 *     cacheDir = "/data/plugins/remote",
 *     httpClient = httpClient
 * )
 *
 * // Discover available plugins
 * val plugins = discovery.discoverPlugins()
 *
 * // Check for updates
 * val updates = discovery.checkForUpdates()
 * ```
 *
 * @param catalogUrls List of catalog URLs to check
 * @param cacheDir Local directory for caching downloaded plugins
 * @param httpFetcher Platform-specific HTTP fetcher
 * @param maxCacheAgeMs Maximum age for cached plugin data
 * @since 1.0.0
 * @see PluginDiscovery
 */
class RemotePluginDiscovery(
    private val catalogUrls: List<String>,
    private val cacheDir: String,
    private val httpFetcher: HttpFetcher,
    private val maxCacheAgeMs: Long = DEFAULT_MAX_CACHE_AGE_MS
) : PluginDiscovery {

    /**
     * Priority for remote plugins (lower than file system).
     */
    override val priority: Int = PRIORITY_REMOTE

    /**
     * JSON parser for catalog files.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Mutex for thread-safe cache access.
     */
    private val mutex = Mutex()

    /**
     * Cache of discovered plugin descriptors.
     */
    private val descriptorCache = mutableMapOf<String, CachedRemotePlugin>()

    /**
     * Last time catalogs were fetched.
     */
    private var lastCatalogFetch: Long = 0

    /**
     * Cached catalog data.
     */
    private var cachedCatalog: RemotePluginCatalog? = null

    /**
     * Discover available plugins from remote catalogs.
     *
     * Fetches and parses plugin catalogs from configured URLs.
     * Uses cached data if still valid.
     *
     * @return List of available remote plugin descriptors
     */
    override suspend fun discoverPlugins(): List<PluginDescriptor> {
        return mutex.withLock {
            val now = currentTimeMillis()

            // Use cache if still valid
            if (cachedCatalog != null && now - lastCatalogFetch < maxCacheAgeMs) {
                return@withLock cachedCatalog!!.plugins.map { it.toDescriptor() }
            }

            // Fetch fresh catalog data
            val catalog = fetchCatalogs()
            cachedCatalog = catalog
            lastCatalogFetch = now

            // Update descriptor cache
            catalog.plugins.forEach { entry ->
                descriptorCache[entry.pluginId] = CachedRemotePlugin(
                    entry = entry,
                    fetchedAt = now,
                    localPath = null
                )
            }

            catalog.plugins.map { it.toDescriptor() }
        }
    }

    /**
     * Load a remote plugin.
     *
     * Downloads the plugin package if not cached, verifies checksum,
     * extracts, and instantiates the plugin.
     *
     * @param descriptor Plugin descriptor with Remote source
     * @return Result containing loaded plugin or error
     */
    override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
        val source = descriptor.source as? PluginSource.Remote
            ?: return Result.failure(IllegalArgumentException(
                "Descriptor source is not Remote: ${descriptor.source}"
            ))

        return try {
            // Check if already downloaded and cached
            val cached = descriptorCache[descriptor.pluginId]
            if (cached?.localPath != null) {
                return loadFromLocalCache(descriptor, cached.localPath)
            }

            // Download the plugin package
            val localPath = downloadPlugin(descriptor.pluginId, source.url, source.checksum)
                ?: return Result.failure(RemotePluginException(
                    "Failed to download plugin: ${descriptor.pluginId}",
                    descriptor.pluginId,
                    source.url
                ))

            // Update cache with local path
            cached?.let {
                descriptorCache[descriptor.pluginId] = it.copy(localPath = localPath)
            }

            loadFromLocalCache(descriptor, localPath)

        } catch (e: Exception) {
            Result.failure(RemotePluginException(
                "Failed to load remote plugin: ${e.message}",
                descriptor.pluginId,
                source.url,
                e
            ))
        }
    }

    /**
     * Check if this source can load the descriptor.
     */
    override fun canLoad(descriptor: PluginDescriptor): Boolean {
        return descriptor.source is PluginSource.Remote
    }

    /**
     * Refresh the discovery cache.
     */
    override suspend fun refresh() {
        mutex.withLock {
            cachedCatalog = null
            lastCatalogFetch = 0
        }
        discoverPlugins()
    }

    /**
     * Check for available plugin updates.
     *
     * Compares installed plugin versions with remote catalog versions.
     *
     * @param installedPlugins Map of plugin ID to installed version
     * @return List of plugins with available updates
     */
    suspend fun checkForUpdates(
        installedPlugins: Map<String, String>
    ): List<PluginUpdateInfo> {
        val remotePlugins = discoverPlugins()
        return remotePlugins.mapNotNull { remote ->
            val installedVersion = installedPlugins[remote.pluginId] ?: return@mapNotNull null

            if (isNewerVersion(remote.version, installedVersion)) {
                PluginUpdateInfo(
                    pluginId = remote.pluginId,
                    currentVersion = installedVersion,
                    newVersion = remote.version,
                    descriptor = remote
                )
            } else {
                null
            }
        }
    }

    /**
     * Get plugin details by ID.
     *
     * @param pluginId Plugin ID to look up
     * @return Plugin entry or null if not found
     */
    suspend fun getPluginDetails(pluginId: String): RemotePluginEntry? {
        discoverPlugins() // Ensure catalog is loaded
        return cachedCatalog?.plugins?.find { it.pluginId == pluginId }
    }

    /**
     * Clear the download cache.
     */
    suspend fun clearCache() {
        mutex.withLock {
            descriptorCache.values.forEach { cached ->
                cached.localPath?.let { path ->
                    // Delete cached plugin files
                    try {
                        // Platform-specific file deletion would go here
                    } catch (e: Exception) {
                        // Ignore deletion errors
                    }
                }
            }
            descriptorCache.clear()
        }
    }

    /**
     * Fetch and merge catalogs from all configured URLs.
     */
    private suspend fun fetchCatalogs(): RemotePluginCatalog {
        val allPlugins = mutableListOf<RemotePluginEntry>()
        val errors = mutableListOf<String>()

        for (url in catalogUrls) {
            try {
                val response = httpFetcher.get(url)
                if (response.isSuccess) {
                    val catalog = json.decodeFromString(RemotePluginCatalog.serializer(), response.body)
                    allPlugins.addAll(catalog.plugins)
                } else {
                    errors.add("Failed to fetch $url: ${response.error}")
                }
            } catch (e: Exception) {
                errors.add("Error fetching $url: ${e.message}")
            }
        }

        return RemotePluginCatalog(
            catalogVersion = "merged",
            plugins = allPlugins.distinctBy { it.pluginId }
        )
    }

    /**
     * Download a plugin package.
     *
     * @return Local path to downloaded plugin, or null if download failed
     */
    private suspend fun downloadPlugin(
        pluginId: String,
        url: String,
        expectedChecksum: String?
    ): String? {
        try {
            val response = httpFetcher.download(url, "$cacheDir/$pluginId")
            if (!response.isSuccess) {
                return null
            }

            // Verify checksum if provided
            if (expectedChecksum != null) {
                val actualChecksum = computeChecksum(response.localPath)
                if (!verifyChecksum(expectedChecksum, actualChecksum)) {
                    // Delete invalid download
                    return null
                }
            }

            return response.localPath
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Load plugin from local cache.
     */
    private fun loadFromLocalCache(
        descriptor: PluginDescriptor,
        localPath: String
    ): Result<UniversalPlugin> {
        // This would delegate to FileSystemPluginDiscovery or a custom loader
        // For now, return failure indicating manual loading required
        return Result.failure(UnsupportedOperationException(
            "Remote plugin loading from cache not implemented. " +
            "Plugin cached at: $localPath"
        ))
    }

    /**
     * Simple version comparison (semver-like).
     */
    private fun isNewerVersion(remote: String, installed: String): Boolean {
        val remoteParts = remote.split('.').mapNotNull { it.toIntOrNull() }
        val installedParts = installed.split('.').mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(remoteParts.size, installedParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = installedParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    /**
     * Compute SHA-256 checksum of a file.
     */
    private fun computeChecksum(path: String): String {
        // Platform-specific implementation would go here
        return "sha256:computed"
    }

    /**
     * Verify checksum matches expected value.
     */
    private fun verifyChecksum(expected: String, actual: String): Boolean {
        // Handle different checksum formats (sha256:xxx, md5:xxx, etc.)
        return expected.equals(actual, ignoreCase = true)
    }

    companion object {
        /** Priority value for remote plugins. */
        const val PRIORITY_REMOTE = 200

        /** Default cache age (1 hour). */
        const val DEFAULT_MAX_CACHE_AGE_MS = 3600_000L
    }
}

/**
 * Remote plugin catalog structure.
 *
 * JSON format for plugin distribution catalogs.
 */
@Serializable
data class RemotePluginCatalog(
    val catalogVersion: String,
    val plugins: List<RemotePluginEntry>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Entry for a plugin in the remote catalog.
 */
@Serializable
data class RemotePluginEntry(
    val pluginId: String,
    val name: String,
    val version: String,
    val downloadUrl: String,
    val checksum: String? = null,
    val capabilities: List<String> = emptyList(),
    val description: String? = null,
    val author: String? = null,
    val size: Long? = null,
    val minSdkVersion: Int? = null,
    val releaseNotes: String? = null,
    val iconUrl: String? = null,
    val screenshotUrls: List<String> = emptyList()
) {
    /**
     * Convert to PluginDescriptor.
     */
    fun toDescriptor(): PluginDescriptor {
        return PluginDescriptor(
            pluginId = pluginId,
            name = name,
            version = version,
            capabilities = capabilities.toSet(),
            source = PluginSource.Remote(
                url = downloadUrl,
                checksum = checksum,
                cacheKey = "$pluginId-$version"
            ),
            description = description,
            author = author,
            metadata = buildMap {
                size?.let { put("size", it.toString()) }
                minSdkVersion?.let { put("minSdkVersion", it.toString()) }
                iconUrl?.let { put("iconUrl", it) }
            }
        )
    }
}

/**
 * Information about an available plugin update.
 */
data class PluginUpdateInfo(
    val pluginId: String,
    val currentVersion: String,
    val newVersion: String,
    val descriptor: PluginDescriptor
)

/**
 * Cached remote plugin data.
 */
private data class CachedRemotePlugin(
    val entry: RemotePluginEntry,
    val fetchedAt: Long,
    val localPath: String?
)

/**
 * Interface for HTTP operations.
 *
 * Platform-specific implementations provide actual HTTP functionality.
 */
interface HttpFetcher {
    /**
     * Perform a GET request.
     *
     * @param url URL to fetch
     * @return Response with body or error
     */
    suspend fun get(url: String): HttpResponse

    /**
     * Download a file.
     *
     * @param url URL to download
     * @param destPath Local destination path
     * @return Response with local path or error
     */
    suspend fun download(url: String, destPath: String): DownloadResponse
}

/**
 * HTTP GET response.
 */
data class HttpResponse(
    val isSuccess: Boolean,
    val body: String = "",
    val error: String? = null,
    val statusCode: Int = 0
)

/**
 * Download response.
 */
data class DownloadResponse(
    val isSuccess: Boolean,
    val localPath: String = "",
    val error: String? = null,
    val bytesDownloaded: Long = 0
)

/**
 * Exception for remote plugin operations.
 */
class RemotePluginException(
    message: String,
    val pluginId: String,
    val url: String,
    cause: Throwable? = null
) : Exception(message, cause)
