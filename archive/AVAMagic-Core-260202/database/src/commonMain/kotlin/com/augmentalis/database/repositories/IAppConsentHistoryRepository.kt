/**
 * IAppConsentHistoryRepository.kt - Repository interface for app consent history
 *
 * Tracks user consent decisions for LearnApp feature.
 * Part of LearnApp UX improvements (Phase 2).
 *
 * Date: 2025-11-28
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.AppConsentHistoryDTO

/**
 * Repository interface for app consent history tracking.
 * Abstracts database implementation (SQLDelight).
 *
 * User consent choices:
 * - APPROVED: User clicked "Learn Now" - full learning mode
 * - DECLINED: User clicked "Not Now" - ask again later
 * - DONT_ASK_AGAIN: User clicked "Don't Ask Again" - never ask for this app
 * - SKIPPED: User clicked "Skip" - activate just-in-time learning mode
 */
interface IAppConsentHistoryRepository {

    /**
     * Insert a new consent record.
     * @return The ID of the inserted record.
     */
    suspend fun insert(packageName: String, userChoice: String, timestamp: Long): Long

    /**
     * Get all consent history for a specific package.
     * @return List of consent records ordered by timestamp descending.
     */
    suspend fun getConsentHistory(packageName: String): List<AppConsentHistoryDTO>

    /**
     * Get the latest consent record for a package.
     * @return Most recent consent record or null if none exists.
     */
    suspend fun getLatestConsent(packageName: String): AppConsentHistoryDTO?

    /**
     * Get all packages where user clicked "Don't Ask Again" and hasn't changed it.
     * @return List of package names with persistent "Don't Ask Again" status.
     */
    suspend fun getDontAskAgainApps(): List<String>

    /**
     * Get all packages where user clicked "Skip" (just-in-time mode).
     * @return List of package names in JIT learning mode.
     */
    suspend fun getSkippedApps(): List<String>

    /**
     * Check if package has "Don't Ask Again" consent.
     * @return True if latest consent is "Don't Ask Again".
     */
    suspend fun hasDontAskAgain(packageName: String): Boolean

    /**
     * Delete all consent history for a package.
     */
    suspend fun deleteConsentHistory(packageName: String)

    /**
     * Delete all consent history.
     */
    suspend fun deleteAll()

    /**
     * Count total consent records.
     */
    suspend fun count(): Long

    /**
     * Get consent statistics by choice.
     * @return Map of user_choice to count.
     */
    suspend fun getConsentStats(): Map<String, Long>
}
