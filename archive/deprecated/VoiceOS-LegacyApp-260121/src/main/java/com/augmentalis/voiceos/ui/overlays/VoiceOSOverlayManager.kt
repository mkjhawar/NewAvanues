/**
 * VoiceOSOverlayManager.kt - Unified overlay management for VoiceOS
 *
 * Coordinates all overlay components with a simple state-based API.
 * Implements the minimalistic hidden UI pattern where overlays are
 * invisible until triggered by "Hey AVA" wake word.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-02
 */
package com.augmentalis.voiceos.ui.overlays

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.util.Log
import com.augmentalis.voiceos.ui.overlays.debug.*

enum class VoiceOSOverlayState {
    HIDDEN, LISTENING_PILL, COMMAND_BAR, NUMBER_OVERLAY, FEEDBACK
}

data class ElementData(
    val label: String, val bounds: Rect,
    val isForeground: Boolean = true, val windowTitle: String? = null
)

class VoiceOSOverlayManager(private val context: Context) {
    companion object {
        private const val TAG = "VoiceOSOverlayManager"

        @Volatile
        private var instance: VoiceOSOverlayManager? = null

        fun getInstance(context: Context): VoiceOSOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceOSOverlayManager(context.applicationContext).also {
                    instance = it
                    Log.i(TAG, "VoiceOSOverlayManager created")
                }
            }
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private val debugFAB by lazy { DebugFAB(context, windowManager) }
    private val debugPanel by lazy { DebugPanel(context, windowManager) }
    private val listeningPill by lazy { ListeningPill(context, windowManager) }
    private val commandBar by lazy { CommandBar(context, windowManager) }
    private val numberOverlay by lazy { EnhancedNumberOverlay(context, windowManager) }
    private val feedbackToast by lazy { FeedbackToast(context, windowManager) }

    private var currentState = VoiceOSOverlayState.HIDDEN
    private var debugModeEnabled = false

    // ===== DEBUG MODE =====

