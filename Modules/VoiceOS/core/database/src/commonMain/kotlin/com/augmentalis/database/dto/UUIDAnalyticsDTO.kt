/**
 * UUIDAnalyticsDTO.kt - Data transfer object for UUID analytics
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.dto

import com.augmentalis.database.uuid.Uuid_analytics

/**
 * DTO for UUID analytics data.
 */
data class UUIDAnalyticsDTO(
    val uuid: String,
    val accessCount: Long,
    val firstAccessed: Long,
    val lastAccessed: Long,
    val executionTimeMs: Long,
    val successCount: Long,
    val failureCount: Long,
    val lifecycleState: String
)

/**
 * Convert SQLDelight entity to DTO.
 */
fun Uuid_analytics.toUUIDAnalyticsDTO(): UUIDAnalyticsDTO = UUIDAnalyticsDTO(
    uuid = uuid,
    accessCount = access_count,
    firstAccessed = first_accessed,
    lastAccessed = last_accessed,
    executionTimeMs = execution_time_ms,
    successCount = success_count,
    failureCount = failure_count,
    lifecycleState = lifecycle_state
)
