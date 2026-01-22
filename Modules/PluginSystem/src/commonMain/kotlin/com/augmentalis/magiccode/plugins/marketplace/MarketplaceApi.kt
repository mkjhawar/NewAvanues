package com.augmentalis.magiccode.plugins.marketplace

import kotlinx.serialization.Serializable

/**
 * Marketplace API contract for plugin discovery and distribution.
 *
 * MarketplaceApi defines the interface for interacting with the plugin
 * marketplace backend. It provides operations for searching, retrieving
 * plugin details, downloading packages, and checking for updates.
 *
 * ## Operations
 * The API supports the following operations:
 *
 * ### Discovery
 * - [search]: Find plugins by query with filtering and sorting
 * - [getDetails]: Get full details for a specific plugin
 * - [getVersions]: List all available versions of a plugin
 *
 * ### Distribution
 * - [download]: Download a specific plugin version
 *
 * ### Updates
 * - [checkUpdates]: Check for updates to installed plugins
 *
 * ## Error Handling
 * All operations return [Result] to enable proper error handling:
 * ```kotlin
 * when (val result = api.search("voice commands")) {
 *     is Result.Success -> handleResults(result.value)
 *     is Result.Failure -> handleError(result.exception)
 * }
 * ```
 *
 * ## Implementation Notes
 * Implementations should:
 * - Handle network timeouts and retries
 * - Implement proper caching (see [MarketplaceCache])
 * - Validate responses before returning
 * - Log errors for debugging
 *
 * @since 1.0.0
 * @see PluginListing
 * @see PluginDetails
 * @see PluginPackage
 */
interface MarketplaceApi {

    /**
     * Search for plugins matching the given query.
     *
     * Searches plugin names, descriptions, capabilities, and tags.
     * Results are paginated and can be filtered and sorted.
     *
     * ## Search Syntax
     * The query supports basic search syntax:
     * - Simple terms: `voice commands`
     * - Quoted phrases: `"voice commands"`
     * - Capability prefix: `capability:voice.commands`
     * - Author prefix: `author:acme`
     *
     * ## Example
     * ```kotlin
     * val results = api.search(
     *     query = "voice assistant",
     *     filters = SearchFilters(
     *         capability = "voice.commands",
     *         minRating = 4.0f,
     *         sortBy = SortOption.RATING
     *     )
     * )
     * ```
     *
     * @param query Search query string
     * @param filters Optional filters to narrow results
     * @return Result containing list of matching plugins, or error
     */
    suspend fun search(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): Result<List<PluginListing>>

    /**
     * Get detailed information about a specific plugin.
     *
     * Returns comprehensive plugin information including full description,
     * screenshots, changelog, requirements, and permissions.
     *
     * @param pluginId Unique plugin identifier
     * @return Result containing plugin details, or error if not found
     */
    suspend fun getDetails(pluginId: String): Result<PluginDetails>

    /**
     * Get all available versions of a plugin.
     *
     * Returns version history sorted by release date (newest first).
     * Includes changelog and size information for each version.
     *
     * @param pluginId Unique plugin identifier
     * @return Result containing list of version info, or error
     */
    suspend fun getVersions(pluginId: String): Result<List<VersionInfo>>

    /**
     * Download a specific plugin version.
     *
     * Downloads the complete plugin package including manifest,
     * code, and assets. The package is signed and checksummed
     * for verification.
     *
     * ## Download Flow
     * 1. Request download from marketplace
     * 2. Receive signed package
     * 3. Verify checksum
     * 4. Verify signature
     * 5. Extract and install
     *
     * @param pluginId Unique plugin identifier
     * @param version Specific version to download (e.g., "1.0.0")
     * @return Result containing plugin package, or error
     */
    suspend fun download(
        pluginId: String,
        version: String
    ): Result<PluginPackage>

    /**
     * Check for available updates to installed plugins.
     *
     * Compares installed versions against marketplace to find
     * plugins with newer versions available.
     *
     * ## Example
     * ```kotlin
     * val installed = mapOf(
     *     "com.example.plugin1" to "1.0.0",
     *     "com.example.plugin2" to "2.1.0"
     * )
     * val updates = api.checkUpdates(installed)
     * ```
     *
     * @param installed Map of plugin ID to currently installed version
     * @return Result containing list of available updates, or error
     */
    suspend fun checkUpdates(
        installed: Map<String, String>
    ): Result<List<UpdateInfo>>
}

/**
 * Search filters for marketplace queries.
 *
 * Allows narrowing search results by capability, category,
 * minimum rating, and sort order.
 *
 * ## Filter Behavior
 * - All filters are optional and can be combined
 * - Multiple filters use AND logic
 * - Null/empty filters are ignored
 *
 * ## Example
 * ```kotlin
 * val filters = SearchFilters(
 *     capability = "voice.commands",
 *     category = "accessibility",
 *     minRating = 4.0f,
 *     sortBy = SortOption.DOWNLOADS
 * )
 * ```
 *
 * @property capability Filter by plugin capability (e.g., "voice.commands")
 * @property category Filter by category (e.g., "accessibility", "productivity")
 * @property minRating Minimum rating filter (0.0 to 5.0)
 * @property sortBy Result sort order
 * @since 1.0.0
 * @see MarketplaceApi.search
 * @see SortOption
 */
