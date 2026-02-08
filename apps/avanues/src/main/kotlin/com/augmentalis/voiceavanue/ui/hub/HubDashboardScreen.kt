/**
 * HubDashboardScreen.kt - Master dashboard for the Avanues ecosystem
 *
 * The central hub showing all available apps/modules as large cards,
 * with access to ecosystem-wide settings and help.
 * Launched from the "Avanues" launcher icon.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.avamagic.ui.foundation.GlassCard
import com.augmentalis.avamagic.ui.foundation.GlassLevel
import com.augmentalis.avamagic.ui.foundation.GlassSurface
import com.augmentalis.avamagic.ui.foundation.OceanDesignTokens
import com.augmentalis.avamagic.ui.foundation.OceanTheme

/**
 * Master hub dashboard for the Avanues ecosystem.
 * Shows app cards for each module, plus ecosystem settings and help.
 */
@Composable
fun HubDashboardScreen(
    onNavigateToVoice: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanTheme.background)
    ) {
        val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

        if (isLandscape) {
            HubLandscape(
                onNavigateToVoice = onNavigateToVoice,
                onNavigateToBrowser = onNavigateToBrowser,
                onNavigateToSettings = onNavigateToSettings
            )
        } else {
            HubPortrait(
                onNavigateToVoice = onNavigateToVoice,
                onNavigateToBrowser = onNavigateToBrowser,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

// ──────────────────────────── PORTRAIT ────────────────────────────

@Composable
private fun HubPortrait(
    onNavigateToVoice: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OceanDesignTokens.Spacing.md)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        HubHeader(onNavigateToSettings = onNavigateToSettings)

        Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.lg))

        // Section: Apps
        Text(
            text = "APPS",
            style = MaterialTheme.typography.labelLarge,
            color = OceanDesignTokens.Text.secondary,
            modifier = Modifier.padding(bottom = OceanDesignTokens.Spacing.sm)
        )

        // App cards stacked
        AppCard(
            title = "VoiceAvanue",
            subtitle = "Voice control & accessibility platform",
            icon = Icons.Default.Mic,
            accentColor = OceanDesignTokens.State.success,
            onClick = onNavigateToVoice
        )

        Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.md))

        AppCard(
            title = "WebAvanue",
            subtitle = "Voice-enabled web browser",
            icon = Icons.Default.Language,
            accentColor = OceanDesignTokens.State.info,
            onClick = onNavigateToBrowser
        )

        Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.xl))

        // Section: Ecosystem
        Text(
            text = "ECOSYSTEM",
            style = MaterialTheme.typography.labelLarge,
            color = OceanDesignTokens.Text.secondary,
            modifier = Modifier.padding(bottom = OceanDesignTokens.Spacing.sm)
        )

        EcosystemItem(
            title = "Settings",
            subtitle = "Permissions, voice control, browser, system",
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.sm))

        EcosystemItem(
            title = "About Avanues",
            subtitle = "Version, licenses, and credits",
            icon = Icons.Default.Info,
            onClick = onNavigateToSettings // Opens settings where About section lives
        )

        Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.xl))

        // Footer: Branding
        Text(
            text = "Avanues Ecosystem",
            style = MaterialTheme.typography.bodySmall,
            color = OceanDesignTokens.Text.disabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OceanDesignTokens.Spacing.lg)
        )
    }
}

// ──────────────────────────── LANDSCAPE ────────────────────────────

@Composable
private fun HubLandscape(
    onNavigateToVoice: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(OceanDesignTokens.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.md)
    ) {
        // Left: Header + Ecosystem
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.md)
        ) {
            HubHeader(onNavigateToSettings = onNavigateToSettings)

            Spacer(modifier = Modifier.height(OceanDesignTokens.Spacing.sm))

            Text(
                text = "ECOSYSTEM",
                style = MaterialTheme.typography.labelLarge,
                color = OceanDesignTokens.Text.secondary
            )

            EcosystemItem(
                title = "Settings",
                subtitle = "Permissions, voice, browser, system",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )

            EcosystemItem(
                title = "About",
                subtitle = "Version and credits",
                icon = Icons.Default.Info,
                onClick = onNavigateToSettings
            )
        }

        // Right: App cards
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.md)
        ) {
            Text(
                text = "APPS",
                style = MaterialTheme.typography.labelLarge,
                color = OceanDesignTokens.Text.secondary
            )

            AppCard(
                title = "VoiceAvanue",
                subtitle = "Voice control & accessibility platform",
                icon = Icons.Default.Mic,
                accentColor = OceanDesignTokens.State.success,
                onClick = onNavigateToVoice
            )

            AppCard(
                title = "WebAvanue",
                subtitle = "Voice-enabled web browser",
                icon = Icons.Default.Language,
                accentColor = OceanDesignTokens.State.info,
                onClick = onNavigateToBrowser
            )
        }
    }
}

// ──────────────────────────── SHARED COMPONENTS ────────────────────────────

@Composable
private fun HubHeader(onNavigateToSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OceanDesignTokens.Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Avanues",
                style = MaterialTheme.typography.headlineMedium,
                color = OceanDesignTokens.Text.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your accessibility ecosystem",
                style = MaterialTheme.typography.bodyMedium,
                color = OceanDesignTokens.Text.secondary
            )
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = OceanDesignTokens.Text.secondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AppCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    GlassCard(
        onClick = onClick,
        glassLevel = GlassLevel.MEDIUM,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OceanDesignTokens.Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            GlassSurface(
                glassLevel = GlassLevel.LIGHT,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.xs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = OceanDesignTokens.Text.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanDesignTokens.Text.secondary
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = OceanDesignTokens.Text.secondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EcosystemItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    GlassCard(
        onClick = onClick,
        glassLevel = GlassLevel.LIGHT,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = OceanDesignTokens.Spacing.md,
                    vertical = OceanDesignTokens.Spacing.sm
                ),
            horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OceanDesignTokens.Text.secondary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OceanDesignTokens.Text.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanDesignTokens.Text.secondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = OceanDesignTokens.Text.disabled,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
