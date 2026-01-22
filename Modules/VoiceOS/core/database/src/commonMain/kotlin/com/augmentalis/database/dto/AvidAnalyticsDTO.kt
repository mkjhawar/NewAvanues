/**
 * AvidAnalyticsDTO.kt - Data Transfer Object for AVID analytics
 *
 * Maps to the avid_analytics SQLDelight table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

/**
 * DTO representing analytics for an AVID element.
 *
 * @param avid AVID of the element (primary key)
 * @param accessCount Total access count
 * @param firstAccessed First access timestamp
 * @param lastAccessed Most recent access timestamp
 * @param executionTimeMs Cumulative execution time in milliseconds
 * @param successCount Number of successful executions
 * @param failureCount Number of failed executions
 * @param lifecycleState Current lifecycle state (CREATED, ACTIVE, STALE, ARCHIVED)
 */
data class AvidAnalyticsDTO(
    val avid: String,
    val accessCount: Long = 0,
    val firstAccessed: Long,
    val lastAccessed: Long,
    val executionTimeMs: Long = 0,
    val successCount: Long = 0,
    val failureCount: Long = 0,
    val lifecycleState: String = "CREATED"
)
