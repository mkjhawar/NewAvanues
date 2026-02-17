package com.augmentalis.noteavanue.voice

import android.util.Log
import com.augmentalis.noteavanue.INoteController
import com.augmentalis.noteavanue.model.NoteVoiceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AndroidNoteVoiceRouter"

/**
 * Android implementation of [INoteVoiceRouter].
 *
 * Routes speech results based on the current voice mode:
 * - COMMANDING: Logs the command (actual dispatch via VoiceOSCore handler pipeline)
 * - DICTATING: Runs NoteFormatDetector → applies formatting → inserts text
 * - CONTINUOUS: Same as DICTATING with auto-restart signaling
 *
 * @param controller The active INoteController for text insertion/formatting
 * @param dictationManager Tracks voice-origin percentage and markers
 */
class AndroidNoteVoiceRouter(
    private val controller: INoteController,
    private val dictationManager: NoteDictationManager
) : INoteVoiceRouter {

    private val _currentMode = MutableStateFlow(NoteVoiceMode.COMMANDING)
    override val currentMode: StateFlow<NoteVoiceMode> = _currentMode.asStateFlow()

    override fun switchMode(mode: NoteVoiceMode) {
        val previous = _currentMode.value
        _currentMode.value = mode
        Log.i(TAG, "Voice mode: $previous → $mode")

        when (mode) {
            NoteVoiceMode.DICTATING, NoteVoiceMode.CONTINUOUS -> {
                dictationManager.startDictationSegment()
            }
            NoteVoiceMode.COMMANDING -> {
                dictationManager.endDictationSegment()
            }
        }
    }

    override suspend fun onSpeechResult(text: String, confidence: Float) {
        Log.d(TAG, "Speech result: '$text' (confidence=$confidence, mode=${_currentMode.value})")

        when (_currentMode.value) {
            NoteVoiceMode.COMMANDING -> {
                // In command mode, speech goes through VoiceOSCore command pipeline
                // (NoteCommandHandler handles it). Nothing to do here — the handler
                // already received the command via the standard dispatch path.
                Log.d(TAG, "Command mode — delegating to VoiceOSCore pipeline")
            }

            NoteVoiceMode.DICTATING, NoteVoiceMode.CONTINUOUS -> {
                // Run format detection on the finalized text
                val formatResult = NoteFormatDetector.detect(text)
                Log.d(TAG, "Format detected: ${formatResult.format}, clean='${formatResult.cleanText}'")

                // Apply detected formatting
                when (formatResult.format) {
                    DetectedFormat.HEADING -> {
                        controller.setHeadingLevel(formatResult.headingLevel)
                        controller.insertText(formatResult.cleanText)
                        controller.insertParagraph()
                        controller.setHeadingLevel(0) // Reset for next line
                    }
                    DetectedFormat.BULLET -> {
                        controller.toggleBulletList()
                        controller.insertText(formatResult.cleanText)
                        controller.insertParagraph()
                    }
                    DetectedFormat.NUMBERED -> {
                        controller.toggleNumberedList()
                        controller.insertText(formatResult.cleanText)
                        controller.insertParagraph()
                    }
                    DetectedFormat.CHECKLIST -> {
                        controller.insertText("- [ ] ${formatResult.cleanText}")
                        controller.insertParagraph()
                    }
                    DetectedFormat.BLOCKQUOTE -> {
                        controller.toggleBlockquote()
                        controller.insertText(formatResult.cleanText)
                        controller.insertParagraph()
                    }
                    DetectedFormat.CODE -> {
                        controller.toggleCodeBlock()
                        controller.insertText(formatResult.cleanText)
                        controller.insertParagraph()
                    }
                    DetectedFormat.PARAGRAPH -> {
                        controller.insertText(formatResult.cleanText + " ")
                    }
                }

                // Track dictated content
                dictationManager.onTextDictated(formatResult.cleanText)
            }
        }
    }

    override fun onPartialResult(partialText: String) {
        // Partial results shown as interim feedback in the editor
        // For now, we don't insert partials — they'd create ghost text
        Log.v(TAG, "Partial: '$partialText'")
    }

    override fun onSpeechError(errorCode: Int) {
        Log.w(TAG, "Speech error: $errorCode (mode=${_currentMode.value})")
        if (_currentMode.value == NoteVoiceMode.CONTINUOUS) {
            // In continuous mode, signal that speech should auto-restart
            Log.i(TAG, "Continuous mode — will auto-restart")
        }
    }

    override fun release() {
        dictationManager.endDictationSegment()
        Log.d(TAG, "Released")
    }
}
