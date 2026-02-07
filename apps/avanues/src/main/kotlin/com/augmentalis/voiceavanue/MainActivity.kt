/**
 * MainActivity.kt - Main entry point for Avanues Consolidated App
 *
 * Routes to different modules based on which launcher icon was tapped:
 * - VoiceAvanue alias → voice dashboard (HomeScreen)
 * - WebAvanue alias → full browser (WebAvanue BrowserApp)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.avanueui.AvanueTheme
import com.augmentalis.voiceavanue.service.VoiceAvanueAccessibilityService
import com.augmentalis.voiceavanue.ui.browser.BrowserEntryViewModel
import com.augmentalis.voiceavanue.ui.developer.DeveloperConsoleScreen
import com.augmentalis.voiceavanue.ui.home.HomeScreen
import com.augmentalis.voiceavanue.ui.settings.SettingsScreen
import com.augmentalis.webavanue.BrowserApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Determine launch mode from activity-alias
        val launchMode = determineLaunchMode(intent)

        setContent {
            AvanueTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AvanuesApp(startMode = launchMode)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        if (!VoiceAvanueAccessibilityService.isEnabled(this)) {
            // Will prompt user in UI
        }
        if (!Settings.canDrawOverlays(this)) {
            // Will prompt user in UI
        }
    }

    /**
     * Determines which module to launch based on the activity-alias class name.
     * - ".VoiceAvanueAlias" or default → voice dashboard
     * - ".WebAvanueAlias" → browser
     */
    private fun determineLaunchMode(intent: Intent?): AvanueMode {
        val className = intent?.component?.className ?: return AvanueMode.VOICE
        return when {
            className.contains("WebAvanue") -> AvanueMode.BROWSER
            else -> AvanueMode.VOICE
        }
    }
}

/**
 * Modular navigation modes — each represents a launcher icon / app section.
 * Add new modules here as they become ready.
 */
enum class AvanueMode(val route: String, val label: String) {
    VOICE("voice_home", "VoiceAvanue"),
    BROWSER("browser", "WebAvanue"),
    SETTINGS("settings", "Settings"),
    DEVELOPER_CONSOLE("developer_console", "Developer Console")
    // Future: CURSOR("cursor", "VoiceCursor"), GAZE("gaze", "GazeControl")
}

@Composable
fun AvanuesApp(startMode: AvanueMode = AvanueMode.VOICE) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startMode.route
    ) {
        composable(AvanueMode.VOICE.route) {
            HomeScreen(
                onNavigateToBrowser = { navController.navigate(AvanueMode.BROWSER.route) },
                onNavigateToSettings = { navController.navigate(AvanueMode.SETTINGS.route) }
            )
        }

        composable(AvanueMode.BROWSER.route) {
            val browserViewModel: BrowserEntryViewModel = hiltViewModel()
            BrowserApp(repository = browserViewModel.repository)
        }

        composable(AvanueMode.SETTINGS.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDeveloperConsole = {
                    navController.navigate(AvanueMode.DEVELOPER_CONSOLE.route)
                }
            )
        }

        composable(AvanueMode.DEVELOPER_CONSOLE.route) {
            DeveloperConsoleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
