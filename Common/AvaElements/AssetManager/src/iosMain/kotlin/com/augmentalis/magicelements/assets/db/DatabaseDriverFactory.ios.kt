package com.augmentalis.avaelements.assets.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS database driver factory
 *
 * Uses NativeSqliteDriver for SQLite on iOS
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AssetDatabase.Schema,
            name = "avaelements_assets.db"
        )
    }
}
