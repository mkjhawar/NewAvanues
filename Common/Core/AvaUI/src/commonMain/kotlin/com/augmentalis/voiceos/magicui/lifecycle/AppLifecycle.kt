package com.augmentalis.avanues.avaui.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app lifecycle state.
 */
class AppLifecycle {
    private val _state = MutableStateFlow<LifecycleState>(LifecycleState.CREATED)
    val state: StateFlow<LifecycleState> = _state.asStateFlow()

    private val observers = mutableListOf<LifecycleObserver>()
    private var isInitialized = false

    /**
     * Initialize app (onCreate).
     */
    suspend fun create() {
        if (isInitialized) return

        _state.value = LifecycleState.CREATED
        notifyObservers { it.onCreate() }
        isInitialized = true
    }

    /**
     * Start app (onStart).
     */
    suspend fun start() {
        requireInitialized()
        _state.value = LifecycleState.STARTED
        notifyObservers { it.onStart() }
    }

    /**
     * Pause app (onPause).
     */
    suspend fun pause() {
        requireInitialized()
        _state.value = LifecycleState.PAUSED
        notifyObservers { it.onPause() }
    }

    /**
     * Resume app (onResume).
     */
    suspend fun resume() {
        requireInitialized()
        _state.value = LifecycleState.RESUMED
        notifyObservers { it.onResume() }
    }

    /**
     * Stop app (onStop).
     */
    suspend fun stop() {
        requireInitialized()
        _state.value = LifecycleState.STOPPED
        notifyObservers { it.onStop() }
    }

    /**
     * Destroy app (onDestroy).
     */
    suspend fun destroy() {
        requireInitialized()
        _state.value = LifecycleState.DESTROYED
        notifyObservers { it.onDestroy() }
        observers.clear()
        isInitialized = false
    }

    /**
     * Add lifecycle observer.
     */
    fun addObserver(observer: LifecycleObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    /**
     * Remove lifecycle observer.
     */
    fun removeObserver(observer: LifecycleObserver) {
        observers.remove(observer)
    }

    private suspend fun notifyObservers(action: suspend (LifecycleObserver) -> Unit) {
        observers.forEach { action(it) }
    }

    private fun requireInitialized() {
        if (!isInitialized) {
            throw LifecycleException("App not initialized. Call create() first.")
        }
    }
}

enum class LifecycleState {
    CREATED,
    STARTED,
    PAUSED,
    RESUMED,
    STOPPED,
    DESTROYED
}

interface LifecycleObserver {
    suspend fun onCreate() {}
    suspend fun onStart() {}
    suspend fun onPause() {}
    suspend fun onResume() {}
    suspend fun onStop() {}
    suspend fun onDestroy() {}
}

class LifecycleException(message: String) : Exception(message)
