package com.augmentalis.avaelements.assets.utils

import com.augmentalis.avaelements.assets.models.*

/**
 * Utility functions for asset processing
 */
object AssetUtils {
    /**
     * Detect image format from filename
     *
     * @param fileName Filename with extension
     * @return ImageFormat or null if unknown
     */
    fun detectImageFormat(fileName: String): ImageFormat? {
        val extension = fileName.substringAfterLast('.', "")
        return ImageFormat.fromExtension(extension)
    }

    /**
     * Extract search tags from filename
     *
     * Examples:
     * - "home_icon.svg" → ["home", "icon"]
     * - "user-profile.png" → ["user", "profile"]
     *
     * @param fileName Filename
     * @return List of tags
     */
    fun extractTagsFromFileName(fileName: String): List<String> {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return nameWithoutExt
            .split(Regex("[_\\-\\s]+"))
            .map { it.lowercase() }
            .filter { it.isNotBlank() }
    }

    /**
     * Calculate thumbnail dimensions maintaining aspect ratio
     *
     * @param originalWidth Original image width
     * @param originalHeight Original image height
     * @param maxWidth Maximum thumbnail width
     * @param maxHeight Maximum thumbnail height
     * @return Thumbnail dimensions
     */
    fun calculateThumbnailDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Dimensions {
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        return when {
            originalWidth <= maxWidth && originalHeight <= maxHeight -> {
                // Image is already smaller than max dimensions
                Dimensions(originalWidth, originalHeight)
            }
            originalWidth > originalHeight -> {
                // Landscape
                val width = minOf(originalWidth, maxWidth)
                val height = (width / aspectRatio).toInt()
                Dimensions(width, height)
            }
            else -> {
                // Portrait or square
                val height = minOf(originalHeight, maxHeight)
                val width = (height * aspectRatio).toInt()
                Dimensions(width, height)
            }
        }
    }

    /**
     * Validate icon ID format
     *
     * Valid formats:
     * - "library:name" (e.g., "material:home")
     * - "name" (custom icon)
     *
     * @param iconId Icon ID to validate
     * @return true if valid
     */
    fun isValidIconId(iconId: String): Boolean {
        if (iconId.isBlank()) return false

        val parts = iconId.split(":")
        return when (parts.size) {
            1 -> parts[0].isNotBlank()
            2 -> parts[0].isNotBlank() && parts[1].isNotBlank()
            else -> false
        }
    }

    /**
     * Parse library and name from icon ID
     *
     * @param iconId Icon ID (format: "library:name" or "name")
     * @return Pair of (library, name) or (null, name)
     */
    fun parseIconId(iconId: String): Pair<String?, String> {
        val parts = iconId.split(":")
        return when (parts.size) {
            1 -> null to parts[0]
            2 -> parts[0] to parts[1]
            else -> null to iconId
        }
    }

    /**
     * Calculate search relevance score
     *
     * @param query Search query
     * @param name Asset name
     * @param tags Asset tags
     * @param aliases Asset aliases
     * @return Relevance score (0.0 to 1.0)
     */
    fun calculateRelevanceScore(
        query: String,
        name: String,
        tags: List<String>,
        aliases: List<String> = emptyList()
    ): Float {
        val queryLower = query.lowercase()
        val nameLower = name.lowercase()

        // Exact match
        if (nameLower == queryLower) return 1.0f

        // Name starts with query
        if (nameLower.startsWith(queryLower)) return 0.9f

        // Name contains query
        if (nameLower.contains(queryLower)) {
            val position = nameLower.indexOf(queryLower)
            val positionScore = 1.0f - (position.toFloat() / nameLower.length)
            return 0.7f + (positionScore * 0.2f) // 0.7 to 0.9
        }

        // Alias exact match
        if (aliases.any { it.lowercase() == queryLower }) return 0.85f

        // Alias contains query
        val aliasScore = aliases
            .map { it.lowercase() }
            .filter { it.contains(queryLower) }
            .maxOfOrNull { 0.6f } ?: 0f
        if (aliasScore > 0) return aliasScore

        // Tag exact match
        if (tags.any { it.lowercase() == queryLower }) return 0.8f

        // Tag starts with query
        val tagStartScore = tags
            .map { it.lowercase() }
            .filter { it.startsWith(queryLower) }
            .maxOfOrNull { 0.6f } ?: 0f
        if (tagStartScore > 0) return tagStartScore

        // Tag contains query
        val tagContainsScore = tags
            .map { it.lowercase() }
            .filter { it.contains(queryLower) }
            .maxOfOrNull { 0.4f } ?: 0f
        if (tagContainsScore > 0) return tagContainsScore

        // No match
        return 0f
    }

    /**
     * Format file size for display
     *
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "1.5 MB")
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
