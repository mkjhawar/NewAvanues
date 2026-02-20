// Author: Manoj Jhawar
// Purpose: Comprehensive UI for configuring feedback settings (haptic, audio, visual)

package com.augmentalis.devicemanager.dashboardui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.devicemanager.accessibility.FeedbackManager
import com.augmentalis.devicemanager.accessibility.FeedbackManager.*
import kotlinx.coroutines.launch

/**
 * Main Feedback Settings UI Screen
 * Provides comprehensive configuration for haptic, audio, and visual feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSettingsScreen(
    feedbackManager: FeedbackManager = FeedbackManager(LocalContext.current)
) {
    val coroutineScope = rememberCoroutineScope()
    val feedbackState by feedbackManager.feedbackState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Feedback Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { 
                        coroutineScope.launch {
                            // Test feedback on refresh
                            feedbackManager.provideFeedback(
                                FeedbackType.INFO,
                                includeHaptic = true,
                                includeAudio = true
                            )
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Test Feedback")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row for different feedback categories
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Haptic") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audio") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Visual") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Presets") }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> FeedbackOverviewTab(feedbackManager, feedbackState)
                1 -> HapticSettingsTab(feedbackManager, feedbackState.haptic)
                2 -> AudioSettingsTab(feedbackManager, feedbackState.audio)
                3 -> VisualSettingsTab(feedbackManager, feedbackState.visual)
                4 -> FeedbackPresetsTab(feedbackManager, feedbackState)
            }
        }
    }
}

/**
 * Overview Tab - Shows general feedback status and quick settings
 */
@Composable
fun FeedbackOverviewTab(
    feedbackManager: FeedbackManager,
    feedbackState: FeedbackState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FeedbackCard(
                title = "System Feedback",
                icon = Icons.Default.Settings,
                enabled = feedbackState.systemFeedbackEnabled
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enable System Feedback",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = feedbackState.systemFeedbackEnabled,
                        onCheckedChange = { feedbackManager.setSystemFeedbackEnabled(it) }
                    )
                }
            }
        }
        
        item {
            FeedbackStatusGrid(feedbackState)
        }
        
        item {
            QuickActionsCard(feedbackManager)
        }
        
        item {
            FeedbackTestCard(feedbackManager)
        }
    }
}

/**
 * Haptic Settings Tab - Configure haptic feedback
 */
