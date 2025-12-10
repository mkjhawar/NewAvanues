/**
 * OnboardingActivity.kt - VOS4 Setup Wizard with Voice Guidance
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.voiceos.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Onboarding Activity - Step-by-step setup with voice guidance
 */
class OnboardingActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle permission results
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                OnboardingScreen(
                    onRequestPermissions = { permissions ->
                        permissionLauncher.launch(permissions)
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onRequestPermissions: (Array<String>) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1) / 6f },
            modifier = Modifier.fillMaxWidth(),
        )
        
        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomeStep()
                1 -> PermissionsStep(onRequestPermissions = onRequestPermissions)
                2 -> AccessibilityStep(onOpenAccessibilitySettings = onOpenAccessibilitySettings)
                3 -> VoiceCalibrationStep()
                4 -> CommandTrainingStep()
                5 -> CompletionStep()
            }
        }
        
        // Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            Button(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage < 5) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            // Complete onboarding
                        }
                    }
                }
            ) {
                Text(if (pagerState.currentPage < 5) "Next" else "Complete")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (pagerState.currentPage < 5) Icons.AutoMirrored.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    OnboardingStepLayout(
        icon = Icons.Default.WavingHand,
        title = "Welcome to VoiceOS",
        description = "Your voice-controlled operating system that makes Android accessible through natural speech commands.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎯 Key Features:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Voice-controlled navigation")
                    Text("• Hands-free app control")
                    Text("• Accessibility support")
                    Text("• Multi-language commands")
                    Text("• Smart glasses integration")
                }
            }
        }
    )
}

@Composable
fun PermissionsStep(
    onRequestPermissions: (Array<String>) -> Unit
) {
    OnboardingStepLayout(
        icon = Icons.Default.Security,
        title = "Required Permissions",
        description = "VoiceOS needs specific permissions to provide voice control functionality.",
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PermissionCard(
                        icon = Icons.Default.Mic,
                        title = "Microphone Access",
                        description = "Required for voice command recognition",
                        onGrant = {
                            onRequestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    )
                }
                item {
                    PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = "Notification Access",
                        description = "Optional: Show voice recognition status",
                        onGrant = {
                            onRequestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun AccessibilityStep(
    onOpenAccessibilitySettings: () -> Unit
) {
    OnboardingStepLayout(
        icon = Icons.Default.Accessibility,
        title = "Accessibility Service",
        description = "Enable VoiceOS Accessibility Service to control your device with voice commands.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Why do we need this?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Navigate through apps")
                    Text("• Click buttons and links")
                    Text("• Scroll and interact with content")
                    Text("• Control system functions")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Accessibility Settings")
                    }
                }
            }
        }
    )
}

@Composable
fun VoiceCalibrationStep() {
    val scope = rememberCoroutineScope()
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationStep by remember { mutableIntStateOf(0) }
    
    OnboardingStepLayout(
        icon = Icons.Default.Tune,
        title = "Voice Calibration",
        description = "Let's calibrate VoiceOS to recognize your voice clearly.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (calibrationStep) {
                        0 -> {
                            Text(
                                "Say: \"Hello VoiceOS\"",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        1 -> {
                            Text(
                                "Say: \"Go back\"",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        2 -> {
                            Text(
                                "Say: \"Open settings\"",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                "✓ Calibration Complete!",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (calibrationStep < 3) {
                        if (isCalibrating) {
                            CircularProgressIndicator()
                        } else {
                            Button(
                                onClick = {
                                    isCalibrating = true
                                    // Launch in composable scope with proper lifecycle
                                    scope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        calibrationStep++
                                        isCalibrating = false
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Recording")
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CommandTrainingStep() {
    OnboardingStepLayout(
        icon = Icons.Default.School,
        title = "Command Training",
        description = "Learn essential voice commands to get started.",
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CommandCard("Navigation", listOf(
                        "\"Go back\" - Navigate back",
                        "\"Go home\" - Return to home screen",
                        "\"Open recent\" - Show recent apps"
                    ))
                }
                item {
                    CommandCard("System Control", listOf(
                        "\"Volume up\" - Increase volume",
                        "\"Volume down\" - Decrease volume",
                        "\"Open settings\" - System settings"
                    ))
                }
                item {
                    CommandCard("Interaction", listOf(
                        "\"Click [text]\" - Click on text/button",
                        "\"Scroll up\" - Scroll page up",
                        "\"Scroll down\" - Scroll page down"
                    ))
                }
            }
        }
    )
}

@Composable
fun CompletionStep() {
    OnboardingStepLayout(
        icon = Icons.Default.CheckCircle,
        title = "Setup Complete!",
        description = "VoiceOS is now ready to use. Start using voice commands to control your device.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎉 You're all set!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Try saying:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("\"Hello VoiceOS\"", style = MaterialTheme.typography.bodyLarge)
                    Text("\"Go back\"", style = MaterialTheme.typography.bodyLarge)
                    Text("\"Open settings\"", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    )
}

@Composable
fun OnboardingStepLayout(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onGrant) {
                Text("Grant")
            }
        }
    }
}

@Composable
fun CommandCard(
    category: String,
    commands: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                category,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            commands.forEach { command ->
                Text(
                    command,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}