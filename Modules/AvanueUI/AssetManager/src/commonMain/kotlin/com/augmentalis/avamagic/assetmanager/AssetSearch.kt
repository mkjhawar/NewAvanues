package com.augmentalis.avamagic.assetmanager

import com.augmentalis.universal.assetmanager.AssetManager
import com.augmentalis.universal.assetmanager.Icon
import com.augmentalis.universal.assetmanager.IconLibrary
import com.augmentalis.universal.assetmanager.ImageAsset
import com.augmentalis.universal.assetmanager.ImageLibrary
import com.augmentalis.universal.assetmanager.ImageFormat
import com.augmentalis.universal.assetmanager.Dimensions

/**
 * Asset search engine with relevance scoring
 *
 * Provides fast, accurate search across icon and image libraries with
 * intelligent ranking based on multiple factors.
 */
class AssetSearch(
    private val manager: AssetManager
) {

    /**
     * Search for icons across all libraries
     *
     * @param query Search query string
     * @param filters Optional filters to apply
     * @param limit Maximum number of results (default: 50)
     * @return List of icon search results sorted by relevance
     */
    suspend fun searchIcons(
        query: String,
        filters: SearchFilters = SearchFilters(),
        limit: Int = 50
    ): List<IconSearchResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<IconSearchResult>()
        val lowerQuery = query.lowercase().trim()

        manager.getAllIconLibraries().forEach { library ->
            // Apply library filter
            if (filters.libraryIds.isNotEmpty() &&
                library.id !in filters.libraryIds) {
                return@forEach
            }

            library.icons.forEach { icon ->
                // Apply category filter
                if (filters.categories.isNotEmpty() &&
                    icon.category !in filters.categories) {
                    return@forEach
                }

                // Apply tag filter
                if (filters.tags.isNotEmpty() &&
                    filters.tags.none { it in icon.tags }) {
                    return@forEach
                }

                val score = calculateIconRelevance(lowerQuery, icon, library)
                if (score > 0) {
                    results.add(
                        IconSearchResult(
                            libraryId = library.id,
                            libraryName = library.name,
                            icon = icon,
                            relevanceScore = score,
                            matchedFields = getMatchedFields(lowerQuery, icon)
                        )
                    )
                }
            }
        }

        return results
            .sortedByDescending { it.relevanceScore }
            .take(limit)
    }

    /**
     * Search for images across all libraries
     *
     * @param query Search query string
     * @param filters Optional filters to apply
     * @param limit Maximum number of results (default: 50)
     * @return List of image search results sorted by relevance
     */
    suspend fun searchImages(
        query: String,
        filters: SearchFilters = SearchFilters(),
        limit: Int = 50
    ): List<ImageSearchResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<ImageSearchResult>()
        val lowerQuery = query.lowercase().trim()

        manager.getAllImageLibraries().forEach { library ->
            // Apply library filter
            if (filters.libraryIds.isNotEmpty() &&
                library.id !in filters.libraryIds) {
                return@forEach
            }

            library.images.forEach { image ->
                // Apply category filter
                if (filters.categories.isNotEmpty() &&
                    image.category !in filters.categories) {
                    return@forEach
                }

                // Apply tag filter
                if (filters.tags.isNotEmpty() &&
                    filters.tags.none { it in image.tags }) {
                    return@forEach
                }

                val score = calculateImageRelevance(lowerQuery, image, library)
                if (score > 0) {
                    results.add(
                        ImageSearchResult(
                            libraryId = library.id,
                            libraryName = library.name,
                            image = image,
                            relevanceScore = score,
                            matchedFields = getMatchedFields(lowerQuery, image)
                        )
                    )
                }
            }
        }

        return results
            .sortedByDescending { it.relevanceScore }
            .take(limit)
    }

    /**
     * Get search suggestions based on partial query
     *
     * @param partialQuery Partial search query
     * @param limit Maximum number of suggestions
     * @return List of suggested search terms
     */
    suspend fun getSuggestions(partialQuery: String, limit: Int = 10): List<String> {
        if (partialQuery.length < 2) return emptyList()

        val suggestions = mutableSetOf<String>()
        val lowerQuery = partialQuery.lowercase()

        // Collect suggestions from icon names, tags, and keywords
        manager.getAllIconLibraries().forEach { library ->
            library.icons.forEach { icon ->
                if (icon.name.lowercase().startsWith(lowerQuery)) {
                    suggestions.add(icon.name)
                }
                icon.tags.forEach { tag ->
                    if (tag.lowercase().startsWith(lowerQuery)) {
                        suggestions.add(tag)
                    }
                }
                icon.keywords.forEach { keyword ->
                    if (keyword.lowercase().startsWith(lowerQuery)) {
                        suggestions.add(keyword)
                    }
                }
            }
        }

        return suggestions.take(limit)
    }

    /**
     * Search by category
     *
     * @param category Category name
     * @param type Asset type (icon or image)
     * @return List of assets in the category
     */
    suspend fun searchByCategory(
        category: String,
        type: AssetType
    ): List<SearchResult> {
        return when (type) {
            AssetType.ICON -> searchIcons(
                query = "",
                filters = SearchFilters(categories = setOf(category))
            )
            AssetType.IMAGE -> searchImages(
                query = "",
                filters = SearchFilters(categories = setOf(category))
            )
        }
    }

    /**
     * Search by tags
     *
     * @param tags Set of tags to match
     * @param matchAll If true, asset must have all tags; if false, any tag
     * @return List of matching assets
     */
    suspend fun searchByTags(
        tags: Set<String>,
        matchAll: Boolean = false
    ): List<SearchResult> {
        val iconResults = searchIcons(
            query = tags.first(), // Use first tag as base query
            filters = SearchFilters(tags = tags)
        )

        val imageResults = searchImages(
            query = tags.first(),
            filters = SearchFilters(tags = tags)
        )

        return (iconResults + imageResults).sortedByDescending { it.relevanceScore }
    }

    /**
     * Calculate relevance score for an icon
     */
    private fun calculateIconRelevance(query: String, icon: Icon, library: IconLibrary): Int {
        var score = 0

        // Exact match (highest priority)
        if (icon.id.lowercase() == query) score += 100
        if (icon.name.lowercase() == query) score += 95

        // Starts with (high priority)
        if (icon.id.lowercase().startsWith(query)) score += 60
        if (icon.name.lowercase().startsWith(query)) score += 55

        // Contains (medium priority)
        if (icon.id.lowercase().contains(query)) score += 30
        if (icon.name.lowercase().contains(query)) score += 25

        // Tag matches (high priority)
        icon.tags.forEach { tag ->
            when {
                tag.lowercase() == query -> score += 50
                tag.lowercase().startsWith(query) -> score += 35
                tag.lowercase().contains(query) -> score += 15
            }
        }

        // Keyword matches (medium priority)
        icon.keywords.forEach { keyword ->
            when {
                keyword.lowercase() == query -> score += 40
                keyword.lowercase().startsWith(query) -> score += 25
                keyword.lowercase().contains(query) -> score += 10
            }
        }

        // Category match (bonus)
        icon.category?.let { category ->
            if (category.lowercase().contains(query)) score += 20
        }

        // Library name match (small bonus)
        if (library.name.lowercase().contains(query)) score += 5

        // Multi-word query handling
        if (query.contains(" ")) {
            val words = query.split(" ")
            words.forEach { word ->
                if (icon.name.lowercase().contains(word)) score += 10
                icon.tags.forEach { tag ->
                    if (tag.lowercase().contains(word)) score += 5
                }
            }
        }

        return score
    }

    /**
     * Calculate relevance score for an image
     */
    private fun calculateImageRelevance(
        query: String,
        image: ImageAsset,
        library: ImageLibrary
    ): Int {
        var score = 0

        // Exact match
        if (image.id.lowercase() == query) score += 100
        if (image.name.lowercase() == query) score += 95

        // Starts with
        if (image.id.lowercase().startsWith(query)) score += 60
        if (image.name.lowercase().startsWith(query)) score += 55

        // Contains
        if (image.id.lowercase().contains(query)) score += 30
        if (image.name.lowercase().contains(query)) score += 25

        // Tag matches
        image.tags.forEach { tag ->
            when {
                tag.lowercase() == query -> score += 50
                tag.lowercase().startsWith(query) -> score += 35
                tag.lowercase().contains(query) -> score += 15
            }
        }

        // Category match
        image.category?.let { category ->
            if (category.lowercase().contains(query)) score += 20
        }

        // Metadata matches
        image.metadata.values.forEach { value ->
            if (value.lowercase().contains(query)) score += 10
        }

        return score
    }

    /**
     * Get list of fields that matched the query
     */
    private fun getMatchedFields(query: String, icon: Icon): List<String> {
        val fields = mutableListOf<String>()

        if (icon.id.lowercase().contains(query)) fields.add("id")
        if (icon.name.lowercase().contains(query)) fields.add("name")
        if (icon.tags.any { it.lowercase().contains(query) }) fields.add("tags")
        if (icon.keywords.any { it.lowercase().contains(query) }) fields.add("keywords")
        icon.category?.let {
            if (it.lowercase().contains(query)) fields.add("category")
        }

        return fields
    }

    /**
     * Get list of fields that matched the query (image variant)
     */
    private fun getMatchedFields(query: String, image: ImageAsset): List<String> {
        val fields = mutableListOf<String>()

        if (image.id.lowercase().contains(query)) fields.add("id")
        if (image.name.lowercase().contains(query)) fields.add("name")
        if (image.tags.any { it.lowercase().contains(query) }) fields.add("tags")
        image.category?.let {
            if (it.lowercase().contains(query)) fields.add("category")
        }

        return fields
    }
}

