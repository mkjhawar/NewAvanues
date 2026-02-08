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
import com.augmentalis.avamagic.ui.foundation.GlassSurface
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme

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
            .background(AvanueTheme.colors.background)
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
            .padding(horizontal = SpacingTokens.md)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        HubHeader(onNavigateToSettings = onNavigateToSettings)

        Spacer(modifier = Modifier.height(SpacingTokens.lg))

        // Section: Apps
        Text(
            text = "APPS",
            style = MaterialTheme.typography.labelLarge,
            color = AvanueTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = SpacingTokens.sm)
        )

        // App cards stacked
        AppCard(
            title = "VoiceAvanue",
            subtitle = "Voice control & accessibility platform",
            icon = Icons.Default.Mic,
            accentColor = AvanueTheme.colors.success,
            onClick = onNavigateToVoice
        )

        Spacer(modifier = Modifier.height(SpacingTokens.md))

        AppCard(
            title = "WebAvanue",
            subtitle = "Voice-enabled web browser",
            icon = Icons.Default.Language,
            accentColor = AvanueTheme.colors.info,
            onClick = onNavigateToBrowser
        )

        Spacer(modifier = Modifier.height(SpacingTokens.xl))

        // Section: Ecosystem
        Text(
            text = "ECOSYSTEM",
            style = MaterialTheme.typography.labelLarge,
            color = AvanueTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = SpacingTokens.sm)
        )

        EcosystemItem(
            title = "Settings",
            subtitle = "Permissions, voice control, browser, system",
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(SpacingTokens.sm))

        EcosystemItem(
            title = "About Avanues",
            subtitle = "Version, licenses, and credits",
            icon = Icons.Default.Info,
            onClick = onNavigateToSettings // Opens settings where About section lives
        )

        Spacer(modifier = Modifier.height(SpacingTokens.xl))

        // Footer: Branding
        Text(
            text = "Avanues Ecosystem",
            style = MaterialTheme.typography.bodySmall,
            color = AvanueTheme.colors.textDisabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingTokens.lg)
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
            .padding(SpacingTokens.md),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md)
    ) {
        // Left: Header + Ecosystem
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
        ) {
            HubHeader(onNavigateToSettings = onNavigateToSettings)

            Spacer(modifier = Modifier.height(SpacingTokens.sm))

            Text(
                text = "ECOSYSTEM",
                style = MaterialTheme.typography.labelLarge,
                color = AvanueTheme.colors.textSecondary
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
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)
        ) {
            Text(
                text = "APPS",
                style = MaterialTheme.typography.labelLarge,
                color = AvanueTheme.colors.textSecondary
            )

            AppCard(
                title = "VoiceAvanue",
                subtitle = "Voice control & accessibility platform",
                icon = Icons.Default.Mic,
                accentColor = AvanueTheme.colors.success,
                onClick = onNavigateToVoice
            )

            AppCard(
                title = "WebAvanue",
                subtitle = "Voice-enabled web browser",
                icon = Icons.Default.Language,
                accentColor = AvanueTheme.colors.info,
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
            .padding(top = SpacingTokens.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Avanues",
                style = MaterialTheme.typography.headlineMedium,
                color = AvanueTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your accessibility ecosystem",
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.textSecondary
            )
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = AvanueTheme.colors.textSecondary,
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
                .padding(SpacingTokens.lg),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
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
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = AvanueTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textSecondary
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = AvanueTheme.colors.textSecondary,
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
                    horizontal = SpacingTokens.md,
                    vertical = SpacingTokens.sm
                ),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AvanueTheme.colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AvanueTheme.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = AvanueTheme.colors.textDisabled,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
