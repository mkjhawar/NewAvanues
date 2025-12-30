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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.database.VoiceOSDatabaseManager
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
 */
data class HomeUiState(
    val appsLearned: Long = 0,
    val commandsAvailable: Long = 0,
    val commandsToday: Long = 0,
    val isLoading: Boolean = true,
    val error: String? = null
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
 */
class HomeViewModel(
    private val databaseManager: VoiceOSDatabaseManager
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
     * Refreshes statistics by reloading from the database.
     *
     * Use this function to manually update statistics, for example
     * in response to a pull-to-refresh gesture or after completing
     * an app learning session.
     */
    fun refresh() {
        loadStats()
    }
}
