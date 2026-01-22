package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.OceanTheme

/**
 * Dialog shown on app startup if crash recovery session is detected
 *
 * Features:
 * - Shows tab count from crashed session
 * - Restore button (primary action)
 * - Dismiss button (starts fresh)
 * - Cannot be dismissed by outside click (critical decision)
 *
 * @param visible Whether dialog should be shown
 * @param tabCount Number of tabs in crashed session
 * @param onRestore Callback when user chooses to restore tabs
 * @param onDismiss Callback when user chooses to start fresh
 * @param modifier Modifier for customization
 */
@Composable
fun SessionRestoreDialog(
    visible: Boolean,
    tabCount: Int,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss by outside click - critical decision */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = "Restore session",
                tint = OceanTheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Restore Previous Session?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "WebAvanue didn't close properly last time.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Restore $tabCount ${if (tabCount == 1) "tab" else "tabs"}?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OceanTheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanTheme.primary
                )
            ) {
                Text("Restore Tabs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Start Fresh")
            }
        },
        modifier = modifier
    )
}