@Composable
fun HapticSettingsTab(
    feedbackManager: FeedbackManager,
    hapticState: HapticFeedbackState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FeedbackCard(
                title = "Haptic Feedback",
                icon = Icons.Default.Vibration,
                enabled = hapticState.enabled && hapticState.isAvailable
            ) {
                // Enable/Disable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Enable Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            if (hapticState.isAvailable) "Hardware available" else "Hardware not available",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hapticState.isAvailable)
                                AvanueTheme.colors.primary else
                                AvanueTheme.colors.error
                        )
                    }
                    Switch(
                        checked = hapticState.enabled,
                        onCheckedChange = { feedbackManager.setHapticEnabled(it) },
                        enabled = hapticState.isAvailable
                    )
                }
                
                if (hapticState.enabled && hapticState.isAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Intensity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Intensity")
                            Text("${(hapticState.intensity * 100).toInt()}%")
                        }
                        Slider(
                            value = hapticState.intensity,
                            onValueChange = { feedbackManager.setHapticIntensity(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Duration Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration")
                            Text("${hapticState.duration}ms")
                        }
                        Slider(
                            value = hapticState.duration.toFloat(),
                            onValueChange = { feedbackManager.setHapticDuration(it.toLong()) },
                            valueRange = 50f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Pattern Selection
                    Text("Vibration Pattern", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HapticPattern.values().forEach { pattern ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { feedbackManager.setHapticPattern(pattern) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = hapticState.pattern == pattern,
                                onClick = { feedbackManager.setHapticPattern(pattern) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pattern.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Test Button
                    Button(
                        onClick = { feedbackManager.vibrateWithPattern(hapticState.pattern) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !hapticState.isVibrating
                    ) {
                        if (hapticState.isVibrating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Test Vibration")
                    }
                }
            }
        }
    }
}

/**
 * Audio Settings Tab - Configure audio feedback
 */
@Composable
fun AudioSettingsTab(
    feedbackManager: FeedbackManager,
    audioState: AudioFeedbackState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FeedbackCard(
                title = "Audio Feedback",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                enabled = audioState.enabled && audioState.isAvailable
            ) {
                // Enable/Disable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Enable Audio Feedback",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            if (audioState.isAvailable) "Audio system available" else "Audio system not available",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (audioState.isAvailable)
                                AvanueTheme.colors.primary else
                                AvanueTheme.colors.error
                        )
                    }
                    Switch(
                        checked = audioState.enabled,
                        onCheckedChange = { feedbackManager.setAudioEnabled(it) },
                        enabled = audioState.isAvailable
                    )
                }
                
                if (audioState.enabled && audioState.isAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Volume Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume")
                            Text("${(audioState.volume * 100).toInt()}%")
                        }
                        Slider(
                            value = audioState.volume,
                            onValueChange = { feedbackManager.setAudioVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Duration Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration")
                            Text("${audioState.duration}ms")
                        }
                        Slider(
                            value = audioState.duration.toFloat(),
                            onValueChange = { feedbackManager.setAudioDuration(it.toLong()) },
                            valueRange = 100f..2000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tone Type Selection
                    Text("Tone Type", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AudioToneType.values().forEach { toneType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { feedbackManager.setAudioToneType(toneType) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = audioState.toneType == toneType,
                                onClick = { feedbackManager.setAudioToneType(toneType) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(toneType.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Test Button
                    Button(
                        onClick = { feedbackManager.playTone(audioState.toneType) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !audioState.isPlaying
                    ) {
                        if (audioState.isPlaying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Test Audio")
                    }
                }
            }
        }
    }
}

/**
 * Visual Settings Tab - Configure visual feedback
 */
@Composable
fun VisualSettingsTab(
    feedbackManager: FeedbackManager,
    visualState: VisualFeedbackState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FeedbackCard(
                title = "Visual Feedback",
                icon = Icons.Default.Visibility,
                enabled = visualState.enabled
            ) {
                // Enable/Disable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enable Visual Feedback",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = visualState.enabled,
                        onCheckedChange = { feedbackManager.setVisualEnabled(it) }
                    )
                }
                
                if (visualState.enabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Screen Flash Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Screen Flash")
                        Switch(
                            checked = visualState.screenFlashEnabled,
                            onCheckedChange = { feedbackManager.setScreenFlashEnabled(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Animation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Animations")
                        Switch(
                            checked = visualState.animationEnabled,
                            onCheckedChange = { feedbackManager.setAnimationEnabled(it) }
                        )
                    }
                    
                    if (visualState.screenFlashEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Flash Intensity Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Flash Intensity")
                                Text("${(visualState.flashIntensity * 100).toInt()}%")
                            }
                            Slider(
                                value = visualState.flashIntensity,
                                onValueChange = { feedbackManager.setFlashIntensity(it) },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    if (visualState.animationEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Animation Duration Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Animation Duration")
                                Text("${visualState.animationDuration}ms")
                            }
                            Slider(
                                value = visualState.animationDuration.toFloat(),
                                onValueChange = { feedbackManager.setAnimationDuration(it.toLong()) },
                                valueRange = 100f..1000f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Presets Tab - Pre-configured feedback settings
 */
@Composable
fun FeedbackPresetsTab(
    feedbackManager: FeedbackManager,
    feedbackState: FeedbackState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Feedback Presets",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Choose from pre-configured settings or test different feedback types.",
                style = MaterialTheme.typography.bodyMedium,
                color = AvanueTheme.colors.textSecondary
            )
        }
        
        // Feedback Type Test Cards
        FeedbackType.values().forEach { type ->
            item {
                FeedbackTestTypeCard(feedbackManager, type, feedbackState)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            FeedbackPresetButtons(feedbackManager)
        }
    }
}

/**
 * Status Grid showing current feedback system status
 */
@Composable
fun FeedbackStatusGrid(feedbackState: FeedbackState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Feedback Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip(
                    "Haptic", 
                    feedbackState.haptic.enabled && feedbackState.haptic.isAvailable,
                    Icons.Default.Vibration
                )
                StatusChip(
                    "Audio", 
                    feedbackState.audio.enabled && feedbackState.audio.isAvailable,
                    Icons.AutoMirrored.Filled.VolumeUp
                )
                StatusChip(
                    "Visual", 
                    feedbackState.visual.enabled,
                    Icons.Default.Visibility
                )
            }
        }
    }
}

/**
 * Quick Actions Card for common feedback operations
 */
@Composable
fun QuickActionsCard(feedbackManager: FeedbackManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { 
                        feedbackManager.setHapticEnabled(true)
                        feedbackManager.setAudioEnabled(true)
                        feedbackManager.setVisualEnabled(true)
                    }
                ) {
                    Text("Enable All")
                }
                
                OutlinedButton(
                    onClick = { 
                        feedbackManager.setHapticEnabled(false)
                        feedbackManager.setAudioEnabled(false)
                        feedbackManager.setVisualEnabled(false)
                    }
                ) {
                    Text("Disable All")
                }
            }
        }
    }
}

/**
 * Test Card for trying different feedback types
 */
@Composable
fun FeedbackTestCard(feedbackManager: FeedbackManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Test Feedback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { 
                    feedbackManager.provideFeedback(
                        FeedbackType.SUCCESS,
                        includeHaptic = true,
                        includeAudio = true
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test All Feedback")
            }
        }
    }
}

/**
 * Individual feedback type test card
 */
@Composable
fun FeedbackTestTypeCard(
    feedbackManager: FeedbackManager,
    type: FeedbackType,
    feedbackState: FeedbackState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Test ${type.name.lowercase()} feedback",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textSecondary
                )
            }
            
            Button(
                onClick = { 
                    feedbackManager.provideFeedback(
                        type,
                        includeHaptic = feedbackState.haptic.enabled,
                        includeAudio = feedbackState.audio.enabled
                    )
                }
            ) {
                Text("Test")
            }
        }
    }
}

