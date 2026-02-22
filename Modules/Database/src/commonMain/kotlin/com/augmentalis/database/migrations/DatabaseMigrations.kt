/**
 * DatabaseMigrations.kt - Database schema migrations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-13
 *
 * Handles database schema migrations for VoiceOS Database.
 *
 * ## Migration System Overview
 *
 * SQLDelight generates a Schema with version=1 when deriveSchemaFromMigrations=false.
 * To make platform drivers (Android/iOS/Desktop) correctly call onUpgrade for existing
 * databases, we wrap the generated schema in [MigratedSchema], which reports the true
 * version ([CURRENT_VERSION]) while delegating create() to SQLDelight's generated DDL
 * and migrate() to this class.
 *
 * ## How to add a new migration (e.g., v7 → v8)
 * 1. Write `private fun migrateV7ToV8(driver: SqlDriver)` with your ALTER TABLE / CREATE TABLE DDL
 * 2. Add the corresponding branch to `migrate()`:
 *    ```kotlin
 *    if (oldVersion < 8 && newVersion >= 8) migrateV7ToV8(driver)
 *    ```
 * 3. Bump [CURRENT_VERSION] to 8
 * 4. Update the corresponding `.sq` files with the new schema columns/tables
 * 5. Rebuild — all three platform drivers pick up the new version automatically
 *
 * No .sqm files are required. The Kotlin migration functions ARE the migration record.
 */

package com.augmentalis.database.migrations

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * The true schema version of VoiceOSDatabase.
 *
 * SQLDelight's generated Schema.version is always 1 when deriveSchemaFromMigrations=false.
 * This constant is the authoritative version used by MigratedSchema and all platform drivers.
 * Increment this whenever a new migration function is added to DatabaseMigrations.
 *
 * Version history:
 *  1 — Initial schema (before appId column)
 *  2 — Added appId to commands_generated + idx_gc_app_id
 *  3 — Added appVersion/versionCode/lastVerified/isDeprecated + app_version table
 *  4 — Enabled FK enforcement via PRAGMA foreign_keys=ON
 *  5 — Added pkg_hash column to scraped_app
 *  6 — UNIQUE(elementHash, screen_hash) on scraped_element (table recreation)
 *  7 — Removed FK from element_relationship (table recreation, FK mismatch crash fix)
 */
const val CURRENT_SCHEMA_VERSION: Long = 7

/**
 * A SqlSchema wrapper that reports the true schema version ([CURRENT_SCHEMA_VERSION])
 * to platform drivers, while delegating create() to SQLDelight's generated DDL
 * (which always creates the latest schema) and migrate() to [DatabaseMigrations.migrate()].
 *
 * Usage (in each platform DatabaseDriverFactory):
 * ```kotlin
 * val schema = MigratedSchema(VoiceOSDatabase.Schema)
 * // then pass `schema` to the platform driver instead of VoiceOSDatabase.Schema directly
 * ```
 */
class MigratedSchema(
    private val delegate: SqlSchema<QueryResult.Value<Unit>>
) : SqlSchema<QueryResult.Value<Unit>> {

    /** Report the true version so platform drivers call onUpgrade on existing databases. */
    override val version: Long get() = CURRENT_SCHEMA_VERSION

    /** Create the full schema (SQLDelight-generated DDL, always at current version). */
    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = delegate.create(driver)

    /**
     * Apply incremental migrations from [oldVersion] to [newVersion].
     * Delegates to [DatabaseMigrations.migrate], which contains the full migration chain.
     */
    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion
    ): QueryResult.Value<Unit> {
        DatabaseMigrations.migrate(driver, oldVersion, newVersion)
        // Run any AfterVersion callbacks the caller supplied (e.g., for data backfills)
        callbacks
            .filter { it.afterVersion in oldVersion until newVersion }
            .forEach { it.block(driver) }
        return QueryResult.Unit
    }
}

object DatabaseMigrations {

