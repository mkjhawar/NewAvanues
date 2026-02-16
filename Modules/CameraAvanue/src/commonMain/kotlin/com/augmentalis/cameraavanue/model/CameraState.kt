package com.augmentalis.cameraavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class CameraState(
    val lens: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val zoom: Float = 1.0f,
    val isCapturing: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val lastCapturedUri: String? = null,
    val error: String? = null,
    val captureMode: CaptureMode = CaptureMode.PHOTO
)

enum class CameraLens { FRONT, BACK }
enum class FlashMode { OFF, ON, AUTO, TORCH }
enum class CaptureMode { PHOTO, VIDEO, SCAN }
