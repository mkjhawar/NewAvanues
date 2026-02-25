package com.augmentalis.nlu.repository

import com.augmentalis.nlu.model.UnifiedIntent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * JS/Web in-memory implementation of IntentRepository
 *
 * Stores intents in memory (backed by a MutableMap).
 * For persistent storage on web, this could be extended to use
 * IndexedDB via the SQLDelight web-worker-driver in the future.
 *
 * This allows all commonMain NLU code that references IntentRepository
 * to compile and run on JS without modification.
 */
class JsIntentRepository : IntentRepository {

    private val intents = mutableMapOf<String, UnifiedIntent>()
    private val intentsFlow = MutableStateFlow<List<UnifiedIntent>>(emptyList())

    private fun notifyChange() {
        intentsFlow.value = intents.values.toList()
    }

    override suspend fun getAll(): List<UnifiedIntent> = intents.values.toList()

    override fun getAllAsFlow(): Flow<List<UnifiedIntent>> = intentsFlow

    override suspend fun getById(id: String): UnifiedIntent? = intents[id]

    override suspend fun getByCategory(category: String): List<UnifiedIntent> =
        intents.values.filter { it.category == category }

    override suspend fun getBySource(source: String): List<UnifiedIntent> =
        intents.values.filter { it.source.equals(source, ignoreCase = true) }

    override suspend fun getByLocale(locale: String): List<UnifiedIntent> =
        intents.values.filter { it.locale == locale }

    override suspend fun searchByPhrase(query: String): List<UnifiedIntent> {
        val lowerQuery = query.lowercase()
        return intents.values.filter {
            it.canonicalPhrase.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun findByPattern(pattern: String): UnifiedIntent? =
        intents.values.find { intent ->
            intent.patterns.any { it.equals(pattern, ignoreCase = true) }
        }

    override suspend fun findBySynonym(synonym: String): UnifiedIntent? =
        intents.values.find { intent ->
            intent.synonyms.any { it.equals(synonym, ignoreCase = true) }
        }

    override suspend fun getWithEmbeddings(): List<UnifiedIntent> =
        intents.values.filter { it.embedding != null }

    override suspend fun save(intent: UnifiedIntent) {
        intents[intent.id] = intent
        notifyChange()
    }

    override suspend fun saveAll(intents: List<UnifiedIntent>) {
        intents.forEach { this.intents[it.id] = it }
        notifyChange()
    }

    override suspend fun delete(id: String) {
        intents.remove(id)
        notifyChange()
    }

    override suspend fun deleteBySource(source: String) {
        intents.entries.removeAll { it.value.source.equals(source, ignoreCase = true) }
        notifyChange()
    }

    override suspend fun clear() {
        intents.clear()
        notifyChange()
    }

    override suspend fun count(): Long = intents.size.toLong()

    override suspend fun countByCategory(): Map<String, Long> =
        intents.values.groupBy { it.category }.mapValues { it.value.size.toLong() }

    override suspend fun getCategories(): List<String> =
        intents.values.map { it.category }.distinct().sorted()
}

/**
 * JS/Web factory for IntentRepository
 * Returns an in-memory repository instance
 */
actual object IntentRepositoryFactory {
    private var instance: IntentRepository? = null

    actual fun create(context: Any?): IntentRepository {
        return instance ?: JsIntentRepository().also { instance = it }
    }
}
