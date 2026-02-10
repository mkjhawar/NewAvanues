package com.augmentalis.webavanue

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.navigator.Navigator
import com.augmentalis.webavanue.WebViewPoolManager
import cafe.adriel.voyager.transitions.SlideTransition
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.BrowserSettings
import com.augmentalis.webavanue.BrowserScreenNav
import com.augmentalis.webavanue.ViewModelHolder
import com.augmentalis.webavanue.LocalViewModelHolder
import com.augmentalis.webavanue.LocalXRManager
import com.augmentalis.webavanue.LocalXRState
import com.augmentalis.webavanue.AppTheme
import com.augmentalis.webavanue.ThemeType
import com.augmentalis.webavanue.SecureStorageProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

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
    downloadQueue: DownloadQueue? = null,  // AndroidDownloadQueue on Android, null on other platforms
    xrManager: Any? = null,  // XRManager on Android, null on other platforms
    xrState: Any? = null,    // XRManager.XRState on Android, null on other platforms
    onExitBrowser: (() -> Unit)? = null,  // Exit browser and return to parent navigation
    modifier: Modifier = Modifier
) {
    // FIX: Use remember to ensure ViewModels are created only once and persist across recompositions
    // ViewModels should only be cleared when BrowserApp is disposed (app termination)
    val viewModels = remember { ViewModelHolder.create(repository, secureStorage, downloadQueue) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val didCleanup = remember { AtomicBoolean(false) }

    // FIX BUG #3: Observe settings for theme changes
    val settings by viewModels.settingsViewModel.settings.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()

    fun runExitCleanup(reason: String) {
        if (!didCleanup.compareAndSet(false, true)) return
        println("BrowserApp: runExitCleanup reason ($reason)")
        // FIX Issue #3: Check if "Clear History on Exit" is enabled
        // CRITICAL: Must be done BEFORE onCleared() to ensure the coroutine scope is still active
        val currentSettings = viewModels.settingsViewModel.settings.value
        if (currentSettings?.clearHistoryOnExit == true) {
            println("BrowserApp: Clearing history on exit (setting enabled)")
            viewModels.historyViewModel.clearHistory()
        }

        // FIX: Check if "Clear Cookies on Exit" is enabled
        if (currentSettings?.clearCookiesOnExit == true) {
            println("BrowserApp: Clearing cookies on exit (setting enabled)")
            // Cookies are cleared via WebView - handled in WebViewPoolManager
            WebViewPoolManager.clearCookiesOnExit()
        }

        // Clear WebView pool
        WebViewPoolManager.clearAllWebViews()
        println("BrowserApp: WebViewPool cleared on dispose")

        // FIX C3: Clear ViewModels - cancels coroutine scopes to prevent memory leaks
        // CRITICAL: This MUST be called LAST, after all coroutine operations complete
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

    // FIX C3: Cleanup ALL resources when BrowserApp is disposed (Activity.onDestroy)
    // This is the ONLY place where ViewModels should be cleared - not in individual screens!
    // CRITICAL: clearHistory() MUST be called BEFORE onCleared() because it uses viewModelScope
    // 1) Handle "exit app" semantics via lifecycle (Home/Recents => ON_STOP)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    // Treat background as exit if your setting says “on exit”
                    runExitCleanup("ON_STOP")
                }
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    runExitCleanup("ON_DESTROY")
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 2) Keep your onDispose as a final fallback (won’t hurt, but not sufficient alone)
    DisposableEffect(Unit) {
        onDispose { runExitCleanup("COMPOSE_DISPOSE") }
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
        // Provide non-Serializable objects via CompositionLocals so Voyager
        // Screen data classes remain serialization-safe (fixes NotSerializableException)
        CompositionLocalProvider(
            LocalViewModelHolder provides viewModels,
            LocalXRManager provides xrManager,
            LocalXRState provides xrState,
            LocalExitBrowser provides onExitBrowser
        ) {
            Navigator(
                screen = BrowserScreenNav
            ) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
