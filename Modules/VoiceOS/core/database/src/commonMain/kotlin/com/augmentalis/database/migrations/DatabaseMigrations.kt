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

        // Create index for efficient package-based queries
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

        // Future migrations go here:
        // if (oldVersion < 3 && newVersion >= 3) {
        //     migrateV2ToV3(driver)
        // }
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
}
