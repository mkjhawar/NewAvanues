/**
 * MediaHandler.kt - IHandler for media playback and volume commands
 *
 * Handles: play, pause, next, previous, volume up/down, mute
 * Uses AudioManager for media key dispatch and volume control.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "MediaHandler"

class MediaHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.MEDIA

    override val supportedActions: List<String> = listOf(
        // Play/Resume
        "play music", "play", "resume",
        // Pause/Stop
        "pause music", "pause", "stop music",
        // Track control
        "next song", "next track", "skip",
        "previous song", "previous track",
        // Volume
        "increase volume", "volume up", "louder",
        "decrease volume", "volume down", "lower volume", "quieter",
        "mute volume", "mute", "silence"
    )

    private val audioManager: AudioManager by lazy {
        service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "MediaHandler.execute: '$phrase'")

        return when {
            phrase in listOf("play music", "play", "resume") -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY, "Playing")
            phrase in listOf("pause music", "pause", "stop music") -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE, "Paused")
            phrase in listOf("next song", "next track", "skip") -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Next track")
            phrase in listOf("previous song", "previous track") -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Previous track")
            phrase in listOf("increase volume", "volume up", "louder") -> adjustVolume(AudioManager.ADJUST_RAISE, "Volume up")
            phrase in listOf("decrease volume", "volume down", "lower volume", "quieter") -> adjustVolume(AudioManager.ADJUST_LOWER, "Volume down")
            phrase in listOf("mute volume", "mute", "silence") -> adjustVolume(AudioManager.ADJUST_MUTE, "Muted")
            else -> HandlerResult.notHandled()
        }
    }

    private fun dispatchMediaKey(keyCode: Int, label: String): HandlerResult {
        return try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            Log.i(TAG, "Media key dispatched: $label")
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Media key dispatch failed", e)
            HandlerResult.failure("Failed: ${e.message}")
        }
    }

    private fun adjustVolume(direction: Int, label: String): HandlerResult {
        return try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            Log.i(TAG, "Volume adjusted: $label")
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Volume adjust failed", e)
            HandlerResult.failure("Failed: ${e.message}")
        }
    }
}
