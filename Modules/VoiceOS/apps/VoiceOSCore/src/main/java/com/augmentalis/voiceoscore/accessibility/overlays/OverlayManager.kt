/**
 * OverlayManager.kt - Centralized overlay management system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import com.augmentalis.voiceos.speech.confidence.ConfidenceResult

/**
 * Centralized manager for all accessibility overlays
 * Provides coordinated control and lifecycle management
 */
class OverlayManager(
    private val context: Context
) {
    companion object {
        @Volatile
        private var instance: OverlayManager? = null

        /**
         * Get or create singleton instance
         */
        fun getInstance(context: Context): OverlayManager {
            return instance ?: synchronized(this) {
                instance ?: OverlayManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Lazy initialization of overlays
    private val confidenceOverlay by lazy {
        ConfidenceOverlay(context, windowManager)
    }

    private val numberedSelectionOverlay by lazy {
        NumberedSelectionOverlay(context, windowManager)
    }

    private val commandStatusOverlay by lazy {
        CommandStatusOverlay(context, windowManager)
    }

    private val contextMenuOverlay by lazy {
        ContextMenuOverlay(context, windowManager)
    }

    // Track active overlays for coordination
    private val activeOverlays = mutableSetOf<String>()

    /**
     * Show confidence indicator overlay
     */
    fun showConfidence(result: ConfidenceResult) {
        confidenceOverlay.show(result)
        activeOverlays.add("confidence")
    }

    /**
     * Update confidence without showing/hiding
     */
    fun updateConfidence(result: ConfidenceResult) {
        if (confidenceOverlay.isVisible()) {
            confidenceOverlay.updateConfidence(result)
        } else {
            showConfidence(result)
        }
    }

    /**
     * Hide confidence overlay
     */
    fun hideConfidence() {
        confidenceOverlay.hide()
        activeOverlays.remove("confidence")
    }

    /**
     * Show numbered selection overlay
     */
    fun showNumberedSelection(items: List<SelectableItem>) {
        // Hide conflicting overlays
        hideContextMenu()

        numberedSelectionOverlay.showItems(items)
        activeOverlays.add("numberedSelection")
    }

    /**
     * Update numbered selection items
     */
    fun updateNumberedSelection(items: List<SelectableItem>) {
        numberedSelectionOverlay.updateItems(items)
    }

    /**
     * Select item by number
     */
    fun selectNumberedItem(number: Int): Boolean {
        return numberedSelectionOverlay.selectItem(number)
    }

    /**
     * Hide numbered selection overlay
     */
    fun hideNumberedSelection() {
        numberedSelectionOverlay.hide()
        activeOverlays.remove("numberedSelection")
    }

    /**
     * Show command status overlay
     */
    fun showCommandStatus(
        command: String,
        state: CommandState,
        message: String? = null
    ) {
        commandStatusOverlay.showStatus(command, state, message)
        activeOverlays.add("commandStatus")
    }

    /**
     * Update command status
     */
    fun updateCommandStatus(
        command: String? = null,
        state: CommandState? = null,
        message: String? = null
    ) {
        commandStatusOverlay.updateStatus(command, state, message)
    }

    /**
     * Hide command status overlay
     */
    fun hideCommandStatus() {
        commandStatusOverlay.hide()
        activeOverlays.remove("commandStatus")
    }

    /**
     * Show context menu overlay at center
     */
    fun showContextMenu(
        items: List<MenuItem>,
        title: String? = null
    ) {
        // Hide conflicting overlays
        hideNumberedSelection()

        contextMenuOverlay.showMenu(items, title)
        activeOverlays.add("contextMenu")
    }

    /**
     * Show context menu overlay at position
     */
    fun showContextMenuAt(
        items: List<MenuItem>,
        position: Point,
        title: String? = null
    ) {
        // Hide conflicting overlays
        hideNumberedSelection()

        contextMenuOverlay.showMenuAt(items, position, title)
        activeOverlays.add("contextMenu")
    }

    /**
     * Select context menu item by ID
     */
    fun selectContextMenuItem(id: String): Boolean {
        return contextMenuOverlay.selectItemById(id)
    }

    /**
     * Select context menu item by number
     */
    fun selectContextMenuByNumber(number: Int): Boolean {
        return contextMenuOverlay.selectItemByNumber(number)
    }

    /**
     * Hide context menu overlay
     */
    fun hideContextMenu() {
        contextMenuOverlay.hide()
        activeOverlays.remove("contextMenu")
    }

    /**
     * Hide all overlays
     */
    fun hideAll() {
        confidenceOverlay.hide()
        numberedSelectionOverlay.hide()
        commandStatusOverlay.hide()
        contextMenuOverlay.hide()
        activeOverlays.clear()
    }

    /**
     * Check if any overlay is visible
     */
    fun isAnyVisible(): Boolean {
        return activeOverlays.isNotEmpty()
    }

    /**
     * Check if specific overlay is visible
     */
    fun isOverlayVisible(overlayName: String): Boolean {
        return activeOverlays.contains(overlayName)
    }

    /**
     * Get list of active overlays
     */
    fun getActiveOverlays(): Set<String> {
        return activeOverlays.toSet()
    }

    /**
     * Dispose all overlays and clean up resources
     */
    fun dispose() {
        confidenceOverlay.dispose()
        numberedSelectionOverlay.dispose()
        commandStatusOverlay.dispose()
        contextMenuOverlay.dispose()
        activeOverlays.clear()
        instance = null
    }

    /**
     * Convenience method: Show listening state
     */
    fun showListening(partialText: String = "") {
        showCommandStatus(
            command = partialText.ifEmpty { "Listening..." },
            state = CommandState.LISTENING
        )
    }

    /**
     * Convenience method: Show processing state
     */
    fun showProcessing(command: String) {
        showCommandStatus(
            command = command,
            state = CommandState.PROCESSING,
            message = "Recognizing..."
        )
    }

    /**
     * Convenience method: Show executing state
     */
    fun showExecuting(command: String) {
        showCommandStatus(
            command = command,
            state = CommandState.EXECUTING,
            message = "Executing..."
        )
    }

    /**
     * Convenience method: Show success state
     */
    fun showSuccess(command: String, message: String? = null) {
        showCommandStatus(
            command = command,
            state = CommandState.SUCCESS,
            message = message ?: "Command executed successfully"
        )
    }

    /**
     * Convenience method: Show error state
     */
    fun showError(command: String, error: String) {
        showCommandStatus(
            command = command,
            state = CommandState.ERROR,
            message = error
        )
    }

    /**
     * Quick dismiss - hide command status after delay
     */
    suspend fun dismissAfterDelay(delayMs: Long = 2000) {
        kotlinx.coroutines.delay(delayMs)
        hideCommandStatus()
    }
}
