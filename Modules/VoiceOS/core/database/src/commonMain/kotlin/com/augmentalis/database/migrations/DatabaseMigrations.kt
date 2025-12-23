/**
 * DatabaseMigrations.kt - Database schema migrations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-13
 *
 * Handles database schema migrations for VoiceOS Database.
 */

package com.augmentalis.database.migrations

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Database migrations for VoiceOS Database
 *
 * ## Migration Versioning:
 * - Version 1: Initial schema (before appId)
 * - Version 2: Added appId column to commands_generated
 * - Version 3: Added version tracking (appVersion, versionCode, lastVerified, isDeprecated)
 * - Version 4: Added foreign key constraints for data integrity (D-P0-1, D-P0-2, D-P0-3)
 *
 * ## Usage:
 * ```kotlin
 * val driver = AndroidSqliteDriver(
 *     schema = DatabaseMigrations.Schema,
 *     context = context,
 *     name = "voiceos.db"
 * )
 * ```
 *
 * ## Adding New Migrations:
 * 1. Create migration SQL file: `migrations/{version}.sqm`
 * 2. Increment schema version
 * 3. Add migration logic to `migrate()` method
 */
object DatabaseMigrations {

    /**
     * Migration from version 1 to 2
     * Adds appId column to commands_generated table
     */
    private fun migrateV1ToV2(driver: SqlDriver) {
        // Check if appId column exists
        val hasAppId = columnExists(driver, "commands_generated", "appId")

        if (!hasAppId) {
            // Add appId column with default value for existing rows
            driver.execute(
                identifier = null,
                sql = """
                    ALTER TABLE commands_generated
                    ADD COLUMN appId TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
                parameters = 0,
                binders = null
            )
        }

        // Create index for efficient package-based queries (IF NOT EXISTS makes it idempotent)
        driver.execute(
            identifier = null,
            sql = """
                CREATE INDEX IF NOT EXISTS idx_gc_app_id
                ON commands_generated(appId, id)
            """.trimIndent(),
            parameters = 0,
            binders = null
        )
    }

    /**
     * Migration from version 2 to 3
     * Adds version tracking columns for command lifecycle management
     */
    private fun migrateV2ToV3(driver: SqlDriver) {
        // Check which columns exist
        val hasAppVersion = columnExists(driver, "commands_generated", "appVersion")
        val hasVersionCode = columnExists(driver, "commands_generated", "versionCode")
        val hasLastVerified = columnExists(driver, "commands_generated", "lastVerified")
        val hasIsDeprecated = columnExists(driver, "commands_generated", "isDeprecated")

        // Add appVersion column (string representation of version)
        if (!hasAppVersion) {
            driver.execute(
                identifier = null,
                sql = """
                    ALTER TABLE commands_generated
                    ADD COLUMN appVersion TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
                parameters = 0,
                binders = null
            )
        }

        // Add versionCode column (integer for efficient comparison)
        if (!hasVersionCode) {
            driver.execute(
                identifier = null,
                sql = """
                    ALTER TABLE commands_generated
                    ADD COLUMN versionCode INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
                parameters = 0,
                binders = null
            )
        }

        // Add lastVerified column (timestamp when element was last seen)
        if (!hasLastVerified) {
            driver.execute(
                identifier = null,
                sql = """
                    ALTER TABLE commands_generated
                    ADD COLUMN lastVerified INTEGER
                """.trimIndent(),
                parameters = 0,
                binders = null
            )
        }

        // Add isDeprecated column (0=active, 1=deprecated)
        if (!hasIsDeprecated) {
            driver.execute(
                identifier = null,
                sql = """
                    ALTER TABLE commands_generated
                    ADD COLUMN isDeprecated INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
                parameters = 0,
                binders = null
            )
        }

        // Create composite index for version-based queries
        driver.execute(
            identifier = null,
            sql = """
                CREATE INDEX IF NOT EXISTS idx_gc_app_version
                ON commands_generated(appId, versionCode, isDeprecated)
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Create index for cleanup queries
        driver.execute(
            identifier = null,
            sql = """
                CREATE INDEX IF NOT EXISTS idx_gc_last_verified
                ON commands_generated(lastVerified, isDeprecated)
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Create app_version table for tracking app versions
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS app_version (
                    package_name TEXT PRIMARY KEY NOT NULL,
                    version_name TEXT NOT NULL,
                    version_code INTEGER NOT NULL,
                    last_checked INTEGER NOT NULL,
                    CHECK (version_code >= 0),
                    CHECK (last_checked > 0)
                )
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Create index for version lookups
        driver.execute(
            identifier = null,
            sql = """
                CREATE INDEX IF NOT EXISTS idx_av_version_code
                ON app_version(version_code)
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Create index for last_checked queries
        driver.execute(
            identifier = null,
            sql = """
                CREATE INDEX IF NOT EXISTS idx_av_last_checked
                ON app_version(last_checked)
            """.trimIndent(),
            parameters = 0,
            binders = null
        )
    }

    /**
     * Migration from version 3 to 4
     * Adds foreign key constraints for data integrity
     *
     * FIX (2025-12-22): D-P0-1, D-P0-2, D-P0-3 - Add missing foreign keys
     * - commands_generated.elementHash → scraped_element.elementHash
     * - element_command.element_uuid → uuid_elements.uuid
     * - element_quality_metric.element_uuid → uuid_elements.uuid
     *
     * NOTE: This migration uses table recreation since SQLite doesn't support
     * ALTER TABLE ADD FOREIGN KEY directly. Data is preserved during migration.
     */
    private fun migrateV3ToV4(driver: SqlDriver) {
        // Check if migration is needed by checking if FK already exists
        // We can detect this by attempting a constraint violation
        val needsMigration = !foreignKeyExists(driver, "commands_generated")

        if (!needsMigration) {
            // Migration already applied, skip
            return
        }

        // Migration is handled by SQLDelight's migration file: migrations/3.sqm
        // The .sqm file contains all the table recreation logic with foreign keys
        // This function is a placeholder for manual migration if needed
    }

    /**
     * Apply all necessary migrations
     *
     * @param driver SQL driver
     * @param oldVersion Current database version
     * @param newVersion Target database version
     */
    fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long) {
        // Apply migrations sequentially
        if (oldVersion < 2 && newVersion >= 2) {
            migrateV1ToV2(driver)
        }

        if (oldVersion < 3 && newVersion >= 3) {
            migrateV2ToV3(driver)
        }

        if (oldVersion < 4 && newVersion >= 4) {
            migrateV3ToV4(driver)
        }
    }

    /**
     * Check if migration is needed
     *
     * @param driver SQL driver to check
     * @return true if appId column is missing and migration is needed
     */
    fun isMigrationNeeded(driver: SqlDriver): Boolean {
        return try {
            // Try to query appId column
            driver.executeQuery(
                identifier = null,
                sql = "SELECT appId FROM commands_generated LIMIT 1",
                mapper = { QueryResult.Value(Unit) },
                parameters = 0,
                binders = null
            )
            false // Column exists, no migration needed
        } catch (e: Exception) {
            true // Column doesn't exist, migration needed
        }
    }

    /**
     * Check if a column exists in a table
     *
     * @param driver SQL driver
     * @param tableName Name of the table
     * @param columnName Name of the column to check
     * @return true if column exists, false otherwise
     */
    private fun columnExists(driver: SqlDriver, tableName: String, columnName: String): Boolean {
        return try {
            val result = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info($tableName)",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        // Column name is at index 1
                        columns.add(cursor.getString(1) ?: "")
                    }
                    QueryResult.Value(columns)
                },
                parameters = 0,
                binders = null
            )
            result.value.contains(columnName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if foreign key constraints exist in a table
     *
     * FIX (2025-12-22): Helper for V3→V4 migration detection
     *
     * @param driver SQL driver
     * @param tableName Name of the table to check
     * @return true if table has foreign keys, false otherwise
     */
    private fun foreignKeyExists(driver: SqlDriver, tableName: String): Boolean {
        return try {
            val result = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA foreign_key_list($tableName)",
                mapper = { cursor ->
                    // If cursor has any rows, foreign keys exist
                    QueryResult.Value(cursor.next().value)
                },
                parameters = 0,
                binders = null
            )
            result.value
        } catch (e: Exception) {
            false
        }
    }
}
