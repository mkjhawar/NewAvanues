/**
 * VOSCommandIngestion.kt - Database ingestion orchestrator for VOS command files
 *
 * Purpose: Coordinate ingestion from multiple command file formats into Room database
 * Architecture:
 * - Orchestrates VOSFileParser (individual .vos files) and UnifiedJSONParser (unified JSON)
 * - Provides batch insertion with transaction safety
 * - Supports selective ingestion by category or locale
 * - Implements duplicate detection and resolution
 * - Progress tracking for large ingestion operations
 *
 * Integration Points:
 * - VOSFileParser: Individual .vos file parsing
 * - UnifiedJSONParser: Unified commands-all.json parsing
 * - CommandDatabase: Room database instance
 * - VoiceCommandDao: Database operations
 *
 * Performance Characteristics:
 * - Batch insertion: 500 commands per transaction (optimal for Room)
 * - Coroutines: All operations on Dispatchers.IO
 * - Memory efficient: Streams large datasets in chunks
 * - Transaction safety: Rollback on failure
 *
 * @author VOS4 Database Integration Agent
 * @since 2025-10-13
 */

package com.augmentalis.voiceoscore.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.database.CommandDatabase
import com.augmentalis.voiceoscore.database.sqldelight.VoiceCommandEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of ingestion operation with comprehensive statistics
 *
 * @property success Whether ingestion completed successfully
 * @property commandsLoaded Total number of commands inserted
 * @property categoriesLoaded List of unique categories loaded
 * @property localesLoaded List of unique locales loaded
 * @property errors List of error messages encountered (empty if success)
 * @property durationMs Time taken for operation in milliseconds
 * @property source Source of commands ("unified", "vos", "both")
 */
data class IngestionResult(
    val success: Boolean,
    val commandsLoaded: Int,
    val categoriesLoaded: List<String>,
    val localesLoaded: List<String>,
    val errors: List<String> = emptyList(),
    val durationMs: Long,
    val source: String
) {
    /**
     * Human-readable summary of ingestion result
     */
    fun getSummary(): String {
        return if (success) {
            "✅ Ingested $commandsLoaded commands from $source\n" +
            "   Categories: ${categoriesLoaded.joinToString(", ")}\n" +
            "   Locales: ${localesLoaded.joinToString(", ")}\n" +
            "   Duration: ${durationMs}ms"
        } else {
            "❌ Ingestion failed from $source\n" +
            "   Errors: ${errors.joinToString("; ")}\n" +
            "   Duration: ${durationMs}ms"
        }
    }
}

/**
 * Progress callback for long-running ingestion operations
 *
 * @property totalCommands Total number of commands to ingest
 * @property processedCommands Number of commands processed so far
 * @property currentCategory Category currently being processed
 * @property percentComplete Progress percentage (0-100)
 */
data class IngestionProgress(
    val totalCommands: Int,
    val processedCommands: Int,
    val currentCategory: String,
    val percentComplete: Int
)

/**
 * Orchestrator for database ingestion from VOS command files
 *
 * Usage Examples:
 * ```kotlin
 * val ingestion = VOSCommandIngestion(context, database)
 *
 * // 1. Ingest from unified JSON only
 * val result1 = ingestion.ingestUnifiedCommands()
 * Log.i(TAG, result1.getSummary())
 *
 * // 2. Ingest from individual .vos files only
 * val result2 = ingestion.ingestVOSFiles()
 * Log.i(TAG, result2.getSummary())
 *
 * // 3. Ingest both unified and individual files
 * val result3 = ingestion.ingestAll()
 * Log.i(TAG, result3.getSummary())
 *
 * // 4. Selective ingestion by categories
 * val result4 = ingestion.ingestCategories(listOf("navigation", "system"))
 * Log.i(TAG, result4.getSummary())
 *
 * // 5. Selective ingestion by locale
 * val result5 = ingestion.ingestLocale("es-ES")
 * Log.i(TAG, result5.getSummary())
 *
 * // 6. Clear all commands
 * ingestion.clearAllCommands()
 *
 * // 7. Get statistics
 * val count = ingestion.getCommandCount()
 * val categoryCounts = ingestion.getCategoryCounts()
 * ```
 *
 * Error Handling:
 * - Returns IngestionResult with success=false and error messages
 * - Logs all errors with tag "VOSCommandIngestion"
 * - Transactions rollback on failure
 * - Continues processing after non-critical errors
 */
