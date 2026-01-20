package com.augmentalis.webavanue

import com.augmentalis.webavanue.BrowserSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe state machine for settings updates.
 * Ensures atomic transitions and prevents race conditions.
 *
 * Purpose: Fix L3 Runtime concurrency gaps
 * - Prevents race conditions during settings updates
 * - Provides synchronized state transitions
 * - Queues rapid setting changes to avoid partial application
 *
 * States:
 * - Idle: No updates in progress
 * - Applying: Currently applying settings to WebView
 * - Queued: Update queued while another applies
 * - Error: Application failed (allows retry with exponential backoff)
 *
 * Thread Safety:
 * - Mutex ensures atomic state transitions
 * - StateFlow provides thread-safe state visibility
 * - All public methods are suspend functions with mutex protection
 *
 * @param scope CoroutineScope for launching apply operations
 */
class SettingsStateMachine(
    private val scope: CoroutineScope
) {
    // Mutex for thread-safe state transitions
    private val mutex = Mutex()

    // Current state of the state machine
    private val _state = MutableStateFlow<SettingsState>(SettingsState.Idle)
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    // Pending settings to apply after current operation completes
    private var pendingSettings: BrowserSettings? = null

    // Currently applying settings (for error recovery)
    private var currentSettings: BrowserSettings? = null

    /**
     * Request settings update. Queues if update in progress.
     *
     * Thread Safety: Uses mutex to ensure atomic state transitions.
     *
     * State Transitions:
     * - Idle → Applying (start new update)
     * - Applying → Queued (queue new update, replacing any existing queue)
     * - Queued → Queued (replace queued update with newer one)
     * - Error → Applying (retry from error state)
     *
     * @param newSettings Settings to apply
     * @param applyFunction Suspend function that applies settings (e.g., to WebView)
     */
    suspend fun requestUpdate(
        newSettings: BrowserSettings,
        applyFunction: suspend (BrowserSettings) -> Result<Unit>
    ) {
        mutex.withLock {
            when (val currentState = _state.value) {
                is SettingsState.Idle -> {
                    // No update in progress, start applying immediately
                    startApplying(newSettings, applyFunction)
                }
                is SettingsState.Applying -> {
                    // Update in progress, queue this one
                    queueUpdate(newSettings)
                }
                is SettingsState.Queued -> {
                    // Already queued, replace with newest settings
                    // This prevents queue buildup for rapid changes
                    pendingSettings = newSettings
                    _state.value = SettingsState.Queued(
                        current = currentSettings!!,
                        queued = newSettings
                    )
                }
                is SettingsState.Error -> {
                    // Retry from error state with new settings
                    startApplying(newSettings, applyFunction)
                }
            }
        }
    }

    /**
     * Start applying settings (must be called within mutex)
     *
     * Transitions: Idle/Error → Applying
     *
     * @param settings Settings to apply
     * @param applyFunction Function to apply settings
     */
    private suspend fun startApplying(
        settings: BrowserSettings,
        applyFunction: suspend (BrowserSettings) -> Result<Unit>
    ) {
        _state.value = SettingsState.Applying(settings)
        currentSettings = settings

        // Launch coroutine to apply settings asynchronously
        scope.launch {
            val result = applyFunction(settings)
            handleApplyResult(result, applyFunction)
        }
    }

    /**
     * Handle result of applying settings
     *
     * Success Transitions:
     * - Applying → Idle (no queue)
     * - Applying → Applying (process queued update)
     *
     * Failure Transitions:
     * - Applying → Error (with retry count)
     *
     * @param result Result of apply operation
     * @param applyFunction Function for applying queued updates
     */
    private suspend fun handleApplyResult(
        result: Result<Unit>,
        applyFunction: suspend (BrowserSettings) -> Result<Unit>
    ) {
        mutex.withLock {
            result.fold(
                onSuccess = {
                    // Application successful
                    // Check if there's a queued update
                    val queued = pendingSettings
                    if (queued != null) {
                        // Apply queued update
                        pendingSettings = null
                        startApplying(queued, applyFunction)
                    } else {
                        // No queued update, return to idle
                        _state.value = SettingsState.Idle
                    }
                },
                onFailure = { error ->
                    // Application failed, enter error state
                    _state.value = SettingsState.Error(
                        settings = currentSettings!!,
                        error = error,
                        retryCount = 0
                    )
                }
            )
        }
    }

    /**
     * Queue update for later (must be called within mutex)
     *
     * Transitions: Applying → Queued
     *
     * @param settings Settings to queue
     */
    private fun queueUpdate(settings: BrowserSettings) {
        pendingSettings = settings
        _state.value = SettingsState.Queued(
            current = currentSettings!!,
            queued = settings
        )
    }

    /**
     * Retry failed update with exponential backoff.
     *
     * Thread Safety: Uses mutex for atomic state check and transition.
     *
     * Transitions: Error → Applying (if retry count < maxRetries)
     *
     * @param applyFunction Function to apply settings
     * @param maxRetries Maximum number of retries (default 3)
     * @return true if retry started, false if max retries reached
     */
    suspend fun retryError(
        applyFunction: suspend (BrowserSettings) -> Result<Unit>,
        maxRetries: Int = 3
    ): Boolean {
        mutex.withLock {
            val errorState = _state.value as? SettingsState.Error ?: return false

            if (errorState.retryCount < maxRetries) {
                // Calculate exponential backoff delay
                val delayMs = (1000L * (1 shl errorState.retryCount)) // 1s, 2s, 4s, 8s...

                // Update state to show retry attempt
                _state.value = errorState.copy(retryCount = errorState.retryCount + 1)

                // Launch retry with delay
                scope.launch {
                    delay(delayMs)
                    mutex.withLock {
                        startApplying(errorState.settings, applyFunction)
                    }
                }

                return true
            } else {
                // Max retries reached, stay in error state
                return false
            }
        }
    }

    /**
     * Clear any queued updates and return to idle.
     *
     * Thread Safety: Uses mutex for atomic state transition.
     *
     * Transitions: Any → Idle
     *
     * Use Case: When user navigates away from settings or closes tab
     */
    suspend fun reset() {
        mutex.withLock {
            pendingSettings = null
            currentSettings = null
            _state.value = SettingsState.Idle
        }
    }

    /**
     * Check if state machine is currently applying settings.
     *
     * @return true if applying or queued, false if idle or error
     */
    fun isApplying(): Boolean {
        val currentState = _state.value
        return currentState is SettingsState.Applying || currentState is SettingsState.Queued
    }

    /**
     * Get current error if in error state.
     *
     * @return Error throwable if in error state, null otherwise
     */
    fun getCurrentError(): Throwable? {
        return (_state.value as? SettingsState.Error)?.error
    }
}

