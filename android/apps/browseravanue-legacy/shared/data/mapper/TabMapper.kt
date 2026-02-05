package com.augmentalis.browseravanue.data.mapper

import com.augmentalis.browseravanue.data.local.entity.TabEntity
import com.augmentalis.browseravanue.domain.model.Tab

/**
 * Mapper for converting between Tab domain model and TabEntity
 *
 * Architecture:
 * - Pure mapping logic, no business rules
 * - Bidirectional conversion (toEntity, toDomain)
 * - Extension functions for clean usage
 *
 * Usage:
 * ```
 * val domain: Tab = entity.toDomain()
 * val entity: TabEntity = domain.toEntity()
 * ```
 */
object TabMapper {

    /**
     * Convert TabEntity to Tab domain model
     */
    fun TabEntity.toDomain(): Tab {
        return Tab(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            isDesktopMode = isDesktopMode,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            createdAt = createdAt,
            lastAccessed = lastAccessed,
            isLoading = isLoading
        )
    }

    /**
     * Convert Tab domain model to TabEntity
     */
    fun Tab.toEntity(): TabEntity {
        return TabEntity(
            id = id,
            url = url,
            title = title,
            favicon = favicon,
            isDesktopMode = isDesktopMode,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            createdAt = createdAt,
            lastAccessed = lastAccessed,
            isLoading = isLoading
        )
    }

    /**
     * Convert list of TabEntity to list of Tab
     */
    fun List<TabEntity>.toDomain(): List<Tab> {
        return map { it.toDomain() }
    }

    /**
     * Convert list of Tab to list of TabEntity
     */
    fun List<Tab>.toEntity(): List<TabEntity> {
        return map { it.toEntity() }
    }
}
