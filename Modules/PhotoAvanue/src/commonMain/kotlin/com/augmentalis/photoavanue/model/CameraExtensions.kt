package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable

/**
 * CameraX Extensions availability and active state.
 *
 * Maps Apple-like features to CameraX Extensions API:
 * - Portrait (Apple) -> Bokeh (CameraX)
 * - Night Mode (Apple) -> Night (CameraX)
 * - Smart HDR (Apple) -> HDR (CameraX)
 * - Face Retouch -> FaceRetouch (CameraX)
 */
@Serializable
data class CameraExtensions(
    val bokehAvailable: Boolean = false,
    val hdrAvailable: Boolean = false,
    val nightAvailable: Boolean = false,
    val faceRetouchAvailable: Boolean = false,
    val activeMode: ExtensionMode = ExtensionMode.NONE
) {
    val hasAnyExtension: Boolean
        get() = bokehAvailable || hdrAvailable || nightAvailable || faceRetouchAvailable

    fun isAvailable(mode: ExtensionMode): Boolean = when (mode) {
        ExtensionMode.NONE -> true
        ExtensionMode.BOKEH -> bokehAvailable
        ExtensionMode.HDR -> hdrAvailable
        ExtensionMode.NIGHT -> nightAvailable
        ExtensionMode.FACE_RETOUCH -> faceRetouchAvailable
    }
}

@Serializable
enum class ExtensionMode {
    NONE,
    BOKEH,
    HDR,
    NIGHT,
    FACE_RETOUCH
}
