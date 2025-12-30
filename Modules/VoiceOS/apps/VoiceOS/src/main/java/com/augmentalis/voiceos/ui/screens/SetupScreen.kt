/**
 * SetupScreen.kt - Onboarding wizard for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceos.ui.theme.VoiceOSColors
import com.augmentalis.voiceos.util.AccessibilityServiceHelper
import com.augmentalis.voiceos.util.rememberAccessibilityServiceState
import com.augmentalis.voiceos.util.rememberMicrophonePermissionState

/**
 * Setup wizard to guide users through VoiceOS configuration.
 *
 * Steps:
 * 1. Welcome
 * 2. Enable Accessibility Service
 * 3. Grant Microphone Permission
 * 4. Complete
 */
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    val isServiceEnabled by rememberAccessibilityServiceState(context)
    val isMicPermissionGranted by rememberMicrophonePermissionState(context)

    // Auto-advance steps when conditions are met
    LaunchedEffect(isServiceEnabled, currentStep) {
        if (currentStep == 1 && isServiceEnabled) {
            currentStep = 2
        }
    }

    LaunchedEffect(isMicPermissionGranted, currentStep) {
        if (currentStep == 2 && isMicPermissionGranted) {
            currentStep = 3
        }
    }

    // Permission launcher - the hook auto-updates on resume, but we also advance on callback
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentStep = 3
        }
    }

    val totalSteps = 4
    val progress = (currentStep + 1f) / totalSteps

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Step ${currentStep + 1} of $totalSteps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step Content
        when (currentStep) {
            0 -> WelcomeStep(
                onNext = { currentStep = 1 }
            )
            1 -> AccessibilityStep(
                isEnabled = isServiceEnabled,
                onOpenSettings = {
                    AccessibilityServiceHelper.openAccessibilitySettings(context)
                },
                onNext = {
                    if (isServiceEnabled) currentStep = 2
                }
            )
            2 -> MicrophoneStep(
                isGranted = isMicPermissionGranted,
                onRequestPermission = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onNext = {
                    if (isMicPermissionGranted) currentStep = 3
                }
            )
            3 -> CompleteStep(
                onComplete = onSetupComplete
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    SetupStepCard(
        icon = Icons.Default.RecordVoiceOver,
        title = "Welcome to VoiceOS",
        description = "Control your Android device with voice commands. VoiceOS learns your apps and creates custom voice commands automatically.",
        buttonText = "Get Started",
        onButtonClick = onNext
    )
}

@Composable
private fun AccessibilityStep(
    isEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    SetupStepCard(
        icon = Icons.Default.Accessibility,
        title = "Enable Accessibility Service",
        description = "VoiceOS needs accessibility permission to read screen content and perform actions on your behalf.",
        buttonText = if (isEnabled) "Continue" else "Open Settings",
        onButtonClick = if (isEnabled) onNext else onOpenSettings,
        isComplete = isEnabled,
        secondaryButtonText = if (!isEnabled) "I've enabled it" else null,
        onSecondaryClick = onNext
    )
}

@Composable
private fun MicrophoneStep(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    SetupStepCard(
        icon = Icons.Default.Mic,
        title = "Microphone Permission",
        description = "VoiceOS needs microphone access to listen for your voice commands.",
        buttonText = if (isGranted) "Continue" else "Grant Permission",
        onButtonClick = if (isGranted) onNext else onRequestPermission,
        isComplete = isGranted
    )
}

@Composable
private fun CompleteStep(onComplete: () -> Unit) {
    SetupStepCard(
        icon = Icons.Default.Check,
        title = "You're All Set!",
        description = "VoiceOS is ready to use. Say 'Hey Ava' followed by a command to control your device.",
        buttonText = "Start Using VoiceOS",
        onButtonClick = onComplete,
        isComplete = true
    )
}

@Composable
private fun SetupStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    isComplete: Boolean = false,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                VoiceOSColors.StatusActive.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$title icon",
                modifier = Modifier.size(64.dp),
                tint = if (isComplete)
                    VoiceOSColors.StatusActive
                else
                    MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }

            if (secondaryButtonText != null && onSecondaryClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(secondaryButtonText)
                }
            }
        }
    }
}
