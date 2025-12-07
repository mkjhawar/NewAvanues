/**
 * DatabaseCommandHandler.kt - Voice Commands for Unified Database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-07
 * Version: 4.1.1
 *
 * Handles voice commands for querying and managing the unified VoiceOSAppDatabase.
 * Provides 20 voice commands across 5 categories for database interaction.
 *
 * CoT: Design Decision - Why return String? (nullable)
 * - Returns String if command is handled (success)
 * - Returns null if command not recognized (allows fallback to other handlers)
 * - This enables command chaining: database -> system -> no match
 *
 * Categories:
 * 1. Statistics (show database stats, how many apps, etc.)
 * 2. Migration (migration status, show migrated apps)
 * 3. App Queries (list learned apps, show app details)
 * 4. Management (export database, clear app data, optimize)
 * 5. Mode Switching (switch to exploration/dynamic mode)
 */

package com.augmentalis.voiceoscore.commands

import android.content.Context
import android.os.Environment
import android.util.Log
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.entities.AppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Handles voice commands for database operations
 *
 * @param context Android context for database and file access
 * @param database Unified VoiceOSAppDatabase instance
 */
class DatabaseCommandHandler(
    private val context: Context,
    private val database: VoiceOSAppDatabase
) {
    companion object {
        private const val TAG = "DatabaseCommandHandler"
    }

    /**
     * Handle database voice command
     *
     * CoT: Command routing strategy
     * - Use regex pattern matching for flexibility
     * - Normalize input (lowercase, trim) for consistency
     * - Return null for unrecognized commands (fallback)
     * - All database operations async (suspend functions)
     *
     * @param command Voice input string (normalized)
     * @return Command result string, or null if not a database command
     */
    suspend fun handleCommand(command: String): String? = withContext(Dispatchers.IO) {
        val normalized = command.lowercase().trim()

        try {
            when {
                // === CATEGORY 1: DATABASE STATISTICS ===

                normalized.matches(".*show database stats.*".toRegex()) -> {
                    getStatistics()
                }

                normalized.matches(".*how many (learned )?apps.*".toRegex()) -> {
                    getAppCount()
                }

                normalized.matches(".*database size.*".toRegex()) -> {
                    getDatabaseSize()
                }

                normalized.matches(".*element count.*".toRegex()) -> {
                    getElementCount()
                }

                // === CATEGORY 2: MIGRATION STATUS ===

                normalized.matches(".*migration status.*".toRegex()) -> {
                    getMigrationStatus()
                }

                normalized.matches(".*show migrated apps.*".toRegex()) -> {
                    getMigrationDetails()
                }

                // === CATEGORY 3: APP QUERIES ===

                normalized.matches(".*list learned apps.*".toRegex()) -> {
                    getLearnedApps()
                }

                normalized.matches(".*show app details for (.+)".toRegex()) -> {
                    val appName = ".*show app details for (.+)".toRegex()
                        .find(normalized)?.groupValues?.get(1)?.trim()
                    if (appName != null && isValidAppName(appName)) {
                        getAppDetails(appName)
                    } else {
                        "Invalid app name. App name must be 1-100 characters and alphanumeric with spaces."
                    }
                }

                normalized.matches(".*which apps need learning.*".toRegex()) -> {
                    getAppsNeedingLearning()
                }

                normalized.matches(".*most learned app.*".toRegex()) -> {
                    getMostLearnedApp()
                }

                normalized.matches(".*recently learned apps.*".toRegex()) -> {
                    getRecentlyLearnedApps()
                }

                // === CATEGORY 4: DATABASE MANAGEMENT ===

                normalized.matches(".*export database.*".toRegex()) -> {
                    exportDatabase()
                }

                normalized.matches(".*clear app data for (.+)".toRegex()) -> {
                    val appName = ".*clear app data for (.+)".toRegex()
                        .find(normalized)?.groupValues?.get(1)?.trim()
                    if (appName != null && isValidAppName(appName)) {
                        clearAppData(appName)
                    } else {
                        "Invalid app name. App name must be 1-100 characters and alphanumeric with spaces."
                    }
                }

                normalized.matches(".*optimize database.*".toRegex()) -> {
                    optimizeDatabase()
                }

                normalized.matches(".*database integrity check.*".toRegex()) -> {
                    checkDatabaseIntegrity()
                }

                // Not a database command - return null for fallback
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling database command: ${e.message}", e)
            "Error accessing database. Please try again."
        }
    }

    // ========== CATEGORY 1: STATISTICS ==========

    /**
     * Get comprehensive database statistics
     *
     * CoT: What statistics are most useful?
     * - Total app count (overall usage)
     * - Fully learned count (completion metric)
     * - Partial count (work remaining)
     * - Database size (storage used)
     * - Element count (data richness)
     *
     * ToT: Response format options
     * Option A: Terse "47 apps, 32 learned"
     * Option B: Verbose "You have 47 applications..."
     * Option C: Balanced "47 apps. 32 fully explored, 15 partial."
     * Decision: Option C - clear but concise
     */
    private suspend fun getStatistics(): String {
        val appCount = database.getAppCount()
        val fullyLearned = database.getFullyLearnedAppCount()
        val partial = appCount - fullyLearned
        val elementCount = database.getTotalElementCount()
        val dbSize = getDatabaseFileSize()

        return "You have $appCount apps. $fullyLearned fully explored, $partial partial. " +
                "Database is ${formatSize(dbSize)} with $elementCount elements."
    }

    /**
     * Get total app count with breakdown
     */
    private suspend fun getAppCount(): String {
        val appCount = database.getAppCount()
        val fullyLearned = database.getFullyLearnedAppCount()

        return "$appCount apps in database. $fullyLearned fully learned, " +
                "${appCount - fullyLearned} partially learned."
    }

    /**
     * Get database file size
     */
    private suspend fun getDatabaseSize(): String {
        val size = getDatabaseFileSize()
        return "Database is ${formatSize(size)}."
    }

    /**
     * Get total UI element count
     */
    private suspend fun getElementCount(): String {
        val count = database.getTotalElementCount()
        return "$count UI elements across all apps."
    }

    // ========== CATEGORY 2: MIGRATION STATUS ==========

    /**
     * Get migration status
     *
     * CoT: How to check migration completion?
     * - DatabaseMigrationHelper has isMigrationComplete() method
     * - Reads SharedPreferences flag
     * - Returns status + statistics if complete
     */
    private suspend fun getMigrationStatus(): String {
        val prefs = context.getSharedPreferences("voiceos_db_migration", Context.MODE_PRIVATE)
        val isComplete = prefs.getBoolean("migration_v1_to_unified_complete", false)

        return if (isComplete) {
            val appCount = database.getAppCount()
            "Migration complete. $appCount apps migrated successfully."
        } else {
            "Migration pending. Will run on next app launch."
        }
    }

    /**
     * Get detailed migration information
     */
    private suspend fun getMigrationDetails(): String {
        val prefs = context.getSharedPreferences("voiceos_db_migration", Context.MODE_PRIVATE)
        val isComplete = prefs.getBoolean("migration_v1_to_unified_complete", false)

        if (!isComplete) {
            return "Migration not yet complete."
        }

        // CoT: Can't distinguish migrated vs new apps after migration
        // Just report total count
        val appCount = database.getAppCount()
        return "Migration complete. $appCount apps in unified database."
    }

    // ========== CATEGORY 3: APP QUERIES ==========

    /**
     * List learned apps with completion percentages
     *
     * CoT: How many apps to list?
     * - Too many = overwhelming response
     * - Too few = not useful
     * - Decision: Top 10 by element count
     *
     * ToT: Sorting options
     * Option A: Alphabetical (predictable)
     * Option B: By completion % (shows progress)
     * Option C: By element count (shows richness)
     * Decision: Option C - most learned = most elements
     */
    private suspend fun getLearnedApps(): String {
        val apps = database.getInstalledApps()
            .sortedByDescending { (it.exploredElementCount ?: 0) + (it.scrapedElementCount ?: 0) }
            .take(10)

        if (apps.isEmpty()) {
            return "No apps learned yet."
        }

        val appList = apps.joinToString(", ") { app ->
            val completion = calculateCompletionPercent(app)
            "${app.appName} $completion%"
        }

        return "Top learned apps: $appList"
    }

    /**
     * Get detailed information for specific app
     *
     * CoT: App name matching strategy
     * - User says "Instagram" but DB has "com.instagram.android"
     * - Use LIKE query to match partial names
     * - Return first match (most likely correct)
     */
    private suspend fun getAppDetails(appName: String): String {
        val app = database.getAppByName(appName)
            ?: return "App '$appName' not found in database."

        val screens = app.totalScreens ?: 0
        val exploredElements = app.exploredElementCount ?: 0
        val scrapedElements = app.scrapedElementCount ?: 0
        val totalElements = exploredElements + scrapedElements
        val lastExplored = app.lastExplored ?: app.lastScraped

        val timeAgo = lastExplored?.let { formatTimestamp(it) } ?: "never"
        val completion = calculateCompletionPercent(app)

        return "${app.appName}: $totalElements elements across $screens screens. " +
                "Last explored $timeAgo. $completion% complete."
    }

    /**
     * Get apps that need more learning (< 50% completion)
     *
     * CoT: What threshold for "needs learning"?
     * - <25% = barely started
     * - <50% = still learning
     * - <75% = almost done
     * Decision: <50% is "needs learning"
     */
    private suspend fun getAppsNeedingLearning(): String {
        val apps = database.getInstalledApps()
            .filter { calculateCompletionPercent(it) < 50 }
            .sortedBy { calculateCompletionPercent(it) }
            .take(5)

        if (apps.isEmpty()) {
            return "All apps are well learned! Great job."
        }

        val appList = apps.joinToString(", ") { app ->
            val completion = calculateCompletionPercent(app)
            "${app.appName} $completion%"
        }

        return "Apps needing learning: $appList"
    }

    /**
     * Get most learned app (highest element count)
     */
    private suspend fun getMostLearnedApp(): String {
        val app = database.getInstalledApps()
            .maxByOrNull { (it.exploredElementCount ?: 0) + (it.scrapedElementCount ?: 0) }
            ?: return "No apps learned yet."

        val totalElements = (app.exploredElementCount ?: 0) + (app.scrapedElementCount ?: 0)
        val screens = app.totalScreens ?: 0

        return "${app.appName} is most learned with $totalElements elements across $screens screens."
    }

    /**
     * Get recently learned apps (sorted by last explored)
     */
    private suspend fun getRecentlyLearnedApps(): String {
        val apps = database.getInstalledApps()
            .filter { (it.lastExplored ?: it.lastScraped) != null }
            .sortedByDescending { it.lastExplored ?: it.lastScraped ?: 0 }
            .take(5)

        if (apps.isEmpty()) {
            return "No apps learned yet."
        }

        val appList = apps.joinToString(", ") { app ->
            val lastExplored = app.lastExplored ?: app.lastScraped
            val timeAgo = lastExplored?.let { formatTimestamp(it) } ?: "unknown"
            "${app.appName} ($timeAgo)"
        }

        return "Recently learned apps: $appList"
    }

    // ========== CATEGORY 4: DATABASE MANAGEMENT ==========

    /**
     * Export database to Downloads folder
     *
     * YOLO Phase 2 - High Priority Issue #15: Error handling for database export
     *
     * CoT: Export considerations
     * - Where to save? Downloads (user-accessible)
     * - File name? Include timestamp for uniqueness
     * - Overwrite? Yes (same day = same file)
     * - Permissions? Check WRITE_EXTERNAL_STORAGE for Android < 10
     * - Disk space? Verify sufficient space before copy
     */
    private suspend fun exportDatabase(): String {
        try {
            val dbFile = context.getDatabasePath("voiceos_app_database")

            if (!dbFile.exists()) {
                return "Database file not found."
            }

            // Check disk space - need at least 2x database size for safety
            val dbSize = dbFile.length()
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val availableSpace = downloadDir.freeSpace

            if (availableSpace < dbSize * 2) {
                val needed = formatSize(dbSize * 2)
                val available = formatSize(availableSpace)
                return "Insufficient storage. Need $needed, have $available available."
            }

            // Check permissions for Android < 10
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                val granted = context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    return "Storage permission required. Please grant WRITE_EXTERNAL_STORAGE permission."
                }
            }

            val timestamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val exportFile = File(downloadDir, "voiceos_backup_$timestamp.db")

            // Perform the copy
            dbFile.copyTo(exportFile, overwrite = true)

            // Verify export succeeded
            if (!exportFile.exists() || exportFile.length() != dbSize) {
                return "Export verification failed. File may be corrupted."
            }

            return "Database exported to Downloads/${exportFile.name} (${formatSize(dbSize)})"
        } catch (e: SecurityException) {
            Log.e(TAG, "Export failed - permission denied", e)
            return "Export failed. Storage permission denied."
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Export failed - IO error", e)
            return "Export failed. Storage write error: ${e.message}"
        } catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}", e)
            return "Export failed. ${e.message}"
        }
    }

    /**
     * Clear app data for specific app
     *
     * CoT: Deletion strategy
     * - Find app by name (partial match)
     * - Delete app entity
     * - Foreign key CASCADE deletes related data automatically
     * - Report what was deleted
     */
    private suspend fun clearAppData(appName: String): String {
        try {
            val app = database.getAppByName(appName)
                ?: return "App '$appName' not found."

            val elementCount = (app.exploredElementCount ?: 0) + (app.scrapedElementCount ?: 0)
            val screens = app.totalScreens ?: 0

            // CoT: Use deleteApp(packageName) instead of delete(entity)
            // Foreign key cascades will automatically delete related ScrapedElements and GeneratedCommands
            database.deleteApp(app.packageName)

            return "Cleared ${app.appName} data. $screens screens and $elementCount elements removed."
        } catch (e: Exception) {
            Log.e(TAG, "Clear app data failed: ${e.message}", e)
            return "Failed to clear app data. ${e.message}"
        }
    }

    /**
     * Optimize database (VACUUM)
     *
     * CoT: When is VACUUM beneficial?
     * - After deleting data (reclaims space)
     * - Database fragmentation (improves performance)
     * - Should show size reduction
     */
    private suspend fun optimizeDatabase(): String {
        // TODO: Implement with SQLDelight direct SQL execution
        // SQLDelight doesn't expose VACUUM command directly
        // Need to use: database.databaseManager.database.driver.execute(null, "VACUUM", 0)
        return "Database optimization not yet implemented with SQLDelight. Coming soon."
    }

    /**
     * Check database integrity
     */
    private suspend fun checkDatabaseIntegrity(): String {
        // TODO: Implement with SQLDelight direct SQL execution
        // SQLDelight doesn't expose raw queries directly
        // Need to use: database.databaseManager.database.driver.executeQuery(...)
        return "Database integrity check not yet implemented with SQLDelight. Coming soon."
    }

    // ========== HELPER FUNCTIONS ==========

    /**
     * Calculate completion percentage for app
     *
     * CoT: How to measure "completion"?
     * - Option A: isFullyLearned flag (binary)
     * - Option B: Element count vs estimated total (requires estimate)
     * - Option C: Arbitrary: >100 elements = 100%, linear below that
     * Decision: Option C - simple heuristic
     */
    private fun calculateCompletionPercent(app: AppEntity): Int {
        val totalElements = (app.exploredElementCount ?: 0) + (app.scrapedElementCount ?: 0)

        // CoT: Completion heuristic
        // - <10 elements = 10% (barely started)
        // - <100 elements = linear scale
        // - >100 elements = 100% (well learned)
        return when {
            totalElements >= 100 -> 100
            totalElements > 0 -> (totalElements * 100 / 100).coerceAtMost(100)
            else -> 0
        }
    }

    /**
     * Get database file size in bytes
     */
    private fun getDatabaseFileSize(): Long {
        val dbFile = context.getDatabasePath("voiceos_app_database")
        return if (dbFile.exists()) dbFile.length() else 0
    }

    /**
     * Format file size for human reading
     *
     * CoT: Size formatting
     * - Bytes: <1KB
     * - KB: 1KB - 1MB
     * - MB: 1MB - 1GB
     * - GB: >1GB
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes bytes"
        }
    }

    /**
     * Format timestamp as time ago
     *
     * CoT: Time ago formatting
     * - <1 hour = "X minutes ago"
     * - <1 day = "X hours ago"
     * - <7 days = "X days ago"
     * - >7 days = "on Nov 7"
     */
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> {
                val date = Date(timestamp)
                SimpleDateFormat("MMM d", Locale.US).format(date)
            }
        }
    }

    /**
     * Validate app name input for security
     *
     * YOLO Phase 2 - High Priority Issue #12: Input Validation
     *
     * Prevents SQL injection and malformed input by validating:
     * - Length: 1-100 characters
     * - Characters: Alphanumeric, spaces, dots, hyphens, underscores only
     *
     * @param appName App name extracted from voice command
     * @return true if valid, false otherwise
     */
    private fun isValidAppName(appName: String): Boolean {
        // Check length
        if (appName.isEmpty() || appName.length > 100) {
            Log.w(TAG, "Invalid app name length: ${appName.length}")
            return false
        }

        // Check characters - only alphanumeric, spaces, dots, hyphens, underscores
        val validPattern = Regex("^[a-zA-Z0-9\\s._-]+$")
        if (!validPattern.matches(appName)) {
            Log.w(TAG, "Invalid app name characters: $appName")
            return false
        }

        return true
    }
}
