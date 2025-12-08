package com.augmentalis.universal.assetmanager

/**
 * Represents an icon library containing multiple icons
 *
 * @property id Unique identifier for this library
 * @property name Display name of the library
 * @property version Version string for the library
 * @property description Optional description of the library
 * @property icons List of icons in this library
 * @property metadata Additional metadata for the library
 */
data class IconLibrary(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val icons: List<Icon> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents a single icon with multiple format support
 *
 * @property id Unique identifier within the library
 * @property name Human-readable name
 * @property svg SVG content if available
 * @property png Map of sizes to PNG byte arrays
 * @property tags Searchable tags for the icon
 * @property category Category for organization
 * @property keywords Additional search keywords
 */
data class Icon(
    val id: String,
    val name: String,
    val svg: String? = null,
    val png: Map<Int, ByteArray>? = null, // size -> bytes (e.g., 16, 24, 32, 48, 64, 128, 256)
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val keywords: List<String> = emptyList()
) {
    /**
     * Check if this icon has a specific format available
     */
    fun hasFormat(format: IconFormat): Boolean = when (format) {
        IconFormat.SVG -> svg != null
        IconFormat.PNG -> !png.isNullOrEmpty()
    }

    /**
     * Get PNG data for a specific size, or closest available size
     */
    fun getPngForSize(requestedSize: Int): ByteArray? {
        if (png.isNullOrEmpty()) return null

        // Exact match
        png[requestedSize]?.let { return it }

        // Find closest larger size
        val largerSizes = png.keys.filter { it > requestedSize }.sorted()
        if (largerSizes.isNotEmpty()) {
            return png[largerSizes.first()]
        }

        // Use largest available
        val maxSize = png.keys.maxOrNull()
        return maxSize?.let { png[it] }
    }

    /**
     * Get all available PNG sizes
     */
    fun getAvailableSizes(): List<Int> = png?.keys?.sorted() ?: emptyList()

    /**
     * Search helper to check if icon matches query
     */
    fun matchesQuery(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return name.lowercase().contains(lowerQuery) ||
               id.lowercase().contains(lowerQuery) ||
               tags.any { it.lowercase().contains(lowerQuery) } ||
               keywords.any { it.lowercase().contains(lowerQuery) } ||
               category?.lowercase()?.contains(lowerQuery) == true
    }
}

/**
 * Supported icon formats
 */
enum class IconFormat {
    SVG,
    PNG
}

/**
 * Icon reference for use in components
 *
 * Format: "library:icon-id" (e.g., "MaterialIcons:home", "custom:user-avatar")
 */
data class IconReference(
    val libraryId: String,
    val iconId: String
) {
    companion object {
        /**
         * Parse an icon reference string
         * Format: "library:icon-id"
         */
        fun parse(reference: String): IconReference? {
            val parts = reference.split(":", limit = 2)
            if (parts.size != 2) return null
            return IconReference(parts[0], parts[1])
        }
    }

    override fun toString(): String = "$libraryId:$iconId"
}
