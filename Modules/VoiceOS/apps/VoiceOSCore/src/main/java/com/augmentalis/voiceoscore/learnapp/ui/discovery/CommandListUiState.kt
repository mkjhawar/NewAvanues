/**
 * CommandListUiState.kt - UI state models for command list with version info
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.1: Version info display in command list UI
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

/**
 * UI state for command list screen with version information
 */
data class CommandListUiState(
    val appInfo: AppVersionInfo? = null,
    val commandGroups: List<CommandGroupUiModel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * App version information for header display
 */
data class AppVersionInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,      // e.g., "8.2024.11.123"
    val versionCode: Long,        // e.g., 82024
    val lastUpdated: Long,        // Timestamp
    val totalCommands: Int,
    val deprecatedCommands: Int
) {
    /**
     * Get human-readable update time text
     */
    fun getUpdateTimeText(): String {
        val now = System.currentTimeMillis()
        val daysAgo = ((now - lastUpdated) / 86400000L).toInt()
        return when {
            daysAgo == 0 -> "Updated today"
            daysAgo == 1 -> "Updated yesterday"
            daysAgo < 7 -> "Updated $daysAgo days ago"
            daysAgo < 30 -> "Updated ${daysAgo / 7} weeks ago"
            else -> "Updated ${daysAgo / 30} months ago"
        }
    }

    /**
     * Get abbreviated version for badges (e.g., "8.2024")
     */
    fun getAbbreviatedVersion(): String {
        return versionName.split(".").take(2).joinToString(".")
    }

    /**
     * Get version display string (e.g., "v8.2024.11.123 (82024)")
     */
    fun getVersionDisplayString(): String {
        return "v$versionName ($versionCode)"
    }
}

/**
 * Command group with UI-specific data
 */
data class CommandGroupUiModel(
    val screenName: String,
    val commands: List<CommandUiModel>,
    val isExpanded: Boolean = true
)

/**
 * Individual command with version metadata and deprecation info
 */
data class CommandUiModel(
    val id: Long,
    val commandText: String,
    val actionType: String,
    val confidence: Double,
    val versionName: String,
    val isDeprecated: Boolean,
    val daysUntilDeletion: Int?,    // Null if not deprecated
    val isUserApproved: Boolean,
    val elementHash: String? = null,
    val screenHash: String? = null
) {
    /**
     * Get version badge text (e.g., "v8.2024")
     */
    fun getVersionBadgeText(): String {
        return "v${versionName.split(".").take(2).joinToString(".")}"
    }

    /**
     * Get deletion warning text for deprecated commands
     */
    fun getDeleteWarningText(): String? {
        return daysUntilDeletion?.let { days ->
            when {
                days <= 0 -> "⚠️ Deleting soon"
                days <= 7 -> "⚠️ Deletes in $days days"
                days <= 30 -> "Deletes in $days days"
                else -> null  // Don't show if >30 days
            }
        }
    }

    /**
     * Get confidence percentage as integer
     */
    fun getConfidencePercent(): Int {
        return (confidence * 100).toInt()
    }

    /**
     * Determine confidence color category
     */
    fun getConfidenceCategory(): ConfidenceCategory {
        return when {
            confidence >= 0.85 -> ConfidenceCategory.HIGH
            confidence >= 0.60 -> ConfidenceCategory.MEDIUM
            else -> ConfidenceCategory.LOW
        }
    }
}

/**
 * Confidence level categories for color coding
 */
enum class ConfidenceCategory {
    HIGH,    // >= 85% - Green
    MEDIUM,  // 60-84% - Yellow
    LOW      // < 60% - Red
}
