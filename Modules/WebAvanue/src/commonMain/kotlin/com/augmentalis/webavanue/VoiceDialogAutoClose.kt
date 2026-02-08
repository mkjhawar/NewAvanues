package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import kotlinx.coroutines.delay

/**
 * VoiceDialogAutoClose - Manages auto-close behavior for voice dialog
 *
 * FR-011: Voice Dialog Auto-Close
 * - Automatically closes voice dialog after successful command execution
 * - Provides brief feedback before closing
 * - Respects settings (enabled/disabled, configurable delay)
 *
 * @param isVisible Current visibility state of voice dialog
 * @param autoCloseEnabled Whether auto-close is enabled in settings
 * @param delayMs Delay in milliseconds before auto-closing (from settings)
 * @param commandExecuted Whether a command was just executed
 * @param onClose Callback to close the voice dialog
 */
@Composable
fun rememberVoiceDialogAutoClose(
    isVisible: Boolean,
    autoCloseEnabled: Boolean,
    delayMs: Long,
    commandExecuted: Boolean,
    onClose: () -> Unit
): VoiceDialogAutoCloseState {
    val state = remember { VoiceDialogAutoCloseState() }

    // Update state when settings change
    LaunchedEffect(autoCloseEnabled, delayMs) {
        state.autoCloseEnabled = autoCloseEnabled
        state.delayMs = delayMs
    }

    // Auto-close after command execution
    LaunchedEffect(isVisible, commandExecuted, autoCloseEnabled, delayMs) {
        if (isVisible && commandExecuted && autoCloseEnabled && delayMs > 0) {
            // Brief delay to show feedback (configurable)
            delay(delayMs)
            onClose()
        }
    }

    return state
}

/**
 * State holder for voice dialog auto-close
 */
class VoiceDialogAutoCloseState {
    var autoCloseEnabled by mutableStateOf(true)
    var delayMs by mutableStateOf(2000L)

    /**
     * Show confirmation feedback before auto-closing
     */
    var showFeedback by mutableStateOf(false)
        private set

    fun setFeedback(show: Boolean) {
        showFeedback = show
    }
}

/**
 * VoiceCommandFeedback - Shows brief confirmation when command executes
 */
@Composable
fun VoiceCommandFeedback(
    command: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = AvanueTheme.colors.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = AvanueTheme.colors.success,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = command,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AvanueTheme.colors.textPrimary
                )
            }
        }
    }
}
