package com.augmentalis.browseravanue.data.mapper

import com.augmentalis.browseravanue.data.local.entity.FavoriteEntity
import com.augmentalis.browseravanue.domain.model.Favorite

/**
 * Mapper for converting between Favorite domain model and FavoriteEntity
 *
 * Architecture:
 * - Pure mapping logic, no business rules
 * - Bidirectional conversion (toEntity, toDomain)
 * - Handles data type conversions (List<String> ↔ comma-separated String)
 * - Extension functions for clean usage
 *
 * Special Conversions:
 * - tags: List<String> (domain) ↔ String (entity, comma-separated)
 *
 * Usage:
 * ```
 * val domain: Favorite = entity.toDomain()
 * val entity: FavoriteEntity = domain.toEntity()
 * ```
 */
object FavoriteMapper {

    /**
     * Convert FavoriteEntity to Favorite domain model
     */
    fun FavoriteEntity.toDomain(): Favorite {
        return Favorite(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            folder = folder,
            createdAt = createdAt,
            visitCount = visitCount,
            lastVisited = lastVisited,
            tags = parseTags(tags),
            notes = notes
        )
    }

    /**
     * Convert Favorite domain model to FavoriteEntity
     */
    fun Favorite.toEntity(): FavoriteEntity {
        return FavoriteEntity(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            folder = folder,
            createdAt = createdAt,
            visitCount = visitCount,
            lastVisited = lastVisited,
            tags = serializeTags(tags),
            notes = notes
        )
    }

    /**
     * Convert list of FavoriteEntity to list of Favorite
     */
    fun List<FavoriteEntity>.toDomain(): List<Favorite> {
        return map { it.toDomain() }
    }

    /**
     * Convert list of Favorite to list of FavoriteEntity
     */
    fun List<Favorite>.toEntity(): List<FavoriteEntity> {
        return map { it.toEntity() }
    }

    /**
     * Parse comma-separated tags string to List<String>
     *
     * @param tagsString Comma-separated tags
     * @return List of tags (empty list if null or blank)
     */
    private fun parseTags(tagsString: String): List<String> {
        if (tagsString.isBlank()) return emptyList()
        return tagsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Serialize List<String> to comma-separated tags string
     *
     * @param tags List of tags
     * @return Comma-separated tags string (empty string if empty list)
     */
    private fun serializeTags(tags: List<String>): String {
        return tags
            .filter { it.isNotBlank() }
            .joinToString(",")
    }
}
