package com.augmentalis.avaelements.assets.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Database driver factory (expect/actual pattern)
 *
 * Provides platform-specific SQLite drivers for:
 * - Android: AndroidSqliteDriver (requires Context)
 * - iOS: NativeSqliteDriver
 * - Desktop: JdbcSqliteDriver (optional database path)
 * - Web: WebWorkerDriver (browser-based sql.js)
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
