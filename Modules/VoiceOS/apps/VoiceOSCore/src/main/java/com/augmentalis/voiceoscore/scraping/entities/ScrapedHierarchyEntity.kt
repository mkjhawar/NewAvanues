/**
 * ScrapedHierarchyEntity.kt - UI element hierarchy data for accessibility scraping database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-17
 */
package com.augmentalis.voiceoscore.scraping.entities

/**
 * Entity representing parent-child hierarchy relationships between scraped UI elements
 *
 * MIGRATION NOTE: This entity has been migrated to use SQLDelight.
 * The schema is defined in: core/database/src/commonMain/sqldelight/com/augmentalis/database/ScrapedHierarchy.sq
 *
 * This entity stores the hierarchical structure of UI elements as discovered through
 * the accessibility tree. It allows reconstruction of the UI tree and understanding
 * of element relationships.
 *
 * Note: Unlike the DTO which uses element hashes for foreign keys (matching the database schema),
 * this entity uses element IDs during construction before database insertion. The hashes are
 * derived from the elements themselves.
 *
 * @property id Auto-generated primary key
 * @property parentElementId ID of the parent element (converted to hash for database)
 * @property childElementId ID of the child element (converted to hash for database)
 * @property childOrder Order of child among siblings
 * @property depth Depth in hierarchy tree
 * @property createdAt Timestamp when relationship was recorded (milliseconds)
 */
data class ScrapedHierarchyEntity(
    val id: Long = 0,
    val parentElementId: Long = 0,
    val childElementId: Long = 0,
    val parentElementHash: String = "",
    val childElementHash: String = "",
    val childOrder: Int = 0,
    val depth: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