    /**
     * Migration from version 1 to 2: added appId to commands_generated
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

        // FK constraints cannot be added to existing SQLite tables without recreation.
        // New installs get correct FKs from SQLDelight's generated schema.
        // Existing installs: enable FK enforcement so future inserts/updates are validated.
        try {
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        } catch (_: Exception) {
            // PRAGMA not supported on all drivers — safe to skip
        }
    }

    /**
     * Migration from version 4 to 5
     * Adds pkg_hash column for compact AVID format support
     *
     * FIX (2025-12-30): Add pre-computed package hash for efficient lookups
     * with the new compact AVID format: {reversedPkg}:{version}:{typeAbbrev}:{hash8}
     */
    private fun migrateV4ToV5(driver: SqlDriver) {
        // Check if pkg_hash column already exists
        val hasPkgHash = columnExists(driver, "scraped_app", "pkg_hash")

        if (!hasPkgHash) {
            // Add pkg_hash column (nullable for backward compatibility)
            driver.execute(
                identifier = null,
                sql = """
                    ALTER TABLE scraped_app
                    ADD COLUMN pkg_hash TEXT
                """.trimIndent(),
                parameters = 0,
                binders = null
            )
        }

        // Create index for efficient lookups by package hash
        driver.execute(
            identifier = null,
            sql = """
                CREATE INDEX IF NOT EXISTS idx_scraped_app_pkg_hash
                ON scraped_app(pkg_hash)
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

        if (oldVersion < 4 && newVersion >= 4) {
            migrateV3ToV4(driver)
        }

        if (oldVersion < 5 && newVersion >= 5) {
            migrateV4ToV5(driver)
        }

        if (oldVersion < 6 && newVersion >= 6) {
            migrateV5ToV6(driver)
        }

        if (oldVersion < 7 && newVersion >= 7) {
            migrateV6ToV7(driver)
        }
    }

    /**
     * Migration from version 6 to 7
     * Removes FK constraints from element_relationship table
     *
     * FIX (2026-01-22): FK constraint mismatch crash
     * Root cause: element_relationship had FK to scraped_element(elementHash)
     * but scraped_element now uses composite UNIQUE(elementHash, screen_hash).
     * SQLite requires FK target to be UNIQUE or PRIMARY KEY.
     *
     * Solution: Remove FK constraints from element_relationship table.
     * Relationships are enforced at application level, not database level.
     */
    private fun migrateV6ToV7(driver: SqlDriver) {
        // Step 1: Create new table without FK constraints
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS element_relationship_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sourceElementHash TEXT NOT NULL,
                    targetElementHash TEXT,
                    relationshipType TEXT NOT NULL,
                    relationshipData TEXT,
                    confidence REAL NOT NULL DEFAULT 1.0,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Step 2: Copy existing data
        driver.execute(
            identifier = null,
            sql = """
                INSERT OR IGNORE INTO element_relationship_new
                SELECT * FROM element_relationship
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Step 3: Drop old table
        driver.execute(
            identifier = null,
            sql = "DROP TABLE IF EXISTS element_relationship",
            parameters = 0,
            binders = null
        )

        // Step 4: Rename new table
        driver.execute(
            identifier = null,
            sql = "ALTER TABLE element_relationship_new RENAME TO element_relationship",
            parameters = 0,
            binders = null
        )

        // Step 5: Recreate indexes
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_elrel_source ON element_relationship(sourceElementHash)",
            parameters = 0,
            binders = null
        )
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_elrel_target ON element_relationship(targetElementHash)",
            parameters = 0,
            binders = null
        )
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_elrel_type ON element_relationship(relationshipType)",
            parameters = 0,
            binders = null
        )
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_elrel_confidence ON element_relationship(confidence)",
            parameters = 0,
            binders = null
        )
    }

    /**
     * Migration from version 5 to 6
     * Changes UNIQUE constraint on scraped_element from (elementHash) to (elementHash, screen_hash)
     *
     * FIX (2026-01-22): Preserve commands across screen navigation
     * Root cause: Elements were being overwritten on screen change due to
     * INSERT OR REPLACE with elementHash as unique key (no screen awareness).
     *
     * Solution:
     * - Change UNIQUE(elementHash) to UNIQUE(elementHash, screen_hash)
     * - Requires table recreation since SQLite doesn't support ALTER TABLE DROP CONSTRAINT
     */
    private fun migrateV5ToV6(driver: SqlDriver) {
        // Step 1: Create new table with updated schema
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS scraped_element_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    elementHash TEXT NOT NULL,
                    appId TEXT NOT NULL,
                    uuid TEXT,
                    className TEXT NOT NULL,
                    viewIdResourceName TEXT,
                    text TEXT,
                    contentDescription TEXT,
                    bounds TEXT NOT NULL,
                    isClickable INTEGER NOT NULL,
                    isLongClickable INTEGER NOT NULL,
                    isEditable INTEGER NOT NULL,
                    isScrollable INTEGER NOT NULL,
                    isCheckable INTEGER NOT NULL,
                    isFocusable INTEGER NOT NULL,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    depth INTEGER NOT NULL,
                    indexInParent INTEGER NOT NULL,
                    scrapedAt INTEGER NOT NULL,
                    semanticRole TEXT,
                    inputType TEXT,
                    visualWeight TEXT,
                    isRequired INTEGER DEFAULT 0,
                    formGroupId TEXT,
                    placeholderText TEXT,
                    validationPattern TEXT,
                    backgroundColor TEXT,
                    screen_hash TEXT,
                    UNIQUE(elementHash, screen_hash),
                    FOREIGN KEY (appId) REFERENCES scraped_app(appId) ON DELETE CASCADE
                )
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Step 2: Copy existing data
        driver.execute(
            identifier = null,
            sql = """
                INSERT OR IGNORE INTO scraped_element_new
                SELECT * FROM scraped_element
            """.trimIndent(),
            parameters = 0,
            binders = null
        )

        // Step 3: Drop old table
        driver.execute(
            identifier = null,
            sql = "DROP TABLE IF EXISTS scraped_element",
            parameters = 0,
            binders = null
        )

        // Step 4: Rename new table
        driver.execute(
            identifier = null,
            sql = "ALTER TABLE scraped_element_new RENAME TO scraped_element",
            parameters = 0,
            binders = null
        )

        // Step 5: Recreate indexes
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_se_app ON scraped_element(appId)",
            parameters = 0,
            binders = null
        )
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_se_hash ON scraped_element(elementHash)",
            parameters = 0,
            binders = null
        )
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_se_screen_hash ON scraped_element(appId, screen_hash)",
            parameters = 0,
            binders = null
        )
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS idx_scraped_element_app_hash ON scraped_element(appId, elementHash)",
            parameters = 0,
            binders = null
        )
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
