package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

/**
 * CommandBarAutoHide - Manages auto-hide behavior for command bar
 *
 * FR-010: Command Bar Auto-Hide
 * - Auto-hides after configured delay when idle
 * - Resets timer on user interaction
 * - Respects settings (enabled/disabled, delay duration)
 *
 * @param isVisible Current visibility state
 * @param autoHideEnabled Whether auto-hide is enabled in settings
 * @param delayMs Delay in milliseconds before hiding (from settings)
 * @param onHide Callback to hide the command bar
 * @param onResetTimer Callback when timer should be reset (returns reset function)
 */
@Composable
fun rememberCommandBarAutoHide(
    isVisible: Boolean,
    autoHideEnabled: Boolean,
    delayMs: Long,
    onHide: () -> Unit
): CommandBarAutoHideState {
    val state = remember { CommandBarAutoHideState() }

    // Update state when settings change
    LaunchedEffect(autoHideEnabled, delayMs) {
        state.autoHideEnabled = autoHideEnabled
        state.delayMs = delayMs
    }

    // Auto-hide timer
    LaunchedEffect(isVisible, state.lastInteractionTime, autoHideEnabled, delayMs) {
        if (isVisible && autoHideEnabled && delayMs > 0) {
            delay(delayMs)
            // Only hide if no interaction occurred during delay
            if (state.lastInteractionTime == 0L ||
                (System.currentTimeMillis() - state.lastInteractionTime) >= delayMs) {
                onHide()
            }
        }
    }

    return state
}

/**
 * State holder for command bar auto-hide
 */
class CommandBarAutoHideState {
    var autoHideEnabled by mutableStateOf(false)
    var delayMs by mutableStateOf(5000L)
    var lastInteractionTime by mutableLongStateOf(0L)
        private set

    /**
     * Call this when user interacts with command bar to reset timer
     */
    fun recordInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * Reset interaction tracking
     */
    fun reset() {
        lastInteractionTime = 0L
    }
}
