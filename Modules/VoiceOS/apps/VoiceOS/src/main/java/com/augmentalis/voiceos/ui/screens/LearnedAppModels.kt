/**
 * LearnedAppModels.kt - Data models for learned apps display
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-30
 */

package com.augmentalis.voiceos.ui.screens

/**
 * Summary of a learned app for display in the UI.
 *
 * @property packageName App package identifier
 * @property appName Human-readable app name
 * @property screensLearned Number of screens learned for this app
 * @property commandsGenerated Number of voice commands generated
 * @property lastLearnedTimestamp When the app was last learned
 */
data class LearnedAppSummary(
    val packageName: String,
    val appName: String,
    val screensLearned: Int,
    val commandsGenerated: Int,
    val lastLearnedTimestamp: Long
)

/**
 * Detailed view of a learned app with screens and commands.
 *
 * @property summary The basic app summary
 * @property screens List of learned screens
 * @property commands List of generated commands
 */
data class LearnedAppDetail(
    val summary: LearnedAppSummary,
    val screens: List<ScreenDetail>,
    val commands: List<CommandSummary>
)

/**
 * Detail of a single learned screen.
 *
 * @property screenHash Unique screen identifier
 * @property activityName Android activity name (if available)
 * @property elementCount Number of UI elements on this screen
 * @property commandCount Number of commands generated for this screen
 */
data class ScreenDetail(
    val screenHash: String,
    val activityName: String?,
    val elementCount: Int,
    val commandCount: Int
)

/**
 * Summary of a generated command.
 *
 * @property commandPhrase The voice command phrase
 * @property targetElementHash Hash of the target UI element
 * @property action The action type (e.g., "click", "scroll")
 * @property usageCount How many times this command has been used
 */
data class CommandSummary(
    val commandPhrase: String,
    val targetElementHash: String,
    val action: String,
    val usageCount: Int = 0
)
