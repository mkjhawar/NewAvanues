package com.augmentalis.avanues.avamagic.components.state

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Base class for ViewModels in AvaElements.
 *
 * MagicViewModel provides lifecycle-aware state management with coroutine support,
 * automatic cleanup, and integration with platform-specific lifecycle systems.
 *
 * Usage:
 * ```kotlin
 * class LoginViewModel : MagicViewModel() {
 *     private val _email = mutableState("")
 *     val email: StateFlow<String> = _email
 *
 *     fun login() {
 *         viewModelScope.launch {
 *             // Async login logic
 *         }
 *     }
 * }
 * ```
 */
abstract class MagicViewModel {
    /**
     * Coroutine scope tied to the ViewModel lifecycle.
     * All coroutines launched in this scope will be automatically cancelled
     * when the ViewModel is cleared.
     */
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * State container for this ViewModel
     */
    protected val stateContainer = StateContainer()

    /**
     * Create a mutable state flow
     */
    protected fun <T> mutableState(initialValue: T): MutableStateFlow<T> {
        return MutableStateFlow(initialValue)
    }

    /**
     * Create a state flow from a mutable state flow
     */
    protected fun <T> MutableStateFlow<T>.asState(): StateFlow<T> {
        return this.asStateFlow()
    }

    /**
     * Create a derived state that depends on other states
     */
    protected fun <T> derivedState(
        vararg sources: StateFlow<*>,
        compute: () -> T
    ): StateFlow<T> {
        return if (sources.isEmpty()) {
            MutableStateFlow(compute()).asStateFlow()
        } else {
            combine(sources.toList()) { compute() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = compute()
                )
        }
    }

    /**
     * Create a state from the state container
     */
    protected fun <T> rememberState(key: String, initialValue: T): MutableMagicState<T> {
        return stateContainer.remember(key, initialValue)
    }

    /**
     * Execute a suspending operation with loading state
     */
    protected suspend fun <T> withLoading(
        loadingState: MutableStateFlow<Boolean>,
        block: suspend () -> T
    ): Result<T> {
        return try {
            loadingState.value = true
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            loadingState.value = false
        }
    }

    /**
     * Execute a suspending operation with loading and error state
     */
    protected suspend fun <T> execute(
        loadingState: MutableStateFlow<Boolean>? = null,
        errorState: MutableStateFlow<String?>? = null,
        block: suspend () -> T
    ): Result<T> {
        return try {
            loadingState?.value = true
            errorState?.value = null
            Result.success(block())
        } catch (e: Exception) {
            errorState?.value = e.message ?: "An error occurred"
            Result.failure(e)
        } finally {
            loadingState?.value = false
        }
    }

    /**
     * Debounce state changes
     */
    protected fun <T> StateFlow<T>.debounce(timeoutMillis: Long): StateFlow<T> {
        return this.debounce(timeoutMillis)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = this.value
            )
    }

    /**
     * Called when the ViewModel is being cleared.
     * Override this to perform cleanup operations.
     */
    protected open fun onCleared() {}

    /**
     * Clear all resources associated with this ViewModel.
     * This will cancel all coroutines in the viewModelScope.
     */
    fun clear() {
        onCleared()
        viewModelScope.cancel()
        stateContainer.clearAll()
    }
}

/**
 * Event handler for one-time events in ViewModels
 */
sealed class ViewModelEvent<out T> {
    data class Success<T>(val data: T) : ViewModelEvent<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : ViewModelEvent<Nothing>()
    object Loading : ViewModelEvent<Nothing>()
    object Idle : ViewModelEvent<Nothing>()
}

/**
 * Channel-based event system for ViewModels
 */
class ViewModelEventChannel<T> {
    private val _events = MutableSharedFlow<T>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<T> = _events.asSharedFlow()

    /**
     * Send an event
     */
    suspend fun send(event: T) {
        _events.emit(event)
    }

    /**
     * Send an event without suspending
     */
    fun trySend(event: T): Boolean {
        return _events.tryEmit(event)
    }
}

/**
 * Base class for ViewModels with UI state management
 */
abstract class StatefulViewModel<State : Any>(
    initialState: State
) : MagicViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    /**
     * Update the UI state
     */
    protected fun updateState(update: State.() -> State) {
        _uiState.value = _uiState.value.update()
    }

    /**
     * Set the UI state directly
     */
    protected fun setState(state: State) {
        _uiState.value = state
    }

    /**
     * Get current state
     */
    protected fun currentState(): State = _uiState.value
}

/**
 * Base class for ViewModels with loading and error states
 */
abstract class AsyncViewModel : MagicViewModel() {
    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    protected val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Execute an async operation with automatic loading and error handling
     */
    protected fun launchAsync(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                block()
            } catch (e: CancellationException) {
                // Ignore cancellation
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * Factory for creating ViewModels
 */
interface ViewModelFactory<T : MagicViewModel> {
    fun create(): T
}

/**
 * Store for managing ViewModel instances
 */
object ViewModelStore {
    private val viewModels = mutableMapOf<String, MagicViewModel>()

    /**
     * Get or create a ViewModel
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : MagicViewModel> getOrCreate(
        key: String,
        factory: () -> T
    ): T {
        return viewModels.getOrPut(key) { factory() } as T
    }

    /**
     * Remove a ViewModel
     */
    fun remove(key: String) {
        viewModels.remove(key)?.clear()
    }

    /**
     * Clear all ViewModels
     */
    fun clear() {
        viewModels.values.forEach { it.clear() }
        viewModels.clear()
    }
}
