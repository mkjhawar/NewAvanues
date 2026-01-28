/**
 * ProgressHandler.kt - Voice handler for Progress indicator interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven progress indicator interaction and announcements
 * Features:
 * - Announce current progress status
 * - Announce remaining progress and estimated time
 * - Cancel, pause, resume, and retry operations
 * - Get detailed progress information
 * - Named progress targeting (e.g., "what's the download progress")
 * - Focused progress targeting (e.g., "what's the progress")
 * - AVID-based targeting for precise element selection
 * - Voice feedback for progress state changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Status queries:
 * - "progress" / "status" / "what's the progress" - Announce current progress
 * - "how much left" / "remaining" - Announce remaining progress
 * - "time left" / "eta" - Announce estimated time remaining
 * - "details" / "more info" - Get detailed progress info
 *
 * Operation control:
 * - "cancel" / "stop" - Cancel operation (if cancellable)
 * - "pause" / "wait" - Pause operation (if pausable)
 * - "resume" / "continue" - Resume paused operation
 * - "retry" / "try again" - Retry failed operation
 *
 * Named targeting:
 * - "what's the [name] progress" - Query named progress indicator
 * - "cancel [name]" - Cancel named operation
 * - "pause [name]" - Pause named operation
 *
 * ## Progress Formatting
 *
 * Supports:
 * - Percentage display: "50%", "75 percent complete"
 * - Time formatting: "2 minutes remaining", "about 30 seconds left"
 * - Indeterminate state: "Loading...", "Please wait"
 * - Detailed info: progress value, bounds, state, message
 */

package com.augmentalis.avamagic.voice.handlers.display

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Progress indicator interactions.
 *
 * Provides comprehensive voice control for progress components including:
 * - Status announcements (current progress, remaining, ETA)
 * - Operation control (cancel, pause, resume, retry)
 * - Detailed progress information retrieval
 * - Named progress targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for progress operations
 */
