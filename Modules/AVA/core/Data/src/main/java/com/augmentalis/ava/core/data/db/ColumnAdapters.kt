/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.data.db

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLDelight ColumnAdapters for complex data types
 *
 * Migrated from Room TypeConverters to SQLDelight ColumnAdapters.
 * These adapters handle serialization/deserialization of complex types to database primitives.
 *
 * Note: ByteArray (BLOB) is natively supported by SQLDelight and doesn't need an adapter.
 */

/**
 * ColumnAdapter for List<String> fields
 *
 * Used by SemanticIntentOntology for:
 * - synonyms: List of synonym phrases
 * - action_sequence: Ordered list of actions
 * - required_capabilities: List of required app capabilities
 *
 * Serializes to JSON array string: ["item1", "item2"] → '["item1","item2"]'
 */
object StringListAdapter : ColumnAdapter<List<String>, String> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Encode List<String> to database string (JSON array)
     */
    override fun encode(value: List<String>): String {
        return json.encodeToString(value)
    }

    /**
     * Decode database string (JSON array) to List<String>
     */
    override fun decode(databaseValue: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(databaseValue)
        } catch (e: Exception) {
            emptyList()  // Return empty list for invalid JSON
        }
    }
}

/**
 * ColumnAdapter for Boolean to INTEGER conversion
 *
 * SQLite doesn't have native BOOLEAN type, uses INTEGER (0 = false, 1 = true)
 * This adapter is automatically applied by SQLDelight when using `AS Boolean` type hint.
 *
 * Example in .sq file:
 * ```sql
 * is_active INTEGER AS Boolean NOT NULL DEFAULT 1
 * ```
 *
 * Note: This is typically handled automatically by SQLDelight, but included here for reference.
 */
object BooleanAdapter : ColumnAdapter<Boolean, Long> {

    override fun encode(value: Boolean): Long {
        return if (value) 1L else 0L
    }

    override fun decode(databaseValue: Long): Boolean {
        return databaseValue != 0L
    }
}
