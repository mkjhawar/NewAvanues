package com.augmentalis.webavanue.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UiState - Manages common loading/error/success state trilogy
 *
 * BEFORE (12 lines):
 * ```
 * private val _isLoading = MutableStateFlow(false)
 * val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
 *
 * private val _error = MutableStateFlow<String?>(null)
 * val error: StateFlow<String?> = _error.asStateFlow()
 *
 * private val _saveSuccess = MutableStateFlow(false)
 * val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
 *
 * fun clearError() { _error.value = null }
 * fun clearSuccess() { _saveSuccess.value = false }
 * ```
 *
 * AFTER (1 line):
 * ```
 * val uiState = UiState()
 * ```
 *
 * Usage in ViewModel:
 * ```
 * // Access flows
 * val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
 * val error: StateFlow<String?> = uiState.error.flow
 * val saveSuccess: StateFlow<Boolean> = uiState.saveSuccess.flow
 *
 * // Execute async operation with automatic state management
 * uiState.execute(viewModelScope) {
 *     repository.save(data)
 * }
 * ```
 */
class UiState {
    val isLoading = ViewModelState(false)
    val error = NullableState<String>()
    val saveSuccess = ViewModelState(false)

    /**
     * Execute an async operation with automatic loading/error/success management
     *
     * @param scope CoroutineScope for launching the operation
     * @param showLoading Whether to show loading indicator (default: true)
     * @param operation Suspend function that returns Result<T>
     */
    fun <T> execute(
        scope: CoroutineScope,
        showLoading: Boolean = true,
        operation: suspend () -> Result<T>
    ) {
        scope.launch {
            if (showLoading) isLoading.value = true
            error.clear()
            saveSuccess.value = false

            operation()
                .onSuccess { saveSuccess.value = true }
                .onFailure { e -> error.value = e.message ?: "Unknown error" }

            if (showLoading) isLoading.value = false
        }
    }

    /**
     * Execute with custom error message
     */
    fun <T> executeWithMessage(
        scope: CoroutineScope,
        errorPrefix: String,
        showLoading: Boolean = true,
        operation: suspend () -> Result<T>
    ) {
        scope.launch {
            if (showLoading) isLoading.value = true
            error.clear()
            saveSuccess.value = false

            operation()
                .onSuccess { saveSuccess.value = true }
                .onFailure { e -> error.value = "$errorPrefix: ${e.message}" }

            if (showLoading) isLoading.value = false
        }
    }

    /**
     * Reset all state
     */
    fun reset() {
        isLoading.value = false
        error.clear()
        saveSuccess.value = false
    }

    /**
     * Set error state
     */
    fun setError(message: String) {
        error.value = message
    }

    /**
     * Clear error state
     */
    fun clearError() {
        error.clear()
    }

    /**
     * Clear success state
     */
    fun clearSuccess() {
        saveSuccess.value = false
    }
}

/**
 * Extension function to execute Result-returning suspend functions with UiState management
 *
 * Usage:
 * ```
 * repository.save(data).handleWith(uiState, scope) { savedItem ->
 *     // Handle success
 * }
 * ```
 */
suspend fun <T> Result<T>.handleWith(
    uiState: UiState,
    onSuccess: (T) -> Unit = {}
): Result<T> {
    return this
        .onSuccess {
            uiState.saveSuccess.value = true
            onSuccess(it)
        }
        .onFailure { e ->
            uiState.error.value = e.message ?: "Unknown error"
        }
}
