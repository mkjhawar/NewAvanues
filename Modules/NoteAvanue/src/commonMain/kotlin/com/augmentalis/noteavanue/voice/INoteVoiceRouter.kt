package com.augmentalis.noteavanue.voice

import com.augmentalis.noteavanue.model.NoteVoiceMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic voice routing interface for NoteAvanue.
 *
 * Routes speech recognition results based on the current [NoteVoiceMode]:
 * - COMMANDING: Speech is parsed as voice commands (bold, heading, save)
 * - DICTATING: Speech is inserted as rich text content
 * - CONTINUOUS: Like DICTATING but auto-resumes after silence
 *
 * The router bridges between the speech recognition layer (platform-specific)
 * and the note controller (INoteController).
 */
interface INoteVoiceRouter {

    /** Current voice mode */
    val currentMode: StateFlow<NoteVoiceMode>

    /**
     * Switch to a new voice mode.
     * In COMMANDING mode, the router passes speech to the command pipeline.
     * In DICTATING/CONTINUOUS mode, it passes speech to the dictation manager.
     */
    fun switchMode(mode: NoteVoiceMode)

    /**
     * Route a finalized speech result.
     *
     * In COMMANDING mode: parses as a note command and executes via the handler pipeline.
     * In DICTATING mode: runs through NoteFormatDetector, applies formatting, inserts text.
     * In CONTINUOUS mode: same as DICTATING, but signals the speech engine to auto-restart.
     *
     * @param text Finalized speech recognition text
     * @param confidence Recognition confidence (0.0–1.0)
     */
    suspend fun onSpeechResult(text: String, confidence: Float)

    /**
     * Handle partial (in-progress) speech recognition results.
     * Shows interim text in the editor for visual feedback.
     *
     * @param partialText Partial recognition text
     */
    fun onPartialResult(partialText: String)

    /**
     * Called when speech recognition encounters an error.
     * In CONTINUOUS mode, this triggers auto-restart.
     */
    fun onSpeechError(errorCode: Int)

    /** Release resources */
    fun release()
}
