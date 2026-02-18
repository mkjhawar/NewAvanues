package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable

/**
 * Pro camera manual control state.
 *
 * Exposes Camera2 manual parameters via CameraX Camera2 interop.
 * All values use normalized ranges (0.0..1.0) for cross-platform UI;
 * platform controllers map to hardware-specific ranges.
 */
@Serializable
data class ProCameraState(
    val isProMode: Boolean = false,
    val iso: IsoState = IsoState(),
    val shutterSpeed: ShutterSpeedState = ShutterSpeedState(),
    val focusDistance: FocusState = FocusState(),
    val whiteBalance: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val stabilization: StabilizationMode = StabilizationMode.AUTO,
    val isRawEnabled: Boolean = false,
    val isRawSupported: Boolean = false,
    val isIsoLocked: Boolean = false,
    val isShutterLocked: Boolean = false,
    val isFocusLocked: Boolean = false,
    val isWhiteBalanceLocked: Boolean = false
)

@Serializable
data class IsoState(
    val currentValue: Int = 100,
    val minValue: Int = 100,
    val maxValue: Int = 3200,
    val isManual: Boolean = false
) {
    /** Normalized position (0.0..1.0) for slider UI. */
    val normalized: Float get() {
        val range = maxValue - minValue
        return if (range > 0) (currentValue - minValue).toFloat() / range else 0f
    }

    val displayText: String get() = "ISO $currentValue"
}

@Serializable
data class ShutterSpeedState(
    /** Shutter speed in nanoseconds. */
    val currentNanos: Long = 33_333_333L,
    val minNanos: Long = 1_000_000L,
    val maxNanos: Long = 1_000_000_000L,
    val isManual: Boolean = false
) {
    /** Normalized position (0.0..1.0) for slider UI. */
    val normalized: Float get() {
        val range = maxNanos - minNanos
        return if (range > 0) (currentNanos - minNanos).toFloat() / range else 0f
    }

    val displayText: String get() {
        val seconds = currentNanos / 1_000_000_000.0
        return if (seconds >= 1.0) {
            "${seconds.toInt()}s"
        } else {
            "1/${(1.0 / seconds).toInt()}"
        }
    }
}

@Serializable
data class FocusState(
    /** Focus distance in diopters (0.0 = infinity). */
    val currentDiopters: Float = 0f,
    val minDiopters: Float = 0f,
    val maxDiopters: Float = 10f,
    val isManual: Boolean = false
) {
    val normalized: Float get() {
        val range = maxDiopters - minDiopters
        return if (range > 0) (currentDiopters - minDiopters) / range else 0f
    }

    val displayText: String get() {
        return if (currentDiopters <= 0.01f) "INF" else String.format("%.1fm", 1f / currentDiopters)
    }
}

@Serializable
enum class WhiteBalanceMode {
    AUTO,
    DAYLIGHT,
    CLOUDY,
    TUNGSTEN,
    FLUORESCENT,
    SHADE
}

@Serializable
enum class StabilizationMode {
    OFF,
    AUTO,
    OPTICAL,
    VIDEO
}
