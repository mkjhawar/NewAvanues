/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme

@Composable
fun SystemHealthBar(permissions: PermissionStatus) {
    val context = LocalContext.current

    // Microphone: runtime permission dialog (falls back to App Info if permanently denied)
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled by onResume re-check in DashboardViewModel */ }

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
    ) {
        // Summary header
        val grantedCount = listOf(
            permissions.microphoneGranted,
            permissions.accessibilityEnabled,
            permissions.overlayEnabled,
            permissions.notificationsEnabled,
            permissions.batteryOptimized
        ).count { it }
        val totalCount = 5

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = SpacingTokens.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SYSTEM PERMISSIONS",
                style = MaterialTheme.typography.labelMedium,
                color = if (permissions.allGranted) AvanueTheme.colors.success
                else AvanueTheme.colors.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$grantedCount/$totalCount",
                style = MaterialTheme.typography.labelSmall,
                color = if (permissions.allGranted) AvanueTheme.colors.success
                else AvanueTheme.colors.warning,
                fontWeight = FontWeight.Bold
            )
        }

        // Microphone — Direct (runtime dialog)
        if (permissions.microphoneGranted) {
            PermissionGrantedRow("Microphone")
        } else {
            PermissionActionCard(
                title = "Microphone",
                description = "Tap to grant — one-step dialog",
                badge = "Direct",
                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        }

        // Accessibility — Manual (opens system list, user must find service)
        if (permissions.accessibilityEnabled) {
            PermissionGrantedRow("Accessibility Service")
        } else {
            PermissionActionCard(
                title = "Accessibility Service",
                description = "Find \"VoiceOS\u00AE Avanues\" \u2192 toggle ON",
                badge = "Manual",
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        Toast.makeText(
                            context,
                            "Scroll to Downloaded services \u2192 tap \"VoiceOS\u00AE Avanues\" \u2192 toggle ON",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
        }

        // Overlay — Direct (opens app-specific overlay toggle)
        if (permissions.overlayEnabled) {
            PermissionGrantedRow("Display Over Apps")
        } else {
            PermissionActionCard(
                title = "Display Over Apps",
                description = "Tap to open — toggle the switch ON",
                badge = "Direct",
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
        }

        // Notifications — Direct (opens app notification settings)
        if (permissions.notificationsEnabled) {
            PermissionGrantedRow("Notifications")
        } else {
            PermissionActionCard(
                title = "Notifications",
                description = "Tap to open — enable notifications",
                badge = "Direct",
                onClick = {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
        }

        // Battery Optimization — Direct (one-tap system dialog)
        if (permissions.batteryOptimized) {
            PermissionGrantedRow("Battery Unrestricted")
        } else {
            @SuppressLint("BatteryLife")
            PermissionActionCard(
                title = "Battery Restricted",
                description = "Tap to allow unrestricted — one-step dialog",
                badge = "Direct",
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
        }
    }
}

/** Compact row for a granted permission — green check + name. */
@Composable
private fun PermissionGrantedRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Granted",
            tint = AvanueTheme.colors.success,
            modifier = Modifier.size(16.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = AvanueTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "OK",
            style = MaterialTheme.typography.labelSmall,
            color = AvanueTheme.colors.success,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Action card for a denied permission — tappable, with description and Direct/Manual badge. */
@Composable
private fun PermissionActionCard(
    title: String,
    description: String,
    badge: String,
    onClick: () -> Unit
) {
    AvanueCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SpacingTokens.md),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AvanueTheme.colors.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AvanueTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AvanueTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    val badgeColor = if (badge == "Direct") AvanueTheme.colors.success else AvanueTheme.colors.warning
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), shape = CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = AvanueTheme.colors.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open settings", tint = AvanueTheme.colors.textPrimary, modifier = Modifier.size(18.dp))
        }
    }
}
