package com.augmentalis.voiceoscore.dsl.tooling

import com.augmentalis.voiceoscore.currentTimeMillis
import com.augmentalis.voiceoscore.dsl.interpreter.DispatchLogEntry
import com.augmentalis.voiceoscore.dsl.interpreter.DispatchResult

/**
 * Records dispatch calls and generates AVU DSL (.vos) files.
 *
 * Used for the "workflow recorder" feature: users perform actions manually while
 * the recorder captures them, then generates a .vos file that replays those actions.
 *
 * ## Usage
 * ```kotlin
 * val recorder = WorkflowRecorder("My Workflow")
 * recorder.start()
 * // ... user performs actions, recorder captures dispatch calls ...
 * recorder.recordAction("VCM", mapOf("action" to "open_app", "package" to "com.example"))
 * recorder.recordDelay(1000)
 * recorder.recordAction("AAC", mapOf("action" to "CLICK", "target" to "login"))
 * val vosContent = recorder.stop()
 * ```
 */
class WorkflowRecorder(
    private val workflowName: String,
    private val schemaVersion: String = "avu-2.2"
) {
    private val steps = mutableListOf<RecordedStep>()
    private val codesUsed = mutableSetOf<String>()
    private var isRecording = false
    private var startTimeMs: Long = 0
    private var lastStepTimeMs: Long = 0

    /** Whether the recorder is currently active. */
    val recording: Boolean get() = isRecording

    /** Number of steps recorded so far. */
    val stepCount: Int get() = steps.size

    /** Start recording. Clears any previous recording. */
    fun start() {
        steps.clear()
        codesUsed.clear()
        isRecording = true
        startTimeMs = currentTimeMillis()
        lastStepTimeMs = startTimeMs
    }

    /**
     * Record a code invocation step.
     *
     * @param code The 3-letter code (e.g., "VCM", "AAC")
     * @param arguments Named arguments for the code
     */
    fun recordAction(code: String, arguments: Map<String, Any?>) {
        if (!isRecording) return
        val now = currentTimeMillis()
        val gapMs = now - lastStepTimeMs
        // Auto-insert delay if gap > 200ms
        if (gapMs > 200 && steps.isNotEmpty()) {
            steps.add(RecordedStep.Delay(gapMs))
        }
        codesUsed.add(code)
        steps.add(RecordedStep.Action(code, arguments.toMap()))
        lastStepTimeMs = now
    }

    /**
     * Record an explicit delay step.
     *
     * @param delayMs Delay in milliseconds
     */
    fun recordDelay(delayMs: Long) {
        if (!isRecording) return
        steps.add(RecordedStep.Delay(delayMs))
        lastStepTimeMs = currentTimeMillis()
    }

    /**
     * Record a log/comment step.
     *
     * @param message The message to log
     */
    fun recordComment(message: String) {
        if (!isRecording) return
        steps.add(RecordedStep.Comment(message))
    }

    /**
     * Record from a dispatch log entry (from LoggingDispatcher).
     */
    fun recordFromLog(entry: DispatchLogEntry) {
        if (!isRecording) return
        if (entry.isSuccess) {
            recordAction(entry.code, entry.arguments)
        }
    }

    /**
     * Record all entries from a dispatch log (from LoggingDispatcher).
     */
    fun recordFromLog(entries: List<DispatchLogEntry>) {
        entries.forEach { recordFromLog(it) }
    }

    /**
     * Stop recording and generate the .vos file content.
     *
     * @return Generated AVU DSL source text
     */
    fun stop(): String {
        isRecording = false
        return generate()
    }

    /**
     * Generate .vos content from recorded steps without stopping.
     *
     * @return Generated AVU DSL source text
     */
    fun generate(): String = buildString {
        // Header
        appendLine("---")
        appendLine("schema: $schemaVersion")
        appendLine("version: 1.0.0")
        appendLine("type: workflow")
        if (codesUsed.isNotEmpty()) {
            appendLine("codes:")
            codesUsed.sorted().forEach { code ->
                appendLine("  $code: ${codeDescription(code)}")
            }
        }
        appendLine("---")
        appendLine()

        // Body
        val safeName = workflowName.lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

        appendLine("@workflow \"$workflowName\"")
        for (step in steps) {
            when (step) {
                is RecordedStep.Action -> {
                    val args = step.arguments.entries
                        .filter { it.value != null }
                        .joinToString(", ") { (k, v) ->
                            "$k: ${formatArgValue(v)}"
                        }
                    appendLine("  ${step.code}($args)")
                }
                is RecordedStep.Delay -> {
                    appendLine("  @wait ${step.delayMs}")
                }
                is RecordedStep.Comment -> {
                    appendLine("  # ${step.message}")
                }
            }
        }
    }

    /**
     * Get a summary of the recording.
     */
    fun getSummary(): RecordingSummary {
        val totalDuration = if (startTimeMs > 0) currentTimeMillis() - startTimeMs else 0
        return RecordingSummary(
            workflowName = workflowName,
            stepCount = steps.size,
            codesUsed = codesUsed.toSet(),
            totalDurationMs = totalDuration,
            isRecording = isRecording
        )
    }

    private fun formatArgValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"$value\""
        is Number -> value.toString()
        is Boolean -> value.toString()
        else -> "\"$value\""
    }

    private fun codeDescription(code: String): String = when (code) {
        "VCM" -> "Voice Command"
        "AAC" -> "Accessibility Action"
        "CHT" -> "Chat Message"
        "TTS" -> "Text to Speech"
        "SCR" -> "Screen Control"
        "APP" -> "App Control"
        "NAV" -> "Navigation"
        "SYS" -> "System Command"
        "GES" -> "Gesture"
        "QRY" -> "Query"
        "DRG" -> "Drag Action"
        "EDT" -> "Edit Action"
        "TBL" -> "Table Action"
        "VOL" -> "Volume Control"
        "DIC" -> "Dictation"
        "OVR" -> "Overlay"
        "CLP" -> "Clipboard"
        "KBD" -> "Keyboard"
        else -> code
    }
}

/**
 * A single recorded step.
 */
sealed class RecordedStep {
    data class Action(val code: String, val arguments: Map<String, Any?>) : RecordedStep()
    data class Delay(val delayMs: Long) : RecordedStep()
    data class Comment(val message: String) : RecordedStep()
}

/**
 * Summary of a recording session.
 */
data class RecordingSummary(
    val workflowName: String,
    val stepCount: Int,
    val codesUsed: Set<String>,
    val totalDurationMs: Long,
    val isRecording: Boolean
)
