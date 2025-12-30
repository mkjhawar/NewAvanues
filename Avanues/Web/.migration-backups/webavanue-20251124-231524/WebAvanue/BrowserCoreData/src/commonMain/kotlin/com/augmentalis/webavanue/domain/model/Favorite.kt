package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a favorite/bookmark in the WebAvanue browser.
 * Cross-platform model with support for folders and tags.
 */
@Serializable
data class Favorite(
    val id: String,
    val url: String,
    val title: String,
    val favicon: String? = null,
    val folderId: String? = null, // For organizing in folders
    val tags: List<String> = emptyList(), // For categorization
    val description: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val visitCount: Int = 0,
    val position: Int = 0 // For custom ordering
) {
    companion object {
        /**
         * Creates a new favorite
         */
        fun create(
            url: String,
            title: String,
            favicon: String? = null,
            description: String? = null,
            folderId: String? = null
        ): Favorite {
            val now = kotlinx.datetime.Clock.System.now()
            return Favorite(
                id = generateFavoriteId(),
                url = url,
                title = title,
                favicon = favicon,
                description = description,
                folderId = folderId,
                createdAt = now,
                lastModifiedAt = now
            )
        }

        private fun generateFavoriteId(): String {
            return "fav_${System.currentTimeMillis()}_${(0..9999).random()}"
        }

        /**
         * Root folder ID for top-level favorites
         */
        const val ROOT_FOLDER_ID = "root"

        /**
         * Maximum title length
         */
        const val MAX_TITLE_LENGTH = 255
    }
}

/**
 * Represents a folder for organizing favorites
 */
@Serializable
data class FavoriteFolder(
    val id: String,
    val name: String,
    val parentId: String? = null, // For nested folders
    val icon: String? = null,
    val color: String? = null, // Hex color for visual organization
    val createdAt: Instant,
    val position: Int = 0
) {
    companion object {
        fun create(name: String, parentId: String? = null): FavoriteFolder {
            return FavoriteFolder(
                id = "folder_${System.currentTimeMillis()}",
                name = name,
                parentId = parentId,
                createdAt = kotlinx.datetime.Clock.System.now()
            )
        }
    }
}