/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.data.db.IntentCategoryQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of IntentCategoryRepository using SQLDelight
 *
 * Provides database-driven category lookups for Phase 2 intent routing.
 * Uses coroutines to avoid blocking on IO operations.
 *
 * @author Manoj Jhawar
 * @since 2025-12-06
 */
class IntentCategoryRepositoryImpl(
    private val queries: IntentCategoryQueries
) : IntentCategoryRepository {

    override suspend fun getCategoryForIntent(intent: String): String? = withContext(Dispatchers.IO) {
        queries.selectByIntent(intent).executeAsOneOrNull()?.category
    }

    override suspend fun getIntentsForCategory(category: String): List<String> = withContext(Dispatchers.IO) {
        queries.selectByCategory(category).executeAsList().map { it.intent_name }
    }

    override suspend fun saveIntentCategory(
        intent: String,
        category: String,
        requiresAccessibility: Boolean,
        priority: Int
    ) = withContext(Dispatchers.IO) {
        queries.insert(
            intent_name = intent,
            category = category,
            requires_accessibility = requiresAccessibility,
            priority = priority.toLong(),
            last_updated = System.currentTimeMillis()
        )
    }

    override suspend fun deleteIntentCategory(intent: String) = withContext(Dispatchers.IO) {
        queries.deleteByIntent(intent)
    }

    override suspend fun getAllCategories(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        queries.selectAll()
            .executeAsList()
            .groupBy({ it.category }, { it.intent_name })
    }

    override suspend fun getCategoryStats(): Map<String, Long> = withContext(Dispatchers.IO) {
        queries.countByCategory()
            .executeAsList()
            .associate { it.category to it.entry_count }
    }

    override suspend fun intentExists(intent: String): Boolean = withContext(Dispatchers.IO) {
        queries.existsIntent(intent).executeAsOne()
    }

    override suspend fun getTotalCount(): Long = withContext(Dispatchers.IO) {
        queries.count().executeAsOne()
    }

    override suspend fun deleteAll() = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }
}