/**
 * Search filters
 */
data class SearchFilters(
    val libraryIds: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val formats: Set<ImageFormat> = emptySet(),
    val minSize: Dimensions? = null,
    val maxSize: Dimensions? = null
)

/**
 * Base search result interface
 */
sealed interface SearchResult {
    val libraryId: String
    val libraryName: String
    val relevanceScore: Int
    val matchedFields: List<String>
}

/**
 * Icon search result
 */
data class IconSearchResult(
    override val libraryId: String,
    override val libraryName: String,
    val icon: Icon,
    override val relevanceScore: Int,
    override val matchedFields: List<String>
) : SearchResult

/**
 * Image search result
 */
data class ImageSearchResult(
    override val libraryId: String,
    override val libraryName: String,
    val image: ImageAsset,
    override val relevanceScore: Int,
    override val matchedFields: List<String>
) : SearchResult

/**
 * Asset type enum
 */
enum class AssetType {
    ICON,
    IMAGE
}

/**
 * Search statistics
 */
data class SearchStats(
    val totalResults: Int,
    val iconResults: Int,
    val imageResults: Int,
    val searchTimeMs: Long,
    val librariesSearched: Int
)

/**
 * Search index for faster lookups (future optimization)
 */
class SearchIndex {
    private val iconIndex = mutableMapOf<String, MutableList<IconSearchEntry>>()
    private val imageIndex = mutableMapOf<String, MutableList<ImageSearchEntry>>()

