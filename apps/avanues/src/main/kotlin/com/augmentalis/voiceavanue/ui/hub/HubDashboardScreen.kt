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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.augmentalis.voiceavanue.R
import com.augmentalis.avanueui.components.glass.GlassCard
import com.augmentalis.avanueui.components.glass.GlassSurface
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
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDeveloperSettings: () -> Unit = {}
) {
    var showVoiceActivePopup by remember { mutableStateOf(false) }

    // Auto-dismiss the popup after 2.5 seconds
    LaunchedEffect(showVoiceActivePopup) {
        if (showVoiceActivePopup) {
            kotlinx.coroutines.delay(2500L)
            showVoiceActivePopup = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AvanueTheme.colors.background)
                .statusBarsPadding()
        ) {
            val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

            if (isLandscape) {
                HubLandscape(
                    onVoiceAvanueClick = { showVoiceActivePopup = true },
                    onNavigateToBrowser = onNavigateToBrowser,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAbout = onNavigateToAbout,
                    onNavigateToDeveloperSettings = onNavigateToDeveloperSettings
                )
            } else {
                HubPortrait(
                    onVoiceAvanueClick = { showVoiceActivePopup = true },
                    onNavigateToBrowser = onNavigateToBrowser,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAbout = onNavigateToAbout,
                    onNavigateToDeveloperSettings = onNavigateToDeveloperSettings
                )
            }
        }

        // Fading popup overlay for VoiceAvanue
        AnimatedVisibility(
            visible = showVoiceActivePopup,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AvanueTheme.colors.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.md),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AvanueTheme.colors.success,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.voiceavanue_active_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = AvanueTheme.colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.voiceavanue_active_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────── PORTRAIT ────────────────────────────

@Composable
private fun HubPortrait(
    onVoiceAvanueClick: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDeveloperSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.md)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        HubHeader(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToDeveloperSettings = onNavigateToDeveloperSettings
        )

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
            onClick = onVoiceAvanueClick
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
            onClick = onNavigateToAbout
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
    onVoiceAvanueClick: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDeveloperSettings: () -> Unit
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
            HubHeader(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToDeveloperSettings = onNavigateToDeveloperSettings
            )

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
                onClick = onNavigateToAbout
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
                onClick = onVoiceAvanueClick
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
private fun HubHeader(
    onNavigateToSettings: () -> Unit,
    onNavigateToDeveloperSettings: () -> Unit = {}
) {
    // 4-tap easter egg: tap "Avanues" title 4 times within 2 seconds to reveal dev icon
    var tapCount by remember { mutableIntStateOf(0) }
    var firstTapTime by remember { mutableLongStateOf(0L) }
    var devModeActivated by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SpacingTokens.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                val now = System.currentTimeMillis()
                if (now - firstTapTime > 2000L) {
                    tapCount = 1
                    firstTapTime = now
                } else {
                    tapCount++
                }
                if (tapCount >= 4) {
                    devModeActivated = true
                    tapCount = 0
                }
            }
        ) {
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

        Row {
            if (devModeActivated) {
                IconButton(onClick = onNavigateToDeveloperSettings) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Developer Settings",
                        tint = AvanueTheme.colors.warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
