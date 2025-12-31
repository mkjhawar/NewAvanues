package com.augmentalis.voiceoscoreng.functions

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggingUtilsTest {

    private val capturedLogs = mutableListOf<CapturedLog>()

    data class CapturedLog(
        val level: LoggingUtils.Level,
        val tag: String,
        val message: String,
        val throwable: Throwable?
    )

    private val testLogger = object : LoggingUtils.Logger {
        override fun log(
            level: LoggingUtils.Level,
            tag: String,
            message: String,
            throwable: Throwable?
        ) {
            capturedLogs.add(CapturedLog(level, tag, message, throwable))
        }
    }

    @BeforeTest
    fun setup() {
        capturedLogs.clear()
        LoggingUtils.logger = testLogger
        LoggingUtils.minLevel = LoggingUtils.Level.VERBOSE
        LoggingUtils.enabledTags = emptySet()
    }

    @AfterTest
    fun teardown() {
        // Reset to defaults
        LoggingUtils.minLevel = LoggingUtils.Level.DEBUG
        LoggingUtils.enabledTags = emptySet()
    }

    // ==================== Basic Logging Tests ====================

    @Test
    fun `v logs verbose message`() {
        LoggingUtils.v("Verbose message")

        assertEquals(1, capturedLogs.size)
        assertEquals(LoggingUtils.Level.VERBOSE, capturedLogs[0].level)
        assertEquals("Verbose message", capturedLogs[0].message)
        assertEquals(LoggingUtils.DEFAULT_TAG, capturedLogs[0].tag)
    }

    @Test
    fun `d logs debug message`() {
        LoggingUtils.d("Debug message", "CustomTag")

        assertEquals(1, capturedLogs.size)
        assertEquals(LoggingUtils.Level.DEBUG, capturedLogs[0].level)
        assertEquals("Debug message", capturedLogs[0].message)
        assertEquals("CustomTag", capturedLogs[0].tag)
    }

    @Test
    fun `i logs info message`() {
        LoggingUtils.i("Info message")

        assertEquals(1, capturedLogs.size)
        assertEquals(LoggingUtils.Level.INFO, capturedLogs[0].level)
    }

    @Test
    fun `w logs warning with optional throwable`() {
        val exception = Exception("Test error")
        LoggingUtils.w("Warning message", throwable = exception)

        assertEquals(1, capturedLogs.size)
        assertEquals(LoggingUtils.Level.WARN, capturedLogs[0].level)
        assertEquals(exception, capturedLogs[0].throwable)
    }

    @Test
    fun `e logs error with optional throwable`() {
        val exception = RuntimeException("Runtime error")
        LoggingUtils.e("Error message", throwable = exception)

        assertEquals(1, capturedLogs.size)
        assertEquals(LoggingUtils.Level.ERROR, capturedLogs[0].level)
        assertEquals(exception, capturedLogs[0].throwable)
    }

    // ==================== Level Filtering Tests ====================

    @Test
    fun `minLevel filters out lower level logs`() {
        LoggingUtils.minLevel = LoggingUtils.Level.WARN

        LoggingUtils.v("Verbose") // Should be filtered
        LoggingUtils.d("Debug")   // Should be filtered
        LoggingUtils.i("Info")    // Should be filtered
        LoggingUtils.w("Warning") // Should pass
        LoggingUtils.e("Error")   // Should pass

        assertEquals(2, capturedLogs.size)
        assertEquals(LoggingUtils.Level.WARN, capturedLogs[0].level)
        assertEquals(LoggingUtils.Level.ERROR, capturedLogs[1].level)
    }

    @Test
    fun `shouldLog respects minLevel`() {
        LoggingUtils.minLevel = LoggingUtils.Level.INFO

        assertFalse(LoggingUtils.shouldLog(LoggingUtils.Level.VERBOSE))
        assertFalse(LoggingUtils.shouldLog(LoggingUtils.Level.DEBUG))
        assertTrue(LoggingUtils.shouldLog(LoggingUtils.Level.INFO))
        assertTrue(LoggingUtils.shouldLog(LoggingUtils.Level.WARN))
        assertTrue(LoggingUtils.shouldLog(LoggingUtils.Level.ERROR))
    }

    // ==================== Tag Filtering Tests ====================

    @Test
    fun `enabledTags filters to specific tags`() {
        LoggingUtils.enabledTags = setOf("AllowedTag")

        LoggingUtils.d("Message 1", "AllowedTag")    // Should pass
        LoggingUtils.d("Message 2", "BlockedTag")    // Should be filtered

        assertEquals(1, capturedLogs.size)
        assertEquals("AllowedTag", capturedLogs[0].tag)
    }

    @Test
    fun `empty enabledTags allows all tags`() {
        LoggingUtils.enabledTags = emptySet()

        LoggingUtils.d("Message 1", "Tag1")
        LoggingUtils.d("Message 2", "Tag2")

        assertEquals(2, capturedLogs.size)
    }

    // ==================== Structured Logging Tests ====================

    @Test
    fun `logElementProcessing formats correctly`() {
        LoggingUtils.logElementProcessing(
            action = "click",
            vuid = "a3f2e1-b917cc9dc",
            elementInfo = "Button:Submit",
            durationMs = 50
        )

        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("action=click"))
        assertTrue(capturedLogs[0].message.contains("vuid=a3f2e1-b917cc9dc"))
        assertTrue(capturedLogs[0].message.contains("element=Button:Submit"))
        assertTrue(capturedLogs[0].message.contains("duration=50ms"))
    }

    @Test
    fun `logElementProcessing handles optional fields`() {
        LoggingUtils.logElementProcessing(action = "scan")

        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("action=scan"))
        assertFalse(capturedLogs[0].message.contains("vuid="))
    }

    @Test
    fun `logFrameworkDetection formats correctly`() {
        LoggingUtils.logFrameworkDetection(
            packageName = "com.example.app",
            detectedType = "Flutter",
            confidence = 0.95f
        )

        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("Flutter"))
        assertTrue(capturedLogs[0].message.contains("com.example.app"))
        assertTrue(capturedLogs[0].message.contains("95%"))
    }

    @Test
    fun `logVuidGeneration formats correctly`() {
        LoggingUtils.logVuidGeneration(
            vuid = "a3f2e1-b917cc9dc",
            packageHash = "a3f2e1",
            typeCode = 'b',
            elementHash = "917cc9dc"
        )

        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("a3f2e1-b917cc9dc"))
        assertTrue(capturedLogs[0].message.contains("pkg=a3f2e1"))
        assertTrue(capturedLogs[0].message.contains("type=b"))
    }

    @Test
    fun `logValidationFailure formats issues`() {
        LoggingUtils.logValidationFailure(
            context = "ElementInfo",
            issues = listOf("missing className", "invalid bounds")
        )

        assertEquals(1, capturedLogs.size)
        assertEquals(LoggingUtils.Level.WARN, capturedLogs[0].level)
        assertTrue(capturedLogs[0].message.contains("ElementInfo"))
        assertTrue(capturedLogs[0].message.contains("missing className"))
    }

    @Test
    fun `logPerformance formats with item count`() {
        LoggingUtils.logPerformance(
            operation = "Element scan",
            durationMs = 150,
            itemCount = 42
        )

        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("Element scan"))
        assertTrue(capturedLogs[0].message.contains("150ms"))
        assertTrue(capturedLogs[0].message.contains("42 items"))
    }

    // ==================== Tagged Logger Tests ====================

    @Test
    fun `withTag creates logger with fixed tag`() {
        val logger = LoggingUtils.withTag("MyComponent")

        logger.d("Message 1")
        logger.i("Message 2")

        assertEquals(2, capturedLogs.size)
        assertTrue(capturedLogs.all { it.tag == "MyComponent" })
    }

    @Test
    fun `TaggedLogger supports all log levels`() {
        val logger = LoggingUtils.withTag("Test")

        logger.v("Verbose")
        logger.d("Debug")
        logger.i("Info")
        logger.w("Warning")
        logger.e("Error")

        assertEquals(5, capturedLogs.size)
        assertEquals(LoggingUtils.Level.VERBOSE, capturedLogs[0].level)
        assertEquals(LoggingUtils.Level.DEBUG, capturedLogs[1].level)
        assertEquals(LoggingUtils.Level.INFO, capturedLogs[2].level)
        assertEquals(LoggingUtils.Level.WARN, capturedLogs[3].level)
        assertEquals(LoggingUtils.Level.ERROR, capturedLogs[4].level)
    }

    // ==================== Timing Tests ====================

    @Test
    fun `timed logs execution time`() {
        val result = LoggingUtils.timed("TestOperation") {
            // Simulate some work
            42
        }

        assertEquals(42, result)
        assertEquals(1, capturedLogs.size)
        assertTrue(capturedLogs[0].message.contains("TestOperation"))
        assertTrue(capturedLogs[0].message.contains("ms"))
    }

    @Test
    fun `timed returns block result`() {
        val result = LoggingUtils.timed("Calculate") {
            "Hello, World!"
        }

        assertEquals("Hello, World!", result)
    }

    // ==================== Level Enum Tests ====================

    @Test
    fun `Level ordinals are in correct order`() {
        assertTrue(LoggingUtils.Level.VERBOSE.ordinal < LoggingUtils.Level.DEBUG.ordinal)
        assertTrue(LoggingUtils.Level.DEBUG.ordinal < LoggingUtils.Level.INFO.ordinal)
        assertTrue(LoggingUtils.Level.INFO.ordinal < LoggingUtils.Level.WARN.ordinal)
        assertTrue(LoggingUtils.Level.WARN.ordinal < LoggingUtils.Level.ERROR.ordinal)
    }
}