/**
 * Settings state machine states.
 *
 * Sealed class ensures exhaustive when expressions.
 */
sealed class SettingsState {
    /**
     * Idle - No updates in progress
     *
     * Valid Transitions:
     * - → Applying (when requestUpdate called)
     */
    object Idle : SettingsState()

    /**
     * Applying - Currently applying settings
     *
     * @param settings Settings being applied
     *
     * Valid Transitions:
     * - → Idle (on success, no queue)
     * - → Applying (on success, with queue)
     * - → Error (on failure)
     * - → Queued (when new requestUpdate called)
     */
    data class Applying(
        val settings: BrowserSettings
    ) : SettingsState()

    /**
     * Queued - Update queued while another applies
     *
     * @param current Currently applying settings
     * @param queued Settings queued for next application
     *
     * Valid Transitions:
     * - → Applying (when current application completes)
     * - → Queued (when newer requestUpdate replaces queue)
     */
    data class Queued(
        val current: BrowserSettings,
        val queued: BrowserSettings
    ) : SettingsState()

    /**
     * Error - Application failed
     *
     * @param settings Settings that failed to apply
     * @param error Exception that occurred
     * @param retryCount Number of retry attempts made
     *
     * Valid Transitions:
     * - → Applying (on retry)
     * - → Idle (on reset)
     */
    data class Error(
        val settings: BrowserSettings,
        val error: Throwable,
        val retryCount: Int = 0
    ) : SettingsState()
}
