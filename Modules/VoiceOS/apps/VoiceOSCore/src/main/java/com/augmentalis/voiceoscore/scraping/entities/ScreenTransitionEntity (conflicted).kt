/**
 * ScreenTransitionEntity.kt - Screen navigation transitions for flow analysis
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 * Migrated to SQLDelight: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing screen-to-screen transitions
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/ScreenTransition.sq
 *
 * Tracks navigation patterns and user flows between screens.
 * Enables understanding of:
 * - Common navigation paths
 * - Screen flow sequences
 * - Dead-end screens vs gateway screens
 * - User journey analysis
 *
 * @property id Auto-generated primary key
 * @property fromScreenHash Hash of source screen
 * @property toScreenHash Hash of destination screen
 * @property triggerElementHash Hash of element that triggered transition
 * @property triggerAction Action that triggered transition
 * @property transitionCount Number of times this transition occurred
 * @property avgDurationMs Average duration of transition in milliseconds
 * @property lastTransitionAt Timestamp of most recent occurrence
 */
data class ScreenTransitionEntity(
    val id: Long = 0,
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerElementHash: String?,
    val triggerAction: String,
    val transitionCount: Long = 1,
    val avgDurationMs: Long = 0,
    val lastTransitionAt: Long = System.currentTimeMillis()
)
