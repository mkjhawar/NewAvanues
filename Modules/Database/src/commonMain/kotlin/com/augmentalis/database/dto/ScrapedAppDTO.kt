/**
 * ScrapedAppDTO.kt - DTO for scraped app metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 *
 * Data Transfer Object for app scraping metadata.
 * Maps to ScrapedApp.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.Scraped_app

/**
 * DTO for scraped app metadata
 *
 * Simplified schema - stores essential app metadata for UI scraping.
 * Full Room entity has additional fields (appName, etc.) that are stored
 * in the adapter layer when needed.
 *
 * @property pkgHash Pre-computed package hash for compact AVID format lookups.
 *                   Format: 6-char hex hash of reversed package name.
 *                   Example: "a3f2e1" for "com.instagram.android"
 */
data class ScrapedAppDTO(
    val appId: String,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val appHash: String,
    val isFullyLearned: Long = 0,  // SQLite Boolean (0/1)
    val learnCompletedAt: Long? = null,
    val scrapingMode: String = "DYNAMIC",
    val scrapeCount: Long = 0,
    val elementCount: Long = 0,
    val commandCount: Long = 0,
    val firstScrapedAt: Long,
    val lastScrapedAt: Long,
    val pkgHash: String? = null  // Pre-computed package hash for compact AVID format
)

/**
 * Extension to convert SQLDelight generated type to DTO
 */
fun Scraped_app.toScrapedAppDTO(): ScrapedAppDTO {
    return ScrapedAppDTO(
        appId = appId,
        packageName = packageName,
        versionCode = versionCode,
        versionName = versionName,
        appHash = appHash,
        isFullyLearned = isFullyLearned,
        learnCompletedAt = learnCompletedAt,
        scrapingMode = scrapingMode,
        scrapeCount = scrapeCount,
        elementCount = elementCount,
        commandCount = commandCount,
        firstScrapedAt = firstScrapedAt,
        lastScrapedAt = lastScrapedAt,
        pkgHash = pkg_hash
    )
}
