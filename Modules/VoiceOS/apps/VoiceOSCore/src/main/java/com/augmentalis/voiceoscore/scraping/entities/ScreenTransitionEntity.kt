/**
 * ScreenTransitionEntity.kt - Screen navigation transitions for flow analysis
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 3B)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing screen-to-screen transitions
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
 * @property triggerElementHash Hash of element that triggered transition (nullable)
 * @property triggerAction Action that triggered transition
 * @property transitionCount Number of times this transition occurred
 * @property avgDurationMs Average duration of transition in milliseconds
 * @property lastTransitionAt Timestamp of most recent occurrence
 */
data class ScreenTransitionEntity(
    val id: Long = 0,
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerElementHash: String? = null,
    val triggerAction: String = "unknown",
    val transitionCount: Int = 1,
    val avgDurationMs: Long = 0,
    val lastTransitionAt: Long = System.currentTimeMillis()
)
