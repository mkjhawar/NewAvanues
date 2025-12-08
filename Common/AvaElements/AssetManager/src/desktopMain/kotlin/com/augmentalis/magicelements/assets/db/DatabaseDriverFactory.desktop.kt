package com.augmentalis.avaelements.assets.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Desktop (JVM) database driver factory
 *
 * Uses JdbcSqliteDriver for SQLite on Desktop (macOS, Windows, Linux)
 */
actual class DatabaseDriverFactory(private val databasePath: String? = null) {
    actual fun createDriver(): SqlDriver {
        val dbPath = databasePath ?: getDefaultDatabasePath()

        // Ensure parent directory exists
        val dbFile = File(dbPath)
        dbFile.parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")

        // Create schema if database doesn't exist
        if (!dbFile.exists()) {
            AssetDatabase.Schema.create(driver)
        }

        return driver
    }

    /**
     * Get default database path for desktop platforms
     */
    private fun getDefaultDatabasePath(): String {
        val userHome = System.getProperty("user.home")
        val appDataDir = when {
            System.getProperty("os.name").contains("Mac") -> {
                // macOS: ~/Library/Application Support/AvaElements
                "$userHome/Library/Application Support/AvaElements"
            }
            System.getProperty("os.name").contains("Windows") -> {
                // Windows: %APPDATA%\AvaElements
                "${System.getenv("APPDATA")}/AvaElements"
            }
            else -> {
                // Linux: ~/.local/share/AvaElements
                "$userHome/.local/share/AvaElements"
            }
        }

        return "$appDataDir/avaelements_assets.db"
    }
}
