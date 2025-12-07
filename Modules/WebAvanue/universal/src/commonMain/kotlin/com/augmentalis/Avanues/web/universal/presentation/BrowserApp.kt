package com.augmentalis.Avanues.web.universal.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.navigation.BrowserScreenNav
import com.augmentalis.Avanues.web.universal.presentation.navigation.ViewModelHolder
import com.augmentalis.Avanues.web.universal.presentation.ui.browser.WebViewPoolManager
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.AppTheme
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.ThemeType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * BrowserApp - Main composable for WebAvanue browser
 *
 * This is the root composable that sets up:
 * - AppTheme (with auto-detection)
 * - ViewModels
 * - Voyager Navigation
 * - WebXR state management (Android only)
 *
 * Usage in Activity:
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Initialize theme system
 *         initializeThemeSystem(this)
 *
 *         // Create repository
 *         val repository = BrowserRepositoryImpl(database)
 *
 *         // Create XR manager (Android only)
 *         val xrManager = XRManager(this, lifecycle)
 *
 *         setContent {
 *             val xrState by xrManager.xrState.collectAsState()
 *             BrowserApp(
 *                 repository = repository,
 *                 xrManager = xrManager,
 *                 xrState = xrState
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @param repository BrowserRepository instance
 * @param xrManager XRManager instance (Android only, nullable for other platforms)
 * @param xrState Current XR state (null for non-Android platforms)
 * @param modifier Modifier for customization
 */
@Composable
fun BrowserApp(
    repository: BrowserRepository,
    xrManager: Any? = null,  // XRManager on Android, null on other platforms
    xrState: Any? = null,    // XRManager.XRState on Android, null on other platforms
    modifier: Modifier = Modifier
) {
    // FIX Issues #2 & #3: Remember ViewModels across recompositions
    val viewModels = remember(repository) { ViewModelHolder.create(repository) }

    // FIX BUG: Observe settings for theme changes (was hardcoded to dark)
    val settings by viewModels.settingsViewModel.settings.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()

    // Cleanup WebViewPool when BrowserApp is disposed
    DisposableEffect(Unit) {
        onDispose {
            WebViewPoolManager.clearAllWebViews()
            println("BrowserApp: WebViewPool cleared on dispose")
        }
    }

    // FIX BUG: Determine dark theme based on settings (not hardcoded)
    val useDarkTheme = when (settings?.theme) {
        BrowserSettings.Theme.LIGHT -> false
        BrowserSettings.Theme.DARK -> true
        BrowserSettings.Theme.SYSTEM -> systemDarkTheme
        BrowserSettings.Theme.AUTO -> {
            // Time-based: dark from 7 PM to 7 AM
            val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
            hour < 7 || hour >= 19
        }
        null -> true  // Default to dark theme (Ocean Blue)
    }

    // Apply theme based on settings
    AppTheme(
        darkTheme = useDarkTheme,
        themeType = ThemeType.APP_BRANDING  // Force WebAvanue branding
    ) {
        // Voyager Navigator with slide transitions
        Navigator(
            screen = BrowserScreenNav(viewModels, xrManager, xrState)
        ) { navigator ->
            SlideTransition(navigator)
        }
    }
}
