package com.augmentalis.noteavanue.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NoteDictationManager — tracks voice-dictated content within a note.
 *
 * Responsibilities:
 * 1. Track voice-origin percentage (what % of content was dictated vs typed)
 * 2. Insert `<!-- dictated:start/end -->` HTML comment markers in Markdown
 *    (invisible to the user, survive round-trips through compose-rich-editor)
 * 3. Buffer partial speech results
 *
 * The voice-origin percentage is persisted in the Note.voiceOriginPct field
 * and stored in the database for analytics (smart folder filter: "all dictated notes").
 *
 * Usage:
 * ```kotlin
 * val manager = NoteDictationManager()
 * manager.startDictationSegment()          // User says "dictation mode"
 * manager.onTextDictated("Hello world")    // Speech finalized
 * manager.onTextDictated("More text")      // More speech
 * manager.endDictationSegment()            // User says "command mode"
 *
 * val pct = manager.voiceOriginPct.value   // 0.0–1.0
 * val markers = manager.getDictationMarkers() // Markdown comment pairs
 * ```
 */
class NoteDictationManager {

    private var totalCharsTyped: Int = 0
    private var totalCharsDictated: Int = 0
    private var isInDictationSegment: Boolean = false
    private var currentSegmentChars: Int = 0

    private val _voiceOriginPct = MutableStateFlow(0f)
    /** Percentage of note content from voice dictation (0.0–1.0) */
    val voiceOriginPct: StateFlow<Float> = _voiceOriginPct.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    /** Whether a dictation segment is currently active */
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val segments = mutableListOf<DictationSegment>()

    /**
     * Start a new dictation segment.
     * Inserts a `<!-- dictated:start -->` marker.
     */
    fun startDictationSegment() {
        if (isInDictationSegment) return
        isInDictationSegment = true
        currentSegmentChars = 0
        _isActive.value = true
    }

    /**
     * End the current dictation segment.
     * Inserts a `<!-- dictated:end -->` marker.
     */
    fun endDictationSegment() {
        if (!isInDictationSegment) return
        isInDictationSegment = false
        if (currentSegmentChars > 0) {
            segments.add(DictationSegment(charCount = currentSegmentChars))
        }
        currentSegmentChars = 0
        _isActive.value = false
    }

    /**
     * Record that text was dictated (from finalized speech result).
     *
     * @param text The dictated text content
     */
    fun onTextDictated(text: String) {
        val chars = text.length
        totalCharsDictated += chars
        currentSegmentChars += chars
        updatePercentage()
    }

    /**
     * Record that text was typed manually.
     * Called from the text editor's onChange handler.
     *
     * @param charDelta Number of characters added by typing
     */
    fun onTextTyped(charDelta: Int) {
        if (charDelta > 0) {
            totalCharsTyped += charDelta
            updatePercentage()
        }
    }

    /**
     * Initialize from a previously saved note's stats.
     *
     * @param totalChars Total character count of the note
     * @param voiceOriginPct Previously calculated voice origin percentage
     */
    fun initializeFromNote(totalChars: Int, voiceOriginPct: Float) {
        val dictated = (totalChars * voiceOriginPct).toInt()
        totalCharsDictated = dictated
        totalCharsTyped = totalChars - dictated
        _voiceOriginPct.value = voiceOriginPct
    }

    /**
     * Get Markdown comment markers for all recorded dictation segments.
     * These are inserted into the Markdown content for provenance tracking.
     *
     * @return Pair of (start marker, end marker) strings
     */
    fun getDictationStartMarker(): String = "<!-- dictated:start -->"
    fun getDictationEndMarker(): String = "<!-- dictated:end -->"

    /** Reset all tracking state (e.g., for a new note) */
    fun reset() {
        totalCharsTyped = 0
        totalCharsDictated = 0
        currentSegmentChars = 0
        isInDictationSegment = false
        segments.clear()
        _voiceOriginPct.value = 0f
        _isActive.value = false
    }

    private fun updatePercentage() {
        val total = totalCharsTyped + totalCharsDictated
        _voiceOriginPct.value = if (total > 0) {
            (totalCharsDictated.toFloat() / total).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}

/**
 * Record of a completed dictation segment.
 */
internal data class DictationSegment(
    val charCount: Int
)
