/**
 * MainActivity.kt - Main entry point for VoiceOS application
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.augmentalis.voiceos.ui.screens.HomeScreen
import com.augmentalis.voiceos.ui.screens.SetupScreen
import com.augmentalis.voiceos.ui.screens.SettingsScreen
import com.augmentalis.voiceos.ui.theme.VoiceOSTheme
import com.augmentalis.voiceos.util.AccessibilityServiceHelper

/**
 * Main Activity - VoiceOS launcher entry point.
 *
 * Shows:
 * - SetupScreen if accessibility service is not enabled
 * - HomeScreen if service is enabled
 * - Settings accessible from top bar
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VoiceOSTheme {
                VoiceOSApp()
            }
        }
    }
}

/**
 * Navigation routes for VoiceOS app.
 */
object VoiceOSRoutes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceOSApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Track accessibility service state with lifecycle awareness
    var isServiceEnabled by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Recheck service status when app resumes (user may have enabled it)
                isServiceEnabled = AccessibilityServiceHelper.isVoiceOSServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial check
    isServiceEnabled = AccessibilityServiceHelper.isVoiceOSServiceEnabled(context)

    // Determine start destination based on service state
    val startDestination = if (isServiceEnabled) VoiceOSRoutes.HOME else VoiceOSRoutes.SETUP

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("VoiceOS") },
                colors = TopAppBarDefaults.topAppBarColors(),
                actions = {
                    IconButton(onClick = { navController.navigate(VoiceOSRoutes.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(VoiceOSRoutes.HOME) {
                HomeScreen(
                    onNavigateToSettings = { navController.navigate(VoiceOSRoutes.SETTINGS) },
                    onNavigateToSetup = { navController.navigate(VoiceOSRoutes.SETUP) }
                )
            }

            composable(VoiceOSRoutes.SETUP) {
                SetupScreen(
                    onSetupComplete = {
                        navController.navigate(VoiceOSRoutes.HOME) {
                            popUpTo(VoiceOSRoutes.SETUP) { inclusive = true }
                        }
                    }
                )
            }

            composable(VoiceOSRoutes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
