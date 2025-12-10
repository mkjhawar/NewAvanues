/**
 * LearnedAppDTO.kt - Data Transfer Object for learned apps
 *
 * DTOs for transferring LearnApp data between SQLDelight queries and entities.
 * Part of LearnApp module migration from Room to SQLDelight.
 *
 * Author: Agent 1 (LearnApp Migration Specialist)
 * Date: 2025-11-27
 */

package com.avanues.database.dto

/**
 * Data transfer object for learned apps.
 * Maps between SQLDelight query results and LearnedAppEntity.
 *
 * Updated for LearnApp UX improvements (Phase 2) - 2025-11-28
 */
data class LearnedAppDTO(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val firstLearnedAt: Long,
    val lastUpdatedAt: Long,
    val totalScreens: Long,  // SQLDelight returns INTEGER as Long
    val totalElements: Long,  // SQLDelight returns INTEGER as Long
    val appHash: String,
    val explorationStatus: String,  // Legacy field - COMPLETE, PARTIAL, FAILED
    // NEW FIELDS for LearnApp UX improvements
    val learningMode: String = "AUTO_DETECT",  // AUTO_DETECT, MANUAL, JUST_IN_TIME
    val status: String = "NOT_LEARNED",  // NOT_LEARNED, LEARNING, LEARNED, FAILED, JIT_ACTIVE
    val progress: Long = 0,  // 0-100 percentage
    val commandCount: Long = 0,  // Number of generated commands
    val screensExplored: Long = 0,  // Screens learned so far
    val isAutoDetectEnabled: Boolean = true  // SQLDelight INTEGER mapped to Boolean
)

/**
 * Exploration Session DTO
 */
data class ExplorationSessionDTO(
    val sessionId: String,
    val packageName: String,
    val startedAt: Long,
    val completedAt: Long?,
    val durationMs: Long?,
    val screensExplored: Long,  // SQLDelight returns INTEGER as Long
    val elementsDiscovered: Long,  // SQLDelight returns INTEGER as Long
    val status: String
)

/**
 * Navigation Edge DTO
 */
data class NavigationEdgeDTO(
    val edgeId: String,
    val packageName: String,
    val sessionId: String,
    val fromScreenHash: String,
    val clickedElementUuid: String,
    val toScreenHash: String,
    val timestamp: Long
)

/**
 * Screen State DTO
 */
data class ScreenStateDTO(
    val screenHash: String,
    val packageName: String,
    val activityName: String,
    val fingerprint: String,
    val elementCount: Long,  // SQLDelight returns INTEGER as Long
    val discoveredAt: Long
)
