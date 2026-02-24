/**
 * ToggleHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-28 00:00 PST
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for VoiceOS toggle/switch controls
 * Features: Turn on/off, enable/disable, toggle switches by name or focus
 * Location: MagicVoiceHandlers module
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler pattern with executor
 * - v1.0.0 (2026-01-27): Initial implementation for toggle voice commands
 */

package com.augmentalis.avamagic.voice.handlers.input

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for toggle/switch controls
 *
 * Routes commands to toggle UI elements (switches, checkboxes, toggles)
 * via executor pattern.
 *
 * Supported Commands:
 * - "turn on [name]" / "enable [name]" - turn on toggle by name
 * - "turn off [name]" / "disable [name]" - turn off toggle by name
 * - "toggle [name]" / "switch [name]" - flip toggle state by name
 * - "on" / "off" - for focused toggle
 * - "toggle" - flip focused toggle
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for toggle operations
 */
class ToggleHandler(
    private val executor: ToggleExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ToggleHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command patterns for voice recognition
        private val TURN_ON_PREFIXES = listOf("turn on", "enable")
        private val TURN_OFF_PREFIXES = listOf("turn off", "disable")
        private val TOGGLE_PREFIXES = listOf("toggle", "switch")
        private val STANDALONE_ON = setOf("on")
        private val STANDALONE_OFF = setOf("off")
        private val STANDALONE_TOGGLE = setOf("toggle")
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Named toggle commands
        "turn on", "turn off",
        "enable", "disable",
        "toggle", "switch",

        // Focused toggle commands (standalone)
        "on", "off"
    )

    /**
     * Callback for voice feedback when toggle state changes
     */
    var onToggleChanged: ((toggleName: String, newState: Boolean) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing toggle command: $normalizedAction" }

        return try {
            when {
                // "turn on [name]" or "enable [name]"
                TURN_ON_PREFIXES.any { normalizedAction.startsWith(it) } -> {
                    val name = extractToggleName(normalizedAction, TURN_ON_PREFIXES)
                    if (name.isNotEmpty()) {
                        handleTurnOn(name)
                    } else {
                        handleSetFocusedToggle(true)
                    }
                }

                // "turn off [name]" or "disable [name]"
                TURN_OFF_PREFIXES.any { normalizedAction.startsWith(it) } -> {
                    val name = extractToggleName(normalizedAction, TURN_OFF_PREFIXES)
                    if (name.isNotEmpty()) {
                        handleTurnOff(name)
                    } else {
                        handleSetFocusedToggle(false)
                    }
                }

                // "toggle [name]" or "switch [name]"
                TOGGLE_PREFIXES.any { normalizedAction.startsWith(it) } -> {
                    val name = extractToggleName(normalizedAction, TOGGLE_PREFIXES)
                    if (name.isNotEmpty()) {
                        handleFlipToggle(name)
                    } else {
                        handleFlipFocusedToggle()
                    }
                }

                // Standalone "on" - set focused toggle to on
                normalizedAction in STANDALONE_ON -> handleSetFocusedToggle(true)

                // Standalone "off" - set focused toggle to off
                normalizedAction in STANDALONE_OFF -> handleSetFocusedToggle(false)

                // Standalone "toggle" - flip focused toggle
                normalizedAction in STANDALONE_TOGGLE -> handleFlipFocusedToggle()

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error processing toggle command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    private suspend fun handleTurnOn(name: String): HandlerResult {
        val result = executor.turnOnToggle(name)

        return when (result) {
            is ToggleResult.Success -> {
                onToggleChanged?.invoke(result.toggleName, true)
                HandlerResult.Success(
                    message = "'$name' turned on",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "newState" to true
                    )
                )
            }
            is ToggleResult.AlreadyInState -> {
                HandlerResult.Success(
                    message = "'$name' is already on",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "state" to result.state
                    )
                )
            }
            is ToggleResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Toggle '$name' not found",
                    recoverable = true
                )
            }
            is ToggleResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            ToggleResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            ToggleResult.NoFocusedToggle -> {
                HandlerResult.Failure(
                    reason = "No focused toggle found",
                    recoverable = true,
                    suggestedAction = "Focus on a toggle first"
                )
            }
        }
    }

    private suspend fun handleTurnOff(name: String): HandlerResult {
        val result = executor.turnOffToggle(name)

        return when (result) {
            is ToggleResult.Success -> {
                onToggleChanged?.invoke(result.toggleName, false)
                HandlerResult.Success(
                    message = "'$name' turned off",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "newState" to false
                    )
                )
            }
            is ToggleResult.AlreadyInState -> {
                HandlerResult.Success(
                    message = "'$name' is already off",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "state" to result.state
                    )
                )
            }
            is ToggleResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Toggle '$name' not found",
                    recoverable = true
                )
            }
            is ToggleResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            ToggleResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            ToggleResult.NoFocusedToggle -> {
                HandlerResult.failure(
                    reason = "No focused toggle found",
                    recoverable = true
                )
            }
        }
    }

    private suspend fun handleFlipToggle(name: String): HandlerResult {
        val result = executor.flipToggle(name)

        return when (result) {
            is ToggleResult.Success -> {
                onToggleChanged?.invoke(result.toggleName, result.newState)
                HandlerResult.Success(
                    message = "'$name' toggled ${if (result.newState) "on" else "off"}",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "newState" to result.newState
                    )
                )
            }
            is ToggleResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Toggle '$name' not found",
                    recoverable = true
                )
            }
            is ToggleResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            is ToggleResult.AlreadyInState -> {
                HandlerResult.success(message = "'$name' is ${if (result.state) "on" else "off"}")
            }
            ToggleResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            ToggleResult.NoFocusedToggle -> {
                HandlerResult.failure(
                    reason = "No focused toggle found",
                    recoverable = true
                )
            }
        }
    }

    private suspend fun handleSetFocusedToggle(targetState: Boolean): HandlerResult {
        val result = executor.setFocusedToggleState(targetState)

        return when (result) {
            is ToggleResult.Success -> {
                onToggleChanged?.invoke(result.toggleName, targetState)
                HandlerResult.Success(
                    message = "Toggle turned ${if (targetState) "on" else "off"}",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "newState" to targetState
                    )
                )
            }
            is ToggleResult.AlreadyInState -> {
                HandlerResult.success(
                    message = "Toggle is already ${if (targetState) "on" else "off"}"
                )
            }
            ToggleResult.NoFocusedToggle -> {
                HandlerResult.Failure(
                    reason = "No focused toggle found",
                    recoverable = true,
                    suggestedAction = "Focus on a toggle first, or say 'turn on WiFi'"
                )
            }
            is ToggleResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Toggle not found",
                    recoverable = true
                )
            }
            is ToggleResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            ToggleResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
        }
    }

    private suspend fun handleFlipFocusedToggle(): HandlerResult {
        val result = executor.flipFocusedToggle()

        return when (result) {
            is ToggleResult.Success -> {
                onToggleChanged?.invoke(result.toggleName, result.newState)
                HandlerResult.Success(
                    message = "Toggle flipped ${if (result.newState) "on" else "off"}",
                    data = mapOf(
                        "toggleName" to result.toggleName,
                        "newState" to result.newState
                    )
                )
            }
            ToggleResult.NoFocusedToggle -> {
                HandlerResult.Failure(
                    reason = "No focused toggle found",
                    recoverable = true,
                    suggestedAction = "Focus on a toggle first"
                )
            }
            is ToggleResult.NotFound -> {
                HandlerResult.failure(
                    reason = "Toggle not found",
                    recoverable = true
                )
            }
            is ToggleResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            is ToggleResult.AlreadyInState -> {
                HandlerResult.success(message = "Toggle is ${if (result.state) "on" else "off"}")
            }
            ToggleResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    private fun extractToggleName(command: String, prefixes: List<String>): String {
        for (prefix in prefixes) {
            if (command.startsWith(prefix)) {
                return command.removePrefix(prefix).trim()
            }
        }
        return ""
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Toggle handler status
 */
data class ToggleHandlerStatus(
    val hasAccessibilityService: Boolean
)

/**
 * Toggle operation result
 */
sealed class ToggleResult {
    data class Success(val toggleName: String, val newState: Boolean) : ToggleResult()
    data class AlreadyInState(val toggleName: String, val state: Boolean) : ToggleResult()
    data class NotFound(val toggleName: String) : ToggleResult()
    data class Error(val message: String) : ToggleResult()
    object NoAccessibility : ToggleResult()
    object NoFocusedToggle : ToggleResult()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for toggle operations.
 *
 * Implementations should:
 * 1. Find toggle components (Switch, CheckBox, ToggleButton) in accessibility tree
 * 2. Set toggle states (on/off)
 * 3. Find toggles by name or focus state
 */
interface ToggleExecutor {

    /**
     * Turn on a toggle by name.
     */
    suspend fun turnOnToggle(name: String): ToggleResult

    /**
     * Turn off a toggle by name.
     */
    suspend fun turnOffToggle(name: String): ToggleResult

    /**
     * Flip a toggle's state by name.
     */
    suspend fun flipToggle(name: String): ToggleResult

    /**
     * Set the focused toggle to a specific state.
     */
    suspend fun setFocusedToggleState(targetState: Boolean): ToggleResult

    /**
     * Flip the focused toggle's state.
     */
    suspend fun flipFocusedToggle(): ToggleResult

    /**
     * Get handler status.
     */
    suspend fun getStatus(): ToggleHandlerStatus
}
