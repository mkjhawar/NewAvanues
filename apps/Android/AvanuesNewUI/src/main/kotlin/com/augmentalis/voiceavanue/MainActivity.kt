/**
 * MainActivity.kt - Main entry point for Avanues Consolidated App
 *
 * Routes to different modules based on which launcher icon was tapped:
 * - Avanues (default) → Cockpit Dashboard
 * - VoiceAvanueAlias → voice dashboard (HomeScreen)
 * - WebAvanueAlias → full browser (WebAvanue BrowserApp)
 * - PDFAvanueAlias → Cockpit with PDFAvanue module
 * - ImageAvanueAlias → Cockpit with ImageAvanue module
 * - VideoAvanueAlias → Cockpit with VideoAvanue module
 * - NoteAvanueAlias → Cockpit with NoteAvanue module
 * - PhotoAvanueAlias → Cockpit with PhotoAvanue module
 * - CastAvanueAlias → Cockpit with CastAvanue (RemoteCast) module
 * - DrawAvanueAlias → Cockpit with DrawAvanue (Annotation) module
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.AvanueThemeProvider
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.display.DisplayProfileResolver
import com.augmentalis.devicemanager.DeviceCapabilityFactory
import com.augmentalis.devicemanager.KmpDeviceType
import com.augmentalis.foundation.settings.models.AvanuesSettings
import com.augmentalis.foundation.settings.models.DeveloperSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.data.DeveloperPreferencesRepository
import com.augmentalis.voiceavanue.service.VoiceAvanueAccessibilityService
import com.augmentalis.voiceavanue.ui.browser.BrowserEntryViewModel
import com.augmentalis.voiceavanue.ui.cockpit.CockpitEntryViewModel
import com.augmentalis.voiceavanue.ui.developer.DeveloperConsoleScreen
import com.augmentalis.voiceavanue.ui.developer.DeveloperSettingsScreen
import com.augmentalis.voiceavanue.ui.home.CommandsScreen
import com.augmentalis.voiceavanue.ui.home.HomeScreen
import com.augmentalis.voiceavanue.ui.about.AboutScreen
import com.augmentalis.voiceavanue.ui.settings.UnifiedSettingsScreen
import com.augmentalis.voiceavanue.ui.sync.VosSyncScreen
import com.augmentalis.cockpit.ui.CockpitScreen
import com.augmentalis.webavanue.BrowserApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: AvanuesSettingsRepository

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Determine launch mode from activity-alias
        val launchMode = determineLaunchMode(intent)

        // Detect display profile from device hardware
        val displayProfile = detectDisplayProfile()

        setContent {
            val settings by settingsRepository.settings.collectAsState(
                initial = AvanuesSettings()
            )
            val palette = AvanueColorPalette.fromString(settings.themePalette)
            val style = MaterialMode.fromString(settings.themeStyle)
            val appearance = AppearanceMode.fromString(settings.themeAppearance)
            val isDark = when (appearance) {
                AppearanceMode.Auto -> isSystemInDarkTheme()
                AppearanceMode.Dark -> true
                AppearanceMode.Light -> false
            }

            AvanueThemeProvider(
                colors = palette.colors(isDark),
                glass = palette.glass(isDark),
                water = palette.water(isDark),
                displayProfile = displayProfile,
                materialMode = style,
                isDark = isDark
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AvanueTheme.colors.background
                ) {
                    AvanuesApp(
                        startMode = launchMode,
                        settings = settings,
                        onNavControllerReady = { this@MainActivity.navController = it }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("navigate_to")?.let { route ->
            when (route) {
                "developer_console" -> navController?.navigate(AvanueMode.DEVELOPER_CONSOLE.route)
                else -> { /* Unknown route, ignore */ }
            }
        }
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
     * Detects the appropriate DisplayProfile from device hardware.
     * Uses DeviceManager capabilities + DisplayProfileResolver.
     */
    private fun detectDisplayProfile(): DisplayProfile {
        return try {
            val provider = DeviceCapabilityFactory.create()
            val display = provider.getDisplayCapabilities()
            val deviceInfo = provider.getKmpDeviceInfo()
            val isSmartGlass = deviceInfo.deviceType == KmpDeviceType.SMART_GLASS

            DisplayProfileResolver.resolve(
                widthPx = display.widthPixels,
                heightPx = display.heightPixels,
                densityDpi = display.densityDpi,
                isSmartGlass = isSmartGlass
            )
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "DisplayProfile detection failed, defaulting to PHONE", e)
            DisplayProfile.PHONE
        }
    }

    /**
     * Determines which module to launch based on the activity-alias class name.
     * Module-specific aliases (PDF, Image, Video, Note, Photo, Cast, Draw) are
     * checked first to avoid false matches with broader patterns like "VoiceAvanue".
     * Falls back to COCKPIT for the default AvanuesNewUI launcher icon.
     * AvanuesNewUI defaults to Cockpit (Lens shell) instead of HUB.
     */
    private fun determineLaunchMode(intent: Intent?): AvanueMode {
        val className = intent?.component?.className ?: return AvanueMode.COCKPIT
        return when {
            className.contains("PDFAvanue") -> AvanueMode.PDF
            className.contains("ImageAvanue") -> AvanueMode.IMAGE
            className.contains("VideoAvanue") -> AvanueMode.VIDEO
            className.contains("NoteAvanue") -> AvanueMode.NOTE
            className.contains("PhotoAvanue") -> AvanueMode.PHOTO
            className.contains("CastAvanue") -> AvanueMode.CAST
            className.contains("DrawAvanue") -> AvanueMode.DRAW
            className.contains("WebAvanue") -> AvanueMode.BROWSER
            className.contains("VoiceAvanue") -> AvanueMode.VOICE
            else -> AvanueMode.COCKPIT
        }
    }
}

