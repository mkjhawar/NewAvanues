package com.augmentalis.voiceoscoreng.functions

/**
 * Cross-platform logging abstraction for VoiceOSCoreNG.
 *
 * Provides consistent logging across all platforms with:
 * - Log level filtering
 * - Tag-based filtering
 * - Structured log messages
 *
 * Platform implementations should set the [logger] property.
 */
object LoggingUtils {

    /**
     * Log levels in order of severity.
     */
    enum class Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * Interface for platform-specific logger implementations.
     */
    interface Logger {
        fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)
    }

    /**
     * Default console logger (prints to stdout).
     */
    private val defaultLogger = object : Logger {
        override fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
            println("${level.name}/$tag: $message")
            throwable?.let { println(it.stackTraceToString()) }
        }
    }

    /**
     * Current logger implementation.
     * Set this to a platform-specific logger (e.g., Android Log, iOS NSLog).
     */
    var logger: Logger = defaultLogger

    /**
     * Minimum log level to output. Messages below this level are ignored.
     */
    var minLevel: Level = Level.DEBUG

    /**
     * Tags to filter (empty = allow all).
     */
    var enabledTags: Set<String> = emptySet()

    /**
     * Default tag used when no tag is specified.
     */
    const val DEFAULT_TAG = "VoiceOSCoreNG"

    // ==================== Logging Methods ====================

    /**
     * Log verbose message.
     */
    fun v(message: String, tag: String = DEFAULT_TAG) {
        log(Level.VERBOSE, tag, message)
    }

    /**
     * Log debug message.
     */
    fun d(message: String, tag: String = DEFAULT_TAG) {
        log(Level.DEBUG, tag, message)
    }

    /**
     * Log info message.
     */
    fun i(message: String, tag: String = DEFAULT_TAG) {
        log(Level.INFO, tag, message)
    }

    /**
     * Log warning message.
     */
    fun w(message: String, tag: String = DEFAULT_TAG, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }

    /**
     * Log error message.
     */
    fun e(message: String, tag: String = DEFAULT_TAG, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }

    /**
     * Log with explicit level.
     */
    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (!shouldLog(level, tag)) return
        logger.log(level, tag, message, throwable)
    }

    /**
     * Check if a message at the given level and tag should be logged.
     */
    fun shouldLog(level: Level, tag: String = DEFAULT_TAG): Boolean {
        if (level.ordinal < minLevel.ordinal) return false
        if (enabledTags.isNotEmpty() && tag !in enabledTags) return false
        return true
    }

    // ==================== Structured Logging ====================

    /**
     * Log element processing event.
     */
    fun logElementProcessing(
        action: String,
        vuid: String? = null,
        elementInfo: String? = null,
        durationMs: Long? = null
    ) {
        val parts = mutableListOf("action=$action")
        vuid?.let { parts.add("vuid=$it") }
        elementInfo?.let { parts.add("element=$it") }
        durationMs?.let { parts.add("duration=${it}ms") }

        d(parts.joinToString(" "), "ElementProcessor")
    }

    /**
     * Log framework detection event.
     */
    fun logFrameworkDetection(
        packageName: String,
        detectedType: String,
        confidence: Float? = null
    ) {
        val msg = buildString {
            append("Detected framework: $detectedType for $packageName")
            confidence?.let { append(" (confidence: ${(it * 100).toInt()}%)") }
        }
        i(msg, "FrameworkDetector")
    }

    /**
     * Log VUID generation event.
     */
    fun logVuidGeneration(
        vuid: String,
        packageHash: String,
        typeCode: Char,
        elementHash: String
    ) {
        d("Generated VUID: $vuid (pkg=$packageHash, type=$typeCode, elem=$elementHash)", "VUIDGenerator")
    }

    /**
     * Log validation failure.
     */
    fun logValidationFailure(
        context: String,
        issues: List<String>
    ) {
        w("Validation failed for $context: ${issues.joinToString("; ")}", "Validation")
    }

    /**
     * Log performance metric.
     */
    fun logPerformance(
        operation: String,
        durationMs: Long,
        itemCount: Int? = null
    ) {
        val msg = buildString {
            append("$operation completed in ${durationMs}ms")
            itemCount?.let { append(" ($it items)") }
        }
        d(msg, "Performance")
    }

    // ==================== Scoped Logging ====================

    /**
     * Create a logger with a fixed tag.
     */
    fun withTag(tag: String): TaggedLogger = TaggedLogger(tag)

    /**
     * Logger with a fixed tag.
     */
    class TaggedLogger(private val tag: String) {
        fun v(message: String) = LoggingUtils.v(message, tag)
        fun d(message: String) = LoggingUtils.d(message, tag)
        fun i(message: String) = LoggingUtils.i(message, tag)
        fun w(message: String, throwable: Throwable? = null) = LoggingUtils.w(message, tag, throwable)
        fun e(message: String, throwable: Throwable? = null) = LoggingUtils.e(message, tag, throwable)
    }

    // ==================== Timing Utilities ====================

    /**
     * Measure and log execution time of a block.
     *
     * @param operation Name of the operation
     * @param tag Log tag
     * @param block The code block to measure
     * @return The result of the block
     */
    inline fun <T> timed(
        operation: String,
        tag: String = "Performance",
        block: () -> T
    ): T {
        val startTime = currentTimeMillis()
        val result = block()
        val duration = currentTimeMillis() - startTime
        d("$operation completed in ${duration}ms", tag)
        return result
    }

    /**
     * Get current time in milliseconds (cross-platform).
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun currentTimeMillis(): Long = getCurrentTimeMillis()
}

/**
 * Expect function for getting current time.
 * Implemented in platform-specific source sets.
 */
expect fun getCurrentTimeMillis(): Long
