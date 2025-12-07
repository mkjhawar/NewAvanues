/**
 * ConsentDialog.kt - Compose UI for consent dialog
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Jetpack Compose UI for app learning consent dialog
 */

package com.augmentalis.learnapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Consent Dialog Composable
 *
 * Jetpack Compose UI for asking user permission to learn app.
 *
 * ## UI Layout
 *
 * ```
 * ┌────────────────────────────────────────┐
 * │                                        │
 * │   Do you want VoiceOS to Learn         │
 * │   Instagram?                           │
 * │                                        │
 * │   VoiceOS will explore Instagram       │
 * │   to enable voice commands.            │
 * │                                        │
 * │   This will:                           │
 * │   • Click buttons and menus            │
 * │   • Navigate between screens           │
 * │   • Skip dangerous actions             │
 * │   • Take ~2-5 minutes                  │
 * │                                        │
 * │   ┌────────┐            ┌────────┐    │
 * │   │  Yes   │            │   No   │    │
 * │   └────────┘            └────────┘    │
 * │                                        │
 * │   [ ] Don't ask again for this app    │
 * │                                        │
 * └────────────────────────────────────────┘
 * ```
 *
 * @param appName Human-readable app name
 * @param onApprove Callback when user approves (with dontAskAgain flag)
 * @param onDecline Callback when user declines (with dontAskAgain flag)
 *
 * @since 1.0.0
 */
@Composable
fun ConsentDialog(
    appName: String,
    onApprove: (dontAskAgain: Boolean) -> Unit,
    onDecline: (dontAskAgain: Boolean) -> Unit
) {
    var dontAskAgain by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDecline(dontAskAgain) },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Learn $appName?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "VoiceOS will explore $appName to enable voice commands.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "This will:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BulletPoint("Click buttons and menus")
                        BulletPoint("Navigate between screens")
                        BulletPoint("Skip dangerous actions")
                        BulletPoint("Take ~2-5 minutes")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decline button
                    OutlinedButton(
                        onClick = { onDecline(dontAskAgain) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("No")
                    }

                    // Approve button
                    Button(
                        onClick = { onApprove(dontAskAgain) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Yes")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Don't ask again checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it }
                    )

                    Text(
                        text = "Don't ask again for this app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Bullet Point Composable
 *
 * Displays a bullet point with text.
 *
 * @param text Text to display
 */
@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
