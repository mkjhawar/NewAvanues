package com.augmentalis.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.augmentalis.ava.core.domain.model.InstalledApp
import com.augmentalis.ava.core.domain.resolution.AppResolverService
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Bottom sheet for selecting the preferred app for a capability.
 *
 * Shown when AVA detects multiple apps that can handle a capability
 * (e.g., multiple email apps) and needs the user to choose.
 *
 * Features:
 * - Shows app icon + name for each available app
 * - "Recommended" badge for most popular app
 * - "Always ask me" option at bottom
 * - Remembers user choice by default
 *
 * Part of Intelligent Resolution System (Chapter 71).
 *
 * @param show Whether the bottom sheet is visible
 * @param capability The capability ID (e.g., "email")
 * @param capabilityDisplayName The display name (e.g., "Email")
 * @param apps List of available apps
 * @param recommendedIndex Index of the recommended app
 * @param appResolverService Service for getting app icons
 * @param onAppSelected Callback when user selects (app, rememberChoice)
 * @param onDismiss Callback when user dismisses without selecting
 *
 * Author: Manoj Jhawar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPreferenceBottomSheet(
    show: Boolean,
    capability: String,
    capabilityDisplayName: String,
    apps: List<InstalledApp>,
    recommendedIndex: Int,
    appResolverService: AppResolverService?,
    onAppSelected: (InstalledApp, Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = modifier
        ) {
            AppPreferenceContent(
                capabilityDisplayName = capabilityDisplayName,
                apps = apps,
                recommendedIndex = recommendedIndex,
                appResolverService = appResolverService,
                onAppSelected = onAppSelected,
                onAlwaysAsk = { onDismiss() }
            )
        }
    }
}

@Composable
private fun AppPreferenceContent(
    capabilityDisplayName: String,
    apps: List<InstalledApp>,
    recommendedIndex: Int,
    appResolverService: AppResolverService?,
    onAppSelected: (InstalledApp, Boolean) -> Unit,
    onAlwaysAsk: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Text(
            text = "Choose $capabilityDisplayName App",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics {
                contentDescription = "Choose your preferred $capabilityDisplayName app"
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AVA found multiple apps. Which would you like to use?",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // App list
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(apps) { index, app ->
                AppOptionCard(
                    app = app,
                    isRecommended = index == recommendedIndex,
                    appResolverService = appResolverService,
                    onClick = { onAppSelected(app, true) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Always ask option
        TextButton(
            onClick = onAlwaysAsk,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Always ask me",
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.primary
            )
        }
    }
}

@Composable
private fun AppOptionCard(
    app: InstalledApp,
    isRecommended: Boolean,
    appResolverService: AppResolverService?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isRecommended) {
                    "${app.appName}, recommended"
                } else {
                    app.appName
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                AvanueTheme.colors.primaryContainer.copy(alpha = 0.5f)
            } else {
                AvanueTheme.colors.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            val icon = appResolverService?.getAppIcon(app.packageName)
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = "${app.appName} icon",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                // Placeholder icon
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AvanueTheme.colors.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = app.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App name and recommendation badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isRecommended) FontWeight.SemiBold else FontWeight.Normal
                )

                if (isRecommended) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AvanueTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.primary
                        )
                    }
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select ${app.appName}",
                tint = AvanueTheme.colors.textSecondary
            )
        }
    }
}

/**
 * Preview for the bottom sheet content.
 */
@Composable
private fun AppPreferenceContentPreview() {
    MaterialTheme {
        Surface {
            AppPreferenceContent(
                capabilityDisplayName = "Email",
                apps = listOf(
                    InstalledApp("com.google.android.gm", "Gmail"),
                    InstalledApp("com.microsoft.office.outlook", "Outlook"),
                    InstalledApp("com.yahoo.mobile.client.android.mail", "Yahoo Mail")
                ),
                recommendedIndex = 0,
                appResolverService = null,
                onAppSelected = { _, _ -> },
                onAlwaysAsk = {}
            )
        }
    }
}
