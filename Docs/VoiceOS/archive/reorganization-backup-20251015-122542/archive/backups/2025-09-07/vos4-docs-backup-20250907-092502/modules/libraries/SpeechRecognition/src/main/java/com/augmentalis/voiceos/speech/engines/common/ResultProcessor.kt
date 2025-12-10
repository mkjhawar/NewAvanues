/**
 * ResultProcessor.kt - Unified result processing for all engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.voiceos.speech.engines.common

import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.SpeechEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Processes and normalizes recognition results from all engines.
 * Provides consistent result handling, filtering, and confidence validation.
 */
class ResultProcessor(
    private val commandCache: CommandCache = CommandCache()
) {
    // Result flow for observers
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()
    
    // Configuration
    private var confidenceThreshold: Float = 0.7f
    private var currentMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND
    
    // Statistics
    private var processedCount: Long = 0
    private var acceptedCount: Long = 0
    private var rejectedCount: Long = 0
    
    // Duplicate detection
    private var lastResultText: String? = null
    private var lastResultTime: Long = 0
    private val DUPLICATE_WINDOW_MS = 500L // 500ms window for duplicate detection
    
    /**
     * Process raw recognition result from engine
     * Applies filtering, validation, and normalization
     */
    suspend fun processResult(
        text: String,
        confidence: Float,
        engine: SpeechEngine,
        isPartial: Boolean = false,
        alternatives: List<String> = emptyList()
    ): RecognitionResult? {
        processedCount++
        
        // Skip empty results
        if (text.isBlank()) {
            rejectedCount++
            return null
        }
        
        // Check for duplicate within time window (for final results only)
        if (!isPartial) {
            val currentTime = System.currentTimeMillis()
            if (text == lastResultText && 
                (currentTime - lastResultTime) < DUPLICATE_WINDOW_MS) {
                rejectedCount++
                return null // Skip duplicate
            }
            lastResultText = text
            lastResultTime = currentTime
        }
        
        // Apply confidence threshold (except for partial results)
        if (!isPartial && confidence < confidenceThreshold) {
            rejectedCount++
            return null
        }
        
        // Normalize text
        val normalizedText = normalizeText(text)
        
        // Process based on mode
        val finalText = when (currentMode) {
            SpeechMode.STATIC_COMMAND,
            SpeechMode.DYNAMIC_COMMAND -> {
                // For command modes, try to match with cache
                commandCache.findMatch(normalizedText) ?: normalizedText
            }
            SpeechMode.DICTATION,
            SpeechMode.FREE_SPEECH -> {
                // For dictation/free speech, return as-is
                normalizedText
            }
            SpeechMode.HYBRID -> {
                // For hybrid mode, try command matching first, fallback to normalized
                commandCache.findMatch(normalizedText) ?: normalizedText
            }
        }
        
        // Create result
        val result = RecognitionResult(
            text = finalText,
            originalText = text,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            isPartial = isPartial,
            isFinal = !isPartial,
            alternatives = alternatives.map { normalizeText(it) },
            engine = engine.name,
            mode = currentMode.name
        )
        
        // Emit result
        if (!isPartial) {
            acceptedCount++
            _resultFlow.emit(result)
        }
        
        return result
    }
    
    /**
     * Process batch of results (for engines that return multiple hypotheses)
     */
    suspend fun processBatch(
        results: List<Pair<String, Float>>,
        engine: SpeechEngine,
        isPartial: Boolean = false
    ): RecognitionResult? {
        if (results.isEmpty()) {
            rejectedCount++
            return null
        }
        
        // Take best result (highest confidence)
        val best = results.maxByOrNull { it.second } ?: return null
        val alternatives = results
            .filter { it != best }
            .map { it.first }
            .take(5) // Limit alternatives to 5
        
        return processResult(
            text = best.first,
            confidence = best.second,
            engine = engine,
            isPartial = isPartial,
            alternatives = alternatives
        )
    }
    
    /**
     * Normalize text for consistent processing
     * Made public for engine access
     */
    fun normalizeText(text: String): String {
        // For dictation/free speech modes, preserve original case
        val preserveCase = currentMode in listOf(SpeechMode.DICTATION, SpeechMode.FREE_SPEECH)
        
        return text
            .trim()
            .replace(Regex("\\s+"), " ") // Collapse multiple spaces
            .let { if (preserveCase) it else it.lowercase() } // Only lowercase for commands
    }
    
    /**
     * Find best match from alternatives (for engine compatibility)
     */
    fun findBestMatch(text: String, alternatives: List<String>): String? {
        val normalized = normalizeText(text)
        // Try to find in command cache first
        commandCache.findMatch(normalized)?.let { return it }
        // Otherwise check alternatives
        return alternatives.firstOrNull { normalizeText(it) == normalized }
    }
    
    /**
     * Create result object (for engine compatibility)
     */
    fun createResult(
        text: String,
        confidence: Float = 1.0f,
        engine: String = "unknown",
        mode: String = currentMode.name
    ): RecognitionResult {
        return RecognitionResult(
            text = text,
            originalText = text,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            isPartial = false,
            isFinal = true,
            alternatives = emptyList(),
            engine = engine,
            mode = mode
        )
    }
    
    /**
     * Create error result (for engine compatibility)
     */
    fun createErrorResult(error: String, engine: String = "unknown"): RecognitionResult {
        return RecognitionResult(
            text = "",
            originalText = "",
            confidence = 0f,
            timestamp = System.currentTimeMillis(),
            isPartial = false,
            isFinal = true,
            alternatives = emptyList(),
            engine = engine,
            mode = currentMode.name,
            metadata = mapOf("error" to error)
        )
    }
    
    /**
     * Update confidence threshold
     */
    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold.coerceIn(0f, 1f)
    }
    
    /**
     * Update current recognition mode
     */
    fun setMode(mode: SpeechMode) {
        currentMode = mode
    }
    
    /**
     * Get current mode
     */
    fun getMode(): SpeechMode = currentMode
    
    /**
     * Get current confidence threshold
     */
    fun getConfidenceThreshold(): Float = confidenceThreshold
    
    /**
     * Get processing statistics
     */
    fun getStatistics(): ProcessingStats {
        return ProcessingStats(
            totalProcessed = processedCount,
            totalAccepted = acceptedCount,
            totalRejected = rejectedCount,
            acceptanceRate = if (processedCount > 0) {
                (acceptedCount.toFloat() / processedCount.toFloat()) * 100f
            } else 0f
        )
    }
    
    /**
     * Reset statistics
     */
    fun resetStatistics() {
        processedCount = 0
        acceptedCount = 0
        rejectedCount = 0
    }
    
    /**
     * Clear cache and reset state
     */
    /**
     * Check if a result should be accepted (duplicate detection)
     */
    fun shouldAccept(result: RecognitionResult): Boolean {
        // Skip empty results
        if (result.text.isBlank()) {
            return false
        }
        
        // Check for duplicate within time window (for final results only)
        if (result.isFinal) {
            val currentTime = System.currentTimeMillis()
            if (result.text == lastResultText && 
                (currentTime - lastResultTime) < DUPLICATE_WINDOW_MS) {
                return false // Skip duplicate
            }
            lastResultText = result.text
            lastResultTime = currentTime
        }
        
        // Apply confidence threshold (except for partial results)
        if (result.isFinal && result.confidence < confidenceThreshold) {
            return false
        }
        
        return true
    }
    
    fun clear() {
        commandCache.clear()
        resetStatistics()
        lastResultText = null
        lastResultTime = 0
    }
    
    /**
     * Data class for processing statistics
     */
    data class ProcessingStats(
        val totalProcessed: Long,
        val totalAccepted: Long,
        val totalRejected: Long,
        val acceptanceRate: Float
    )
}