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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

/**
 * Onboarding Activity - Step-by-step setup with voice guidance
 */
class OnboardingActivity : ComponentActivity() {

    private var permissionRefreshTrigger by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Trigger recomposition by incrementing the refresh trigger
        permissionRefreshTrigger++
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        overlayPermissionLauncher.launch(intent)
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check overlay permission after returning from settings

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        context = this@OnboardingActivity,
                        permissionRefreshTrigger = permissionRefreshTrigger,
                        onRequestPermissions = { permissions ->
                            permissionLauncher.launch(permissions)
                        },
                        requestOverlayPermission ={
                            requestOverlayPermission()
                        },
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onSetupComplete = {
                            this@OnboardingActivity.finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    context: android.content.Context,
    permissionRefreshTrigger: Int,
    onRequestPermissions: (Array<String>) -> Unit,
    requestOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    // Handle Android back button press
    BackHandler {
        activity?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoiceOS Setup") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                1 -> PermissionsStep(
                    context = context,
                    permissionRefreshTrigger = permissionRefreshTrigger,
                    requestOverlayPermission = requestOverlayPermission,
                    onRequestPermissions = onRequestPermissions
                )
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
                            onSetupComplete()
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
}

@Composable
fun WelcomeStep() {
    val scrollState = rememberScrollState()
    OnboardingStepLayout(
        icon = Icons.Default.WavingHand,
        title = "Welcome to VoiceOS",
        description = "Your voice-controlled operating system that makes Android accessible through natural speech commands.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
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
    context: android.content.Context,
    permissionRefreshTrigger: Int,
    requestOverlayPermission: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track permission states
    var microphoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var notificationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var overlayGranted by remember {
        mutableStateOf(
            Settings.canDrawOverlays(context)
        )
    }

    // Function to refresh permission status
    fun refreshPermissions() {
        microphoneGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        notificationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        overlayGranted = Settings.canDrawOverlays(context)
    }

    // Refresh on permission launcher callback
    LaunchedEffect(permissionRefreshTrigger) {
        refreshPermissions()
    }

    // Refresh when activity resumes (user returns from permission dialog)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
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
                        isGranted = microphoneGranted,
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
                        isGranted = notificationGranted,
                        onGrant = {
                            onRequestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    )
                }

                item {
                    PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = "Draw Overlay",
                        description = "Permission to draw over app",
                        isGranted = overlayGranted,
                        onGrant = requestOverlayPermission
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
    val scrollState = rememberScrollState()
    OnboardingStepLayout(
        icon = Icons.Default.Accessibility,
        title = "Accessibility Service",
        description = "Enable VoiceOS Accessibility Service to control your device with voice commands.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
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
    val scrollState = rememberScrollState()
    OnboardingStepLayout(
        icon = Icons.Default.Tune,
        title = "Voice Calibration",
        description = "Let's calibrate VoiceOS to recognize your voice clearly.",
        content = {
            Card(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
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
    val scrollState = rememberScrollState()
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
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
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
    isGranted: Boolean,
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
            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Granted",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Button(onClick = onGrant) {
                    Text("Grant")
                }
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