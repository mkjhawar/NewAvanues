/**
 * HomeViewModel.kt - ViewModel for the Home screen
 *
 * Manages the UI state for the Home screen, providing statistics about
 * learned apps, available commands, and command usage. Uses VoiceOSDatabaseManager
 * to fetch statistics and exposes them through a reactive StateFlow.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.viewmodel

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceos.ui.screens.CommandSummary
import com.augmentalis.voiceos.ui.screens.LearnedAppDetail
import com.augmentalis.voiceos.ui.screens.LearnedAppSummary
import com.augmentalis.voiceos.ui.screens.ScreenDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Home screen.
 *
 * @property appsLearned Number of apps that have been learned by the system
 * @property commandsAvailable Total number of commands available across all apps
 * @property commandsToday Number of commands used today
 * @property isLoading Whether statistics are currently being loaded
 * @property error Error message if loading failed, null otherwise
 * @property learnedApps List of learned app summaries for clickable display
 * @property selectedAppDetail Detailed view of a selected app (for bottom sheet)
 * @property showAppDetailSheet Whether to show the app detail bottom sheet
 * @property isLoadingAppDetail Whether app detail is currently loading
 */
data class HomeUiState(
    val appsLearned: Long = 0,
    val commandsAvailable: Long = 0,
    val commandsToday: Long = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val learnedApps: List<LearnedAppSummary> = emptyList(),
    val selectedAppDetail: LearnedAppDetail? = null,
    val showAppDetailSheet: Boolean = false,
    val isLoadingAppDetail: Boolean = false
)

/**
 * ViewModel for the Home screen.
 *
 * Provides statistics about the VoiceOS system including learned apps,
 * available commands, and command usage. Automatically loads statistics
 * when created and provides a refresh function for manual updates.
 *
 * Example usage:
 * ```kotlin
 * val viewModel: HomeViewModel by viewModel()
 * val uiState by viewModel.uiState.collectAsState()
 *
 * when {
 *     uiState.isLoading -> CircularProgressIndicator()
 *     uiState.error != null -> ErrorMessage(uiState.error!!)
 *     else -> StatsDisplay(uiState)
 * }
 * ```
 *
 * @property databaseManager The database manager for accessing VoiceOS data
 * @property packageManager PackageManager for resolving app names
 */
