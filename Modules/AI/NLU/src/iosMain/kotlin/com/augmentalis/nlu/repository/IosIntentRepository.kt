/**
 * IosIntentRepository - iOS implementation of IntentRepository
 *
 * Uses SQLDelight with NativeSqliteDriver for persistent storage on iOS.
 * Mirrors AndroidIntentRepository with Dispatchers.Default (IO is internal on Native).
 *
 * Created: 2026-02-12
 */

package com.augmentalis.nlu.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.augmentalis.shared.nlu.db.SharedNluDatabase
import com.augmentalis.nlu.model.UnifiedIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * iOS implementation of IntentRepository.
 */
class IosIntentRepository(
    private val database: SharedNluDatabase
) : IntentRepository {

    private val queries = database.unifiedIntentQueries

    override suspend fun getAll(): List<UnifiedIntent> = withContext(Dispatchers.Default) {
        queries.selectAll().executeAsList().map { it.toUnifiedIntent() }
    }

    override fun getAllAsFlow(): Flow<List<UnifiedIntent>> = flow {
        emit(getAll())
    }

    override suspend fun getById(id: String): UnifiedIntent? = withContext(Dispatchers.Default) {
        queries.selectById(id).executeAsOneOrNull()?.toUnifiedIntent()
    }

    override suspend fun getByCategory(category: String): List<UnifiedIntent> = withContext(Dispatchers.Default) {
        queries.selectByCategory(category).executeAsList().map { it.toUnifiedIntent() }
    }

    override suspend fun getBySource(source: String): List<UnifiedIntent> = withContext(Dispatchers.Default) {
        queries.selectBySource(source).executeAsList().map { it.toUnifiedIntent() }
    }

    override suspend fun getByLocale(locale: String): List<UnifiedIntent> = withContext(Dispatchers.Default) {
        queries.selectByLocale(locale).executeAsList().map { it.toUnifiedIntent() }
    }

    override suspend fun searchByPhrase(query: String): List<UnifiedIntent> = withContext(Dispatchers.Default) {
        queries.searchByPhrase(query).executeAsList().map { it.toUnifiedIntent() }
    }

    override suspend fun findByPattern(pattern: String): UnifiedIntent? = withContext(Dispatchers.Default) {
        queries.selectByPattern(pattern).executeAsOneOrNull()?.toUnifiedIntent()
    }

    override suspend fun findBySynonym(synonym: String): UnifiedIntent? = withContext(Dispatchers.Default) {
        queries.selectBySynonym(synonym).executeAsOneOrNull()?.toUnifiedIntent()
    }

    override suspend fun getWithEmbeddings(): List<UnifiedIntent> = withContext(Dispatchers.Default) {
        queries.selectWithEmbeddings().executeAsList().map { it.toUnifiedIntent() }
    }

    override suspend fun save(intent: UnifiedIntent) = withContext(Dispatchers.Default) {
        database.transaction {
            // Delete existing patterns and synonyms
            queries.deletePatterns(intent.id)
            queries.deleteSynonyms(intent.id)

            // Insert or update intent
            queries.insertIntent(
                id = intent.id,
                canonical_phrase = intent.canonicalPhrase,
                category = intent.category,
                action_id = intent.actionId,
                priority = intent.priority.toLong(),
                locale = intent.locale,
                source = intent.source,
                embedding = intent.embedding?.toByteArray()
            )

            // Insert patterns
            for (pattern in intent.patterns) {
                queries.insertPattern(intent.id, pattern)
            }

            // Insert synonyms
            for (synonym in intent.synonyms) {
                queries.insertSynonym(intent.id, synonym)
            }
        }
    }

    override suspend fun saveAll(intents: List<UnifiedIntent>) = withContext(Dispatchers.Default) {
        database.transaction {
            for (intent in intents) {
                queries.deletePatterns(intent.id)
                queries.deleteSynonyms(intent.id)

                queries.insertIntent(
                    id = intent.id,
                    canonical_phrase = intent.canonicalPhrase,
                    category = intent.category,
                    action_id = intent.actionId,
                    priority = intent.priority.toLong(),
                    locale = intent.locale,
                    source = intent.source,
                    embedding = intent.embedding?.toByteArray()
                )

                for (pattern in intent.patterns) {
                    queries.insertPattern(intent.id, pattern)
                }

                for (synonym in intent.synonyms) {
                    queries.insertSynonym(intent.id, synonym)
                }
            }
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteIntent(id)
    }

    override suspend fun deleteBySource(source: String) = withContext(Dispatchers.Default) {
        queries.deleteBySource(source)
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        database.transaction {
            queries.selectAll().executeAsList().forEach {
                queries.deleteIntent(it.id)
            }
        }
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.countIntents().executeAsOne()
    }

    override suspend fun countByCategory(): Map<String, Long> = withContext(Dispatchers.Default) {
        queries.countByCategory().executeAsList().associate { it.category to it.count }
    }

    override suspend fun getCategories(): List<String> = withContext(Dispatchers.Default) {
        queries.selectCategories().executeAsList()
    }

    /**
     * Extension to convert database entity to domain model
     */
    private fun com.augmentalis.shared.nlu.db.Unified_intent.toUnifiedIntent(): UnifiedIntent {
        val patterns = queries.selectPatterns(id).executeAsList()
        val synonyms = queries.selectSynonyms(id).executeAsList()

        return UnifiedIntent(
            id = id,
            canonicalPhrase = canonical_phrase,
            patterns = patterns,
            synonyms = synonyms,
            embedding = embedding?.toFloatArray(),
            category = category,
            actionId = action_id,
            priority = priority.toInt(),
            locale = locale,
            source = source
        )
    }

    /**
     * Extension to convert SelectWithEmbeddings result to domain model
     */
    private fun com.augmentalis.shared.nlu.db.SelectWithEmbeddings.toUnifiedIntent(): UnifiedIntent {
        val patterns = queries.selectPatterns(id).executeAsList()
        val synonyms = queries.selectSynonyms(id).executeAsList()

        return UnifiedIntent(
            id = id,
            canonicalPhrase = canonical_phrase,
            patterns = patterns,
            synonyms = synonyms,
            embedding = embedding.toFloatArray(),  // Non-null in this query
            category = category,
            actionId = action_id,
            priority = priority.toInt(),
            locale = locale,
            source = source
        )
    }

    /**
     * Convert FloatArray to ByteArray for storage
     */
    private fun FloatArray.toByteArray(): ByteArray {
        val bytes = ByteArray(size * 4)
        for (i in indices) {
            val bits = this[i].toRawBits()
            bytes[i * 4] = (bits and 0xFF).toByte()
            bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
            bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
            bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Convert ByteArray to FloatArray for retrieval
     */
    private fun ByteArray.toFloatArray(): FloatArray {
        val floats = FloatArray(size / 4)
        for (i in floats.indices) {
            val offset = i * 4
            val bits = (this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8) or
                    ((this[offset + 2].toInt() and 0xFF) shl 16) or
                    ((this[offset + 3].toInt() and 0xFF) shl 24)
            floats[i] = Float.fromBits(bits)
        }
        return floats
    }
}

/**
 * Factory implementation for iOS
 */
actual object IntentRepositoryFactory {
    actual fun create(context: Any?): IntentRepository {
        val driver = NativeSqliteDriver(
            schema = SharedNluDatabase.Schema,
            name = "shared_nlu.db"
        )
        val database = SharedNluDatabase(driver)
        return IosIntentRepository(database)
    }
}
