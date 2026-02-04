/**
 * DatabaseIntegrityChecker.kt - Database corruption detection and integrity validation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Phase: 3 (Medium Priority)
 * Issue: Database corruption detection
 */
package com.avanues.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thread-safe database integrity checker for corruption detection
 *
 * Features:
 * - SQLite integrity check (PRAGMA integrity_check)
 * - Foreign key constraint validation (PRAGMA foreign_key_check)
 * - Quick check for common corruption patterns
 * - Detailed corruption reporting
 * - Database statistics and health metrics
 *
 * Usage:
 * ```kotlin
 * val integrityChecker = DatabaseIntegrityChecker(context)
 *
 * // Quick integrity check
 * val result = integrityChecker.checkIntegrity("VoiceOSDatabase.db")
 * if (!result.isHealthy) {
 *     Log.e(TAG, "Database corrupted: ${result.errors}")
 * }
 *
 * // Full integrity check with foreign keys
 * val fullResult = integrityChecker.checkIntegrityFull("VoiceOSDatabase.db")
 * if (fullResult.isHealthy) {
 *     Log.i(TAG, "Database is healthy")
 * }
 *
 * // Get database statistics
 * val stats = integrityChecker.getDatabaseStats("VoiceOSDatabase.db")
 * Log.i(TAG, "DB size: ${stats.sizeBytes} bytes, ${stats.tableCount} tables")
 * ```
 *
 * Thread Safety: All operations are thread-safe using Kotlin coroutines
 */
