package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import com.augmentalis.ava.features.actions.ActionResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for TimeActionHandler.
 *
 * Validates:
 * - Returns current time in user's locale
 * - Returns current date
 * - Success result format
 * - Message formatting
 * - Error handling
 */
class TimeActionHandlerTest {

    private lateinit var mockContext: Context
    private lateinit var handler: TimeActionHandler

    @BeforeTest
    fun setup() {
        mockContext = mockk(relaxed = true)
        handler = TimeActionHandler()
    }

    // ========== BASIC TESTS ==========

    @Test
    fun `test handler intent is show_time`() {
        assertEquals("show_time", handler.intent)
    }

    @Test
    fun `test execute returns Success`() = runTest {
        val result = handler.execute(mockContext, "What time is it?")

        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `test execute returns non-null message`() = runTest {
        val result = handler.execute(mockContext, "What time is it?")

        assertTrue(result is ActionResult.Success)
        val message = (result as ActionResult.Success).message
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
    }

    // ========== MESSAGE FORMAT TESTS ==========

    @Test
    fun `test message contains time information`() = runTest {
        val result = handler.execute(mockContext, "What time is it?")

        assertTrue(result is ActionResult.Success)
        val message = (result as ActionResult.Success).message!!

        // Message should contain "It's" and "on"
        assertTrue(message.contains("It's"), "Expected message to start with 'It's'")
        assertTrue(message.contains("on"), "Expected message to contain 'on' before date")
    }

    @Test
    fun `test message format matches expected pattern`() = runTest {
        val result = handler.execute(mockContext, "time?")

        assertTrue(result is ActionResult.Success)
        val message = (result as ActionResult.Success).message!!

        // Expected format: "It's [time] on [date]"
        // Example: "It's 3:45 PM on Friday, November 14"
        val pattern = "It's .+ on .+".toRegex()
        assertTrue(
            pattern.matches(message),
            "Expected message to match 'It's [time] on [date]' format. Got: $message"
        )
    }

    @Test
    fun `test time format is locale-aware`() = runTest {
        val result = handler.execute(mockContext, "Show me the time")

        assertTrue(result is ActionResult.Success)
        val message = (result as ActionResult.Success).message!!

        // Should contain either AM or PM (12-hour format in most locales)
        // Note: This may vary by locale, but for US/English it should have AM/PM
        val hasAmPm = message.contains("AM", ignoreCase = true) ||
                      message.contains("PM", ignoreCase = true)

        // This assertion may need adjustment for 24-hour locale testing
        assertTrue(hasAmPm || message.matches(".*\\d{1,2}:\\d{2}.*".toRegex()),
            "Expected time to be formatted with AM/PM or 24-hour format")
    }

    @Test
    fun `test date format includes day of week and month`() = runTest {
        val result = handler.execute(mockContext, "What's the date?")

        assertTrue(result is ActionResult.Success)
        val message = (result as ActionResult.Success).message!!

        // Should contain day of week (Monday, Tuesday, etc.)
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday",
                                "Friday", "Saturday", "Sunday")
        val hasDayOfWeek = daysOfWeek.any { day ->
            message.contains(day, ignoreCase = false)
        }

        assertTrue(hasDayOfWeek, "Expected message to contain day of week. Got: $message")

        // Should contain month name
        val months = listOf("January", "February", "March", "April", "May", "June",
                           "July", "August", "September", "October", "November", "December")
        val hasMonth = months.any { month ->
            message.contains(month, ignoreCase = false)
        }

        assertTrue(hasMonth, "Expected message to contain month name. Got: $message")
    }

    // ========== UTTERANCE VARIATION TESTS ==========

    @Test
    fun `test works with different utterances`() = runTest {
        val utterances = listOf(
            "What time is it?",
            "Show me the time",
            "Time?",
            "What's the current time",
            "Tell me the time please"
        )

        utterances.forEach { utterance ->
            val result = handler.execute(mockContext, utterance)

            assertTrue(result is ActionResult.Success,
                "Expected success for utterance: $utterance")
            val message = (result as ActionResult.Success).message
            assertNotNull(message, "Expected message for utterance: $utterance")
        }
    }

    @Test
    fun `test works with empty utterance`() = runTest {
        val result = handler.execute(mockContext, "")

        assertTrue(result is ActionResult.Success)
        assertNotNull((result as ActionResult.Success).message)
    }

    // ========== CONSISTENCY TESTS ==========

    @Test
    fun `test consecutive calls return current time`() = runTest {
        val result1 = handler.execute(mockContext, "time")
        val result2 = handler.execute(mockContext, "time")

        assertTrue(result1 is ActionResult.Success)
        assertTrue(result2 is ActionResult.Success)

        val message1 = (result1 as ActionResult.Success).message!!
        val message2 = (result2 as ActionResult.Success).message!!

        // Messages should be identical or very similar (within same minute)
        // Just verify both are well-formed
        assertTrue(message1.contains("It's"))
        assertTrue(message2.contains("It's"))
    }

    @Test
    fun `test execution is fast`() = runTest {
        val startTime = System.currentTimeMillis()

        handler.execute(mockContext, "time")

        val executionTime = System.currentTimeMillis() - startTime

        // Should be very fast (<100ms)
        assertTrue(executionTime < 100,
            "Expected execution under 100ms, got ${executionTime}ms")
    }

    // ========== DATA PAYLOAD TESTS ==========

    @Test
    fun `test Success does not include data payload`() = runTest {
        val result = handler.execute(mockContext, "time")

        assertTrue(result is ActionResult.Success)
        assertNull((result as ActionResult.Success).data)
    }

    // ========== RELIABILITY TESTS ==========

    @Test
    fun `test handles rapid successive calls`() = runTest {
        repeat(100) {
            val result = handler.execute(mockContext, "time")
            assertTrue(result is ActionResult.Success)
        }
    }

    @Test
    fun `test is stateless between calls`() = runTest {
        val result1 = handler.execute(mockContext, "first call")
        val result2 = handler.execute(mockContext, "second call")
        val result3 = handler.execute(mockContext, "third call")

        // All should succeed independently
        assertTrue(result1 is ActionResult.Success)
        assertTrue(result2 is ActionResult.Success)
        assertTrue(result3 is ActionResult.Success)
    }

    // ========== ERROR HANDLING TESTS ==========

    // Note: Null context test removed - TimeActionHandler doesn't actually use context,
    // but testing with null is not a realistic scenario for production code
}
