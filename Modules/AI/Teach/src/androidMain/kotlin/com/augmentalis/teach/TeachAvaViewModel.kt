package com.augmentalis.teach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Android ViewModel for Teach-Ava screen
 * Wraps the platform-independent business logic and provides lifecycle-aware functionality
 */
@HiltViewModel
class TeachAvaViewModel @Inject constructor(
    trainExampleRepository: TrainExampleRepository
) : ViewModel(), TeachAvaViewModelInterface {

    private val businessLogic = TeachAvaBusinessLogic(
        trainExampleRepository = trainExampleRepository,
        coroutineScope = viewModelScope
    )

    override val uiState: StateFlow<TeachAvaUiState> = businessLogic.uiState
    override val selectedLocale: StateFlow<String> = businessLogic.selectedLocale
    override val selectedIntent: StateFlow<String?> = businessLogic.selectedIntent

    override fun loadTrainingExamples() = businessLogic.loadTrainingExamples()
    override fun addExample(example: TrainExample) = businessLogic.addExample(example)
    override fun updateExample(example: TrainExample) = businessLogic.updateExample(example)
    override fun deleteExample(exampleId: Long) = businessLogic.deleteExample(exampleId)
    override fun setLocaleFilter(locale: String) = businessLogic.setLocaleFilter(locale)
    override fun setIntentFilter(intent: String?) = businessLogic.setIntentFilter(intent)
    override fun clearError() = businessLogic.clearError()
}
