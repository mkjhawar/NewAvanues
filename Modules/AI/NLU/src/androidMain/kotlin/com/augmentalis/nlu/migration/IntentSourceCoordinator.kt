package com.augmentalis.nlu.migration

import android.content.Context
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.IntentExampleQueries
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.nlu.ava.AssetExtractor
import com.augmentalis.nlu.ava.converter.AvaToEntityConverter
import com.augmentalis.nlu.ava.io.AvaFileReader
import com.augmentalis.nlu.LanguagePackManager
import com.augmentalis.nlu.nluLogDebug
import com.augmentalis.nlu.nluLogError
import com.augmentalis.nlu.nluLogInfo
import com.augmentalis.nlu.nluLogWarn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Intent Source Coordinator
 *
 * Orchestrates loading from multiple sources:
 * 1. .ava files (core/voiceos/user)
 * 2. Legacy JSON fallback
 *
 * Priority: Try .ava files first, fallback to JSON if not found
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 */
class IntentSourceCoordinator(
    private val context: Context
) {

    companion object {
        private const val TAG = "IntentSourceCoordinator"
        private const val JSON_PATH = "intent_examples.json"
    }

    private val avaFileReader by lazy { AvaFileReader() }
    private val languagePackManager by lazy { LanguagePackManager(context) }

    // SQLDelight database and queries
    private val database: AVADatabase by lazy {
        DatabaseDriverFactory(context).createDriver().createDatabase()
    }
    private val queries: IntentExampleQueries by lazy { database.intentExampleQueries }

    // Use app-specific storage paths
    private val storageBase = AssetExtractor.getStorageBasePath(context)
    private val corePath = "$storageBase/core"
    private val voiceosPath = "$storageBase/voiceos"
    private val userPath = "$storageBase/user"

    suspend fun migrateIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if this is the first run (no examples in database)
            val hasExamples = queries.hasExamples().executeAsOne()

            if (!hasExamples) {
                nluLogInfo(TAG, "First run detected - starting intent examples migration from .ava files...")
                migrate()
                val count = queries.count().executeAsOne()
                nluLogInfo(TAG, "Migration complete. Total examples: $count")
                return@withContext true
            } else {
                // Database has examples, but check if they're from JSON fallback
                val allExamples = queries.selectAll().executeAsList()
                val hasJsonSource = allExamples.any { it.source == "STATIC_JSON" }

                if (hasJsonSource) {
                    // Database was populated from JSON fallback, need to reload from .ava files
                    nluLogInfo(TAG, "Database contains JSON fallback data - reloading from .ava files...")
                    clearDatabase()
                    migrate()
                    val count = queries.count().executeAsOne()
                    nluLogInfo(TAG, "Migration complete. Total examples: $count")
                    return@withContext true
                } else {
                    nluLogInfo(TAG, "Database already populated from .ava files, skipping migration")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            nluLogError(TAG, "Migration failed: ${e.message}", e)
            false
        }
    }

    suspend fun forceMigration(): Int = withContext(Dispatchers.IO) {
        migrate()
    }

    private suspend fun migrate(): Int = withContext(Dispatchers.IO) {
        val insertParams = try {
            val avaParams = loadFromAvaSources()
            if (avaParams.isNotEmpty()) {
                nluLogInfo(TAG, "Using .ava files as migration source (${avaParams.size} examples)")
                avaParams
            } else {
                nluLogInfo(TAG, "No .ava files found, falling back to JSON")
                loadFromJsonSource()
            }
        } catch (e: Exception) {
            nluLogWarn(TAG, "Failed to load .ava files, falling back to JSON: ${e.message}")
            loadFromJsonSource()
        }

        if (insertParams.isEmpty()) {
            nluLogError(TAG, "No intent examples found from any source")
            return@withContext 0
        }

        // Insert using SQLDelight transaction
        var insertedCount = 0
        database.transaction {
            insertParams.forEach { params ->
                try {
                    queries.insert(
                        example_hash = params.exampleHash,
                        intent_id = params.intentId,
                        example_text = params.exampleText,
                        is_primary = params.isPrimary,
                        source = params.source,
                        format_version = params.formatVersion,
                        ipc_code = params.ipcCode,
                        locale = params.locale,
                        created_at = params.createdAt,
                        usage_count = params.usageCount,
                        last_used = params.lastUsed
                    )
                    insertedCount++
                } catch (e: Exception) {
                    // Duplicate hash - skip silently (expected for REPLACE behavior)
                }
            }
        }

        nluLogInfo(TAG, "Inserted $insertedCount examples (${insertParams.size - insertedCount} duplicates skipped)")

        val intentCounts = queries.countPerIntent().executeAsList()
        nluLogDebug(TAG, "=== Intent Example Counts ===")
        intentCounts.forEach { row ->
            nluLogDebug(TAG, "  ${row.intent_id}: ${row.example_count} examples")
        }

        insertedCount
    }

    private suspend fun loadFromAvaSources(): List<AvaToEntityConverter.IntentExampleInsertParams> = withContext(Dispatchers.IO) {
        try {
            val activeLanguage = languagePackManager.getActiveLanguage()
            nluLogInfo(TAG, "Loading from .ava files for locale: $activeLanguage")
            nluLogDebug(TAG, "Storage paths: core=$corePath, voiceos=$voiceosPath, user=$userPath")

            // Load from external storage (extracted from APK)
            val coreIntents = avaFileReader.loadIntentsFromDirectory(
                "$corePath/$activeLanguage",
                "CORE"
            )
            val voiceosIntents = avaFileReader.loadIntentsFromDirectory(
                "$voiceosPath/$activeLanguage",
                "VOICEOS"
            )
            val userIntents = avaFileReader.loadIntentsFromDirectory(
                "$userPath/$activeLanguage",
                "USER"
            )

            // Load from APK assets (ava-examples folder)
            val assetIntents = try {
                nluLogDebug(TAG, "Loading .ava files from APK assets (ava-examples/$activeLanguage/)")
                val assetManager = context.assets
                val assetPath = "ava-examples/$activeLanguage"
                val files = assetManager.list(assetPath) ?: emptyArray()
                val avaFiles = files.filter { it.endsWith(".ava") }

                nluLogInfo(TAG, "Found ${avaFiles.size} .ava files in assets: ${avaFiles.joinToString()}")

                val intents = mutableListOf<com.augmentalis.nlu.ava.model.AvaIntent>()
                avaFiles.forEach { fileName ->
                    try {
                        val json = assetManager.open("$assetPath/$fileName").bufferedReader().use { it.readText() }
                        val fileIntents = avaFileReader.parseAvaFile(json, "ASSETS")
                        intents.addAll(fileIntents)
                        nluLogDebug(TAG, "Loaded ${fileIntents.size} intents from assets/$assetPath/$fileName")
                    } catch (e: Exception) {
                        nluLogWarn(TAG, "Failed to load $fileName from assets: ${e.message}")
                    }
                }
                intents
            } catch (e: Exception) {
                nluLogWarn(TAG, "Failed to load from assets: ${e.message}")
                emptyList()
            }

            val allIntents = coreIntents + voiceosIntents + userIntents + assetIntents

            if (allIntents.isEmpty()) {
                nluLogWarn(TAG, "No intents found in .ava files for $activeLanguage")
                return@withContext emptyList()
            }

            val params = AvaToEntityConverter.convertToInsertParams(allIntents)
            nluLogInfo(TAG, "Loaded ${params.size} examples from ${allIntents.size} intents (.ava files)")

            params
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to load from .ava files: ${e.message}", e)
            emptyList()
        }
    }

    private fun loadFromJsonSource(): List<AvaToEntityConverter.IntentExampleInsertParams> {
        val json = loadJsonFromAssets()
        if (json == null) {
            nluLogError(TAG, "Failed to load intent_examples.json")
            return emptyList()
        }

        val jsonObject = JSONObject(json)
        nluLogDebug(TAG, "Loaded JSON with ${jsonObject.length()} intents")

        val params = mutableListOf<AvaToEntityConverter.IntentExampleInsertParams>()
        val intentNames = jsonObject.keys()
        val timestamp = System.currentTimeMillis()

        while (intentNames.hasNext()) {
            val intentId = intentNames.next()
            val examplesArray = jsonObject.getJSONArray(intentId)

            nluLogDebug(TAG, "Processing intent: $intentId (${examplesArray.length()} examples)")

            for (i in 0 until examplesArray.length()) {
                val exampleText = examplesArray.getString(i)

                params.add(
                    AvaToEntityConverter.IntentExampleInsertParams(
                        exampleHash = AvaToEntityConverter.generateHash(intentId, exampleText),
                        intentId = intentId,
                        exampleText = exampleText,
                        isPrimary = true,
                        source = "STATIC_JSON",
                        formatVersion = "v1.0",
                        ipcCode = null,
                        locale = "en-US",
                        createdAt = timestamp,
                        usageCount = 0,
                        lastUsed = null
                    )
                )
            }
        }

        nluLogInfo(TAG, "Parsed ${params.size} examples from ${jsonObject.length()} intents (JSON)")
        return params
    }

    private fun loadJsonFromAssets(): String? {
        return try {
            context.assets.open(JSON_PATH).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to load JSON from assets: ${e.message}", e)
            null
        }
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        try {
            val count = queries.count().executeAsOne()
            queries.deleteAll()
            nluLogInfo(TAG, "Cleared $count examples from database")
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to clear database: ${e.message}", e)
        }
    }

    suspend fun getMigrationStatus(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val hasExamples = queries.hasExamples().executeAsOne()
            val totalCount = queries.count().executeAsOne()
            val intentCounts = queries.countPerIntent().executeAsList()
                .associate { it.intent_id to it.example_count }

            mapOf(
                "has_examples" to hasExamples,
                "total_count" to totalCount,
                "intent_counts" to intentCounts
            )
        } catch (e: Exception) {
            nluLogError(TAG, "Failed to get migration status: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}
