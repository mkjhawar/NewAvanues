package com.avanues.cockpit.voice.feedback

import com.avanues.cockpit.core.workspace.Vector3D

/**
 * Interface for providing 3D spatial audio feedback.
 */
interface AudioFeedback {
    fun playSound(soundId: String, position: Vector3D, volume: Float = 1.0f)
    fun playBackgroundAmbience(soundId: String, volume: Float = 0.5f)
    fun stopAll()
    
    companion object SoundIds {
        const val WINDOW_OPEN = "window_open"
        const val WINDOW_CLOSE = "window_close"
        const val FOCUS_CHANGE = "focus_change"
        const val LAYOUT_SWITCH = "layout_switch"
        const val ERROR = "error_tone"
    }
}
