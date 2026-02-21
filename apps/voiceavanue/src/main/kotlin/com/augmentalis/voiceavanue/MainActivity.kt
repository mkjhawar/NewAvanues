/**
 * MainActivity.kt - Main entry point for AVA Unified
 *
 * Hosts the Compose navigation and manages module lifecycle.
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
import androidx.compose.material3.Surface
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.augmentalis.voiceavanue.service.VoiceAvanueAccessibilityService
import com.augmentalis.voiceavanue.ui.home.HomeScreen
import com.augmentalis.voiceavanue.ui.browser.BrowserScreen
import com.augmentalis.voiceavanue.ui.settings.SettingsScreen
import com.augmentalis.voiceavanue.ui.theme.VoiceAvanueTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            VoiceAvanueTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AvanueTheme.colors.background
                ) {
                    VoiceAvanueApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        // Check accessibility service
        if (!VoiceAvanueAccessibilityService.isEnabled(this)) {
            // Will prompt user in UI
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            // Will prompt user in UI
        }
    }
}

/**
 * Navigation routes
 */
object Routes {
    const val HOME = "home"
    const val BROWSER = "browser"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
}

@Composable
fun VoiceAvanueApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToBrowser = { navController.navigate(Routes.BROWSER) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.BROWSER) {
            BrowserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
