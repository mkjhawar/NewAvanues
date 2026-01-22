package com.augmentalis.teach
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Platform-independent business logic for Teach-Ava feature
 *
 * This class contains all the core logic without Android dependencies.
 * The Android ViewModel wraps this class to provide lifecycle-aware functionality.
 */
class TeachAvaBusinessLogic(
    private val trainExampleRepository: TrainExampleRepository,
    private val coroutineScope: CoroutineScope
) {

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
        coroutineScope.launch {
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
        coroutineScope.launch {
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
        coroutineScope.launch {
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
        coroutineScope.launch {
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
        coroutineScope.launch {
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
            coroutineScope.launch {
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
