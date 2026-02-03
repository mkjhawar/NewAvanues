package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.HistoryEntry
import com.augmentalis.webavanue.HistorySession
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDbModel
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Repository interface for browsing history operations.
 *
 * Handles all history-related data persistence including:
 * - History entry CRUD operations
 * - Date range queries
 * - Search functionality
 * - Most visited sites
 * - Reactive observation via Flow
 */
interface HistoryRepository {
    /** Adds a history entry (skips for incognito) */
    suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry>

    /** Gets history entries with pagination */
    suspend fun getHistory(limit: Int = 100, offset: Int = 0): Result<List<HistoryEntry>>

    /** Gets history for a specific date range */
    suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>>

    /** Searches history */
    suspend fun searchHistory(query: String, limit: Int = 50): Result<List<HistoryEntry>>

    /** Gets most visited sites */
    suspend fun getMostVisited(limit: Int = 10): Result<List<HistoryEntry>>

    /** Observes history with real-time updates */
    fun observeHistory(): Flow<List<HistoryEntry>>

    /** Deletes a specific history entry */
    suspend fun deleteHistoryEntry(entryId: String): Result<Unit>

    /** Deletes history for a URL */
    suspend fun deleteHistoryForUrl(url: String): Result<Unit>

    /** Clears history for a date range */
    suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit>

    /** Clears all history */
    suspend fun clearAllHistory(): Result<Unit>

    /** Gets history sessions */
    suspend fun getHistorySessions(limit: Int = 10): Result<List<HistorySession>>

    /** Refreshes in-memory state from database */
    suspend fun refresh()

    /** Updates in-memory state directly (for fast startup) */
    suspend fun updateState(history: List<HistoryEntry>)
}

/**
 * SQLDelight implementation of HistoryRepository.
 *
 * @param database SQLDelight database instance
 */
class HistoryRepositoryImpl(
    private val database: BrowserDatabase
) : HistoryRepository {

    private val queries = database.browserDatabaseQueries
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry> = withContext(Dispatchers.IO) {
        try {
            // PRIVATE BROWSING: Skip history for incognito/private tabs
            if (entry.isIncognito) {
                Napier.d("Skipping history entry for private browsing: ${entry.url}", tag = TAG)
                return@withContext Result.success(entry)
            }

            queries.insertHistoryEntry(entry.toDbModel())
            refresh()
            Result.success(entry)
        } catch (e: Exception) {
            Napier.e("Error adding history entry: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getHistory(limit: Int, offset: Int): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val history = queries.selectAllHistory(limit.toLong(), offset.toLong()).executeAsList().map { it.toDomainModel() }
            Result.success(history)
        } catch (e: Exception) {
            Napier.e("Error getting history: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val history = queries.selectHistoryByDateRange(
                startDate.toEpochMilliseconds(),
                endDate.toEpochMilliseconds()
            ).executeAsList().map { it.toDomainModel() }
            Result.success(history)
        } catch (e: Exception) {
            Napier.e("Error getting history by date range: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun searchHistory(query: String, limit: Int): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val history = queries.searchHistory(query, query, query, limit.toLong()).executeAsList().map { it.toDomainModel() }
            Result.success(history)
        } catch (e: Exception) {
            Napier.e("Error searching history: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getMostVisited(limit: Int): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val results = queries.selectMostVisited(limit.toLong()).executeAsList()
            val history = results.map { result ->
                HistoryEntry(
                    id = "",
                    url = result.url,
                    title = result.title,
                    favicon = result.favicon,
                    visitedAt = Clock.System.now(),
                    visitCount = result.max_visits?.toInt() ?: 0,
                    visitDuration = 0,
                    referrer = null,
                    searchTerms = null,
                    isIncognito = false,
                    deviceId = null
                )
            }
            Result.success(history)
        } catch (e: Exception) {
            Napier.e("Error getting most visited: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override fun observeHistory(): Flow<List<HistoryEntry>> = _history.asStateFlow()

    override suspend fun deleteHistoryEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteHistoryEntry(entryId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting history entry: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteHistoryForUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteHistoryByUrl(url)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting history for URL: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                queries.deleteHistoryByDateRange(
                    startDate.toEpochMilliseconds(),
                    endDate.toEpochMilliseconds()
                )
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error clearing history by date range: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun clearAllHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                queries.deleteAllHistory()
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error clearing all history: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getHistorySessions(limit: Int): Result<List<HistorySession>> = withContext(Dispatchers.IO) {
        // TODO: Implement history sessions grouping
        Result.success(emptyList())
    }

    override suspend fun refresh() {
        try {
            _history.value = queries.selectAllHistory(100, 0).executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error refreshing history: ${e.message}", e, tag = TAG)
        }
    }

    override suspend fun updateState(history: List<HistoryEntry>) {
        _history.value = history
    }

    companion object {
        private const val TAG = "HistoryRepository"
    }
}