    /**
     * Add icon to index
     */
    fun indexIcon(libraryId: String, icon: Icon) {
        val terms = buildSearchTerms(icon)
        terms.forEach { term ->
            iconIndex.getOrPut(term) { mutableListOf() }
                .add(IconSearchEntry(libraryId, icon))
        }
    }

    /**
     * Add image to index
     */
    fun indexImage(libraryId: String, image: ImageAsset) {
        val terms = buildSearchTerms(image)
        terms.forEach { term ->
            imageIndex.getOrPut(term) { mutableListOf() }
                .add(ImageSearchEntry(libraryId, image))
        }
    }

    /**
     * Build search terms from icon
     */
    private fun buildSearchTerms(icon: Icon): Set<String> {
        val terms = mutableSetOf<String>()

        terms.add(icon.id.lowercase())
        terms.add(icon.name.lowercase())
        terms.addAll(icon.tags.map { it.lowercase() })
        terms.addAll(icon.keywords.map { it.lowercase() })
        icon.category?.let { terms.add(it.lowercase()) }

        // Add word-level terms
        terms.addAll(icon.name.lowercase().split(" ", "-", "_"))

        return terms
    }

    /**
     * Build search terms from image
     */
    private fun buildSearchTerms(image: ImageAsset): Set<String> {
        val terms = mutableSetOf<String>()

        terms.add(image.id.lowercase())
        terms.add(image.name.lowercase())
        terms.addAll(image.tags.map { it.lowercase() })
        image.category?.let { terms.add(it.lowercase()) }

        // Add word-level terms
        terms.addAll(image.name.lowercase().split(" ", "-", "_"))

        return terms
    }

    /**
     * Clear index
     */
    fun clear() {
        iconIndex.clear()
        imageIndex.clear()
    }

    /**
     * Get index size
     */
    fun size(): Int = iconIndex.size + imageIndex.size
}

/**
 * Icon search index entry
 */
private data class IconSearchEntry(
    val libraryId: String,
    val icon: Icon
)

/**
 * Image search index entry
 */
private data class ImageSearchEntry(
    val libraryId: String,
    val image: ImageAsset
)
