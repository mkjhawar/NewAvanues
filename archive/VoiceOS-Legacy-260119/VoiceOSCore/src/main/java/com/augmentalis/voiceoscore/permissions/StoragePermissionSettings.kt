/**
 * StoragePermissionSettings.kt - Composable UI for storage permission management
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-22
 *
 * Provides a settings UI component for viewing and managing storage permissions.
 * Can be integrated into the main settings screen.
 */
package com.augmentalis.voiceoscore.permissions

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Storage permission settings section
 * Shows current permission status and allows user to manage permissions
 */
@Composable
fun StoragePermissionSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }

    var hasPermission by remember { mutableStateOf(permissionManager.hasStoragePermission()) }
    var permissionStatus by remember { mutableStateOf(permissionManager.getPermissionStatusSummary()) }
    var storageLocation by remember { mutableStateOf(permissionManager.getStorageLocation()) }

    // Refresh permission status
    DisposableEffect(Unit) {
        val refreshStatus = {
            hasPermission = permissionManager.hasStoragePermission()
            permissionStatus = permissionManager.getPermissionStatusSummary()
            storageLocation = permissionManager.getStorageLocation()
        }

        onDispose {
            refreshStatus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
            Text(
                text = "Storage & Permissions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        HorizontalDivider(color = Color(0xFF333333))

        // Permission Status
        SettingsRow(
            label = "Permission Status",
            value = permissionStatus,
            valueColor = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )

        // Storage Location
        SettingsRow(
            label = "Storage Location",
            value = storageLocation,
            valueColor = Color(0xFFBBBBBB)
        )

        // Show warning if permission denied
        if (!hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x33FF9800)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Limited Functionality",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Without storage permission, models must be extracted from APK or downloaded. Grant permission to use pre-deployed models.",
                            fontSize = 12.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF333333))

        // Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!hasPermission && permissionManager.shouldRequestPermission()) {
                // Request Permission Button
                Button(
                    onClick = {
                        val intent = Intent(context, PermissionRequestActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Request Permission")
                }
            }

            // Open Settings Button (always show - allows manual permission grant)
            OutlinedButton(
                onClick = {
                    context.startActivity(permissionManager.createAppSettingsIntent())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open App Settings")
            }
        }
    }
}

/**
 * Simple settings row component
 */
@Composable
private fun SettingsRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
