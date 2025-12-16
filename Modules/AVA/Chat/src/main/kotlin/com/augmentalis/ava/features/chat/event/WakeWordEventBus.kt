package com.augmentalis.ava.features.chat.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wake Word Event Bus - SOLID-compliant event communication
 *
 * Created to remove reflection-based wake word event observation (P1).
 * Provides a clean, type-safe interface for wake word event communication
 * between MainActivity and ChatViewModel.
 *
 * Benefits:
 * - No reflection (was using Class.forName)
 * - Type-safe event handling
 * - Testable (can be mocked)
 * - Decoupled from Activity lifecycle
 *
 * Usage:
 * - MainActivity: wakeWordEventBus.emit("AVA")
 * - ChatViewModel: wakeWordEventBus.events.collect { keyword -> ... }
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class WakeWordEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<WakeWordEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events: SharedFlow<WakeWordEvent> = _events.asSharedFlow()

    /**
     * Emit a wake word detection event.
     *
     * @param keyword The detected wake word (e.g., "AVA", "Hey AVA")
     */
    suspend fun emit(keyword: String) {
        _events.emit(WakeWordEvent(keyword, System.currentTimeMillis()))
    }

    /**
     * Try to emit a wake word event without suspension.
     * Useful for non-suspend contexts.
     *
     * @param keyword The detected wake word
     * @return True if event was emitted, false if buffer was full
     */
    fun tryEmit(keyword: String): Boolean {
        return _events.tryEmit(WakeWordEvent(keyword, System.currentTimeMillis()))
    }
}

/**
 * Wake word detection event.
 *
 * @param keyword The detected wake word
 * @param timestamp When the wake word was detected
 */
data class WakeWordEvent(
    val keyword: String,
    val timestamp: Long
)
