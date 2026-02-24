/**
 * ReadingHandler.kt - IHandler for screen reading / TTS commands
 *
 * Handles: read screen, read aloud, stop reading
 * Uses TextToSpeech engine and accessibility tree text extraction.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand
import java.util.Locale

private const val TAG = "ReadingHandler"

class ReadingHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.ACCESSIBILITY

    override val supportedActions: List<String> = listOf(
        "read screen", "read aloud", "read this", "read page",
        "stop reading", "stop", "quiet", "be quiet"
    )

    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false

    override suspend fun initialize() {
        tts = TextToSpeech(service.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                Log.i(TAG, "TTS engine initialized")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    override suspend fun dispose() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "ReadingHandler.execute: '$phrase'")

        return when {
            phrase in listOf("read screen", "read aloud", "read this", "read page") -> readScreen()
            phrase in listOf("stop reading", "stop", "quiet", "be quiet") -> stopReading()
            else -> HandlerResult.notHandled()
        }
    }

    private fun readScreen(): HandlerResult {
        if (!ttsReady || tts == null) {
            return HandlerResult.failure("Text-to-speech not available")
        }

        val rootNode = service.rootInActiveWindow
            ?: return HandlerResult.failure("Cannot access screen content")

        val text = extractScreenText(rootNode)
        if (text.isBlank()) {
            return HandlerResult.failure("No readable text found on screen")
        }

        // Speak the extracted text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "read_screen")
        Log.i(TAG, "Reading screen: ${text.take(100)}...")
        return HandlerResult.success("Reading screen content")
    }

    private fun stopReading(): HandlerResult {
        tts?.stop()
        Log.i(TAG, "Reading stopped")
        return HandlerResult.success("Stopped reading")
    }

    private fun extractScreenText(node: AccessibilityNodeInfo, depth: Int = 0): String {
        if (depth > 15) return "" // Prevent infinite recursion

        val builder = StringBuilder()

        // Add this node's text
        node.text?.let { text ->
            if (text.isNotBlank()) {
                builder.append(text).append(". ")
            }
        }

        // Add content description if no text
        if (node.text.isNullOrBlank()) {
            node.contentDescription?.let { desc ->
                if (desc.isNotBlank()) {
                    builder.append(desc).append(". ")
                }
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            try {
                val child = node.getChild(i) ?: continue
                builder.append(extractScreenText(child, depth + 1))
            } catch (_: Exception) {
                // Skip inaccessible children
            }
        }

        return builder.toString()
    }
}
