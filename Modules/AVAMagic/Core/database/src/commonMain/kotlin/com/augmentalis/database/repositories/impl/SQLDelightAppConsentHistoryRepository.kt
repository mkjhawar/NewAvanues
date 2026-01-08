/**
 * SQLDelightAppConsentHistoryRepository.kt - SQLDelight implementation of consent history repository
 *
 * Tracks user consent decisions for LearnApp feature.
 * Part of LearnApp UX improvements (Phase 2).
 *
 * Date: 2025-11-28
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.AppConsentHistoryDTO
import com.augmentalis.database.dto.toDTO
import com.augmentalis.database.repositories.IAppConsentHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IAppConsentHistoryRepository.
 *
 * Uses SQLDelight queries defined in AppConsentHistory.sq
 */
class SQLDelightAppConsentHistoryRepository(
    private val database: VoiceOSDatabase
) : IAppConsentHistoryRepository {

    private val queries = database.appConsentHistoryQueries

    override suspend fun insert(packageName: String, userChoice: String, timestamp: Long): Long =
        withContext(Dispatchers.Default) {
            queries.insertConsentRecord(
                package_name = packageName,
                user_choice = userChoice,
                timestamp = timestamp
            )
            queries.transactionWithResult {
                queries.count().executeAsOne()
            }
        }

    override suspend fun getConsentHistory(packageName: String): List<AppConsentHistoryDTO> =
        withContext(Dispatchers.Default) {
            queries.getConsentHistory(packageName).executeAsList().map { it.toDTO() }
        }

    override suspend fun getLatestConsent(packageName: String): AppConsentHistoryDTO? =
        withContext(Dispatchers.Default) {
            queries.getLatestConsent(packageName).executeAsOneOrNull()?.toDTO()
        }

    override suspend fun getDontAskAgainApps(): List<String> =
        withContext(Dispatchers.Default) {
            queries.getDontAskAgainApps().executeAsList()
        }

    override suspend fun getSkippedApps(): List<String> =
        withContext(Dispatchers.Default) {
            queries.getSkippedApps().executeAsList()
        }

    override suspend fun hasDontAskAgain(packageName: String): Boolean =
        withContext(Dispatchers.Default) {
            queries.hasDontAskAgain(packageName, packageName).executeAsOne() > 0
        }

    override suspend fun deleteConsentHistory(packageName: String) =
        withContext(Dispatchers.Default) {
            queries.deleteConsentHistory(packageName)
        }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAllConsentHistory()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun getConsentStats(): Map<String, Long> =
        withContext(Dispatchers.Default) {
            queries.getConsentStats().executeAsList().associate {
                it.user_choice to it.count
            }
        }
}
