/**
 * AppConsentHistoryDTO.kt - Data Transfer Object for app consent history
 *
 * Tracks user consent decisions for LearnApp feature.
 * Part of LearnApp UX improvements (Phase 2).
 *
 * Date: 2025-11-28
 */

package com.augmentalis.database.dto

/**
 * Data transfer object for app consent history.
 * Tracks when users approve, decline, or skip learning for an app.
 */
data class AppConsentHistoryDTO(
    val id: Long,
    val packageName: String,
    val userChoice: String,  // APPROVED, DECLINED, DONT_ASK_AGAIN, SKIPPED
    val timestamp: Long
)

/**
 * Enum representing user consent choices.
 */
enum class ConsentChoice {
    APPROVED,       // User clicked "Learn Now" - full learning mode
    DECLINED,       // User clicked "Not Now" - ask again later
    DONT_ASK_AGAIN, // User clicked "Don't Ask Again" - never ask for this app
    SKIPPED         // User clicked "Skip" - activate just-in-time learning mode
}

/**
 * Extension to convert SQLDelight entity to DTO.
 */
fun com.augmentalis.database.App_consent_history.toDTO(): AppConsentHistoryDTO = AppConsentHistoryDTO(
    id = id,
    packageName = package_name,
    userChoice = user_choice,
    timestamp = timestamp
)
