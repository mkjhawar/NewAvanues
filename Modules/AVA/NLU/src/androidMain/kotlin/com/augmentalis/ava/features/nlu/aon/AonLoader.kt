package com.augmentalis.ava.features.nlu.aon

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.ava.features.nlu.IntentClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified loader for AVA 2.0 .aot ontology files
 *
 * Coordinates the complete pipeline:
 * 1. Parse .aot files from assets
 * 2. Insert ontologies into semantic_intent_ontology table
 * 3. Compute mALBERT embeddings (768-dim, L2-normalized)
 * 4. Insert embeddings into intent_embeddings table
 *
 * This is the main entry point for .aot file processing.
 * Call loadAllOntologies() at app startup to initialize the NLU system.
 *
 * AVA 2.0 .aot Integration:
 * - Native .aot format parsing
 * - Semantic intent understanding via descriptions
 * - Pre-computed embeddings for fast runtime classification
 * - Multilingual support (100+ languages via mALBERT)
 * - Persistent database storage
 *
 * Example usage:
 * ```kotlin
 * val loader = AonLoader(context, intentClassifier)
 * val result = loader.loadAllOntologies()
 * when (result) {
 *     is Result.Success -> {
 *         val stats = result.data
 *         Log.i(TAG, "Loaded ${stats.totalIntents} intents")
 *     }
 *     is Result.Error -> Log.e(TAG, "Failed: ${result.message}")
 * }
 * ```
 */