/**
 * Preset configuration buttons
 */
@Composable
fun FeedbackPresetButtons(feedbackManager: FeedbackManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Preset Configurations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Gentle Preset
            OutlinedButton(
                onClick = {
                    feedbackManager.setHapticIntensity(0.3f)
                    feedbackManager.setHapticDuration(100L)
                    feedbackManager.setAudioVolume(0.4f)
                    feedbackManager.setAudioDuration(150L)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gentle")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Standard Preset
            Button(
                onClick = {
                    feedbackManager.setHapticIntensity(0.5f)
                    feedbackManager.setHapticDuration(200L)
                    feedbackManager.setAudioVolume(0.7f)
                    feedbackManager.setAudioDuration(200L)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Standard")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Strong Preset
            OutlinedButton(
                onClick = {
                    feedbackManager.setHapticIntensity(0.8f)
                    feedbackManager.setHapticDuration(300L)
                    feedbackManager.setAudioVolume(0.9f)
                    feedbackManager.setAudioDuration(250L)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Strong")
            }
        }
    }
}

/**
 * Reusable Components
 */
@Composable
fun FeedbackCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled)
                        AvanueTheme.colors.primary
                    else
                        AvanueTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!enabled) {
                    Surface(
                        shape = CircleShape,
                        color = AvanueTheme.colors.error.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Disabled",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(2.dp),
                            tint = AvanueTheme.colors.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatusChip(name: String, enabled: Boolean, icon: ImageVector) {
    Surface(
        modifier = Modifier
            .height(56.dp)
            .width(80.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled)
            AvanueTheme.colors.surfaceVariant
        else
            AvanueTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                modifier = Modifier.size(20.dp),
                tint = if (enabled)
                    AvanueTheme.colors.primary
                else
                    AvanueTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}