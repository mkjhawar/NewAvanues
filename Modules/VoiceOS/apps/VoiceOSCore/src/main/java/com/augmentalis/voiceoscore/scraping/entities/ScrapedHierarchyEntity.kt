/**
 * ScrapedHierarchyEntity.kt - Parent-child relationships for accessibility tree
 *
 * Migrated from Room to SQLDelight (Phase 2)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Scraping Migration Specialist (Agent 2)
 * Created: 2025-11-27
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing parent-child relationships in accessibility tree
 *
 * This entity captures the hierarchical structure of the UI, allowing the system
 * to understand element relationships and context. Essential for commands like
 * "click the button in the dialog" or "scroll the list".
 *
 * @property id Auto-generated primary key
 * @property parentElementHash Hash of parent ScrapedElementEntity
 * @property childElementHash Hash of child ScrapedElementEntity
 * @property depth Depth in the hierarchy
 * @property createdAt Timestamp when relationship was recorded
 */
data class ScrapedHierarchyEntity(
    val id: Long? = null,
    val parentElementHash: String,
    val childElementHash: String,
    val depth: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
