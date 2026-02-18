/**
 * IosVoiceControlHandler.kt - iOS handler for VoiceOS service control commands
 *
 * Handles voice engine state management: mute/wake, dictation/command mode
 * switching, voice help, and status queries. Dispatches through static
 * callbacks that the iOS app sets when VoiceOSCore is initialized.
 *
 * No AccessibilityService needed — all operations are in-app VoiceOSCore
 * state changes.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile
import platform.Foundation.NSLog

private const val TAG = "IosVoiceControlHandler"

/**
 * Static callback registry for iOS voice control operations.
 * The iOS host app sets these callbacks during VoiceOSCore initialization
 * to wire voice commands to actual speech engine state changes.
 */
object IosVoiceControlCallbacks {
    @Volatile var onMuteVoice: (() -> Boolean)? = null
    @Volatile var onWakeVoice: (() -> Boolean)? = null
    @Volatile var onStartDictation: (() -> Boolean)? = null
    @Volatile var onStopDictation: (() -> Boolean)? = null
    @Volatile var onShowCommands: (() -> Boolean)? = null
    @Volatile var onVoiceHelp: (() -> Boolean)? = null
    @Volatile var onVoiceStatus: (() -> Boolean)? = null

    fun clear() {
        onMuteVoice = null
        onWakeVoice = null
        onStartDictation = null
        onStopDictation = null
        onShowCommands = null
        onVoiceHelp = null
        onVoiceStatus = null
    }
}

class IosVoiceControlHandler : BaseHandler() {

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Mute/Wake
        "mute voice", "stop listening", "go to sleep",
        "wake up", "start listening", "wake up voice",
        // Dictation mode
        "dictation mode", "start dictation", "type what i say",
        "command mode", "stop dictation",
        // Help/Status
        "voice help", "what can i say", "show commands",
        "voice status", "voice info",
        // Numbers mode
        "numbers on", "show numbers",
        "numbers off", "hide numbers",
        "numbers auto"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        NSLog("$TAG.execute: '$phrase', actionType=${command.actionType}")

        return when (command.actionType) {
            CommandActionType.VOICE_MUTE -> invokeCallback(IosVoiceControlCallbacks.onMuteVoice, "Voice muted")
            CommandActionType.VOICE_WAKE -> invokeCallback(IosVoiceControlCallbacks.onWakeVoice, "Voice activated")
            CommandActionType.DICTATION_START -> invokeCallback(IosVoiceControlCallbacks.onStartDictation, "Dictation started")
            CommandActionType.DICTATION_STOP -> invokeCallback(IosVoiceControlCallbacks.onStopDictation, "Dictation stopped")
            CommandActionType.SHOW_COMMANDS -> invokeCallback(IosVoiceControlCallbacks.onShowCommands, "Commands shown")
            CommandActionType.NUMBERS_ON,
            CommandActionType.NUMBERS_OFF,
            CommandActionType.NUMBERS_AUTO -> {
                // Numbers overlay not applicable on iOS (no AccessibilityService overlay)
                NSLog("$TAG: Numbers mode not available on iOS")
                HandlerResult.failure("Numbers overlay not available on iOS", recoverable = true)
            }
            else -> {
                // Fall back to phrase matching for help/status
                when {
                    phrase.contains("help") || phrase.contains("what can i say") ->
                        invokeCallback(IosVoiceControlCallbacks.onVoiceHelp, "Voice help shown")
                    phrase.contains("status") || phrase.contains("info") ->
                        invokeCallback(IosVoiceControlCallbacks.onVoiceStatus, "Voice status shown")
                    else -> HandlerResult.notHandled()
                }
            }
        }
    }

    private fun invokeCallback(callback: (() -> Boolean)?, successMessage: String): HandlerResult {
        if (callback == null) {
            NSLog("$TAG: No callback registered for '$successMessage'")
            return HandlerResult.failure("Voice control callback not registered", recoverable = true)
        }
        return if (callback.invoke()) {
            NSLog("$TAG: $successMessage")
            HandlerResult.success(successMessage)
        } else {
            NSLog("$TAG: Callback returned false for '$successMessage'")
            HandlerResult.failure("Voice control action failed", recoverable = true)
        }
    }
}
