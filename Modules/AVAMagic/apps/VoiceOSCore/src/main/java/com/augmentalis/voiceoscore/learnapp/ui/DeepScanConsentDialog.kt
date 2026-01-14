/**
 * DeepScanConsentDialog.kt - Consent dialog for deep scanning hidden menus
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Shows popup when hidden menu/drawer items are detected, asking user
 * if they want to enable voice commands for those items.
 */

package com.augmentalis.voiceoscore.learnapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Deep Scan Consent Dialog
 *
 * Prompts user to enable voice commands for hidden menu/drawer items.
 *
 * User Options:
 * - **Yes**: Run deep scan now, discover hidden items
 * - **Skip**: Ask again next time hidden menus detected
 * - **No**: Never ask again for this app
 *
 * @param packageName Package name of the app
 * @param appName Human-readable app name
 * @param expandableCount Number of expandable controls found
 * @param onYes Callback when user clicks "Yes" - perform deep scan
 * @param onSkip Callback when user clicks "Skip" - ask again later
 * @param onNo Callback when user clicks "No" - never ask again
 * @param onDismiss Callback when dialog is dismissed (same as Skip)
 */
@Composable
fun DeepScanConsentDialog(
    packageName: String,
    appName: String,
    expandableCount: Int,
    onYes: () -> Unit,
    onSkip: () -> Unit,
    onNo: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false  // Require explicit choice
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Hidden Menu",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Title
                Text(
                    text = "Hidden Menu Items Found!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Message
                Text(
                    text = "I discovered $expandableCount hidden menu${if (expandableCount > 1) "s" else ""} in $appName.\n\n" +
                            "Shall I review ${if (expandableCount > 1) "them" else "it"} to enable voice commands?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // What happens info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "What happens:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Menus will briefly open and close",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Voice commands will be created",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Takes ~2-5 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Yes button (primary)
                    Button(
                        onClick = onYes,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Yes, Review Now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Skip button (secondary)
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Skip (Ask Again Later)",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // No button (text)
                    TextButton(
                        onClick = onNo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No, Never Ask for This App",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Deep Scan Consent Response
 *
 * User's response to deep scan consent dialog.
 */
enum class DeepScanConsentResponse {
    YES,        // Run deep scan now
    SKIP,       // Ask again next time
    NO,         // Never ask again for this app
    DISMISSED   // Dialog dismissed (same as SKIP)
}
