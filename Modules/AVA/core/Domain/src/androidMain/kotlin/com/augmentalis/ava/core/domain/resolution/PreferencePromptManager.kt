package com.augmentalis.ava.core.domain.resolution

import com.augmentalis.ava.core.domain.model.AppResolution
import com.augmentalis.ava.core.domain.model.InstalledApp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app preference prompts in the UI.
 *
 * Responsibilities:
 * - Queue preference prompts (never show multiple simultaneously)
 * - Coordinate between AppResolverService and UI
 * - Handle "always ask" option
 * - Emit events for UI to show/hide prompts
 *
 * Part of Intelligent Resolution System (Chapter 71).
 *
 * Author: Manoj Jhawar
 */
class PreferencePromptManager(
    private val appResolverService: AppResolverService
) {
    // Current prompt being shown
    private val _currentPrompt = MutableStateFlow<AppSelectionPrompt?>(null)
    val currentPrompt: StateFlow<AppSelectionPrompt?> = _currentPrompt.asStateFlow()

    // Queue of pending prompts
    private val promptQueue = mutableListOf<AppSelectionPrompt>()

    // Events for action completion
    private val _actionCompleted = MutableSharedFlow<ActionCompletedEvent>()
    val actionCompleted: SharedFlow<ActionCompletedEvent> = _actionCompleted.asSharedFlow()

    /**
     * Request an app selection prompt for a capability.
     *
     * If called with MultipleAvailable resolution, queues a prompt.
     * If already showing a prompt, adds to queue.
     *
     * @param resolution The resolution result from AppResolverService
     * @param onAppSelected Callback when user selects an app
     * @return True if prompt will be shown, false if already resolved
     */
    suspend fun requestAppSelection(
        resolution: AppResolution.MultipleAvailable,
        onAppSelected: suspend (InstalledApp, Boolean) -> Unit
    ): Boolean {
        val prompt = AppSelectionPrompt(
            capability = resolution.capability,
            capabilityDisplayName = resolution.capabilityDisplayName,
            apps = resolution.apps,
            recommendedIndex = resolution.recommendedIndex,
            onAppSelected = onAppSelected
        )

        return if (_currentPrompt.value == null) {
            // No prompt showing, show immediately
            _currentPrompt.value = prompt
            true
        } else {
            // Already showing a prompt, queue this one
            promptQueue.add(prompt)
            true
        }
    }

    /**
     * User selected an app from the prompt.
     *
     * @param app The selected app
     * @param remember If true, save this preference for future use
     */
    suspend fun onAppSelected(app: InstalledApp, remember: Boolean) {
        val prompt = _currentPrompt.value ?: return

        // Save the preference
        appResolverService.savePreference(
            capability = prompt.capability,
            packageName = app.packageName,
            appName = app.appName,
            remember = remember
        )

        // Invoke the callback
        prompt.onAppSelected(app, remember)

        // Emit completion event
        _actionCompleted.emit(
            ActionCompletedEvent(
                capability = prompt.capability,
                selectedApp = app,
                remembered = remember
            )
        )

        // Clear current prompt and show next if queued
        showNextPrompt()
    }

    /**
     * User dismissed the prompt without selecting.
     */
    suspend fun onPromptDismissed() {
        _currentPrompt.value = null
        showNextPrompt()
    }

    /**
     * Clear all pending prompts.
     */
    fun clearAllPrompts() {
        promptQueue.clear()
        _currentPrompt.value = null
    }

    /**
     * Check if any prompt is currently showing.
     */
    fun isPromptShowing(): Boolean = _currentPrompt.value != null

    /**
     * Get the number of queued prompts.
     */
    fun getQueueSize(): Int = promptQueue.size

    private suspend fun showNextPrompt() {
        if (promptQueue.isNotEmpty()) {
            _currentPrompt.value = promptQueue.removeAt(0)
        } else {
            _currentPrompt.value = null
        }
    }
}

/**
 * Data for an app selection prompt.
 */
data class AppSelectionPrompt(
    val capability: String,
    val capabilityDisplayName: String,
    val apps: List<InstalledApp>,
    val recommendedIndex: Int,
    val onAppSelected: suspend (InstalledApp, Boolean) -> Unit
)

/**
 * Event emitted when an action completes after app selection.
 */
data class ActionCompletedEvent(
    val capability: String,
    val selectedApp: InstalledApp,
    val remembered: Boolean
)
