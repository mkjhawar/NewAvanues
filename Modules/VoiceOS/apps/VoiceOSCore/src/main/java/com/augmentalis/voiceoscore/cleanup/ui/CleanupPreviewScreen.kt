/**
 * CleanupPreviewScreen.kt - Compose UI for cleanup preview with all states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.2: Full cleanup preview screen with statistics, safety indicators,
 * and execution states (Loading, Preview, Executing, Success, Error)
 */

package com.augmentalis.voiceoscore.cleanup.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Main cleanup preview screen with state-based UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupPreviewScreen(
    viewModel: CleanupPreviewViewModel,
    onNavigateBack: () -> Unit,
    onCleanupComplete: (deletedCount: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle cleanup completion
    LaunchedEffect(uiState) {
        if (uiState is CleanupPreviewUiState.Success) {
            val success = uiState as CleanupPreviewUiState.Success
            onCleanupComplete(success.deletedCount)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cleanup Preview") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = uiState !is CleanupPreviewUiState.Executing
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is CleanupPreviewUiState.Loading -> LoadingView()
                is CleanupPreviewUiState.Preview -> PreviewContent(
                    state = state,
                    onExecuteCleanup = { viewModel.executeCleanup() },
                    onCancel = onNavigateBack,
                    onRefresh = { viewModel.loadPreview(state.gracePeriodDays, state.keepUserApproved) }
                )
                is CleanupPreviewUiState.Executing -> ExecutingView(state)
                is CleanupPreviewUiState.Success -> SuccessView(state, onDone = onNavigateBack)
                is CleanupPreviewUiState.Error -> ErrorView(
                    state = state,
                    onRetry = { viewModel.retry() },
                    onCancel = onNavigateBack
                )
            }
        }
    }
}

/**
 * Loading state view
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading cleanup preview" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Analyzing commands...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Preview content with statistics and affected apps
 */
@Composable
private fun PreviewContent(
    state: CleanupPreviewUiState.Preview,
    onExecuteCleanup: () -> Unit,
    onCancel: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Statistics card
        StatisticsCard(
            statistics = state.statistics,
            safetyLevel = state.safetyLevel,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Affected apps header
        Text(
            text = "Affected Apps (${state.affectedApps.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Affected apps list
        AffectedAppsCard(
            affectedApps = state.affectedApps,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onExecuteCleanup,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (state.safetyLevel) {
                        SafetyLevel.SAFE -> MaterialTheme.colorScheme.primary
                        SafetyLevel.MODERATE -> Color(0xFFFFC107)
                        SafetyLevel.HIGH_RISK -> Color(0xFFFF5722)
                    }
                )
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Execute Cleanup")
            }
        }
    }
}

/**
 * Statistics card with safety indicator
 */
@Composable
private fun StatisticsCard(
    statistics: CleanupStatistics,
    safetyLevel: SafetyLevel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = "Cleanup statistics: ${statistics.commandsToDelete} commands to delete"
        },
        colors = CardDefaults.cardColors(
            containerColor = safetyLevel.getBackgroundColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Safety level badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = safetyLevel.icon,
                    contentDescription = null,
                    tint = safetyLevel.color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = safetyLevel.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = safetyLevel.color
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${statistics.deletionPercentage}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = safetyLevel.color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = safetyLevel.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "To Delete",
                    value = statistics.commandsToDelete.toString(),
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.error
                )
                StatItem(
                    label = "To Keep",
                    value = statistics.commandsToPreserve.toString(),
                    icon = Icons.Default.Check,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Space Saved",
                    value = statistics.getFormattedSize(),
                    icon = Icons.Default.Storage,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Affected apps scrollable list
 */
@Composable
private fun AffectedAppsCard(
    affectedApps: List<AffectedAppInfo>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (affectedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps affected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(affectedApps, key = { it.packageName }) { appInfo ->
                    AffectedAppItem(appInfo)
                }
            }
        }
    }
}

/**
 * Individual affected app item
 */
@Composable
private fun AffectedAppItem(appInfo: AffectedAppInfo) {
    ListItem(
        headlineContent = {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = "${appInfo.commandsToDelete} commands to delete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            AppIcon(drawable = appInfo.icon)
        },
        trailingContent = {
            Badge {
                Text(appInfo.commandsToDelete.toString())
            }
        },
        modifier = Modifier.semantics(mergeDescendants = true) {}
    )
}

/**
 * App icon composable with fallback
 */
@Composable
private fun AppIcon(drawable: Drawable?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (drawable != null) {
            Image(
                bitmap = drawable.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Executing state with progress
 */
@Composable
private fun ExecutingView(state: CleanupPreviewUiState.Executing) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress / 100f,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Executing cleanup, ${state.progress}% complete" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${state.progress}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.currentApp,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Do not close this screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Success state view
 */
@Composable
private fun SuccessView(
    state: CleanupPreviewUiState.Success,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Cleanup completed successfully" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Cleanup Complete",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ResultRow(
                        label = "Commands Deleted",
                        value = state.deletedCount.toString(),
                        icon = Icons.Default.Delete,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultRow(
                        label = "Commands Preserved",
                        value = state.preservedCount.toString(),
                        icon = Icons.Default.Check,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultRow(
                        label = "Duration",
                        value = state.getDurationSeconds(),
                        icon = Icons.Default.Timer,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

/**
 * Result row for success view
 */
@Composable
private fun ResultRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Error state view
 */
@Composable
private fun ErrorView(
    state: CleanupPreviewUiState.Error,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Error: ${state.message}" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Cleanup Failed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                if (state.canRetry) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
