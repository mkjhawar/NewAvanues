package com.augmentalis.ava.features.teach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Teach-Ava screen
 * Manages training examples and intent learning
 */
@HiltViewModel
class TeachAvaViewModel @Inject constructor(
    private val trainExampleRepository: TrainExampleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeachAvaUiState>(TeachAvaUiState.Loading)
    val uiState: StateFlow<TeachAvaUiState> = _uiState.asStateFlow()

    private val _selectedLocale = MutableStateFlow("en-US")
    val selectedLocale: StateFlow<String> = _selectedLocale.asStateFlow()

    private val _selectedIntent = MutableStateFlow<String?>(null)
    val selectedIntent: StateFlow<String?> = _selectedIntent.asStateFlow()

    init {
        loadTrainingExamples()
    }

    fun loadTrainingExamples() {
        viewModelScope.launch {
            trainExampleRepository.getAllExamples()
                .catch { exception ->
                    _uiState.value = TeachAvaUiState.Error(
                        message = "Failed to load training examples: ${exception.message}"
                    )
                }
                .collect { examples ->
                    if (examples.isEmpty()) {
                        _uiState.value = TeachAvaUiState.Empty
                    } else {
                        _uiState.value = TeachAvaUiState.Success(
                            examples = examples,
                            intents = examples.map { it.intent }.distinct().sorted()
                        )
                    }
                }
        }
    }

    fun addExample(example: TrainExample) {
        viewModelScope.launch {
            when (val result = trainExampleRepository.addTrainExample(example)) {
                is Result.Success -> {
                    // Success - UI will update via Flow
                }
                is Result.Error -> {
                    _uiState.value = TeachAvaUiState.Error(
                        message = result.message ?: "Failed to add example"
                    )
                }
            }
        }
    }

    fun updateExample(example: TrainExample) {
        viewModelScope.launch {
            // Delete old example and add updated one
            // This ensures hash is recalculated and constraints are checked
            when (trainExampleRepository.deleteTrainExample(example.id)) {
                is Result.Success<*> -> {
                    when (val result = trainExampleRepository.addTrainExample(example)) {
                        is Result.Success -> {
                            // Success - UI will update via Flow
                        }
                        is Result.Error -> {
                            _uiState.value = TeachAvaUiState.Error(
                                message = result.message ?: "Failed to update example"
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.value = TeachAvaUiState.Error(
                        message = "Failed to update example"
                    )
                }
            }
        }
    }

    fun deleteExample(exampleId: Long) {
        viewModelScope.launch {
            when (val result = trainExampleRepository.deleteTrainExample(exampleId)) {
                is Result.Success<*> -> {
                    // Success - UI will update via Flow
                }
                is Result.Error -> {
                    _uiState.value = TeachAvaUiState.Error(
                        message = result.message ?: "Failed to delete example"
                    )
                }
            }
        }
    }

    fun setLocaleFilter(locale: String) {
        _selectedLocale.value = locale
        viewModelScope.launch {
            trainExampleRepository.getExamplesForLocale(locale)
                .catch { exception ->
                    _uiState.value = TeachAvaUiState.Error(
                        message = "Failed to filter by locale: ${exception.message}"
                    )
                }
                .collect { examples ->
                    if (examples.isEmpty()) {
                        _uiState.value = TeachAvaUiState.Empty
                    } else {
                        _uiState.value = TeachAvaUiState.Success(
                            examples = examples,
                            intents = examples.map { it.intent }.distinct().sorted()
                        )
                    }
                }
        }
    }

    fun setIntentFilter(intent: String?) {
        _selectedIntent.value = intent
        if (intent == null) {
            loadTrainingExamples()
        } else {
            viewModelScope.launch {
                trainExampleRepository.getExamplesForIntent(intent)
                    .catch { exception ->
                        _uiState.value = TeachAvaUiState.Error(
                            message = "Failed to filter by intent: ${exception.message}"
                        )
                    }
                    .collect { examples ->
                        if (examples.isEmpty()) {
                            _uiState.value = TeachAvaUiState.Empty
                        } else {
                            _uiState.value = TeachAvaUiState.Success(
                                examples = examples,
                                intents = examples.map { it.intent }.distinct().sorted()
                            )
                        }
                    }
            }
        }
    }

    fun clearError() {
        if (_uiState.value is TeachAvaUiState.Error) {
            loadTrainingExamples()
        }
    }
}

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
