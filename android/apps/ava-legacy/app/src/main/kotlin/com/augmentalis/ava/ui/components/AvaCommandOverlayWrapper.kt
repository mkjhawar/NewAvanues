/**
 * AVA Command Overlay Wrapper
 *
 * Wraps any screen content with the command overlay and trigger FAB.
 * This allows the command overlay to be easily added to any screen.
 *
 * Created: 2025-11-08
 * Author: AVA Team
 */

package com.augmentalis.ava.ui.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.augmentalis.nlu.debug.NLUDebugManager
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Wraps content with AVA command overlay
 *
 * Usage in a screen:
 * ```kotlin
 * AvaCommandOverlayWrapper(
 *     navController = navController
 * ) {
 *     YourScreenContent()
 * }
 * ```
 *
 * @param navController Navigation controller for handling navigation commands
 * @param content Screen content to wrap
 */
@Composable
fun AvaCommandOverlayWrapper(
    navController: NavController,
    modifier: Modifier = Modifier,
    currentRoute: String? = null,
    content: @Composable (onTriggerVoiceOverlay: () -> Unit) -> Unit
) {
    var showOverlay by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Ocean Blue theme colors
    val CoralBlue = Color(0xFF3B82F6)

    // FAB positioning - responsive for portrait/landscape
    // Offset from bottom to float above navigation bar (different z-level)
    val fabBottomPadding = if (isLandscape) 24.dp else 72.dp  // Above nav bar
    val fabEndPadding = if (isLandscape) 24.dp else 16.dp

    // Trigger function that can be passed to content
    val triggerVoiceOverlay: () -> Unit = { showOverlay = true }

    // Hide floating FAB when on chat screen (mic button is in input row)
    val hideFloatingFab = currentRoute == "chat"

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main content - pass the trigger function
        content(triggerVoiceOverlay)

        // Voice command FAB - only show when overlay is NOT visible
        // AND not on chat screen (where voice button is in input row)
        // Prevents duplicate microphone icons
        if (!showOverlay && !hideFloatingFab) {
            FloatingActionButton(
                onClick = { showOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = fabBottomPadding, end = fabEndPadding)
                    .size(56.dp),
                shape = CircleShape,
                containerColor = CoralBlue,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    focusedElevation = 10.dp,
                    hoveredElevation = 10.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Commands",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Command overlay
        AvaCommandOverlay(
            visible = showOverlay,
            onCommand = { command ->
                handleCommand(command, navController, context, scope)
                showOverlay = false
            },
            onDismiss = { showOverlay = false }
        )
    }
}

/**
 * Handle command events
 */
private fun handleCommand(
    command: AvaCommand,
    navController: NavController,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Timber.d("Command triggered: $command")

    when (command) {
        // Chat commands
        AvaCommand.NewConversation -> {
            Timber.i("New conversation")
            // TODO: Implement new conversation logic
        }
        AvaCommand.ShowHistory -> {
            Timber.i("Show history")
            // TODO: Implement history view
        }
        AvaCommand.ClearChat -> {
            Timber.i("Clear chat")
            // TODO: Implement clear chat
        }
        AvaCommand.ExportChat -> {
            Timber.i("Export chat")
            // TODO: Implement export
        }
        AvaCommand.ShowTemplates -> {
            Timber.i("Show templates")
            // TODO: Implement templates
        }
        AvaCommand.StopGeneration -> {
            Timber.i("Stop generation")
            // Note: Core functionality implemented in ChatViewModel.stopGeneration()
            // Integration requires architectural changes:
            // Option 1: Pass callback parameter to AvaCommandOverlayWrapper
            // Option 2: Use event bus (SharedFlow/StateFlow) for app-wide events
            // Option 3: Singleton ChatController with ViewModel reference
            //
            // For now, this command is logged. Full integration tracked in backlog.
            // See: ChatViewModel.stopGeneration() for the actual cancellation logic
        }

        // Teach commands
        AvaCommand.AddExample -> {
            Timber.i("Add example")
            navController.navigate("teach")
        }
        AvaCommand.ViewExamples -> {
            Timber.i("View examples")
            navController.navigate("teach")
        }
        AvaCommand.ManageCategories -> {
            Timber.i("Manage categories")
            navController.navigate("teach")
        }
        AvaCommand.ImportExamples -> {
            Timber.i("Import examples")
            // TODO: Implement import
        }
        AvaCommand.ExportExamples -> {
            Timber.i("Export examples")
            // TODO: Implement export
        }

        // Model commands
        AvaCommand.DownloadModels -> {
            Timber.i("Download models")
            navController.navigate("model_download")
        }
        AvaCommand.SelectModel -> {
            Timber.i("Select model")
            navController.navigate("model_download")
        }
        AvaCommand.ModelSettings -> {
            Timber.i("Model settings")
            navController.navigate("settings")
        }
        AvaCommand.RunTests -> {
            Timber.i("Run tests")
            navController.navigate("test_launcher")
        }
        AvaCommand.ViewModelInfo -> {
            Timber.i("View model info")
            navController.navigate("model_download")
        }

        // Voice/NLU commands
        AvaCommand.ToggleNLU -> {
            Timber.i("Toggle NLU")
            navController.navigate("settings")
        }
        AvaCommand.SetConfidenceThreshold -> {
            Timber.i("Set confidence threshold")
            navController.navigate("settings")
        }
        AvaCommand.SelectLanguage -> {
            Timber.i("Select language")
            navController.navigate("settings")
        }
        AvaCommand.TestVoice -> {
            Timber.i("Test voice")
            // TODO: Implement voice test
        }
        AvaCommand.ViewNLUStats -> {
            Timber.i("View NLU stats")
            // TODO: Implement NLU stats
        }
        AvaCommand.ReloadNLUData -> {
            Timber.i("Reload NLU data from .ava files")
            scope.launch {
                try {
                    Timber.i("Starting NLU data reload...")
                    val result = NLUDebugManager.reloadFromAvaSources(context)
                    result.onSuccess { count ->
                        Timber.i("✅ Successfully loaded $count examples from .ava files")
                        android.widget.Toast.makeText(
                            context,
                            "✅ Loaded $count NLU examples from .ava files",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    result.onFailure { error ->
                        Timber.e("❌ Failed to reload NLU data: ${error.message}")
                        android.widget.Toast.makeText(
                            context,
                            "❌ Failed to reload NLU data: ${error.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Timber.e("❌ Exception during NLU reload: ${e.message}", e)
                    android.widget.Toast.makeText(
                        context,
                        "❌ Exception: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Settings commands
        AvaCommand.ChangeTheme -> {
            Timber.i("Change theme")
            navController.navigate("settings")
        }
        AvaCommand.PrivacySettings -> {
            Timber.i("Privacy settings")
            navController.navigate("settings")
        }
        AvaCommand.ClearCache -> {
            Timber.i("Clear cache")
            navController.navigate("settings")
        }
        AvaCommand.ViewAbout -> {
            Timber.i("View about")
            navController.navigate("settings")
        }
        AvaCommand.OpenSettings -> {
            Timber.i("Open settings")
            navController.navigate("settings")
        }
    }
}
