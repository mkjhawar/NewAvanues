package com.augmentalis.teach
import com.augmentalis.ava.core.domain.model.TrainExample
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-independent interface for TeachAvaViewModel
 *
 * This interface defines the contract for the ViewModel, allowing the business logic
 * to be shared across platforms while keeping the Android ViewModel implementation
 * in the androidMain source set.
 */
interface TeachAvaViewModelInterface {
    val uiState: StateFlow<TeachAvaUiState>
    val selectedLocale: StateFlow<String>
    val selectedIntent: StateFlow<String?>

    fun loadTrainingExamples()
    fun addExample(example: TrainExample)
    fun updateExample(example: TrainExample)
    fun deleteExample(exampleId: Long)
    fun setLocaleFilter(locale: String)
    fun setIntentFilter(intent: String?)
    fun clearError()
}
