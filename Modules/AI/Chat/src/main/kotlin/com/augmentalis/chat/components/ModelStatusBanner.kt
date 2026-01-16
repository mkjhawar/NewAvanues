package com.augmentalis.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.llm.alc.models.MissingModelAction
import com.augmentalis.llm.alc.models.ModelLoadingState
import com.augmentalis.llm.alc.models.ModelTypeInfo

/**
 * Banner component that shows model loading status and actions.
 *
 * Displays contextual information when:
 * - Models are loading
 * - Models are missing
 * - Model loading errors occur
 *
 * Provides action buttons for recovery (download, select different, etc.)
 */
@Composable
fun ModelStatusBanner(
    nluState: ModelLoadingState,
    llmState: ModelLoadingState,
    onAction: (MissingModelAction, ModelTypeInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ocean Glass colors
    val WarningBackground = Color(0xFF422006) // Dark amber
    val ErrorBackground = Color(0xFF450A0A)   // Dark red
    val InfoBackground = Color(0xFF1E293B)    // Ocean slate
    val CoralBlue = Color(0xFF3B82F6)
    val WarningText = Color(0xFFFCD34D)       // Amber text
    val ErrorText = Color(0xFFFCA5A5)         // Red text

    // Determine which state to show (priority: Error > Missing > Loading)
    val stateToShow = when {
        nluState is ModelLoadingState.Error -> nluState
        llmState is ModelLoadingState.Error -> llmState
        nluState is ModelLoadingState.Missing -> nluState
        llmState is ModelLoadingState.Missing -> llmState
        nluState is ModelLoadingState.Loading -> nluState
        llmState is ModelLoadingState.Loading -> llmState
        else -> null
    }

    AnimatedVisibility(
        visible = stateToShow != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        stateToShow?.let { state ->
            val (backgroundColor, iconTint, icon) = when (state) {
                is ModelLoadingState.Error -> Triple(ErrorBackground, ErrorText, Icons.Default.Error)
                is ModelLoadingState.Missing -> Triple(WarningBackground, WarningText, Icons.Default.Warning)
                is ModelLoadingState.Loading -> Triple(InfoBackground, CoralBlue, null)
                else -> Triple(InfoBackground, Color.White, null)
            }

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(backgroundColor)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    when (state) {
                        is ModelLoadingState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CoralBlue,
                                strokeWidth = 2.dp
                            )
                        }
                        else -> {
                            icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title and message
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getStateTitle(state),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getStateMessage(state),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Dismiss button (for non-loading states)
                    if (state !is ModelLoadingState.Loading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Action buttons for missing/error states
                if (state is ModelLoadingState.Missing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAction(state.suggestedAction, state.modelType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralBlue
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.suggestedAction.displayText)
                        }

                        if (state.suggestedAction != MissingModelAction.CONTINUE_WITHOUT) {
                            Button(
                                onClick = { onAction(MissingModelAction.CONTINUE_WITHOUT, state.modelType) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                )
                            ) {
                                Text("Continue Anyway", color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                // Retry button for error states
                if (state is ModelLoadingState.Error && state.isRecoverable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onAction(MissingModelAction.DOWNLOAD, state.modelType) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralBlue
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

private fun getStateTitle(state: ModelLoadingState): String = when (state) {
    is ModelLoadingState.Loading -> "Loading ${state.modelType.displayName}..."
    is ModelLoadingState.Missing -> "${state.modelType.displayName} Not Found"
    is ModelLoadingState.Error -> "${state.modelType.displayName} Error"
    is ModelLoadingState.Incompatible -> "${state.modelType.displayName} Incompatible"
    else -> "Model Status"
}

private fun getStateMessage(state: ModelLoadingState): String = when (state) {
    is ModelLoadingState.Loading -> state.statusMessage
    is ModelLoadingState.Missing -> {
        val pathHint = state.searchedPaths.lastOrNull()?.let {
            "\nSearched: ${it.substringAfterLast("/sdcard/")}"
        } ?: ""
        "The ${state.modelType.displayName.lowercase()} model is required for AVA to work.$pathHint"
    }
    is ModelLoadingState.Error -> state.message
    is ModelLoadingState.Incompatible -> {
        "${state.reason}\nRequired: ${state.requiredCapabilities.joinToString(", ")}"
    }
    else -> ""
}
