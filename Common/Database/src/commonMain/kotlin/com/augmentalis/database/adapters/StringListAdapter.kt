/**
 * StringListAdapter.kt - ColumnAdapter for List<String> JSON serialization
 *
 * Converts between List<String> and JSON string for SQLDelight columns.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Adapter for storing List<String> as JSON in database.
 * Used for CustomCommand.phrases and similar fields.
 */
val stringListAdapter = object : ColumnAdapter<List<String>, String> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun decode(databaseValue: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(databaseValue)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun encode(value: List<String>): String {
        return json.encodeToString(value)
    }
}

/**
 * Adapter for Boolean stored as INTEGER (0/1).
 */
val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue != 0L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}
