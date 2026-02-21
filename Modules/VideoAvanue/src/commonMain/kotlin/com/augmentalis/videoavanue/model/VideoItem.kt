package com.augmentalis.videoavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoItem(
    val uri: String,
    val title: String = "",
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/*",
    val fileSizeBytes: Long = 0,
    val thumbnailUri: String? = null,
    val subtitleUri: String? = null
) {
    val durationFormatted: String get() {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0)
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        else
            "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

@Serializable
data class VideoPlayerState(
    val video: VideoItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
}

enum class RepeatMode { OFF, ONE, ALL }
