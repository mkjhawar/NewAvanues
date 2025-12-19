package com.augmentalis.avaelements.assets.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android database driver factory
 *
 * Uses AndroidSqliteDriver for SQLite on Android
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AssetDatabase.Schema,
            context = context,
            name = "avaelements_assets.db"
        )
    }
}
