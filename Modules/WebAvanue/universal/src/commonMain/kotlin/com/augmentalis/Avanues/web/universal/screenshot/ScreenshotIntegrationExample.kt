package com.augmentalis.Avanues.web.universal.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * Example integration of screenshot feature into browser UI
 *
 * This file demonstrates how to:
 * 1. Add a screenshot button to the browser menu
 * 2. Handle screenshot workflow with dialogs
 * 3. Show notifications after capture
 * 4. Integrate with TabViewModel
 *
 * Usage:
 * Add this composable to your browser toolbar or menu:
 * ```kotlin
 * ScreenshotButton(
 *     webView = currentWebView,
 *     onScreenshotCaptured = { filepath ->
 *         // Show notification or toast
 *     }
 * )
 * ```
 */

/**
 * Screenshot button with full workflow
 *
 * @param webView Current WebView instance (platform-specific)
 * @param onScreenshotCaptured Callback when screenshot is successfully captured
 * @param onError Callback when screenshot fails
 */
@Composable
fun ScreenshotButton(
    webView: Any?,
    onScreenshotCaptured: (String?) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val screenshotManager = remember { ScreenshotManager(scope) }

    var screenshotState by remember { mutableStateOf<ScreenshotState>(ScreenshotState.Idle) }

    // Screenshot button
    IconButton(
        onClick = {
            if (webView != null) {
                screenshotManager.startScreenshotCapture(webView) { state ->
                    screenshotState = state
                }
            } else {
                onError("No active page to capture")
            }
        }
    ) {
        Icon(
            imageVector = Icons.Default.Share, // Replace with camera icon
            contentDescription = "Take Screenshot"
        )
    }

    // Handle screenshot states with dialogs
    when (val state = screenshotState) {
        ScreenshotState.SelectingType -> {
            ScreenshotTypeDialog(
                onDismiss = {
                    screenshotManager.reset { screenshotState = it }
                },
                onSelectType = { type ->
                    screenshotManager.captureScreenshot(
                        type = type,
                        quality = 80,
                        saveToGallery = true
                    ) { newState ->
                        screenshotState = newState
                    }
                }
            )
        }

        is ScreenshotState.Capturing -> {
            ScreenshotProgressDialog(
                progress = state.progress,
                message = state.message,
                onCancel = {
                    screenshotManager.cancelCapture { screenshotState = it }
                }
            )
        }

        is ScreenshotState.Success -> {
            // Notify caller
            LaunchedEffect(state.filepath) {
                onScreenshotCaptured(state.filepath)
            }

            ScreenshotPreviewDialog(
                screenshotPath = state.filepath,
                onSave = {
                    // Already saved, just dismiss
                    screenshotManager.reset { screenshotState = it }
                },
                onShare = {
                    state.filepath?.let { path ->
                        scope.launch {
                            screenshotManager.shareScreenshot(path)
                            screenshotManager.reset { screenshotState = it }
                        }
                    }
                },
                onDismiss = {
                    screenshotManager.reset { screenshotState = it }
                }
            )
        }

        is ScreenshotState.Error -> {
            // Notify caller
            LaunchedEffect(state.error) {
                onError(state.error)
            }

            ScreenshotErrorDialog(
                error = state.error,
                onDismiss = {
                    screenshotManager.reset { screenshotState = it }
                },
                onRetry = {
                    screenshotManager.startScreenshotCapture(webView!!) { newState ->
                        screenshotState = newState
                    }
                }
            )
        }

        ScreenshotState.Idle -> {
            // No dialog shown
        }
    }

    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            screenshotManager.cleanup()
        }
    }
}

/**
 * Example: Screenshot menu item in overflow menu
 */
@Composable
fun ScreenshotMenuItem(
    webView: Any?,
    onDismissMenu: () -> Unit,
    onScreenshotCaptured: (String?) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val screenshotManager = remember { ScreenshotManager(scope) }

    var screenshotState by remember { mutableStateOf<ScreenshotState>(ScreenshotState.Idle) }

    DropdownMenuItem(
        text = { Text("Screenshot") },
        onClick = {
            onDismissMenu()
            if (webView != null) {
                screenshotManager.startScreenshotCapture(webView) { state ->
                    screenshotState = state
                }
            } else {
                onError("No active page to capture")
            }
        }
    )

    // Handle states (same as ScreenshotButton)
    when (val state = screenshotState) {
        ScreenshotState.SelectingType -> {
            ScreenshotTypeDialog(
                onDismiss = { screenshotManager.reset { screenshotState = it } },
                onSelectType = { type ->
                    screenshotManager.captureScreenshot(type) { screenshotState = it }
                }
            )
        }

        is ScreenshotState.Capturing -> {
            ScreenshotProgressDialog(
                progress = state.progress,
                message = state.message,
                onCancel = { screenshotManager.cancelCapture { screenshotState = it } }
            )
        }

        is ScreenshotState.Success -> {
            LaunchedEffect(state.filepath) {
                onScreenshotCaptured(state.filepath)
            }

            ScreenshotPreviewDialog(
                screenshotPath = state.filepath,
                onSave = { screenshotManager.reset { screenshotState = it } },
                onShare = {
                    state.filepath?.let { path ->
                        scope.launch {
                            screenshotManager.shareScreenshot(path)
                            screenshotManager.reset { screenshotState = it }
                        }
                    }
                },
                onDismiss = { screenshotManager.reset { screenshotState = it } }
            )
        }

        is ScreenshotState.Error -> {
            LaunchedEffect(state.error) {
                onError(state.error)
            }

            ScreenshotErrorDialog(
                error = state.error,
                onDismiss = { screenshotManager.reset { screenshotState = it } }
            )
        }

        ScreenshotState.Idle -> {}
    }
}

/**
 * Example: Programmatic screenshot capture
 *
 * Call this from voice command or other automation
 */
@Composable
fun rememberScreenshotCapture(
    webView: Any?,
    onComplete: (String?) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()

    return remember(webView) {
        {
            if (webView == null) {
                onError("No active page")
                return@remember
            }

            val capture = createScreenshotCapture(webView)

            scope.launch {
                capture.capture(
                    ScreenshotRequest(
                        type = ScreenshotType.VISIBLE_AREA,
                        quality = 80,
                        saveToGallery = true
                    )
                ).collect { result ->
                    when (result) {
                        is ScreenshotResult.Success -> {
                            onComplete(result.filepath)
                        }
                        is ScreenshotResult.Error -> {
                            onError(result.error)
                        }
                        is ScreenshotResult.Progress -> {
                            // Ignore progress for programmatic capture
                        }
                    }
                }
            }
        }
    }
}
