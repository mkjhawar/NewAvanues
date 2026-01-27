// filename: Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/settings/WakeWordViewModel.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.core.common.Result
import com.augmentalis.wakeword.WakeWordEvent
import com.augmentalis.wakeword.WakeWordKeyword
import com.augmentalis.wakeword.WakeWordSettings
import com.augmentalis.wakeword.WakeWordState
import com.augmentalis.wakeword.WakeWordStats
import com.augmentalis.wakeword.IWakeWordDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Wake Word ViewModel
 *
 * Manages wake word detection state and settings.
 *
 * @author Manoj Jhawar
 */
@HiltViewModel
class WakeWordViewModel @Inject constructor(
    private val detector: IWakeWordDetector,
    private val settingsRepository: WakeWordSettingsRepository
) : ViewModel() {

    // Settings
    val settings: StateFlow<WakeWordSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WakeWordSettings()
        )

    // Detection state
    val state: StateFlow<WakeWordState> = detector.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WakeWordState.UNINITIALIZED
        )

    // Detection count
    val detectionCount: StateFlow<Int> = detector.detectionCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Events
    private val _events = MutableSharedFlow<WakeWordEvent>()
    val events: SharedFlow<WakeWordEvent> = _events.asSharedFlow()

    // Statistics
    private val _stats = MutableStateFlow(WakeWordStats())
    val stats: StateFlow<WakeWordStats> = _stats.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Initialize wake word detector with current settings
     */
    fun initialize(onDetected: (WakeWordKeyword) -> Unit) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val result = detector.initialize(currentSettings) { keyword ->
                // Wake word detected!
                _stats.value = _stats.value.copy(
                    totalDetections = _stats.value.totalDetections + 1,
                    lastDetection = System.currentTimeMillis()
                )

                // Emit event
                viewModelScope.launch {
                    _events.emit(WakeWordEvent.Detected(keyword))
                }

                // Invoke callback
                onDetected(keyword)
            }

            when (result) {
                is Result.Success -> {
                    Timber.i("Wake word detector initialized successfully")
                    _errorMessage.value = null
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to initialize wake word detector: ${result.message}")
                    _errorMessage.value = result.message
                    val errorMsg = result.message ?: "Unknown error during initialization"
                    viewModelScope.launch {
                        _events.emit(
                            WakeWordEvent.Error(
                                errorMsg,
                                (result.exception as? Exception) ?: Exception(errorMsg)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Start wake word detection
     */
    fun start() {
        viewModelScope.launch {
            val result = detector.start()

            when (result) {
                is Result.Success -> {
                    Timber.i("Wake word detection started")
                    _errorMessage.value = null
                    _events.emit(WakeWordEvent.Started)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to start wake word detection: ${result.message}")
                    _errorMessage.value = result.message
                    val errorMsg = result.message ?: "Failed to start wake word detection"
                    _events.emit(
                        WakeWordEvent.Error(
                            errorMsg,
                            (result.exception as? Exception) ?: Exception(errorMsg)
                        )
                    )
                }
            }
        }
    }

    /**
     * Stop wake word detection
     */
    fun stop() {
        viewModelScope.launch {
            val result = detector.stop()

            when (result) {
                is Result.Success -> {
                    Timber.i("Wake word detection stopped")
                    _errorMessage.value = null
                    _events.emit(WakeWordEvent.Stopped)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to stop wake word detection: ${result.message}")
                    _errorMessage.value = result.message
                }
            }
        }
    }

    /**
     * Pause wake word detection
     */
    fun pause(reason: String) {
        viewModelScope.launch {
            detector.pause(reason)
            _events.emit(WakeWordEvent.Paused(reason))
        }
    }

    /**
     * Resume wake word detection
     */
    fun resume() {
        viewModelScope.launch {
            detector.resume()
            _events.emit(WakeWordEvent.Resumed)
        }
    }

    /**
     * Update settings
     */
    fun updateSettings(settings: WakeWordSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)

            // If detector is running, restart with new settings
            if (state.value == WakeWordState.LISTENING) {
                Timber.i("Restarting detector with new settings")
                stop()
                // Note: Caller should reinitialize and start
            }
        }
    }

    /**
     * Update enabled state
     */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEnabled(enabled)

            if (enabled) {
                Timber.i("Wake word detection enabled")
            } else {
                Timber.i("Wake word detection disabled")
                stop()
            }
        }
    }

    /**
     * Update keyword
     */
    fun setKeyword(keyword: WakeWordKeyword) {
        viewModelScope.launch {
            settingsRepository.setKeyword(keyword)
            Timber.i("Wake word keyword updated: ${keyword.displayName}")
        }
    }

    /**
     * Update sensitivity
     */
    fun setSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            settingsRepository.setSensitivity(sensitivity)
            Timber.i("Wake word sensitivity updated: $sensitivity")
        }
    }

    /**
     * Update battery optimization
     */
    fun setBatteryOptimization(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBatteryOptimization(enabled)
            Timber.i("Battery optimization: $enabled")
        }
    }

    /**
     * Reset statistics
     */
    fun resetStats() {
        _stats.value = WakeWordStats()
        Timber.i("Wake word statistics reset")
    }

    /**
     * Mark detection as false positive
     */
    fun markFalsePositive() {
        _stats.value = _stats.value.copy(
            falsePositives = _stats.value.falsePositives + 1
        )
        Timber.w("Wake word detection marked as false positive")
    }

    /**
     * Check if detector is currently listening
     */
    fun isListening(): Boolean {
        return detector.isListening()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            detector.cleanup()
        }
    }
}
