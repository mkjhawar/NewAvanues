/**
 * DatabaseFactory.android.kt - Android SQLite driver
 *
 * Actual implementation for Android platform.
 *
 * ## Migration Strategy
 * We wrap VoiceOSDatabase.Schema in MigratedSchema so Android's SQLiteOpenHelper
 * sees the true schema version (CURRENT_SCHEMA_VERSION = 7). On first open of an
 * existing database the helper calls onUpgrade(oldVersion, newVersion), which invokes
 * DatabaseMigrations.migrate() to apply the correct chain of incremental migrations.
 * New installs get the full schema created at version 7 directly.
 *
 * To add a future migration (e.g., v7 → v8):
 *   1. Add migrateV7ToV8() in DatabaseMigrations.kt
 *   2. Wire it in DatabaseMigrations.migrate()
 *   3. Bump CURRENT_SCHEMA_VERSION to 8
 *   No changes needed in this file.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.database.migrations.MigratedSchema

/**
 * Android implementation of DatabaseDriverFactory.
 *
 * Configures SQLite with:
 * - MigratedSchema: Reports true version (7) so onUpgrade fires on existing databases
 * - busy_timeout: 30 seconds to handle concurrent write contention
 * - WAL mode: Enables Write-Ahead Logging for better concurrency
 * - foreign_keys: ON for referential integrity
 *
 * FIX (2025-11-30): Added busy_timeout and WAL mode to prevent
 * SQLiteDatabaseLockedException when AccessibilityScrapingIntegration
 * and LearnApp write concurrently.
 * FIX (2026-02-22): Wired MigratedSchema so DatabaseMigrations.migrate()
 * is called on upgrade instead of silently dropping all user data.
 */
actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        val schema = MigratedSchema(VoiceOSDatabase.Schema)
        return AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = "voiceos.db",
            callback = object : AndroidSqliteDriver.Callback(schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)

                    // CRITICAL: Enable foreign key constraints
                    // Without this, all FK constraints are ignored, leading to data corruption
                    // FIX (2025-12-19): Added to enforce referential integrity
                    db.query("PRAGMA foreign_keys = ON").close()

                    // Set busy timeout to 30 seconds (30000ms)
                    // This allows concurrent operations to wait instead of failing immediately
                    // Note: Use query() not execSQL() - execSQL() throws in onOpen callback
                    db.query("PRAGMA busy_timeout = 30000").close()

                    // Enable WAL mode for better concurrent read/write performance
                    db.query("PRAGMA journal_mode = WAL").close()
                }
            }
        )
    }
}
