/**
 * DatabaseFactory.android.kt - Android SQLite driver
 *
 * Actual implementation for Android platform.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of DatabaseDriverFactory.
 *
 * Configures SQLite with:
 * - busy_timeout: 30 seconds to handle concurrent write contention
 * - WAL mode: Enables Write-Ahead Logging for better concurrency
 *
 * FIX (2025-11-30): Added busy_timeout and WAL mode to prevent
 * SQLiteDatabaseLockedException when AccessibilityScrapingIntegration
 * and LearnApp write concurrently.
 */
actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = "voiceos.db",
            callback = object : AndroidSqliteDriver.Callback(VoiceOSDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Set busy timeout to 30 seconds (30000ms)
                    // This allows concurrent operations to wait instead of failing immediately
                    // Note: Use query() not execSQL() - execSQL() throws in onOpen callback
                    db.query("PRAGMA busy_timeout = 30000").close()

                    // Enable WAL mode for better concurrent read/write performance
                    // Note: WAL mode should already be enabled by default on Android,
                    // but we set it explicitly to ensure consistency
                    db.query("PRAGMA journal_mode = WAL").close()
                }
            }
        )
    }
}
