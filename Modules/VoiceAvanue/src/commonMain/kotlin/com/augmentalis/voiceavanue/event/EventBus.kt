/**
 * EventBus.kt - SharedFlow-based event bus for cross-module communication
 *
 * Provides type-safe event broadcasting across VoiceOSCore, WebAvanue, and VoiceCursor.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Central event bus for VoiceAvanue modules
 *
 * Usage:
 * ```
 * // Subscribe to specific event type
 * EventBus.subscribe<CommandRecognizedEvent> { event ->
 *     processCommand(event.command)
 * }
 *
 * // Publish event
 * EventBus.publish(CommandRecognizedEvent(command))
 * ```
 */
object EventBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<VoiceAvanueEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<VoiceAvanueEvent> = _events.asSharedFlow()

    /**
     * Publish an event to all subscribers
     */
    fun publish(event: VoiceAvanueEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    /**
     * Publish an event suspending until delivered
     */
    suspend fun publishSuspending(event: VoiceAvanueEvent) {
        _events.emit(event)
    }

    /**
     * Subscribe to events of a specific type
     */
    inline fun <reified T : VoiceAvanueEvent> subscribe(
        scope: CoroutineScope,
        crossinline handler: suspend (T) -> Unit
    ) {
        scope.launch {
            events.filterIsInstance<T>().collect { event ->
                handler(event)
            }
        }
    }
}

/**
 * Base class for all VoiceAvanue events
 */
@Serializable
sealed class VoiceAvanueEvent {
    abstract val timestamp: Long
    abstract val source: EventSource
}

/**
 * Event source identification
 */
@Serializable
enum class EventSource {
    VOICE_OS_CORE,
    WEB_AVANUE,
    VOICE_CURSOR,
    GAZE_TRACKER,
    RPC_SERVER,
    USER_INPUT,
    SYSTEM
}

// ============================================================
// Voice Command Events
// ============================================================

@Serializable
data class CommandRecognizedEvent(
    val commandId: String,
    val phrase: String,
    val confidence: Float,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.VOICE_OS_CORE
) : VoiceAvanueEvent()

@Serializable
data class CommandExecutedEvent(
    val commandId: String,
    val success: Boolean,
    val resultMessage: String? = null,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.VOICE_OS_CORE
) : VoiceAvanueEvent()

// ============================================================
// Browser Events
// ============================================================

@Serializable
data class NavigationEvent(
    val url: String,
    val title: String?,
    val isLoading: Boolean,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.WEB_AVANUE
) : VoiceAvanueEvent()

@Serializable
data class TabChangedEvent(
    val tabId: String,
    val action: TabAction,
    val tabCount: Int,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.WEB_AVANUE
) : VoiceAvanueEvent()

@Serializable
enum class TabAction {
    CREATED, CLOSED, SWITCHED, UPDATED
}

@Serializable
data class PageLoadedEvent(
    val url: String,
    val title: String,
    val loadTimeMs: Long,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.WEB_AVANUE
) : VoiceAvanueEvent()

// ============================================================
// Cursor Events
// ============================================================

@Serializable
data class CursorMovedEvent(
    val x: Float,
    val y: Float,
    val isLocked: Boolean,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.VOICE_CURSOR
) : VoiceAvanueEvent()

@Serializable
data class CursorClickEvent(
    val x: Float,
    val y: Float,
    val clickType: CursorClickType,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.VOICE_CURSOR
) : VoiceAvanueEvent()

@Serializable
enum class CursorClickType {
    SINGLE, DOUBLE, LONG_PRESS, DWELL
}

@Serializable
data class DwellProgressEvent(
    val x: Float,
    val y: Float,
    val progress: Float, // 0.0 to 1.0
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.VOICE_CURSOR
) : VoiceAvanueEvent()

// ============================================================
// Gaze/Eye Tracking Events
// ============================================================

@Serializable
data class GazePositionEvent(
    val x: Float,
    val y: Float,
    val confidence: Float,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.GAZE_TRACKER
) : VoiceAvanueEvent()

@Serializable
data class GazeCalibrationEvent(
    val status: CalibrationStatus,
    val accuracy: Float?,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.GAZE_TRACKER
) : VoiceAvanueEvent()

@Serializable
enum class CalibrationStatus {
    STARTED, IN_PROGRESS, COMPLETED, FAILED
}

// ============================================================
// RPC Events
// ============================================================

@Serializable
data class RpcConnectedEvent(
    val clientId: String,
    val protocol: String,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.RPC_SERVER
) : VoiceAvanueEvent()

@Serializable
data class RpcDisconnectedEvent(
    val clientId: String,
    val reason: String?,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.RPC_SERVER
) : VoiceAvanueEvent()

@Serializable
data class RpcMessageEvent(
    val clientId: String,
    val method: String,
    val requestId: String?,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.RPC_SERVER
) : VoiceAvanueEvent()

// ============================================================
// System Events
// ============================================================

@Serializable
data class SystemStateEvent(
    val state: SystemState,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.SYSTEM
) : VoiceAvanueEvent()

@Serializable
enum class SystemState {
    INITIALIZING, READY, PAUSED, STOPPING, STOPPED, ERROR
}

@Serializable
data class ErrorEvent(
    val errorCode: String,
    val message: String,
    val details: String? = null,
    override val timestamp: Long = currentTimeMillis(),
    override val source: EventSource = EventSource.SYSTEM
) : VoiceAvanueEvent()

// Helper function for cross-platform time using kotlinx-datetime
internal fun currentTimeMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
