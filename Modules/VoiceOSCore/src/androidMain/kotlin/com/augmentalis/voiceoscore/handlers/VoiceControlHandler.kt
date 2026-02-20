/**
 * VoiceControlHandler.kt - IHandler for VoiceOS service control commands
 *
 * Handles: mute/wake voice, dictation start/stop, show commands/help.
 * Numbers on/off/auto delegated to NumbersOverlayHandler (ACCESSIBILITY).
 *
 * Uses a static callback registry for service-level actions that require
 * direct access to the VoiceOS service internals (speech engine, overlays).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "VoiceControlHandler"

/**
 * Static callback registry for VoiceOS service-level actions.
 *
 * The accessibility service sets these callbacks during onServiceReady().
 * Handlers call them to trigger service-level actions without direct coupling.
 */
object VoiceControlCallbacks {
    /** Mute voice recognition. Returns true if successfully muted. */
    @Volatile var onMuteVoice: (() -> Boolean)? = null

    /** Wake/unmute voice recognition. Returns true if successfully woken. */
    @Volatile var onWakeVoice: (() -> Boolean)? = null

    /** Start dictation mode. Returns true if successfully started. */
    @Volatile var onStartDictation: (() -> Boolean)? = null

    /** Stop dictation mode. Returns true if successfully stopped. */
    @Volatile var onStopDictation: (() -> Boolean)? = null

    /** Show voice commands help overlay. Returns true if shown. */
    @Volatile var onShowCommands: (() -> Boolean)? = null

    // Numbers overlay is handled by NumbersOverlayHandler (ACCESSIBILITY category)
    // via NumbersOverlayExecutor, NOT via callbacks. This avoids duplicate handlers
    // and ensures proper assignment clearing on "numbers off".

    /** Clear all callbacks (call on service destroy). */
    fun clear() {
        onMuteVoice = null
        onWakeVoice = null
        onStartDictation = null
        onStopDictation = null
        onShowCommands = null
    }
}

class VoiceControlHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.SYSTEM

    override val supportedActions: List<String> = listOf(
        // Mute/Wake
        "mute voice", "stop listening", "voice off",
        "wake up voice", "start listening", "voice on",
        // Dictation
        "start dictation", "dictation", "type mode",
        "stop dictation", "end dictation", "command mode",
        // Help
        "show voice commands", "what can i say", "help"
        // Numbers overlay: handled by NumbersOverlayHandler (ACCESSIBILITY category)
        // which has the NumbersOverlayExecutor with proper assignment clearing
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "VoiceControlHandler.execute: '$phrase'")

        return when {
            // Mute/Wake
            phrase in listOf("mute voice", "stop listening", "voice off") ->
                invokeCallback(VoiceControlCallbacks.onMuteVoice, "Voice muted", "Cannot mute voice")

            phrase in listOf("wake up voice", "start listening", "voice on") ->
                invokeCallback(VoiceControlCallbacks.onWakeVoice, "Voice activated", "Cannot wake voice")

            // Dictation
            phrase in listOf("start dictation", "dictation", "type mode") ->
                invokeCallback(VoiceControlCallbacks.onStartDictation, "Dictation started", "Cannot start dictation")

            phrase in listOf("stop dictation", "end dictation", "command mode") ->
                invokeCallback(VoiceControlCallbacks.onStopDictation, "Dictation stopped", "Cannot stop dictation")

            // Help
            phrase in listOf("show voice commands", "what can i say", "help") ->
                invokeCallback(VoiceControlCallbacks.onShowCommands, "Showing commands", "Cannot show commands")

            // Numbers overlay: delegated to NumbersOverlayHandler (ACCESSIBILITY category)
            // which owns NumbersOverlayExecutor with proper assignment clearing.

            else -> HandlerResult.notHandled()
        }
    }

    private fun invokeCallback(callback: (() -> Boolean)?, successMsg: String, failMsg: String): HandlerResult {
        if (callback == null) {
            Log.w(TAG, "Callback not registered: $failMsg")
            return HandlerResult.failure("$failMsg — service callback not registered", recoverable = true)
        }
        return try {
            if (callback()) {
                HandlerResult.success(successMsg)
            } else {
                HandlerResult.failure(failMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Callback failed: $failMsg", e)
            HandlerResult.failure("$failMsg: ${e.message}")
        }
    }
}
