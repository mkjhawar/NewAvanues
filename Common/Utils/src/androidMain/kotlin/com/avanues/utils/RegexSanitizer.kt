/**
 * RegexSanitizer.kt - Safe regex pattern validation and sanitization
 *
 * YOLO Phase 3 - Medium Priority Issue #28: Missing Input Sanitization on Regex
 *
 * Problem Solved:
 * - ReDoS (Regular Expression Denial of Service) attacks
 * - Malicious regex patterns causing infinite loops/crashes
 * - No timeout protection for regex matching
 * - Unbounded pattern complexity
 *
 * Solution:
 * - Pattern complexity validation
 * - Regex timeout enforcement
 * - Dangerous pattern detection
 * - Safe pattern compilation with error handling
 * - Thread-safe pattern caching
 *
 * Usage:
 * ```kotlin
 * val sanitizer = RegexSanitizer()
 *
 * val result = sanitizer.safeMatch(userInput, userPattern)
 * when (result) {
 *     is RegexResult.Match -> processMatches(result.matches)
 *     is RegexResult.NoMatch -> handleNoMatch()
 *     is RegexResult.Invalid -> showError(result.reason)
 *     is RegexResult.Timeout -> showTimeoutError()
 * }
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Code Quality Expert Agent)
 * Created: 2025-11-09
 */
package com.avanues.utils

import com.augmentalis.voiceos.constants.VoiceOSConstants.Validation
import java.util.concurrent.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * RegexSanitizer - Safe regex pattern validation and execution
 *
 * Protects against ReDoS attacks and malicious patterns by validating
 * complexity, enforcing timeouts, and detecting dangerous patterns.
 *
 * Features:
 * - Pattern complexity validation
 * - Timeout enforcement (1000ms default)
 * - Dangerous pattern detection
 * - Safe pattern compilation
 * - Thread-safe pattern caching
 * - Comprehensive error handling
 */
