package com.augmentalis.webavanue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.BookmarkListScreen
import com.augmentalis.webavanue.BrowserScreen
import com.augmentalis.webavanue.DownloadListScreen
import com.augmentalis.webavanue.HistoryScreen
import com.augmentalis.webavanue.SettingsScreen
import com.augmentalis.webavanue.*

/**
 * CompositionLocals for non-Serializable objects that must NOT be embedded
 * in Voyager Screen data classes. Voyager serializes Screen objects when
 * Android saves activity state; any non-Serializable property causes
 * NotSerializableException crashes.
 *
 * These are provided at the BrowserApp level via CompositionLocalProvider.
 */
val LocalViewModelHolder = staticCompositionLocalOf<ViewModelHolder> {
    error("No ViewModelHolder provided. Wrap your Navigator in CompositionLocalProvider(LocalViewModelHolder provides viewModels)")
}

val LocalXRManager = staticCompositionLocalOf<Any?> { null }
val LocalXRState = staticCompositionLocalOf<Any?> { null }
val LocalExitBrowser = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * ViewModelHolder - Holds all ViewModels for the browser app
 * Provided via LocalViewModelHolder CompositionLocal (NOT via Screen constructors)
 */
data class ViewModelHolder(
    val repository: BrowserRepository,
    val tabViewModel: TabViewModel,
    val favoriteViewModel: FavoriteViewModel,
    val downloadViewModel: DownloadViewModel,
    val historyViewModel: HistoryViewModel,
    val settingsViewModel: SettingsViewModel,
    val securityViewModel: SecurityViewModel // PHASE 3
) {
    companion object {
        fun create(
            repository: BrowserRepository,
            secureStorage: SecureStorageProvider? = null,
            downloadQueue: DownloadQueue? = null
        ): ViewModelHolder {
            val securityViewModel = SecurityViewModel(repository, secureStorage)

            return ViewModelHolder(
                repository = repository,
                tabViewModel = TabViewModel(repository),
                favoriteViewModel = FavoriteViewModel(repository),
                downloadViewModel = DownloadViewModel(repository, downloadQueue),
                historyViewModel = HistoryViewModel(repository),
                settingsViewModel = SettingsViewModel(repository),
                securityViewModel = securityViewModel // PHASE 3
            )
        }
    }
}

/**
 * BrowserScreenNav - Main browser screen with tab bar and WebView
 */
data object BrowserScreenNav : Screen {

    @Composable
    override fun Content() {
        val viewModels = LocalViewModelHolder.current
        val xrManager = LocalXRManager.current
        val xrState = LocalXRState.current
        val exitBrowser = LocalExitBrowser.current
        val navigator = LocalNavigator.currentOrThrow

        BrowserScreen(
            tabViewModel = viewModels.tabViewModel,
            settingsViewModel = viewModels.settingsViewModel,
            historyViewModel = viewModels.historyViewModel,
            favoriteViewModel = viewModels.favoriteViewModel,
            securityViewModel = viewModels.securityViewModel, // PHASE 3
            downloadViewModel = viewModels.downloadViewModel, // FIX Issue #5: Pass downloadViewModel for download tracking
            xrManager = xrManager,
            xrState = xrState,
            onNavigateToBookmarks = {
                navigator.push(BookmarksScreenNav())
            },
            onNavigateToDownloads = {
                navigator.push(DownloadsScreenNav)
            },
            onNavigateToHistory = {
                navigator.push(HistoryScreenNav)
            },
            onNavigateToSettings = {
                navigator.push(SettingsScreenNav)
            },
            onNavigateToXRSettings = {
                navigator.push(XRSettingsScreenNav)
            },
            onExitBrowser = exitBrowser
        )
    }
}

/**
 * BookmarksScreenNav - Bookmark/Favorite management screen
 */
data class BookmarksScreenNav(
    val folderId: String? = null
) : Screen {

    @Composable
    override fun Content() {
        val viewModels = LocalViewModelHolder.current
        val navigator = LocalNavigator.currentOrThrow

        // Filter by folder if specified
        if (folderId != null) {
            viewModels.favoriteViewModel.filterByFolder(folderId)
        }

        BookmarkListScreen(
            viewModel = viewModels.favoriteViewModel,
            onNavigateBack = {
                if (folderId != null) {
                    viewModels.favoriteViewModel.filterByFolder(null)
                }
                navigator.pop()
            },
            onBookmarkClick = { url ->
                viewModels.tabViewModel.navigateToUrl(url)
                navigator.popUntilRoot()
            }
        )
    }
}

/**
 * DownloadsScreenNav - Download management screen
 */
data object DownloadsScreenNav : Screen {

    @Composable
    override fun Content() {
        val viewModels = LocalViewModelHolder.current
        val navigator = LocalNavigator.currentOrThrow

        DownloadListScreen(
            viewModel = viewModels.downloadViewModel,
            onNavigateBack = {
                navigator.pop()
            },
            onDownloadClick = { download ->
                // TODO: Open downloaded file
            }
        )
    }
}

/**
 * HistoryScreenNav - Browsing history screen
 */
data object HistoryScreenNav : Screen {

    @Composable
    override fun Content() {
        val viewModels = LocalViewModelHolder.current
        val navigator = LocalNavigator.currentOrThrow

        HistoryScreen(
            viewModel = viewModels.historyViewModel,
            onNavigateBack = {
                navigator.pop()
            },
            onHistoryClick = { url ->
                viewModels.tabViewModel.navigateToUrl(url)
                navigator.popUntilRoot()
            }
        )
    }
}

/**
 * SettingsScreenNav - Browser settings screen
 */
data object SettingsScreenNav : Screen {

    @Composable
    override fun Content() {
        val viewModels = LocalViewModelHolder.current
        val navigator = LocalNavigator.currentOrThrow

        SettingsScreen(
            viewModel = viewModels.settingsViewModel,
            onNavigateBack = {
                navigator.pop()
            }
        )
    }
}

/**
 * XRSettingsScreenNav - WebXR settings screen
 * Note: XR features only work on Android, but UI is cross-platform
 */
data object XRSettingsScreenNav : Screen {

    @Composable
    override fun Content() {
        val viewModels = LocalViewModelHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val settings by viewModels.settingsViewModel.settings.collectAsState()

        // Only show XR settings if settings are loaded
        settings?.let { currentSettings ->
            XRSettingsScreen(
                settings = currentSettings,
                onSettingsChange = { newSettings ->
                    viewModels.settingsViewModel.updateSettings(newSettings)
                },
                onNavigateBack = {
                    navigator.pop()
                }
            )
        }
    }
}

/**
 * AboutScreenNav - About screen (app info, version, licenses)
 */
data object AboutScreenNav : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO: Create AboutScreen composable
        // For now, just pop back
        navigator.pop()
    }
}

/**
 * ARPreviewScreenNav - AR Layout Preview screen for testing spatial arc layout
 */
data object ARPreviewScreenNav : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        ARLayoutPreview(
            onBack = {
                navigator.pop()
            }
        )
    }
}
