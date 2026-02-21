/**
 * RecognitionResultTest.kt - Tests for speech recognition result model
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests utility methods, factory functions, and edge cases of RecognitionResult.
 */
package com.augmentalis.speechrecognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecognitionResultTest {

    // ── Factory Methods ──────────────────────────────────────────

    @Test
    fun `empty result has blank text and zero confidence`() {
        val result = RecognitionResult.empty("TestEngine", "test_mode")
        assertTrue(result.isEmpty())
        assertEquals(0f, result.confidence)
        assertEquals("TestEngine", result.engine)
        assertEquals("test_mode", result.mode)
    }

    @Test
    fun `error result contains error in metadata`() {
        val result = RecognitionResult.error("Something went wrong", "Whisper")
        assertTrue(result.isEmpty())
        assertEquals("Something went wrong", result.metadata["error"])
    }

    // ── isEmpty ──────────────────────────────────────────────────

    @Test
    fun `isEmpty returns true for blank text`() {
        assertTrue(RecognitionResult(text = "", confidence = 0.5f).isEmpty())
        assertTrue(RecognitionResult(text = "   ", confidence = 0.5f).isEmpty())
    }

    @Test
    fun `isEmpty returns false for non-blank text`() {
        assertFalse(RecognitionResult(text = "hello", confidence = 0.9f).isEmpty())
    }

    // ── meetsThreshold ───────────────────────────────────────────

    @Test
    fun `meetsThreshold returns true when confidence above threshold`() {
        val result = RecognitionResult(text = "test", confidence = 0.85f)
        assertTrue(result.meetsThreshold(0.7f))
    }

    @Test
    fun `meetsThreshold returns true when confidence equals threshold`() {
        val result = RecognitionResult(text = "test", confidence = 0.7f)
        assertTrue(result.meetsThreshold(0.7f))
    }

    @Test
    fun `meetsThreshold returns false when confidence below threshold`() {
        val result = RecognitionResult(text = "test", confidence = 0.5f)
        assertFalse(result.meetsThreshold(0.7f))
    }

    // ── getBestText ──────────────────────────────────────────────

    @Test
    fun `getBestText returns text when non-blank`() {
        val result = RecognitionResult(text = "hello world", confidence = 0.9f)
        assertEquals("hello world", result.getBestText())
    }

    @Test
    fun `getBestText falls back to first alternative when text blank`() {
        val result = RecognitionResult(
            text = "",
            confidence = 0.5f,
            alternatives = listOf("option A", "option B")
        )
        assertEquals("option A", result.getBestText())
    }

    @Test
    fun `getBestText falls back to originalText when text and alternatives empty`() {
        val result = RecognitionResult(
            text = "",
            originalText = "original",
            confidence = 0.5f,
            alternatives = emptyList()
        )
        assertEquals("original", result.getBestText())
    }

    // ── getBestAlternative ───────────────────────────────────────

    @Test
    fun `getBestAlternative returns first alternative`() {
        val result = RecognitionResult(
            text = "main",
            confidence = 0.9f,
            alternatives = listOf("alt1", "alt2")
        )
        assertEquals("alt1", result.getBestAlternative())
    }

    @Test
    fun `getBestAlternative returns null when no alternatives`() {
        val result = RecognitionResult(text = "main", confidence = 0.9f)
        assertNull(result.getBestAlternative())
    }

    // ── Advanced Features ────────────────────────────────────────

    @Test
    fun `hasAdvancedFeatures returns false when no extras`() {
        val result = RecognitionResult(text = "hello", confidence = 0.9f)
        assertFalse(result.hasAdvancedFeatures())
    }

    @Test
    fun `hasAdvancedFeatures returns true with language`() {
        val result = RecognitionResult(text = "hello", confidence = 0.9f, language = "en")
        assertTrue(result.hasAdvancedFeatures())
    }

    @Test
    fun `hasAdvancedFeatures returns true with translation`() {
        val result = RecognitionResult(text = "hola", confidence = 0.9f, translation = "hello")
        assertTrue(result.hasAdvancedFeatures())
    }

    @Test
    fun `hasAdvancedFeatures returns true with word timestamps`() {
        val timestamps = listOf(WordTimestamp("hello", 0.0f, 0.5f, 0.9f))
        val result = RecognitionResult(text = "hello", confidence = 0.9f, wordTimestamps = timestamps)
        assertTrue(result.hasAdvancedFeatures())
    }

    // ── Word Timestamps ──────────────────────────────────────────

    @Test
    fun `getTotalDuration computes from first to last word`() {
        val timestamps = listOf(
            WordTimestamp("hello", 0.0f, 0.5f, 0.9f),
            WordTimestamp("world", 0.6f, 1.2f, 0.85f),
            WordTimestamp("test", 1.3f, 1.8f, 0.92f)
        )
        val result = RecognitionResult(text = "hello world test", confidence = 0.9f, wordTimestamps = timestamps)
        assertEquals(1.8f, result.getTotalDuration(), 0.01f)
    }

    @Test
    fun `getTotalDuration returns zero when no timestamps`() {
        val result = RecognitionResult(text = "hello", confidence = 0.9f)
        assertEquals(0f, result.getTotalDuration())
    }

    @Test
    fun `getTotalDuration returns zero for empty timestamp list`() {
        val result = RecognitionResult(text = "hello", confidence = 0.9f, wordTimestamps = emptyList())
        assertEquals(0f, result.getTotalDuration())
    }

    @Test
    fun `getHighConfidenceWords filters by threshold`() {
        val timestamps = listOf(
            WordTimestamp("hello", 0.0f, 0.5f, 0.9f),
            WordTimestamp("um", 0.6f, 0.7f, 0.3f),
            WordTimestamp("world", 0.8f, 1.2f, 0.85f)
        )
        val result = RecognitionResult(text = "hello um world", confidence = 0.7f, wordTimestamps = timestamps)
        val high = result.getHighConfidenceWords(0.8f)
        assertEquals(2, high.size)
        assertEquals("hello", high[0].word)
        assertEquals("world", high[1].word)
    }

    @Test
    fun `getHighConfidenceWords returns empty when no timestamps`() {
        val result = RecognitionResult(text = "hello", confidence = 0.9f)
        assertTrue(result.getHighConfidenceWords(0.5f).isEmpty())
    }

    // ── Translation & Language Detection ─────────────────────────

    @Test
    fun `hasTranslation returns true when translation present`() {
        val result = RecognitionResult(text = "bonjour", confidence = 0.9f, translation = "hello")
        assertTrue(result.hasTranslation())
    }

    @Test
    fun `hasTranslation returns false for null or blank`() {
        assertFalse(RecognitionResult(text = "hi", confidence = 0.9f).hasTranslation())
        assertFalse(RecognitionResult(text = "hi", confidence = 0.9f, translation = "").hasTranslation())
    }

    @Test
    fun `hasDetectedLanguage works correctly`() {
        assertTrue(RecognitionResult(text = "hi", confidence = 0.9f, language = "en").hasDetectedLanguage())
        assertFalse(RecognitionResult(text = "hi", confidence = 0.9f).hasDetectedLanguage())
        assertFalse(RecognitionResult(text = "hi", confidence = 0.9f, language = "").hasDetectedLanguage())
    }

    // ── toString ─────────────────────────────────────────────────

    @Test
    fun `toString contains text and engine`() {
        val result = RecognitionResult(text = "hello", confidence = 0.9f, engine = "Whisper")
        val str = result.toString()
        assertTrue(str.contains("hello"))
        assertTrue(str.contains("Whisper"))
    }

    @Test
    fun `toString includes advanced features when present`() {
        val result = RecognitionResult(
            text = "hello",
            confidence = 0.9f,
            language = "en",
            wordTimestamps = listOf(WordTimestamp("hello", 0f, 0.5f, 0.9f))
        )
        val str = result.toString()
        assertTrue(str.contains("lang=en"))
        assertTrue(str.contains("words=1"))
    }
}