@Serializable
data class SearchFilters(
    val capability: String? = null,
    val category: String? = null,
    val minRating: Float? = null,
    val sortBy: SortOption = SortOption.RELEVANCE
) {
    /**
     * Check if any filters are active.
     *
     * @return true if at least one filter is set
     */
    fun hasActiveFilters(): Boolean {
        return capability != null || category != null || minRating != null
    }

    /**
     * Create a copy with updated capability filter.
     *
     * @param capability New capability filter value
     * @return New SearchFilters with updated capability
     */
    fun withCapability(capability: String?): SearchFilters {
        return copy(capability = capability)
    }

    /**
     * Create a copy with updated category filter.
     *
     * @param category New category filter value
     * @return New SearchFilters with updated category
     */
    fun withCategory(category: String?): SearchFilters {
        return copy(category = category)
    }

    /**
     * Create a copy with updated rating filter.
     *
     * @param minRating New minimum rating filter value
     * @return New SearchFilters with updated rating
     */
    fun withMinRating(minRating: Float?): SearchFilters {
        return copy(minRating = minRating)
    }

    /**
     * Create a copy with updated sort option.
     *
     * @param sortBy New sort option
     * @return New SearchFilters with updated sort
     */
    fun withSortBy(sortBy: SortOption): SearchFilters {
        return copy(sortBy = sortBy)
    }

    /**
     * Clear all filters.
     *
     * @return New SearchFilters with default values
     */
    fun clearAll(): SearchFilters {
        return SearchFilters()
    }
}

/**
 * Sort options for marketplace search results.
 *
 * Determines the order in which search results are returned.
 *
 * @since 1.0.0
 * @see SearchFilters
 */
@Serializable
enum class SortOption {
    /**
     * Sort by relevance to search query (default).
     * Uses text matching score and other relevance signals.
     */
    RELEVANCE,

    /**
     * Sort by total download count (highest first).
     * Shows most popular plugins first.
     */
    DOWNLOADS,

    /**
     * Sort by average user rating (highest first).
     * Shows highest rated plugins first.
     */
    RATING,

    /**
     * Sort by publication/update date (newest first).
     * Shows recently updated plugins first.
     */
    RECENT;

    /**
     * Get human-readable display name for this sort option.
     *
     * @return Display name string
     */
    fun getDisplayName(): String {
        return when (this) {
            RELEVANCE -> "Relevance"
            DOWNLOADS -> "Most Downloaded"
            RATING -> "Highest Rated"
            RECENT -> "Recently Updated"
        }
    }
}

/**
 * Marketplace API error types.
 *
 * Represents different error conditions that can occur during
 * marketplace operations.
 *
 * @since 1.0.0
 */
sealed class MarketplaceError : Exception() {

    /**
     * Plugin was not found in the marketplace.
     *
     * @property pluginId The plugin ID that was not found
     */
    data class NotFound(val pluginId: String) : MarketplaceError() {
        override val message: String = "Plugin not found: $pluginId"
    }

    /**
     * Network error during API call.
     *
     * @property cause Underlying network exception
     */
    data class NetworkError(override val cause: Throwable?) : MarketplaceError() {
        override val message: String = "Network error: ${cause?.message}"
    }

    /**
     * Server returned an error response.
     *
     * @property statusCode HTTP status code
     * @property serverMessage Error message from server
     */
    data class ServerError(
        val statusCode: Int,
        val serverMessage: String
    ) : MarketplaceError() {
        override val message: String = "Server error ($statusCode): $serverMessage"
    }

    /**
     * Rate limit exceeded.
     *
     * @property retryAfterSeconds Seconds to wait before retrying
     */
    data class RateLimited(val retryAfterSeconds: Int) : MarketplaceError() {
        override val message: String = "Rate limited. Retry after $retryAfterSeconds seconds"
    }

    /**
     * Authentication or authorization error.
     *
     * @property reason Specific auth failure reason
     */
    data class Unauthorized(val reason: String) : MarketplaceError() {
        override val message: String = "Unauthorized: $reason"
    }

    /**
     * Invalid request parameters.
     *
     * @property parameter Name of invalid parameter
     * @property reason Why it's invalid
     */
    data class InvalidRequest(
        val parameter: String,
        val reason: String
    ) : MarketplaceError() {
        override val message: String = "Invalid request - $parameter: $reason"
    }

    /**
     * Package download failed.
     *
     * @property pluginId Plugin that failed to download
     * @property reason Failure reason
     */
    data class DownloadFailed(
        val pluginId: String,
        val reason: String
    ) : MarketplaceError() {
        override val message: String = "Download failed for $pluginId: $reason"
    }

    /**
     * Package verification failed.
     *
     * @property pluginId Plugin with verification failure
     * @property reason Specific verification failure reason
     */
    data class VerificationFailed(
        val pluginId: String,
        val reason: String
    ) : MarketplaceError() {
        override val message: String = "Verification failed for $pluginId: $reason"
    }
}