class RegexSanitizer(
    private val maxPatternLength: Int = Validation.MAX_REGEX_PATTERN_LENGTH,
    private val timeoutMs: Long = Validation.REGEX_TIMEOUT_MS,
    private val enableCaching: Boolean = true
) {

    companion object {
        private const val TAG = "RegexSanitizer"

        /** Dangerous regex patterns that can cause ReDoS */
        private val DANGEROUS_PATTERNS = listOf(
            Regex("""(\(.*\)\+)+"""), // Nested quantifiers: (a+)+
            Regex("""(\(.*\)\*)+"""), // Nested quantifiers: (a*)*
            Regex("""(\(.*\)\{.*})+"""), // Nested bounded quantifiers
            Regex("""(.+\*)+"""), // Repeated greedy quantifiers
            Regex("""(.+\+)+"""), // Repeated possessive quantifiers
            Regex("""(\.|.+)\{[0-9]{4,},""") // Large min repeats: .{10000,}
        )

        /** Maximum allowed nesting depth */
        private const val MAX_NESTING_DEPTH = 10

        /** Maximum allowed alternations */
        private const val MAX_ALTERNATIONS = 50
    }

    /**
     * Result of regex operation
     */
    sealed class RegexResult {
        /** Pattern matched successfully */
        data class Match(val matches: List<String>) : RegexResult()

        /** Pattern did not match */
        object NoMatch : RegexResult()

        /** Pattern is invalid or dangerous */
        data class Invalid(val reason: String) : RegexResult()

        /** Pattern matching timed out */
        object Timeout : RegexResult()

        /** Exception occurred during matching */
        data class Error(val exception: Exception) : RegexResult()
    }

    /** Thread pool for timeout enforcement */
    private val executor: ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "RegexSanitizer-Timeout").apply {
            isDaemon = true
        }
    }

    /** Pattern cache (thread-safe) */
    private val patternCache = ConcurrentHashMap<String, Pattern>()

    /**
     * Safely match input against regex pattern
     *
     * Validates pattern safety, compiles with timeout, and executes match.
     *
     * @param input Input string to match against
     * @param pattern Regex pattern (potentially untrusted)
     * @param flags Regex flags (e.g., Pattern.CASE_INSENSITIVE)
     * @return Result indicating match, no match, invalid, timeout, or error
     */
    fun safeMatch(
        input: String,
        pattern: String,
        flags: Int = 0
    ): RegexResult {
        // Validate input length
        if (input.length > Validation.MAX_INPUT_LENGTH) {
            return RegexResult.Invalid("Input too long (max ${Validation.MAX_INPUT_LENGTH} chars)")
        }

        // Validate pattern
        val validationResult = validatePattern(pattern)
        if (validationResult != null) {
            return RegexResult.Invalid(validationResult)
        }

        // Compile pattern (with caching)
        val compiledPattern = try {
            compilePattern(pattern, flags)
        } catch (e: PatternSyntaxException) {
            return RegexResult.Invalid("Invalid regex syntax: ${e.description}")
        } catch (e: Exception) {
            return RegexResult.Error(e)
        }

        // Execute match with timeout
        return try {
            val future = executor.submit(Callable {
                performMatch(compiledPattern, input)
            })

            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                future.cancel(true)
                ConditionalLogger.w(TAG) {
                    "Regex timeout after ${timeoutMs}ms: pattern=$pattern"
                }
                RegexResult.Timeout
            } catch (e: ExecutionException) {
                ConditionalLogger.w(TAG) {
                    "Regex execution error: ${e.cause?.message}"
                }
                RegexResult.Error(Exception(e.cause))
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                RegexResult.Error(e)
            }
        } catch (e: Exception) {
            RegexResult.Error(e)
        }
    }

    /**
     * Safely test if pattern matches input
     *
     * Simpler version that just returns boolean.
     *
     * @param input Input string
     * @param pattern Regex pattern
     * @param flags Regex flags
     * @return true if matches, false otherwise (including invalid/timeout)
     */
    fun safeMatches(input: String, pattern: String, flags: Int = 0): Boolean {
        return when (safeMatch(input, pattern, flags)) {
            is RegexResult.Match -> true
            else -> false
        }
    }

    /**
     * Safely find all matches
     *
     * @param input Input string
     * @param pattern Regex pattern
     * @param flags Regex flags
     * @return List of matches, or empty list on error/timeout
     */
    fun safeFindAll(input: String, pattern: String, flags: Int = 0): List<String> {
        return when (val result = safeMatch(input, pattern, flags)) {
            is RegexResult.Match -> result.matches
            else -> emptyList()
        }
    }

    /**
     * Validate pattern safety
     *
     * Checks for dangerous patterns, complexity, and length.
     *
     * @param pattern Pattern to validate
     * @return Error message if invalid, null if valid
     */
    private fun validatePattern(pattern: String): String? {
        // Check length
        if (pattern.length > maxPatternLength) {
            return "Pattern too long (max $maxPatternLength chars)"
        }

        // Check for dangerous patterns
        for (dangerousPattern in DANGEROUS_PATTERNS) {
            if (dangerousPattern.containsMatchIn(pattern)) {
                return "Pattern contains dangerous construct (potential ReDoS)"
            }
        }

        // Check nesting depth
        val nestingDepth = calculateNestingDepth(pattern)
        if (nestingDepth > MAX_NESTING_DEPTH) {
            return "Pattern nesting too deep (max $MAX_NESTING_DEPTH)"
        }

        // Check alternations
        val alternations = pattern.count { it == '|' }
        if (alternations > MAX_ALTERNATIONS) {
            return "Too many alternations (max $MAX_ALTERNATIONS)"
        }

        return null
    }

    /**
     * Calculate nesting depth of pattern
     *
     * Counts maximum depth of nested groups.
     *
     * @param pattern Pattern to analyze
     * @return Maximum nesting depth
     */
    private fun calculateNestingDepth(pattern: String): Int {
        var maxDepth = 0
        var currentDepth = 0
        var escaped = false

        for (char in pattern) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '(' -> {
                    currentDepth++
                    if (currentDepth > maxDepth) {
                        maxDepth = currentDepth
                    }
                }
                char == ')' -> currentDepth = maxOf(0, currentDepth - 1)
            }
        }

        return maxDepth
    }

    /**
     * Compile pattern with caching
     *
     * @param pattern Pattern string
     * @param flags Regex flags
     * @return Compiled Pattern object
     */
    private fun compilePattern(pattern: String, flags: Int): Pattern {
        if (!enableCaching) {
            return Pattern.compile(pattern, flags)
        }

        val cacheKey = "$pattern:$flags"

        return patternCache.getOrPut(cacheKey) {
            Pattern.compile(pattern, flags)
        }
    }

    /**
     * Perform actual pattern matching
     *
     * Executed in timeout-controlled thread.
     *
     * @param pattern Compiled pattern
     * @param input Input string
     * @return Result of match
     */
    private fun performMatch(pattern: Pattern, input: String): RegexResult {
        val matcher = pattern.matcher(input)

        return if (matcher.find()) {
            val matches = mutableListOf<String>()
            do {
                matches.add(matcher.group())
            } while (matcher.find())

            RegexResult.Match(matches)
        } else {
            RegexResult.NoMatch
        }
    }

    /**
     * Sanitize user input by escaping special regex characters
     *
     * Use when user input should be treated as literal string in regex.
     *
     * @param input User input string
     * @return Escaped string safe for use in regex
     */
    fun escapeRegex(input: String): String {
        return Pattern.quote(input)
    }

    /**
     * Clear pattern cache
     *
     * Call periodically to prevent unbounded cache growth.
     */
    fun clearCache() {
        patternCache.clear()
        ConditionalLogger.d(TAG) { "Cleared regex pattern cache" }
    }

    /**
     * Get cache metrics
     *
     * @return Map of cache statistics
     */
    fun getCacheMetrics(): Map<String, Any> {
        return mapOf(
            "cacheSize" to patternCache.size,
            "cachingEnabled" to enableCaching
        )
    }

    /**
     * Shutdown executor
     *
     * Call when sanitizer no longer needed to clean up threads.
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * Global singleton instance for convenience
 */
object GlobalRegexSanitizer {
    private val instance = RegexSanitizer()

    fun safeMatch(input: String, pattern: String, flags: Int = 0): RegexSanitizer.RegexResult {
        return instance.safeMatch(input, pattern, flags)
    }

    fun safeMatches(input: String, pattern: String, flags: Int = 0): Boolean {
        return instance.safeMatches(input, pattern, flags)
    }

    fun safeFindAll(input: String, pattern: String, flags: Int = 0): List<String> {
        return instance.safeFindAll(input, pattern, flags)
    }

    fun escapeRegex(input: String): String {
        return instance.escapeRegex(input)
    }
}
