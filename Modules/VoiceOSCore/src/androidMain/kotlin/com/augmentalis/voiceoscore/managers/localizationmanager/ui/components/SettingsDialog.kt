/**
 * SettingsDialog.kt - User settings dialog with debounce timing controls
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-09-06
 */
package com.augmentalis.voiceoscore.managers.localizationmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.augmentalis.voiceoscore.managers.localizationmanager.data.DebounceDuration
import com.augmentalis.voiceoscore.managers.localizationmanager.ui.LocalizationColors
import com.augmentalis.voiceoscore.managers.localizationmanager.ui.LocalizationGlassConfigs
import com.augmentalis.datamanager.ui.glassMorphism

/**
 * Settings dialog with user preferences
 */
@Composable
fun SettingsDialog(
    currentDebounceDuration: Long,
    statisticsAutoShow: Boolean,
    languageAnimationEnabled: Boolean,
    onDebounceDurationChange: (Long) -> Unit,
    onStatisticsAutoShowChange: (Boolean) -> Unit,
    onLanguageAnimationChange: (Boolean) -> Unit,
    onResetPreferences: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .glassMorphism(LocalizationGlassConfigs.Primary),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Message Timing Section
                SettingsSection(
                    title = "Message Timing",
                    icon = Icons.Default.Timer
                ) {
                    Text(
                        text = "How long should messages stay visible?",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    DebounceDurationSelector(
                        currentDuration = currentDebounceDuration,
                        onDurationChange = onDebounceDurationChange
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // UI Preferences Section
                SettingsSection(
                    title = "Interface",
                    icon = Icons.Default.Palette
                ) {
                    // Statistics Auto-show
                    SettingToggle(
                        title = "Auto-show Statistics Details",
                        description = "Automatically open detailed view when clicking statistics",
                        checked = statisticsAutoShow,
                        onCheckedChange = onStatisticsAutoShowChange
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Language Animation
                    SettingToggle(
                        title = "Language Change Animations",
                        description = "Show smooth animations when switching languages",
                        checked = languageAnimationEnabled,
                        onCheckedChange = onLanguageAnimationChange
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Reset Section
                SettingsSection(
                    title = "Reset",
                    icon = Icons.Default.RestartAlt
                ) {
                    Text(
                        text = "Reset all settings to their default values",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedButton(
                        onClick = onResetPreferences,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.9f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Settings")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalizationColors.StatusActive
                        )
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Debounce duration selector with preset options
 */
@Composable
fun DebounceDurationSelector(
    currentDuration: Long,
    onDurationChange: (Long) -> Unit
) {
    Column {
        DebounceDuration.values().forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = currentDuration == option.milliseconds,
                        onClick = { onDurationChange(option.milliseconds) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentDuration == option.milliseconds,
                    onClick = { onDurationChange(option.milliseconds) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = LocalizationColors.StatusActive,
                        unselectedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = option.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    
                    if (option.milliseconds > 0) {
                        Text(
                            text = "${option.milliseconds / 1000.0}s duration",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "Messages stay until manually dismissed",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings section with title and icon
 */
@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalizationColors.StatusActive,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        
        content()
    }
}

/**
 * Toggle setting with title and description
 */
@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LocalizationColors.StatusActive,
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
            )
        )
    }
}