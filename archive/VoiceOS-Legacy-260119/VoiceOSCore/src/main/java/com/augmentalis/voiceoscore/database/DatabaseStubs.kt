package com.augmentalis.voiceoscore.database

/**
 * Minimal legacy stubs for backward compatibility
 *
 * **NOTE:** DAO interfaces have been removed. Use VoiceOSDatabaseManager directly:
 * ```kotlin
 * val adapter = VoiceOSCoreDatabaseAdapter.getInstance(context)
 * val apps = adapter.databaseManager.scrapedApps.getAll() // Direct SQLDelight access
 * ```
 *
 * This file only contains minimal stub classes required for legacy code that hasn't
 * been migrated yet. New code should NOT use these stubs.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Refactored: 2025-11-27 (Removed DAO abstraction layer)
 */

/**
 * Legacy database helper stub
 * Use VoiceOSDatabaseManager for database operations instead
 */
class VoiceOSDatabaseHelper {
    val writableDatabase: WritableDatabaseStub = WritableDatabaseStub()
    fun exportToJson(): String = "{}"
    fun close() {}
}

/**
 * Legacy writable database stub
 * Use VoiceOSDatabaseManager.transaction {} for database operations instead
 */
class WritableDatabaseStub {
    fun delete(table: String, whereClause: String?, whereArgs: Array<String>?): Int = 0
    fun execSQL(sql: String) {}
    fun query(sql: String): CursorStub = CursorStub()
}

/**
 * Legacy cursor stub
 * Use SQLDelight query objects which return typed results instead
 */
class CursorStub {
    fun moveToFirst(): Boolean = false
    fun getString(columnIndex: Int): String = ""
    fun close() {}
}
