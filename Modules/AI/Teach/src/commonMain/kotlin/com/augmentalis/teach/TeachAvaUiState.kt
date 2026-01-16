package com.augmentalis.teach
import com.augmentalis.ava.core.domain.model.TrainExample

/**
 * UI state for Teach-Ava screen
 */
sealed class TeachAvaUiState {
    data object Loading : TeachAvaUiState()
    data object Empty : TeachAvaUiState()
    data class Success(
        val examples: List<TrainExample>,
        val intents: List<String>
    ) : TeachAvaUiState()
    data class Error(val message: String) : TeachAvaUiState()
}