class ProgressHandler(
    private val executor: ProgressExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ProgressHandler"

        // Patterns for parsing commands with optional name targeting
        private val PROGRESS_STATUS_PATTERN = Regex(
            """^(?:what'?s?\s+(?:the\s+)?)?(?:(.+?)\s+)?(?:progress|status)$""",
            RegexOption.IGNORE_CASE
        )

        private val REMAINING_PATTERN = Regex(
            """^(?:how\s+much\s+)?(?:(.+?)\s+)?(?:left|remaining)$""",
            RegexOption.IGNORE_CASE
        )

        private val ETA_PATTERN = Regex(
            """^(?:(.+?)\s+)?(?:time\s+left|eta|estimated\s+time)$""",
            RegexOption.IGNORE_CASE
        )

        private val CANCEL_PATTERN = Regex(
            """^(?:cancel|stop)\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )

        private val PAUSE_PATTERN = Regex(
            """^(?:pause|wait)\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )

        private val RESUME_PATTERN = Regex(
            """^(?:resume|continue)\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )

        private val RETRY_PATTERN = Regex(
            """^(?:retry|try\s+again)\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )

        private val DETAILS_PATTERN = Regex(
            """^(?:(.+?)\s+)?(?:details|more\s+info|information)$""",
            RegexOption.IGNORE_CASE
        )

        // Time thresholds for formatting
        private const val SECONDS_PER_MINUTE = 60L
        private const val SECONDS_PER_HOUR = 3600L
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Status queries
        "progress", "status", "what's the progress", "what is the progress",
        // Remaining
        "how much left", "remaining", "left",
        // ETA
        "time left", "eta", "estimated time",
        // Cancel
        "cancel", "stop",
        // Pause
        "pause", "wait",
        // Resume
        "resume", "continue",
        // Retry
        "retry", "try again",
        // Details
        "details", "more info", "information"
    )

    /**
     * Callback for voice feedback when progress state changes.
     */
    var onProgressStateChanged: ((progressName: String, state: String, message: String) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing progress command: $normalizedAction")

        return try {
            when {
                // Status queries: "progress", "status", "what's the progress", "[name] progress"
                PROGRESS_STATUS_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("progress", "status") -> {
                    handleProgressStatus(normalizedAction, command)
                }

                // Remaining queries: "how much left", "remaining", "[name] remaining"
                REMAINING_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("remaining", "left") -> {
                    handleRemaining(normalizedAction, command)
                }

                // ETA queries: "time left", "eta", "[name] eta"
                ETA_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("time left", "eta") -> {
                    handleEta(normalizedAction, command)
                }

                // Cancel commands: "cancel", "stop", "cancel [name]"
                CANCEL_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("cancel", "stop") -> {
                    handleCancel(normalizedAction, command)
                }

                // Pause commands: "pause", "wait", "pause [name]"
                PAUSE_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("pause", "wait") -> {
                    handlePause(normalizedAction, command)
                }

                // Resume commands: "resume", "continue", "resume [name]"
                RESUME_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("resume", "continue") -> {
                    handleResume(normalizedAction, command)
                }

                // Retry commands: "retry", "try again", "retry [name]"
                RETRY_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("retry", "try again") -> {
                    handleRetry(normalizedAction, command)
                }

                // Details queries: "details", "more info", "[name] details"
                DETAILS_PATTERN.containsMatchIn(normalizedAction) ||
                normalizedAction in listOf("details", "more info", "information") -> {
                    handleDetails(normalizedAction, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing progress command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ===============================================================================
    // Command Handlers
    // ===============================================================================

    /**
     * Handle progress status query commands.
     */
    private suspend fun handleProgressStatus(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(PROGRESS_STATUS_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        val progressValue = executor.getProgress(progressInfo)
            ?: return HandlerResult.failure("Could not read progress value")

        val feedback = buildProgressFeedback(progressInfo, progressValue)

        Log.i(TAG, "Progress status: ${progressInfo.name} = $progressValue")

        return HandlerResult.Success(
            message = feedback,
            data = mapOf(
                "progressName" to progressInfo.name,
                "progressAvid" to progressInfo.avid,
                "progress" to progressValue,
                "isIndeterminate" to progressInfo.isIndeterminate,
                "message" to (progressInfo.message ?: ""),
                "accessibility_announcement" to feedback
            )
        )
    }

    /**
     * Handle remaining progress query commands.
     */
    private suspend fun handleRemaining(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(REMAINING_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        if (progressInfo.isIndeterminate) {
            return HandlerResult.Success(
                message = buildString {
                    if (progressInfo.name.isNotBlank()) {
                        append(progressInfo.name)
                        append(": ")
                    }
                    append("Cannot determine remaining progress for indeterminate operation")
                },
                data = mapOf(
                    "progressName" to progressInfo.name,
                    "isIndeterminate" to true
                )
            )
        }

        val remaining = executor.getRemaining(progressInfo)
            ?: return HandlerResult.failure("Could not determine remaining progress")

        val feedback = buildString {
            if (progressInfo.name.isNotBlank()) {
                append(progressInfo.name)
                append(": ")
            }
            append(formatPercentage(remaining))
            append(" remaining")
        }

        Log.i(TAG, "Remaining progress: ${progressInfo.name} = $remaining%")

        return HandlerResult.Success(
            message = feedback,
            data = mapOf(
                "progressName" to progressInfo.name,
                "progressAvid" to progressInfo.avid,
                "remaining" to remaining,
                "accessibility_announcement" to feedback
            )
        )
    }

    /**
     * Handle ETA query commands.
     */
    private suspend fun handleEta(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(ETA_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        if (progressInfo.isIndeterminate) {
            return HandlerResult.Success(
                message = buildString {
                    if (progressInfo.name.isNotBlank()) {
                        append(progressInfo.name)
                        append(": ")
                    }
                    append("Cannot estimate time for indeterminate operation")
                },
                data = mapOf(
                    "progressName" to progressInfo.name,
                    "isIndeterminate" to true
                )
            )
        }

        val eta = executor.getEta(progressInfo)

        val feedback = buildString {
            if (progressInfo.name.isNotBlank()) {
                append(progressInfo.name)
                append(": ")
            }
            if (eta != null && eta > 0) {
                append(formatTime(eta))
                append(" remaining")
            } else if (eta == 0L) {
                append("Almost complete")
            } else {
                append("Unable to estimate time remaining")
            }
        }

        Log.i(TAG, "ETA: ${progressInfo.name} = ${eta ?: "unknown"}ms")

        return HandlerResult.Success(
            message = feedback,
            data = mapOf(
                "progressName" to progressInfo.name,
                "progressAvid" to progressInfo.avid,
                "etaMillis" to (eta ?: -1L),
                "etaFormatted" to (eta?.let { formatTime(it) } ?: "Unknown"),
                "accessibility_announcement" to feedback
            )
        )
    }

    /**
     * Handle cancel operation commands.
     */
    private suspend fun handleCancel(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(CANCEL_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        if (!progressInfo.isCancellable) {
            return HandlerResult.Failure(
                reason = buildString {
                    if (progressInfo.name.isNotBlank()) {
                        append(progressInfo.name)
                        append(": ")
                    }
                    append("This operation cannot be cancelled")
                },
                recoverable = false
            )
        }

        val result = executor.cancel(progressInfo)

        return if (result.success) {
            val feedback = buildString {
                if (progressInfo.name.isNotBlank()) {
                    append(progressInfo.name)
                    append(" ")
                }
                append("cancelled")
            }

            onProgressStateChanged?.invoke(
                progressInfo.name.ifBlank { "Progress" },
                "cancelled",
                feedback
            )

            Log.i(TAG, "Progress cancelled: ${progressInfo.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "progressName" to progressInfo.name,
                    "progressAvid" to progressInfo.avid,
                    "action" to "cancel",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not cancel operation",
                recoverable = true
            )
        }
    }

    /**
     * Handle pause operation commands.
     */
    private suspend fun handlePause(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(PAUSE_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        if (!progressInfo.isPausable) {
            return HandlerResult.Failure(
                reason = buildString {
                    if (progressInfo.name.isNotBlank()) {
                        append(progressInfo.name)
                        append(": ")
                    }
                    append("This operation cannot be paused")
                },
                recoverable = false
            )
        }

        if (progressInfo.isPaused) {
            return HandlerResult.Failure(
                reason = buildString {
                    if (progressInfo.name.isNotBlank()) {
                        append(progressInfo.name)
                        append(": ")
                    }
                    append("Operation is already paused")
                },
                recoverable = false,
                suggestedAction = "Say 'resume' to continue"
            )
        }

        val result = executor.pause(progressInfo)

        return if (result.success) {
            val feedback = buildString {
                if (progressInfo.name.isNotBlank()) {
                    append(progressInfo.name)
                    append(" ")
                }
                append("paused")
            }

            onProgressStateChanged?.invoke(
                progressInfo.name.ifBlank { "Progress" },
                "paused",
                feedback
            )

            Log.i(TAG, "Progress paused: ${progressInfo.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "progressName" to progressInfo.name,
                    "progressAvid" to progressInfo.avid,
                    "action" to "pause",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not pause operation",
                recoverable = true
            )
        }
    }

    /**
     * Handle resume operation commands.
     */
    private suspend fun handleResume(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(RESUME_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        if (!progressInfo.isPaused) {
            return HandlerResult.Failure(
                reason = buildString {
                    if (progressInfo.name.isNotBlank()) {
                        append(progressInfo.name)
                        append(": ")
                    }
                    append("Operation is not paused")
                },
                recoverable = false
            )
        }

        val result = executor.resume(progressInfo)

        return if (result.success) {
            val feedback = buildString {
                if (progressInfo.name.isNotBlank()) {
                    append(progressInfo.name)
                    append(" ")
                }
                append("resumed")
            }

            onProgressStateChanged?.invoke(
                progressInfo.name.ifBlank { "Progress" },
                "resumed",
                feedback
            )

            Log.i(TAG, "Progress resumed: ${progressInfo.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "progressName" to progressInfo.name,
                    "progressAvid" to progressInfo.avid,
                    "action" to "resume",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not resume operation",
                recoverable = true
            )
        }
    }

    /**
     * Handle retry operation commands.
     */
    private suspend fun handleRetry(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(RETRY_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        val result = executor.retry(progressInfo)

        return if (result.success) {
            val feedback = buildString {
                if (progressInfo.name.isNotBlank()) {
                    append(progressInfo.name)
                    append(" ")
                }
                append("retrying")
            }

            onProgressStateChanged?.invoke(
                progressInfo.name.ifBlank { "Progress" },
                "retrying",
                feedback
            )

            Log.i(TAG, "Progress retry: ${progressInfo.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "progressName" to progressInfo.name,
                    "progressAvid" to progressInfo.avid,
                    "action" to "retry",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not retry operation",
                recoverable = true
            )
        }
    }

    /**
     * Handle details query commands.
     */
    private suspend fun handleDetails(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val progressName = extractNameFromPattern(DETAILS_PATTERN, normalizedAction)

        val progressInfo = findProgress(
            name = progressName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (progressName != null) "Progress '$progressName' not found" else "No progress indicator found",
            recoverable = true,
            suggestedAction = "Focus on a progress indicator or specify a name"
        )

        val details = executor.getDetails(progressInfo)
            ?: return HandlerResult.failure("Could not retrieve progress details")

        val feedback = buildDetailedFeedback(progressInfo, details)

        Log.i(TAG, "Progress details: ${progressInfo.name}")

        return HandlerResult.Success(
            message = feedback,
            data = mapOf(
                "progressName" to progressInfo.name,
                "progressAvid" to progressInfo.avid,
                "progress" to progressInfo.progress,
                "isIndeterminate" to progressInfo.isIndeterminate,
                "isCancellable" to progressInfo.isCancellable,
                "isPausable" to progressInfo.isPausable,
                "isPaused" to progressInfo.isPaused,
                "message" to (progressInfo.message ?: ""),
                "details" to details,
                "accessibility_announcement" to feedback
            )
        )
    }

    // ===============================================================================
    // Helper Methods
    // ===============================================================================

    /**
     * Extract optional name from a regex pattern match.
     */
    private fun extractNameFromPattern(pattern: Regex, input: String): String? {
        val match = pattern.find(input) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }?.trim()
    }

    /**
     * Find progress indicator by name, AVID, or focus state.
     */
    private suspend fun findProgress(
        name: String? = null,
        avid: String? = null
    ): ProgressInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val progress = executor.findByAvid(avid)
            if (progress != null) return progress
        }

        // Priority 2: Name lookup
        if (name != null) {
            val progress = executor.findByName(name)
            if (progress != null) return progress
        }

        // Priority 3: Focused progress indicator
        return executor.findFocused()
    }

    /**
     * Build feedback message for progress status.
     */
    private fun buildProgressFeedback(progressInfo: ProgressInfo, progress: Double): String {
        return buildString {
            if (progressInfo.name.isNotBlank()) {
                append(progressInfo.name)
                append(": ")
            }

            when {
                progressInfo.isIndeterminate -> {
                    append(progressInfo.message ?: "In progress")
                }
                progressInfo.isPaused -> {
                    append("Paused at ")
                    append(formatPercentage(progress))
                }
                progress >= 100.0 -> {
                    append("Complete")
                }
                else -> {
                    append(formatPercentage(progress))
                    append(" complete")
                }
            }
        }
    }

    /**
     * Build detailed feedback message.
     */
    private fun buildDetailedFeedback(progressInfo: ProgressInfo, details: Map<String, Any>): String {
        return buildString {
            if (progressInfo.name.isNotBlank()) {
                append(progressInfo.name)
                append(": ")
            }

            if (progressInfo.isIndeterminate) {
                append("Indeterminate progress. ")
            } else {
                append(formatPercentage(progressInfo.progress))
                append(" complete. ")
            }

            if (progressInfo.isPaused) {
                append("Currently paused. ")
            }

            progressInfo.estimatedTimeRemaining?.let { eta ->
                if (eta > 0) {
                    append(formatTime(eta))
                    append(" remaining. ")
                }
            }

            if (progressInfo.isCancellable) {
                append("Can be cancelled. ")
            }

            if (progressInfo.isPausable && !progressInfo.isPaused) {
                append("Can be paused. ")
            }

            progressInfo.message?.let { msg ->
                if (msg.isNotBlank()) {
                    append("Status: ")
                    append(msg)
                }
            }
        }.trim()
    }

    /**
     * Format a percentage value for voice output.
     */
    private fun formatPercentage(value: Double): String {
        val rounded = value.toInt().coerceIn(0, 100)
        return "$rounded%"
    }

    /**
     * Format time in milliseconds for voice output.
     *
     * @param millis Time in milliseconds
     * @return Human-readable time string (e.g., "2 minutes", "about 30 seconds")
     */
    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        return when {
            seconds < 5 -> "A few seconds"
            seconds < SECONDS_PER_MINUTE -> {
                val roundedSeconds = (seconds / 10) * 10
                if (roundedSeconds <= 10) "About 10 seconds"
                else "About $roundedSeconds seconds"
            }
            seconds < SECONDS_PER_HOUR -> {
                val minutes = seconds / SECONDS_PER_MINUTE
                val remainingSeconds = seconds % SECONDS_PER_MINUTE
                when {
                    minutes == 1L && remainingSeconds < 30 -> "About a minute"
                    minutes == 1L -> "About a minute and a half"
                    remainingSeconds < 30 -> "$minutes minutes"
                    else -> "$minutes and a half minutes"
                }
            }
            else -> {
                val hours = seconds / SECONDS_PER_HOUR
                val remainingMinutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
                when {
                    hours == 1L && remainingMinutes < 15 -> "About an hour"
                    hours == 1L && remainingMinutes < 45 -> "About an hour and a half"
                    hours == 1L -> "About 2 hours"
                    remainingMinutes < 15 -> "$hours hours"
                    remainingMinutes < 45 -> "$hours and a half hours"
                    else -> "${hours + 1} hours"
                }
            }
        }
    }

}

// ===================================================================================
// Supporting Types
// ===================================================================================

/**
 * Information about a progress indicator component.
 *
 * @property avid AVID fingerprint for the progress indicator (format: PRG:{hash8})
 * @property name Display name or associated label
 * @property progress Current progress value (0.0 to 100.0 for determinate)
 * @property isIndeterminate Whether progress is indeterminate (spinning/unknown)
 * @property isCancellable Whether the operation can be cancelled
 * @property isPausable Whether the operation can be paused
 * @property isPaused Whether the operation is currently paused
 * @property estimatedTimeRemaining Estimated time to completion in milliseconds (null if unknown)
 * @property message Optional status message associated with the progress
 * @property bounds Screen bounds for the progress indicator
 * @property node Platform-specific node reference
 */
data class ProgressInfo(
    val avid: String,
    val name: String = "",
    val progress: Double = 0.0,
    val isIndeterminate: Boolean = false,
    val isCancellable: Boolean = false,
    val isPausable: Boolean = false,
    val isPaused: Boolean = false,
    val estimatedTimeRemaining: Long? = null,
    val message: String? = null,
    val bounds: Bounds = Bounds.EMPTY,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "ProgressBar",
        text = name,
        bounds = bounds,
        isClickable = false,
        isEnabled = true,
        avid = avid,
        stateDescription = when {
            isIndeterminate -> "Loading"
            isPaused -> "Paused at ${progress.toInt()}%"
            else -> "${progress.toInt()}%"
        }
    )
}

/**
 * Result of a progress operation (cancel, pause, resume, retry).
 */
data class ProgressOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousState: String? = null,
    val newState: String? = null
) {
    companion object {
        fun success(previousState: String? = null, newState: String? = null) = ProgressOperationResult(
            success = true,
            previousState = previousState,
            newState = newState
        )

        fun error(message: String) = ProgressOperationResult(
            success = false,
            error = message
        )
    }
}

// ===================================================================================
// Platform Executor Interface
// ===================================================================================

/**
 * Platform-specific executor for progress indicator operations.
 *
 * Implementations should:
 * 1. Find progress components by AVID, name, or focus state
 * 2. Read current progress values and state
 * 3. Control progress operations (cancel, pause, resume, retry)
 * 4. Handle both determinate and indeterminate progress indicators
 *
 * ## Progress Detection Algorithm
 *
 * ```kotlin
 * fun findProgressNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.widget.ProgressBar
 *     // - androidx.core.widget.ContentLoadingProgressBar
 *     // - com.google.android.material.progressindicator.*
 *     // - Custom progress implementations with RangeInfo
 * }
 * ```
 *
 * ## State Reading Algorithm
 *
 * ```kotlin
 * fun getProgressState(node: AccessibilityNodeInfo): ProgressState {
 *     // Read RangeInfo for current, min, max values
 *     // Check isIndeterminate from node properties
 *     // Check contentDescription for status messages
 * }
 * ```
 */
interface ProgressExecutor {

    // ===============================================================================
    // Progress Discovery
    // ===============================================================================

    /**
     * Find a progress indicator by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: PRG:{hash8})
     * @return ProgressInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): ProgressInfo?

    /**
     * Find a progress indicator by its name or associated label.
     *
     * Searches for:
     * 1. Progress with matching contentDescription
     * 2. Progress with label text matching name
     * 3. Progress with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return ProgressInfo if found, null otherwise
     */
    suspend fun findByName(name: String): ProgressInfo?

    /**
     * Find the currently focused progress indicator.
     *
     * @return ProgressInfo if a progress indicator has focus, null otherwise
     */
    suspend fun findFocused(): ProgressInfo?

    /**
     * Get all progress indicators on the current screen.
     *
     * @return List of all visible progress components
     */
    suspend fun getAllProgress(): List<ProgressInfo>

    // ===============================================================================
    // Value Operations
    // ===============================================================================

    /**
     * Get the current progress value.
     *
     * @param progress The progress indicator to query
     * @return Current progress (0.0 to 100.0), or null if unable to read
     */
    suspend fun getProgress(progress: ProgressInfo): Double?

    /**
     * Get the remaining progress value.
     *
     * @param progress The progress indicator to query
     * @return Remaining progress percentage (0.0 to 100.0), or null if unable to calculate
     */
    suspend fun getRemaining(progress: ProgressInfo): Double?

    /**
     * Get the estimated time to completion.
     *
     * @param progress The progress indicator to query
     * @return Estimated time remaining in milliseconds, or null if unable to estimate
     */
    suspend fun getEta(progress: ProgressInfo): Long?

    /**
     * Get detailed information about the progress.
     *
     * @param progress The progress indicator to query
     * @return Map of detailed progress information
     */
    suspend fun getDetails(progress: ProgressInfo): Map<String, Any>?

    // ===============================================================================
    // Control Operations
    // ===============================================================================

    /**
     * Cancel the operation associated with this progress indicator.
     *
     * @param progress The progress indicator whose operation to cancel
     * @return Operation result indicating success or failure
     */
    suspend fun cancel(progress: ProgressInfo): ProgressOperationResult

    /**
     * Pause the operation associated with this progress indicator.
     *
     * @param progress The progress indicator whose operation to pause
     * @return Operation result indicating success or failure
     */
    suspend fun pause(progress: ProgressInfo): ProgressOperationResult

    /**
     * Resume the paused operation associated with this progress indicator.
     *
     * @param progress The progress indicator whose operation to resume
     * @return Operation result indicating success or failure
     */
    suspend fun resume(progress: ProgressInfo): ProgressOperationResult

    /**
     * Retry the failed operation associated with this progress indicator.
     *
     * @param progress The progress indicator whose operation to retry
     * @return Operation result indicating success or failure
     */
    suspend fun retry(progress: ProgressInfo): ProgressOperationResult
}
