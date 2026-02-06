package com.augmentalis.webavanue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avanues.themes.OceanTheme

/**
 * VoiceCommandHandler - Composable that shows voice listening state and feedback
 *
 * @param isListening Whether currently listening for voice
 * @param voiceState Current state of voice recognition
 * @param lastCommand Last recognized command (for feedback)
 * @param onStartListening Callback to start listening
 * @param onStopListening Callback to stop listening
 * @param modifier Modifier for customization
 */
@Composable
fun VoiceListeningIndicator(
    isListening: Boolean,
    lastCommand: String? = null,
    modifier: Modifier = Modifier
) {
    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    AnimatedVisibility(
        visible = isListening || lastCommand != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Listening indicator with pulsing effect
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .background(
                            color = OceanTheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = OceanTheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Listening",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "Listening...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textPrimary
                )
            }

            // Show last recognized command
            lastCommand?.let { command ->
                Surface(
                    color = OceanTheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "\"$command\"",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OceanTheme.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Voice command execution result for feedback
 */
sealed class VoiceCommandResult {
    data class Success(val message: String) : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
}

/**
 * Execute a voice command and return result
 */
fun executeVoiceCommand(
    command: VoiceCommand,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    onFreezeScroll: () -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: () -> Unit,
    onNextTab: () -> Unit,
    onPreviousTab: () -> Unit,
    onShowTabs: () -> Unit,
    onShowFavorites: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onDesktopMode: () -> Unit,
    onMobileMode: () -> Unit,
    onBookmarkThis: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowHelp: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    onSearch: (String) -> Unit
): VoiceCommandResult {
    return when (command) {
        is VoiceCommand.GoBack -> {
            onBack()
            VoiceCommandResult.Success("Going back")
        }
        is VoiceCommand.GoForward -> {
            onForward()
            VoiceCommandResult.Success("Going forward")
        }
        is VoiceCommand.GoHome -> {
            onHome()
            VoiceCommandResult.Success("Going home")
        }
        is VoiceCommand.Refresh -> {
            onRefresh()
            VoiceCommandResult.Success("Refreshing")
        }
        is VoiceCommand.ScrollUp -> {
            onScrollUp()
            VoiceCommandResult.Success("Scrolling up")
        }
        is VoiceCommand.ScrollDown -> {
            onScrollDown()
            VoiceCommandResult.Success("Scrolling down")
        }
        is VoiceCommand.ScrollToTop -> {
            onScrollToTop()
            VoiceCommandResult.Success("Scrolling to top")
        }
        is VoiceCommand.ScrollToBottom -> {
            onScrollToBottom()
            VoiceCommandResult.Success("Scrolling to bottom")
        }
        is VoiceCommand.FreezeScroll -> {
            onFreezeScroll()
            VoiceCommandResult.Success("Toggling scroll freeze")
        }
        is VoiceCommand.NewTab -> {
            onNewTab()
            VoiceCommandResult.Success("Opening new tab")
        }
        is VoiceCommand.CloseTab -> {
            onCloseTab()
            VoiceCommandResult.Success("Closing tab")
        }
        is VoiceCommand.NextTab -> {
            onNextTab()
            VoiceCommandResult.Success("Next tab")
        }
        is VoiceCommand.PreviousTab -> {
            onPreviousTab()
            VoiceCommandResult.Success("Previous tab")
        }
        is VoiceCommand.ShowTabs -> {
            onShowTabs()
            VoiceCommandResult.Success("Showing tabs")
        }
        is VoiceCommand.ShowFavorites -> {
            onShowFavorites()
            VoiceCommandResult.Success("Showing favorites")
        }
        is VoiceCommand.ZoomIn -> {
            onZoomIn()
            VoiceCommandResult.Success("Zooming in")
        }
        is VoiceCommand.ZoomOut -> {
            onZoomOut()
            VoiceCommandResult.Success("Zooming out")
        }
        is VoiceCommand.ResetZoom -> {
            onResetZoom()
            VoiceCommandResult.Success("Resetting zoom")
        }
        is VoiceCommand.DesktopMode -> {
            onDesktopMode()
            VoiceCommandResult.Success("Desktop mode")
        }
        is VoiceCommand.MobileMode -> {
            onMobileMode()
            VoiceCommandResult.Success("Mobile mode")
        }
        is VoiceCommand.BookmarkThis -> {
            onBookmarkThis()
            VoiceCommandResult.Success("Bookmarking page")
        }
        is VoiceCommand.OpenBookmarks -> {
            onOpenBookmarks()
            VoiceCommandResult.Success("Opening bookmarks")
        }
        is VoiceCommand.OpenDownloads -> {
            onOpenDownloads()
            VoiceCommandResult.Success("Opening downloads")
        }
        is VoiceCommand.OpenHistory -> {
            onOpenHistory()
            VoiceCommandResult.Success("Opening history")
        }
        is VoiceCommand.OpenSettings -> {
            onOpenSettings()
            VoiceCommandResult.Success("Opening settings")
        }
        is VoiceCommand.ShowHelp -> {
            onShowHelp()
            VoiceCommandResult.Success("Showing voice commands")
        }
        is VoiceCommand.GoToUrl -> {
            onNavigateToUrl(command.url)
            VoiceCommandResult.Success("Going to ${command.url}")
        }
        is VoiceCommand.Search -> {
            onSearch(command.query)
            VoiceCommandResult.Success("Searching for ${command.query}")
        }
        is VoiceCommand.Unknown -> {
            VoiceCommandResult.Error("Unknown command: ${command.text}")
        }
    }
}
