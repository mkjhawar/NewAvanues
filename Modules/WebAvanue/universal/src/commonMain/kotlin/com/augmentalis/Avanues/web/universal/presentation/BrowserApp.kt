package com.augmentalis.Avanues.web.universal.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.augmentalis.Avanues.web.universal.presentation.ui.browser.WebViewPoolManager
import cafe.adriel.voyager.transitions.SlideTransition
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.Avanues.web.universal.presentation.navigation.BrowserScreenNav
import com.augmentalis.Avanues.web.universal.presentation.navigation.ViewModelHolder
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.AppTheme
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.ThemeType
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SecureStorageProvider
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
 * @param secureStorage SecureStorage instance for encrypted credentials (Android only, nullable for other platforms)
 * @param xrManager XRManager instance (Android only, nullable for other platforms)
 * @param xrState Current XR state (null for non-Android platforms)
 * @param modifier Modifier for customization
 */
@Composable
fun BrowserApp(
    repository: BrowserRepository,
    secureStorage: SecureStorageProvider? = null,  // SecureStorage on Android, null on other platforms
    xrManager: Any? = null,  // XRManager on Android, null on other platforms
    xrState: Any? = null,    // XRManager.XRState on Android, null on other platforms
    modifier: Modifier = Modifier
) {
    // FIX: Use remember to ensure ViewModels are created only once and persist across recompositions
    // ViewModels should only be cleared when BrowserApp is disposed (app termination)
    val viewModels = remember { ViewModelHolder.create(repository, secureStorage) }

    // FIX BUG #3: Observe settings for theme changes
    val settings by viewModels.settingsViewModel.settings.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()

    // FIX: Cleanup ALL resources when BrowserApp is disposed (Activity.onDestroy)
    // This is the ONLY place where ViewModels should be cleared - not in individual screens!
    DisposableEffect(Unit) {
        onDispose {
            // FIX Issue #3: Check if "Clear History on Exit" is enabled
            // Must be done BEFORE onCleared() to ensure the coroutine scope is still active
            val currentSettings = viewModels.settingsViewModel.settings.value
            if (currentSettings?.clearHistoryOnExit == true) {
                println("BrowserApp: Clearing history on exit (setting enabled)")
                viewModels.historyViewModel.clearHistory()
            }

            // FIX: Check if "Clear Cookies on Exit" is enabled
            if (currentSettings?.clearCookiesOnExit == true) {
                println("BrowserApp: Clearing cookies on exit (setting enabled)")
                // Cookies are cleared via WebView - handled in WebViewPoolManager
            }

            // Clear WebView pool
            WebViewPoolManager.clearAllWebViews()
            println("BrowserApp: WebViewPool cleared on dispose")

            // Clear ViewModels - cancels coroutine scopes to prevent memory leaks
            viewModels.tabViewModel.onCleared()
            viewModels.settingsViewModel.onCleared()
            viewModels.historyViewModel.onCleared()
            viewModels.favoriteViewModel.onCleared()
            viewModels.securityViewModel.onCleared()
            viewModels.downloadViewModel.onCleared()
            println("BrowserApp: All ViewModels cleared on dispose")

            // Cleanup repository resources
            repository.cleanup()
            println("BrowserApp: Repository cleaned up on dispose")
        }
    }

    // FIX BUG #3: Determine dark theme based on settings
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
