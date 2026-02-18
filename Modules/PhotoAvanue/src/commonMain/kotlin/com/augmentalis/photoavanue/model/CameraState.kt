package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class CameraState(
    val lens: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val zoom: ZoomState = ZoomState(),
    val exposure: ExposureState = ExposureState(),
    val aspectRatio: AspectRatioMode = AspectRatioMode.AUTO,
    val recording: RecordingState = RecordingState(),
    val isCapturing: Boolean = false,
    val hasGpsLocation: Boolean = false,
    val lastCapturedUri: String? = null,
    val error: String? = null,
    val extensions: CameraExtensions = CameraExtensions(),
    val pro: ProCameraState = ProCameraState()
)

@Serializable
enum class CameraLens { FRONT, BACK }

@Serializable
enum class FlashMode { OFF, ON, AUTO, TORCH }

@Serializable
enum class CaptureMode { PHOTO, VIDEO, SCAN }
