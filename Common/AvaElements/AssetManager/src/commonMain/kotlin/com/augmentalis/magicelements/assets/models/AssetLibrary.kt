package com.augmentalis.avaelements.assets.models

import kotlinx.serialization.Serializable

/**
 * Represents a collection of icons or images
 * (e.g., Material Icons, Font Awesome)
 */
@Serializable
data class AssetLibrary(
    /** Unique library ID */
    val id: String,

    /** Library name */
    val name: String,

    /** Library version */
    val version: String,

    /** Total number of assets */
    val assetCount: Int,

    /** Library categories */
    val categories: List<String> = emptyList(),

    /** License information */
    val license: String? = null,

    /** Attribution text */
    val attribution: String? = null,

    /** Library website/URL */
    val url: String? = null,

    /** Library description */
    val description: String? = null
)

/**
 * Search query for assets
 */
data class AssetQuery(
    /** Search text (name, tags, aliases) */
    val query: String? = null,

    /** Filter by library */
    val library: String? = null,

    /** Filter by category */
    val category: String? = null,

    /** Filter by tags */
    val tags: List<String> = emptyList(),

    /** Maximum results */
    val limit: Int = 50,

    /** Result offset for pagination */
    val offset: Int = 0
)

/**
 * Search result
 */
data class AssetSearchResult<T>(
    /** Matching assets */
    val results: List<T>,

    /** Total count (for pagination) */
    val totalCount: Int,

    /** Query that produced this result */
    val query: AssetQuery
)
