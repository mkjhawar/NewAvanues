/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.data.repository

/**
 * Repository for intent-to-category mappings
 *
 * Phase 2: Provides database-driven category lookup for IntentRouter.
 * Replaces hardcoded CategoryCapabilityRegistry with dynamic database queries.
 *
 * Benefits:
 * - Runtime category configuration without code changes
 * - Fast lookups with database indices
 * - Support for priority-based intent classification
 * - Accessibility requirement tracking
 *
 * Fallback:
 * - IntentRouter uses registry as fallback if database empty or intent not found
 * - Ensures graceful degradation if database initialization fails
 *
 * @author Manoj Jhawar
 * @since 2025-12-06
 */
interface IntentCategoryRepository {
    /**
     * Get category for an intent
     *
     * @param intent Intent identifier (e.g., "wifi_on", "cursor_move_up")
     * @return Category string, or null if not found
     */
    suspend fun getCategoryForIntent(intent: String): String?

    /**
     * Get all intents for a category
     *
     * @param category Category identifier (e.g., "connectivity", "cursor")
     * @return List of intent names in priority order
     */
    suspend fun getIntentsForCategory(category: String): List<String>

    /**
     * Save intent category mapping
     *
     * @param intent Intent identifier
     * @param category Category identifier
     * @param requiresAccessibility Whether this intent requires accessibility service
     * @param priority Priority for ordering (higher = more important)
     */
    suspend fun saveIntentCategory(
        intent: String,
        category: String,
        requiresAccessibility: Boolean = false,
        priority: Int = 50
    )

    /**
     * Delete intent category mapping
     *
     * @param intent Intent identifier
     */
    suspend fun deleteIntentCategory(intent: String)

    /**
     * Get all categories with their intents
     *
     * @return Map of category -> List of intents
     */
    suspend fun getAllCategories(): Map<String, List<String>>

    /**
     * Get category statistics
     *
     * @return Map of category -> count
     */
    suspend fun getCategoryStats(): Map<String, Long>

    /**
     * Check if intent exists in database
     *
     * @param intent Intent identifier
     * @return True if intent exists, false otherwise
     */
    suspend fun intentExists(intent: String): Boolean

    /**
     * Get total count of intents
     *
     * @return Total number of intent mappings
     */
    suspend fun getTotalCount(): Long

    /**
     * Delete all intent categories (for testing/reset)
     */
    suspend fun deleteAll()
}
