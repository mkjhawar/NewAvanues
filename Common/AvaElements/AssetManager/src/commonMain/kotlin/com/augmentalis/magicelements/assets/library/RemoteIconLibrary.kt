package com.augmentalis.avaelements.assets.library

import com.augmentalis.avaelements.assets.AssetStorage
import com.augmentalis.avaelements.assets.models.*
import com.augmentalis.avaelements.assets.utils.AssetUtils

/**
 * Remote icon library with CDN + local cache
 *
 * Downloads icons on-demand from CDN and caches locally.
 * Zero bloat - only bundled data is lightweight metadata.
 */
class RemoteIconLibrary(
    private val config: LibraryConfig,
    private val storage: AssetStorage,
    private val httpClient: HttpClient
) : IconLibraryProvider {

    override val libraryId: String = config.id

    // In-memory cache of manifest (loaded once)
    private var manifestCache: List<IconMetadata>? = null

    override suspend fun getLibraryInfo(): AssetLibrary {
        return AssetLibrary(
            id = config.id,
            name = config.name,
            version = config.version,
            assetCount = getManifest().size,
            license = config.license,
            url = config.cdnBaseUrl
        )
    }

    override suspend fun getIconManifest(limit: Int, offset: Int): List<IconMetadata> {
        return getManifest()
            .drop(offset)
            .take(limit)
    }

    override suspend fun searchManifest(query: String): List<IconMetadata> {
        val manifest = getManifest()

        return manifest
            .map { metadata ->
                val score = AssetUtils.calculateRelevanceScore(
                    query,
                    metadata.name,
                    metadata.tags,
                    metadata.aliases
                )
                metadata to score
            }
            .filter { (_, score) -> score > 0.2f }
            .sortedByDescending { (_, score) -> score }
            .map { (metadata, _) -> metadata }
    }

    override suspend fun loadIcon(iconId: String): Icon? {
        // Check if already in storage
        val cached = storage.getIcon(iconId)
        if (cached != null) {
            return cached
        }

        // Find in manifest
        val metadata = getManifest().find { it.id == iconId }
            ?: return null

        // Download from CDN
        val iconData = if (metadata.url != null) {
            httpClient.downloadIcon(metadata.url)
        } else {
            // Construct URL from library config
            val iconPath = "${iconId.replace("${config.id}:", "")}.svg"
            val url = "${config.cdnBaseUrl}/$iconPath"
            httpClient.downloadIcon(url)
        }

        // Create Icon object
        val icon = Icon(
            id = iconId,
            name = metadata.name,
            svg = iconData.decodeToString(),
            tags = metadata.tags,
            library = config.id,
            category = metadata.category,
            aliases = metadata.aliases
        )

        // Cache it
        storage.storeIcon(icon)

        return icon
    }

    override suspend fun preloadIcons(
        iconIds: List<String>,
        onProgress: ((Int, Int) -> Unit)?
    ) {
        val total = iconIds.size
        iconIds.forEachIndexed { index, iconId ->
            loadIcon(iconId)
            onProgress?.invoke(index + 1, total)
        }
    }

    override suspend fun isCached(iconId: String): Boolean {
        return storage.getIcon(iconId) != null
    }

    override suspend fun clearCache() {
        storage.clearLibrary(config.id)
    }

    override suspend fun getCacheStats(): LibraryCacheStats {
        val manifest = getManifest()
        val totalIcons = manifest.size

        var cachedCount = 0
        manifest.forEach { metadata ->
            if (isCached(metadata.id)) {
                cachedCount++
            }
        }

        val stats = storage.getStats()

        return LibraryCacheStats(
            totalIcons = totalIcons,
            cachedIcons = cachedCount,
            cacheSizeBytes = stats.totalSizeBytes,
            hitRate = stats.cacheHitRate ?: 0f
        )
    }

    /**
     * Get or load manifest
     */
    private suspend fun getManifest(): List<IconMetadata> {
        if (manifestCache != null) {
            return manifestCache!!
        }

        // Load from bundled resource or download
        val manifest = httpClient.downloadManifest(config.manifestUrl)
        manifestCache = manifest
        return manifest
    }

    /**
     * Auto-cache popular icons on first use
     */
    suspend fun initializePopularIcons() {
        if (config.autoCachePopular && config.popularIcons.isNotEmpty()) {
            config.popularIcons.forEach { iconName ->
                val iconId = "${config.id}:$iconName"
                if (!isCached(iconId)) {
                    try {
                        loadIcon(iconId)
                    } catch (e: Exception) {
                        // Ignore failures for popular icons
                    }
                }
            }
        }
    }
}

/**
 * HTTP client interface for downloading icons
 *
 * Platform-specific implementations (Ktor, OkHttp, URLSession)
 */
interface HttpClient {
    /**
     * Download icon data
     *
     * @param url Icon URL
     * @return Icon bytes (SVG or PNG)
     */
    suspend fun downloadIcon(url: String): ByteArray

    /**
     * Download and parse manifest
     *
     * @param url Manifest URL
     * @return List of icon metadata
     */
    suspend fun downloadManifest(url: String): List<IconMetadata>
}

/**
 * Expect/actual for platform HTTP clients
 */
expect fun createHttpClient(): HttpClient
