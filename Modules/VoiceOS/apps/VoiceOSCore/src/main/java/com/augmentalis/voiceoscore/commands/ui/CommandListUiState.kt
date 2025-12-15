/**
 * CommandListUiState.kt - UI state models for command list screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-15
 *
 * P3 Task 2.1: Version-aware command list UI with deprecation indicators
 */

package com.augmentalis.voiceoscore.commands.ui

/**
 * UI state for command list screen.
 *
 * Encapsulates loading, data, and error states for the command list display.
 * Follows unidirectional data flow pattern with immutable state.
 *
 * @property appInfo Overall app version information (null if not loaded)
 * @property commandGroups Grouped commands by app package
 * @property isLoading Whether data is currently being loaded
 * @property error Error message if loading failed (null if no error)
 */
data class CommandListUiState(
    val appInfo: AppVersionInfo? = null,
    val commandGroups: List<CommandGroupUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * App version information for display.
 *
 * Aggregates version metadata and command statistics for an app.
 *
 * @property packageName App package identifier (e.g., "com.google.android.gm")
 * @property appName Human-readable app name (e.g., "Gmail")
 * @property versionName Version string (e.g., "8.2024.11.123")
 * @property versionCode Version code (e.g., 800241123)
 * @property lastUpdated Timestamp of last app update (epoch millis)
 * @property totalCommands Total number of commands for this app
 * @property deprecatedCommands Number of deprecated commands
 */
data class AppVersionInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val lastUpdated: Long,
    val totalCommands: Int,
    val deprecatedCommands: Int
) {
    /**
     * Percentage of commands that are deprecated.
     * Returns 0.0 if totalCommands is 0.
     */
    val deprecationRate: Float
        get() = if (totalCommands > 0) {
            (deprecatedCommands.toFloat() / totalCommands) * 100
        } else 0f
}

/**
 * Grouped commands for a single app.
 *
 * Groups commands by package name for hierarchical display.
 *
 * @property packageName App package identifier
 * @property appName Human-readable app name
 * @property commands List of commands for this app
 */
data class CommandGroupUiModel(
    val packageName: String,
    val appName: String,
    val commands: List<CommandUiModel>
)

/**
 * UI model for a single voice command.
 *
 * Enriches database command with UI-specific computed properties
 * (deprecation countdown, confidence percentage, etc.)
 *
 * @property id Command database ID
 * @property commandText The voice command phrase (e.g., "compose new email")
 * @property confidence Confidence score (0.0-1.0)
 * @property versionName App version when command was generated
 * @property versionCode App version code when command was generated
 * @property isDeprecated Whether command is marked as deprecated
 * @property isUserApproved Whether user explicitly approved this command
 * @property usageCount Number of times command has been used
 * @property lastUsed Timestamp of last usage (null if never used)
 * @property daysUntilDeletion Days remaining before cleanup (null if not deprecated)
 */
data class CommandUiModel(
    val id: Long,
    val commandText: String,
    val confidence: Double,
    val versionName: String,
    val versionCode: Long,
    val isDeprecated: Boolean,
    val isUserApproved: Boolean,
    val usageCount: Long,
    val lastUsed: Long?,
    val daysUntilDeletion: Int?  // Null if not deprecated
) {
    /**
     * Confidence as percentage (0-100).
     */
    val confidencePercentage: Int get() = (confidence * 100).toInt()

    /**
     * Check if command will be deleted soon (< 7 days).
     */
    val isDeletionImminent: Boolean
        get() = daysUntilDeletion != null && daysUntilDeletion < 7
}
