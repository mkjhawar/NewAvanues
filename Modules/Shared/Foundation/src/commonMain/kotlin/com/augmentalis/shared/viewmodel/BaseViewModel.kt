package com.augmentalis.shared.viewmodel

import com.augmentalis.shared.state.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * BaseViewModel - Consolidates common ViewModel patterns
 *
 * BEFORE (4+ lines per ViewModel):
 * ```
 * class MyViewModel {
 *     private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
 *
 *     fun onCleared() {
 *         viewModelScope.cancel()
 *     }
 * }
 * ```
 *
 * AFTER:
 * ```
 * class MyViewModel : BaseViewModel() {
 *     // viewModelScope inherited
 *     // onCleared() inherited
 * }
 * ```
 *
 * Features:
 * - Pre-configured viewModelScope with SupervisorJob
 * - Automatic cleanup via onCleared()
 * - Helper methods for common async patterns
 * - UiState integration for loading/error/success
 */
abstract class BaseViewModel {
    /**
     * CoroutineScope tied to ViewModel lifecycle
     *
     * Uses SupervisorJob to prevent sibling coroutine failures from propagating.
     * Uses Main dispatcher by default for UI-safe state updates.
     */
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Clean up resources when ViewModel is no longer needed
     *
     * IMPORTANT: Must be called when the ViewModel is destroyed
     * (e.g., when Activity/Fragment is destroyed, or Composable leaves composition)
     */
    open fun onCleared() {
        viewModelScope.cancel()
    }

    /**
     * Launch a coroutine in viewModelScope
     *
     * Shorthand for `viewModelScope.launch { ... }`
     */
    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    /**
     * Launch a coroutine with IO dispatcher
     *
     * Use for database/network operations
     */
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO, block = block)
    }

    /**
     * Observe a Flow with automatic error handling
     *
     * @param flow Flow to observe
     * @param onError Error handler (default: logs error)
     * @param onCollect Handler for each emitted value
     */
    protected fun <T> observe(
        flow: Flow<T>,
        onError: (Throwable) -> Unit = { /* default: silent */ },
        onCollect: suspend (T) -> Unit
    ) {
        viewModelScope.launch {
            flow
                .catch { e -> onError(e) }
                .collect { onCollect(it) }
        }
    }

    /**
     * Execute a Result-returning operation with automatic error handling
     *
     * @param operation Suspend function returning Result<T>
     * @param onSuccess Handler for successful result
     * @param onError Handler for error (receives error message)
     */
    protected fun <T> executeResult(
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            operation()
                .onSuccess(onSuccess)
                .onFailure { e -> onError(e.message ?: "Unknown error") }
        }
    }
}

/**
 * BaseStatefulViewModel - BaseViewModel with built-in UiState
 *
 * For ViewModels that need loading/error/success state management.
 *
 * Usage:
 * ```
 * class MyViewModel : BaseStatefulViewModel() {
 *     val isLoading = uiState.isLoading.flow
 *     val error = uiState.error.flow
 *
 *     fun save(data: Data) {
 *         uiState.execute(viewModelScope) {
 *             repository.save(data)
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseStatefulViewModel : BaseViewModel() {
    /**
     * Common UI state (loading, error, success)
     */
    protected val uiState = UiState()

    /**
     * Execute operation with automatic state management
     *
     * - Sets isLoading = true before operation
     * - Sets saveSuccess = true on success
     * - Sets error message on failure
     * - Sets isLoading = false after operation
     */
    protected fun <T> execute(
        showLoading: Boolean = true,
        operation: suspend () -> Result<T>
    ) {
        uiState.execute(viewModelScope, showLoading, operation)
    }

    /**
     * Execute with custom error message prefix
     */
    protected fun <T> executeWithMessage(
        errorPrefix: String,
        showLoading: Boolean = true,
        operation: suspend () -> Result<T>
    ) {
        uiState.executeWithMessage(viewModelScope, errorPrefix, showLoading, operation)
    }
}
