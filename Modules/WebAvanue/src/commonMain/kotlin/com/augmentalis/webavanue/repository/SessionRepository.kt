package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.Session
import com.augmentalis.webavanue.SessionTab
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDbModel
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * Repository interface for session/tab restore operations.
 *
 * Handles all session-related data persistence including:
 * - Session saving and restoration
 * - Crash recovery
 * - Session history
 */
interface SessionRepository {
    /** Saves a browsing session with its tabs */
    suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit>

    /** Gets a specific session by ID */
    suspend fun getSession(sessionId: String): Result<Session?>

    /** Gets the most recent session */
    suspend fun getLatestSession(): Result<Session?>

    /** Gets the most recent crash recovery session */
    suspend fun getLatestCrashSession(): Result<Session?>

    /** Gets all sessions (for session history) */
    suspend fun getAllSessions(limit: Int = 20, offset: Int = 0): Result<List<Session>>

    /** Gets all tabs for a specific session */
    suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>>

    /** Gets the active tab for a session */
    suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?>

    /** Deletes a specific session and its tabs */
    suspend fun deleteSession(sessionId: String): Result<Unit>

    /** Deletes all sessions */
    suspend fun deleteAllSessions(): Result<Unit>

    /** Deletes sessions older than a specific timestamp */
    suspend fun deleteOldSessions(timestamp: Instant): Result<Unit>
}

/**
 * SQLDelight implementation of SessionRepository.
 *
 * @param database SQLDelight database instance
 */
class SessionRepositoryImpl(
    private val database: BrowserDatabase
) : SessionRepository {

    private val queries = database.browserDatabaseQueries

    override suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                queries.insertSession(
                    id = session.id,
                    timestamp = session.timestamp.toEpochMilliseconds(),
                    active_tab_id = session.activeTabId,
                    tab_count = session.tabCount.toLong(),
                    is_crash_recovery = if (session.isCrashRecovery) 1L else 0L
                )

                tabs.forEach { sessionTab ->
                    queries.insertSessionTab(sessionTab.toDbModel())
                }
            }

            Napier.d("Saved session ${session.id} with ${tabs.size} tabs", tag = TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error saving session: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getSession(sessionId: String): Result<Session?> = withContext(Dispatchers.IO) {
        try {
            val dbSession = queries.selectSessionById(sessionId).executeAsOneOrNull()
            Result.success(dbSession?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting session $sessionId: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getLatestSession(): Result<Session?> = withContext(Dispatchers.IO) {
        try {
            val dbSession = queries.selectLatestSession().executeAsOneOrNull()
            Result.success(dbSession?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting latest session: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getLatestCrashSession(): Result<Session?> = withContext(Dispatchers.IO) {
        try {
            val dbSession = queries.selectLatestCrashSession().executeAsOneOrNull()
            Result.success(dbSession?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting latest crash session: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getAllSessions(limit: Int, offset: Int): Result<List<Session>> = withContext(Dispatchers.IO) {
        try {
            val dbSessions = queries.selectAllSessions(limit.toLong(), offset.toLong()).executeAsList()
            Result.success(dbSessions.map { it.toDomainModel() })
        } catch (e: Exception) {
            Napier.e("Error getting all sessions: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>> = withContext(Dispatchers.IO) {
        try {
            val dbTabs = queries.selectSessionTabs(sessionId).executeAsList()
            Result.success(dbTabs.map { it.toDomainModel() })
        } catch (e: Exception) {
            Napier.e("Error getting session tabs for $sessionId: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?> = withContext(Dispatchers.IO) {
        try {
            val dbTabs = queries.selectSessionTabs(sessionId).executeAsList()
            val activeTab = dbTabs.firstOrNull { it.is_active != 0L }
            Result.success(activeTab?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting active session tab for $sessionId: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteSession(sessionId)
            Napier.d("Deleted session $sessionId", tag = TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting session $sessionId: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteAllSessions(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessions = queries.selectAllSessions(1000, 0).executeAsList()
            sessions.forEach { session ->
                queries.deleteSession(session.id)
            }
            Napier.d("Deleted all sessions", tag = TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting all sessions: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteOldSessions(timestamp: Instant): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteOldSessions(timestamp.toEpochMilliseconds())
            Napier.d("Deleted sessions older than $timestamp", tag = TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting old sessions: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SessionRepository"
    }
}