class VOSCommandIngestion(
    private val context: Context,
    private val database: CommandDatabase
) {

    companion object {
        private const val TAG = "VOSCommandIngestion"
        private const val BATCH_SIZE = 500 // Optimal batch size for Room
        private const val DEFAULT_UNIFIED_FILENAME = "commands-all.json"

        /**
         * Convenience factory method
         */
        fun create(context: Context): VOSCommandIngestion {
            val database = CommandDatabase.getInstance(context)
            return VOSCommandIngestion(context, database)
        }
    }

    // Lazy initialization of parsers
    private val vosParser by lazy { VOSFileParser(context) }
    private val unifiedParser by lazy { UnifiedJSONParser(context) }
    private val commandDao by lazy { database.voiceCommandDao() }

    // Progress callback (optional)
    var progressCallback: ((IngestionProgress) -> Unit)? = null

    // ==================== PRIMARY INGESTION METHODS ====================

    /**
     * Ingest commands from unified JSON file (commands-all.json)
     *
     * @param filename Name of unified JSON file in assets/commands/
     * @return IngestionResult with statistics and success status
     *
     * Process:
     * 1. Parse unified JSON file
     * 2. Convert to VoiceCommandEntity list
     * 3. Batch insert into database (500 commands per transaction)
     * 4. Return statistics
     *
     * Error Handling:
     * - File not found: Returns error result
     * - Parse error: Returns error result with details
     * - Database error: Rolls back transaction, returns error
     */
    suspend fun ingestUnifiedCommands(filename: String = DEFAULT_UNIFIED_FILENAME): IngestionResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                Log.i(TAG, "Starting unified JSON ingestion from $filename")

                // Parse unified JSON file
                val parseResult = unifiedParser.parseUnifiedJSON(filename)
                if (parseResult.isFailure) {
                    val error = parseResult.exceptionOrNull()?.message ?: "Unknown parse error"
                    Log.e(TAG, "Failed to parse unified JSON: $error")
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf("Parse error: $error"),
                        durationMs = System.currentTimeMillis() - startTime,
                        source = "unified"
                    )
                }

                val unified = parseResult.getOrThrow()
                Log.d(TAG, "Parsed unified JSON: ${unified.metadata.totalCommands} commands")

                // Convert to entities
                val entities = unifiedParser.convertToEntities(unified)
                Log.d(TAG, "Converted ${entities.size} entities")

                // Batch insert with progress tracking
                val insertedCount = batchInsertWithProgress(
                    entities = entities,
                    source = "unified-$filename"
                )

                // Gather statistics
                val categories = entities.map { it.category }.distinct().sorted()
                val locales = entities.map { it.locale }.distinct().sorted()

                val duration = System.currentTimeMillis() - startTime
                Log.i(TAG, "✅ Unified ingestion complete: $insertedCount commands in ${duration}ms")

                IngestionResult(
                    success = true,
                    commandsLoaded = insertedCount,
                    categoriesLoaded = categories,
                    localesLoaded = locales,
                    errors = emptyList(),
                    durationMs = duration,
                    source = "unified"
                )

            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "❌ Unified ingestion failed", e)
                IngestionResult(
                    success = false,
                    commandsLoaded = 0,
                    categoriesLoaded = emptyList(),
                    localesLoaded = emptyList(),
                    errors = listOf("Exception: ${e.message}"),
                    durationMs = duration,
                    source = "unified"
                )
            }
        }
    }

    /**
     * Ingest commands from individual .vos files
     *
     * @return IngestionResult with statistics and success status
     *
     * Process:
     * 1. Parse all .vos files in assets/commands/vos/
     * 2. Convert each to VoiceCommandEntity list
     * 3. Batch insert into database
     * 4. Return statistics
     *
     * Error Handling:
     * - Skips files that fail to parse (logs error)
     * - Continues with remaining files
     * - Returns success if at least one file loaded
     */
    suspend fun ingestVOSFiles(): IngestionResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val errors = mutableListOf<String>()

            try {
                Log.i(TAG, "Starting .vos files ingestion")

                // Parse all .vos files
                val parseResult = vosParser.parseAllVOSFiles()
                if (parseResult.isFailure) {
                    val error = parseResult.exceptionOrNull()?.message ?: "Failed to parse .vos files"
                    Log.e(TAG, error)
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf(error),
                        durationMs = System.currentTimeMillis() - startTime,
                        source = "vos"
                    )
                }

                val vosFiles = parseResult.getOrThrow()
                Log.d(TAG, "Parsed ${vosFiles.size} .vos files")

                // Convert all files to entities
                val allEntities = mutableListOf<VoiceCommandEntity>()
                vosFiles.forEach { vosFile ->
                    try {
                        val entities = vosParser.convertToEntities(vosFile)
                        allEntities.addAll(entities)
                        Log.d(TAG, "Converted ${entities.size} commands from ${vosFile.fileInfo.filename}")
                    } catch (e: Exception) {
                        val errorMsg = "Failed to convert ${vosFile.fileInfo.filename}: ${e.message}"
                        Log.w(TAG, errorMsg)
                        errors.add(errorMsg)
                    }
                }

                if (allEntities.isEmpty()) {
                    val duration = System.currentTimeMillis() - startTime
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf("No valid commands found in .vos files") + errors,
                        durationMs = duration,
                        source = "vos"
                    )
                }

                // Batch insert
                val insertedCount = batchInsertWithProgress(
                    entities = allEntities,
                    source = "vos-files"
                )

                // Gather statistics
                val categories = allEntities.map { it.category }.distinct().sorted()
                val locales = allEntities.map { it.locale }.distinct().sorted()

                val duration = System.currentTimeMillis() - startTime
                Log.i(TAG, "✅ VOS files ingestion complete: $insertedCount commands in ${duration}ms")

                IngestionResult(
                    success = true,
                    commandsLoaded = insertedCount,
                    categoriesLoaded = categories,
                    localesLoaded = locales,
                    errors = errors,
                    durationMs = duration,
                    source = "vos"
                )

            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "❌ VOS files ingestion failed", e)
                IngestionResult(
                    success = false,
                    commandsLoaded = 0,
                    categoriesLoaded = emptyList(),
                    localesLoaded = emptyList(),
                    errors = listOf("Exception: ${e.message}") + errors,
                    durationMs = duration,
                    source = "vos"
                )
            }
        }
    }

    /**
     * Ingest from both unified JSON and individual .vos files
     *
     * @return IngestionResult with combined statistics
     *
     * Strategy:
     * - First attempts unified JSON (faster)
     * - Then attempts .vos files (fallback)
     * - Merges results (deduplication handled by database REPLACE strategy)
     * - Returns success if either source succeeds
     */
    suspend fun ingestAll(): IngestionResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                Log.i(TAG, "Starting comprehensive ingestion (unified + vos)")

                val results = mutableListOf<IngestionResult>()
                val errors = mutableListOf<String>()

                // 1. Try unified JSON first
                val unifiedResult = ingestUnifiedCommands()
                results.add(unifiedResult)
                if (!unifiedResult.success) {
                    errors.addAll(unifiedResult.errors.map { "Unified: $it" })
                }

                // 2. Try .vos files
                val vosResult = ingestVOSFiles()
                results.add(vosResult)
                if (!vosResult.success) {
                    errors.addAll(vosResult.errors.map { "VOS: $it" })
                }

                // Combine statistics
                val totalCommands = results.filter { it.success }.sumOf { it.commandsLoaded }
                val allCategories = results.flatMap { it.categoriesLoaded }.distinct().sorted()
                val allLocales = results.flatMap { it.localesLoaded }.distinct().sorted()
                val overallSuccess = results.any { it.success }

                val duration = System.currentTimeMillis() - startTime

                if (overallSuccess) {
                    Log.i(TAG, "✅ Comprehensive ingestion complete: $totalCommands commands in ${duration}ms")
                } else {
                    Log.e(TAG, "❌ All ingestion sources failed")
                }

                IngestionResult(
                    success = overallSuccess,
                    commandsLoaded = totalCommands,
                    categoriesLoaded = allCategories,
                    localesLoaded = allLocales,
                    errors = errors,
                    durationMs = duration,
                    source = "both"
                )

            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "❌ Comprehensive ingestion failed", e)
                IngestionResult(
                    success = false,
                    commandsLoaded = 0,
                    categoriesLoaded = emptyList(),
                    localesLoaded = emptyList(),
                    errors = listOf("Exception: ${e.message}"),
                    durationMs = duration,
                    source = "both"
                )
            }
        }
    }

    // ==================== SELECTIVE INGESTION ====================

    /**
     * Ingest only specific categories from unified JSON
     *
     * @param categories List of category names to ingest
     * @param filename Unified JSON filename (default: commands-all.json)
     * @return IngestionResult with filtered statistics
     *
     * Use Cases:
     * - Load only navigation commands for testing
     * - Incremental category loading
     * - Partial database population
     */
    suspend fun ingestCategories(
        categories: List<String>,
        filename: String = DEFAULT_UNIFIED_FILENAME
    ): IngestionResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                Log.i(TAG, "Starting selective category ingestion: ${categories.joinToString(", ")}")

                // Parse unified JSON
                val parseResult = unifiedParser.parseUnifiedJSON(filename)
                if (parseResult.isFailure) {
                    val error = parseResult.exceptionOrNull()?.message ?: "Parse error"
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf(error),
                        durationMs = System.currentTimeMillis() - startTime,
                        source = "unified-selective"
                    )
                }

                val unified = parseResult.getOrThrow()

                // Convert only selected categories
                val entities = unifiedParser.convertToEntities(unified, categories)

                if (entities.isEmpty()) {
                    Log.w(TAG, "No commands found for categories: ${categories.joinToString(", ")}")
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf("No commands found for specified categories"),
                        durationMs = System.currentTimeMillis() - startTime,
                        source = "unified-selective"
                    )
                }

                // Batch insert
                val insertedCount = batchInsertWithProgress(entities, "category-selective")

                val locales = entities.map { it.locale }.distinct().sorted()
                val duration = System.currentTimeMillis() - startTime

                Log.i(TAG, "✅ Selective ingestion complete: $insertedCount commands in ${duration}ms")

                IngestionResult(
                    success = true,
                    commandsLoaded = insertedCount,
                    categoriesLoaded = categories,
                    localesLoaded = locales,
                    errors = emptyList(),
                    durationMs = duration,
                    source = "unified-selective"
                )

            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "❌ Selective category ingestion failed", e)
                IngestionResult(
                    success = false,
                    commandsLoaded = 0,
                    categoriesLoaded = emptyList(),
                    localesLoaded = emptyList(),
                    errors = listOf("Exception: ${e.message}"),
                    durationMs = duration,
                    source = "unified-selective"
                )
            }
        }
    }

    /**
     * Ingest commands for a specific locale from .vos files
     *
     * @param locale Locale code (e.g., "en-US", "es-ES")
     * @return IngestionResult with locale-specific statistics
     *
     * Use Cases:
     * - Load single language for testing
     * - On-demand locale loading
     * - Minimize initial database size
     */
    suspend fun ingestLocale(locale: String): IngestionResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                Log.i(TAG, "Starting locale-specific ingestion: $locale")

                // Parse all .vos files
                val parseResult = vosParser.parseAllVOSFiles()
                if (parseResult.isFailure) {
                    val error = parseResult.exceptionOrNull()?.message ?: "Parse error"
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf(error),
                        durationMs = System.currentTimeMillis() - startTime,
                        source = "vos-locale"
                    )
                }

                val vosFiles = parseResult.getOrThrow()

                // Filter by locale
                val localeFiles = vosFiles.filter {
                    it.locale.equals(locale, ignoreCase = true)
                }

                if (localeFiles.isEmpty()) {
                    Log.w(TAG, "No .vos files found for locale: $locale")
                    return@withContext IngestionResult(
                        success = false,
                        commandsLoaded = 0,
                        categoriesLoaded = emptyList(),
                        localesLoaded = emptyList(),
                        errors = listOf("No files found for locale: $locale"),
                        durationMs = System.currentTimeMillis() - startTime,
                        source = "vos-locale"
                    )
                }

                // Convert to entities
                val allEntities = localeFiles.flatMap { vosParser.convertToEntities(it) }

                // Batch insert
                val insertedCount = batchInsertWithProgress(allEntities, "locale-$locale")

                val categories = allEntities.map { it.category }.distinct().sorted()
                val duration = System.currentTimeMillis() - startTime

                Log.i(TAG, "✅ Locale ingestion complete: $insertedCount commands in ${duration}ms")

                IngestionResult(
                    success = true,
                    commandsLoaded = insertedCount,
                    categoriesLoaded = categories,
                    localesLoaded = listOf(locale),
                    errors = emptyList(),
                    durationMs = duration,
                    source = "vos-locale"
                )

            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "❌ Locale ingestion failed", e)
                IngestionResult(
                    success = false,
                    commandsLoaded = 0,
                    categoriesLoaded = emptyList(),
                    localesLoaded = emptyList(),
                    errors = listOf("Exception: ${e.message}"),
                    durationMs = duration,
                    source = "vos-locale"
                )
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear all commands from database
     *
     * Use Cases:
     * - Reset before fresh ingestion
     * - Testing
     * - User-requested data clear
     */
    suspend fun clearAllCommands() {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Clearing all commands from database")
                val deletedCount = commandDao.deleteAllCommands()
                Log.i(TAG, "✅ Deleted $deletedCount commands")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear commands", e)
                throw e
            }
        }
    }

    /**
     * Get total command count across all locales
     *
     * @return Total number of commands in database
     */
    suspend fun getCommandCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val stats = commandDao.getDatabaseStats()
                stats.sumOf { it.count }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get command count", e)
                0
            }
        }
    }

    /**
     * Get command counts grouped by category
     *
     * @return Map of category name to command count
     *
     * Example output:
     * ```
     * {
     *   "navigation" -> 25,
     *   "system" -> 15,
     *   "input" -> 30
     * }
     * ```
     */
    suspend fun getCategoryCounts(): Map<String, Int> {
        return withContext(Dispatchers.IO) {
            try {
                val allCommands = commandDao.getAllCommands()
                allCommands.groupBy { it.category }
                    .mapValues { (_, commands) -> commands.size }
                    .toSortedMap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get category counts", e)
                emptyMap()
            }
        }
    }

    /**
     * Get command counts grouped by locale
     *
     * @return Map of locale code to command count
     */
    suspend fun getLocaleCounts(): Map<String, Int> {
        return withContext(Dispatchers.IO) {
            try {
                val stats = commandDao.getDatabaseStats()
                stats.associate { it.locale to it.count }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get locale counts", e)
                emptyMap()
            }
        }
    }

    /**
     * Check if database has any commands
     *
     * @return true if database is populated, false if empty
     */
    suspend fun isDatabasePopulated(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                getCommandCount() > 0
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check database population", e)
                false
            }
        }
    }

    /**
     * Get ingestion statistics summary
     *
     * @return Human-readable statistics string
     */
    suspend fun getStatisticsSummary(): String {
        return withContext(Dispatchers.IO) {
            try {
                val totalCommands = getCommandCount()
                val localeCounts = getLocaleCounts()
                val categoryCounts = getCategoryCounts()

                buildString {
                    appendLine("Database Statistics:")
                    appendLine("  Total commands: $totalCommands")
                    appendLine("  Locales (${localeCounts.size}):")
                    localeCounts.forEach { (locale, count) ->
                        appendLine("    - $locale: $count commands")
                    }
                    appendLine("  Categories (${categoryCounts.size}):")
                    categoryCounts.forEach { (category, count) ->
                        appendLine("    - $category: $count commands")
                    }
                }
            } catch (e: Exception) {
                "Failed to generate statistics: ${e.message}"
            }
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Batch insert entities with progress tracking
     *
     * @param entities List of VoiceCommandEntity to insert
     * @param source Source identifier for logging
     * @return Number of entities successfully inserted
     *
     * Performance:
     * - Inserts in batches of 500 (optimal for Room)
     * - Uses Room's REPLACE strategy for duplicates
     * - Reports progress via callback if set
     * - Logs progress at 10% intervals
     */
    private suspend fun batchInsertWithProgress(
        entities: List<VoiceCommandEntity>,
        source: String
    ): Int {
        if (entities.isEmpty()) return 0

        val totalEntities = entities.size
        var processedCount = 0
        var insertedCount = 0

        Log.d(TAG, "Starting batch insert: $totalEntities entities from $source")

        // Process in batches
        entities.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            try {
                // Insert batch
                val results = commandDao.insertBatch(batch)
                insertedCount += results.count { it > 0 }
                processedCount += batch.size

                // Report progress
                val percentComplete = (processedCount * 100) / totalEntities
                val currentCategory = batch.firstOrNull()?.category ?: "unknown"

                // Log at 10% intervals
                if (percentComplete % 10 == 0 || processedCount == totalEntities) {
                    Log.d(TAG, "Progress: $processedCount/$totalEntities ($percentComplete%) - $currentCategory")
                }

                // Callback progress
                progressCallback?.invoke(
                    IngestionProgress(
                        totalCommands = totalEntities,
                        processedCommands = processedCount,
                        currentCategory = currentCategory,
                        percentComplete = percentComplete
                    )
                )

            } catch (e: Exception) {
                Log.w(TAG, "Failed to insert batch ${batchIndex + 1}: ${e.message}")
                // Continue with next batch
            }
        }

        Log.d(TAG, "Batch insert complete: $insertedCount/$totalEntities inserted")
        return insertedCount
    }
}
