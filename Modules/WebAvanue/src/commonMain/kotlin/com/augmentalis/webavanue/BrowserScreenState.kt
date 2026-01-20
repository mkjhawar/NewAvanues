package com.augmentalis.webavanue

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.augmentalis.webavanue.TabViewModel
import com.augmentalis.webavanue.Favorite
import com.augmentalis.webavanue.BrowserSettings

/**
 * BrowserScreenState - Encapsulates all UI state for BrowserScreen
 *
 * Extracted from BrowserScreen.kt for Single Responsibility Principle compliance.
 * Each state category is grouped together for maintainability.
 */
class BrowserScreenState(
    // URL input
    var urlInput: MutableState<String>,

    // Voice UI state
    var isListening: MutableState<Boolean>,
    var showTextCommand: MutableState<Boolean>,
    var showVoiceHelp: MutableState<Boolean>,
    var isScrollFrozen: MutableState<Boolean>,
    var lastVoiceCommand: MutableState<String?>,

    // Tab switcher states
    var showTabSwitcher: MutableState<Boolean>,
    var showSpatialTabSwitcher: MutableState<Boolean>,
    var showSpatialFavorites: MutableState<Boolean>,

    // Command bar visibility
    var isCommandBarVisible: MutableState<Boolean>,

    // Browser modes
    var isHeadlessMode: MutableState<Boolean>,
    var isLandscape: MutableState<Boolean>,

    // Dialog states
    var showAddPageDialog: MutableState<Boolean>,
    var newPageUrl: MutableState<String>,
    var showBasicAuthDialog: MutableState<Boolean>,
    var authUrl: MutableState<String>,
    var authRealm: MutableState<String?>,
    var showAddToFavoritesDialog: MutableState<Boolean>,

    // Tab group state
    var tabGroups: MutableState<List<com.augmentalis.webavanue.domain.model.TabGroup>>,
    var showTabGroupDialog: MutableState<Boolean>,
    var showTabGroupAssignmentDialog: MutableState<Boolean>,
    var selectedTabForGroupAssignment: MutableState<String?>,

    // Session restore state
    var showSessionRestoreDialog: MutableState<Boolean>,
    var sessionRestoreTabCount: MutableState<Int>,

    // Download location state
    var showDownloadLocationDialog: MutableState<Boolean>,
    var pendingDownloadRequest: MutableState<Any?>,
    var pendingDownloadSourceUrl: MutableState<String?>,
    var pendingDownloadSourceTitle: MutableState<String?>,
    var customDownloadPath: MutableState<String?>
)

/**
 * Creates and remembers BrowserScreenState
 */
@Composable
fun rememberBrowserScreenState(): BrowserScreenState {
    return BrowserScreenState(
        urlInput = rememberSaveable { mutableStateOf("") },
        isListening = rememberSaveable { mutableStateOf(false) },
        showTextCommand = rememberSaveable { mutableStateOf(false) },
        showVoiceHelp = rememberSaveable { mutableStateOf(false) },
        isScrollFrozen = rememberSaveable { mutableStateOf(false) },
        lastVoiceCommand = rememberSaveable { mutableStateOf<String?>(null) },
        showTabSwitcher = rememberSaveable { mutableStateOf(false) },
        showSpatialTabSwitcher = rememberSaveable { mutableStateOf(false) },
        showSpatialFavorites = rememberSaveable { mutableStateOf(false) },
        isCommandBarVisible = rememberSaveable { mutableStateOf(false) },
        isHeadlessMode = rememberSaveable { mutableStateOf(false) },
        isLandscape = remember { mutableStateOf(false) },
        showAddPageDialog = rememberSaveable { mutableStateOf(false) },
        newPageUrl = rememberSaveable { mutableStateOf("") },
        showBasicAuthDialog = rememberSaveable { mutableStateOf(false) },
        authUrl = rememberSaveable { mutableStateOf("") },
        authRealm = rememberSaveable { mutableStateOf<String?>(null) },
        showAddToFavoritesDialog = rememberSaveable { mutableStateOf(false) },
        tabGroups = rememberSaveable { mutableStateOf<List<com.augmentalis.webavanue.domain.model.TabGroup>>(emptyList()) },
        showTabGroupDialog = rememberSaveable { mutableStateOf(false) },
        showTabGroupAssignmentDialog = rememberSaveable { mutableStateOf(false) },
        selectedTabForGroupAssignment = rememberSaveable { mutableStateOf<String?>(null) },
        showSessionRestoreDialog = rememberSaveable { mutableStateOf(false) },
        sessionRestoreTabCount = rememberSaveable { mutableStateOf(0) },
        showDownloadLocationDialog = rememberSaveable { mutableStateOf(false) },
        pendingDownloadRequest = remember { mutableStateOf<Any?>(null) },
        pendingDownloadSourceUrl = rememberSaveable { mutableStateOf<String?>(null) },
        pendingDownloadSourceTitle = rememberSaveable { mutableStateOf<String?>(null) },
        customDownloadPath = rememberSaveable { mutableStateOf<String?>(null) }
    )
}

/**
 * Helper to show command bar briefly (for voice commands feedback)
 */
fun showCommandBarBriefly(
    isCommandBarVisible: MutableState<Boolean>,
    scope: CoroutineScope
) {
    if (!isCommandBarVisible.value) {
        isCommandBarVisible.value = true
        scope.launch {
            delay(3000L)
            isCommandBarVisible.value = false
        }
    }
}

/**
 * Check if URL is in favorites list
 */
@Composable
fun rememberIsFavorite(
    currentUrl: String?,
    favorites: List<Favorite>
): Boolean {
    return remember(currentUrl, favorites) {
        derivedStateOf {
            currentUrl?.let { url ->
                favorites.any { it.url == url }
            } ?: false
        }
    }.value
}