/**
 * Modular navigation modes — each represents a launcher icon / app section.
 * Add new modules here as they become ready.
 */
enum class AvanueMode(val route: String, val label: String) {
    HUB("hub", "Avanues"),
    VOICE("voice_home", "VoiceAvanue"),
    BROWSER("browser", "WebAvanue"),
    COMMANDS("commands", "Voice Commands"),
    SETTINGS("settings", "Settings"),
    ABOUT("about", "About Avanues"),
    DEVELOPER_CONSOLE("developer_console", "Developer Console"),
    DEVELOPER_SETTINGS("developer_settings", "Developer Settings"),
    VOS_SYNC("vos_sync", "VOS Sync"),
    COCKPIT("cockpit", "Cockpit"),
    PDF("cockpit/pdf", "PDFAvanue"),
    IMAGE("cockpit/image", "ImageAvanue"),
    VIDEO("cockpit/video", "VideoAvanue"),
    NOTE("cockpit/note", "NoteAvanue"),
    PHOTO("cockpit/photo", "PhotoAvanue"),
    CAST("cockpit/cast", "CastAvanue"),
    DRAW("cockpit/draw", "DrawAvanue")
    // Future: CURSOR("cursor", "VoiceCursor"), GAZE("gaze", "GazeControl")
}

@Composable
fun AvanuesApp(
    startMode: AvanueMode = AvanueMode.HUB,
    settings: AvanuesSettings = AvanuesSettings(),
    onNavControllerReady: ((NavHostController) -> Unit)? = null
) {
    val navController = rememberNavController()

    // Collect developer settings for shell mode override
    val context = androidx.compose.ui.platform.LocalContext.current
    val devRepo = remember(context) { DeveloperPreferencesRepository(context) }
    val devSettings by devRepo.settings.collectAsState(initial = DeveloperSettings())

    // Expose navController to activity for onNewIntent handling
    LaunchedEffect(navController) {
        onNavControllerReady?.invoke(navController)
    }

    NavHost(
        navController = navController,
        startDestination = startMode.route
    ) {
        composable(AvanueMode.HUB.route) {
            val cockpitEntry: CockpitEntryViewModel = hiltViewModel()
            CockpitScreen(
                viewModel = cockpitEntry.cockpitViewModel,
                onNavigateBack = { /* no-op, this is home */ },
                onNavigateToSettings = { navController.navigate(AvanueMode.SETTINGS.route) },
                onSpecialModuleLaunch = { moduleId ->
                    when (moduleId) {
                        "voicecursor" -> navController.navigate(AvanueMode.VOICE.route)
                        else -> android.util.Log.w("MainActivity",
                            "Unknown special module: $moduleId — no navigation target")
                    }
                },
                userShellMode = settings.shellMode,
                devForceShellMode = devSettings.forceShellMode
            )
        }

        composable(AvanueMode.VOICE.route) {
            HomeScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AvanueMode.HUB.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToBrowser = { navController.navigate(AvanueMode.BROWSER.route) },
                onNavigateToSettings = { navController.navigate(AvanueMode.SETTINGS.route) },
                onNavigateToCommands = { navController.navigate(AvanueMode.COMMANDS.route) }
            )
        }

        composable(AvanueMode.COMMANDS.route) {
            CommandsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AvanueMode.BROWSER.route) {
            val browserViewModel: BrowserEntryViewModel = hiltViewModel()
            BrowserApp(
                repository = browserViewModel.repository,
                onExitBrowser = { navController.popBackStack() }
            )
        }

        composable(AvanueMode.SETTINGS.route) {
            UnifiedSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDeveloperConsole = {
                    navController.navigate(AvanueMode.DEVELOPER_CONSOLE.route)
                },
                onNavigateToVosSync = {
                    navController.navigate(AvanueMode.VOS_SYNC.route)
                }
            )
        }

        composable(AvanueMode.ABOUT.route) {
            AboutScreen(
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

        composable(AvanueMode.DEVELOPER_SETTINGS.route) {
            DeveloperSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AvanueMode.VOS_SYNC.route) {
            VosSyncScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AvanueMode.COCKPIT.route) {
            val cockpitEntry: CockpitEntryViewModel = hiltViewModel()
            CockpitScreen(
                viewModel = cockpitEntry.cockpitViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(AvanueMode.SETTINGS.route) },
                userShellMode = settings.shellMode,
                devForceShellMode = devSettings.forceShellMode
            )
        }

        // ── Module-Direct Launcher Routes ──
        // Each launches Cockpit with a specific module auto-opened via launchModule()
        moduleDirectRoute(AvanueMode.PDF, "pdfavanue", navController, settings.shellMode, devSettings.forceShellMode)
        moduleDirectRoute(AvanueMode.IMAGE, "imageavanue", navController, settings.shellMode, devSettings.forceShellMode)
        moduleDirectRoute(AvanueMode.VIDEO, "videoavanue", navController, settings.shellMode, devSettings.forceShellMode)
        moduleDirectRoute(AvanueMode.NOTE, "noteavanue", navController, settings.shellMode, devSettings.forceShellMode)
        moduleDirectRoute(AvanueMode.PHOTO, "photoavanue", navController, settings.shellMode, devSettings.forceShellMode)
        moduleDirectRoute(AvanueMode.CAST, "remotecast", navController, settings.shellMode, devSettings.forceShellMode)
        moduleDirectRoute(AvanueMode.DRAW, "annotationavanue", navController, settings.shellMode, devSettings.forceShellMode)
    }
}

/**
 * Registers a module-direct launcher route — launches Cockpit with a specific
 * module auto-opened via [CockpitViewModel.launchModule].
 * Used for launcher icon aliases (PDF, Image, Video, Note, Photo, Cast, Draw).
 */
private fun NavGraphBuilder.moduleDirectRoute(
    mode: AvanueMode,
    moduleId: String,
    navController: NavHostController,
    userShellMode: String = "",
    devForceShellMode: String = ""
) {
    composable(mode.route) {
        val cockpitEntry: CockpitEntryViewModel = hiltViewModel()
        LaunchedEffect(moduleId) {
            if (cockpitEntry.cockpitViewModel.activeSession.value == null) {
                cockpitEntry.cockpitViewModel.launchModule(moduleId)
            }
        }
        CockpitScreen(
            viewModel = cockpitEntry.cockpitViewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(AvanueMode.SETTINGS.route) },
            userShellMode = userShellMode,
            devForceShellMode = devForceShellMode
        )
    }
}