    fun setDebugMode(enabled: Boolean) {
        debugModeEnabled = enabled
        if (enabled) {
            debugFAB.show()
            debugFAB.setOnClickListener { toggleDebugPanel() }
        } else {
            debugFAB.hide()
            debugPanel.hide()
        }
        Log.i(TAG, "Debug mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun toggleDebugPanel() {
        if (debugPanel.isVisible()) debugPanel.hide() else debugPanel.show()
    }

    fun updateDebugStatus(status: DebugFABStatus) { debugFAB.updateStatus(status) }
    fun updateDebugPanelState(state: DebugPanelState) { debugPanel.updateState(state) }

    fun logRecognition(text: String, confidence: Float, isCommand: Boolean) {
        debugPanel.addRecognitionEntry(RecognitionEntry(System.currentTimeMillis(), text, confidence, isCommand))
    }

    fun updateAccessibilityTree(nodes: List<A11yTreeNode>) { debugPanel.updateAccessibilityTree(nodes) }
    fun updateGeneratedCommands(commands: List<GeneratedCommand>) { debugPanel.updateGeneratedCommands(commands) }

    // ===== WAKE WORD / LISTENING =====

    fun onWakeWordDetected() {
        hideCurrentOverlay()
        listeningPill.showWakeDetected()
        currentState = VoiceOSOverlayState.LISTENING_PILL
        updateDebugStatus(DebugFABStatus.LISTENING)
        Log.i(TAG, "Wake word detected")
    }

    fun showListening() {
        hideCurrentOverlay()
        listeningPill.showListening()
        currentState = VoiceOSOverlayState.LISTENING_PILL
        updateDebugStatus(DebugFABStatus.LISTENING)
    }

    fun updatePartialRecognition(text: String) { listeningPill.updatePartialText(text) }
    fun showProcessing() { listeningPill.showProcessing(); updateDebugStatus(DebugFABStatus.PROCESSING) }

    fun showTimeout() {
        listeningPill.showTimeout()
        handler.postDelayed({ hideAll() }, 1500)
    }

    // ===== COMMAND BAR =====

    fun showCommandBar() {
        hideCurrentOverlay()
        commandBar.show(CommandBar.defaultCategories())
        currentState = VoiceOSOverlayState.COMMAND_BAR
        Log.i(TAG, "Showing command bar")
    }

    fun showCommandBar(categories: List<CommandCategory>) {
        hideCurrentOverlay()
        commandBar.show(categories)
        currentState = VoiceOSOverlayState.COMMAND_BAR
    }

    fun setCommandBarListening(listening: Boolean, partialText: String = "") { commandBar.setListening(listening, partialText) }
    fun setOnCommandSelected(callback: (QuickCommand) -> Unit) { commandBar.setOnCommandSelectedListener(callback) }

    // ===== NUMBER OVERLAY =====

    fun showNumberOverlay(items: List<EnhancedSelectableItem>) {
        hideCurrentOverlay()
        numberOverlay.showItems(items)
        currentState = VoiceOSOverlayState.NUMBER_OVERLAY
        updateDebugStatus(DebugFABStatus.ACTIVE)
        Log.i(TAG, "Showing number overlay with ${items.size} items")
    }

    fun showNumberOverlay(elements: List<ElementData>, onSelect: (Int) -> Unit) {
        val items = elements.mapIndexed { index, element ->
            EnhancedSelectableItem(index + 1, element.label, element.bounds, element.isForeground, element.windowTitle) { onSelect(index) }
        }
        showNumberOverlay(items)
    }

    fun selectNumber(number: Int): Boolean = numberOverlay.selectItem(number)
    fun setShowBackgroundElements(show: Boolean) { numberOverlay.setShowBackgroundElements(show) }

    // ===== FEEDBACK =====

    fun showSuccess(message: String, details: String? = null) {
        feedbackToast.showSuccess(message, details)
        currentState = VoiceOSOverlayState.FEEDBACK
        updateDebugStatus(DebugFABStatus.ACTIVE)
    }

    fun showError(message: String, details: String? = null) {
        feedbackToast.showError(message, details)
        currentState = VoiceOSOverlayState.FEEDBACK
        updateDebugStatus(DebugFABStatus.ERROR)
    }

    fun showInfo(message: String, details: String? = null) {
        feedbackToast.showInfo(message, details)
        currentState = VoiceOSOverlayState.FEEDBACK
    }

    fun showWarning(message: String, details: String? = null) {
        feedbackToast.showWarning(message, details)
        currentState = VoiceOSOverlayState.FEEDBACK
    }

    // ===== STATE MANAGEMENT =====

    fun getCurrentState(): VoiceOSOverlayState = currentState

    private fun hideCurrentOverlay() {
        when (currentState) {
            VoiceOSOverlayState.LISTENING_PILL -> listeningPill.hide()
            VoiceOSOverlayState.COMMAND_BAR -> commandBar.hide()
            VoiceOSOverlayState.NUMBER_OVERLAY -> numberOverlay.hide()
            VoiceOSOverlayState.FEEDBACK -> feedbackToast.hide()
            VoiceOSOverlayState.HIDDEN -> { }
        }
    }

    fun hideAll() {
        listeningPill.hide()
        commandBar.hide()
        numberOverlay.hide()
        feedbackToast.hide()
        currentState = VoiceOSOverlayState.HIDDEN
        updateDebugStatus(DebugFABStatus.IDLE)
        Log.i(TAG, "All overlays hidden")
    }

    fun dispose() {
        debugFAB.dispose()
        debugPanel.dispose()
        listeningPill.dispose()
        commandBar.dispose()
        numberOverlay.dispose()
        feedbackToast.dispose()
        instance = null
        Log.i(TAG, "VoiceOSOverlayManager disposed")
    }
}
