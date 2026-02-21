// filename: Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/settings/RAGSettingsSection.kt
// created: 2025-11-22
// author: RAG Settings Integration Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.chat.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * RAG Settings Section for Chat UI (Phase 2 - Task 1)
 *
 * Displays RAG configuration options:
 * - Enable/disable toggle
 * - Document selector button
 * - Similarity threshold slider
 *
 * Design:
 * - Material 3 components
 * - Proper spacing and labels
 * - Visual feedback for user actions
 * - Disabled state when RAG is off
 *
 * Usage:
 * ```kotlin
 * RAGSettingsSection(
 *     ragEnabled = ragEnabled,
 *     selectedDocumentCount = selectedDocuments.size,
 *     ragThreshold = threshold,
 *     onRagEnabledChange = { enabled -> viewModel.setRagEnabled(enabled) },
 *     onSelectDocuments = { showDocumentSelector = true },
 *     onThresholdChange = { threshold -> viewModel.setRagThreshold(threshold) }
 * )
 * ```
 */
@Composable
fun RAGSettingsSection(
    ragEnabled: Boolean,
    selectedDocumentCount: Int,
    ragThreshold: Float,
    onRagEnabledChange: (Boolean) -> Unit,
    onSelectDocuments: () -> Unit,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AvanueTheme.colors.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            Text(
                text = "RAG (Retrieval-Augmented Generation)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AvanueTheme.colors.textSecondary
            )

            HorizontalDivider()

            // Enable/Disable Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable RAG",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AvanueTheme.colors.textSecondary
                    )
                    Text(
                        text = "Use documents to enhance responses",
                        style = MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = ragEnabled,
                    onCheckedChange = onRagEnabledChange
                )
            }

            // Document Selector Button
            OutlinedButton(
                onClick = onSelectDocuments,
                enabled = ragEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Documents",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (selectedDocumentCount > 0) {
                                "$selectedDocumentCount document(s) selected"
                            } else {
                                "No documents selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedDocumentCount > 0) {
                                AvanueTheme.colors.primary
                            } else {
                                AvanueTheme.colors.textPrimary.copy(alpha = 0.6f)
                            }
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Select documents"
                    )
                }
            }

            // Similarity Threshold Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Similarity Threshold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ragEnabled) {
                            AvanueTheme.colors.textSecondary
                        } else {
                            AvanueTheme.colors.textSecondary.copy(alpha = 0.4f)
                        }
                    )
                    Text(
                        text = String.format("%.2f", ragThreshold),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (ragEnabled) {
                            AvanueTheme.colors.primary
                        } else {
                            AvanueTheme.colors.textSecondary.copy(alpha = 0.4f)
                        }
                    )
                }

                Slider(
                    value = ragThreshold,
                    onValueChange = onThresholdChange,
                    enabled = ragEnabled,
                    valueRange = 0.5f..1.0f,
                    steps = 9, // 0.05 increments
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Higher threshold = more relevant results only",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ragEnabled) {
                        AvanueTheme.colors.textSecondary.copy(alpha = 0.7f)
                    } else {
                        AvanueTheme.colors.textSecondary.copy(alpha = 0.4f)
                    }
                )
            }

            // Info Message
            if (ragEnabled && selectedDocumentCount == 0) {
                Surface(
                    color = AvanueTheme.colors.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Info,
                            contentDescription = "Info",
                            tint = AvanueTheme.colors.onTertiaryContainer
                        )
                        Text(
                            text = "Please select at least one document to enable RAG",
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}
