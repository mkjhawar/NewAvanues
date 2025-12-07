/**
 * GestureTypingHandler.kt - Handles gesture/swipe typing functionality
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.gestures

import android.content.Context
import android.graphics.PointF
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles gesture typing (swipe typing) functionality
 * Processes touch path to detect words based on key positions
 * 
 * SOLID Principles:
 * - Single Responsibility: Only handles gesture path processing
 * - Open/Closed: Extensible through callbacks
 * - Dependency Inversion: Depends on word prediction interface
 */
class GestureTypingHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "GestureTypingHandler"
        private const val MIN_GESTURE_LENGTH = 3
        private const val KEY_PROXIMITY_THRESHOLD = 50f
        private const val GESTURE_SAMPLING_RATE = 10 // Sample every 10 pixels
        private const val MAX_WORD_LENGTH = 20
    }
    
    // Coroutine scope for async processing
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Dictionary and word prediction
    private val wordDictionary = mutableSetOf<String>()
    private val commonWords = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their",
        "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me"
    )
    
    // Keyboard layout reference (QWERTY)
    private val keyPositions = mapOf(
        'q' to PointF(0.05f, 0.2f), 'w' to PointF(0.15f, 0.2f), 'e' to PointF(0.25f, 0.2f),
        'r' to PointF(0.35f, 0.2f), 't' to PointF(0.45f, 0.2f), 'y' to PointF(0.55f, 0.2f),
        'u' to PointF(0.65f, 0.2f), 'i' to PointF(0.75f, 0.2f), 'o' to PointF(0.85f, 0.2f),
        'p' to PointF(0.95f, 0.2f),
        
        'a' to PointF(0.1f, 0.4f), 's' to PointF(0.2f, 0.4f), 'd' to PointF(0.3f, 0.4f),
        'f' to PointF(0.4f, 0.4f), 'g' to PointF(0.5f, 0.4f), 'h' to PointF(0.6f, 0.4f),
        'j' to PointF(0.7f, 0.4f), 'k' to PointF(0.8f, 0.4f), 'l' to PointF(0.9f, 0.4f),
        
        'z' to PointF(0.15f, 0.6f), 'x' to PointF(0.25f, 0.6f), 'c' to PointF(0.35f, 0.6f),
        'v' to PointF(0.45f, 0.6f), 'b' to PointF(0.55f, 0.6f), 'n' to PointF(0.65f, 0.6f),
        'm' to PointF(0.75f, 0.6f)
    )
    
    init {
        // Initialize with common words
        wordDictionary.addAll(commonWords)
        loadUserDictionary()
    }
    
    /**
     * Process a gesture path and return predicted word
     */
    fun processGesture(
        points: List<Pair<Float, Float>>,
        callback: (String) -> Unit
    ) {
        if (points.size < MIN_GESTURE_LENGTH) {
            Log.d(TAG, "Gesture too short: ${points.size} points")
            return
        }
        
        scope.launch {
            try {
                // Sample and normalize the path
                val sampledPath = samplePath(points)
                val normalizedPath = normalizePath(sampledPath)
                
                // Extract possible characters from path
                val possibleChars = extractCharactersFromPath(normalizedPath)
                
                // Find matching words
                val candidates = findMatchingWords(possibleChars)
                
                // Score and rank candidates
                val bestMatch = rankCandidates(candidates, normalizedPath)
                
                if (bestMatch != null) {
                    withContext(Dispatchers.Main) {
                        callback(bestMatch)
                    }
                    
                    // Learn from user input
                    learnWord(bestMatch)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing gesture", e)
            }
        }
    }
    
    /**
     * Sample the gesture path at regular intervals
     */
    private fun samplePath(points: List<Pair<Float, Float>>): List<PointF> {
        val sampled = mutableListOf<PointF>()
        var distance = 0f
        
        sampled.add(PointF(points[0].first, points[0].second))
        
        for (i in 1 until points.size) {
            val dx = points[i].first - points[i - 1].first
            val dy = points[i].second - points[i - 1].second
            val segmentDistance = sqrt(dx * dx + dy * dy)
            
            distance += segmentDistance
            
            if (distance >= GESTURE_SAMPLING_RATE) {
                sampled.add(PointF(points[i].first, points[i].second))
                distance = 0f
            }
        }
        
        // Always add the last point
        if (sampled.last() != PointF(points.last().first, points.last().second)) {
            sampled.add(PointF(points.last().first, points.last().second))
        }
        
        return sampled
    }
    
    /**
     * Normalize path coordinates to 0-1 range
     */
    private fun normalizePath(path: List<PointF>): List<PointF> {
        if (path.isEmpty()) return emptyList()
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        
        // Find bounds
        path.forEach { point ->
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }
        
        val width = maxX - minX
        val height = maxY - minY
        
        if (width == 0f || height == 0f) return path
        
        // Normalize to 0-1 range
        return path.map { point ->
            PointF(
                (point.x - minX) / width,
                (point.y - minY) / height
            )
        }
    }
    
    /**
     * Extract possible characters from the gesture path
     */
    private fun extractCharactersFromPath(path: List<PointF>): List<Set<Char>> {
        val possibleChars = mutableListOf<Set<Char>>()
        
        path.forEach { point ->
            val nearbyKeys = findNearbyKeys(point)
            if (nearbyKeys.isNotEmpty()) {
                possibleChars.add(nearbyKeys)
            }
        }
        
        // Deduplicate consecutive identical sets
        return possibleChars.fold(mutableListOf()) { acc, chars ->
            if (acc.isEmpty() || acc.last() != chars) {
                acc.add(chars)
            }
            acc
        }
    }
    
    /**
     * Find keys near a given point
     */
    private fun findNearbyKeys(point: PointF): Set<Char> {
        val nearbyKeys = mutableSetOf<Char>()
        
        keyPositions.forEach { (char, position) ->
            val distance = calculateDistance(point, position)
            if (distance < KEY_PROXIMITY_THRESHOLD / 100f) { // Normalize threshold
                nearbyKeys.add(char)
            }
        }
        
        return nearbyKeys
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Find words matching the possible character sequence
     */
    private fun findMatchingWords(possibleChars: List<Set<Char>>): List<String> {
        if (possibleChars.isEmpty()) return emptyList()
        
        val candidates = mutableListOf<String>()
        
        wordDictionary.forEach { word ->
            if (word.length <= MAX_WORD_LENGTH && matchesPattern(word, possibleChars)) {
                candidates.add(word)
            }
        }
        
        return candidates
    }
    
    /**
     * Check if a word matches the possible character pattern
     */
    private fun matchesPattern(word: String, possibleChars: List<Set<Char>>): Boolean {
        // Simple matching: check if word characters appear in possible chars
        var charIndex = 0
        
        for (char in word) {
            var found = false
            
            // Look for the character in remaining possible char sets
            for (i in charIndex until possibleChars.size) {
                if (char in possibleChars[i]) {
                    charIndex = i + 1
                    found = true
                    break
                }
            }
            
            if (!found) return false
        }
        
        return true
    }
    
    /**
     * Rank candidate words based on various factors
     */
    private fun rankCandidates(
        candidates: List<String>,
        normalizedPath: List<PointF>
    ): String? {
        if (candidates.isEmpty()) return null
        
        // Score each candidate
        val scores = candidates.map { word ->
            var score = 0f
            
            // Prefer common words
            if (word in commonWords) {
                score += 10f
            }
            
            // Prefer shorter words for shorter paths
            val lengthDiff = abs(word.length - normalizedPath.size)
            score -= lengthDiff * 0.5f
            
            // Calculate path similarity
            score += calculatePathSimilarity(word, normalizedPath)
            
            word to score
        }
        
        // Return the best scoring word
        return scores.maxByOrNull { it.second }?.first
    }
    
    /**
     * Calculate how similar a word's key path is to the gesture path
     */
    private fun calculatePathSimilarity(word: String, path: List<PointF>): Float {
        val wordPath = word.mapNotNull { keyPositions[it] }
        if (wordPath.isEmpty()) return 0f
        
        var similarity = 0f
        val minLength = minOf(wordPath.size, path.size)
        
        for (i in 0 until minLength) {
            val distance = calculateDistance(wordPath[i], path[i])
            similarity += (1f - minOf(distance, 1f)) // Inverse distance as similarity
        }
        
        // Penalize length mismatch
        similarity -= abs(wordPath.size - path.size) * 0.2f
        
        return similarity
    }
    
    /**
     * Learn from user input to improve predictions
     */
    private fun learnWord(word: String) {
        wordDictionary.add(word.lowercase())
        
        // Persist to user dictionary
        saveUserDictionary(word)
    }
    
    /**
     * Load user dictionary from storage
     */
    private fun loadUserDictionary() {
        // TODO: Load from SharedPreferences or database
        // For now, just add some common tech words
        wordDictionary.addAll(listOf(
            "android", "kotlin", "java", "code", "function", "class",
            "variable", "method", "interface", "abstract", "override",
            "public", "private", "protected", "static", "final"
        ))
    }
    
    /**
     * Save word to user dictionary
     */
    private fun saveUserDictionary(word: String) {
        // TODO: Save to SharedPreferences or database
        Log.d(TAG, "Learning new word: $word")
    }
    
    /**
     * Release resources
     */
    fun release() {
        scope.cancel()
    }
}