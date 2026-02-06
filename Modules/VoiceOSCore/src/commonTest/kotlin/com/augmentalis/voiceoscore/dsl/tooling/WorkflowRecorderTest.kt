package com.augmentalis.voiceoscore.dsl.tooling

import com.augmentalis.voiceoscore.dsl.interpreter.DispatchLogEntry
import com.augmentalis.voiceoscore.dsl.interpreter.DispatchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkflowRecorderTest {

    @Test
    fun start_resets_state() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        assertTrue(recorder.recording)
        assertEquals(0, recorder.stepCount)
    }

    @Test
    fun recordAction_adds_step() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        recorder.recordAction("VCM", mapOf("action" to "test"))
        assertEquals(1, recorder.stepCount)
    }

    @Test
    fun recordAction_ignores_when_not_recording() {
        val recorder = WorkflowRecorder("Test")
        recorder.recordAction("VCM", mapOf("action" to "test"))
        assertEquals(0, recorder.stepCount)
    }

    @Test
    fun recordDelay_adds_delay_step() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        recorder.recordDelay(1000)
        assertEquals(1, recorder.stepCount)
    }

    @Test
    fun stop_generates_vos_content() {
        val recorder = WorkflowRecorder("Login Flow")
        recorder.start()
        recorder.recordAction("VCM", mapOf("action" to "open_app", "package" to "com.example"))
        recorder.recordDelay(500)
        recorder.recordAction("AAC", mapOf("action" to "CLICK", "target" to "login_btn"))

        val content = recorder.stop()
        assertFalse(recorder.recording)

        assertTrue(content.contains("schema: avu-2.2"))
        assertTrue(content.contains("type: workflow"))
        assertTrue(content.contains("@workflow \"Login Flow\""))
        assertTrue(content.contains("VCM("))
        assertTrue(content.contains("AAC("))
        assertTrue(content.contains("@wait 500"))
    }

    @Test
    fun generate_includes_all_used_codes() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        recorder.recordAction("VCM", mapOf("action" to "a"))
        recorder.recordAction("AAC", mapOf("action" to "b"))
        recorder.recordAction("CHT", mapOf("text" to "c"))

        val content = recorder.generate()
        assertTrue(content.contains("codes:"))
        assertTrue(content.contains("AAC:"))
        assertTrue(content.contains("CHT:"))
        assertTrue(content.contains("VCM:"))
    }

    @Test
    fun recordComment_adds_comment_step() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        recorder.recordComment("This is a note")
        recorder.recordAction("VCM", mapOf("action" to "test"))

        val content = recorder.stop()
        assertTrue(content.contains("# This is a note"))
    }

    @Test
    fun recordFromLog_records_successful_entries() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()

        val entries = listOf(
            DispatchLogEntry("VCM", mapOf("action" to "a"), DispatchResult.Success(), 1L),
            DispatchLogEntry("AAC", mapOf("action" to "b"), DispatchResult.Error("fail"), 2L),
            DispatchLogEntry("CHT", mapOf("text" to "c"), DispatchResult.Success(), 3L)
        )
        recorder.recordFromLog(entries)

        // Only successful entries should be recorded
        val content = recorder.generate()
        assertTrue(content.contains("VCM("))
        assertTrue(content.contains("CHT("))
        // AAC had an error, should not be in body codes
        assertFalse("AAC" in content.substringAfter("@workflow"))
    }

    @Test
    fun getSummary_returns_accurate_info() {
        val recorder = WorkflowRecorder("Summary Test")
        recorder.start()
        recorder.recordAction("VCM", mapOf("action" to "x"))
        recorder.recordAction("AAC", mapOf("action" to "y"))

        val summary = recorder.getSummary()
        assertEquals("Summary Test", summary.workflowName)
        assertEquals(2, summary.stepCount)
        assertTrue("VCM" in summary.codesUsed)
        assertTrue("AAC" in summary.codesUsed)
        assertTrue(summary.isRecording)
    }

    @Test
    fun start_clears_previous_recording() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        recorder.recordAction("VCM", mapOf("action" to "a"))
        recorder.recordAction("AAC", mapOf("action" to "b"))
        assertEquals(2, recorder.stepCount)

        recorder.start()
        assertEquals(0, recorder.stepCount)
    }

    @Test
    fun generate_formats_string_values_with_quotes() {
        val recorder = WorkflowRecorder("Test")
        recorder.start()
        recorder.recordAction("VCM", mapOf("action" to "test", "count" to 5, "flag" to true))

        val content = recorder.generate()
        assertTrue(content.contains("\"test\""))
        assertTrue(content.contains("count: 5"))
        assertTrue(content.contains("flag: true"))
    }
}
