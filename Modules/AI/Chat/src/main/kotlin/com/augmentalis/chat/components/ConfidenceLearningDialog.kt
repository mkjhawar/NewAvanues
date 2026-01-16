package com.augmentalis.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * State for the confidence learning dialog.
 *
 * @property userInput The original user query
 * @property interpretedIntent The intent NLU thought it understood
 * @property confidence NLU confidence score (0.0 to 1.0)
 * @property alternateIntents List of alternate intents with their confidence scores
 */
data class ConfidenceLearningState(
    val userInput: String,
    val interpretedIntent: String,
    val confidence: Float,
    val alternateIntents: List<AlternateIntent> = emptyList()
)

/**
 * Alternate intent option with confidence score.
 *
 * @property intentId Intent identifier
 * @property displayName Human-readable intent name
 * @property confidence Confidence score (0.0 to 1.0)
 */
data class AlternateIntent(
    val intentId: String,
    val displayName: String,
    val confidence: Float
)

/**
 * Interactive confidence learning dialog (REQ-004).
 *
 * Shows what NLU thought it understood and offers alternatives when confidence is below threshold.
 * Allows users to confirm or correct NLU's interpretation, learning from their feedback.
 *
 * **User Flow:**
 * 1. Dialog shows: "I think you meant: [interpreted intent]"
 * 2. Show confidence score
 * 3. User options:
 *    - YES: Confirm interpretation → Add to learned database
 *    - NO: Show alternates → User selects correct one → Add to database
 *    - SKIP: Dismiss without learning
 *
 * @param state Dialog state with user input, interpreted intent, alternates
 * @param onConfirm Called when user confirms interpretation (YES button)
 * @param onSelectAlternate Called when user selects alternate intent
 * @param onDismiss Called when user dismisses dialog (SKIP button)
 */
@Composable
fun ConfidenceLearningDialog(
    state: ConfidenceLearningState,
    onConfirm: () -> Unit,
    onSelectAlternate: (AlternateIntent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAlternates by remember { mutableStateOf(false) }
    var selectedAlternate by remember { mutableStateOf<AlternateIntent?>(null) }

    // Ocean Glass Design v2.3 - Solid colors to prevent transparency stacking
    val OceanSolidBackground = Color(0xFF1E293B)  // Solid slate instead of transparent glass
    val CoralBlue = Color(0xFF3B82F6)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = OceanSolidBackground,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.9f),
        iconContentColor = CoralBlue,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        title = {
            Text(
                text = "Did I understand you correctly?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Ocean Glass colors for text
            val textPrimary = Color.White
            val textSecondary = Color.White.copy(alpha = 0.7f)
            val accentColor = CoralBlue

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User's original query
                Text(
                    text = "You said:",
                    style = MaterialTheme.typography.labelMedium,
                    color = textSecondary
                )
                Text(
                    text = "\"${state.userInput}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NLU's interpretation
                if (!showAlternates) {
                    Text(
                        text = "I think you meant:",
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondary
                    )
                    Text(
                        text = formatIntentName(state.interpretedIntent),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    Text(
                        text = "Confidence: ${(state.confidence * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                } else {
                    // Show alternates
                    Text(
                        text = "Which one did you mean?",
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondary
                    )

                    if (state.alternateIntents.isEmpty()) {
                        Text(
                            text = "No alternatives available. Please try rephrasing your query.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEF4444) // Red for error
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            state.alternateIntents.forEach { alternate ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selectedAlternate == alternate,
                                            onClick = { selectedAlternate = alternate }
                                        )
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedAlternate == alternate,
                                        onClick = { selectedAlternate = alternate }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = formatIntentName(alternate.displayName),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "${(alternate.confidence * 100).roundToInt()}% confidence",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showAlternates) {
                // Initial view: YES/NO buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onConfirm) {
                        Text("YES", color = CoralBlue)
                    }
                    TextButton(
                        onClick = {
                            if (state.alternateIntents.isNotEmpty()) {
                                showAlternates = true
                            } else {
                                // No alternates available, just dismiss
                                onDismiss()
                            }
                        }
                    ) {
                        Text("NO", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            } else {
                // Alternates view: CONFIRM/BACK buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedAlternate?.let { onSelectAlternate(it) }
                        },
                        enabled = selectedAlternate != null
                    ) {
                        Text(
                            "CONFIRM",
                            color = if (selectedAlternate != null) CoralBlue else Color.White.copy(alpha = 0.3f)
                        )
                    }
                    TextButton(
                        onClick = { showAlternates = false }
                    ) {
                        Text("BACK", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("SKIP", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}

/**
 * Format intent name for display.
 *
 * Converts SNAKE_CASE or camelCase to Title Case.
 *
 * Examples:
 * - "LIGHT_ON" → "Light On"
 * - "send_email" → "Send Email"
 * - "turnOnLights" → "Turn On Lights"
 *
 * @param intentId Intent identifier from NLU
 * @return Formatted display name
 */
private fun formatIntentName(intentId: String): String {
    return intentId
        .replace("_", " ")
        .replace(Regex("([a-z])([A-Z])"), "$1 $2") // camelCase to spaces
        .split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
}
