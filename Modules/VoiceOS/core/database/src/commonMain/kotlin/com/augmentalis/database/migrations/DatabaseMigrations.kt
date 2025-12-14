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
     * Migration from version 2 to 3
     * Adds version tracking columns for command lifecycle management
     */
    private fun migrateV2ToV3(driver: SqlDriver) {
        // Add appVersion column (string representation of version)
        driver.execute(
            identifier = null,
            sql = """
                ALTER TABLE commands_generated
                ADD COLUMN appVersion TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Add versionCode column (integer for efficient comparison)
        driver.execute(
            identifier = null,
            sql = """
                ALTER TABLE commands_generated
                ADD COLUMN versionCode INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Add lastVerified column (timestamp when element was last seen)
        driver.execute(
            identifier = null,
            sql = """
                ALTER TABLE commands_generated
                ADD COLUMN lastVerified INTEGER
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Add isDeprecated column (0=active, 1=deprecated)
        driver.execute(
            identifier = null,
            sql = """
                ALTER TABLE commands_generated
                ADD COLUMN isDeprecated INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

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