class DatabaseIntegrityChecker(
    private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseIntegrityChecker"
    }

    private val databaseDir: File by lazy {
        File(context.applicationInfo.dataDir, "databases")
    }

    /**
     * Perform quick integrity check on database
     *
     * Uses SQLite's PRAGMA integrity_check to detect corruption.
     * This is faster than full check but may miss some issues.
     *
     * @param databaseName Name of database file (e.g., "VoiceOSDatabase.db")
     * @return IntegrityResult with health status and errors
     */
    suspend fun checkIntegrity(databaseName: String): IntegrityResult = withContext(Dispatchers.IO) {
        try {
            val dbFile = File(databaseDir, databaseName)
            if (!dbFile.exists()) {
                return@withContext IntegrityResult(
                    isHealthy = false,
                    errors = listOf("Database file not found: $databaseName"),
                    warnings = emptyList(),
                    checkType = CheckType.QUICK
                )
            }

            Log.d(TAG, "Running quick integrity check on: $databaseName")

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Open database in read-only mode
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                // Run PRAGMA integrity_check
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val result = cursor.getString(0)
                        if (result != "ok") {
                            errors.add("Integrity check failed: $result")
                        }
                    }
                }

                // Check for common corruption indicators
                checkCommonCorruptionPatterns(db, errors, warnings)
            }

            val isHealthy = errors.isEmpty()
            Log.i(TAG, "Quick integrity check complete: ${if (isHealthy) "HEALTHY" else "CORRUPTED"}")

            IntegrityResult(
                isHealthy = isHealthy,
                errors = errors,
                warnings = warnings,
                checkType = CheckType.QUICK
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error checking database integrity", e)
            IntegrityResult(
                isHealthy = false,
                errors = listOf("Integrity check failed: ${e.message}"),
                warnings = emptyList(),
                checkType = CheckType.QUICK
            )
        }
    }

    /**
     * Perform full integrity check including foreign key validation
     *
     * More thorough than quick check. Validates:
     * - Database integrity (PRAGMA integrity_check)
     * - Foreign key constraints (PRAGMA foreign_key_check)
     * - Table schema consistency
     * - Index integrity
     *
     * @param databaseName Name of database file
     * @return IntegrityResult with comprehensive validation results
     */
    suspend fun checkIntegrityFull(databaseName: String): IntegrityResult = withContext(Dispatchers.IO) {
        try {
            val dbFile = File(databaseDir, databaseName)
            if (!dbFile.exists()) {
                return@withContext IntegrityResult(
                    isHealthy = false,
                    errors = listOf("Database file not found: $databaseName"),
                    warnings = emptyList(),
                    checkType = CheckType.FULL
                )
            }

            Log.d(TAG, "Running FULL integrity check on: $databaseName")

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                // 1. PRAGMA integrity_check
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val result = cursor.getString(0)
                        if (result != "ok") {
                            errors.add("Integrity: $result")
                        }
                    }
                }

                // 2. PRAGMA foreign_key_check
                db.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                    if (cursor.count > 0) {
                        while (cursor.moveToNext()) {
                            val table = cursor.getString(0)
                            val rowid = cursor.getLong(1)
                            val parent = cursor.getString(2)
                            val fkid = cursor.getInt(3)
                            errors.add("Foreign key violation: table=$table, rowid=$rowid, parent=$parent, fkid=$fkid")
                        }
                    }
                }

                // 3. Check common corruption patterns
                checkCommonCorruptionPatterns(db, errors, warnings)

                // 4. Validate schema consistency
                validateSchemaConsistency(db, errors, warnings)
            }

            val isHealthy = errors.isEmpty()
            Log.i(TAG, "Full integrity check complete: ${if (isHealthy) "HEALTHY" else "CORRUPTED"} (${errors.size} errors, ${warnings.size} warnings)")

            IntegrityResult(
                isHealthy = isHealthy,
                errors = errors,
                warnings = warnings,
                checkType = CheckType.FULL
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in full integrity check", e)
            IntegrityResult(
                isHealthy = false,
                errors = listOf("Full integrity check failed: ${e.message}"),
                warnings = emptyList(),
                checkType = CheckType.FULL
            )
        }
    }

    /**
     * Get database statistics and health metrics
     *
     * @param databaseName Name of database file
     * @return DatabaseStats with size, table count, and other metrics
     */
    suspend fun getDatabaseStats(databaseName: String): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            val dbFile = File(databaseDir, databaseName)
            if (!dbFile.exists()) {
                return@withContext DatabaseStats(
                    sizeBytes = 0,
                    tableCount = 0,
                    indexCount = 0,
                    exists = false
                )
            }

            var tableCount = 0
            var indexCount = 0

            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                // Count tables
                db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        tableCount = cursor.getInt(0)
                    }
                }

                // Count indexes
                db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        indexCount = cursor.getInt(0)
                    }
                }
            }

            DatabaseStats(
                sizeBytes = dbFile.length(),
                tableCount = tableCount,
                indexCount = indexCount,
                exists = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error getting database stats", e)
            DatabaseStats(
                sizeBytes = 0,
                tableCount = 0,
                indexCount = 0,
                exists = false
            )
        }
    }

    /**
     * Check for common database corruption patterns
     *
     * @param db SQLiteDatabase to check
     * @param errors Mutable list to add errors to
     * @param warnings Mutable list to add warnings to
     */
    private fun checkCommonCorruptionPatterns(
        db: SQLiteDatabase,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        try {
            // Check if database is empty (might indicate corruption)
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null).use { cursor ->
                if (cursor.count == 0) {
                    warnings.add("Database contains no user tables (might be empty or corrupted)")
                }
            }

            // Check for orphaned indexes
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name NOT IN " +
                "(SELECT name FROM sqlite_master WHERE type='table')",
                null
            ).use { cursor ->
                if (cursor.count > 0) {
                    warnings.add("Found ${cursor.count} orphaned indexes")
                }
            }

        } catch (e: Exception) {
            errors.add("Error checking corruption patterns: ${e.message}")
        }
    }

    /**
     * Validate database schema consistency
     *
     * @param db SQLiteDatabase to validate
     * @param errors Mutable list to add errors to
     * @param warnings Mutable list to add warnings to
     */
    private fun validateSchemaConsistency(
        db: SQLiteDatabase,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        try {
            // Get all tables
            val tables = mutableListOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }

            // Validate each table can be accessed
            for (table in tables) {
                try {
                    db.rawQuery("SELECT COUNT(*) FROM \"$table\"", null).use { cursor ->
                        if (!cursor.moveToFirst()) {
                            errors.add("Table '$table' is inaccessible")
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Table '$table' validation failed: ${e.message}")
                }
            }

        } catch (e: Exception) {
            errors.add("Schema validation failed: ${e.message}")
        }
    }

    /**
     * Check if database file exists
     *
     * @param databaseName Name of database file
     * @return true if database exists, false otherwise
     */
    fun databaseExists(databaseName: String): Boolean {
        val dbFile = File(databaseDir, databaseName)
        return dbFile.exists()
    }

    /**
     * Get database file size
     *
     * @param databaseName Name of database file
     * @return Size in bytes, or 0 if not found
     */
    fun getDatabaseSize(databaseName: String): Long {
        val dbFile = File(databaseDir, databaseName)
        return if (dbFile.exists()) dbFile.length() else 0L
    }
}

/**
 * Result of integrity check
 *
 * @property isHealthy true if database is healthy, false if corrupted
 * @property errors List of corruption errors found
 * @property warnings List of potential issues (non-critical)
 * @property checkType Type of check performed
 */
data class IntegrityResult(
    val isHealthy: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val checkType: CheckType
)

/**
 * Database statistics and health metrics
 *
 * @property sizeBytes Database file size in bytes
 * @property tableCount Number of user tables (excludes sqlite_* tables)
 * @property indexCount Number of indexes
 * @property exists Whether database file exists
 */
data class DatabaseStats(
    val sizeBytes: Long,
    val tableCount: Int,
    val indexCount: Int,
    val exists: Boolean
)

/**
 * Type of integrity check performed
 */
enum class CheckType {
    /** Quick check using PRAGMA integrity_check only */
    QUICK,

    /** Full check including foreign keys and schema validation */
    FULL
}
