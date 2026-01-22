package com.augmentalis.chat.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock

/**
 * Wake Word Event Bus - KMP-compatible event communication
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
 * - Cross-platform compatible (KMP)
 *
 * Usage:
 * - MainActivity: wakeWordEventBus.emit("AVA")
 * - ChatViewModel: wakeWordEventBus.events.collect { keyword -> ... }
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 * @updated 2025-12-17 - Converted to KMP-compatible format
 */
class WakeWordEventBus {

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
        _events.emit(WakeWordEvent(keyword, Clock.System.now().toEpochMilliseconds()))
    }

    /**
     * Try to emit a wake word event without suspension.
     * Useful for non-suspend contexts.
     *
     * @param keyword The detected wake word
     * @return True if event was emitted, false if buffer was full
     */
    fun tryEmit(keyword: String): Boolean {
        return _events.tryEmit(WakeWordEvent(keyword, Clock.System.now().toEpochMilliseconds()))
    }
}

/**
 * Wake word detection event.
 *
 * @param keyword The detected wake word
 * @param timestamp When the wake word was detected (epoch milliseconds)
 */
data class WakeWordEvent(
    val keyword: String,
    val timestamp: Long
)