class AonLoader(
    private val context: Context,
    private val intentClassifier: IntentClassifier
) {

    companion object {
        private const val TAG = "AonLoader"

        /**
         * Default directory for .aot files in assets
         */
        private const val DEFAULT_ONTOLOGY_DIR = "ontology"

        /**
         * Supported locales (extend as needed)
         */
        private val SUPPORTED_LOCALES = listOf(
            "en-US",
            "es-ES",
            "fr-FR",
            "de-DE",
            "ja-JP",
            "zh-CN"
        )
    }

    private val parser = AonFileParser(context)
    private val embeddingComputer = AonEmbeddingComputer(context, intentClassifier)

    /**
     * Load all .aot ontology files for all supported locales
     *
     * This is the main entry point. Call this at app startup.
     *
     * Process:
     * 1. For each supported locale:
     *    a. Find all .aot files in assets/ontology/{locale}/
     *    b. Parse each .aot file
     *    c. Insert ontologies into database
     *    d. Compute embeddings
     *    e. Insert embeddings into database
     * 2. Return loading statistics
     *
     * @param forceReload If true, delete existing data and reload (default: false)
     * @return Loading statistics
     */
    suspend fun loadAllOntologies(forceReload: Boolean = false): Result<LoadingStats> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i(TAG, "======================================")
            android.util.Log.i(TAG, "  AVA 2.0 Ontology Loader")
            android.util.Log.i(TAG, "======================================")

            val database = DatabaseDriverFactory(context).createDriver().createDatabase()
            val ontologyQueries = database.semanticIntentOntologyQueries
            val embeddingQueries = database.intentEmbeddingQueries

            val stats = LoadingStats()

            // Check if already loaded
            val hasOntologies = ontologyQueries.count().executeAsOne() > 0
            if (!forceReload && hasOntologies) {
                val existingCount = ontologyQueries.count().executeAsOne().toInt()
                android.util.Log.i(TAG, "Ontologies already loaded ($existingCount entries)")
                android.util.Log.i(TAG, "Use forceReload=true to reload")

                return@withContext Result.Success(
                    LoadingStats(
                        totalIntents = existingCount,
                        skipped = existingCount
                    )
                )
            }

            // Force reload: clear existing data
            if (forceReload) {
                android.util.Log.w(TAG, "Force reload: clearing existing data...")
                ontologyQueries.deleteAll()
                embeddingQueries.deleteAll()
            }

            // Load for each supported locale
            for (locale in SUPPORTED_LOCALES) {
                android.util.Log.i(TAG, "\n--- Processing locale: $locale ---")

                val localeDir = "$DEFAULT_ONTOLOGY_DIR/$locale"
                val localeStats = loadOntologiesForLocale(localeDir, locale)

                stats.merge(localeStats)
            }

            android.util.Log.i(TAG, "\n======================================")
            android.util.Log.i(TAG, "  Loading Complete")
            android.util.Log.i(TAG, "======================================")
            android.util.Log.i(TAG, "Total intents:      ${stats.totalIntents}")
            android.util.Log.i(TAG, "Files processed:    ${stats.filesProcessed}")
            android.util.Log.i(TAG, "Embeddings created: ${stats.embeddingsCreated}")
            android.util.Log.i(TAG, "Failures:           ${stats.failures}")
            android.util.Log.i(TAG, "Duration:           ${stats.duration}ms")
            android.util.Log.i(TAG, "======================================")

            Result.Success(stats)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load ontologies", e)
            Result.Error(
                exception = e,
                message = "Ontology loading failed: ${e.message}"
            )
        }
    }

    /**
     * Load ontologies for a specific locale
     *
     * @param assetDirectory Directory in assets (e.g., "ontology/en-US")
     * @param locale Locale code (e.g., "en-US")
     * @return Loading statistics for this locale
     */
    private suspend fun loadOntologiesForLocale(
        assetDirectory: String,
        locale: String
    ): LoadingStats = withContext(Dispatchers.IO) {
        val stats = LoadingStats()
        val startTime = System.currentTimeMillis()

        try {
            // Parse all .aot files in directory
            val parseResult = parser.loadAllAonFiles(assetDirectory)

            when (parseResult) {
                is Result.Success -> {
                    val aonFiles = parseResult.data

                    if (aonFiles.isEmpty()) {
                        android.util.Log.w(TAG, "  No .aot files found in $assetDirectory")
                        return@withContext stats
                    }

                    android.util.Log.i(TAG, "  Found ${aonFiles.size} .aot files")

                    // Process each .aot file
                    for (aonFile in aonFiles) {
                        val fileStats = processAonFile(aonFile)
                        stats.merge(fileStats)
                    }
                }

                is Result.Error -> {
                    android.util.Log.w(TAG, "  Failed to load .aot files: ${parseResult.message}")
                    stats.failures++
                }
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "  Error loading locale $locale", e)
            stats.failures++
        }

        stats.duration = System.currentTimeMillis() - startTime
        return@withContext stats
    }

    /**
     * Process a single .aot file
     *
     * Pipeline:
     * 1. Extract ontologies from parsed .aot file
     * 2. Insert ontologies into database
     * 3. Compute embeddings for each ontology
     * 4. Insert embeddings into database
     *
     * @param aonFile Parsed .aot file
     * @return Processing statistics
     */
    private suspend fun processAonFile(aonFile: AonFile): LoadingStats = withContext(Dispatchers.IO) {
        val stats = LoadingStats()

        try {
            android.util.Log.i(TAG, "  Processing: ${aonFile.metadata.filename}")
            android.util.Log.i(TAG, "    Category: ${aonFile.metadata.category}")
            android.util.Log.i(TAG, "    Intents:  ${aonFile.ontologies.size}")

            val database = DatabaseDriverFactory(context).createDriver().createDatabase()
            val ontologyQueries = database.semanticIntentOntologyQueries
            val embeddingQueries = database.intentEmbeddingQueries

            // Step 1: Insert ontologies into database using SQLDelight
            var insertedCount = 0
            for (ontology in aonFile.ontologies) {
                ontologyQueries.insert(
                    intent_id = ontology.intentId,
                    locale = ontology.locale,
                    canonical_form = ontology.canonicalForm,
                    description = ontology.description,
                    synonyms = ontology.synonyms.joinToString(","),
                    action_type = ontology.actionType,
                    action_sequence = ontology.actionSequence.joinToString(","),
                    required_capabilities = ontology.requiredCapabilities.joinToString(","),
                    ontology_file_source = ontology.ontologyFileSource,
                    created_at = System.currentTimeMillis(),
                    updated_at = System.currentTimeMillis()
                )
                insertedCount++
            }
            android.util.Log.d(TAG, "    ✓ Inserted $insertedCount ontologies")

            stats.totalIntents += insertedCount
            stats.filesProcessed++

            // Step 2: Compute embeddings for all ontologies
            val embeddingResult = embeddingComputer.computeEmbeddingsFromAonFile(aonFile)

            when (embeddingResult) {
                is Result.Success -> {
                    val embeddings = embeddingResult.data

                    // Step 3: Verify embedding quality
                    val validEmbeddings = embeddings.filter { embedding ->
                        embeddingComputer.verifyEmbeddingQuality(embedding)
                    }

                    if (validEmbeddings.size < embeddings.size) {
                        android.util.Log.w(TAG, "    ⚠ ${embeddings.size - validEmbeddings.size} embeddings failed quality check")
                    }

                    // Step 4: Insert embeddings into database using SQLDelight
                    var embeddingInsertCount = 0
                    val currentTime = System.currentTimeMillis()
                    for (embedding in validEmbeddings) {
                        embeddingQueries.insert(
                            intent_id = embedding.intentId,
                            locale = embedding.locale,
                            embedding_vector = embedding.embeddingVector,
                            embedding_dimension = embedding.embeddingDimension.toLong(),
                            model_version = embedding.modelVersion,
                            normalization_type = embedding.normalizationType,
                            ontology_id = embedding.ontologyId,
                            created_at = currentTime,
                            updated_at = currentTime,
                            example_count = embedding.exampleCount.toLong(),
                            source = embedding.source
                        )
                        embeddingInsertCount++
                    }
                    android.util.Log.d(TAG, "    ✓ Inserted $embeddingInsertCount embeddings")

                    stats.embeddingsCreated += embeddingInsertCount
                }

                is Result.Error -> {
                    android.util.Log.w(TAG, "    ✗ Failed to compute embeddings: ${embeddingResult.message}")
                    stats.failures++
                }
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "  Failed to process ${aonFile.metadata.filename}", e)
            stats.failures++
        }

        return@withContext stats
    }

    /**
     * Load ontologies for a specific locale only
     *
     * @param locale Locale code (e.g., "en-US")
     * @param forceReload If true, delete existing data for this locale
     * @return Loading statistics
     */
    suspend fun loadOntologiesForLocale(
        locale: String,
        forceReload: Boolean = false
    ): Result<LoadingStats> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i(TAG, "Loading ontologies for locale: $locale")

            val database = DatabaseDriverFactory(context).createDriver().createDatabase()
            val ontologyQueries = database.semanticIntentOntologyQueries
            val embeddingQueries = database.intentEmbeddingQueries

            // Force reload: clear existing data for locale
            if (forceReload) {
                android.util.Log.w(TAG, "Force reload: clearing data for $locale...")
                ontologyQueries.deleteByLocale(locale)
                embeddingQueries.deleteByLocale(locale)
            }

            // Load ontologies
            val localeDir = "$DEFAULT_ONTOLOGY_DIR/$locale"
            val stats = loadOntologiesForLocale(localeDir, locale)

            Result.Success(stats)

        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to load ontologies for $locale: ${e.message}"
            )
        }
    }

    /**
     * Get loading statistics from database
     *
     * @return Current state of loaded ontologies and embeddings
     */
    suspend fun getLoadingStatus(): Result<LoadingStatus> = withContext(Dispatchers.IO) {
        try {
            val database = DatabaseDriverFactory(context).createDriver().createDatabase()
            val ontologyQueries = database.semanticIntentOntologyQueries
            val embeddingQueries = database.intentEmbeddingQueries

            val totalOntologies = ontologyQueries.count().executeAsOne().toInt()
            val totalEmbeddings = embeddingQueries.count().executeAsOne().toInt()

            // Get distinct locales from ontologies
            val allOntologies = ontologyQueries.selectAll().executeAsList()
            val loadedLocales = allOntologies.map { it.locale }.distinct()

            // Build stats by locale
            val ontologyStats = loadedLocales.map { locale ->
                val count = ontologyQueries.countByLocale(locale).executeAsOne().toInt()
                OntologyStats(locale = locale, count = count)
            }

            val embeddingStats = loadedLocales.map { locale ->
                val count = embeddingQueries.countByLocale(locale).executeAsOne().toInt()
                EmbeddingStats(locale = locale, count = count)
            }

            val status = LoadingStatus(
                totalOntologies = totalOntologies,
                totalEmbeddings = totalEmbeddings,
                loadedLocales = loadedLocales,
                ontologyStats = ontologyStats,
                embeddingStats = embeddingStats
            )

            Result.Success(status)

        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to get loading status: ${e.message}"
            )
        }
    }
}

/**
 * Statistics from loading .aot files
 */
data class LoadingStats(
    var totalIntents: Int = 0,
    var filesProcessed: Int = 0,
    var embeddingsCreated: Int = 0,
    var failures: Int = 0,
    var skipped: Int = 0,
    var duration: Long = 0
) {
    /**
     * Merge another stats object into this one
     */
    fun merge(other: LoadingStats) {
        totalIntents += other.totalIntents
        filesProcessed += other.filesProcessed
        embeddingsCreated += other.embeddingsCreated
        failures += other.failures
        skipped += other.skipped
        duration += other.duration
    }
}

/**
 * Current loading status from database
 */
data class LoadingStatus(
    val totalOntologies: Int,
    val totalEmbeddings: Int,
    val loadedLocales: List<String>,
    val ontologyStats: List<OntologyStats>,
    val embeddingStats: List<EmbeddingStats>
)

/**
 * Statistics for ontologies by locale
 */
data class OntologyStats(
    val locale: String,
    val count: Int
)

/**
 * Statistics for embeddings by locale
 */
data class EmbeddingStats(
    val locale: String,
    val count: Int
)
