package com.augmentalis.voiceos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.augmentalis.voiceos.service.OverlayStateManager
import com.augmentalis.voiceos.service.VoiceOSAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Settings activity for VoiceOS accessibility service.
 * Launched from System Settings > Accessibility > VoiceOS > Settings gear icon.
 *
 * This keeps the accessibility service invisible to users while
 * providing access to configuration options.
 */
class AccessibilitySettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AccessibilitySettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibilitySettingsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // State from accessibility service
    val isConnected by VoiceOSAccessibilityService.isConnected.collectAsState()
    val numbersOverlayMode by VoiceOSAccessibilityService.numbersOverlayMode.collectAsState()
    val instructionBarMode by VoiceOSAccessibilityService.instructionBarMode.collectAsState()
    val badgeTheme by VoiceOSAccessibilityService.badgeTheme.collectAsState()
    val isContinuousMonitoring by VoiceOSAccessibilityService.isContinuousMonitoring.collectAsState()

    // Developer mode unlock
    var devModeTapCount by remember { mutableStateOf(0) }
    var devModeUnlocked by remember { mutableStateOf(false) }

    // Developer settings state
    var debugLogging by remember { mutableStateOf(true) }
    var showAvidsOverlay by remember { mutableStateOf(false) }
    var autoMinimize by remember { mutableStateOf(true) }

    // Rescan feedback
    var rescanMessage by remember { mutableStateOf<String?>(null) }
    var showRescanConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoiceOS Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121218)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF10B981) else Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isConnected) "Service Active" else "Service Inactive",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isConnected) "Monitoring apps" else "Enable in Accessibility Settings",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Monitoring toggle
                    if (isConnected) {
                        Switch(
                            checked = isContinuousMonitoring,
                            onCheckedChange = { VoiceOSAccessibilityService.setContinuousMonitoring(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }

            // User Settings Section
            Text(
                text = "User Settings",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Numbers Overlay Mode
                    SettingsRow(
                        title = "Numbers Overlay",
                        subtitle = "Show numbered badges on list items",
                        onClick = { VoiceOSAccessibilityService.cycleNumbersOverlayMode() }
                    ) {
                        ModeChip(
                            mode = numbersOverlayMode.name,
                            color = when (numbersOverlayMode) {
                                OverlayStateManager.NumbersOverlayMode.ON -> Color(0xFF10B981)
                                OverlayStateManager.NumbersOverlayMode.OFF -> Color(0xFF6B7280)
                                OverlayStateManager.NumbersOverlayMode.AUTO -> Color(0xFF3B82F6)
                            }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Instruction Bar Mode
                    SettingsRow(
                        title = "Instruction Bar",
                        subtitle = "Show voice command hints",
                        onClick = { VoiceOSAccessibilityService.cycleInstructionBarMode() }
                    ) {
                        ModeChip(
                            mode = instructionBarMode.name,
                            color = when (instructionBarMode) {
                                OverlayStateManager.InstructionBarMode.ON -> Color(0xFF10B981)
                                OverlayStateManager.InstructionBarMode.OFF -> Color(0xFF6B7280)
                                OverlayStateManager.InstructionBarMode.AUTO -> Color(0xFF3B82F6)
                            }
                        )
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Badge Theme
                    SettingsRow(
                        title = "Badge Color",
                        subtitle = "Customize number badge appearance",
                        onClick = { VoiceOSAccessibilityService.cycleBadgeTheme() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(badgeTheme.backgroundColor), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = badgeTheme.name,
                                color = Color(badgeTheme.backgroundColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Developer Mode Unlock
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable {
                    devModeTapCount++
                    if (devModeTapCount >= 3 && !devModeUnlocked) {
                        devModeUnlocked = true
                        devModeTapCount = 0
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (devModeUnlocked) "Developer Mode Enabled" else "Version 1.0.0",
                        color = if (devModeUnlocked) Color(0xFF10B981) else Color.Gray,
                        fontSize = 12.sp
                    )
                    if (!devModeUnlocked && devModeTapCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${3 - devModeTapCount} more taps)",
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Developer Settings (only shown when unlocked)
            if (devModeUnlocked) {
                Text(
                    text = "Developer Settings",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Debug Logging
                        SettingsToggle(
                            title = "Debug Logging",
                            checked = debugLogging,
                            onCheckedChange = { debugLogging = it }
                        )

                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        // Show AVIDs
                        SettingsToggle(
                            title = "Show AVIDs",
                            checked = showAvidsOverlay,
                            onCheckedChange = { showAvidsOverlay = it }
                        )

                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        // Auto-minimize
                        SettingsToggle(
                            title = "Auto-minimize",
                            checked = autoMinimize,
                            onCheckedChange = { autoMinimize = it }
                        )
                    }
                }

                // Rescan Actions
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Rescan Current App
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val count = VoiceOSAccessibilityService.rescanCurrentApp()
                                        rescanMessage = "Cleared $count screens for current app"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rescan Current App", color = Color(0xFF3B82F6))
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        // Rescan Everything
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRescanConfirmation = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rescan Everything", color = Color(0xFFEF4444))
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Rescan message
                rescanMessage?.let { message ->
                    LaunchedEffect(message) {
                        delay(2000)
                        rescanMessage = null
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = message,
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Rescan Everything Confirmation Dialog
    if (showRescanConfirmation) {
        AlertDialog(
            onDismissRequest = { showRescanConfirmation = false },
            title = { Text("Rescan Everything?") },
            text = { Text("This will clear ALL cached screens and force a rescan. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescanConfirmation = false
                        coroutineScope.launch {
                            val count = VoiceOSAccessibilityService.rescanEverything()
                            rescanMessage = "Cleared ALL $count cached screens"
                        }
                    }
                ) {
                    Text("Rescan All", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E1E2E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        trailing()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF4B5563)
            )
        )
    }
}

@Composable
private fun ModeChip(mode: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = mode,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
