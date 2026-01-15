/**
 * ProgressOverlay.kt - Progress overlay Compose UI
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/learnapp/ui/ProgressOverlay.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Jetpack Compose UI for exploration progress overlay
 */

package com.augmentalis.voiceoscore.learnapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.learnapp.models.ExplorationProgress

/**
 * Progress Overlay
 *
 * Shows real-time exploration progress with pause/stop controls.
 *
 * ## UI Layout
 *
 * ```
 * ┌────────────────────────────────────────┐
 * │  Learning Instagram...                 │
 * │                                        │
 * │  📱 Screens Explored: 15 / ~30        │
 * │  🎯 Elements Mapped: 234               │
 * │  ⏱️  Time Elapsed: 02:35               │
 * │                                        │
 * │  Current: Profile → Settings           │
 * │                                        │
 * │  [████████████░░░░░░░░] 60%           │
 * │                                        │
 * │  ┌────────┐            ┌────────┐    │
 * │  │ Pause  │            │  Stop  │    │
 * │  └────────┘            └────────┘    │
 * │                                        │
 * └────────────────────────────────────────┘
 * ```
 *
 * @param progress Exploration progress
 * @param onPause Callback for pause button
 * @param onStop Callback for stop button
 *
 * @since 1.0.0
 */
@Composable
fun ProgressOverlay(
    progress: ExplorationProgress,
    onPause: () -> Unit,
    onStop: () -> Unit
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
                .padding(20.dp)
        ) {
            // Title
            Text(
                text = "Learning ${progress.appName}...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            ProgressStatsRow(
                icon = "📱",
                label = "Screens Explored",
                value = "${progress.screensExplored} / ~${progress.estimatedTotalScreens}"
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProgressStatsRow(
                icon = "🎯",
                label = "Elements Mapped",
                value = progress.elementsDiscovered.toString()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProgressStatsRow(
                icon = "⏱️",
                label = "Time Elapsed",
                value = progress.formatDuration()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Current: ${progress.currentScreen}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                LinearProgressIndicator(
                    progress = { progress.calculatePercentage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${(progress.calculatePercentage() * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pause button
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Pause")
                }

                // Stop button
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop")
                }
            }

            // Timeout warning (if getting close)
            if (progress.elapsedTimeMs > 25 * 60 * 1000L) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "⚠️ Approaching time limit (30 min)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Progress Stats Row
 *
 * Displays a single stat row with icon, label, and value.
 *
 * @param icon Icon emoji
 * @param label Label text
 * @param value Value text
 */
@Composable
private fun ProgressStatsRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Progress Overlay Compact
 *
 * Smaller version of progress overlay for less intrusive display.
 *
 * @param progress Exploration progress
 * @param onExpand Callback to expand to full view
 */
@Composable
fun ProgressOverlayCompact(
    progress: ExplorationProgress,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onExpand
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Learning ${progress.appName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${progress.screensExplored} screens • ${progress.formatDuration()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CircularProgressIndicator(
                progress = { progress.calculatePercentage() },
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp
            )
        }
    }
}
