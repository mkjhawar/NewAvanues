package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationMs: Long = 0L,
    val outputUri: String? = null
)

@Serializable
enum class RecordingEvent {
    STARTED, PAUSED, RESUMED, STOPPED, FINALIZED, ERROR
}
