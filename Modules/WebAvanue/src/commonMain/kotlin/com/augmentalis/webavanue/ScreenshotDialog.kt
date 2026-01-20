package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ScreenshotType

/**
 * Dialog for selecting screenshot type
 *
 * Presents options:
 * - Visible Area: Capture current viewport
 * - Full Page: Capture entire page (scroll and stitch)
 */
@Composable
fun ScreenshotTypeDialog(
    onDismiss: () -> Unit,
    onSelectType: (ScreenshotType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Capture Screenshot")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Select screenshot type:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Visible Area option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSelectType(ScreenshotType.VISIBLE_AREA)
                        onDismiss()
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Visible Area",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Capture only what's currently visible on screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Full Page option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onSelectType(ScreenshotType.FULL_PAGE)
                        onDismiss()
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Full Page",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Capture entire webpage by scrolling and stitching",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Progress dialog shown during screenshot capture
 *
 * Displays:
 * - Progress indicator
 * - Progress percentage
 * - Status message
 * - Cancel button for full page capture
 */
@Composable
fun ScreenshotProgressDialog(
    progress: Float,
    message: String,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss during capture */ },
        title = {
            Text("Capturing Screenshot")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Preview dialog showing captured screenshot
 *
 * Displays:
 * - Screenshot preview (platform-specific image rendering)
 * - Actions: Save, Share, Cancel
 */
@Composable
fun ScreenshotPreviewDialog(
    screenshotPath: String?,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Screenshot Captured")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                screenshotPath?.let {
                    Text(
                        "Screenshot saved to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Text(
                    "Screenshot captured in memory",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (screenshotPath != null) {
                    TextButton(onClick = onShare) {
                        Text("Share")
                    }
                } else {
                    TextButton(onClick = onSave) {
                        Text("Save")
                    }
                }

                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = null
    )
}

/**
 * Error dialog for screenshot capture failures
 */
@Composable
fun ScreenshotErrorDialog(
    error: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Screenshot Failed")
        },
        text = {
            Text(error)
        },
        confirmButton = {
            if (onRetry != null) {
                Button(onClick = {
                    onRetry()
                    onDismiss()
                }) {
                    Text("Retry")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = if (onRetry != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        } else null
    )
}
