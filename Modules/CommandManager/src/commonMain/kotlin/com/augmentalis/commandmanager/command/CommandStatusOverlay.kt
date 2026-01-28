/**
 * CommandStatusOverlay.kt - Voice command status overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * KMP migration of VoiceOSCore CommandStatusOverlay.
 * Displays visual feedback for voice command execution states:
 * LISTENING -> PROCESSING -> EXECUTING -> SUCCESS/ERROR
 *
 * Platform-specific rendering is handled by platform overlay managers.
 * This class provides the state management and data flow.
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Overlay for displaying voice command execution status.
 *
 * Shows visual feedback for the command lifecycle:
 * - LISTENING: Microphone active, waiting for speech
 * - PROCESSING: Speech recognized, matching to commands
 * - EXECUTING: Command matched, performing action
 * - SUCCESS: Action completed successfully
 * - ERROR: An error occurred
 *
 * Usage:
 * ```kotlin
 * val overlay = CommandStatusOverlay()
 *
 * // Show listening state
 * overlay.showStatus("...", CommandState.LISTENING, "Listening for command...")
 *
 * // Update to processing
 * overlay.updateStatus("click button", CommandState.PROCESSING, "Processing...")
 *
 * // Show success
 * overlay.updateStatus("click button", CommandState.SUCCESS, "Done!")
 *
 * // Hide after delay
 * overlay.hide()
 * ```
 *
 * @see IOverlay for the base interface contract
 * @see CommandState for available states
 * @see StatusUpdate for the emitted state updates
 */
class CommandStatusOverlay : IOverlay {

    // ═══════════════════════════════════════════════════════════════════════
    // IOverlay Properties
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Unique identifier for this overlay type.
     */
    override val id: String = OVERLAY_ID

    /**
     * Internal visibility state backed by MutableStateFlow.
     */
    private val _isVisible = MutableStateFlow(false)

    /**
     * Current visibility state.
     */
    override val isVisible: Boolean get() = _isVisible.value

    // ═══════════════════════════════════════════════════════════════════════
    // Command Status State
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Internal current state backed by MutableStateFlow.
     */
    private val _currentState = MutableStateFlow(CommandState.LISTENING)

    /**
     * Current command state.
     * Defaults to LISTENING when no command is active.
     */
    val currentState: CommandState get() = _currentState.value

    /**
     * Internal state flow for status updates.
     */
    private val _stateFlow = MutableStateFlow<StatusUpdate?>(null)

    /**
     * Flow of status updates.
     * Emits null initially and after dispose.
     * Subscribe to observe command lifecycle events.
     */
    val stateFlow: StateFlow<StatusUpdate?> = _stateFlow.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // Internal State
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Current command text being displayed.
     */
    @Volatile
    private var currentCommand: String = ""

    /**
     * Current message being displayed.
     */
    @Volatile
    private var currentMessage: String = ""

    /**
     * Flag indicating if overlay has been disposed.
     */
    @Volatile
    private var isDisposed: Boolean = false

    // ═══════════════════════════════════════════════════════════════════════
    // Status Control Methods
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show status overlay with command, state, and message.
     *
     * This method:
     * 1. Makes the overlay visible
     * 2. Updates the current state
     * 3. Emits a StatusUpdate to stateFlow
     *
     * Has no effect if the overlay has been disposed.
     *
     * @param command The command being executed (e.g., "click button")
     * @param state Current command state
     * @param message Human-readable status message
     */
    fun showStatus(command: String, state: CommandState, message: String) {
        if (isDisposed) return

        currentCommand = command
        currentMessage = message
        _currentState.value = state
        _stateFlow.value = StatusUpdate(command, state, message)
        _isVisible.value = true
    }

    /**
     * Update status without recreating overlay or changing visibility.
     *
     * Use this to transition through states after initial showStatus:
     * - LISTENING -> PROCESSING
     * - PROCESSING -> EXECUTING
     * - EXECUTING -> SUCCESS/ERROR
     *
     * Has no effect if the overlay has been disposed.
     *
     * @param command The command being executed
     * @param state New command state
     * @param message Human-readable status message
     */
    fun updateStatus(command: String, state: CommandState, message: String) {
        if (isDisposed) return

        currentCommand = command
        currentMessage = message
        _currentState.value = state
        _stateFlow.value = StatusUpdate(command, state, message)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // IOverlay Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show the overlay.
     *
     * Makes the overlay visible without changing the current state or emitting
     * a new StatusUpdate. Use showStatus() to show with a specific state.
     *
     * Has no effect if the overlay has been disposed.
     */
    override fun show() {
        if (isDisposed) return
        _isVisible.value = true
    }

    /**
     * Hide the overlay.
     *
     * Makes the overlay invisible but retains state.
     * The overlay can be shown again with show() or showStatus().
     */
    override fun hide() {
        _isVisible.value = false
    }

    /**
     * Update the overlay with new data.
     *
     * Only handles OverlayData.Status; other data types are ignored.
     *
     * @param data The overlay data to display
     */
    override fun update(data: OverlayData) {
        if (isDisposed) return

        when (data) {
            is OverlayData.Status -> {
                currentMessage = data.message
                _currentState.value = data.state
                _stateFlow.value = StatusUpdate(data.message, data.state, data.message)
            }
            // Ignore other data types - this overlay only handles Status
            else -> { /* No-op */ }
        }
    }

    /**
     * Dispose the overlay and release all resources.
     *
     * After calling dispose:
     * - The overlay is hidden
     * - stateFlow emits null
     * - show(), showStatus(), updateStatus(), and update() have no effect
     *
     * This method is idempotent and can be called multiple times safely.
     */
    override fun dispose() {
        isDisposed = true
        hide()
        _stateFlow.value = null
    }

    companion object {
        /**
         * Standard overlay ID for command status overlays.
         */
        const val OVERLAY_ID = "command_status"
    }
}

/**
 * Status update event emitted by CommandStatusOverlay.
 *
 * Contains a snapshot of the command execution state at a point in time.
 *
 * @property command The command being executed
 * @property state Current execution state
 * @property message Human-readable status message
 * @property timestamp Unix timestamp in milliseconds when this update was created
 */
data class StatusUpdate(
    val command: String,
    val state: CommandState,
    val message: String,
    val timestamp: Long = currentTimeMillis()
)

