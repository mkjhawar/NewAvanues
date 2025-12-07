package com.augmentalis.Avanues.web.universal.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.Avanues.web.universal.presentation.navigation.BrowserScreenNav
import com.augmentalis.Avanues.web.universal.presentation.navigation.ViewModelHolder
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.AppTheme

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
    // Create ViewModels (in real app, use dependency injection)
    val viewModels = ViewModelHolder.create(repository)

    // Apply theme
    AppTheme {
        // Voyager Navigator with slide transitions
        Navigator(
            screen = BrowserScreenNav(viewModels, xrManager, xrState)
        ) { navigator ->
            SlideTransition(navigator)
        }
    }
}
