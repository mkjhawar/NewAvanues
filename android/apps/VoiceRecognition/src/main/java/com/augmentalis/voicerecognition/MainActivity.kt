/**
 * MainActivity.kt - Main activity for Voice Recognition
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 * 
 * Purpose: Main entry point for Voice Recognition test app
 * Handles permissions and launches speech recognition UI with VoiceCursor-style theming
 */
package com.augmentalis.voicerecognition

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.voicerecognition.ui.SpeechRecognitionScreen
import com.augmentalis.voicerecognition.viewmodel.SpeechViewModel
import com.google.accompanist.permissions.*
import androidx.compose.material3.MaterialTheme

/**
 * Main activity for Voice Recognition System
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                VoiceRecognitionApp()
            }
        }
    }
}

/**
 * Main app composable with permission handling
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecognitionApp() {
    val speechViewModel: SpeechViewModel = viewModel()
    
    // Audio permission state
    val audioPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )
    
    LaunchedEffect(audioPermissionState.status) {
        when (audioPermissionState.status) {
            is PermissionStatus.Granted -> {
                // Permission granted, can proceed
            }
            is PermissionStatus.Denied -> {
                // Request permission
                audioPermissionState.launchPermissionRequest()
            }
        }
    }
    
    SpeechRecognitionScreen(
        viewModel = speechViewModel,
        hasAudioPermission = audioPermissionState.status is PermissionStatus.Granted,
        onRequestPermission = {
            audioPermissionState.launchPermissionRequest()
        }
    )
}