class HomeViewModel(
    private val databaseManager: VoiceOSDatabaseManager,
    private val packageManager: PackageManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /**
     * Current UI state for the Home screen.
     *
     * Emits updates whenever statistics are loaded or an error occurs.
     * Subscribe to this flow to receive reactive UI updates.
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Load statistics when ViewModel is created
        loadStats()
        // Load learned apps for clickable display
        loadLearnedApps()
    }

    /**
     * Loads statistics from the database.
     *
     * Updates the UI state with:
     * - Number of learned apps (from scrapedAppCount)
     * - Number of available commands (from generatedCommandCount)
     * - Number of commands used today (currently 0, will be implemented with usage tracking)
     *
     * Sets isLoading to true while loading and false when complete.
     * Sets error message if loading fails.
     */
    fun loadStats() {
        viewModelScope.launch {
            // Set loading state
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Fetch statistics from database
                val stats = databaseManager.getStats()

                // Update UI state with loaded statistics
                _uiState.update {
                    it.copy(
                        appsLearned = stats.scrapedAppCount,
                        commandsAvailable = stats.generatedCommandCount,
                        commandsToday = 0, // TODO: Implement command usage tracking
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                // Update UI state with error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load statistics: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Loads the list of learned apps for display in the UI.
     *
     * Fetches all scraped apps from the database and converts them
     * to LearnedAppSummary objects with resolved app names.
     */
    fun loadLearnedApps() {
        viewModelScope.launch {
            try {
                val scrapedApps = databaseManager.scrapedApps.getAll()

                val learnedApps = scrapedApps.map { app ->
                    // Get screen count for this app
                    val screenCount = databaseManager.screenContexts.countByApp(app.appId)

                    // Resolve app name from package manager or use package name
                    val appName = resolveAppName(app.packageName)

                    LearnedAppSummary(
                        packageName = app.packageName,
                        appName = appName,
                        screensLearned = screenCount.toInt(),
                        commandsGenerated = app.commandCount.toInt(),
                        lastLearnedTimestamp = app.lastScrapedAt
                    )
                }.sortedByDescending { it.lastLearnedTimestamp }

                _uiState.update { it.copy(learnedApps = learnedApps) }
            } catch (e: Exception) {
                // Silently fail - learned apps list is optional enhancement
                _uiState.update { it.copy(learnedApps = emptyList()) }
            }
        }
    }

    /**
     * Handles click on a learned app card.
     *
     * Loads detailed information about the app including screens and commands,
     * then shows the detail bottom sheet.
     *
     * @param packageName The package name of the clicked app
     */
    fun onAppClicked(packageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAppDetail = true, showAppDetailSheet = true) }

            try {
                // Find the app summary
                val summary = _uiState.value.learnedApps.find { it.packageName == packageName }
                    ?: return@launch

                // Load screens for this app
                val screenContexts = databaseManager.screenContexts.getByPackage(packageName)
                val screens = screenContexts.map { screen ->
                    // Count commands for this screen (commands reference elements on screens)
                    val screenCommands = databaseManager.generatedCommands.getByPackage(packageName)
                        .filter { cmd ->
                            // Match commands to screens via element hash prefix or direct association
                            cmd.elementHash.startsWith(screen.screenHash.take(8))
                        }

                    ScreenDetail(
                        screenHash = screen.screenHash,
                        activityName = screen.activityName,
                        elementCount = screen.elementCount.toInt(),
                        commandCount = screenCommands.size
                    )
                }

                // Load commands for this app
                val generatedCommands = databaseManager.generatedCommands.getByPackage(packageName)
                val commands = generatedCommands.map { cmd ->
                    CommandSummary(
                        commandPhrase = cmd.commandText,
                        targetElementHash = cmd.elementHash,
                        action = cmd.actionType,
                        usageCount = cmd.usageCount.toInt()
                    )
                }.sortedByDescending { it.usageCount }

                val detail = LearnedAppDetail(
                    summary = summary,
                    screens = screens,
                    commands = commands
                )

                _uiState.update {
                    it.copy(
                        selectedAppDetail = detail,
                        isLoadingAppDetail = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAppDetail = false,
                        showAppDetailSheet = false,
                        error = "Failed to load app details: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Dismisses the app detail bottom sheet.
     */
    fun dismissAppDetail() {
        _uiState.update {
            it.copy(
                showAppDetailSheet = false,
                selectedAppDetail = null
            )
        }
    }

    /**
     * Resolves an app name from a package name using PackageManager.
     * Falls back to a formatted package name if resolution fails.
     *
     * @param packageName The app's package name
     * @return Human-readable app name
     */
    private fun resolveAppName(packageName: String): String {
        return try {
            packageManager?.let { pm ->
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } ?: formatPackageName(packageName)
        } catch (e: Exception) {
            formatPackageName(packageName)
        }
    }

    /**
     * Formats a package name into a human-readable name.
     * Example: "com.google.android.gm" -> "Gm"
     *
     * @param packageName The package name to format
     * @return Formatted name
     */
    private fun formatPackageName(packageName: String): String {
        return packageName
            .substringAfterLast(".")
            .replaceFirstChar { it.uppercase() }
    }

    /**
     * Refreshes statistics by reloading from the database.
     *
     * Use this function to manually update statistics, for example
     * in response to a pull-to-refresh gesture or after completing
     * an app learning session.
     */
    fun refresh() {
        loadStats()
        loadLearnedApps()
    }
}
