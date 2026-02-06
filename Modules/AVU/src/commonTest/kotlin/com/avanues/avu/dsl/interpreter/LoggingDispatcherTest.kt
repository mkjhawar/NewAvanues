package com.avanues.avu.dsl.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggingDispatcherTest {

    private class MockDispatcher(
        private val result: DispatchResult = DispatchResult.Success("ok"),
        private val codes: Set<String> = setOf("VCM", "AAC", "QRY")
    ) : IAvuDispatcher {
        override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult = result
        override fun canDispatch(code: String): Boolean = code in codes
    }

    @Test
    fun logs_dispatch_calls() = runBlockingTest {
        val inner = MockDispatcher()
        val logging = LoggingDispatcher(inner)

        logging.dispatch("VCM", mapOf("action" to "test"))
        logging.dispatch("AAC", mapOf("action" to "CLICK"))

        assertEquals(2, logging.logSize)
        assertEquals("VCM", logging.getLog()[0].code)
        assertEquals("AAC", logging.getLog()[1].code)
    }

    @Test
    fun delegates_to_inner_dispatcher() = runBlockingTest {
        val inner = MockDispatcher(DispatchResult.Success("hello"))
        val logging = LoggingDispatcher(inner)

        val result = logging.dispatch("VCM", emptyMap())
        assertTrue(result is DispatchResult.Success)
        assertEquals("hello", (result as DispatchResult.Success).data)
    }

    @Test
    fun delegates_canDispatch() {
        val inner = MockDispatcher(codes = setOf("VCM"))
        val logging = LoggingDispatcher(inner)

        assertTrue(logging.canDispatch("VCM"))
        assertFalse(logging.canDispatch("XYZ"))
    }

    @Test
    fun log_entries_track_success_and_errors() = runBlockingTest {
        val successDispatcher = MockDispatcher(DispatchResult.Success())
        val logging1 = LoggingDispatcher(successDispatcher)
        logging1.dispatch("VCM", emptyMap())
        assertTrue(logging1.getLog()[0].isSuccess)
        assertFalse(logging1.getLog()[0].isError)

        val errorDispatcher = MockDispatcher(DispatchResult.Error("fail"))
        val logging2 = LoggingDispatcher(errorDispatcher)
        logging2.dispatch("VCM", emptyMap())
        assertFalse(logging2.getLog()[0].isSuccess)
        assertTrue(logging2.getLog()[0].isError)
    }

    @Test
    fun clearLog_empties_the_log() = runBlockingTest {
        val inner = MockDispatcher()
        val logging = LoggingDispatcher(inner)

        logging.dispatch("VCM", emptyMap())
        logging.dispatch("VCM", emptyMap())
        assertEquals(2, logging.logSize)

        logging.clearLog()
        assertEquals(0, logging.logSize)
    }

    @Test
    fun log_entry_toString_is_readable() = runBlockingTest {
        val inner = MockDispatcher(DispatchResult.Success("data"))
        val logging = LoggingDispatcher(inner)
        logging.dispatch("VCM", mapOf("action" to "test"))

        val entry = logging.getLog().first()
        val str = entry.toString()
        assertTrue(str.contains("VCM"))
        assertTrue(str.contains("action"))
        assertTrue(str.contains("\"test\""))
        assertTrue(str.contains("OK"))
    }

    @Test
    fun log_preserves_arguments() = runBlockingTest {
        val inner = MockDispatcher()
        val logging = LoggingDispatcher(inner)
        val args = mapOf("action" to "click", "target" to "btn", "count" to 3)
        logging.dispatch("AAC", args)

        val entry = logging.getLog().first()
        assertEquals("click", entry.arguments["action"])
        assertEquals("btn", entry.arguments["target"])
        assertEquals(3, entry.arguments["count"])
    }
}

// Simple runBlocking for common tests
private fun runBlockingTest(block: suspend () -> Unit) {
    kotlinx.coroutines.test.runTest { block() }
}
